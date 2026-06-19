# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Percy visual testing plugin for Katalon Studio. It wraps the `percy-java-selenium` SDK and exposes Katalon `@Keyword` methods (`percySnapshot`, `percyScreenshot`, `createRegion`) so Katalon test scripts can capture DOM snapshots and send them to Percy CLI.

## Build & Test Commands

```bash
# Build the fat JAR (output: build/libs/percy-katalon-<version>.jar)
./gradlew shadowJar

# Run unit tests
./gradlew test

# Integration test (requires PERCY_TOKEN and optionally katalonc)
./scripts/run-integration-test.sh
```

## Architecture

**Gradle + Groovy plugin project** targeting Java 8. Uses the Shadow plugin to produce a fat JAR that bundles `percy-java-selenium` with relocated transitive deps, while excluding Selenium and Katalon classes (provided at runtime by Katalon Studio).

### Source layout (non-standard)

- `Keywords/` — main Groovy source (`PercyKeywords`, `Version`). This is the Katalon convention for keyword classes.
- `stubs/` — minimal Java stubs for Katalon APIs (`DriverFactory`, `KeywordUtil`, `@Keyword`). Compile-only; excluded from the fat JAR. These exist because Katalon's real JARs aren't published to Maven.
- `src/test/groovy/` — JUnit 5 tests with Mockito. Tests spin up a local `HttpServer` on port 5339 that simulates Percy CLI endpoints (`/percy/healthcheck`, `/percy/dom.js`, `/percy/snapshot`, `/percy/automateScreenshot`, `/percy/log`).
- `katalon-plugin.json` — declares the keyword class for Katalon's plugin loader.

### Key design decisions

- **Percy instance caching**: `PercyKeywords` caches a `Percy` instance keyed by WebDriver identity to avoid repeated healthcheck HTTP calls. Call `resetCache()` in tests.
- **Dual-channel logging**: errors log to both Katalon (`KeywordUtil.logInfo`) and Percy CLI (`POST /percy/log`), so issues appear in both consoles.
- **Shadow JAR relocations**: Apache HTTP, Commons Logging/Codec, and org.json are relocated under `com.percy.katalon.shaded.*` to avoid classpath conflicts with Katalon's bundled versions.
- **Test server port**: configured via `test.percy.port` system property (default 5339) and `PERCY_SERVER_ADDRESS` env var, both set in `build.gradle`.
