package io.xtype.libraries.audittrail;

import static io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions.AUDIT_TRAIL_PATH;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.baggage.Baggage;
import org.springframework.web.util.UriComponentsBuilder;
import xtype.common.AuditContext;

public final class AuditContextBuilder {

  private final AuditContext context;

  private AuditContextBuilder(AuditContext context) {
    this.context = context;
  }

  public static AuditContextBuilder forContext(AuditContext context) {
    return new AuditContextBuilder(context);
  }

  public AuditContext build() {
    var path = buildPath();
    return AuditContext.newBuilder(context).setPath(path).build();
  }

  private String buildPath() {
    var entryValue = Baggage.current().getEntryValue(AUDIT_TRAIL_PATH);

    var entityType = context.getEntityType();
    var entityId = context.getEntityId();

    requireNonNull(entityType, "value must not be null");
    requireNonNull(entityId, "value must not be null");

    var builder = UriComponentsBuilder.newInstance();

    if (entryValue == null) {
      builder = builder
          .scheme("audittrail")
          .pathSegment(entityType.toString(), entityId.toString());
    } else {
      builder = UriComponentsBuilder
          .fromUriString(entryValue)
          .pathSegment(entityType.toString(), entityId.toString());
    }

    return builder.toUriString();
  }
}
