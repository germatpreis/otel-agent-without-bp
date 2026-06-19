package io.xtype.server.temporal;

import io.temporal.spring.boot.ActivityImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ActivityImpl(workers = "main")
public class PolicyCheckActivitiesImpl implements PolicyCheckActivities {

  private final RestClient restClient;

  public PolicyCheckActivitiesImpl(
      RestClient.Builder restClientBuilder,
      @Value("${services.policy-engine.url}") String policyEngineUrl
  ) {
    this.restClient = restClientBuilder.baseUrl(policyEngineUrl).build();
  }

  @Override
  public void checkPolicy(DeployContentItemContext context) {
    restClient.post()
        .uri("/policy-check")
        .body(context)
        .retrieve()
        .toBodilessEntity();
  }
}
