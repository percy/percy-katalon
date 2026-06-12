import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// Open a browser and navigate to a test page
WebUI.openBrowser('')
WebUI.navigateToUrl('https://example.com')

// Take a Percy snapshot -- this captures the DOM and sends it to Percy CLI
// Percy CLI renders it on all configured browsers (Chrome, Firefox, Edge, Safari)
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Example Page')

// Take another snapshot with options
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Example Page - Mobile', [
    widths: [375, 768],
    percyCSS: 'header { display: none; }'
])

WebUI.closeBrowser()
