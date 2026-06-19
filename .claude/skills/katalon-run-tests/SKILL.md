---
name: katalon-run-tests
description: "Guide for running Percy Katalon tests and reviewing results on the Percy dashboard. Covers local development (Katalon Studio IDE), CI/CD (Katalon Runtime Engine), PERCY_SERVER_ADDRESS fallback, reviewing diffs, approving builds, and understanding graceful degradation. Use when someone says 'how to run percy tests', 'run katalon percy', 'review percy results', 'percy dashboard', or 'percy not working'."
---

# Percy Katalon Plugin — Running Tests & Reviewing Results

Help the user run Percy Katalon tests and review results on the Percy dashboard.

## Assess the User's Setup

Ask which environment they're using:
1. **Local development** — Katalon Studio IDE
2. **CI/CD** — Katalon Runtime Engine (katalonc)
3. **No KRE license** — IDE only, no console runner

## 1. Local Development (Katalon Studio IDE)

### Start Percy CLI

```bash
export PERCY_TOKEN=<your-token>
npx @percy/cli exec:start
```

Verify it's running:
```bash
curl -s http://localhost:5338/percy/healthcheck
```

### Run Tests in Katalon Studio

1. Open Katalon Studio
2. Open your project (File > Open Project)
3. Run your test case or test suite (green play button, select Chrome/Firefox/Edge)
4. Watch the Katalon console for `[percy]` messages

### Stop Percy CLI

```bash
npx @percy/cli exec:stop
```

This finalizes the build and prints the build URL.

### View Results

Open the build URL printed in the terminal. The Percy dashboard shows:
- All snapshots rendered across configured browsers
- Visual diffs compared against the approved baseline
- Region overlays (if regions were used)

## 2. CI/CD (Katalon Runtime Engine)

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

`percy exec` handles start/stop automatically. The build URL is printed at the end.

## 3. Without KRE License (PERCY_SERVER_ADDRESS Fallback)

```bash
# Terminal 1: Start Percy CLI
PERCY_TOKEN=<token> npx @percy/cli exec:start

# Terminal 2 or Katalon Studio: Run tests
export PERCY_SERVER_ADDRESS=http://localhost:5338
# (Katalon reads this env var when the plugin initializes)

# Terminal 1: Stop Percy CLI
npx @percy/cli exec:stop
```

## 4. Reviewing on Percy Dashboard

### First Build (Baseline)

- All snapshots marked as "new" — no diffs to show
- **Approve the build** to set it as the baseline for future comparisons

### Subsequent Builds

- Changed snapshots show visual diffs
- Unchanged snapshots show as "no changes"
- **Regions visible in diff view:**
  - Ignore regions: grey overlay, changes within are NOT flagged
  - Standard regions: highlighted area, changes ARE flagged with configured sensitivity
  - Intelliignore regions: AI badge, noise auto-skipped, real changes flagged

### Approving Builds

Via dashboard: Click "Approve" on each snapshot or "Approve All"

Via CLI:
```bash
export BROWSERSTACK_USERNAME=<username>
export BROWSERSTACK_ACCESS_KEY=<key>
npx @percy/cli build:approve <build-id>
```

### Waiting for Build to Finish

```bash
npx @percy/cli build:wait --build <build-id>
```

## 5. Graceful Degradation

The plugin **never fails the test**. Guide the user through these scenarios:

| Symptom | Cause | Fix |
|---------|-------|-----|
| Snapshots silently skipped | Percy CLI not running | Start CLI: `npx @percy/cli exec:start` |
| "No active WebDriver found" | No browser open | Call `WebUI.openBrowser(url)` before `percySnapshot` |
| "Percy is not running" | Healthcheck failed | Check port 5338, PERCY_TOKEN, firewall |
| Tests pass but no build | PERCY_TOKEN missing | `export PERCY_TOKEN=<token>` |
| Regions not visible | First build (no baseline) | Approve first build, then run again |
| Port 5338 in use | Stale process | `lsof -ti:5338 \| xargs kill -9; rm -f ~/.percy/agent-5338.lock` |

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `PERCY_TOKEN` | Yes | Percy project token |
| `KATALON_API_KEY` | For KRE | Katalon Runtime Engine license key |
| `PERCY_SERVER_ADDRESS` | No | CLI address (default: `http://localhost:5338`) |
| `PERCY_LOGLEVEL` | No | Set to `debug` for verbose output |
| `BROWSERSTACK_USERNAME` | For approve | BrowserStack credentials |
| `BROWSERSTACK_ACCESS_KEY` | For approve | BrowserStack credentials |
