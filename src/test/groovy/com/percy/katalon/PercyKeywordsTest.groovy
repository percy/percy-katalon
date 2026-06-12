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
                String body = ex.requestBody.text
                snapshotBodies.add(body)
            }
            String resp = '{"success":true}'
            byte[] bytes = resp.getBytes("UTF-8")
            ex.sendResponseHeaders(200, bytes.length)
            ex.responseBody.write(bytes)
            ex.responseBody.close()
        }

        server.createContext("/percy/log") { HttpExchange ex ->
            String resp = '{"success":true}'
            byte[] bytes = resp.getBytes("UTF-8")
            ex.sendResponseHeaders(200, bytes.length)
            ex.responseBody.write(bytes)
            ex.responseBody.close()
        }

        server.executor = null
        server.start()
        // PERCY_SERVER_ADDRESS env var is set by Gradle build.gradle test task
    }

    @AfterAll
    static void stopServer() {
        server?.stop(0)
    }

    @BeforeEach
    void setUp() {
        PercyKeywords.resetCache()
        snapshotBodies.clear()

        mockDriver = mock(WebDriver, withSettings().extraInterfaces(JavascriptExecutor))
        JavascriptExecutor js = (JavascriptExecutor) mockDriver

        // Use doAnswer to avoid Groovy/Mockito varargs matcher issues with executeScript
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

    @Test
    @Order(1)
    void testPercySnapshotSendsRequest() {
        def result = PercyKeywords.percySnapshot("Homepage")
        println "DEBUG: result=${result}, snapshotBodies.size=${snapshotBodies.size()}, logMessages=${logMessages}"
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
    void testPercyInstanceCachedAcrossCalls() {
        PercyKeywords.percySnapshot("Page 1")
        PercyKeywords.percySnapshot("Page 2")
        assertTrue(snapshotBodies.size() >= 2, "Expected at least 2 snapshot requests")
    }

    @Test
    @Order(6)
    void testDriverChangeCreatesNewPercy() {
        WebDriver secondDriver = mock(WebDriver, withSettings().extraInterfaces(JavascriptExecutor))
        JavascriptExecutor js2 = (JavascriptExecutor) secondDriver
        doAnswer({ invocation ->
            String script = invocation.getArgument(0) as String
            if (script != null && script.contains("PercyDOM") && script.contains("serialize")) {
                return [html: '<html><body>second</body></html>', cookies: []]
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
    @Order(7)
    void testNullDriverReturnsNull() {
        driverFactoryMock.when(DriverFactory::getWebDriver).thenReturn(null)
        def result = PercyKeywords.percySnapshot("Homepage")
        assertNull(result)
        assertEquals(0, snapshotBodies.size())
    }

    @Test
    @Order(8)
    void testNoClassDefErrorCaughtGracefully() {
        driverFactoryMock.when(DriverFactory::getWebDriver).thenThrow(
            new NoClassDefFoundError("com/kms/katalon/core/webui/driver/DriverFactory")
        )
        def result = PercyKeywords.percySnapshot("Homepage")
        assertNull(result)
        assertEquals(0, snapshotBodies.size())
    }
}
