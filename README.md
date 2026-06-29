# percy-katalon

[![Percy](https://img.shields.io/badge/visual%20testing-Percy-8b5cf6)](https://percy.io)
[![Katalon](https://img.shields.io/badge/test%20automation-Katalon%20Studio-00b894)](https://katalon.com)

Percy visual testing plugin for [Katalon Studio](https://katalon.com). Capture DOM snapshots and BrowserStack Automate screenshots from your Katalon tests, rendered across all Percy-supported browsers.

---

## Claude Code Skills

If you're using [Claude Code](https://claude.ai/code) in this repo, interactive guides are available as slash commands:

| Command | Description |
|---------|-------------|
| `/katalon-install` | Step-by-step plugin installation |
| `/katalon-write-tests` | Snapshots, options, regions, BDD/Cucumber |
| `/katalon-run-tests` | Run tests, review results, troubleshoot |
| `/katalon-architecture` | Architecture, browsers, compatibility, SDK comparison |
| `/katalon-cicd` | GitHub Actions, Jenkins, Buildkite setup |

---

## Table of Contents

- [Installation](#installation)
- [Writing Tests](#writing-tests)
- [Running Tests & Reviewing Results](#running-tests--reviewing-results)
- [Architecture](#architecture)
- [Browser Support](#browser-support)
- [Compatibility](#compatibility)
- [CI/CD Integration](#cicd-integration)
- [Comparison with Other Percy SDKs](#comparison-with-other-percy-sdks)
- [Troubleshooting](#troubleshooting)
- [Development](#development)

---

## Installation

### Prerequisites

| Tool | Install Command | Purpose |
|------|----------------|---------|
| **Node.js** | [nodejs.org](https://nodejs.org) | Required for Percy CLI |
| **Percy CLI** | `npm install -g @percy/cli` | Orchestrates builds and uploads |
| **Katalon Studio** | [katalon.com/download](https://katalon.com/download) | Test automation IDE (free Community Edition works) |

### Step 1: Get the Plugin JAR

**Option A: Download release**
```bash
# Download the latest percy-katalon-<version>.jar from GitHub Releases
# https://github.com/percy/percy-katalon/releases
```

**Option B: Build from source**
```bash
git clone https://github.com/percy/percy-katalon.git
cd percy-katalon
export JAVA_HOME=/path/to/java-17
gradle shadowJar
# Output: build/libs/percy-katalon-1.0.0.jar
```

### Step 2: Install in Your Katalon Project

```bash
cp percy-katalon-1.0.0.jar /path/to/your-katalon-project/Plugins/
```

### Step 3: Restart Katalon Studio

After restarting, Percy keywords appear under the **"Percy"** category in the keyword browser.

### Step 4: Get Your Percy Token

1. Go to [percy.io](https://percy.io) and create a project
2. Copy the `PERCY_TOKEN` from project settings
3. Set it in your terminal:
   ```bash
   export PERCY_TOKEN=<your-token>
   ```

---

## Writing Tests

### Basic Snapshot

Add a Percy snapshot to any Katalon test script:

```groovy
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

WebUI.openBrowser('https://your-app.com')
WebUI.delay(2)

// Capture a Percy snapshot
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Homepage')

WebUI.closeBrowser()
```

### Snapshot with Options

```groovy
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Homepage', [
    widths: [375, 768, 1280],       // render at these viewport widths
    minHeight: 1024,                  // minimum snapshot height
    percyCSS: 'header { display: none; }',  // hide header in Percy only
    enableJavaScript: true,           // enable JS in Percy's rendering
    responsiveSnapshotCapture: true   // capture DOM at each width client-side
])
```

### Snapshot with Regions

Control how Percy compares specific areas of the page:

```groovy
// Ignore a dynamic ad banner (changes won't be flagged)
def ignoreAd = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: '.ad-banner',
    algorithm: 'ignore'
])

// Monitor main content with sensitivity control
def monitorContent = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: '.main-content',
    algorithm: 'standard',
    diffSensitivity: 2       // 0-4, higher = more sensitive
])

// Ignore header with 10px padding around it
def ignoreHeader = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: 'header',
    algorithm: 'ignore',
    padding: 10               // auto-expands to {top:10, bottom:10, left:10, right:10}
])

// Use AI to detect ad/banner noise vs real changes
def smartRegion = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: '.sidebar',
    algorithm: 'intelliignore',
    adsEnabled: true,
    bannersEnabled: true
])

CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Homepage', [
    regions: [ignoreAd, monitorContent, ignoreHeader, smartRegion]
])
```

### BrowserStack Automate Screenshot

For tests running on BrowserStack Automate (not local browsers):

```groovy
CustomKeywords.'com.percy.katalon.PercyKeywords.percyScreenshot'('Homepage')

// With options
CustomKeywords.'com.percy.katalon.PercyKeywords.percyScreenshot'('Homepage', [
    fullPage: true,
    percyCSS: 'footer { display: none; }',
    ignoreRegionSelectors: ['.dynamic-content']
])
```

### BDD/Cucumber Tests

Percy provides Cucumber step definitions for Katalon's BDD support:

```gherkin
Feature: Visual Regression Testing

  Scenario: Homepage looks correct
    Given I have a Percy instance
    And I create a Percy ignore region with CSS selector ".ad-banner"
    And I create a Percy consider region with CSS selector ".hero" and diff sensitivity 2
    When I take a Percy snapshot named "Homepage" with regions
    Then Percy should be enabled

  Scenario: Responsive check
    When I take a Percy snapshot named "Homepage" with widths "375,768,1280"

  Scenario: Scoped snapshot
    When I take a Percy snapshot named "Footer" with scope "footer"
```

Setup in your Cucumber `@Before`/`@After` hooks:
```groovy
import com.percy.katalon.PercyCucumberSteps

@Before
def setUp() {
    PercyCucumberSteps.init()
}

@After
def tearDown() {
    PercyCucumberSteps.cleanup()
}
```

### All Snapshot Options

| Option | Type | Description |
|--------|------|-------------|
| `widths` | `List<Integer>` | Viewport widths for rendering |
| `minHeight` | `Integer` | Minimum snapshot height |
| `percyCSS` | `String` | Custom CSS injected in Percy's rendering only |
| `scope` | `String` | CSS selector to limit snapshot area |
| `enableJavaScript` | `Boolean` | Enable JS in Percy rendering |
| `responsiveSnapshotCapture` | `Boolean` | Capture DOM at each width client-side |
| `sync` | `Boolean` | Wait for processing to complete |
| `enableLayout` | `Boolean` | Layout comparison mode |
| `disableShadowDom` | `Boolean` | Skip Shadow DOM serialization |
| `labels` | `String` | Comma-separated labels |
| `testCase` | `String` | Test case identifier |
| `domTransformation` | `String` | JS function to transform DOM |
| `regions` | `List<Map>` | Region configs (use `createRegion()`) |
| `readiness` | `Map` | Readiness gate config |

### All Region Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `elementCSS` | `String` | CSS selector |
| `elementXpath` | `String` | XPath selector |
| `boundingBox` | `Map` | `{x, y, width, height}` |
| `algorithm` | `String` | `ignore`, `standard`, `layout`, `intelliignore` |
| `padding` | `Integer` or `Map` | Integer = all sides; Map = `{top, bottom, left, right}` |
| `diffSensitivity` | `Integer` | 0-4 (standard/intelliignore only) |
| `imageIgnoreThreshold` | `Number` | Image threshold |
| `carouselsEnabled` | `Boolean` | Detect carousels (intelliignore) |
| `bannersEnabled` | `Boolean` | Detect banners (intelliignore) |
| `adsEnabled` | `Boolean` | Detect ads (intelliignore) |
| `diffIgnoreThreshold` | `Number` | Assertion threshold |

---

## Running Tests & Reviewing Results

### Local Development (Katalon Studio IDE)

```bash
# Terminal 1: Start Percy CLI
export PERCY_TOKEN=<your-token>
npx @percy/cli exec:start

# Katalon Studio: Run your test case (Chrome, Firefox, or Edge)
# The plugin automatically sends snapshots to Percy CLI

# Terminal 1: Stop Percy CLI (finalizes the build)
npx @percy/cli exec:stop

# Open the build URL printed in terminal to review results
```

### CI/CD (Katalon Runtime Engine)

```bash
export PERCY_TOKEN=<your-token>
npx @percy/cli exec -- katalonc \
  -noSplash -runMode=console \
  -projectPath="$(pwd)/your-project.prj" \
  -testSuitePath="Test Suites/YourSuite" \
  -browserType="Chrome (headless)" \
  -apiKey="$KATALON_API_KEY"
```

### Without KRE License (PERCY_SERVER_ADDRESS fallback)

```bash
# Terminal 1
PERCY_TOKEN=<token> npx @percy/cli exec:start

# Terminal 2 or Katalon Studio IDE
export PERCY_SERVER_ADDRESS=http://localhost:5338
# Run your tests

# Terminal 1
npx @percy/cli exec:stop
```

### Reviewing Results on Percy Dashboard

After stopping Percy CLI, your build appears on the Percy dashboard:

1. **New snapshots** (first build) — displayed as baseline, no diffs
2. **Changed snapshots** (subsequent builds) — visual diffs highlighted
3. **Regions** — visible as overlays on the diff view:
   - Ignore regions: changes within are not flagged
   - Standard regions: changes are highlighted with configured sensitivity
   - Intelliignore regions: AI determines if changes are noise or real
4. **Approve/Reject** — review each snapshot and approve to set as new baseline

### Graceful Degradation

The plugin **never fails your test**. If Percy is unavailable:

| Scenario | Behavior |
|----------|----------|
| Percy CLI not running | Logs warning, returns null, test continues |
| CLI returns error | Logs error, returns null, test continues |
| No active browser | Logs warning, returns null, test continues |
| Plugin loaded outside Katalon | Caught, returns null, no crash |

---

## Architecture

```
Katalon Test Script
    │
    ▼
percy-katalon Plugin (.jar)
    │  • Gets WebDriver from DriverFactory
    │  • Caches Percy instance (one healthcheck per session)
    │  • Graceful degradation: never fails the test
    ▼
percy-java-selenium SDK (bundled)
    │  • Serializes DOM via PercyDOM.serialize()
    │  • POST /percy/snapshot to Percy CLI
    ▼
Percy CLI (localhost:5338)
    │  • Uploads DOM to Percy API
    ▼
Percy Rendering Infrastructure
    │  • Renders DOM on all configured browsers
    │  • Compares against approved baseline
    ▼
Percy Web Dashboard
    • Visual diffs, region overlays, approve/reject
```

---

## Browser Support

All Percy-supported browsers work automatically — no browser-specific code needed:

| Browser | Platform |
|---------|----------|
| Chrome | Linux |
| Firefox | Linux |
| Edge | Windows |
| Safari | macOS |
| Safari on iPhone | iOS |
| Chrome on Android | Android |

Browser configuration is managed in your Percy project settings.

---

## Compatibility

| Component | Version |
|-----------|---------|
| Katalon Studio | 8.x — 11.x+ |
| Percy CLI | 1.28.8+ |
| Java | 8+ (plugin targets Java 8) |
| Selenium | 3.x — 4.x (provided by Katalon) |
| Cucumber | 7.x (provided by Katalon) |

The plugin JAR does **not** need rebuilding for Katalon version upgrades.

---

## CI/CD Integration

### GitHub Actions

```yaml
name: Percy Visual Tests
on: [push, pull_request]
jobs:
  visual-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - run: npm install -g @percy/cli
      - name: Run Percy tests
        env:
          PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}
          KATALON_API_KEY: ${{ secrets.KATALON_API_KEY }}
        run: |
          npx @percy/cli exec -- katalonc \
            -noSplash -runMode=console \
            -projectPath="$(pwd)/your-project.prj" \
            -testSuitePath="Test Suites/VisualTests" \
            -browserType="Chrome (headless)" \
            -apiKey="$KATALON_API_KEY"
```

### Jenkins

```groovy
pipeline {
    agent any
    environment {
        PERCY_TOKEN = credentials('percy-token')
        KATALON_API_KEY = credentials('katalon-api-key')
    }
    stages {
        stage('Visual Tests') {
            steps {
                sh '''
                    npx @percy/cli exec -- katalonc \
                      -noSplash -runMode=console \
                      -projectPath="${WORKSPACE}/your-project.prj" \
                      -testSuitePath="Test Suites/VisualTests" \
                      -browserType="Chrome (headless)" \
                      -apiKey="$KATALON_API_KEY"
                '''
            }
        }
    }
}
```

### Buildkite

```yaml
steps:
  - label: ":percy: Visual Tests"
    command: |
      npx @percy/cli exec -- katalonc \
        -noSplash -runMode=console \
        -projectPath="$$(pwd)/your-project.prj" \
        -testSuitePath="Test Suites/VisualTests" \
        -browserType="Chrome (headless)" \
        -apiKey="$$KATALON_API_KEY"
    env:
      PERCY_TOKEN: "${PERCY_TOKEN}"
      KATALON_API_KEY: "${KATALON_API_KEY}"
```

See [docs/CI-SETUP.md](docs/CI-SETUP.md) for detailed CI/CD setup including Docker and KRE fallback.

---

## Comparison with Other Percy SDKs

| Capability | percy-playwright-java | percy-java-selenium | percy-katalon |
|---|:---:|:---:|:---:|
| DOM snapshot | Yes | Yes | Yes |
| Automate screenshot | Yes | Yes | Yes |
| Region support | Yes | No | Yes |
| Cucumber/BDD steps | Yes | No | Yes |
| Responsive capture | Yes | Yes | Yes |
| Instance caching | No | No | Yes |
| Dual-channel logging | Yes | Yes | Yes |
| Graceful degradation | Yes | Yes | Yes |

---

## Troubleshooting

### "No active WebDriver found"

Make sure you have an active browser session (`WebUI.openBrowser(url)`) before calling `percySnapshot()`.

### Snapshots silently skipped

Percy CLI is not running. Start it with `npx @percy/cli exec:start` before running tests.

### "Percy is not running or not available"

- Verify Percy CLI is running on port 5338
- Check `PERCY_TOKEN` is set correctly
- Check no firewall blocks localhost

### Tests pass but no Percy build

`PERCY_TOKEN` is not set. Percy CLI starts but doesn't create a build without it.

### Plugin not visible in Katalon

- JAR must be in your project's `Plugins/` folder
- Restart Katalon Studio after adding the JAR
- Check Project > Settings > Plugins > "Katalon Store and Local"

### Regions not visible on dashboard

Regions only appear on **diff builds** (not baseline). You need an approved baseline build, then a second build with the same snapshot names + visual changes.

### Port 5338 already in use

```bash
lsof -ti:5338 | xargs kill -9
rm -f ~/.percy/agent-5338.lock
```

---

## Development

### Build

```bash
export JAVA_HOME=/path/to/java-17
gradle shadowJar
```

### Unit Tests (23 tests)

```bash
gradle test
```

### Automated Smoke Tests (14 tests, no Katalon needed)

```bash
export PERCY_TOKEN=<token>
export BROWSERSTACK_USERNAME=<username>
export BROWSERSTACK_ACCESS_KEY=<key>
./scripts/run-all-tests.sh
```

### Manual Katalon Tests (interactive, 22 scenarios)

```bash
export PERCY_TOKEN=<token>
export BROWSERSTACK_USERNAME=<username>
export BROWSERSTACK_ACCESS_KEY=<key>
./scripts/run-manual-tests.sh
```

See [TESTING.md](TESTING.md) for the complete testing guide.