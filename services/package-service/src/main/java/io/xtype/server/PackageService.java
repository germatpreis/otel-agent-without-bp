package io.xtype.server;

import static io.xtype.libraries.audittrail.AuditContextBuilder.ACTOR_TYPE_USER;
import static io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions.AUDIT_TRAIL_PATH;
import static io.xtype.springboot.kafka.ApplicationConstants.Topics.TOPIC_PACKAGE;

import io.opentelemetry.api.baggage.Baggage;
import io.xtype.libraries.audittrail.AuditContextBuilder;
import io.xtype.server.PackageController.DeployPackageRequest;
import io.xtype.springboot.kafka.producer.KafkaProducer;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
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
    var auditContext = AuditContextBuilder.forContext(auditContextFromRequest(request)).build();
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
        .setEntityType("package")
        .setOperation("deploy")
        .setEntityId(request.packageId().toString())
        .setEntityName(request.packageName())
        .setActorType(ACTOR_TYPE_USER)
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
