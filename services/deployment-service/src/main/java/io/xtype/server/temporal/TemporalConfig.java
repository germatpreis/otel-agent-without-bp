package io.xtype.server.temporal;

import io.temporal.client.WorkflowClientOptions;
import io.temporal.spring.boot.TemporalOptionsCustomizer;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TemporalConfig {

  @Bean
  TemporalOptionsCustomizer<WorkflowClientOptions.Builder> baggagePropagatorCustomizer() {
    return builder -> builder.setContextPropagators(List.of(new OtelBaggageContextPropagator()));
  }
}
