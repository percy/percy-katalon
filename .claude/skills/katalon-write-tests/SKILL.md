---
name: katalon-write-tests
description: "Guide for writing Percy visual tests in Katalon Studio. Covers basic snapshots, snapshot options, regions (ignore/standard/intelliignore), BrowserStack Automate screenshots, and BDD/Cucumber step definitions. Use when someone says 'how to write percy tests', 'write a katalon percy test', 'how to use percySnapshot', 'how to add regions', or 'percy cucumber steps'."
---

# Percy Katalon Plugin — Writing Tests Guide

Help the user write Percy visual tests in their Katalon project.

## Assess What They Need

Ask the user which type of test they want to write:
1. **Basic snapshot** — capture a page as-is
2. **Snapshot with options** — custom widths, CSS, scope, responsive
3. **Snapshot with regions** — ignore/monitor specific areas
4. **BrowserStack Automate screenshot** — screenshot from Automate session
5. **BDD/Cucumber** — Gherkin step definitions

## 1. Basic Snapshot

```groovy
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

WebUI.openBrowser('https://your-app.com')
WebUI.delay(2)

CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Homepage')

WebUI.closeBrowser()
```

Key points:
- Snapshot name must be unique within a build
- Browser must be open before calling `percySnapshot`
- If Percy CLI isn't running, the call returns null silently

## 2. Snapshot with Options

```groovy
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Homepage', [
    widths: [375, 768, 1280],        // render at these viewport widths
    minHeight: 1024,                   // minimum snapshot height
    percyCSS: 'header { display: none; }',  // CSS applied in Percy only
    scope: '#main-content',            // snapshot only this section
    enableJavaScript: true,            // enable JS in Percy rendering
    responsiveSnapshotCapture: true,   // capture DOM at each width
    enableLayout: true,                // layout comparison mode
    labels: 'smoke,homepage',          // organize snapshots
    sync: true                         // wait for processing
])
```

All options reference:

| Option | Type | Description |
|--------|------|-------------|
| `widths` | `List<Integer>` | Viewport widths |
| `minHeight` | `Integer` | Min snapshot height |
| `percyCSS` | `String` | CSS for Percy rendering only |
| `scope` | `String` | CSS selector to limit area |
| `enableJavaScript` | `Boolean` | Enable JS in rendering |
| `responsiveSnapshotCapture` | `Boolean` | Client-side responsive |
| `sync` | `Boolean` | Wait for processing |
| `enableLayout` | `Boolean` | Layout comparison |
| `disableShadowDom` | `Boolean` | Skip Shadow DOM |
| `labels` | `String` | Comma-separated labels |
| `testCase` | `String` | Test case ID |
| `domTransformation` | `String` | JS to transform DOM |
| `regions` | `List<Map>` | Region configs |

## 3. Snapshot with Regions

### Region Algorithms

| Algorithm | Use Case | Example |
|-----------|----------|---------|
| `ignore` | Ads, timestamps, counters | Always skip changes |
| `standard` | Main content with sensitivity | Detect with threshold (0-4) |
| `layout` | Responsive layouts | Detect structural shifts |
| `intelliignore` | Mixed content | AI skips noise, flags real changes |

### Creating Regions

```groovy
// Ignore via CSS selector
def ignoreAd = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: '.ad-banner',
    algorithm: 'ignore'
])

// Ignore via XPath
def ignoreNav = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementXpath: '//nav',
    algorithm: 'ignore'
])

// Ignore via bounding box
def ignoreArea = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    boundingBox: [x: 0, y: 0, width: 800, height: 80],
    algorithm: 'ignore'
])

// Standard with sensitivity
def monitorContent = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: '.main-content',
    algorithm: 'standard',
    diffSensitivity: 2          // 0-4, higher = more sensitive
])

// Ignore with padding (integer = all sides)
def ignoreWithPad = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: 'header',
    algorithm: 'ignore',
    padding: 10
])

// Ignore with asymmetric padding
def ignoreWithMapPad = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: 'header',
    algorithm: 'ignore',
    padding: [top: 5, bottom: 10, left: 15, right: 20]
])

// AI-powered noise detection
def smartRegion = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
    elementCSS: '.sidebar',
    algorithm: 'intelliignore',
    adsEnabled: true,
    bannersEnabled: true
])

// Pass regions to snapshot
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Homepage', [
    regions: [ignoreAd, monitorContent, ignoreWithPad, smartRegion]
])
```

### Region Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `elementCSS` | `String` | CSS selector |
| `elementXpath` | `String` | XPath selector |
| `boundingBox` | `Map` | `{x, y, width, height}` |
| `algorithm` | `String` | `ignore`, `standard`, `layout`, `intelliignore` |
| `padding` | `Integer` or `Map` | Integer = all sides; Map = `{top, bottom, left, right}` |
| `diffSensitivity` | `Integer` | 0-4 (standard/intelliignore) |
| `carouselsEnabled` | `Boolean` | Detect carousels (intelliignore) |
| `bannersEnabled` | `Boolean` | Detect banners (intelliignore) |
| `adsEnabled` | `Boolean` | Detect ads (intelliignore) |
| `diffIgnoreThreshold` | `Number` | Assertion threshold |

## 4. BrowserStack Automate Screenshot

For tests running on BrowserStack Automate:

```groovy
CustomKeywords.'com.percy.katalon.PercyKeywords.percyScreenshot'('Homepage')

CustomKeywords.'com.percy.katalon.PercyKeywords.percyScreenshot'('Homepage', [
    fullPage: true,
    percyCSS: 'footer { display: none; }',
    ignoreRegionSelectors: ['.dynamic-content'],
    considerRegionSelectors: ['.hero-section']
])
```

## 5. BDD/Cucumber

Feature file:
```gherkin
Feature: Visual Regression

  Scenario: Homepage check
    Given I have a Percy instance
    And I create a Percy ignore region with CSS selector ".ad-banner"
    And I create a Percy consider region with CSS selector ".hero" and diff sensitivity 2
    When I take a Percy snapshot named "Homepage" with regions
    Then Percy should be enabled
```

Cucumber hooks:
```groovy
import com.percy.katalon.PercyCucumberSteps

@Before
def setUp() { PercyCucumberSteps.init() }

@After
def tearDown() { PercyCucumberSteps.cleanup() }
```

Available steps:

**Given (region setup):**
- `I have a Percy instance`
- `I create a Percy ignore region with CSS selector "<sel>"`
- `I create a Percy ignore region with XPath "<xpath>"`
- `I create a Percy ignore region with bounding box <x>, <y>, <w>, <h>`
- `I create a Percy consider region with CSS selector "<sel>"`
- `I create a Percy consider region with CSS selector "<sel>" and diff sensitivity <n>`
- `I create a Percy intelliignore region with CSS selector "<sel>"`
- `I clear Percy regions`

**When (snapshots):**
- `I take a Percy snapshot named "<name>"`
- `I take a Percy snapshot named "<name>" with widths "<w1,w2>"`
- `I take a Percy snapshot named "<name>" with Percy CSS "<css>"`
- `I take a Percy snapshot named "<name>" with scope "<selector>"`
- `I take a Percy snapshot named "<name>" with regions`
- `I take a Percy snapshot named "<name>" with responsive capture`
- `I take a Percy screenshot named "<name>"`

**Then:**
- `Percy should be enabled`
