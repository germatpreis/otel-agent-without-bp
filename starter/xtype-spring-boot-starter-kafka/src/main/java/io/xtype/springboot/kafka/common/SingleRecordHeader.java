package io.xtype.springboot.kafka.common;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.Header;

public class SingleRecordHeader implements Header {
  private final String key;
  private final byte[] value;

  public SingleRecordHeader(String key, byte[] value) {
    this.key = key;
    this.value = value;
  }

  public SingleRecordHeader(String key, String value) {
    this.key = key;
    this.value = value.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public byte[] value() {
    return value;
  }
}
