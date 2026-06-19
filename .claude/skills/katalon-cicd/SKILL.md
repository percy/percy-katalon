---
name: katalon-cicd
description: "Set up Percy visual testing with Katalon in CI/CD pipelines. Covers GitHub Actions, Jenkins, Buildkite, Docker-based execution, and the PERCY_SERVER_ADDRESS fallback for environments without Katalon Runtime Engine. Use when someone says 'set up percy in CI', 'percy katalon github actions', 'percy jenkins', 'percy buildkite', 'katalon CI/CD', or needs help with automated pipeline configuration."
---

# Percy Katalon Plugin — CI/CD Integration Guide

Help the user set up Percy visual testing with Katalon in their CI/CD pipeline.

## Assess the User's CI System

Ask which CI system they use:
1. **GitHub Actions**
2. **Jenkins**
3. **Buildkite**
4. **Other** (generic approach)

Also ask:
- Do they have a **Katalon Runtime Engine (KRE) license**? (`katalonc` binary)
- If not, they'll need the **PERCY_SERVER_ADDRESS fallback** approach

## Required Secrets/Credentials

These must be configured in the CI system's secrets manager:

| Secret | Required | Description |
|--------|----------|-------------|
| `PERCY_TOKEN` | Yes | Percy project token from percy.io |
| `KATALON_API_KEY` | For KRE | Katalon Runtime Engine license key |
| `BROWSERSTACK_USERNAME` | For auto-approve | BrowserStack credentials |
| `BROWSERSTACK_ACCESS_KEY` | For auto-approve | BrowserStack credentials |

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

      - name: Run Percy visual tests
        env:
          PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}
          KATALON_API_KEY: ${{ secrets.KATALON_API_KEY }}
        run: |
          npx @percy/cli exec -- katalonc \
            -noSplash \
            -runMode=console \
            -projectPath="$(pwd)/your-project.prj" \
            -testSuitePath="Test Suites/VisualTests" \
            -browserType="Chrome (headless)" \
            -apiKey="$KATALON_API_KEY"
```

### With Katalon Docker Image

```yaml
jobs:
  visual-tests:
    runs-on: ubuntu-latest
    container:
      image: katalonstudio/katalon:latest
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
                      -testSuitePath="Test Suites/VisualTests" \
                      -browserType="Chrome (headless)" \
                      -apiKey="$KATALON_API_KEY"
                '''
            }
        }
    }
}
```

### Jenkins with Docker Agent

```groovy
pipeline {
    agent {
        docker {
            image 'katalonstudio/katalon:latest'
            args '-v /tmp:/tmp'
        }
    }
    // ... same stages as above
}
```

## Buildkite

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
    plugins:
      - docker#v5.10.0:
          image: "katalonstudio/katalon:latest"
```

## Without KRE License (PERCY_SERVER_ADDRESS Fallback)

If the user doesn't have a Katalon Runtime Engine license:

```bash
# Start Percy CLI in the background
PERCY_TOKEN=$PERCY_TOKEN npx @percy/cli exec:start &
PERCY_PID=$!

# Wait for Percy CLI to be ready
sleep 10

# Set the server address for the Katalon process
export PERCY_SERVER_ADDRESS=http://localhost:5338

# Run Katalon tests however available
# (Docker image, custom runner, etc.)
your-katalon-command-here

# Stop Percy CLI (finalizes the build)
npx @percy/cli exec:stop

# Clean up
kill $PERCY_PID 2>/dev/null
```

## Generic CI Template

For any CI system:

```bash
#!/bin/bash
set -e

# Prerequisites: PERCY_TOKEN and KATALON_API_KEY set as env vars

# Install Percy CLI
npm install -g @percy/cli

# Run tests with Percy
npx @percy/cli exec -- katalonc \
  -noSplash \
  -runMode=console \
  -projectPath="$(pwd)/your-project.prj" \
  -testSuitePath="Test Suites/VisualTests" \
  -browserType="Chrome (headless)" \
  -apiKey="$KATALON_API_KEY"

# Percy build URL is printed at the end
```

## Auto-Approve Baseline in CI

For automated pipelines that need to auto-approve the first build:

```bash
export BROWSERSTACK_USERNAME=<username>
export BROWSERSTACK_ACCESS_KEY=<key>

# Run tests
npx @percy/cli exec -- katalonc ...

# Get build ID and approve
BUILD_ID=$(npx @percy/cli build:id)
npx @percy/cli build:wait --build $BUILD_ID
npx @percy/cli build:approve $BUILD_ID
```

## Environment Variables Reference

| Variable | Required | Description |
|----------|----------|-------------|
| `PERCY_TOKEN` | Yes | Percy project token |
| `KATALON_API_KEY` | For KRE | Katalon Runtime Engine license |
| `PERCY_SERVER_ADDRESS` | No | Percy CLI address (default: `http://localhost:5338`) |
| `PERCY_LOGLEVEL` | No | `debug` for verbose logging |
| `BROWSERSTACK_USERNAME` | For approve | BrowserStack username |
| `BROWSERSTACK_ACCESS_KEY` | For approve | BrowserStack access key |
