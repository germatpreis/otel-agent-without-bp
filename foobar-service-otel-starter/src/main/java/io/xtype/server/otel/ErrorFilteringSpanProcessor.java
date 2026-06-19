package io.xtype.server.otel;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.springframework.stereotype.Component;

@Component
public class ErrorFilteringSpanProcessor implements SpanProcessor {

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {

  }

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    // Filter logic - don't export filtered spans
    if (shouldFilterSpan(span)) {
      return;
    }
    // If we don't filter, the span will be exported by other processors in the chain

  }

  private boolean shouldFilterSpan(ReadableSpan span) {
    var spanName = span.getName().toLowerCase();

    // Filter out heartbeat errors
    if (spanName.contains("heartbeat")) {
      return true;
    }

    // Add more filtering logic as needed
    return false;
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}
