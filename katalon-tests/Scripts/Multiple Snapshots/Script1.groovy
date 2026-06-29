import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// Expects Percy CLI running on localhost:5338
// Tests taking multiple snapshots on the same page and across page navigations

WebUI.openBrowser('https://example.com')
WebUI.delay(2)

// Test 1 & 2: Two snapshots on same page with distinct names
println "[1/5] First snapshot on same page..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Multi - Same Page First')

println "[2/5] Second snapshot on same page..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Multi - Same Page Second')

// Test 3: Snapshot after DOM modification (same page, different state)
println "[3/5] Snapshot after DOM modification..."
WebUI.executeJavaScript("""
    document.querySelector('h1').textContent = 'Modified Title';
    document.querySelector('h1').style.color = 'blue';
""", null)
WebUI.delay(1)
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Multi - After DOM Change')

// Test 4: Navigate to a different page, take snapshot
println "[4/5] Snapshot on second page..."
WebUI.navigateToUrl('https://example.com/?page=2')
WebUI.delay(2)
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Multi - Second Page')

// Test 5: Navigate back, snapshot again
println "[5/5] Snapshot after navigating back..."
WebUI.navigateToUrl('https://example.com')
WebUI.delay(2)
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Multi - Back to First Page')

WebUI.closeBrowser()
println "[test] Multiple Snapshots - COMPLETE (5 snapshots)"
