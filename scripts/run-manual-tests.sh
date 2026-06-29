#!/bin/bash
#
# Percy Katalon Plugin — Manual Test Orchestrator
#
# Automates Percy CLI lifecycle and guides you through running tests
# manually in Katalon Studio. No other tools or assistance needed.
#
# Prerequisites:
#   - Node.js installed (for Percy CLI via npx)
#   - Katalon Studio installed with percy-katalon-tests project open
#   - Plugin JAR built and copied to katalon-tests/Plugins/
#
# Usage:
#   ./scripts/run-manual-tests.sh
#
#   Or with env vars pre-set:
#   PERCY_TOKEN=xxx BROWSERSTACK_USERNAME=yyy BROWSERSTACK_ACCESS_KEY=zzz ./scripts/run-manual-tests.sh
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
KATALON_TESTS_DIR="${PROJECT_DIR}/katalon-tests"
REGIONS_SCRIPT="${KATALON_TESTS_DIR}/Scripts/All Regions Test/Script1.groovy"

NPX="${NPX:-/opt/homebrew/bin/npx}"
PERCY_PORT=5338
PERCY_URL="http://localhost:${PERCY_PORT}"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

# -------------------------------------------------------------------
# Collect credentials
# -------------------------------------------------------------------

collect_credentials() {
    if [ -z "${PERCY_TOKEN:-}" ]; then
        echo -ne "  ${CYAN}PERCY_TOKEN:${NC} "
        read -r PERCY_TOKEN
        export PERCY_TOKEN
    else
        echo -e "  PERCY_TOKEN: ${DIM}(set)${NC}"
    fi

    if [ -z "${BROWSERSTACK_USERNAME:-}" ]; then
        echo -ne "  ${CYAN}BROWSERSTACK_USERNAME:${NC} "
        read -r BROWSERSTACK_USERNAME
        export BROWSERSTACK_USERNAME
    else
        echo -e "  BROWSERSTACK_USERNAME: ${DIM}(set)${NC}"
    fi

    if [ -z "${BROWSERSTACK_ACCESS_KEY:-}" ]; then
        echo -ne "  ${CYAN}BROWSERSTACK_ACCESS_KEY:${NC} "
        read -r BROWSERSTACK_ACCESS_KEY
        export BROWSERSTACK_ACCESS_KEY
    else
        echo -e "  BROWSERSTACK_ACCESS_KEY: ${DIM}(set)${NC}"
    fi
    echo ""
}

# -------------------------------------------------------------------
# Percy CLI helpers
# -------------------------------------------------------------------

cleanup_percy() {
    lsof -ti:${PERCY_PORT} 2>/dev/null | xargs kill -9 2>/dev/null || true
    rm -f ~/.percy/agent-${PERCY_PORT}.lock 2>/dev/null || true
    sleep 2
}

start_percy() {
    cleanup_percy
    $NPX @percy/cli exec:start >/dev/null 2>&1 &
    echo -n "  Starting Percy CLI"
    for i in $(seq 1 60); do
        if curl -s ${PERCY_URL}/percy/healthcheck >/dev/null 2>&1; then
            BUILD_ID=$(curl -s ${PERCY_URL}/percy/healthcheck | python3 -c "import sys,json; print(json.loads(sys.stdin.read())['build']['id'])")
            BUILD_URL=$(curl -s ${PERCY_URL}/percy/healthcheck | python3 -c "import sys,json; print(json.loads(sys.stdin.read())['build']['url'])")
            echo ""
            echo -e "  ${GREEN}✓ Percy CLI ready${NC}"
            echo "  Build: ${BUILD_ID}"
            echo "  URL:   ${BUILD_URL}"
            return 0
        fi
        echo -n "."
        sleep 1
    done
    echo ""
    echo -e "  ${RED}✗ Percy CLI failed to start${NC}"
    return 1
}

