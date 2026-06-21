package io.xtype.server;

import io.opentelemetry.api.baggage.Baggage;
import io.xtype.libraries.audittrail.AuditBaggageBuilder;
import io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/policy-check")
class PolicyCheckController {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyCheckController.class);

  private final PolicyCheckService policyCheckService;

  PolicyCheckController(PolicyCheckService policyCheckService) {
    this.policyCheckService = policyCheckService;
  }

  @PostMapping
  public ResponseEntity<Void> checkPolicy() throws InterruptedException, ExecutionException {
    var path = Baggage.current().getEntryValue(OtelSemanticConventions.AUDIT_TRAIL_PATH);
    LOGGER.info("Received audittrail path {}", path);

    var policyExecutionId = UUID.randomUUID().toString();

    // prepare audit context information needed to propagate (entity type + id)
    var auditBaggage = AuditBaggageBuilder
        .newBuilder()
        .auditDomainEntity("policy", policyExecutionId);

    // attach the audittrail://package/<packageId>/updateset/<updateSetUid>/policy/<policyExecutionId> to the baggage for the next call
    // all following requests (regardless of the transport mechanism) will have this baggage set
    try (var scope = auditBaggage.build().makeCurrent()) {
      policyCheckService.executeCheck(policyExecutionId).get();
    }

    return ResponseEntity.ok().build();
  }
}
