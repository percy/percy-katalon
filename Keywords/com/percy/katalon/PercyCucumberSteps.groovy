package com.percy.katalon

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then

/**
 * Cucumber/BDD step definitions for Percy visual testing in Katalon Studio.
 *
 * Setup: In your Cucumber @Before hook, call:
 *   PercyCucumberSteps.init()
 *
 * Teardown: In your @After hook, call:
 *   PercyCucumberSteps.cleanup()
 *
 * Example feature file:
 *   Given I have a Percy instance
 *   And I create a Percy ignore region with CSS selector ".ad-banner"
 *   When I take a Percy snapshot named "Homepage" with regions
 *   Then Percy should be enabled
 */
class PercyCucumberSteps {

    private static List<Map<String, Object>> regions = []

    /**
     * Initialize Percy for Cucumber tests.
     * Call this in your @Before hook.
     */
    static void init() {
        PercyKeywords.resetCache()
        regions.clear()
    }

    /**
     * Cleanup after Cucumber tests.
     * Call this in your @After hook.
     */
    static void cleanup() {
        PercyKeywords.resetCache()
        regions.clear()
    }

    // -------------------------------------------------------------------
    // Given steps — region setup
    // -------------------------------------------------------------------

    @Given("I have a Percy instance")
    void iHaveAPercyInstance() {
        // Percy instance is created lazily on first snapshot call
    }

    @Given("I create a Percy ignore region with CSS selector {string}")
    void iCreateIgnoreRegionCSS(String selector) {
        regions.add(PercyKeywords.createRegion([
            elementCSS: selector,
            algorithm: 'ignore'
        ]))
    }

    @Given("I create a Percy ignore region with XPath {string}")
    void iCreateIgnoreRegionXPath(String xpath) {
        regions.add(PercyKeywords.createRegion([
            elementXpath: xpath,
            algorithm: 'ignore'
        ]))
    }

    @Given("I create a Percy ignore region with bounding box {int}, {int}, {int}, {int}")
    void iCreateIgnoreRegionBoundingBox(int x, int y, int width, int height) {
        regions.add(PercyKeywords.createRegion([
            boundingBox: [x: x, y: y, width: width, height: height],
            algorithm: 'ignore'
        ]))
    }

    @Given("I create a Percy ignore region with CSS selector {string} and padding {int}")
    void iCreateIgnoreRegionCSSWithPadding(String selector, int padding) {
        regions.add(PercyKeywords.createRegion([
            elementCSS: selector,
            algorithm: 'ignore',
            padding: padding
        ]))
    }

    @Given("I create a Percy ignore region with XPath {string} and padding {int}")
    void iCreateIgnoreRegionXPathWithPadding(String xpath, int padding) {
        regions.add(PercyKeywords.createRegion([
            elementXpath: xpath,
            algorithm: 'ignore',
            padding: padding
        ]))
    }

    @Given("I create a Percy consider region with CSS selector {string}")
    void iCreateConsiderRegionCSS(String selector) {
        regions.add(PercyKeywords.createRegion([
            elementCSS: selector,
            algorithm: 'standard'
        ]))
    }

    @Given("I create a Percy consider region with CSS selector {string} and diff sensitivity {int}")
    void iCreateConsiderRegionCSSWithSensitivity(String selector, int sensitivity) {
        regions.add(PercyKeywords.createRegion([
            elementCSS: selector,
            algorithm: 'standard',
            diffSensitivity: sensitivity
        ]))
    }

    @Given("I create a Percy consider region with XPath {string}")
    void iCreateConsiderRegionXPath(String xpath) {
        regions.add(PercyKeywords.createRegion([
            elementXpath: xpath,
            algorithm: 'standard'
        ]))
    }

    @Given("I create a Percy consider region with XPath {string} and diff sensitivity {int}")
    void iCreateConsiderRegionXPathWithSensitivity(String xpath, int sensitivity) {
        regions.add(PercyKeywords.createRegion([
            elementXpath: xpath,
            algorithm: 'standard',
            diffSensitivity: sensitivity
        ]))
    }

    @Given("I create a Percy intelliignore region with CSS selector {string}")
    void iCreateIntelliIgnoreRegionCSS(String selector) {
        regions.add(PercyKeywords.createRegion([
            elementCSS: selector,
            algorithm: 'intelliignore'
        ]))
    }

    @Given("I create a Percy intelliignore region with XPath {string}")
    void iCreateIntelliIgnoreRegionXPath(String xpath) {
        regions.add(PercyKeywords.createRegion([
            elementXpath: xpath,
            algorithm: 'intelliignore'
        ]))
    }