stop_percy() {
    echo -n "  Stopping Percy CLI..."
    $NPX @percy/cli exec:stop >/dev/null 2>&1 || true
    sleep 2
    echo -e " ${GREEN}done${NC}"
}

pause() {
    echo ""
    echo -e "  ${CYAN}▶ $1${NC}"
    echo ""
    read -p "  Press ENTER when done... "
    echo ""
}

# -------------------------------------------------------------------
# Build JAR
# -------------------------------------------------------------------

build_jar() {
    echo -e "  Building plugin JAR..."
    JAVA_HOME="${JAVA_HOME:-/Users/sudiptasen/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home}"
    export JAVA_HOME

    # Ensure Gradle wrapper JAR exists
    if [ ! -f "${PROJECT_DIR}/gradle/wrapper/gradle-wrapper.jar" ]; then
        echo -e "  ${DIM}Generating Gradle wrapper...${NC}"
        (cd "$PROJECT_DIR" && gradle wrapper -q 2>&1) || true
    fi

    (cd "$PROJECT_DIR" && ./gradlew shadowJar -q 2>&1) || { echo -e "  ${RED}Build failed${NC}"; exit 1; }

    JAR_FILE=$(ls -1 "${PROJECT_DIR}/build/libs/percy-katalon-"*.jar 2>/dev/null | head -1)
    if [ -z "$JAR_FILE" ]; then
        echo -e "  ${RED}JAR not found${NC}"
        exit 1
    fi

    mkdir -p "${KATALON_TESTS_DIR}/Plugins"
    cp "$JAR_FILE" "${KATALON_TESTS_DIR}/Plugins/"
    echo -e "  ${GREEN}✓ JAR built and copied to katalon-tests/Plugins/${NC}"
}

# -------------------------------------------------------------------
# Run unit tests
# -------------------------------------------------------------------

run_unit_tests() {
    echo -e "  Running unit tests..."
    JAVA_HOME="${JAVA_HOME:-/Users/sudiptasen/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home}"
    export JAVA_HOME
    local output
    output=$(cd "$PROJECT_DIR" && ./gradlew test 2>&1)
    if echo "$output" | grep -q "BUILD SUCCESSFUL"; then
        local count
        count=$(echo "$output" | grep -oE '[0-9]+ tests completed' | head -1 || echo "all")
        echo -e "  ${GREEN}✓ Unit tests passed (${count})${NC}"
    else
        echo -e "  ${RED}✗ Unit tests failed${NC}"
        echo "$output" | grep -E "FAILED|Error" | head -10
        echo ""
        read -p "  Continue anyway? (y/N) " yn
        if [[ ! "$yn" =~ ^[Yy] ]]; then
            exit 1
        fi
    fi
}

# -------------------------------------------------------------------
# Generate Katalon .tc metadata files
# -------------------------------------------------------------------

