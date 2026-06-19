package io.xtype.server.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;

@WorkflowImpl(workers = "main")
public class DeployContentItemWorkflowImpl implements DeployContentItemWorkflow {

  private final KafkaNotificationActivities kafkaActivities = Workflow.newActivityStub(
      KafkaNotificationActivities.class,
      ActivityOptions.newBuilder()
          .setStartToCloseTimeout(Duration.ofSeconds(30))
          .build()
  );

  private final PolicyCheckActivities policyActivities = Workflow.newActivityStub(
      PolicyCheckActivities.class,
      ActivityOptions.newBuilder()
          .setStartToCloseTimeout(Duration.ofSeconds(30))
          .build()
  );

  @Override
  public void execute(DeployContentItemContext context) {
    kafkaActivities.notifyDeploymentStarted(context);
    policyActivities.checkPolicy(context);
    kafkaActivities.notifyDeploymentCompleted(context);
  }
}