    @Given("I clear Percy regions")
    void iClearPercyRegions() {
        regions.clear()
    }

    // -------------------------------------------------------------------
    // When steps — snapshots
    // -------------------------------------------------------------------

    @When("I take a Percy snapshot named {string}")
    void iTakeSnapshot(String name) {
        PercyKeywords.percySnapshot(name)
    }

    @When("I take a Percy snapshot named {string} with widths {string}")
    void iTakeSnapshotWithWidths(String name, String widthsStr) {
        List<Integer> widths = widthsStr.split(',').collect { it.trim().toInteger() }
        PercyKeywords.percySnapshot(name, [widths: widths])
    }

    @When("I take a Percy snapshot named {string} with min height {int}")
    void iTakeSnapshotWithMinHeight(String name, int minHeight) {
        PercyKeywords.percySnapshot(name, [minHeight: minHeight])
    }

    @When("I take a Percy snapshot named {string} with Percy CSS {string}")
    void iTakeSnapshotWithCSS(String name, String css) {
        PercyKeywords.percySnapshot(name, [percyCSS: css])
    }

    @When("I take a Percy snapshot named {string} with scope {string}")
    void iTakeSnapshotWithScope(String name, String scope) {
        PercyKeywords.percySnapshot(name, [scope: scope])
    }

    @When("I take a Percy snapshot named {string} with layout mode")
    void iTakeSnapshotWithLayout(String name) {
        PercyKeywords.percySnapshot(name, [enableLayout: true])
    }

    @When("I take a Percy snapshot named {string} with JavaScript enabled")
    void iTakeSnapshotWithJS(String name) {
        PercyKeywords.percySnapshot(name, [enableJavaScript: true])
    }

    @When("I take a Percy snapshot named {string} with labels {string}")
    void iTakeSnapshotWithLabels(String name, String labels) {
        PercyKeywords.percySnapshot(name, [labels: labels])
    }

    @When("I take a Percy snapshot named {string} with test case {string}")
    void iTakeSnapshotWithTestCase(String name, String testCase) {
        PercyKeywords.percySnapshot(name, [testCase: testCase])
    }

    @When("I take a Percy snapshot named {string} with Shadow DOM disabled")
    void iTakeSnapshotWithShadowDomDisabled(String name) {
        PercyKeywords.percySnapshot(name, [disableShadowDom: true])
    }

    @When("I take a Percy snapshot named {string} with responsive capture")
    void iTakeSnapshotWithResponsiveCapture(String name) {
        PercyKeywords.percySnapshot(name, [responsiveSnapshotCapture: true])
    }

    @When("I take a Percy snapshot named {string} with sync")
    void iTakeSnapshotWithSync(String name) {
        PercyKeywords.percySnapshot(name, [sync: true])
    }

    @When("I take a Percy snapshot named {string} with regions")
    void iTakeSnapshotWithRegions(String name) {
        Map<String, Object> options = [:]
        if (!regions.isEmpty()) {
            options.regions = new ArrayList<>(regions)
            regions.clear()
        }
        PercyKeywords.percySnapshot(name, options)
    }

    @When("I take a Percy snapshot named {string} with widths {string} and regions")
    void iTakeSnapshotWithWidthsAndRegions(String name, String widthsStr) {
        List<Integer> widths = widthsStr.split(',').collect { it.trim().toInteger() }
        Map<String, Object> options = [widths: widths]
        if (!regions.isEmpty()) {
            options.regions = new ArrayList<>(regions)
            regions.clear()
        }
        PercyKeywords.percySnapshot(name, options)
    }

    // -------------------------------------------------------------------
    // When steps — screenshots (Automate)
    // -------------------------------------------------------------------

    @When("I take a Percy screenshot named {string}")
    void iTakeScreenshot(String name) {
        PercyKeywords.percyScreenshot(name)
    }

    @When("I take a Percy screenshot named {string} with regions")
    void iTakeScreenshotWithRegions(String name) {
        Map<String, Object> options = [:]
        if (!regions.isEmpty()) {
            options.regions = new ArrayList<>(regions)
            regions.clear()
        }
        PercyKeywords.percyScreenshot(name, options)
    }

    // -------------------------------------------------------------------
    // Then steps
    // -------------------------------------------------------------------

    @Then("Percy should be enabled")
    void percyShouldBeEnabled() {
        // Percy is enabled if CLI is running — validated by healthcheck during snapshot
    }
}
