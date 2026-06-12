#!/bin/bash
#
# Integration test for Percy Katalon Plugin
#
# Prerequisites:
#   - Node.js installed (for Percy CLI)
#   - PERCY_TOKEN env var set
#   - Katalon Runtime Engine (katalonc) installed OR Katalon Studio IDE
#
# Usage:
#   ./scripts/run-integration-test.sh
#
# If you don't have a KRE license, use the PERCY_SERVER_ADDRESS fallback:
#   1. Start Percy CLI: PERCY_TOKEN=<token> npx @percy/cli start
#   2. Set env: export PERCY_SERVER_ADDRESS=http://localhost:5338
#   3. Run your Katalon test manually from the IDE
#   4. Stop Percy CLI: npx @percy/cli stop

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
SAMPLE_PROJECT="${PROJECT_DIR}/sample-project"

# Check prerequisites
if ! command -v npx &>/dev/null; then
    echo "Error: npx not found. Install Node.js first."
    exit 1
fi

if [ -z "${PERCY_TOKEN:-}" ]; then
    echo "Error: PERCY_TOKEN environment variable not set."
    echo "Get your token from https://percy.io and set it:"
    echo "  export PERCY_TOKEN=<your-token>"
    exit 1
fi

# Build the plugin JAR first
echo "Building Percy Katalon plugin..."
cd "${PROJECT_DIR}"
./gradlew shadowJar

# Copy the plugin JAR to the sample project
PLUGIN_JAR=$(ls -1 build/libs/percy-katalon-*.jar | head -1)
mkdir -p "${SAMPLE_PROJECT}/Plugins"
cp "${PLUGIN_JAR}" "${SAMPLE_PROJECT}/Plugins/"
echo "Plugin JAR copied to sample-project/Plugins/"

# Option 1: Run with katalonc (requires KRE license)
if command -v katalonc &>/dev/null; then
    echo "Running integration test with katalonc..."
    npx @percy/cli exec -- katalonc \
        -noSplash \
        -runMode=console \
        -projectPath="${SAMPLE_PROJECT}/percy-katalon-sample.prj" \
        -testSuitePath="Test Suites/PercySuite" \
        -browserType="Chrome (headless)" \
        -apiKey="${KATALON_API_KEY:-}"
    echo "Integration test complete. Check Percy dashboard for build results."
else
    echo ""
    echo "katalonc not found. To run the integration test:"
    echo ""
    echo "Option A: Install Katalon Runtime Engine and re-run this script"
    echo ""
    echo "Option B: Use PERCY_SERVER_ADDRESS fallback:"
    echo "  1. Start Percy CLI:  PERCY_TOKEN=${PERCY_TOKEN} npx @percy/cli start"
    echo "  2. Open sample-project/ in Katalon Studio IDE"
    echo "  3. Run 'Test Suites/PercySuite'"
    echo "  4. Stop Percy CLI:   npx @percy/cli stop"
    echo "  5. Check Percy dashboard for build results"
fi
