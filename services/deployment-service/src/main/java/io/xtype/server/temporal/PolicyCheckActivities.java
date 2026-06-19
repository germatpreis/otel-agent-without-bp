package io.xtype.server.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PolicyCheckActivities {

  @ActivityMethod
  void checkPolicy(DeployContentItemContext context);
}
