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
            if (ex.requestMethod == "POST") {
                snapshotBodies.add(ex.requestBody.text)
            }
            String resp = '{"success":true}'
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
        assertEquals(10, region.padding)
        assertEquals(0, region.elementSelector.boundingBox.x)
        assertEquals(200, region.elementSelector.boundingBox.width)
    }

    @Test
    @Order(14)
    void testCreateRegionStandardWithConfig() {
        def region = PercyKeywords.createRegion([
            elementXpath: '//div[@id="content"]',
            algorithm: 'standard',
            diffSensitivity: 3,
            imageIgnoreThreshold: 0.2,
            carouselsEnabled: true
        ])

        assertEquals('standard', region.algorithm)
        assertEquals('//div[@id="content"]', region.elementSelector.elementXpath)
        assertEquals(3, region.configuration.diffSensitivity)
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
    void testCreateRegionNoConfigForIgnoreAlgorithm() {
        def region = PercyKeywords.createRegion([
            elementCSS: '.ignore-me',
            algorithm: 'ignore',
            diffSensitivity: 3  // should be ignored for 'ignore' algorithm
        ])

        assertFalse(region.containsKey('configuration'), "Ignore algorithm should not have configuration")
    }

    @Test
    @Order(19)
    void testSnapshotWithRegions() {
        def ignoreRegion = PercyKeywords.createRegion([
            elementCSS: '.dynamic-ad',
            algorithm: 'ignore'
        ])
        def considerRegion = PercyKeywords.createRegion([
            elementCSS: '.main-content',
            algorithm: 'standard',
            diffSensitivity: 5
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
    // Dual-channel logging tests
    // -------------------------------------------------------------------

    @Test
    @Order(20)
    void testDualChannelLoggingOnNullDriver() {
        driverFactoryMock.when(DriverFactory::getWebDriver).thenReturn(null)
        PercyKeywords.percySnapshot("Log Test")

        // Give the async log POST a moment
        Thread.sleep(500)

        // Should have logged to Percy CLI
        assertTrue(logBodies.size() >= 1, "Expected at least 1 log message sent to Percy CLI, got ${logBodies.size()}")
        assertTrue(logBodies.any { it.contains("No active WebDriver") }, "Log should mention missing WebDriver")
    }
}
