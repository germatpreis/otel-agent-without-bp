package io.xtype.server.config;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collection;
import java.util.stream.Collectors;

@Configuration
public class OpenTelemetryConfig {

  @Bean
  @Primary
  @ConditionalOnBean(SpanExporter.class)
  public SpanExporter filteringSpanExporter(SpanExporter spanExporter) {
    return new FilteringSpanExporter(spanExporter);
  }

  static class FilteringSpanExporter implements SpanExporter {

    private final SpanExporter delegate;

    FilteringSpanExporter(SpanExporter delegate) {
      this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
      Collection<SpanData> filtered = spans.stream()
          .filter(span -> !shouldFilterSpan(span))
          .collect(Collectors.toList());

      if (filtered.isEmpty()) {
        return CompletableResultCode.ofSuccess();
      }

      return delegate.export(filtered);
    }

    private boolean shouldFilterSpan(SpanData span) {
      String spanName = span.getName().toLowerCase();
      StatusCode status = span.getStatus().getStatusCode();

      // Filter heartbeat errors
      return status == StatusCode.ERROR && spanName.contains("heartbeat");
    }

    @Override
    public CompletableResultCode flush() {
      return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
      return delegate.shutdown();
    }
  }
}