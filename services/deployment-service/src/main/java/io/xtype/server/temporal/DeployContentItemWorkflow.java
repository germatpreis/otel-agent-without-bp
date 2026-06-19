package io.xtype.server.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DeployContentItemWorkflow {

  @WorkflowMethod
  void execute(DeployContentItemContext context);
}
