package com.example.otel.ext;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class BaggageSpanProcessor implements SpanProcessor {

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    span.setAttribute("cats.are.cool", true);
    var map = Baggage.fromContext(parentContext).asMap();
    if (map != null) {
      map.forEach((key, value) -> span.setAttribute(key, value.getValue()));
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
