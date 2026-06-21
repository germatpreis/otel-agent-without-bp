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
  private final KafkaProducer<Object> kafkaProducer;

  PackageService(KafkaProducer<Object> kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
  }

  public void deployPackage(@Valid DeployPackageRequest request) {
    // create the initial audit context (no baggage entry for `x-at-path` yet)
    // after enrichment, the context has a audittrail://package/<packageId> value in $.path
    var auditContext = enrichAuditContextWithPath(auditContextFromRequest(request));
    var base = dummyBase();
    var payload = payloadFromRequest(request);

    var message = MessageV1.newBuilder()
        .setBase(base)
        .setAudit(auditContext)
        .setPayload(payload)
        .build();

    var rec = new ProducerRecord<String, Object>(TOPIC_PACKAGE, message);

    try (var scope = Baggage.current().toBuilder()
        // attach the audittrail://package/<packageId> to the baggage for the next call
        // all subsequent requests (will have this baggage set)
        .put(AUDIT_TRAIL_PATH, auditContext.getPath().toString())
        .build()
        .makeCurrent()) {
      kafkaProducer.sendToKafkaAsync(rec);
    }
  }

  AuditContext enrichAuditContextWithPath(AuditContext context) {
    var path = buildAuditTrailPathFromAuditContext(context);
    return AuditContext.newBuilder(context).setPath(path).build();
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

  private static AuditContext auditContextFromRequest(DeployPackageRequest request) {
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

  private static DeployPackageEventV1 payloadFromRequest(DeployPackageRequest request) {
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
