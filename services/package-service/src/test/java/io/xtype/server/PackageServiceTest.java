package io.xtype.server;

import static io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions.AUDIT_TRAIL_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.baggage.Baggage;
import io.xtype.springboot.kafka.producer.KafkaProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xtype.common.AuditContext;
import xtype.common.User;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

  @Mock
  private KafkaProducer<Object> kafkaProducer;

  @Test
  void givenNoAuditTrailPathInContext_expectAuditTrailPathIsCreated() {
    var sut = new PackageService(kafkaProducer);

    var actual = sut.buildAuditTrailPathFromAuditContext(createAuditContext("aaa"));

    assertThat(actual).isEqualTo("audittrail:/package/aaa");
  }

  @Test
  void givenAuditTrailPathInContext_expectNewAuditTrailPathIsAppended() {
    var sut = new PackageService(kafkaProducer);

    // given
    var release = createAuditContext("release", "aaa");
    var auditTrailPath = sut.buildAuditTrailPathFromAuditContext(release);

    try (var scope = Baggage.current().toBuilder()
        .put(AUDIT_TRAIL_PATH, auditTrailPath)
        .build()
        .makeCurrent()) {

      var pkg = createAuditContext("package", "bbb");
      var actual = sut.buildAuditTrailPathFromAuditContext(pkg);
      assertThat(actual).isEqualTo("audittrail:/release/aaa/package/bbb");
    }
  }

  private AuditContext createAuditContext(String entityId) {
    return createAuditContext("package", entityId);
  }

  private AuditContext createAuditContext(String entityType, String entityId) {
    var context = new AuditContext();
    context.setEntityId(entityId);
    context.setEntityType(entityType);
    context.setActor(new User("foobar", "foobar"));
    context.setOperation("%s.action".formatted(entityType));
    context.setEntityName("%s '%s'".formatted(entityType, entityId));
    return context;
  }



}