package com.example.server;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.stereotype.Service;

@Service
class FooService {

  @WithSpan("i-am-a-child-span")
  void explode() {
    throw new RuntimeException("kaboom");
  }

}
