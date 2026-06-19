package io.xtype.server;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/packages")
public class PackageController {

  private final PackageService packageService;

  public PackageController(PackageService packageService) {
    this.packageService = packageService;
  }

  @PostMapping("/deploy")
  ResponseEntity<String> deployPackage(@Valid @RequestBody DeployPackageRequest request) {
    packageService.deployPackage(request);
    return ResponseEntity.ok().build();
  }

  public record DeployPackageRequest(
      @NotBlank String triggeredBy,
      @NotBlank String packageName,
      @NotNull UUID packageId,
      @NotEmpty @Valid List<ContentItem> content
  ) {

    public record ContentItem(
        @NotNull UUID uid,
        @NotBlank String type,
        @NotBlank String name
    ) {}
  }

}
