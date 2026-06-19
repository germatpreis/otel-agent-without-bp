package io.xtype.otel.ext;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;

public class FilteringSpanExporter implements SpanExporter {
  private final SpanExporter delegate;

  public FilteringSpanExporter(SpanExporter delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    Collection<SpanData> filtered = spans.stream()
        .filter(span -> !shouldFilterSpan(span))
        .toList();

    if (filtered.isEmpty()) {
      return CompletableResultCode.ofSuccess();
    }

    return delegate.export(filtered);
  }

  private boolean shouldFilterSpan(SpanData span) {
    var spanName = span.getName().toLowerCase();
    var status = span.getStatus().getStatusCode();

    var result = status == StatusCode.ERROR && spanName.contains("i-am-a-child-span");
    System.out.println(">>> " + result);
    return result;
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
