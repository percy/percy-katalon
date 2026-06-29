import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// Expects Percy CLI running on localhost:5338
// Tests discovery options for cross-origin iframe support

WebUI.openBrowser('https://example.com')
WebUI.delay(2)

// Test 1: Discovery with allowedHostnames
println "[1/2] Snapshot with discovery allowedHostnames..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Discovery - Allowed Hostnames', [
    discovery: [allowedHostnames: ['cdn.example.com', 'assets.example.com']]
])

// Test 2: Discovery combined with other options
println "[2/2] Snapshot with discovery + widths + percyCSS..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Discovery - Combined', [
    discovery: [allowedHostnames: ['cdn.example.com']],
    widths: [375, 1280],
    percyCSS: 'iframe { border: none; }'
])

WebUI.closeBrowser()
println "[test] Discovery Options - COMPLETE (2 snapshots)"
