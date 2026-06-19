package io.xtype.server.temporal;

import io.temporal.spring.boot.ActivityImpl;
import io.xtype.springboot.kafka.producer.KafkaProducer;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "main")
public class KafkaNotificationActivitiesImpl implements KafkaNotificationActivities {

  private final KafkaProducer<Object> kafkaProducer;

  public KafkaNotificationActivitiesImpl(KafkaProducer<Object> kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
  }

  @Override
  public void notifyDeploymentStarted(DeployContentItemContext context) {
    // TODO: build Avro message from context and send via kafkaProducer
  }

  @Override
  public void notifyDeploymentCompleted(DeployContentItemContext context) {
    // TODO: build Avro message from context and send via kafkaProducer
  }
}
