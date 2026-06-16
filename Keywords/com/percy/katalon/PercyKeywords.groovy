package com.percy.katalon

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import io.percy.selenium.Percy
import org.openqa.selenium.WebDriver

class PercyKeywords {

    private static Percy cachedPercy
    private static WebDriver cachedDriver

    // -------------------------------------------------------------------
    // Snapshot (Percy Web -- DOM-based visual testing)
    // -------------------------------------------------------------------

    /**
     * Take a Percy snapshot of the current page.
     *
     * @param name A unique name for this snapshot
     * @return The snapshot result from Percy CLI, or null if Percy is unavailable
     */
    @Keyword(keywordObject = "Percy")
    static Object percySnapshot(String name) {
        return percySnapshot(name, [:])
    }

    /**
     * Take a Percy snapshot of the current page with options.
     *
     * Supported options (passed through to percy-java-selenium SDK):
     *   widths (List<Integer>)              - viewport widths for rendering
     *   minHeight (Integer)                 - minimum snapshot height
     *   percyCSS (String)                   - custom CSS injected during Percy rendering
     *   scope (String)                      - CSS selector to scope the snapshot
     *   enableJavaScript (Boolean)          - enable JS execution in Percy rendering
     *   responsiveSnapshotCapture (Boolean) - capture DOM at multiple widths client-side
     *   sync (Boolean)                      - wait for snapshot processing to complete
     *   enableLayout (Boolean)              - enable layout comparison mode
     *   disableShadowDom (Boolean)          - disable Shadow DOM serialization
     *   labels (String)                     - comma-separated labels for the snapshot
     *   testCase (String)                   - test case identifier
     *   domTransformation (String)          - JS function to transform DOM before capture
     *   regions (List<Map>)                 - per-region comparison config (use createRegion())
     *   readiness (Map)                     - per-snapshot readiness gate configuration
     *   discovery (Map)                     - discovery options (e.g., allowedHostnames)
     *
     * @param name A unique name for this snapshot
     * @param options Map of snapshot options
     * @return The snapshot result from Percy CLI, or null if Percy is unavailable
     */
    @Keyword(keywordObject = "Percy")
    static Object percySnapshot(String name, Map<String, Object> options) {
        try {
            WebDriver driver = DriverFactory.getWebDriver()
            if (driver == null) {
                log("[percy] No active WebDriver found. Skipping snapshot '${name}'.")
                return null
            }

            Percy percy = getOrCreatePercy(driver)
            if (percy == null) {
                return null
            }

            Map<String, Object> snapshotOptions = new HashMap<>(options ?: [:])
            snapshotOptions.putIfAbsent('client_info', "${Version.SDK_NAME}/${Version.VERSION}".toString())
            snapshotOptions.putIfAbsent('environment_info', "percy-java-selenium/2.1.1")

            return percy.snapshot(name, snapshotOptions)
        } catch (Throwable t) {
            log("[percy] Could not take snapshot '${name}': ${t.message}")
            return null
        }
    }

    // -------------------------------------------------------------------
    // Screenshot (Percy on Automate -- BrowserStack screenshot-based testing)
    // -------------------------------------------------------------------

    /**
     * Take a Percy screenshot via BrowserStack Automate.
     *
     * Use this when running tests on BrowserStack Automate (not local browsers).
     * Percy CLI must be started with an Automate-type token.
     *
     * @param name A unique name for this screenshot
     * @return The screenshot result from Percy CLI, or null if Percy is unavailable
     */
    @Keyword(keywordObject = "Percy")
    static Object percyScreenshot(String name) {
        return percyScreenshot(name, [:])
    }

    /**
     * Take a Percy screenshot via BrowserStack Automate with options.
     *
     * Supported options:
     *   sync (Boolean)                          - wait for screenshot processing
     *   fullPage (Boolean)                      - capture full page screenshot
     *   freezeAnimatedImage (Boolean)           - freeze image-based animations
     *   freezeImageBySelectors (List<String>)   - CSS selectors for images to freeze
     *   freezeImageByXpaths (List<String>)      - XPaths for images to freeze
     *   percyCSS (String)                       - custom CSS injected before screenshot
     *   ignoreRegionXpaths (List<String>)       - XPaths of elements to ignore
     *   ignoreRegionSelectors (List<String>)    - CSS selectors of elements to ignore
     *   customIgnoreRegions (List<Map>)         - custom bounding boxes to ignore
     *   considerRegionXpaths (List<String>)     - XPaths for consider regions
     *   considerRegionSelectors (List<String>)  - CSS selectors for consider regions
     *   customConsiderRegions (List<Map>)       - custom bounding boxes for consider regions
     *   regions (List<Map>)                     - unified region config (use createRegion())
     *
     * @param name A unique name for this screenshot
     * @param options Map of screenshot options
     * @return The screenshot result from Percy CLI, or null if Percy is unavailable
     */
    @Keyword(keywordObject = "Percy")
    static Object percyScreenshot(String name, Map<String, Object> options) {
        try {
            WebDriver driver = DriverFactory.getWebDriver()
            if (driver == null) {
                log("[percy] No active WebDriver found. Skipping screenshot '${name}'.")
                return null
            }

            Percy percy = getOrCreatePercy(driver)
            if (percy == null) {
                return null
            }

            return percy.screenshot(name, options ?: [:])
        } catch (Throwable t) {
            log("[percy] Could not take screenshot '${name}': ${t.message}")
            return null
        }
    }

