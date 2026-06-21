package io.xtype.libraries.audittrail;

import static io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions.AUDIT_TRAIL_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.baggage.Baggage;
import org.junit.jupiter.api.Test;
import xtype.common.AuditContext;
import xtype.common.User;

class AuditContextBuilderTest {

  @Test
  void givenNoAuditTrailPathInContext_expectAuditTrailPathIsCreated() {
    var actual = AuditContextBuilder.forContext(createAuditContext("aaa")).build();

    assertThat(actual.getPath()).hasToString("audittrail:/package/aaa");
  }

  @Test
  void givenAuditTrailPathInContext_expectNewAuditTrailPathIsAppended() {
    var release = createAuditContext("release", "aaa");
    var auditTrailPath = AuditContextBuilder.forContext(release).build().getPath().toString();

    try (var scope = Baggage.current().toBuilder()
        .put(AUDIT_TRAIL_PATH, auditTrailPath)
        .build()
        .makeCurrent()) {

      var pkg = createAuditContext("package", "bbb");
      var actual = AuditContextBuilder.forContext(pkg).build();
      assertThat(actual.getPath()).hasToString("audittrail:/release/aaa/package/bbb");
    }
  }

  private AuditContext createAuditContext(String entityId) {
    return createAuditContext("package", entityId);
  }

  private AuditContext createAuditContext(String entityType, String entityId) {
    var context = new AuditContext();
    context.setEntityId(entityId);
    context.setEntityType(entityType);
    context.setActorType("USER");
    context.setActor(new User("foobar", "foobar"));
    context.setOperation("%s.action".formatted(entityType));
    context.setEntityName("%s '%s'".formatted(entityType, entityId));
    return context;
  }
}
