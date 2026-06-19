# Percy Visual Testing for Katalon Studio

## Overview

Percy now supports **Katalon Studio** for visual regression testing. Katalon Studio users can capture DOM snapshots and BrowserStack Automate screenshots directly from their Katalon test scripts, with Percy rendering them across all configured browsers (Chrome, Firefox, Edge, Safari, mobile).

The integration is delivered as a **Katalon Custom Keywords plugin** (`.jar`) that wraps the `percy-java-selenium` SDK. It follows the same architecture as all other Percy SDKs (Selenium, Playwright, Cypress) — a thin client that communicates with Percy CLI over HTTP.

**Jira Epic:** PPLT-5586 | **Reference SDK:** [percy-playwright-java](https://github.com/percy/percy-playwright-java)

---

## Claude Code Skills

If you're using [Claude Code](https://claude.ai/code) in this repo, the following slash commands are available as interactive guides:

| Skill | Command | Description |
|-------|---------|-------------|
| Installation | `/katalon-install` | Step-by-step plugin installation |
| Writing Tests | `/katalon-write-tests` | Snapshots, options, regions, BDD/Cucumber |
| Running & Reviewing | `/katalon-run-tests` | Local, CI/CD, dashboard review, troubleshooting |
| Architecture | `/katalon-architecture` | Data flow, browsers, compatibility, SDK comparison |
| CI/CD Integration | `/katalon-cicd` | GitHub Actions, Jenkins, Buildkite setup |

Skills are located at `.claude/skills/` and provide the same content as the guides below but in an interactive, context-aware format.

---

## Guide 1: Installation

### Prerequisites

| Tool | Install | Purpose |
|------|---------|---------|
| Node.js | [nodejs.org](https://nodejs.org) | Required for Percy CLI |
| Percy CLI | `npm install -g @percy/cli` | Orchestrates builds and uploads |
| Katalon Studio | [katalon.com/download](https://katalon.com/download) | Test automation IDE |

### Steps

```
Step 1: Install Percy CLI
  └─ npm install -g @percy/cli

Step 2: Get the plugin JAR
  └─ Download from GitHub Releases OR build from source:
     git clone https://github.com/percy/percy-katalon.git
     cd percy-katalon && gradle shadowJar

Step 3: Copy JAR to your Katalon project
  └─ cp percy-katalon-1.0.0.jar <your-project>/Plugins/

Step 4: Restart Katalon Studio
  └─ Percy keywords appear under "Percy" category in keyword browser

Step 5: Get Percy token
  └─ percy.io > Your Project > Project Settings > copy PERCY_TOKEN
```

**Screenshot: Plugin loaded in Katalon Studio**

> The screenshot below shows Katalon Studio IDE with `percy-katalon-1.0.0.jar` loaded under the Plugins folder in the Tests Explorer.

`[See: katalon_1.png — Katalon Studio IDE with plugin loaded]`

### Verify Installation

In any Katalon test script, add this line — if it runs without error, the plugin is loaded:

```groovy
WebUI.openBrowser('https://example.com')
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Test')
WebUI.closeBrowser()
```

If Percy CLI is not running, the snapshot is silently skipped (no error). If the keyword is not found, the JAR is not loaded — check the Plugins folder and restart Katalon.

---

## Guide 2: Writing Tests

### 2.1 Basic Snapshot

```groovy
WebUI.openBrowser('https://your-app.com')
WebUI.delay(2)

CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Homepage')

WebUI.closeBrowser()
```

### 2.2 Snapshot with Options

```groovy
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Homepage', [
    widths: [375, 768, 1280],        // render at multiple widths
    minHeight: 1024,                   // minimum height
    percyCSS: 'header { display: none; }',  // CSS for Percy only
    enableJavaScript: true,            // enable JS in Percy rendering
    scope: '#main-content',            // snapshot only this section
    responsiveSnapshotCapture: true,   // capture DOM at each width
    labels: 'smoke,homepage'           // organize snapshots
])
```

### 2.3 Snapshot with Regions

```groovy
// Ignore dynamic content
def ignoreAd = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: '.ad-banner',
    algorithm: 'ignore'
])

// Monitor content with sensitivity
def monitorContent = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: '.main-content',
    algorithm: 'standard',
    diffSensitivity: 2
])

// Ignore with padding
def ignoreHeader = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: 'header',
    algorithm: 'ignore',
    padding: 10                 // 10px on all sides
])

// AI-powered noise detection
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

### 2.4 BrowserStack Automate Screenshot

```groovy
CustomKeywords.'com.percy.katalon.PercyKeywords.percyScreenshot'('Homepage')

// With options
CustomKeywords.'com.percy.katalon.PercyKeywords.percyScreenshot'('Homepage', [
    fullPage: true,
    percyCSS: 'footer { display: none; }',
    ignoreRegionSelectors: ['.dynamic-content']
])
```

### 2.5 BDD/Cucumber

```gherkin
Feature: Visual Regression Testing

  Scenario: Homepage visual check
    Given I have a Percy instance
    And I create a Percy ignore region with CSS selector ".ad-banner"
    And I create a Percy consider region with CSS selector ".hero" and diff sensitivity 2
    When I take a Percy snapshot named "Homepage" with regions
    Then Percy should be enabled

  Scenario: Responsive check
    When I take a Percy snapshot named "Homepage" with widths "375,768,1280"
```

Setup in Cucumber hooks:
```groovy
import com.percy.katalon.PercyCucumberSteps

@Before
def setUp() { PercyCucumberSteps.init() }

@After
def tearDown() { PercyCucumberSteps.cleanup() }
```

### 2.6 Region Algorithm Guide

| Algorithm | When to Use | Example |
|-----------|-------------|---------|
| `ignore` | Ads, timestamps, counters — always skip | `algorithm: 'ignore'` |
| `standard` | Main content — detect changes with tunable sensitivity | `algorithm: 'standard', diffSensitivity: 2` |
| `layout` | Responsive layouts — detect structural shifts | `algorithm: 'layout'` |
| `intelliignore` | Mixed pages — AI skips noise, flags real changes | `algorithm: 'intelliignore', adsEnabled: true` |

---

## Guide 3: Running Tests & Reviewing Results

### 3.1 Local Development (Katalon Studio IDE)

```bash
# Terminal 1: Start Percy CLI
export PERCY_TOKEN=<your-token>
npx @percy/cli exec:start

# Katalon Studio: Run your test case (Chrome/Firefox/Edge)
# Snapshots are sent to Percy CLI automatically

# Terminal 1: Stop Percy CLI (finalizes build, prints build URL)
npx @percy/cli exec:stop
```

### 3.2 CI/CD (Katalon Runtime Engine)

```bash
export PERCY_TOKEN=<your-token>
npx @percy/cli exec -- katalonc \
  -noSplash -runMode=console \
  -projectPath="$(pwd)/my-project.prj" \
  -testSuitePath="Test Suites/VisualTests" \
  -browserType="Chrome (headless)" \
  -apiKey="$KATALON_API_KEY"
```

### 3.3 Without KRE License

```bash
# Terminal 1: Start Percy CLI standalone
PERCY_TOKEN=<token> npx @percy/cli exec:start

# Terminal 2 / Katalon Studio: Run tests
export PERCY_SERVER_ADDRESS=http://localhost:5338

# Terminal 1: Stop (finalizes build)
npx @percy/cli exec:stop
```

### 3.4 Reviewing on Percy Dashboard

After stopping Percy CLI, open the build URL printed in the terminal.

**First build (baseline):**
- All snapshots marked as "new" — no diffs
- Approve the build to set as baseline

**Subsequent builds:**
- Visual diffs shown for changed snapshots
- Region overlays visible: ignore (grey), standard (highlighted), intelliignore (AI badge)
- Approve or request changes on each snapshot

**What to look for:**
- Ignore regions: changes within should NOT be flagged
- Standard regions: changes should be highlighted with configured sensitivity
- Intelliignore regions: AI decides — noise skipped, real changes flagged
- Unregioned areas: all changes detected normally

`[Screenshot placeholder: Percy dashboard showing diff build with region overlays]`

### 3.5 Graceful Degradation

The plugin **never fails your test**:

| Scenario | What Happens |
|----------|-------------|
| Percy CLI not running | Warning logged, null returned, test continues |
| CLI returns error | Error logged, null returned, test continues |
| No active browser | Warning logged, null returned, test continues |
| Plugin outside Katalon | Error caught, no crash |

---

## Guide 4: Architecture, Browser Support, Compatibility & SDK Comparison

### Architecture

### Snapshot Keywords (Percy Web — DOM-based)

| Keyword | Description |
|---------|-------------|
| `percySnapshot(name)` | Capture DOM snapshot of current page |
| `percySnapshot(name, options)` | Capture with options (see below) |

### Screenshot Keywords (Percy Automate — BrowserStack)

| Keyword | Description |
|---------|-------------|
| `percyScreenshot(name)` | Capture screenshot via BrowserStack Automate |
| `percyScreenshot(name, options)` | Capture with Automate options |

### Region Keywords

| Keyword | Description |
|---------|-------------|
| `createRegion(params)` | Build a region config map for use in snapshot/screenshot |

### Snapshot Options

| Option | Type | Description |
|--------|------|-------------|
| `widths` | `List<Integer>` | Viewport widths for rendering (e.g., `[375, 768, 1280]`) |
| `minHeight` | `Integer` | Minimum snapshot height in pixels |
| `percyCSS` | `String` | Custom CSS injected only in Percy's rendering |
| `scope` | `String` | CSS selector to limit snapshot to a page section |
| `enableJavaScript` | `Boolean` | Enable JS in Percy's rendering environment |
| `responsiveSnapshotCapture` | `Boolean` | Capture DOM at multiple widths client-side |
| `sync` | `Boolean` | Wait for snapshot processing to complete |
| `enableLayout` | `Boolean` | Enable layout comparison mode |
| `disableShadowDom` | `Boolean` | Disable Shadow DOM serialization |
| `labels` | `String` | Comma-separated labels for organizing snapshots |
| `testCase` | `String` | Test case identifier for tracking |
| `domTransformation` | `String` | JS function to transform DOM before capture |
| `regions` | `List<Map>` | Per-region comparison config (use `createRegion()`) |
| `readiness` | `Map` | Readiness gate configuration |

### Screenshot Options (Automate)

| Option | Type | Description |
|--------|------|-------------|
| `sync` | `Boolean` | Wait for screenshot processing |
| `fullPage` | `Boolean` | Full page screenshot |
| `freezeAnimatedImage` | `Boolean` | Freeze image-based animations |
| `freezeImageBySelectors` | `List<String>` | CSS selectors for images to freeze |
| `freezeImageByXpaths` | `List<String>` | XPaths for images to freeze |
| `percyCSS` | `String` | Custom CSS before screenshot |
| `ignoreRegionSelectors` | `List<String>` | CSS selectors to ignore |
| `ignoreRegionXpaths` | `List<String>` | XPaths to ignore |
| `customIgnoreRegions` | `List<Map>` | Bounding boxes to ignore |
| `considerRegionSelectors` | `List<String>` | CSS selectors for consider regions |
| `considerRegionXpaths` | `List<String>` | XPaths for consider regions |
| `customConsiderRegions` | `List<Map>` | Bounding boxes for consider regions |
| `regions` | `List<Map>` | Unified region config (use `createRegion()`) |

### Region Algorithms

| Algorithm | Behavior | Use Case |
|-----------|----------|----------|
| `ignore` | All changes in region silently skipped | Ads, timestamps, dynamic counters |
| `standard` | Pixel-level diff with configurable sensitivity (0-4) | Main content that should be monitored |
| `layout` | Structural layout comparison | Responsive layout checks |
| `intelliignore` | AI-powered: ignores ad/banner/carousel noise, flags real changes | Pages with mixed dynamic + static content |

### Region Configuration

| Parameter | Type | Description |
|-----------|------|-------------|
| `elementCSS` | `String` | CSS selector for the region element |
| `elementXpath` | `String` | XPath for the region element |
| `boundingBox` | `Map` | `{x, y, width, height}` coordinates |
| `padding` | `Integer` or `Map` | Padding around region. Integer applies to all sides. Map: `{top, bottom, left, right}` |
| `algorithm` | `String` | `ignore`, `standard`, `layout`, `intelliignore` |
| `diffSensitivity` | `Integer` | 0-4, for standard/intelliignore algorithms |
| `imageIgnoreThreshold` | `Number` | Image ignore threshold |
| `carouselsEnabled` | `Boolean` | Detect and handle carousels (intelliignore) |
| `bannersEnabled` | `Boolean` | Detect and handle banners (intelliignore) |
| `adsEnabled` | `Boolean` | Detect and handle ads (intelliignore) |
| `diffIgnoreThreshold` | `Number` | Assertion threshold for minor diffs |

### Cucumber/BDD Step Definitions

| Step | Type |
|------|------|
| `Given I have a Percy instance` | Setup |
| `Given I create a Percy ignore region with CSS selector "<sel>"` | Region |
| `Given I create a Percy ignore region with XPath "<xpath>"` | Region |
| `Given I create a Percy ignore region with bounding box <x>, <y>, <w>, <h>` | Region |
| `Given I create a Percy ignore region with CSS selector "<sel>" and padding <px>` | Region |
| `Given I create a Percy consider region with CSS selector "<sel>"` | Region |
| `Given I create a Percy consider region with CSS selector "<sel>" and diff sensitivity <n>` | Region |
| `Given I create a Percy intelliignore region with CSS selector "<sel>"` | Region |
| `Given I clear Percy regions` | Region |
| `When I take a Percy snapshot named "<name>"` | Snapshot |
| `When I take a Percy snapshot named "<name>" with widths "<w1,w2>"` | Snapshot |
| `When I take a Percy snapshot named "<name>" with Percy CSS "<css>"` | Snapshot |
| `When I take a Percy snapshot named "<name>" with scope "<selector>"` | Snapshot |
| `When I take a Percy snapshot named "<name>" with regions` | Snapshot |
| `When I take a Percy snapshot named "<name>" with responsive capture` | Snapshot |
| `When I take a Percy screenshot named "<name>"` | Screenshot |
| `When I take a Percy screenshot named "<name>" with regions` | Screenshot |
| `Then Percy should be enabled` | Assertion |

### Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Katalon Test Script                          │
│  CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   percy-katalon Plugin (.jar)                       │
│                                                                     │
│  • Gets WebDriver from DriverFactory.getWebDriver()                 │
│  • Caches Percy instance (one healthcheck per driver session)       │
│  • Delegates to percy-java-selenium SDK                             │
│  • Graceful degradation: never fails the test                       │
│  • Dual logging: Katalon console + Percy CLI                        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│              percy-java-selenium SDK (v2.1.1, bundled)              │
│                                                                     │
│  • GET /percy/healthcheck (is CLI running?)                         │
│  • GET /percy/dom.js (fetch serialization script)                   │
│  • Inject PercyDOM.serialize() into browser                         │
│  • POST /percy/snapshot (send serialized DOM + options + regions)   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │  HTTP to localhost:5338
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Percy CLI                                              │
│              (Started via: percy exec -- katalonc ...)               │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Percy API + Rendering Infrastructure                   │
│                                                                     │
│  Renders DOM on all configured browsers:                            │
│  Chrome, Firefox, Edge, Safari, Safari on iPhone, Chrome on Android │
│                                                                     │
│  Compares against approved baseline → generates visual diffs        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Percy Web Dashboard                                    │
│                                                                     │
│  • Visual diffs across all browsers                                 │
│  • Region overlays (ignore/standard/intelliignore)                  │
│  • Approve/reject workflow                                          │
└─────────────────────────────────────────────────────────────────────┘
```

### Browser Support

All Percy-supported browsers work automatically. No browser-specific code in the plugin.

| Browser | Platform | Type |
|---------|----------|------|
| Chrome | Linux | Desktop (sync) |
| Firefox | Linux | Desktop (sync) |
| Edge | Windows | Desktop (async) |
| Safari | macOS | Desktop (async) |
| Safari on iPhone | iOS | Mobile (async) |
| Chrome on Android | Android | Mobile (async) |

Browser configuration is managed in Percy project settings, not in the plugin.

### Compatibility

| Component | Version |
|-----------|---------|
| **Katalon Studio** | 8.x — 11.x+ |
| **Percy CLI** | 1.28.8+ |
| **percy-java-selenium SDK** | 2.1.1 (bundled) |
| **Java** | 8+ (plugin compiled for Java 8) |
| **Selenium** | 3.x — 4.x (provided by Katalon) |
| **Cucumber** | 7.x (provided by Katalon, for BDD support) |

### Comparison with Other Percy SDKs

The plugin **never fails the test**. If Percy is unavailable, snapshots are silently skipped:

| Scenario | Behavior |
|----------|----------|
| Percy CLI not running | Logs warning, returns null, test continues |
| Percy CLI returns error (500, invalid token) | Logs error, returns null, test continues |
| No active browser (`DriverFactory.getWebDriver()` returns null) | Logs warning, returns null, test continues |
| Plugin loaded outside Katalon (`NoClassDefFoundError`) | Caught, returns null, no crash |

---

## Guide 5: CI/CD Integration

### GitHub Actions

```yaml
- name: Run Percy visual tests
  env:
    PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}
    KATALON_API_KEY: ${{ secrets.KATALON_API_KEY }}
  run: |
    npx @percy/cli exec -- katalonc \
      -noSplash -runMode=console \
      -projectPath="$(pwd)/my-project.prj" \
      -testSuitePath="Test Suites/VisualTests" \
      -browserType="Chrome (headless)"  \
      -apiKey="$KATALON_API_KEY"
```

### Jenkins

```groovy
environment {
    PERCY_TOKEN = credentials('percy-token')
    KATALON_API_KEY = credentials('katalon-api-key')
}
steps {
    sh 'npx @percy/cli exec -- katalonc -noSplash -runMode=console ...'
}
```

### Without Katalon Runtime Engine (no KRE license)

```bash
# Terminal 1: Start Percy CLI
PERCY_TOKEN=<token> npx @percy/cli exec:start

# Terminal 2 / Katalon Studio IDE: Run tests
export PERCY_SERVER_ADDRESS=http://localhost:5338

# Terminal 1: Stop Percy CLI (finalizes build)
npx @percy/cli exec:stop
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

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `PERCY_TOKEN` | Yes | Percy project token |
| `KATALON_API_KEY` | For KRE | Katalon Runtime Engine license key |
| `PERCY_SERVER_ADDRESS` | No | Percy CLI address (default: `http://localhost:5338`) |
| `PERCY_LOGLEVEL` | No | Set to `debug` for verbose logging |
| `BROWSERSTACK_USERNAME` | For Automate | BrowserStack credentials |
| `BROWSERSTACK_ACCESS_KEY` | For Automate | BrowserStack credentials |

| Capability | percy-playwright-java | percy-java-selenium | percy-katalon |
|---|:---:|:---:|:---:|
| DOM snapshot | Yes | Yes | Yes |
| Automate screenshot | Yes | Yes | Yes |
| Region support | Yes | No | Yes |
| Cucumber/BDD steps | Yes | No | Yes |
| Responsive capture | Yes | Yes | Yes |
| Client info attribution | Yes | Yes | Yes |
| Instance caching | No | No | Yes (improvement) |
| Dual-channel logging | Yes | Yes | Yes |
| Graceful degradation | Yes | Yes | Yes |

---

---

## Appendix

### Known Limitations

| Limitation | Detail | Workaround |
|---|---|---|
| Mobile responsive capture with height | percy-java-selenium v2.1.1 doesn't call `/percy/widths-config` for height pairs | Use explicit `widths` option; height-aware capture planned for SDK upgrade |
| Katalon TestCloud | Percy CLI needs localhost access; TestCloud runs on Katalon infrastructure | Use `PERCY_SERVER_ADDRESS` if network allows, otherwise use local execution |
| `setClientInfo()` | Not available in SDK v2.1.1 | Client info passed via options map |

### Distribution

| Channel | Status |
|---------|--------|
| Manual install (Plugins/ folder) | **Available** |
| Katalon Store | Planned (pending submission) |
| Maven Central | Not applicable (Katalon plugin, not a Maven artifact) |

### Helpful Links

- Plugin source: `percy/percy-katalon` (GitHub)
- Percy docs: https://www.browserstack.com/docs/percy
- Katalon Custom Keywords: https://docs.katalon.com/katalon-studio/keywords/custom-keywords/introduction-to-custom-keywords-in-katalon-studio
- Percy CLI: https://www.npmjs.com/package/@percy/cli
- Reference SDK: https://github.com/percy/percy-playwright-java
