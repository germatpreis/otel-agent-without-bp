package io.xtype.server.temporal;

import java.util.UUID;

public record DeployContentItemContext(
    UUID eventUuid,
    AuditInfo audit,
    String contentItemUid,
    String contentItemType,
    String contentItemName
) {

  public record AuditInfo(
      String operation,
      String entityType,
      String entityId,
      String entityName,
      String actorType,
      String actorTechnicalName,
      String actorDisplayName
  ) {}
}
