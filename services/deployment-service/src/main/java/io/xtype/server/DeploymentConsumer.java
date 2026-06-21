package io.xtype.server;

import static io.xtype.springboot.kafka.ApplicationConstants.Topics.TOPIC_PACKAGE;

import io.opentelemetry.api.baggage.Baggage;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.xtype.libraries.audittrail.AuditBaggageBuilder;
import io.xtype.server.temporal.DeployContentItemContext;
import io.xtype.server.temporal.DeployContentItemContext.AuditInfo;
import io.xtype.server.temporal.DeployContentItemWorkflow;
import io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import xtype.common.AuditContext;
import xtype.package$.event.internal.v1.MessageV1;
import xtype.package$.event.internal.v1.data.DeployPackageEventV1;

@Service
public class DeploymentConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentConsumer.class);
  private static final String TASK_QUEUE = "deployment-service";

  private final WorkflowClient workflowClient;

  public DeploymentConsumer(WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
  }

  @KafkaListener(topics = TOPIC_PACKAGE)
  public void onMessage(MessageV1 message) {
    var path = Baggage.current().getEntryValue(OtelSemanticConventions.AUDIT_TRAIL_PATH);
    LOGGER.info("Received audittrail path {}", path);

    var base = message.getBase();
    var payload = (DeployPackageEventV1) message.getPayload();
    var auditContext = message.getAuditContext();

    var temporalAuditInfo = toAuditInfo(auditContext);

    var entityType = "updateset";

    for (var contentItem : payload.getContent()) {
      var entityId = contentItem.getUid().toString();
      var entityName = contentItem.getName().toString();

      // prepare audit context information needed to propagate (entity type + id)
      var auditBaggage = AuditBaggageBuilder
          .newBuilder()
          .auditDomainEntity(entityType, entityId);

      // attach the audittrail://package/<packageId>/updateset/<updateSetUid> to the baggage for the next call
      // all following requests (regardless of the transport mechanism) will have this baggage set
      try (var scope = auditBaggage.build().makeCurrent()) {
        var temporalContext = new DeployContentItemContext(
            base.getUuid(),
            temporalAuditInfo,
            entityId,
            entityType,
            entityName
        );
        startWorkflow(temporalContext);
      }
    }
  }

  private void startWorkflow(DeployContentItemContext context) {
    var options = WorkflowOptions.newBuilder()
        .setWorkflowId("deploy-content-item-" + UUID.randomUUID())
        .setTaskQueue(TASK_QUEUE)
        .build();
    var stub = workflowClient.newWorkflowStub(DeployContentItemWorkflow.class, options);
    WorkflowClient.start(stub::execute, context);
  }

  private static AuditInfo toAuditInfo(AuditContext audit) {
    var actor = audit.getActor();
    return new AuditInfo(
        audit.getOperation().toString(),
        audit.getEntityType().toString(),
        audit.getEntityId().toString(),
        audit.getEntityName().toString(),
        audit.getActorType().toString(),
        actor.getTechnicalUserName().toString(),
        actor.getDisplayUserName().toString()
    );
  }
}