generate_test_cases() {
    local tc_dir="${KATALON_TESTS_DIR}/Test Cases"
    local scripts_dir="${KATALON_TESTS_DIR}/Scripts"
    local guid_counter=10
    local generated=0

    mkdir -p "$tc_dir"

    for script_dir in "$scripts_dir"/*/; do
        local name
        name=$(basename "$script_dir")
        local tc_file="${tc_dir}/${name}.tc"

        if [ ! -f "$tc_file" ] && [ -f "${script_dir}Script1.groovy" ]; then
            local guid
            guid=$(printf "00000000-0000-0000-0000-%012d" $guid_counter)
            cat > "$tc_file" <<TCEOF
<?xml version="1.0" encoding="UTF-8"?>
<TestCaseEntity>
   <description>${name}</description>
   <name>${name}</name>
   <tag></tag>
   <comment></comment>
   <testCaseGuid>${guid}</testCaseGuid>
</TestCaseEntity>
TCEOF
            generated=$((generated + 1))
            guid_counter=$((guid_counter + 1))
        else
            guid_counter=$((guid_counter + 1))
        fi
    done

    if [ $generated -gt 0 ]; then
        echo -e "  ${GREEN}✓ Generated ${generated} missing .tc file(s)${NC}"
    else
        echo -e "  ${GREEN}✓ All .tc files present${NC}"
    fi
}

# -------------------------------------------------------------------
# Main
# -------------------------------------------------------------------

echo ""
echo -e "${BOLD}════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}   Percy Katalon Plugin — Manual Test Orchestrator${NC}"
echo -e "${BOLD}════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${DIM}This script automates Percy CLI lifecycle and guides you${NC}"
echo -e "  ${DIM}through running tests in Katalon Studio.${NC}"
echo ""

# Step 0: Credentials
echo -e "${YELLOW}━━━ STEP 0: Credentials ━━━${NC}"
echo ""
collect_credentials

# Step 1: Build + Test + Setup
echo -e "${YELLOW}━━━ STEP 1: Build, Test & Setup ━━━${NC}"
echo ""
build_jar
echo ""
run_unit_tests
echo ""
generate_test_cases
echo ""

# Step 2: Open Katalon
echo -e "${YELLOW}━━━ STEP 2: Open Katalon Studio ━━━${NC}"
echo ""
echo "  Open Katalon Studio and load the project at:"
echo -e "  ${BOLD}${KATALON_TESTS_DIR}${NC}"
echo ""
echo "  (File > Open Project > select the katalon-tests folder)"
pause "Open the project in Katalon Studio, then press ENTER"

# ==================================================================
# PHASE 1: Baseline Build (28 snapshots)
# ==================================================================
echo -e "${YELLOW}━━━ PHASE 1: Baseline Build (29 snapshots) ━━━${NC}"
echo ""

# Set All Regions Test to baseline mode
if [ -f "$REGIONS_SCRIPT" ]; then
    sed -i '' "s/boolean diffRun = .*/boolean diffRun = false  \/\/ BASELINE MODE/" "$REGIONS_SCRIPT"
    echo -e "  ${GREEN}✓ All Regions Test set to BASELINE mode${NC}"
fi
echo ""

start_percy
BASELINE_BUILD_ID=$BUILD_ID
BASELINE_BUILD_URL=$BUILD_URL
echo ""

echo "  Run these 7 tests in Katalon Studio (in order):"
echo ""
echo -e "  ${BOLD}1. Dual Channel Logging${NC}"
echo -e "     ${DIM}Expected: [1/3] null, [2/3] snapshot, [3/3] null (screenshot on web mode)${NC}"
echo ""
echo -e "  ${BOLD}2. Responsive Capture${NC}"
echo -e "     ${DIM}Expected: [1/3] [2/3] [3/3] snapshots taken${NC}"
echo ""
echo -e "  ${BOLD}3. All Regions Test${NC}"
echo -e "     ${DIM}Expected: Mode: BASELINE, [1/8] through [8/8] Done${NC}"
echo ""
echo -e "  ${BOLD}4. Snapshot Options${NC}"
echo -e "     ${DIM}Expected: [1/7] through [7/7] snapshots with various options${NC}"
echo ""
echo -e "  ${BOLD}5. Multiple Snapshots${NC}"
echo -e "     ${DIM}Expected: [1/5] through [5/5] snapshots on same/different pages${NC}"
echo ""
echo -e "  ${BOLD}6. Sync Mode${NC}"
echo -e "     ${DIM}Expected: [1/3] [2/3] fast, [3/3] blocks until render completes (may take 1-2 min)${NC}"
echo ""
echo -e "  ${BOLD}7. Discovery Options${NC}"
echo -e "     ${DIM}Expected: [1/2] [2/2] snapshots with discovery options${NC}"
echo ""

pause "Run all 7 tests in Katalon Studio, then press ENTER"

stop_percy
echo ""

# ==================================================================
# PHASE 2: Approve Baseline
# ==================================================================
echo -e "${YELLOW}━━━ PHASE 2: Approve Baseline ━━━${NC}"
echo ""
echo -n "  Waiting for build ${BASELINE_BUILD_ID} to finish rendering"
$NPX @percy/cli build:wait --build "$BASELINE_BUILD_ID" >/dev/null 2>&1 || true
echo -e " ${GREEN}done${NC}"

echo -n "  Approving build..."
APPROVE_OUTPUT=$($NPX @percy/cli build:approve "$BASELINE_BUILD_ID" 2>&1)
if echo "$APPROVE_OUTPUT" | grep -q "approved successfully"; then
    echo -e " ${GREEN}✓ approved${NC}"
else
    echo -e " ${RED}✗ failed${NC}"
    echo "  $APPROVE_OUTPUT"
fi
echo ""

# ==================================================================
# PHASE 3: Verify Baseline
# ==================================================================
echo -e "${YELLOW}━━━ PHASE 3: Verify Baseline on Dashboard ━━━${NC}"
echo ""
echo "  Dashboard: ${BASELINE_BUILD_URL}"
echo ""
echo "  Verify 29 snapshots:"
echo ""
echo "    ${BOLD}Dual Channel Logging (1 snapshot):${NC}"
echo "    • Log - Valid Snapshot"
echo ""
echo "    ${BOLD}Responsive Capture (3 snapshots):${NC}"
echo "    • Responsive - Explicit Widths"
echo "    • Responsive - Client Side"
echo "    • Responsive - MinHeight"
echo ""
echo "    ${BOLD}All Regions Test (8 snapshots):${NC}"
echo "    • Region - Ignore CSS"
echo "    • Region - Ignore XPath"
echo "    • Region - BoundingBox"
echo "    • Region - Standard"
echo "    • Region - Intelliignore"
echo "    • Region - Padding Integer"
echo "    • Region - Padding Map"
echo "    • Region - Multiple Combined"
echo ""
echo "    ${BOLD}Snapshot Options (7 snapshots):${NC}"
echo "    • Options - percyCSS              (purple background)"
echo "    • Options - domTransformation     (title text changed)"
echo "    • Options - scope                 (scoped to div)"
echo "    • Options - JS and Layout         (JS enabled, layout mode)"
echo "    • Options - labels                (labeled smoke,regression)"
echo "    • Options - Combined              (multiple options together)"
echo "    • Options - disableShadowDom      (Shadow DOM disabled)"
echo ""
echo "    ${BOLD}Multiple Snapshots (5 snapshots):${NC}"
echo "    • Multi - Same Page First"
echo "    • Multi - Same Page Second"
echo "    • Multi - After DOM Change        (title changed to blue)"
echo "    • Multi - Second Page"
echo "    • Multi - Back to First Page"
echo ""
echo "    ${BOLD}Sync Mode (3 snapshots):${NC}"
echo "    • Sync - Non Blocking             (sync: false, validates option key)"
echo "    • Sync - With Options             (widths + percyCSS combined)"
echo "    • Sync - Blocking                 (sync: true, blocks until rendered)"
echo ""
echo "    ${BOLD}Discovery Options (2 snapshots):${NC}"
echo "    • Discovery - Allowed Hostnames"
echo "    • Discovery - Combined            (discovery + widths + CSS)"
echo ""

pause "Verify baseline on dashboard, then press ENTER to continue"

# ==================================================================
# PHASE 4: Diff Build with Regions
# ==================================================================
echo -e "${YELLOW}━━━ PHASE 4: Diff Build (8 region snapshots) ━━━${NC}"
echo ""

# Switch to diff mode
if [ -f "$REGIONS_SCRIPT" ]; then
    sed -i '' "s/boolean diffRun = .*/boolean diffRun = true  \/\/ DIFF MODE/" "$REGIONS_SCRIPT"
    echo -e "  ${GREEN}✓ All Regions Test switched to DIFF mode${NC}"
fi
echo ""

start_percy
DIFF_BUILD_ID=$BUILD_ID
DIFF_BUILD_URL=$BUILD_URL
echo ""

echo -e "  Run ${BOLD}only 'All Regions Test'${NC} in Katalon Studio"
echo -e "  ${DIM}Expected: Mode: DIFF, [1/8] through [8/8] with regions${NC}"
echo ""

pause "Run 'All Regions Test' in Katalon Studio, then press ENTER"

stop_percy
echo ""

# Reset to baseline mode
if [ -f "$REGIONS_SCRIPT" ]; then
    sed -i '' "s/boolean diffRun = .*/boolean diffRun = false  \/\/ BASELINE MODE/" "$REGIONS_SCRIPT"
    echo -e "  ${GREEN}✓ All Regions Test reset to BASELINE mode${NC}"
fi
echo ""

# ==================================================================
# PHASE 5: Verify Regions
# ==================================================================
echo -e "${YELLOW}━━━ PHASE 5: Verify Regions on Dashboard ━━━${NC}"
echo ""
echo "  Diff build: ${DIFF_BUILD_URL}"
echo ""
echo "  Verify each snapshot:"
echo "    ┌───┬──────────────────────────────┬──────────────────────────────────┐"
echo "    │ # │ Snapshot                     │ Expected                         │"
echo "    ├───┼──────────────────────────────┼──────────────────────────────────┤"
echo "    │ 1 │ Region - Ignore CSS          │ h1 diff IGNORED                  │"
echo "    │ 2 │ Region - Ignore XPath        │ h1 diff IGNORED                  │"
echo "    │ 3 │ Region - BoundingBox         │ Top 80px IGNORED                 │"
echo "    │ 4 │ Region - Standard            │ p diff DETECTED (sensitivity 2)  │"
echo "    │ 5 │ Region - Intelliignore       │ p diff likely DETECTED (AI)      │"
echo "    │ 6 │ Region - Padding Integer     │ h1 + 10px all sides IGNORED      │"
echo "    │ 7 │ Region - Padding Map         │ h1 + asymmetric pad IGNORED      │"
echo "    │ 8 │ Region - Multiple Combined   │ h1 IGN, p DETECT, link IGN       │"
echo "    └───┴──────────────────────────────┴──────────────────────────────────┘"
echo ""

pause "Verify all regions on dashboard, then press ENTER for summary"

# ==================================================================
# SUMMARY
# ==================================================================
echo ""
echo -e "${BOLD}════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}   TEST SUMMARY${NC}"
echo -e "${BOLD}════════════════════════════════════════════════════════${NC}"
echo ""
echo "  Builds:"
echo "    Baseline : ${BASELINE_BUILD_ID} (approved)"
echo "    Diff     : ${DIFF_BUILD_ID}"
echo ""
echo "  Dashboard:"
echo "    Baseline : ${BASELINE_BUILD_URL}"
echo "    Diff     : ${DIFF_BUILD_URL}"
echo ""
echo "  Tests Executed (8 Katalon runs, 39 scenarios):"
echo "    ✓ Dual Channel Logging    — 3 scenarios (null driver, valid snapshot, wrong mode)"
echo "    ✓ Responsive Capture      — 3 scenarios (explicit widths, client-side, minHeight)"
echo "    ✓ All Regions (baseline)  — 8 baseline snapshots"
echo "    ✓ Snapshot Options        — 7 scenarios (percyCSS, domTransformation, scope, JS, labels, combined, shadowDom)"
echo "    ✓ Multiple Snapshots      — 5 scenarios (same page, DOM change, navigation, back)"
echo "    ✓ Sync Mode               — 3 scenarios (non-blocking, with options, blocking sync:true)"
echo "    ✓ Discovery Options       — 2 scenarios (allowedHostnames, combined)"
echo "    ✓ All Regions (diff)      — 8 diff snapshots with regions"
echo ""
echo -e "  ${GREEN}Done. Review the dashboard links above to confirm all tests passed.${NC}"
echo ""
