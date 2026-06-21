package io.xtype.server;

import static io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions.AUDIT_TRAIL_PATH;
import static io.xtype.springboot.kafka.ApplicationConstants.Topics.TOPIC_PACKAGE;

import io.opentelemetry.api.baggage.Baggage;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.xtype.libraries.audittrail.AuditContextBuilder;
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
import xtype.common.Base;
import xtype.package$.event.internal.v1.MessageV1;
import xtype.package$.event.internal.v1.data.DeployPackageEventV1;
import xtype.package$.event.internal.v1.data.PackageContentItemV1;

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

    for (var contentItem : payload.getContent()) {

      var newAuditContext = AuditContextBuilder.newBuilder()
          .entityType("updateset")
          .entityId(contentItem.getUid().toString())
          .entityName(contentItem.getName().toString())
          .operation("deploy")
          .build();

      try (var scope = Baggage.current().toBuilder()
          // attach the audittrail://package/<packageId>/updateset/<updateSetUid> to the baggage for the next call
          // all subsequent requests (will have this baggage set)
          .put(AUDIT_TRAIL_PATH, newAuditContext.getPath().toString())
          .build()
          .makeCurrent()) {

        var temporalContext = buildTemporalContext(base, newAuditContext, contentItem);
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

  private static DeployContentItemContext buildTemporalContext(
      Base base, AuditContext audit, PackageContentItemV1 contentItem
  ) {
    return new DeployContentItemContext(
        base.getUuid(),
        audit != null ? toAuditInfo(audit) : null,
        contentItem.getUid(),
        contentItem.getType().toString(),
        contentItem.getName().toString()
    );
  }

  private static AuditInfo toAuditInfo(AuditContext audit) {
    var actor = audit.getActor();
    return new AuditInfo(
        audit.getOperation().toString(),
        audit.getEntityType().toString(),
        audit.getEntityId().toString(),
        audit.getEntityName() != null ? audit.getEntityName().toString() : null,
        audit.getActorType().toString(),
        actor.getTechnicalUserName() != null ? actor.getTechnicalUserName().toString() : null,
        actor.getDisplayUserName() != null ? actor.getDisplayUserName().toString() : null
    );
  }
}
