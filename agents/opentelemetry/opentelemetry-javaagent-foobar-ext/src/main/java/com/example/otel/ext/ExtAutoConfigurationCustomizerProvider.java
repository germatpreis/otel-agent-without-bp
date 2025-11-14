package com.example.otel.ext;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class ExtAutoConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addTracerProviderCustomizer(this::addTraceProviderCustomizer);
    autoConfiguration.addLoggerProviderCustomizer(this::addLoggerProviderCustomizer);
    autoConfiguration.addSpanExporterCustomizer(this::addSpanExporterCustomizer);
  }

  private SpanExporter addSpanExporterCustomizer(SpanExporter spanExporter, ConfigProperties configProperties) {
    return new FilteringSpanExporter(spanExporter);
  }

  private SdkTracerProviderBuilder addTraceProviderCustomizer(
      SdkTracerProviderBuilder sdkTracerProviderBuilder, ConfigProperties configProperties) {
    return sdkTracerProviderBuilder.addSpanProcessor(new BaggageSpanProcessor());
  }

  private SdkLoggerProviderBuilder addLoggerProviderCustomizer(
      SdkLoggerProviderBuilder sdkLoggerProviderBuilder, ConfigProperties configProperties) {
    return sdkLoggerProviderBuilder.addLogRecordProcessor(new BaggageLogProcessor());
  }
}
