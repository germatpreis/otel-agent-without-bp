package io.xtype.springboot.kafka.autoconfig;

import io.xtype.springboot.kafka.Topics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;

@Configuration
public class TopicConfiguration {

  @Bean
  NewTopics xtypeTopics() {
    return new NewTopics(
        TopicBuilder.name(Topics.TOPIC_DATA).build(),
        TopicBuilder.name(Topics.TOPIC_PACKAGE).build()
    );
  }

}
