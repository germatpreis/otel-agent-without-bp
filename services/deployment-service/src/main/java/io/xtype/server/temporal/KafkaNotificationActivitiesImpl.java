package io.xtype.server.temporal;

import static io.xtype.springboot.kafka.ApplicationConstants.Topics.TOPIC_AUDIT;

import io.temporal.spring.boot.ActivityImpl;
import io.xtype.libraries.audittrail.AuditBaggageBuilder;
import io.xtype.springboot.kafka.producer.KafkaProducer;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xtype.audit.event.external.v1.MessageV1;
import xtype.audit.event.external.v1.data.AuditEventV1;
import xtype.common.AuditContext;
import xtype.common.Base;
import xtype.common.User;

@Component
@ActivityImpl(workers = "main")
public class KafkaNotificationActivitiesImpl implements KafkaNotificationActivities {
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaNotificationActivitiesImpl.class);

  private final KafkaProducer<Object> kafkaProducer;

  public KafkaNotificationActivitiesImpl(KafkaProducer<Object> kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
  }

  @Override
  public void notifyDeploymentStarted(DeployContentItemContext context) {
    var path = AuditBaggageBuilder.getPath();
    LOGGER.info("Received audittrail path {}", path);

    // TODO: need to correlate the started and ended message (not sure how to do this with temporal)!
    var rec = createDeploymentStartedMessage(context);
    kafkaProducer.sendToKafkaAsync(rec);
  }

  @Override
  public void notifyDeploymentCompleted(DeployContentItemContext context) {
    var path = AuditBaggageBuilder.getPath();
    LOGGER.info("Received audittrail path {}", path);

    // TODO: need to correlate the started and ended message (not sure how to do this with temporal)!
    var rec = createDeploymentStartedMessage(context);
    kafkaProducer.sendToKafkaAsync(rec);
  }

  private ProducerRecord<String, Object> createDeploymentStartedMessage(DeployContentItemContext context) {
    var payload = AuditEventV1.newBuilder().build();
    var base = createBase();
    var auditContext = createAuditContext(context, "deploy");

    var message = MessageV1.newBuilder()
        .setBase(base)
        .setPayload(payload)
        .setAudit(auditContext)
        .build();

    return new ProducerRecord<>(TOPIC_AUDIT, message);
  }

  private static Base createBase() {
    return Base.newBuilder()
        .setUuid(UUID.randomUUID())
        .setUid(null)
        .setEventType(AuditEventV1.SCHEMA$.getName())
        .setEventVersion("1.0")
        .setCreatedAt(Instant.now().toEpochMilli())
        .setApplicationId("deployment-service")
        .setSnowCompanyId(null)
        .setSnowInstanceId(null)
        .build();
  }

  private static AuditContext createAuditContext(DeployContentItemContext context, String operation) {
    // TODO: improve DX...
    var domainEntityId = AuditBaggageBuilder.getCurrentDomainEntityId();
    var path = AuditBaggageBuilder.getPath();
    return AuditContext.newBuilder()
        .setEntityType(domainEntityId.get().entityType())
        .setEntityId(domainEntityId.get().entityId())
        .setEntityName(context.contentItemName())
        .setOperation(operation)
        .setActorType("USER")
        .setPath(path.get())
        .setActor(User.newBuilder()
            .setTechnicalUserName(context.audit().actorTechnicalName())
            .setDisplayUserName(context.audit().actorDisplayName())
            .build())
        .build();
  }
}
