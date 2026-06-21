package io.xtype.server;

import static io.xtype.springboot.kafka.ApplicationConstants.Topics.TOPIC_PACKAGE;

import io.xtype.libraries.audittrail.AuditBaggageBuilder;
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
    // create kafka message (including auditContext which we sent in-line with the fact message)
    var auditContext = createAuditContext(request);
    var payload = createPayload(request);
    var base = createBase();

    var message = MessageV1.newBuilder()
        .setBase(base)
        .setAuditContext(auditContext)
        .setPayload(payload)
        .build();

    var rec = new ProducerRecord<String, Object>(TOPIC_PACKAGE, message);

    // prepare audit context information needed to propagate (entity type + id)
    var auditBaggage = AuditBaggageBuilder
        .newBuilder()
        .auditDomainEntity(auditContext.getEntityType().toString(), auditContext.getEntityId().toString());

    // attach the audittrail://package/<packageId> to the baggage for the next call
    // all following requests (regardless of the transport mechanism) will have this baggage set
    try (var scope = auditBaggage.build().makeCurrent()) {
      kafkaProducer.sendToKafkaAsync(rec);
    }
  }

  private static Base createBase() {
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

  private static AuditContext createAuditContext(DeployPackageRequest request) {
    return AuditContext.newBuilder()
        .setEntityType("package")
        .setOperation("deploy")
        .setEntityId(request.packageId().toString())
        .setEntityName(request.packageName())
        .setActorType("USER")
        .setActor(User.newBuilder()
            .setTechnicalUserName(request.triggeredBy())
            .setDisplayUserName(request.triggeredBy())
            .build())
        .build();
  }

  private static DeployPackageEventV1 createPayload(DeployPackageRequest request) {
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
