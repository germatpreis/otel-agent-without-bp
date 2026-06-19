package io.xtype.otel.ext;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;

public class BaggageLogProcessor implements LogRecordProcessor {

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    Baggage baggage = Baggage.fromContext(context);

    baggage.forEach((key, value) -> logRecord.setAttribute(AttributeKey.stringKey("baggage." + key), value.getValue()));
  }

  @Override
  public CompletableResultCode shutdown() {
    return LogRecordProcessor.super.shutdown();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return LogRecordProcessor.super.forceFlush();
  }

  @Override
  public void close() {
    LogRecordProcessor.super.close();
  }
}