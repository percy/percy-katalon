# Percy Katalon Plugin — Testing Guide

## Prerequisites

### Tools
- **Java 17** (Corretto recommended)
- **Gradle** (`brew install gradle`)
- **Node.js + npm** (for Percy CLI)
- **Katalon Studio** (free Community Edition, for manual tests)

### Environment Variables

```bash
export PERCY_TOKEN=<your-percy-project-token>
export BROWSERSTACK_USERNAME=<your-browserstack-username>
export BROWSERSTACK_ACCESS_KEY=<your-browserstack-access-key>
export JAVA_HOME=/path/to/java-17   # e.g. /Users/<you>/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home
```

### Install Percy CLI

```bash
npm install -g @percy/cli
```

---

## 1. Unit Tests (automated, no Katalon needed)

Runs 55 tests against a mock Percy CLI server. Covers all plugin features.

```bash
./gradlew test
```

### What's tested

| # | Test | Category |
|---|------|----------|
| 1 | `testPercySnapshotSendsRequest` | Snapshot happy path |
| 2 | `testSnapshotIncludesName` | Snapshot name in payload |
| 3 | `testSnapshotIncludesClientInfo` | percy-katalon attribution |
| 4 | `testSnapshotWithOptions` | Widths option pass-through |
| 5 | `testSnapshotWithAdvancedOptions` | percyCSS, enableJS, scope, labels, enableLayout |
| 6 | `testPercyInstanceCachedAcrossCalls` | Percy instance reused (no redundant healthcheck) |
| 7 | `testDriverChangeCreatesNewPercy` | New driver -> new Percy instance |
| 8 | `testNullDriverReturnsNull` | Null driver -> graceful null return |
| 9 | `testNoClassDefErrorCaughtGracefully` | Missing DriverFactory -> caught, no crash |
| 10 | `testPercyScreenshotBasic` | Screenshot in web mode -> graceful null |
| 11 | `testPercyScreenshotNullDriver` | Screenshot + null driver -> null |
| 12 | `testCreateRegionIgnore` | Ignore region via CSS selector |
| 13 | `testCreateRegionWithBoundingBox` | Bounding box + integer padding auto-expanded |
| 14 | `testCreateRegionStandardWithConfig` | Standard + diffSensitivity + imageIgnoreThreshold |
| 15 | `testCreateRegionIntelliignore` | Intelliignore + adsEnabled + bannersEnabled |
| 16 | `testCreateRegionWithAssertion` | diffIgnoreThreshold assertion |
| 17 | `testCreateRegionDefaultAlgorithm` | No algorithm -> defaults to "ignore" |
| 18 | `testCreateRegionPaddingAsMap` | Map-format padding {top,bottom,left,right} |
| 19 | `testCreateRegionNoConfigForIgnoreAlgorithm` | No configuration block for ignore algorithm |
| 20 | `testSnapshotWithRegions` | Regions included in snapshot POST body |
| 21 | `testSnapshotWithResponsiveCapture` | Responsive option doesn't crash plugin |
| 22 | `testSnapshotWithMinHeight` | minHeight option in POST body |
| 23 | `testDualChannelLoggingOnNullDriver` | Log sent to both Katalon + Percy CLI |
| 24 | `testSnapshotBodyContainsSpecificOptionValues` | percyCSS, domTransformation, scope, widths in body |
| 25 | `testSnapshotBodyContainsUrl` | Page URL included in snapshot payload |
| 26 | `testSnapshotBodyContainsEnvironmentInfo` | percy-java-selenium environment info in body |
| 27 | `testMultipleSnapshotsOnSamePageHaveDistinctNames` | Two snapshots on same page have correct names |
| 28 | `testSnapshotWithSyncOption` | sync option present in request body |
| 29 | `testSnapshotThrowsErrorForAutomateSession` | snapshot() on automate session -> error logged |
| 30 | `testScreenshotErrorMessageOnWebSession` | screenshot() on web session -> error logged |
| 31 | `testScreenshotAutomateSessionFailsGracefullyWithMockDriver` | screenshot() automate + mock driver -> graceful null |
| 32 | `testScreenshotWithOptionsAutomateSessionFailsGracefullyWithMockDriver` | screenshot() automate + options + mock driver -> graceful null |
| 33 | `testCreateRegionAllFields` | All region params in one call (selector, bbox, padding, config, assertion) |
| 34 | `testSnapshotWithDomTransformation` | domTransformation option passes through |
| 35 | `testScreenshotNullDriverLogsError` | Null driver screenshot logs to both channels |
| 36 | `testSnapshotWithDiscoveryOptions` | discovery.allowedHostnames passes through |
| 37 | `testSnapshotWithDisableShadowDom` | disableShadowDom key and value in body |
| 38 | `testSnapshotWithLabelsValue` | Exact labels string value in body |
| 39 | `testSnapshotWithEnableLayout` | enableLayout key in body |
| 40 | `testSnapshotWithCombinedOptions` | 6 options (widths, minHeight, percyCSS, scope, enableJS, labels) all in one body |
| 41 | `testSnapshotWithSyncFalse` | sync: false option passes through |
| 42 | `testSnapshotWithSyncAndOptions` | sync + widths + percyCSS combined in body |
| 43 | `testSnapshotSyncReturnsNonNull` | sync: true snapshot is sent to CLI |
| 44 | `testSnapshotAfterDomChange` | Modified DOM content appears in second snapshot body |
| 45 | `testSnapshotWithDifferentUrl` | New URL appears in body after navigation |
| 46 | `testSnapshotAfterNavigateBack` | 3 snapshots across URL changes, all correct |
| 47 | `testSnapshotWithFiveExplicitWidths` | All 5 widths (320,375,768,1024,1280) in body |
| 48 | `testSnapshotResponsiveCaptureInBody` | responsiveSnapshotCapture + widths doesn't crash |
| 49 | `testSnapshotDiscoveryCombinedWithOptions` | discovery + widths + percyCSS combined in body |
| 50 | `testSuccessfulSnapshotLogsToKatalon` | No error logged on success path |
| 51 | `testScreenshotWebModeLogsToBothChannels` | screenshot() web mode error in both Katalon + CLI logs |
| 52 | `testSnapshotBodyContainsXPathIgnoreRegion` | XPath region round-trip through snapshot body |
| 53 | `testSnapshotBodyContainsBoundingBoxRegion` | BoundingBox region round-trip through snapshot body |
| 54 | `testSnapshotBodyContainsIntelliignoreRegionConfig` | Intelliignore config (ads, banners) in snapshot body |
| 55 | `testSnapshotBodyContainsMultipleRegionsWithValues` | 3 regions with algorithms, diffSensitivity, diffIgnoreThreshold in body |

