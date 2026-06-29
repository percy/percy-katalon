import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// Expects Percy CLI running on localhost:5338
// Tests all snapshot options: percyCSS, domTransformation, scope, widths,
// enableJavaScript, enableLayout, labels, minHeight

WebUI.openBrowser('https://example.com')
WebUI.delay(2)

// Test 1: percyCSS — inject custom CSS in Percy rendering only
println "[1/7] Snapshot with percyCSS..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Options - percyCSS', [
    percyCSS: 'body { background-color: purple; } h1 { color: white; }'
])

// Test 2: domTransformation — transform DOM before capture
println "[2/7] Snapshot with domTransformation..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Options - domTransformation', [
    domTransformation: "(documentElement) => documentElement.querySelector('h1').textContent = 'Transformed Title';"
])

// Test 3: scope — limit snapshot to a section of the page
println "[3/7] Snapshot with scope..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Options - scope', [
    scope: 'div'
])

// Test 4: enableJavaScript + enableLayout
println "[4/7] Snapshot with enableJavaScript and enableLayout..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Options - JS and Layout', [
    enableJavaScript: true,
    enableLayout: true
])

// Test 5: labels — categorize snapshot
println "[5/7] Snapshot with labels..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Options - labels', [
    labels: 'smoke,regression,options-test'
])

// Test 6: Combined options — multiple options together
println "[6/7] Snapshot with combined options..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Options - Combined', [
    widths: [768, 992, 1200],
    minHeight: 800,
    percyCSS: 'header { display: none; }',
    scope: 'body',
    enableJavaScript: true,
    labels: 'combined-test'
])

// Test 7: disableShadowDom
println "[7/7] Snapshot with disableShadowDom..."
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Options - disableShadowDom', [
    disableShadowDom: true
])

WebUI.closeBrowser()
println "[test] Snapshot Options - COMPLETE (7 snapshots)"
