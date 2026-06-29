package com.percy.katalon

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpExchange
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.mockito.MockedStatic
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver

import static org.junit.jupiter.api.Assertions.*
import static org.mockito.Mockito.*

import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.util.KeywordUtil

import java.lang.reflect.Field

@TestMethodOrder(MethodOrderer.OrderAnnotation)
class PercyKeywordsTest {

    static HttpServer server
    static int serverPort
    static List<String> snapshotBodies = Collections.synchronizedList([])
    static List<String> screenshotBodies = Collections.synchronizedList([])
    static List<String> logBodies = Collections.synchronizedList([])

    MockedStatic<DriverFactory> driverFactoryMock
    MockedStatic<KeywordUtil> keywordUtilMock
    List<String> logMessages = []
    WebDriver mockDriver

    @BeforeAll
    static void startServer() {
        serverPort = Integer.parseInt(System.getProperty("test.percy.port", "5339"))
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", serverPort), 0)

        server.createContext("/percy/healthcheck") { HttpExchange ex ->
            String resp = '{"success":true,"config":{"snapshot":{}},"type":"web","widths":{"mobile":[],"config":[375,1280]}}'
            ex.responseHeaders.set("x-percy-core-version", "1.0.0")
            byte[] bytes = resp.getBytes("UTF-8")
            ex.sendResponseHeaders(200, bytes.length)
            ex.responseBody.write(bytes)
            ex.responseBody.close()
        }

        server.createContext("/percy/dom.js") { HttpExchange ex ->
            String script = "window.PercyDOM = { serialize: function() { return {html:'<html></html>',cookies:[]}; }, waitForResize: function() {} };"
            byte[] bytes = script.getBytes("UTF-8")
            ex.sendResponseHeaders(200, bytes.length)
            ex.responseBody.write(bytes)
            ex.responseBody.close()
        }

        server.createContext("/percy/snapshot") { HttpExchange ex ->
            String reqBody = ""
            if (ex.requestMethod == "POST") {
                reqBody = ex.requestBody.text
                snapshotBodies.add(reqBody)
            }
            // Return sync-compatible response when request contains sync option
            String resp
            if (reqBody.contains('"sync"')) {
                // Extract snapshot name from request body for realistic response
                def nameMatch = reqBody =~ /"name"\s*:\s*"([^"]+)"/
                String snapName = nameMatch ? nameMatch[0][1] : "unknown"
                resp = """{"success":true,"data":{"snapshot-name":"${snapName}","status":"success","screenshots":[]}}"""
            } else {
                resp = '{"success":true}'
            }
            byte[] bytes = resp.getBytes("UTF-8")
            ex.sendResponseHeaders(200, bytes.length)
            ex.responseBody.write(bytes)
            ex.responseBody.close()
        }

        server.createContext("/percy/automateScreenshot") { HttpExchange ex ->
            if (ex.requestMethod == "POST") {
                screenshotBodies.add(ex.requestBody.text)
            }
            String resp = '{"success":true}'
            byte[] bytes = resp.getBytes("UTF-8")
            ex.sendResponseHeaders(200, bytes.length)
            ex.responseBody.write(bytes)
            ex.responseBody.close()
        }

        server.createContext("/percy/log") { HttpExchange ex ->
            if (ex.requestMethod == "POST") {
                logBodies.add(ex.requestBody.text)
            }
            String resp = '{"success":true}'
            byte[] bytes = resp.getBytes("UTF-8")
            ex.sendResponseHeaders(200, bytes.length)
            ex.responseBody.write(bytes)
            ex.responseBody.close()
        }

        server.executor = null
        server.start()
    }

    @AfterAll
    static void stopServer() {
        server?.stop(0)
    }

    @BeforeEach
    void setUp() {
        PercyKeywords.resetCache()
        snapshotBodies.clear()
        screenshotBodies.clear()
        logBodies.clear()

        mockDriver = mock(WebDriver, withSettings().extraInterfaces(JavascriptExecutor))
        JavascriptExecutor js = (JavascriptExecutor) mockDriver

        doAnswer({ invocation ->
            Object[] args = invocation.getArguments()
            String script = args[0] as String
            if (script != null && script.contains("serialize")) {
                Map<String, Object> dom = new HashMap<>()
                dom.put("html", "<html><body>test</body></html>")
                dom.put("cookies", new ArrayList<>())
                return dom
            }
            return null
        }).when(js).executeScript(anyString())

        doReturn("https://example.com").when(mockDriver).getCurrentUrl()

        driverFactoryMock = mockStatic(DriverFactory)
        keywordUtilMock = mockStatic(KeywordUtil)
        driverFactoryMock.when(DriverFactory::getWebDriver).thenReturn(mockDriver)
        keywordUtilMock.when(() -> KeywordUtil.logInfo(any(String))).thenAnswer({ inv ->
            logMessages.add(inv.getArgument(0) as String)
            return null
        })
    }

    @AfterEach
    void tearDown() {
        driverFactoryMock.close()
        keywordUtilMock.close()
        PercyKeywords.resetCache()
    }

    // -------------------------------------------------------------------
    // Snapshot tests
    // -------------------------------------------------------------------

    @Test
    @Order(1)
    void testPercySnapshotSendsRequest() {
        PercyKeywords.percySnapshot("Homepage")
        assertTrue(snapshotBodies.size() >= 1, "Expected at least 1 snapshot request, got ${snapshotBodies.size()}. Logs: ${logMessages}")
    }

    @Test
    @Order(2)
    void testSnapshotIncludesName() {
        PercyKeywords.percySnapshot("My Test Page")
        assertTrue(snapshotBodies.size() >= 1)
        assertTrue(snapshotBodies[0].contains("My Test Page"), "Snapshot body should contain the name")
    }

    @Test
    @Order(3)
    void testSnapshotIncludesClientInfo() {
        PercyKeywords.percySnapshot("Attributed Page")
        assertTrue(snapshotBodies.size() >= 1)
        assertTrue(snapshotBodies[0].contains("percy-katalon"), "Snapshot body should contain percy-katalon client info")
    }

    @Test
    @Order(4)
    void testSnapshotWithOptions() {
        PercyKeywords.percySnapshot("Responsive Page", [widths: [375, 1280]])
        assertTrue(snapshotBodies.size() >= 1)
        assertTrue(snapshotBodies[0].contains("Responsive Page"))
    }

    @Test
    @Order(5)
    void testSnapshotWithAdvancedOptions() {
        PercyKeywords.percySnapshot("Advanced Page", [
            percyCSS: 'body { color: red; }',
            enableJavaScript: true,
            scope: '#main',
            labels: 'smoke,regression',
            enableLayout: true
        ])
        assertTrue(snapshotBodies.size() >= 1)
        String body = snapshotBodies[0]
        assertTrue(body.contains("Advanced Page"))
        assertTrue(body.contains("enableJavaScript"))
    }

    @Test
    @Order(6)
    void testPercyInstanceCachedAcrossCalls() {
        PercyKeywords.percySnapshot("Page 1")
        PercyKeywords.percySnapshot("Page 2")
        assertTrue(snapshotBodies.size() >= 2, "Expected at least 2 snapshot requests")
    }

    @Test
    @Order(7)
    void testDriverChangeCreatesNewPercy() {
        WebDriver secondDriver = mock(WebDriver, withSettings().extraInterfaces(JavascriptExecutor))
        JavascriptExecutor js2 = (JavascriptExecutor) secondDriver
        doAnswer({ invocation ->
            Object[] args = invocation.getArguments()
            String script = args[0] as String
            if (script != null && script.contains("serialize")) {
                Map<String, Object> dom = new HashMap<>()
                dom.put("html", "<html><body>second</body></html>")
                dom.put("cookies", new ArrayList<>())
                return dom
            }
            return null
        }).when(js2).executeScript(anyString())
        doReturn("https://example.com/page2").when(secondDriver).getCurrentUrl()

        PercyKeywords.percySnapshot("Page 1")
        driverFactoryMock.when(DriverFactory::getWebDriver).thenReturn(secondDriver)
        PercyKeywords.percySnapshot("Page 2")

        assertTrue(snapshotBodies.size() >= 2)
    }

    @Test
    @Order(8)
    void testNullDriverReturnsNull() {
        driverFactoryMock.when(DriverFactory::getWebDriver).thenReturn(null)
        def result = PercyKeywords.percySnapshot("Homepage")
        assertNull(result)
        assertEquals(0, snapshotBodies.size())
    }

    @Test
    @Order(9)
    void testNoClassDefErrorCaughtGracefully() {
        driverFactoryMock.when(DriverFactory::getWebDriver).thenThrow(
            new NoClassDefFoundError("com/kms/katalon/core/webui/driver/DriverFactory")
        )
        def result = PercyKeywords.percySnapshot("Homepage")
        assertNull(result)
        assertEquals(0, snapshotBodies.size())
    }

    // -------------------------------------------------------------------
    // Screenshot (Automate) tests
    // -------------------------------------------------------------------

    @Test
    @Order(10)
    void testPercyScreenshotBasic() {
        // screenshot() will throw because session type is "web" (not "automate")
        // This is expected behavior -- the SDK enforces session type
        def result = PercyKeywords.percyScreenshot("Screenshot Test")
        // Should return null gracefully (caught by Throwable handler)
        assertNull(result)
    }

    @Test
    @Order(11)
    void testPercyScreenshotNullDriver() {
        driverFactoryMock.when(DriverFactory::getWebDriver).thenReturn(null)
        def result = PercyKeywords.percyScreenshot("Screenshot Test")
        assertNull(result)
    }

    // -------------------------------------------------------------------
    // createRegion tests
    // -------------------------------------------------------------------

    @Test
    @Order(12)
    void testCreateRegionIgnore() {
        def region = PercyKeywords.createRegion([
            elementCSS: '.dynamic-content',
            algorithm: 'ignore'
        ])

        assertEquals('ignore', region.algorithm)
        assertEquals('.dynamic-content', region.elementSelector.elementCSS)
    }

    @Test
    @Order(13)
    void testCreateRegionWithBoundingBox() {
        def region = PercyKeywords.createRegion([
            boundingBox: [x: 0, y: 0, width: 200, height: 100],
            algorithm: 'ignore',
            padding: 10
        ])

        assertEquals('ignore', region.algorithm)
        // Padding integer is expanded to {top, bottom, left, right} object
        assertEquals(10, region.padding.top)
        assertEquals(10, region.padding.bottom)
        assertEquals(10, region.padding.left)
        assertEquals(10, region.padding.right)
        assertEquals(0, region.elementSelector.boundingBox.x)
        assertEquals(200, region.elementSelector.boundingBox.width)
    }

    @Test
    @Order(14)
    void testCreateRegionStandardWithConfig() {
        def region = PercyKeywords.createRegion([
            elementXpath: '//div[@id="content"]',
            algorithm: 'standard',
            diffSensitivity: 2,
            imageIgnoreThreshold: 0.2,
            carouselsEnabled: true
        ])

        assertEquals('standard', region.algorithm)
        assertEquals('//div[@id="content"]', region.elementSelector.elementXpath)
        assertEquals(2, region.configuration.diffSensitivity)
        assertEquals(0.2, region.configuration.imageIgnoreThreshold)
        assertTrue(region.configuration.carouselsEnabled)
    }

    @Test
    @Order(15)
    void testCreateRegionIntelliignore() {
        def region = PercyKeywords.createRegion([
            elementCSS: '.ad-banner',
            algorithm: 'intelliignore',
            adsEnabled: true,
            bannersEnabled: true
        ])

        assertEquals('intelliignore', region.algorithm)
        assertTrue(region.configuration.adsEnabled)
        assertTrue(region.configuration.bannersEnabled)
    }

    @Test
    @Order(16)
    void testCreateRegionWithAssertion() {
        def region = PercyKeywords.createRegion([
            elementCSS: '.content',
            algorithm: 'standard',
            diffIgnoreThreshold: 0.1
        ])

        assertEquals(0.1, region.assertion.diffIgnoreThreshold)
    }

    @Test
    @Order(17)
    void testCreateRegionDefaultAlgorithm() {
        def region = PercyKeywords.createRegion([
            elementCSS: '.ignore-me'
        ])

        assertEquals('ignore', region.algorithm)
    }

    @Test
    @Order(18)
    void testCreateRegionPaddingAsMap() {
        def region = PercyKeywords.createRegion([
            elementCSS: '.sidebar',
            algorithm: 'ignore',
            padding: [top: 5, bottom: 10, left: 15, right: 20]
        ])

        assertEquals(5, region.padding.top)
        assertEquals(10, region.padding.bottom)
        assertEquals(15, region.padding.left)
        assertEquals(20, region.padding.right)
    }

    @Test
    @Order(19)
    void testCreateRegionNoConfigForIgnoreAlgorithm() {
        def region = PercyKeywords.createRegion([
            elementCSS: '.ignore-me',
            algorithm: 'ignore',
            diffSensitivity: 3  // should be ignored for 'ignore' algorithm
        ])

        assertFalse(region.containsKey('configuration'), "Ignore algorithm should not have configuration")
    }

    @Test
    @Order(20)
    void testSnapshotWithRegions() {
        def ignoreRegion = PercyKeywords.createRegion([
            elementCSS: '.dynamic-ad',
            algorithm: 'ignore'
        ])
        def considerRegion = PercyKeywords.createRegion([
            elementCSS: '.main-content',
            algorithm: 'standard',
            diffSensitivity: 3
        ])

        PercyKeywords.percySnapshot("Page With Regions", [
            regions: [ignoreRegion, considerRegion]
        ])

        assertTrue(snapshotBodies.size() >= 1)
        String body = snapshotBodies[0]
        assertTrue(body.contains("Page With Regions"))
        assertTrue(body.contains("regions"))
    }

    // -------------------------------------------------------------------
    // Responsive capture tests
    // -------------------------------------------------------------------

    @Test
    @Order(21)
    void testSnapshotWithResponsiveCapture() {
        // responsiveSnapshotCapture triggers SDK-internal viewport resizing which
        // requires a real browser. With our mock, it fails gracefully and returns null.
        // This test verifies the option doesn't crash the plugin.
        def result = PercyKeywords.percySnapshot("Responsive Page", [
            responsiveSnapshotCapture: true,
            widths: [375, 768, 1280]
        ])
        // May be null (SDK can't resize mock driver) — that's OK, no crash is the test
        // Real responsive capture is validated in the manual Katalon test
    }

    @Test
    @Order(22)
    void testSnapshotWithMinHeight() {
        PercyKeywords.percySnapshot("Tall Page", [
            widths: [375, 1280],
            minHeight: 1200
        ])
        assertTrue(snapshotBodies.size() >= 1)
        String body = snapshotBodies[0]
        assertTrue(body.contains("minHeight"))
        assertTrue(body.contains("1200"))
    }

    // -------------------------------------------------------------------
    // Dual-channel logging tests
    // -------------------------------------------------------------------

    @Test
    @Order(23)
    void testDualChannelLoggingOnNullDriver() {
        driverFactoryMock.when(DriverFactory::getWebDriver).thenReturn(null)
        PercyKeywords.percySnapshot("Log Test")

        // Give the async log POST a moment
        Thread.sleep(500)

        // Should have logged to Percy CLI
        assertTrue(logBodies.size() >= 1, "Expected at least 1 log message sent to Percy CLI, got ${logBodies.size()}")
        assertTrue(logBodies.any { it.contains("No active WebDriver") }, "Log should mention missing WebDriver")
    }

    // -------------------------------------------------------------------
    // Snapshot body field-level assertions
    // -------------------------------------------------------------------

    @Test
    @Order(24)
    void testSnapshotBodyContainsSpecificOptionValues() {
        PercyKeywords.percySnapshot("Options Page", [
            percyCSS: 'body { background: purple; }',
            domTransformation: "(doc) => doc.querySelector('body').style.color = 'green';",
            scope: '#main-content',
            widths: [768, 992, 1200]
        ])
        assertTrue(snapshotBodies.size() >= 1)
        String body = snapshotBodies[0]
        assertTrue(body.contains('body { background: purple; }'), "Body should contain percyCSS value")
        assertTrue(body.contains('domTransformation'), "Body should contain domTransformation key")
        assertTrue(body.contains('#main-content'), "Body should contain scope selector")
        assertTrue(body.contains('768'), "Body should contain width 768")
        assertTrue(body.contains('992'), "Body should contain width 992")
        assertTrue(body.contains('1200'), "Body should contain width 1200")
    }

    @Test
    @Order(25)
    void testSnapshotBodyContainsUrl() {
        PercyKeywords.percySnapshot("URL Check Page")
        assertTrue(snapshotBodies.size() >= 1)
        String body = snapshotBodies[0]
        assertTrue(body.contains("https://example.com"), "Snapshot body should include the page URL")
    }

    @Test
    @Order(26)
    void testSnapshotBodyContainsEnvironmentInfo() {
        PercyKeywords.percySnapshot("Env Info Page")
        assertTrue(snapshotBodies.size() >= 1)
        String body = snapshotBodies[0]
        assertTrue(body.contains("percy-java-selenium"), "Body should contain percy-java-selenium environment info")
    }

    // -------------------------------------------------------------------
    // Multiple snapshots with distinct names
    // -------------------------------------------------------------------

    @Test
    @Order(27)
    void testMultipleSnapshotsOnSamePageHaveDistinctNames() {
        PercyKeywords.percySnapshot("Multi Snap -- First")
        PercyKeywords.percySnapshot("Multi Snap -- Second")
        assertTrue(snapshotBodies.size() >= 2, "Expected at least 2 snapshots")
        assertTrue(snapshotBodies[0].contains("Multi Snap -- First"), "First body should contain first name")
        assertTrue(snapshotBodies[1].contains("Multi Snap -- Second"), "Second body should contain second name")
    }

    // -------------------------------------------------------------------
    // Sync mode snapshot
    // -------------------------------------------------------------------

    @Test
    @Order(28)
    void testSnapshotWithSyncOption() {
        def result = PercyKeywords.percySnapshot("Sync Snapshot", [sync: true])
        assertTrue(snapshotBodies.size() >= 1)
        String body = snapshotBodies[0]
        assertTrue(body.contains('"sync"'), "Snapshot body should contain sync option")
    }

    // -------------------------------------------------------------------
    // Snapshot error on automate session
    // -------------------------------------------------------------------

    @Test
    @Order(29)
    void testSnapshotThrowsErrorForAutomateSession() {
        // First create the Percy instance by taking a normal snapshot
        PercyKeywords.percySnapshot("Setup Snapshot")
        snapshotBodies.clear()
        logMessages.clear()

        // Use reflection to switch sessionType to "automate"
        Field cachedPercyField = PercyKeywords.getDeclaredField("cachedPercy")
        cachedPercyField.setAccessible(true)
        def percyInstance = cachedPercyField.get(null)

        Field sessionTypeField = percyInstance.getClass().getDeclaredField("sessionType")
        sessionTypeField.setAccessible(true)
        sessionTypeField.set(percyInstance, "automate")

        // Now snapshot() should fail with the Percy SDK error
        def result = PercyKeywords.percySnapshot("Should Fail")
        assertNull(result, "Snapshot should return null when sessionType is automate")
        assertTrue(logMessages.any { it.contains("snapshot") },
            "Error log should mention snapshot. Logs: ${logMessages}")
    }

    // -------------------------------------------------------------------
    // Screenshot error message validation on web session
    // -------------------------------------------------------------------

    @Test
    @Order(30)
    void testScreenshotErrorMessageOnWebSession() {
        // First create the Percy instance (sessionType = "web" from healthcheck)
        PercyKeywords.percySnapshot("Setup For Screenshot Test")
        logMessages.clear()

        // screenshot() on web session should fail and log specific error
        def result = PercyKeywords.percyScreenshot("Web Screenshot")
        assertNull(result, "Screenshot should return null on web session")
        assertTrue(logMessages.any { it.contains("screenshot") || it.contains("Screenshot") },
            "Error log should mention screenshot. Logs: ${logMessages}")
    }

    // -------------------------------------------------------------------
    // Successful automate screenshot
    // -------------------------------------------------------------------

    @Test
    @Order(31)
    void testScreenshotAutomateSessionFailsGracefullyWithMockDriver() {
        // Create Percy instance via a snapshot first
        PercyKeywords.percySnapshot("Setup Automate")
        snapshotBodies.clear()
        logMessages.clear()

        // Switch to automate session type via reflection
        Field cachedPercyField = PercyKeywords.getDeclaredField("cachedPercy")
        cachedPercyField.setAccessible(true)
        def percyInstance = cachedPercyField.get(null)

        Field sessionTypeField = percyInstance.getClass().getDeclaredField("sessionType")
        sessionTypeField.setAccessible(true)
        sessionTypeField.set(percyInstance, "automate")

        // screenshot() on automate session needs real BrowserStack RemoteWebDriver
        // capabilities (session metadata). With a mock driver, it fails gracefully.
        def result = PercyKeywords.percyScreenshot("Automate Screenshot")
        assertNull(result, "Screenshot should return null with mock driver (no BrowserStack session)")
        assertTrue(logMessages.any { it.contains("[percy]") },
            "Should log error via Katalon. Logs: ${logMessages}")
    }

    @Test
    @Order(32)
    void testScreenshotWithOptionsAutomateSessionFailsGracefullyWithMockDriver() {
        // Create Percy instance
        PercyKeywords.percySnapshot("Setup Automate Options")
        snapshotBodies.clear()
        logMessages.clear()

        // Switch to automate
        Field cachedPercyField = PercyKeywords.getDeclaredField("cachedPercy")
        cachedPercyField.setAccessible(true)
        def percyInstance = cachedPercyField.get(null)

        Field sessionTypeField = percyInstance.getClass().getDeclaredField("sessionType")
        sessionTypeField.setAccessible(true)
        sessionTypeField.set(percyInstance, "automate")

        def result = PercyKeywords.percyScreenshot("Automate With Options", [
            percyCSS: 'h1 { color: black; }',
            fullPage: true
        ])
        assertNull(result, "Screenshot should return null with mock driver")
        assertTrue(logMessages.any { it.contains("[percy]") },
            "Should log error. Logs: ${logMessages}")
    }

    // -------------------------------------------------------------------
    // createRegion -- all fields in a single call
    // -------------------------------------------------------------------

    @Test
    @Order(33)
    void testCreateRegionAllFields() {
        def region = PercyKeywords.createRegion([
            boundingBox: [x: 100, y: 100, width: 200, height: 200],
            elementXpath: '//div[@id="test"]',
            elementCSS: '.test-class',
            padding: 10,
            algorithm: 'standard',
            diffSensitivity: 2,
            imageIgnoreThreshold: 0.2,
            carouselsEnabled: true,
            bannersEnabled: false,
            adsEnabled: true,
            diffIgnoreThreshold: 0.1
        ])

        assertNotNull(region)

        // Element selector
        assertNotNull(region.elementSelector)
        assertEquals('//div[@id="test"]', region.elementSelector.elementXpath)
        assertEquals('.test-class', region.elementSelector.elementCSS)
        assertEquals(100, region.elementSelector.boundingBox.x)
        assertEquals(200, region.elementSelector.boundingBox.width)

        // Algorithm
        assertEquals('standard', region.algorithm)

        // Padding (integer expanded to map)
        assertEquals(10, region.padding.top)
        assertEquals(10, region.padding.bottom)
        assertEquals(10, region.padding.left)
        assertEquals(10, region.padding.right)

        // Configuration
        assertNotNull(region.configuration)
        assertEquals(2, region.configuration.diffSensitivity)
        assertEquals(0.2, region.configuration.imageIgnoreThreshold)
        assertTrue(region.configuration.carouselsEnabled)
        assertFalse(region.configuration.bannersEnabled)
        assertTrue(region.configuration.adsEnabled)

        // Assertion
        assertNotNull(region.assertion)
        assertEquals(0.1, region.assertion.diffIgnoreThreshold)
    }

    // -------------------------------------------------------------------
    // Snapshot with domTransformation option
    // -------------------------------------------------------------------

    @Test
    @Order(34)
    void testSnapshotWithDomTransformation() {
        String transform = "(documentElement) => documentElement.querySelector('body').style.color = 'green';"
        PercyKeywords.percySnapshot("DOM Transform Page", [
            domTransformation: transform
        ])
        assertTrue(snapshotBodies.size() >= 1)
        String body = snapshotBodies[0]
        assertTrue(body.contains("domTransformation"), "Body should contain domTransformation key")
        assertTrue(body.contains("green"), "Body should contain the transformation content")
    }

    // -------------------------------------------------------------------
    // Screenshot null driver logging
    // -------------------------------------------------------------------

    @Test
    @Order(35)
    void testScreenshotNullDriverLogsError() {
        driverFactoryMock.when(DriverFactory::getWebDriver).thenReturn(null)
        logMessages.clear()

        def result = PercyKeywords.percyScreenshot("Null Driver Screenshot")
        assertNull(result)

        Thread.sleep(500)

        assertTrue(logMessages.any { it.contains("No active WebDriver") },
            "Should log missing WebDriver for screenshot. Logs: ${logMessages}")
        assertTrue(logBodies.any { it.contains("No active WebDriver") },
            "Should also log to Percy CLI. LogBodies: ${logBodies}")
    }

    // -------------------------------------------------------------------
    // Snapshot with discovery options (cross-origin iframe support)
    // -------------------------------------------------------------------

    @Test
    @Order(36)
    void testSnapshotWithDiscoveryOptions() {
        PercyKeywords.percySnapshot("CORS Iframe Page", [
            discovery: [allowedHostnames: ["todomvc.com"]]
        ])
        assertTrue(snapshotBodies.size() >= 1)
        String body = snapshotBodies[0]
        assertTrue(body.contains("discovery"), "Body should contain discovery option")
        assertTrue(body.contains("allowedHostnames"), "Body should contain allowedHostnames")
        assertTrue(body.contains("todomvc.com"), "Body should contain the allowed hostname")
    }
}