---

## 2. Build the Plugin JAR

```bash
./gradlew shadowJar
```

Output: `build/libs/percy-katalon-1.0.0.jar` (13MB shaded JAR)

### Verify JAR contents

```bash
# Should be EMPTY (no Selenium classes)
jar tf build/libs/percy-katalon-1.0.0.jar | grep "org/seleniumhq"

# Should show shaded packages
jar tf build/libs/percy-katalon-1.0.0.jar | grep "com/percy/katalon/shaded" | head -5

# Should show plugin classes
jar tf build/libs/percy-katalon-1.0.0.jar | grep "com/percy/katalon/" | grep -v shaded
```

### Copy JAR to Katalon test project

```bash
cp build/libs/percy-katalon-1.0.0.jar katalon-tests/Plugins/
```

---

## 3. Automated Smoke Test (no Katalon needed)

Runs 30 end-to-end tests using `curl` to POST snapshots directly to Percy CLI. Tests the same scenarios as the Katalon manual tests but without a real browser.

```bash
./scripts/run-all-tests.sh
```

### What's tested

| # | Test Group | Scenarios | What it validates |
|---|-----------|-----------|-------------------|
| 1 | Dual Channel Logging | 3 | Log POST, valid snapshot, error logging |
| 2 | Responsive Capture | 3 | Explicit widths, client-side, minHeight |
| 3 | Region Types | 8+8 | 8 baseline + 8 diff with regions (approve in between) |
| 4 | Snapshot Options | 7 | percyCSS, domTransformation, scope, JS+layout, labels, combined, disableShadowDom |
| 5 | Multiple Snapshots | 5 | Same page x2, DOM change, different URL, back navigation |
| 6 | Sync Mode | 2 | Basic sync, sync + options |
| 7 | Discovery Options | 2 | allowedHostnames, combined with widths + CSS |

