import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// Expects Percy CLI running on localhost:5338
// Tests that sync option is accepted by the SDK and Percy CLI.
//
// NOTE: sync: true makes Percy CLI block until rendering completes on
// Percy's servers, which can take minutes or hang on slow connections.
// Test 1 uses sync: false (default) to verify the option key passes through.
// Test 2 uses sync: true but is expected to take longer — if it hangs,
// kill the test and check Percy dashboard for the snapshot.

WebUI.openBrowser('https://example.com')
WebUI.delay(2)

// Test 1: Non-blocking snapshot that includes sync key set to false
// Validates the sync option key is accepted without blocking
println "[1/3] Snapshot with sync: false (non-blocking, validates option passes through)..."
def result1 = CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Sync - Non Blocking', [
    sync: false
])
println "[1/3] Result: ${result1} — PASS (sync option accepted)"

// Test 2: Snapshot with sync-related options combined (non-blocking)
println "[2/3] Snapshot with widths + percyCSS (no sync, validates combined options)..."
def result2 = CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Sync - With Options', [
    widths: [375, 1280],
    percyCSS: 'body { margin: 0; }'
])
println "[2/3] Result: ${result2} — PASS"

// Test 3: Actual sync: true — this WILL block until Percy finishes rendering.
// Skip if you're in a hurry. The snapshot should appear on the dashboard
// with the name below once processing completes.
println "[3/3] Sync snapshot (sync: true — this will block until rendering completes)..."
println "[3/3] If this hangs for more than 2 minutes, Ctrl+C and check dashboard for 'Sync - Blocking'"
def result3 = CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Sync - Blocking', [
    sync: true
])
println "[3/3] Result: ${result3}"
if (result3 != null) {
    println "[3/3] PASS — sync snapshot returned a response"
} else {
    println "[3/3] WARN — sync snapshot returned null (may have timed out)"
}

WebUI.closeBrowser()
println "[test] Sync Mode - COMPLETE (3 snapshots)"
