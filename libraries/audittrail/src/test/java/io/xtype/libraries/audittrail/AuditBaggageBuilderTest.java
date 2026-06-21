package io.xtype.libraries.audittrail;

import static io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions.AUDIT_TRAIL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.baggage.Baggage;
import org.junit.jupiter.api.Test;

class AuditBaggageBuilderTest {

  @Test
  void givenNoExistingPath_whenAuditDomainEntity_expectNewPathCreated() {
    var baggage = AuditBaggageBuilder.newBuilder()
        .auditDomainEntity("package", "pkg-123")
        .build();

    assertThat(baggage.getEntryValue(AUDIT_TRAIL_PATH)).isEqualTo("audittrail:/package/pkg-123");
  }

  @Test
  void givenNoExistingPath_whenAuditDomainEntity_expectUriReturnedByGetUri() {
    var builder = AuditBaggageBuilder.newBuilder()
        .auditDomainEntity("release", "rel-456");

    assertThat(builder.getUri()).isEqualTo("audittrail:/release/rel-456");
  }

  @Test
  void givenExistingPathInBaggage_whenAuditDomainEntity_expectPathAppended() {
    var existingPath = "audittrail:/release/rel-1";

    try (var scope = Baggage.current().toBuilder()
        .put(AUDIT_TRAIL_PATH, existingPath)
        .build()
        .makeCurrent()) {

      var baggage = AuditBaggageBuilder.newBuilder()
          .auditDomainEntity("package", "pkg-2")
          .build();

      assertThat(baggage.getEntryValue(AUDIT_TRAIL_PATH))
          .isEqualTo("audittrail:/release/rel-1/package/pkg-2");
    }
  }

  @Test
  void givenNullEntityType_whenAuditDomainEntity_expectNullPointerException() {
    assertThatThrownBy(() -> AuditBaggageBuilder.newBuilder().auditDomainEntity(null, "pkg-1"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void givenNullEntityId_whenAuditDomainEntity_expectNullPointerException() {
    assertThatThrownBy(() -> AuditBaggageBuilder.newBuilder().auditDomainEntity("package", null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void whenPut_expectEntryInBaggage() {
    var baggage = AuditBaggageBuilder.newBuilder()
        .put("custom-key", "custom-value")
        .build();

    assertThat(baggage.getEntryValue("custom-key")).isEqualTo("custom-value");
  }

  @Test
  void whenRemove_expectEntryRemovedFromBaggage() {
    try (var scope = Baggage.current().toBuilder()
        .put("to-remove", "some-value")
        .build()
        .makeCurrent()) {

      var baggage = AuditBaggageBuilder.newBuilder()
          .remove("to-remove")
          .build();

      assertThat(baggage.getEntryValue("to-remove")).isNull();
    }
  }

  @Test
  void givenNoBaggagePath_whenGetCurrentDomainEntityId_expectEmpty() {
    assertThat(AuditBaggageBuilder.getCurrentDomainEntityId()).isEmpty();
  }

  @Test
  void givenPathWithTwoSegments_whenGetCurrentDomainEntityId_expectEntityTypeAndId() {
    try (var scope = Baggage.current().toBuilder()
        .put(AUDIT_TRAIL_PATH, "audittrail:/package/pkg-123")
        .build()
        .makeCurrent()) {

      var result = AuditBaggageBuilder.getCurrentDomainEntityId();

      assertThat(result).isPresent();
      assertThat(result.get().entityType()).isEqualTo("package");
      assertThat(result.get().entityId()).isEqualTo("pkg-123");
    }
  }

  @Test
  void givenPathWithMultipleSegments_whenGetCurrentDomainEntityId_expectLastTwoSegments() {
    try (var scope = Baggage.current().toBuilder()
        .put(AUDIT_TRAIL_PATH, "audittrail:/release/rel-1/package/pkg-2")
        .build()
        .makeCurrent()) {

      var result = AuditBaggageBuilder.getCurrentDomainEntityId();

      assertThat(result).isPresent();
      assertThat(result.get().entityType()).isEqualTo("package");
      assertThat(result.get().entityId()).isEqualTo("pkg-2");
    }
  }

  @Test
  void givenPathWithOneSegment_whenGetCurrentDomainEntityId_expectEmpty() {
    try (var scope = Baggage.current().toBuilder()
        .put(AUDIT_TRAIL_PATH, "audittrail:/package")
        .build()
        .makeCurrent()) {

      assertThat(AuditBaggageBuilder.getCurrentDomainEntityId()).isEmpty();
    }
  }

  @Test
  void givenEmptyBuilder_whenBuild_expectBaggageMirrorsCurrent() {
    try (var scope = Baggage.current().toBuilder()
        .put("existing-key", "existing-value")
        .build()
        .makeCurrent()) {

      var baggage = AuditBaggageBuilder.newBuilder().build();

      assertThat(baggage.getEntryValue("existing-key")).isEqualTo("existing-value");
    }
  }
}