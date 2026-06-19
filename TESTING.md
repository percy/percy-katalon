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

Runs 23 tests against a mock Percy CLI server. Covers all plugin features.

```bash
cd /Users/sudiptasen/project/percy-katalon
gradle clean test
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
| 7 | `testDriverChangeCreatesNewPercy` | New driver → new Percy instance |
| 8 | `testNullDriverReturnsNull` | Null driver → graceful null return |
| 9 | `testNoClassDefErrorCaughtGracefully` | Missing DriverFactory → caught, no crash |
| 10 | `testPercyScreenshotBasic` | Screenshot in web mode → graceful null |
| 11 | `testPercyScreenshotNullDriver` | Screenshot + null driver → null |
| 12 | `testCreateRegionIgnore` | Ignore region via CSS selector |
| 13 | `testCreateRegionWithBoundingBox` | Bounding box + integer padding auto-expanded |
| 14 | `testCreateRegionStandardWithConfig` | Standard + diffSensitivity + imageIgnoreThreshold |
| 15 | `testCreateRegionIntelliignore` | Intelliignore + adsEnabled + bannersEnabled |
| 16 | `testCreateRegionWithAssertion` | diffIgnoreThreshold assertion |
| 17 | `testCreateRegionDefaultAlgorithm` | No algorithm → defaults to "ignore" |
| 18 | `testCreateRegionPaddingAsMap` | Map-format padding {top,bottom,left,right} |
| 19 | `testCreateRegionNoConfigForIgnoreAlgorithm` | No configuration block for ignore algorithm |
| 20 | `testSnapshotWithRegions` | Regions included in snapshot POST body |
| 21 | `testSnapshotWithResponsiveCapture` | Responsive option doesn't crash plugin |
| 22 | `testSnapshotWithMinHeight` | minHeight option in POST body |
| 23 | `testDualChannelLoggingOnNullDriver` | Log sent to both Katalon + Percy CLI |

---

## 2. Build the Plugin JAR

```bash
gradle shadowJar
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

Runs 14 end-to-end tests using `curl` to POST snapshots directly to Percy CLI. Tests the same scenarios as the Katalon manual tests but without a real browser.

```bash
./scripts/run-all-tests.sh
```

### What's tested

| # | Test | What it validates |
|---|------|-------------------|
| 1 | Logging: null driver | Log POST to /percy/log without crash |
| 2 | Logging: valid snapshot | Snapshot accepted by Percy CLI |
| 3 | Logging: wrong mode | Error logged gracefully |
| 4 | Responsive: explicit widths | Snapshot with width options accepted |
| 5 | Responsive: client side | Responsive snapshot accepted |
| 6 | Responsive: minHeight | MinHeight snapshot accepted |
| 7 | Region: Ignore CSS | Ignore region via elementCSS |
| 8 | Region: Ignore XPath | Ignore region via elementXpath |
| 9 | Region: BoundingBox | Ignore region via bounding box |
| 10 | Region: Standard | Standard algorithm + diffSensitivity |
| 11 | Region: Intelliignore | Intelliignore + adsEnabled + bannersEnabled |
| 12 | Region: Padding Integer | Padding as object {top,bottom,left,right} |
| 13 | Region: Padding Map | Asymmetric per-side padding |
| 14 | Region: Multiple Combined | 3 regions on one snapshot |

### Expected output

```
Total: 14 passed, 0 failed
Diff build (verify regions on dashboard): https://percy.io/...
```

---

## 4. Manual Katalon Tests (interactive)

Tests the plugin inside real Katalon Studio with a real browser. The script manages Percy CLI lifecycle; you run the tests manually in Katalon Studio when prompted.

```bash
./scripts/run-manual-tests.sh
```

### Flow

```
┌─────────────────────────────────────────────────────────┐
│ STEP 0: Script prompts for credentials (if not set)     │
│ STEP 1: Script builds JAR and copies to katalon-tests/  │
│ STEP 2: You open katalon-tests/ project in Katalon      │
├─────────────────────────────────────────────────────────┤
│ PHASE 1: BASELINE                                       │
│   Script starts Percy CLI                               │
│   ▶ You run 3 tests in Katalon:                         │
│     1. Dual Channel Logging                             │
│     2. Responsive Capture                               │
│     3. All Regions Test                                 │
│   Script stops CLI                                      │
├─────────────────────────────────────────────────────────┤
│ PHASE 2: APPROVE                                        │
│   Script waits for rendering + approves baseline        │
│   ▶ You verify 12 snapshots on Percy dashboard          │
├─────────────────────────────────────────────────────────┤
│ PHASE 3: DIFF                                           │
│   Script switches All Regions Test to diff mode         │
│   Script starts Percy CLI                               │
│   ▶ You run All Regions Test in Katalon                 │
│   Script stops CLI + resets to baseline mode             │
│   ▶ You verify 8 region snapshots on Percy dashboard    │
├─────────────────────────────────────────────────────────┤
│ SUMMARY: 4 Katalon runs, 22 scenarios total             │
└─────────────────────────────────────────────────────────┘
```

### Katalon test cases

Located at: `katalon-tests/`

Open in Katalon Studio: File > Open Project > select `katalon-tests` folder.

| Test Case | Scenarios | What it tests |
|-----------|-----------|---------------|
| **Dual Channel Logging** | 3 | Null driver (null return), valid snapshot, screenshot in wrong mode (null return) |
| **Responsive Capture** | 3 | Explicit widths [320,375,768,1024,1280], client-side responsive capture, minHeight 1200 |
| **All Regions Test** | 8 | Baseline mode: 8 clean snapshots. Diff mode: 8 snapshots with visual changes + regions |

### What to verify on Percy Dashboard

After the diff build, open the URL printed by the script and check:

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
| `gradle test` | 23 unit tests | ~10s |
| `gradle shadowJar` | Build plugin JAR | ~5s |
| `./scripts/run-all-tests.sh` | 14 automated smoke tests | ~3min |
| `./scripts/run-manual-tests.sh` | Interactive Katalon test guide | ~5min |

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

### Katalon can't find test cases

- Quit and reopen Katalon Studio
- File > Open Project > select the `katalon-tests` folder (not the .prj file)

### Regions not visible on Percy dashboard

Regions only appear on **diff builds** (not baseline/new snapshots). You need:
1. An approved baseline build
2. A second build with same snapshot names + visual changes + regions
