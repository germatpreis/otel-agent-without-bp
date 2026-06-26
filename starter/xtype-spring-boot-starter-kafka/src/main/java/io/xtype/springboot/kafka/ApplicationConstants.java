package io.xtype.springboot.kafka;

public class ApplicationConstants {

  public static class Topics {
    // xtype.ingestion.snow_data.event.internal.v1
    public static final String TOPIC_DATA = "xtype-ingestion-snow_data-event-internal-v1";
    // xtype.package.event.internal.v1
    public static final String TOPIC_PACKAGE = "xtype-package-event-internal-v1";
    // xtype.audit.event.external.v1
    public static final String TOPIC_AUDIT = "xtype-audit-event-external-v1";
  }

  public static class OtelSemanticConventions {
    public static final String AUDIT_TRAIL_PATH = "x-at-path";
  }
}
