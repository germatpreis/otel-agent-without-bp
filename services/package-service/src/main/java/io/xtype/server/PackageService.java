package io.xtype.server;

import static io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions.AUDIT_TRAIL_PATH;
import static io.xtype.springboot.kafka.ApplicationConstants.Topics.TOPIC_PACKAGE;
import static java.util.Objects.requireNonNull;

import io.xtype.server.PackageController.DeployPackageRequest;
import io.xtype.springboot.kafka.producer.KafkaProducer;
import io.opentelemetry.api.baggage.Baggage;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import xtype.common.AuditContext;
import xtype.common.Base;
import xtype.common.User;
import xtype.package$.event.internal.v1.MessageV1;
import xtype.package$.event.internal.v1.data.DeployPackageEventV1;
import xtype.package$.event.internal.v1.data.PackageContentItemV1;

@Service
class PackageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PackageService.class);

  private final KafkaProducer<Object> kafkaProducer;

  PackageService(KafkaProducer<Object> kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
  }

  public void deployPackage(@Valid DeployPackageRequest request) {
    var message = buildMessage(request);
    var rec = new ProducerRecord<String, Object>(TOPIC_PACKAGE, message);

    var path = buildAuditTrailPathFromAuditContext(message.getAudit());

    try (var scope = Baggage.current().toBuilder()
        .put(AUDIT_TRAIL_PATH, path)
        .build()
        .makeCurrent()) {
      kafkaProducer.sendToKafkaAsync(rec);
    }
  }

  String buildAuditTrailPathFromAuditContext(AuditContext auditContext) {
    var currentBaggage = Baggage.current();
    var entryValue = currentBaggage.getEntryValue(AUDIT_TRAIL_PATH);

    var entityType = auditContext.getEntityType();
    var entityId = auditContext.getEntityId();

    requireNonNull(entityType, "value must not be null");
    requireNonNull(entityId, "value must not be null");

    var builder = UriComponentsBuilder.newInstance();

    if (entryValue == null) {
      builder = builder
          .scheme("audittrail")
          .pathSegment(entityType.toString(), entityId.toString());
    } else {
      builder = UriComponentsBuilder
          .fromUriString(entryValue)
          .pathSegment(entityType.toString(), entityId.toString());
    }
    return builder.toUriString();
  }

  @KafkaListener(topics = TOPIC_PACKAGE)
  public void dummyListener(MessageV1 message) {
    LOGGER.info("Received message: " + message);
  }

  private static MessageV1 buildMessage(DeployPackageRequest request) {
    return MessageV1.newBuilder()
        .setBase(dummyBase())
        .setAudit(dummyAudit(request))
        .setPayload(buildPayload(request))
        .build();
  }

  private static Base dummyBase() {
    return Base.newBuilder()
        .setUuid(UUID.randomUUID())
        .setUid(null)
        .setEventType("package.deploy")
        .setEventVersion("1.0")
        .setCreatedAt(Instant.now().toEpochMilli())
        .setApplicationId("package-service")
        .setSnowCompanyId(null)
        .setSnowInstanceId(null)
        .build();
  }

  private static AuditContext dummyAudit(DeployPackageRequest request) {
    return AuditContext.newBuilder()
        .setOperation("package.deploy")
        .setEntityType("package")
        .setEntityId(request.packageId().toString())
        .setEntityName(request.packageName())
        .setActorType("USER")
        .setActor(User.newBuilder()
            .setTechnicalUserName(request.triggeredBy())
            .setDisplayUserName(request.triggeredBy())
            .build())
        .build();
  }

  private static DeployPackageEventV1 buildPayload(DeployPackageRequest request) {
    return DeployPackageEventV1.newBuilder()
        .setTriggeredBy(request.triggeredBy())
        .setPackageName(request.packageName())
        .setPackageId(request.packageId())
        .setContent(request.content().stream()
            .map(item -> PackageContentItemV1.newBuilder()
                .setUid(item.uid())
                .setType(item.type())
                .setName(item.name())
                .build())
            .toList())
        .build();
  }
}
