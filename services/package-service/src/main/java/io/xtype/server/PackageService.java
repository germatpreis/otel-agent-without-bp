package io.xtype.server;

import io.xtype.server.PackageController.DeployPackageRequest;
import io.xtype.springboot.kafka.Topics;
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
    var message = buildMessage(request);
    var record = new ProducerRecord<String, Object>(Topics.TOPIC_PACKAGE, message);
    kafkaProducer.sendToKafkaAsync(record);
  }

  private static MessageV1 buildMessage(DeployPackageRequest request) {
    return MessageV1.newBuilder()
        .setBase(dummyBase())
        .setAudit(dummyAudit())
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

  private static AuditContext dummyAudit() {
    return AuditContext.newBuilder()
        .setOperation("package.deploy")
        .setEntityType("package")
        .setEntityId("dummy-entity-id")
        .setEntityName(null)
        .setActorType("SYSTEM")
        .setActor(User.newBuilder()
            .setTechnicalUserName(null)
            .setDisplayUserName(null)
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
