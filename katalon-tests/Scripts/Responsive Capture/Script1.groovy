import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// Expects Percy CLI running on localhost:5338

WebUI.openBrowser('https://example.com')
WebUI.delay(2)

// Test 1: Explicit widths
println "[1/3] Snapshot with explicit widths [320, 375, 768, 1024, 1280]..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Responsive - Explicit Widths', [
    widths: [320, 375, 768, 1024, 1280]
])

// Test 2: Responsive snapshot capture mode (client-side resize)
println "[2/3] Responsive snapshot capture mode..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Responsive - Client Side', [
    responsiveSnapshotCapture: true,
    widths: [375, 768, 1280]
])

// Test 3: Responsive with minHeight
println "[3/3] Responsive with minHeight 1200..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Responsive - MinHeight', [
    widths: [375, 1280],
    minHeight: 1200
])

WebUI.closeBrowser()
println "[test] Responsive Capture - COMPLETE (3 snapshots)"
