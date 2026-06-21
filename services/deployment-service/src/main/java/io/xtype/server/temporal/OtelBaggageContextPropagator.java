package io.xtype.server.temporal;

import io.opentelemetry.api.baggage.Baggage;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.context.ContextPropagator;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import com.google.protobuf.ByteString;

/**
 * Bridges OTel baggage across Temporal's task-queue boundaries by serialising each
 * baggage entry into a Temporal message header and restoring it on the receiving thread.
 *
 * Register this as a Spring bean — the Temporal Spring Boot Starter picks up all
 * ContextPropagator beans and applies them to both the WorkflowClient and Worker options.
 */
public class OtelBaggageContextPropagator implements ContextPropagator {

  private static final String HEADER_PREFIX = "x-baggage-";

  @Override
  public String getName() {
    return "otel-baggage";
  }

  /** Called on the sending side to capture the current OTel context. */
  @Override
  public Object getCurrentContext() {
    return Baggage.current();
  }

  /**
   * Called on the receiving side (workflow/activity thread) to restore the context.
   * Note: the resulting scope is intentionally not closed here — Temporal owns the
   * thread lifecycle and overwrites thread-locals between tasks.
   */
  @Override
  public void setCurrentContext(Object context) {
    if (context instanceof Baggage baggage) {
      baggage.makeCurrent();
    }
  }

  /** Serialises baggage entries to Temporal Payload headers. */
  @Override
  public Map<String, Payload> serializeContext(Object context) {
    if (!(context instanceof Baggage baggage)) return Map.of();
    var result = new HashMap<String, Payload>();
    baggage.forEach((key, entry) ->
        result.put(HEADER_PREFIX + key, utf8Payload(entry.getValue())));
    return result;
  }

  /** Deserialises Temporal Payload headers back into a Baggage instance. */
  @Override
  public Object deserializeContext(Map<String, Payload> header) {
    var builder = Baggage.empty().toBuilder();
    header.forEach((key, payload) -> {
      if (key.startsWith(HEADER_PREFIX)) {
        builder.put(key.substring(HEADER_PREFIX.length()),
            payload.getData().toString(StandardCharsets.UTF_8));
      }
    });
    return builder.build();
  }

  private static Payload utf8Payload(String value) {
    return Payload.newBuilder()
        .setData(ByteString.copyFrom(value, StandardCharsets.UTF_8))
        .build();
  }
}
