package io.xtype.libraries.audittrail;

import static io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions.AUDIT_TRAIL_PATH;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.baggage.Baggage;
import org.springframework.web.util.UriComponentsBuilder;
import xtype.common.AuditContext;
import xtype.common.User;

public final class AuditContextBuilder {
  public static String ACTOR_TYPE_USER = "USER";
  public static String ACTOR_TYPE_SYSTEM = "SYSTEM";
  public static User SYSTEM = new User("SYSTEM", "System");



  private String operation;
  private String entityType;
  private String entityId;
  private String entityName;
  private String actorType;
  private User actor;

  private AuditContextBuilder() {}

  public static AuditContextBuilder newBuilder() {
    return new AuditContextBuilder();
  }

  public static AuditContextBuilder forContext(AuditContext context) {
    var builder = new AuditContextBuilder();
    builder.operation = context.getOperation() != null ? context.getOperation().toString() : null;
    builder.entityType = context.getEntityType() != null ? context.getEntityType().toString() : null;
    builder.entityId = context.getEntityId() != null ? context.getEntityId().toString() : null;
    builder.entityName = context.getEntityName() != null ? context.getEntityName().toString() : null;
    builder.actorType = context.getActorType() != null ? context.getActorType().toString() : null;
    builder.actor = context.getActor();
    return builder;
  }

  public static AuditContextBuilder forContext(
      String operation, String entityType, String entityId, String actorType, User actor) {
    var builder = new AuditContextBuilder();
    builder.operation = operation;
    builder.entityType = entityType;
    builder.entityId = entityId;
    builder.actorType = actorType;
    builder.actor = actor;
    return builder;
  }

  public AuditContextBuilder operation(String operation) {
    this.operation = operation;
    return this;
  }

  public AuditContextBuilder entityType(String entityType) {
    this.entityType = entityType;
    return this;
  }

  public AuditContextBuilder entityId(String entityId) {
    this.entityId = entityId;
    return this;
  }

  public AuditContextBuilder entityName(String entityName) {
    this.entityName = entityName;
    return this;
  }

  public AuditContextBuilder actorType(String actorType) {
    this.actorType = actorType;
    return this;
  }

  public AuditContextBuilder actor(User actor) {
    this.actor = actor;
    return this;
  }

  public AuditContext build() {

    if (actorType == null) {
      actorType = SYSTEM.getTechnicalUserName().toString();
    }
    if (actor == null) {
      actor = SYSTEM;
    }

    if (operation != null) {
      operation = "%s.%s".formatted(entityType, operation);
    }

    return AuditContext.newBuilder()
        .setOperation(operation)
        .setEntityType(entityType)
        .setEntityId(entityId)
        .setEntityName(entityName)
        .setActorType(actorType)
        .setActor(actor)
        .setPath(buildPath())
        .build();
  }

  private String buildPath() {
    var entryValue = Baggage.current().getEntryValue(AUDIT_TRAIL_PATH);

    requireNonNull(entityType, "value must not be null");
    requireNonNull(entityId, "value must not be null");

    UriComponentsBuilder builder;
    if (entryValue == null) {
      builder = UriComponentsBuilder.newInstance()
          .scheme("audittrail")
          .pathSegment(entityType, entityId);
    } else {
      builder = UriComponentsBuilder.fromUriString(entryValue)
          .pathSegment(entityType, entityId);
    }

    return builder.toUriString();
  }
}
