package io.xtype.server;

import io.opentelemetry.api.baggage.Baggage;
import io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
class PolicyCheckService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyCheckService.class);

  @Async("policyCheckExecutor")
  CompletableFuture<Void> executeCheck(String policyExecutionId) throws InterruptedException {
    var path = Baggage.current().getEntryValue(OtelSemanticConventions.AUDIT_TRAIL_PATH);
    LOGGER.info("Received audittrail path {}", path);

    var delaySeconds = ThreadLocalRandom.current().nextInt(1, 4);
    LOGGER.info("Policy check {} running on thread '{}', will complete in {}s",
        policyExecutionId, Thread.currentThread().getName(), delaySeconds);
    TimeUnit.SECONDS.sleep(delaySeconds);
    LOGGER.info("Policy check passed");
    return CompletableFuture.completedFuture(null);
  }
}
