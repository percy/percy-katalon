package com.percy.katalon

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import io.percy.selenium.Percy
import org.openqa.selenium.WebDriver

class PercyKeywords {

    private static Percy cachedPercy
    private static WebDriver cachedDriver

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
     *   widths (List<Integer>)          - viewport widths for rendering
     *   minHeight (Integer)             - minimum snapshot height
     *   percyCSS (String)              - custom CSS injected during rendering
     *   scope (String)                  - CSS selector to scope the snapshot
     *   enableJavaScript (Boolean)      - enable JavaScript execution during rendering
     *   responsiveSnapshotCapture (Boolean) - capture DOM at multiple widths
     *   sync (Boolean)                  - wait for snapshot processing
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
                KeywordUtil.logInfo("[percy] No active WebDriver found. Skipping snapshot '${name}'.")
                return null
            }

            Percy percy = getOrCreatePercy(driver)
            if (percy == null) {
                return null
            }

            // Client info is reported automatically by percy-java-selenium SDK's Environment class.
            // The SDK v2.1.1 does not expose a setClientInfo() method, so attribution is handled
            // via the 'client_info' and 'environment_info' keys in the options map.
            Map<String, Object> snapshotOptions = new HashMap<>(options ?: [:])
            snapshotOptions.putIfAbsent('client_info', "${Version.SDK_NAME}/${Version.VERSION}".toString())
            snapshotOptions.putIfAbsent('environment_info', "percy-java-selenium/2.1.1")

            return percy.snapshot(name, snapshotOptions)
        } catch (Throwable t) {
            KeywordUtil.logInfo("[percy] Could not take snapshot '${name}': ${t.message}")
            return null
        }
    }

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
            KeywordUtil.logInfo("[percy] Percy is not running or not available: ${t.message}")
            cachedPercy = null
            cachedDriver = null
            return null
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
