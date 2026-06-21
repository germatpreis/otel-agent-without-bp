package io.xtype.server.temporal;

import io.opentelemetry.api.baggage.Baggage;
import io.temporal.spring.boot.ActivityImpl;
import io.xtype.springboot.kafka.ApplicationConstants.OtelSemanticConventions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ActivityImpl(workers = "main")
public class PolicyCheckActivitiesImpl implements PolicyCheckActivities {
  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyCheckActivitiesImpl.class);

  private final RestClient restClient;

  public PolicyCheckActivitiesImpl(
      RestClient.Builder restClientBuilder,
      @Value("${services.policy-engine.url}") String policyEngineUrl
  ) {
    this.restClient = restClientBuilder.baseUrl(policyEngineUrl).build();
  }

  @Override
  public void checkPolicy(DeployContentItemContext context) {
    var path = Baggage.current().getEntryValue(OtelSemanticConventions.AUDIT_TRAIL_PATH);
    LOGGER.info("Received audittrail path {}", path);

    restClient.post()
        .uri("/policy-check")
        .body(context)
        .retrieve()
        .toBodilessEntity();
  }
}
