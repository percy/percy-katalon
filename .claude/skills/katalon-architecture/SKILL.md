---
name: katalon-architecture
description: "Explains Percy Katalon plugin architecture, browser support, compatibility matrix, and comparison with other Percy SDKs (Playwright, Selenium). Use when someone asks 'how does percy katalon work', 'which browsers are supported', 'percy sdk comparison', 'is percy katalon compatible with my setup', or wants to understand the technical design."
---

# Percy Katalon Plugin — Architecture, Browser Support & Compatibility

Explain the technical architecture and answer compatibility questions.

## Architecture

### Data Flow

```
Katalon Test Script
    │  CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('name')
    ▼
percy-katalon Plugin (.jar)
    │  • Gets WebDriver from DriverFactory.getWebDriver()
    │  • Caches Percy instance (one healthcheck per driver session)
    │  • Delegates to percy-java-selenium SDK
    │  • Graceful degradation: never fails the test
    │  • Dual logging: Katalon console + Percy CLI
    ▼
percy-java-selenium SDK (v2.1.1, bundled in shaded JAR)
    │  • GET /percy/healthcheck
    │  • GET /percy/dom.js (fetch serialization script)
    │  • Inject PercyDOM.serialize() into browser via executeScript
    │  • POST /percy/snapshot (serialized DOM + options + regions)
    ▼
Percy CLI (localhost:5338)
    │  • Started via: percy exec -- katalonc ... OR percy exec:start
    │  • Uploads DOM + assets to Percy API
    │  • Finalizes build on stop
    ▼
Percy API + Rendering Infrastructure
    │  • Renders DOM in real browsers (Chrome, Firefox, Edge, Safari, mobile)
    │  • Compares against approved baseline
    │  • Generates visual diffs
    ▼
Percy Web Dashboard
    • Visual diffs across all browsers
    • Region overlays (ignore/standard/intelliignore)
    • Approve/reject workflow
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Wraps `percy-java-selenium` SDK | Reuse proven DOM serialization + CLI communication |
| Shaded fat JAR | Avoids classpath conflicts with Katalon's bundled libraries |
| Selenium excluded from JAR | Katalon bundles its own Selenium version |
| Static Percy instance caching | Avoids redundant healthcheck HTTP calls per snapshot |
| `catch (Throwable)` error handling | Handles `NoClassDefFoundError` when loaded outside Katalon |
| Dual-channel logging | Logs to both Katalon console + Percy CLI `/percy/log` |

### What's in the JAR

| Component | Purpose |
|-----------|---------|
| `PercyKeywords.class` | Core plugin — snapshot, screenshot, createRegion keywords |
| `PercyCucumberSteps.class` | Cucumber/BDD step definitions |
| `Version.class` | Version constant |
| `io.percy.selenium.*` | percy-java-selenium SDK classes |
| `com.percy.katalon.shaded.org.apache.http.*` | Shaded Apache HttpClient |
| `com.percy.katalon.shaded.org.json.*` | Shaded JSON library |

**NOT in the JAR:** Selenium classes (provided by Katalon), Katalon API classes (provided at runtime), Cucumber classes (provided by Katalon).

## Browser Support

All Percy-supported browsers work automatically. The plugin captures DOM; Percy's infrastructure renders it on real browsers.

| Browser | Platform | Rendering Type |
|---------|----------|---------------|
| Chrome | Linux | Sync (fast) |
| Firefox | Linux | Sync (fast) |
| Edge | Windows | Async |
| Safari | macOS | Async |
| Safari on iPhone | iOS | Async (mobile) |
| Chrome on Android | Android | Async (mobile) |

Browser configuration is managed in Percy project settings (percy.io), not in the plugin.

## Compatibility

| Component | Supported Versions | Notes |
|-----------|-------------------|-------|
| Katalon Studio | 8.x — 11.x+ | Plugin uses stable APIs (DriverFactory, KeywordUtil, @Keyword) |
| Percy CLI | 1.28.8+ | Required for `exec:start`, `build:approve` |
| Java | 8+ | Plugin compiled targeting Java 8 bytecode |
| Selenium | 3.x — 4.x | Provided by Katalon at runtime |
| Cucumber | 7.x | Provided by Katalon for BDD support |
| Node.js | 16+ | For Percy CLI |

The JAR does **NOT** need rebuilding when Katalon is upgraded. It depends only on stable Katalon APIs.

## Comparison with Other Percy SDKs

| Capability | percy-playwright-java | percy-java-selenium | percy-katalon |
|---|:---:|:---:|:---:|
| DOM snapshot | Yes | Yes | Yes |
| Automate screenshot | Yes | Yes | Yes |
| Region support (ignore/standard/intelliignore) | Yes | No | Yes |
| Cucumber/BDD step definitions | Yes | No | Yes |
| Responsive snapshot capture | Yes | Yes | Yes |
| Client info attribution | Yes | Yes | Yes |
| Percy instance caching | No | No | **Yes** (improvement) |
| Dual-channel logging | Yes | Yes | Yes |
| Graceful degradation | Yes | Yes | Yes |
| Distribution | Maven Central | Maven Central | Katalon Plugins/ JAR |

### How percy-katalon Differs

1. **Wraps percy-java-selenium** — doesn't reimplement SDK communication
2. **Instance caching** — avoids redundant healthcheck calls (unique to percy-katalon)
3. **Shaded dependencies** — relocates Apache HttpClient, org.json to avoid classpath conflicts
4. **Katalon integration** — auto-gets WebDriver from `DriverFactory`, logs via `KeywordUtil`
5. **Region support added** — `createRegion()` with full algorithm support (percy-java-selenium doesn't have this)

## Known Limitations

| Limitation | Detail | Workaround |
|---|---|---|
| Mobile responsive with height | SDK v2.1.1 doesn't call `/percy/widths-config` for height | Use explicit `widths` option |
| Katalon TestCloud | Percy CLI needs localhost access | Use `PERCY_SERVER_ADDRESS` if network allows |
| `setClientInfo()` | Not in SDK v2.1.1 | Client info passed via options map |
