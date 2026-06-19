package io.xtype.server;

import io.xtype.springboot.kafka.Topics;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import xtype.package$.event.internal.v1.MessageV1;

@Service
public class DeploymentConsumer {

  private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DeploymentConsumer.class);

  @KafkaListener(topics = Topics.TOPIC_PACKAGE)
  public void dummyListener(MessageV1 message) {
    LOGGER.info("Received message: {}", message);
  }

}
