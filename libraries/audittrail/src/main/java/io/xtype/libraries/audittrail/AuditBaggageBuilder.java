package io.xtype.libraries.audittrail;

import static io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions.AUDIT_TRAIL_PATH;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntryMetadata;
import org.springframework.web.util.UriComponentsBuilder;

public class AuditBaggageBuilder {

  private BaggageBuilder baggageBuilder;
  private String uri;

  private AuditBaggageBuilder() {
    baggageBuilder = Baggage.current().toBuilder();
  }

  public static AuditBaggageBuilder newBuilder() {
    return new AuditBaggageBuilder();
  }

  public AuditBaggageBuilder auditEntity(String entityType, String entityId) {
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

    this.uri = builder.toUriString();

    put(AUDIT_TRAIL_PATH, uri);
    return this;
  }

  public AuditBaggageBuilder put(String key, String value, BaggageEntryMetadata entryMetadata) {
    baggageBuilder.put(key, value, entryMetadata);
    return this;
  }

  public AuditBaggageBuilder remove(String key) {
    baggageBuilder.remove(key);
    return this;
  }

  public AuditBaggageBuilder put(String key, String value) {
    baggageBuilder.put(key, value);
    return this;
  }

  public String getUri() {
    return uri;
  }

  public Baggage build() {
    return baggageBuilder.build();
  }
}