### Expected output

```
Total: 30 passed, 0 failed
Diff build (verify regions on dashboard): https://percy.io/...
```

---

## 4. Manual Katalon Tests (interactive)

Tests the plugin inside real Katalon Studio with a real browser. The script manages Percy CLI lifecycle, runs unit tests, generates .tc metadata files, and guides you through the manual steps.

```bash
./scripts/run-manual-tests.sh
```

### Flow

```
+-----------------------------------------------------------+
| STEP 0: Script prompts for credentials (if not set)       |
| STEP 1: Build JAR, run 36 unit tests, generate .tc files  |
| STEP 2: You open katalon-tests/ project in Katalon        |
+-----------------------------------------------------------+
| PHASE 1: BASELINE (29 snapshots)                          |
|   Script starts Percy CLI                                 |
|   > You run 7 tests in Katalon:                           |
|     1. Dual Channel Logging   (3 scenarios, 1 snapshot)   |
|     2. Responsive Capture     (3 scenarios, 3 snapshots)  |
|     3. All Regions Test       (8 snapshots, baseline)     |
|     4. Snapshot Options       (7 snapshots)               |
|     5. Multiple Snapshots     (5 snapshots)               |
|     6. Sync Mode              (3 snapshots)               |
|     7. Discovery Options      (2 snapshots)               |
|   Script stops CLI                                        |
+-----------------------------------------------------------+
| PHASE 2: APPROVE                                          |
|   Script waits for rendering + approves baseline          |
|   > You verify 29 snapshots on Percy dashboard            |
+-----------------------------------------------------------+
| PHASE 3: DIFF                                             |
|   Script switches All Regions Test to diff mode           |
|   Script starts Percy CLI                                 |
|   > You run All Regions Test in Katalon                   |
|   Script stops CLI + resets to baseline mode              |
|   > You verify 8 region snapshots on Percy dashboard      |
+-----------------------------------------------------------+
| SUMMARY: 8 Katalon runs, 39 scenarios total               |
+-----------------------------------------------------------+
```

### Katalon test cases

Located at: `katalon-tests/`

Open in Katalon Studio: File > Open Project > select `katalon-tests` folder.

| Test Case | Scenarios | What it tests |
|-----------|-----------|---------------|
| **Dual Channel Logging** | 3 | Null driver (null return), valid snapshot, screenshot in wrong mode (null return) |
| **Responsive Capture** | 3 | Explicit widths [320,375,768,1024,1280], client-side responsive capture, minHeight 1200 |
| **All Regions Test** | 8 | Baseline mode: 8 clean snapshots. Diff mode: 8 snapshots with visual changes + regions |
| **Snapshot Options** | 7 | percyCSS (purple bg), domTransformation (title changed), scope (div only), JS+layout, labels, combined, disableShadowDom |
| **Multiple Snapshots** | 5 | Same page x2, after DOM change (blue title), second page, navigate back |
| **Sync Mode** | 3 | Non-blocking (sync: false), with options (widths+CSS), blocking (sync: true, may take 1-2 min) |
| **Discovery Options** | 2 | allowedHostnames, combined with widths + CSS |

### What to verify on Percy Dashboard

#### Non-region snapshots

