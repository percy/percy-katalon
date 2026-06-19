import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// This test expects Percy CLI to already be running on localhost:5338

WebUI.openBrowser('https://example.com')
WebUI.delay(2)

// Check if this is a diff run (set PERCY_DIFF_RUN=true in environment for diff mode)
boolean diffRun = false  // BASELINE MODE
println "[test] Mode: ${diffRun ? 'DIFF (changes + regions)' : 'BASELINE (clean page)'}"

if (diffRun) {
    WebUI.executeJavaScript("""
        document.querySelector('h1').style.color = 'red';
        document.querySelector('h1').textContent = 'Example Domain - CHANGED';
        document.querySelector('p').style.backgroundColor = '#ffffcc';
        document.querySelector('p').style.fontSize = '20px';
        document.querySelector('p').textContent = 'This paragraph has been modified for the region test.';
        var links = document.querySelectorAll('a');
        if (links.length > 0) { links[0].style.color = 'green'; links[0].textContent = 'Modified Link'; }
    """, null)
    WebUI.delay(1)
}

// --- 1: Ignore region (CSS selector) ---
if (diffRun) {
    def r = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([elementCSS: 'h1', algorithm: 'ignore'])
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Ignore CSS', [regions: [r]])
} else {
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Ignore CSS')
}
println "[1/8] Done"

// --- 2: Ignore region (XPath) ---
if (diffRun) {
    def r = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([elementXpath: '//h1', algorithm: 'ignore'])
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Ignore XPath', [regions: [r]])
} else {
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Ignore XPath')
}
println "[2/8] Done"

// --- 3: Ignore region (bounding box) ---
if (diffRun) {
    def r = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([boundingBox: [x: 0, y: 0, width: 800, height: 80], algorithm: 'ignore'])
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - BoundingBox', [regions: [r]])
} else {
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - BoundingBox')
}
println "[3/8] Done"

// --- 4: Standard region (diffSensitivity) ---
if (diffRun) {
    def r = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([elementCSS: 'p', algorithm: 'standard', diffSensitivity: 2])
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Standard', [regions: [r]])
} else {
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Standard')
}
println "[4/8] Done"

// --- 5: Intelliignore region ---
if (diffRun) {
    def r = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([elementCSS: 'p', algorithm: 'intelliignore', adsEnabled: true, bannersEnabled: true])
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Intelliignore', [regions: [r]])
} else {
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Intelliignore')
}
println "[5/8] Done"

// --- 6: Padding as integer ---
if (diffRun) {
    def r = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([elementCSS: 'h1', algorithm: 'ignore', padding: 10])
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Padding Integer', [regions: [r]])
} else {
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Padding Integer')
}
println "[6/8] Done"

// --- 7: Padding as map ---
if (diffRun) {
    def r = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([elementCSS: 'h1', algorithm: 'ignore', padding: [top: 5, bottom: 10, left: 15, right: 20]])
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Padding Map', [regions: [r]])
} else {
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Padding Map')
}
println "[7/8] Done"

// --- 8: Multiple regions combined ---
if (diffRun) {
    def r1 = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([elementCSS: 'h1', algorithm: 'ignore'])
    def r2 = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([elementCSS: 'p', algorithm: 'standard', diffSensitivity: 3, diffIgnoreThreshold: 0.1])
    def r3 = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([elementCSS: 'a', algorithm: 'ignore'])
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Multiple Combined', [regions: [r1, r2, r3]])
} else {
    CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Region - Multiple Combined')
}
println "[8/8] Done"

WebUI.closeBrowser()
println "[test] COMPLETE - 8 snapshots (${diffRun ? 'DIFF' : 'BASELINE'} mode)"
