package io.xtype.server;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.xtype.server.temporal.DeployContentItemContext;
import io.xtype.server.temporal.DeployContentItemContext.AuditInfo;
import io.xtype.server.temporal.DeployContentItemWorkflow;
import io.xtype.springboot.kafka.Topics;
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

  @KafkaListener(topics = Topics.TOPIC_PACKAGE)
  public void onMessage(MessageV1 message) {
    LOGGER.info("Received message: {}", message);

    var payload = (DeployPackageEventV1) message.getPayload();
    var base = message.getBase();
    var audit = message.getAudit();

    for (var contentItem : payload.getContent()) {
      var context = buildContext(base, audit, contentItem);
      startWorkflow(context);
    }
  }

  private void startWorkflow(DeployContentItemContext context) {
    var options = WorkflowOptions.newBuilder()
        .setWorkflowId("deploy-content-item-" + context.contentItemUid())
        .setTaskQueue(TASK_QUEUE)
        .build();
    var stub = workflowClient.newWorkflowStub(DeployContentItemWorkflow.class, options);
    WorkflowClient.start(stub::execute, context);
  }

  private static DeployContentItemContext buildContext(
      Base base, AuditContext audit, PackageContentItemV1 contentItem
  ) {
    return new DeployContentItemContext(
        base.getUuid(),
        base.getEventType().toString(),
        base.getEventVersion().toString(),
        base.getCreatedAt(),
        base.getApplicationId().toString(),
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
