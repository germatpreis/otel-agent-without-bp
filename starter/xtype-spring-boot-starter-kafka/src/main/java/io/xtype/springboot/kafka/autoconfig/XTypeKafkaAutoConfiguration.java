package io.xtype.springboot.kafka.autoconfig;

import io.xtype.springboot.kafka.producer.KafkaProducer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@EnableKafka
@Import(TopicConfiguration.class)
@AutoConfiguration(before = KafkaAutoConfiguration.class)
public class XTypeKafkaAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(KafkaTemplate.class)
  KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
    var kafkaTemplate = new KafkaTemplate<>(producerFactory);
    kafkaTemplate.setObservationEnabled(true);
    return kafkaTemplate;
  }

  @Bean
  @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
  ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, Object> consumerFactory) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();

    factory.getContainerProperties().setObservationEnabled(true);
    factory.setConsumerFactory(consumerFactory);

    return factory;
  }

  @Bean
  <EventType> KafkaProducer<EventType> kafkaProducer(KafkaTemplate<String, EventType> kafkaTemplate) {
    return new KafkaProducer<>(kafkaTemplate);
  }
}

