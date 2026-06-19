package io.xtype.springboot.kafka.producer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

public final class KafkaProducer<EventType> {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducer.class);
  private static final long DEFAULT_TIMEOUT_SECONDS = 5;
  private static final long BATCH_TIMEOUT_PER_100_RECORDS_SECONDS = 2;
  private static final long MAX_BATCH_TIMEOUT_SECONDS = 20;

  private final KafkaTemplate<String, EventType> kafkaTemplate;

  public KafkaProducer(KafkaTemplate<String, EventType> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void sendToKafkaAsync(List<ProducerRecord<String, EventType>> records) {
    for (var rec : records) {
      sendToKafkaAsync(rec);
    }
  }

  public void sendToKafkaSync(ProducerRecord<String, EventType> rec) {
    var key = rec.key();
    try {
      LOGGER.trace("Sending record in SYNC mode. Key: {}", key);
      kafkaTemplate.send(rec).get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      LOGGER.trace("Record in SYNC mode sent. Key: {}", key);
    } catch (Exception e) {
      var message = String.format("Could not produce to topic %s synchronously", rec.topic());
      LOGGER.error(message, e);
      throw new KafkaSyncProducerException(message, e);
    }
  }

  public void sendToKafkaAsync(ProducerRecord<String, EventType> rec) {
    var key = rec.key();
    LOGGER.trace("Sending record in ASYNC mode. Key: {}", key);
    var sendResult = kafkaTemplate.send(rec);
    LOGGER.trace("Record in ASYNC mode sent. Key: {}", key);
    sendResult.whenComplete((result, throwable) -> {
      var castedThrowable = (KafkaProducerException) throwable;
      var r = result.getProducerRecord();

      if (throwable != null) {
        handleFailure(r, castedThrowable);
      } else {
        handleSuccess(r);
      }
    });
  }

  public void sendBatchToKafkaSync(List<ProducerRecord<String, EventType>> recs) {
    if (recs.isEmpty()) {
      LOGGER.trace("Batch send called with empty list, skipping");
      return;
    }

    long timeoutSeconds = calculateBatchTimeout(recs.size());

    try {
      LOGGER.trace("Sending batch of {} records in SYNC mode (timeout: {}s)", recs.size(), timeoutSeconds);
      List<CompletableFuture<SendResult<String, EventType>>> futures = new ArrayList<>();

      // Send all messages (async)
      for (ProducerRecord<String, EventType> record : recs) {
        CompletableFuture<SendResult<String, EventType>> future =
            kafkaTemplate.send(record);
        futures.add(future);
      }

      // Wait for ALL to complete with calculated timeout
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .get(timeoutSeconds, TimeUnit.SECONDS);
      LOGGER.trace("Batch of {} records in SYNC mode sent successfully", recs.size());
    } catch (Exception e) {
      var topics = recs.stream().map(ProducerRecord::topic).distinct().toList();
      var message = String.format("Could not produce batch of %d records to topics %s synchronously", recs.size(), topics);
      LOGGER.error(message, e);
      throw new KafkaSyncProducerException(message, e);
    }
  }

  /**
   * Calculates timeout for batch operations based on the number of records.
   * Formula: BASE_TIMEOUT + (records / 100) * PER_100_RECORDS_TIMEOUT, capped at MAX_TIMEOUT
   * Examples:
   * - 10 records: 5s
   * - 100 records: 7s
   * - 700 records: 19s
   * - 800 records: 20s (max)
   */
  private long calculateBatchTimeout(int recordCount) {
    long calculatedTimeout = DEFAULT_TIMEOUT_SECONDS +
        ((long) recordCount / 100) * BATCH_TIMEOUT_PER_100_RECORDS_SECONDS;
    return Math.min(calculatedTimeout, MAX_BATCH_TIMEOUT_SECONDS);
  }

  public ProducerRecord<String, EventType> createProducerRecord(String topic, String messageKey, EventType event) {
    return new ProducerRecord<>(topic, messageKey, event);
  }

  private void handleFailure(ProducerRecord<String, EventType> rec, Exception castedThrowable) {
    var value = rec.value();
    LOGGER.error(
        "Couldn't produce {} '{}' onto topic '{}'",
        value.getClass().getSimpleName(),
        value,
        rec.topic(),
        castedThrowable
    );
  }

  private void handleSuccess(ProducerRecord<String, EventType> rec) {
    var value = rec.value();
    LOGGER.trace(
        "Produced {} '{}' onto topic '{}'",
        value.getClass().getSimpleName(),
        value,
        rec.topic()
    );
  }

  public static class KafkaSyncProducerException extends RuntimeException {

    public KafkaSyncProducerException(String message) {
      super(message);
    }

    public KafkaSyncProducerException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
