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

  private final FooService fooService;

  public FoobarController(FooService fooService) {
    this.fooService = fooService;
  }

  @GetMapping
  public ResponseEntity<String> sayFoobar(){
    LOGGER.info("Received a foobar call!");

    try {
      fooService.explode();
    } catch (Exception e) {
      /* suppress error in this span */
    }

    return ResponseEntity.ok("foobar");
  }

}
