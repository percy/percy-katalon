# CI/CD Setup for Percy Katalon Plugin

## Prerequisites

- Node.js (for Percy CLI)
- Katalon Runtime Engine (KRE) license and `katalonc` binary
- `PERCY_TOKEN` environment variable set in your CI secrets
- `KATALON_API_KEY` environment variable set in your CI secrets

## GitHub Actions

```yaml
name: Percy Visual Tests

on: [push, pull_request]

jobs:
  visual-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Install Percy CLI
        run: npm install -g @percy/cli

      - name: Setup Katalon Runtime Engine
        run: |
          # Download and install KRE (adjust version as needed)
          curl -sL https://github.com/nicam/katalon-runtime-engine-docker/archive/main.tar.gz | tar xz
          # Or use Katalon's official Docker image (see Docker section below)

      - name: Run Percy tests
        env:
          PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}
          KATALON_API_KEY: ${{ secrets.KATALON_API_KEY }}
        run: |
          npx @percy/cli exec -- katalonc \
            -noSplash \
            -runMode=console \
            -projectPath="$(pwd)/your-project.prj" \
            -testSuitePath="Test Suites/PercyVisualTests" \
            -browserType="Chrome (headless)" \
            -apiKey="$KATALON_API_KEY"
```

### Using Katalon Docker Image

```yaml
jobs:
  visual-tests:
    runs-on: ubuntu-latest
    container:
      image: katalonstudio/katalon:latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Install Percy CLI
        run: npm install -g @percy/cli

      - name: Run Percy tests
        env:
          PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}
          KATALON_API_KEY: ${{ secrets.KATALON_API_KEY }}
        run: |
          npx @percy/cli exec -- katalonc \
            -noSplash \
            -runMode=console \
            -projectPath="$(pwd)/your-project.prj" \
            -testSuitePath="Test Suites/PercyVisualTests" \
            -browserType="Chrome (headless)" \
            -apiKey="$KATALON_API_KEY"
```

## Jenkins

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
                      -noSplash \
                      -runMode=console \
                      -projectPath="${WORKSPACE}/your-project.prj" \
                      -testSuitePath="Test Suites/PercyVisualTests" \
                      -browserType="Chrome (headless)" \
                      -apiKey="$KATALON_API_KEY"
                '''
            }
        }
    }
}
```

## Buildkite

```yaml
steps:
  - label: ":percy: Visual Tests"
    command: |
      npx @percy/cli exec -- katalonc \
        -noSplash \
        -runMode=console \
        -projectPath="$$(pwd)/your-project.prj" \
        -testSuitePath="Test Suites/PercyVisualTests" \
        -browserType="Chrome (headless)" \
        -apiKey="$$KATALON_API_KEY"
    env:
      PERCY_TOKEN: "${PERCY_TOKEN}"
      KATALON_API_KEY: "${KATALON_API_KEY}"
    plugins:
      - docker#v5.10.0:
          image: "katalonstudio/katalon:latest"
```

## Without KRE License (PERCY_SERVER_ADDRESS Fallback)

If you don't have a Katalon Runtime Engine license, you can still use Percy in CI by starting Percy CLI separately:

```bash
# Start Percy CLI in the background
PERCY_TOKEN=$PERCY_TOKEN npx @percy/cli start &
PERCY_PID=$!

# Wait for Percy CLI to be ready
sleep 5

# Set the server address for the Katalon process
export PERCY_SERVER_ADDRESS=http://localhost:5338

# Run your Katalon tests however you can
# (e.g., using Katalon Docker image, or a custom test runner)
your-katalon-command-here

# Stop Percy CLI (this finalizes the build)
npx @percy/cli stop

# Clean up
kill $PERCY_PID 2>/dev/null
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `PERCY_TOKEN` | Yes | Percy project token from your Percy dashboard |
| `KATALON_API_KEY` | Yes (for KRE) | Katalon API key for Runtime Engine license |
| `PERCY_SERVER_ADDRESS` | No | Percy CLI address (default: `http://localhost:5338`) |
| `PERCY_LOGLEVEL` | No | Set to `debug` for verbose logging |
