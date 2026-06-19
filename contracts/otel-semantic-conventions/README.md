# XType OpenTelemetry Semantic Conventions

Canonical OpenTelemetry Semantic Conventions used by xtype — attributes, events, and metrics for telemetry across all platforms.

## Directory Structure

- `model/xtype/`: YAML files defining the semantic conventions for different domains
    - `cloning_metrics.yaml`: Metrics related to cloning operations
    - `common_registry.yaml`: Common attributes used across all domains
    - `ingestion_registry.yaml`: Conventions for data ingestion
    - `snow_registry.yaml`: ServiceNow-specific conventions
    - `workflow_registry.yaml`: Workflow-related conventions
    - `xus_registry.yaml`: XType User Service conventions

- `templates/`: Weaver templates used by the code generators

## Updating the Models

Edit YAML files under `model/xtype/`. VSCode has JSON schema support and gives inline validation.

## Regenerating Artifacts

The Java and Node codegen run **automatically** via Nx / Maven whenever the model or template files change — no manual step required. Both emit into gitignored directories and are consumed transparently by downstream modules.

- Java (`xtype-otel-semantic-conventions` + `opentelemetry-javaagent-xtype-ext`) — `exec-maven-plugin` wires `./generate.sh` into the Maven `generate-sources` phase.
- Node (`@xtype/lib-opentelemetry`) — the `codegen` Nx target runs `./generate-semantic-conventions.sh` before `build`.

### Markdown docs — stays manual

Documentation under `doc/otel-semantic-conventions/markdown/` is **intentionally kept manual**. Weaver's `update-markdown` command rewrites only the sections between its markers and preserves everything else, so those files are mixed human + generated content and live in git. After editing models, regenerate and commit:

```bash
doc/otel-semantic-conventions/generate.sh
```

Docker is required for all three generators — they invoke `otel/weaver` in a container.

## Further Documentation

[XType Semantic Conventions Wiki](https://xtypeio.atlassian.net/wiki/spaces/XTYP/pages/2395373572/XType+Semantic+Conventions).
