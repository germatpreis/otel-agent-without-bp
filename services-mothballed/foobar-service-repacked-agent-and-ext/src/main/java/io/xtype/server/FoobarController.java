package io.xtype.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
class FoobarController {
  private static final Logger LOGGER = LoggerFactory.getLogger(FoobarController.class);

  @GetMapping
  public ResponseEntity<String> sayFoobar(){
    LOGGER.info("Received a foobar call!");
    return ResponseEntity.ok("foobar");
  }

}