| Snapshot | What to Check |
|----------|---------------|
| `Log - Valid Snapshot` | Exists. Only 1 snapshot from Dual Channel Logging (null driver and wrong mode produce nothing) |
| `Responsive - Explicit Widths` | 5 widths in width selector (320, 375, 768, 1024, 1280) |
| `Responsive - Client Side` | 3 widths in width selector (375, 768, 1280) |
| `Responsive - MinHeight` | Screenshot visibly taller than normal (1200px min) |
| `Options - percyCSS` | Purple background, white h1 |
| `Options - domTransformation` | h1 reads "Transformed Title" (not "Example Domain") |
| `Options - scope` | Only `<div>` content captured, not full page |
| `Options - JS and Layout` | Snapshot present, no errors |
| `Options - labels` | Labels smoke,regression,options-test in metadata |
| `Options - Combined` | 3 widths (768, 992, 1200), header hidden |
| `Options - disableShadowDom` | Snapshot present, no errors |
| `Multi - Same Page First` | Original example.com |
| `Multi - Same Page Second` | Identical to first (proves same-page multi-snapshot works) |
| `Multi - After DOM Change` | Blue "Modified Title" |
| `Multi - Second Page` | Exists (navigation between snapshots works) |
| `Multi - Back to First Page` | Original page (back-navigation works) |
| `Sync - Non Blocking` | Snapshot present |
| `Sync - With Options` | 2 widths (375, 1280) |
| `Sync - Blocking` | Snapshot present (confirms sync completed) |
| `Discovery - Allowed Hostnames` | Snapshot present, no errors |
| `Discovery - Combined` | 2 widths (375, 1280) |

#### Region snapshots (diff build only)

| Snapshot | Region | Expected |
|----------|--------|----------|
| Region - Ignore CSS | `elementCSS: 'h1'`, ignore | h1 changes NOT shown as diff |
| Region - Ignore XPath | `elementXpath: '//h1'`, ignore | h1 changes NOT shown as diff |
| Region - BoundingBox | `boundingBox: {0,0,800,80}`, ignore | Top 80px NOT shown as diff |
| Region - Standard | `elementCSS: 'p'`, standard, sensitivity 2 | p changes SHOWN as diff |
| Region - Intelliignore | `elementCSS: 'p'`, intelliignore | p changes likely SHOWN (AI decides) |
| Region - Padding Integer | `elementCSS: 'h1'`, ignore, padding 10 | h1 + 10px all sides NOT shown |
| Region - Padding Map | `elementCSS: 'h1'`, ignore, padding {5,10,15,20} | h1 + asymmetric padding NOT shown |
| Region - Multiple Combined | ignore(h1) + standard(p) + ignore(a) | h1 ignored, p detected, link ignored |

---

## Quick Reference

| Command | What | Time |
|---------|------|------|
| `./gradlew test` | 55 unit tests | ~10s |
| `./gradlew shadowJar` | Build plugin JAR | ~5s |
| `./scripts/run-all-tests.sh` | 30 automated smoke tests | ~3min |
| `./scripts/run-manual-tests.sh` | Interactive Katalon test guide (39 scenarios) | ~10min |

---

## Troubleshooting

### Port 5338 already in use

```bash
lsof -ti:5338 | xargs kill -9
rm -f ~/.percy/agent-5338.lock
```

### Gradle uses wrong Java version

```bash
# Check
gradle --version | grep JVM

# Fix: set JAVA_HOME to Java 17
export JAVA_HOME=/path/to/java-17
```

### Gradle wrapper JAR missing

```bash
gradle wrapper
```

The `run-manual-tests.sh` script does this automatically.

### Katalon can't find test cases

- Ensure `.tc` files exist in `katalon-tests/Test Cases/` (the manual test script generates these automatically)
- Quit and reopen Katalon Studio
- File > Open Project > select the `katalon-tests` folder (not the .prj file)

### Regions not visible on Percy dashboard

Regions only appear on **diff builds** (not baseline/new snapshots). You need:
1. An approved baseline build
2. A second build with same snapshot names + visual changes + regions

### Sync mode hangs

`sync: true` blocks until Percy's servers finish rendering, which can take 1-2 minutes. If it hangs longer:
- Ctrl+C and check the Percy dashboard — the snapshot may still appear
- Update Percy CLI: `npm i -g @percy/cli@latest`
- Use `sync: false` to test the option passes through without blocking
