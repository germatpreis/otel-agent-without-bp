package io.xtype.springboot.kafka.autoconfig;

import static io.xtype.springboot.kafka.ApplicationConstants.Topics.TOPIC_DATA;
import static io.xtype.springboot.kafka.ApplicationConstants.Topics.TOPIC_PACKAGE;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;

@Configuration
public class TopicConfiguration {

  @Bean
  NewTopics xtypeTopics() {
    return new NewTopics(
        TopicBuilder.name(TOPIC_DATA).build(),
        TopicBuilder.name(TOPIC_PACKAGE).build()
    );
  }

}
