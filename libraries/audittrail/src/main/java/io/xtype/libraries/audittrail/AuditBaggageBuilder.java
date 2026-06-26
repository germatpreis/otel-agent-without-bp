package io.xtype.libraries.audittrail;

import static io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions.AUDIT_TRAIL_PATH;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntryMetadata;
import java.util.Optional;
import org.springframework.web.util.UriComponentsBuilder;

public class AuditBaggageBuilder {
  public record DomainEntityId(String entityType, String entityId) {}
  private final BaggageBuilder baggageBuilder;
  private String uri;

  private AuditBaggageBuilder() {
    baggageBuilder = Baggage.current().toBuilder();
  }

  public static AuditBaggageBuilder newBuilder() {
    return new AuditBaggageBuilder();
  }

  public AuditBaggageBuilder auditDomainEntity(String entityType, String entityId) {
    return auditDomainEntity(new DomainEntityId(entityType, entityId));
  }

  public AuditBaggageBuilder auditDomainEntity(DomainEntityId segment) {
    var entryValue = Baggage.current().getEntryValue(AUDIT_TRAIL_PATH);

    requireNonNull(segment.entityType, "value must not be null");
    requireNonNull(segment.entityId, "value must not be null");

    UriComponentsBuilder builder;
    if (entryValue == null) {
      builder = UriComponentsBuilder.newInstance()
          .scheme("audittrail")
          .pathSegment(segment.entityType, segment.entityId);
    } else {
      builder = UriComponentsBuilder.fromUriString(entryValue)
          .pathSegment(segment.entityType, segment.entityId);
    }

    this.uri = builder.toUriString();

    put(AUDIT_TRAIL_PATH, uri);
    return this;
  }

  public static Optional<DomainEntityId> getCurrentDomainEntityId() {
    return ofNullable(Baggage.current().getEntryValue(AUDIT_TRAIL_PATH))
        .flatMap(uri -> {
          var segments = UriComponentsBuilder.fromUriString(uri).build().getPathSegments();

          if (segments.size() < 2) {
            return empty();
          }

          var size = segments.size();

          return of(new DomainEntityId(
              segments.get(size - 2),
              segments.get(size - 1)));
        });
  }

  public static Optional<String> getPath() {
    return ofNullable(Baggage.current().getEntryValue(AUDIT_TRAIL_PATH));
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