    // -------------------------------------------------------------------
    // Region helper
    // -------------------------------------------------------------------

    /**
     * Build a region configuration map for use with snapshot/screenshot regions option.
     *
     * Supported params:
     *   boundingBox (Map)       - {x: int, y: int, width: int, height: int}
     *   elementXpath (String)   - XPath selector for the element
     *   elementCSS (String)     - CSS selector for the element
     *   padding (Integer)       - padding around the region in pixels
     *   algorithm (String)      - comparison algorithm: "ignore", "standard", "layout", "intelliignore"
     *   diffSensitivity (Number)      - diff sensitivity (for standard/intelliignore)
     *   imageIgnoreThreshold (Number) - image ignore threshold (for standard/intelliignore)
     *   carouselsEnabled (Boolean)    - enable carousel detection
     *   bannersEnabled (Boolean)      - enable banner detection
     *   adsEnabled (Boolean)          - enable ad detection
     *   diffIgnoreThreshold (Number)  - assertion threshold for ignoring minor diffs
     *
     * Example:
     *   def region = CustomKeywords.'com.percy.katalon.PercyKeywords.createRegion'([
     *       elementCSS: '.dynamic-content',
     *       algorithm: 'ignore'
     *   ])
     *   CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Page', [regions: [region]])
     *
     * @param params Region configuration parameters
     * @return A structured region map ready for use in regions option
     */
    @Keyword(keywordObject = "Percy")
    static Map<String, Object> createRegion(Map<String, Object> params) {
        Map<String, Object> region = [:]

        // Algorithm (default: ignore)
        String algorithm = (params.algorithm ?: 'ignore') as String
        region.algorithm = algorithm

        // Element selector
        Map<String, Object> elementSelector = [:]
        if (params.boundingBox) elementSelector.boundingBox = params.boundingBox
        if (params.elementXpath) elementSelector.elementXpath = params.elementXpath
        if (params.elementCSS) elementSelector.elementCSS = params.elementCSS
        region.elementSelector = elementSelector

        // Padding
        if (params.padding != null) {
            region.padding = params.padding
        }

        // Configuration (only for standard and intelliignore algorithms)
        if (algorithm in ['standard', 'intelliignore']) {
            Map<String, Object> configuration = [:]
            if (params.diffSensitivity != null) configuration.diffSensitivity = params.diffSensitivity
            if (params.imageIgnoreThreshold != null) configuration.imageIgnoreThreshold = params.imageIgnoreThreshold
            if (params.carouselsEnabled != null) configuration.carouselsEnabled = params.carouselsEnabled
            if (params.bannersEnabled != null) configuration.bannersEnabled = params.bannersEnabled
            if (params.adsEnabled != null) configuration.adsEnabled = params.adsEnabled
            if (!configuration.isEmpty()) {
                region.configuration = configuration
            }
        }

        // Assertion
        if (params.diffIgnoreThreshold != null) {
            region.assertion = [diffIgnoreThreshold: params.diffIgnoreThreshold]
        }

        return region
    }

    // -------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------

    /**
     * Get or create a cached Percy instance. Reuses the instance if the driver
     * has not changed, avoiding redundant healthcheck HTTP calls.
     */
    private static synchronized Percy getOrCreatePercy(WebDriver driver) {
        if (cachedPercy != null && cachedDriver.is(driver)) {
            return cachedPercy
        }

        try {
            cachedPercy = new Percy(driver)
            cachedDriver = driver
            return cachedPercy
        } catch (Throwable t) {
            log("[percy] Percy is not running or not available: ${t.message}")
            cachedPercy = null
            cachedDriver = null
            return null
        }
    }

    /**
     * Dual-channel logging: logs to both Katalon console and Percy CLI.
     */
    private static void log(String message) {
        // Local logging via Katalon
        try {
            KeywordUtil.logInfo(message)
        } catch (Throwable ignored) {
            System.out.println(message)
        }

        // Remote logging to Percy CLI
        try {
            String serverAddress = System.getenv("PERCY_SERVER_ADDRESS") ?: "http://localhost:5338"
            URL url = new URL("${serverAddress}/percy/log")
            HttpURLConnection conn = (HttpURLConnection) url.openConnection()
            conn.setRequestMethod("POST")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setConnectTimeout(1000)
            conn.setReadTimeout(1000)
            conn.setDoOutput(true)

            String json = "{\"message\":${groovy.json.JsonOutput.toJson(message)},\"level\":\"info\"}"
            conn.outputStream.write(json.getBytes("UTF-8"))
            conn.outputStream.flush()
            conn.responseCode // trigger the request
            conn.disconnect()
        } catch (Throwable ignored) {
            // Silently ignore -- Percy CLI may not be running
        }
    }

    /**
     * Reset the cached Percy instance. Useful for testing.
     */
    static void resetCache() {
        cachedPercy = null
        cachedDriver = null
    }
}
