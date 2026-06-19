package io.xtype.server.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface KafkaNotificationActivities {

  @ActivityMethod
  void notifyDeploymentStarted(DeployContentItemContext context);

  @ActivityMethod
  void notifyDeploymentCompleted(DeployContentItemContext context);
}
