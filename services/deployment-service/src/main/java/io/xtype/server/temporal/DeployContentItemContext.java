package io.xtype.server.temporal;

import java.util.UUID;

public record DeployContentItemContext(
    UUID eventUuid,
    String eventType,
    String eventVersion,
    long createdAt,
    String applicationId,
    AuditInfo audit,
    UUID contentItemUid,
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
