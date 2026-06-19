import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// Expects Percy CLI running on localhost:5338

// Test 1: Null driver (no browser open) — should log to both Katalon + Percy CLI
println "[1/3] Snapshot without browser (null driver)..."
def result1 = CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Log - Null Driver')
println "[1/3] Result: ${result1} (expected: null)"

// Test 2: Valid snapshot
WebUI.openBrowser('https://example.com')
WebUI.delay(2)
println "[2/3] Valid snapshot..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Log - Valid Snapshot')
println "[2/3] Done"

// Test 3: percyScreenshot in web mode (should fail gracefully, log error)
println "[3/3] Screenshot in web mode (wrong mode, should fail gracefully)..."
def result3 = CustomKeywords.'com.percy.katalon.PercyKeywords.percyScreenshot'('Log - Wrong Mode')
println "[3/3] Result: ${result3} (expected: null)"

WebUI.closeBrowser()
println "[test] Dual Channel Logging - COMPLETE"
println "Check Percy CLI terminal for log messages from tests 1 and 3"
