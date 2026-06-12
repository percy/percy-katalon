# percy-katalon

Percy visual testing plugin for [Katalon Studio](https://katalon.com). Capture DOM snapshots from your Katalon tests and render them across all Percy-supported browsers.

## Installation

### Option A: Manual Install (Recommended)

1. Download the latest `percy-katalon-<version>.jar` from [Releases](https://github.com/percy/percy-katalon/releases)
2. Copy it to your Katalon project's `Plugins/` folder
3. Restart Katalon Studio

### Option B: Build from Source

```bash
git clone https://github.com/percy/percy-katalon.git
cd percy-katalon
./gradlew shadowJar
cp build/libs/percy-katalon-*.jar /path/to/your-katalon-project/Plugins/
```

## Setup

You need the Percy CLI to use this plugin:

```bash
npm install -g @percy/cli
```

## Usage

### Basic Snapshot

In any Katalon test script:

```groovy
WebUI.openBrowser('')
WebUI.navigateToUrl('https://your-app.com')

// Take a Percy snapshot
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Homepage')

WebUI.closeBrowser()
```

### Snapshot with Options

```groovy
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Homepage', [
    widths: [375, 768, 1280],
    minHeight: 1024,
    percyCSS: 'header { display: none; }',
    enableJavaScript: true,
    responsiveSnapshotCapture: true
])
```

### Supported Options

| Option | Type | Description |
|--------|------|-------------|
| `widths` | `List<Integer>` | Viewport widths for rendering |
| `minHeight` | `Integer` | Minimum snapshot height |
| `percyCSS` | `String` | Custom CSS injected during rendering |
| `scope` | `String` | CSS selector to scope the snapshot |
| `enableJavaScript` | `Boolean` | Enable JS execution during rendering |
| `responsiveSnapshotCapture` | `Boolean` | Capture DOM at multiple widths |
| `sync` | `Boolean` | Wait for snapshot processing |

## Running Tests

### Local Development (Katalon Studio IDE)

1. Set your Percy token:
   ```bash
   export PERCY_TOKEN=<your-token>
   ```

2. Start Percy CLI in a separate terminal:
   ```bash
   npx @percy/cli start
   ```

3. Open your Katalon project in Katalon Studio and run your test suite

4. Stop Percy CLI:
   ```bash
   npx @percy/cli stop
   ```

5. Check your Percy dashboard for the build results

### CI/CD (Katalon Runtime Engine)

Wrap your `katalonc` command with `percy exec`:

```bash
export PERCY_TOKEN=<your-token>

npx @percy/cli exec -- katalonc \
  -noSplash \
  -runMode=console \
  -projectPath="$(pwd)/your-project.prj" \
  -testSuitePath="Test Suites/YourSuite" \
  -browserType="Chrome (headless)" \
  -apiKey="$KATALON_API_KEY"
```

Percy CLI automatically injects `PERCY_SERVER_ADDRESS` into the child process.

See [docs/CI-SETUP.md](docs/CI-SETUP.md) for framework-specific CI/CD instructions.

### PERCY_SERVER_ADDRESS Fallback

If `percy exec` cannot wrap your Katalon runner (e.g., no KRE license), start Percy CLI separately:

```bash
# Terminal 1
PERCY_TOKEN=<token> npx @percy/cli start

# Terminal 2 (or Katalon Studio IDE)
export PERCY_SERVER_ADDRESS=http://localhost:5338
# Run your Katalon tests

# Terminal 1
npx @percy/cli stop
```

## How It Works

This plugin wraps the [percy-java-selenium](https://github.com/percy/percy-java-selenium) SDK:

1. `percySnapshot()` gets the active WebDriver from Katalon's `DriverFactory`
2. The SDK serializes the page DOM via Percy's DOM serialization script
3. The serialized DOM is sent to Percy CLI (`POST /percy/snapshot`)
4. Percy CLI renders the DOM on all configured browsers (Chrome, Firefox, Edge, Safari, mobile)
5. Visual diffs appear on your Percy dashboard

**No BrowserStack Automate session is involved** -- this is purely DOM-based rendering.

## Browser Support

All Percy-supported browsers work automatically:

- Chrome
- Firefox
- Edge
- Safari
- Safari on iPhone
- Chrome on Android

Browser configuration is managed in your Percy project settings, not in this plugin.

## Troubleshooting

### "No active WebDriver found"

The plugin could not get the WebDriver from `DriverFactory.getWebDriver()`. Make sure you have an active browser session (`WebUI.openBrowser()`) before calling `percySnapshot()`.

### Snapshots are silently skipped

Percy CLI is not running. Make sure you started it with `percy exec` or `npx @percy/cli start` before running your tests.

### "Percy is not running or not available"

The Percy CLI healthcheck failed. Check that:
- Percy CLI is running on the expected port (default: 5338)
- `PERCY_TOKEN` is set correctly
- No firewall is blocking localhost connections

### Tests pass but no Percy build appears

Check that `PERCY_TOKEN` is set. Without it, Percy CLI starts but does not create a build.

### Plugin not visible in Katalon

- Verify the JAR is in your project's `Plugins/` folder
- Restart Katalon Studio after adding the JAR
- Check Project > Settings > Plugins > select "Katalon Store and Local"

## Development

### Build

```bash
./gradlew shadowJar
```

### Test

```bash
./gradlew test
```

### Integration Test

```bash
export PERCY_TOKEN=<your-token>
./scripts/run-integration-test.sh
```

## License

MIT
