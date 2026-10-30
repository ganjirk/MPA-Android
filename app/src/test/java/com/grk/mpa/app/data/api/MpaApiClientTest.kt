package com.grk.mpa.app.data.api

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MpaApiClientTest {
    private lateinit var server: HttpServer
    private var handler: (HttpExchange) -> Unit = { it.reply(200, "{}") }
    private var lastPath: String? = null
    private var lastMethod: String? = null
    private var lastAuth: String? = null
    private var lastBody: String? = null

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/") { exchange ->
            lastPath = exchange.requestURI.path
            lastMethod = exchange.requestMethod
            lastAuth = exchange.requestHeaders.getFirst("Authorization")
            lastBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            handler(exchange)
        }
        server.start()
    }

    @After
    fun stopServer() = server.stop(0)

    private fun HttpExchange.reply(code: Int, body: String, vararg headers: Pair<String, String>) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        responseHeaders.add("Content-Type", "application/json")
        headers.forEach { (name, value) -> responseHeaders.add(name, value) }
        sendResponseHeaders(code, if (bytes.isEmpty()) -1L else bytes.size.toLong())
        if (bytes.isNotEmpty()) responseBody.use { it.write(bytes) } else close()
    }

    private fun client() = MpaApiClient("http://127.0.0.1:${server.address.port}/")

    @Test
    fun loginPostsCredentialsAsJsonAndParsesTokens() = runBlocking {
        handler = { it.reply(200, """{"refresh":"r1","access":"a1"}""") }
        val result = client().login("mobile-user", "s3cret")
        assertEquals(ApiResult.Success(TokenPair("a1", "r1")), result)
        assertEquals("/api/v1/auth/token/", lastPath)
        assertEquals("POST", lastMethod)
        assertTrue(lastBody!!.contains(""""username":"mobile-user""""))
        assertTrue(lastBody!!.contains(""""password":"s3cret""""))
    }

    @Test
    fun wrongCredentialsAreUnauthorized() = runBlocking {
        handler = { it.reply(401, """{"detail":"No active account found"}""") }
        assertEquals(ApiResult.Unauthorized, client().login("u", "bad"))
    }

    @Test
    fun throttledRequestsAreRateLimited() = runBlocking {
        handler = { it.reply(429, """{"detail":"Request was throttled."}""") }
        assertEquals(ApiResult.RateLimited, client().login("u", "p"))
    }

    @Test
    fun otherErrorsKeepTheirStatusCode() = runBlocking {
        handler = { it.reply(500, "oops") }
        assertEquals(ApiResult.HttpError(500), client().login("u", "p"))
    }

    @Test
    fun accountSendsBearerTokenAndIgnoresUnknownFields() = runBlocking {
        handler = {
            it.reply(
                200,
                """{"id":3,"username":"u","first_name":"Ada","last_name":"Lovelace","email":"a@x.com","email_verified":true,"new_field":1}"""
            )
        }
        val result = client().account("tok123")
        assertEquals(ApiResult.Success(Account(3, "u", "Ada", "Lovelace", "a@x.com", true)), result)
        assertEquals("GET", lastMethod)
        assertEquals("/api/v1/account/", lastPath)
        assertEquals("Bearer tok123", lastAuth)
    }

    @Test
    fun refreshReturnsTheRotatedRefreshTokenWhenPresent() = runBlocking {
        handler = { it.reply(200, """{"access":"a2","refresh":"r2"}""") }
        assertEquals(ApiResult.Success(RefreshedTokens("a2", "r2")), client().refresh("r1"))
        assertEquals("/api/v1/auth/token/refresh/", lastPath)
        assertTrue(lastBody!!.contains(""""refresh":"r1""""))
    }

    @Test
    fun refreshWithoutANewRefreshTokenIsAccepted() = runBlocking {
        handler = { it.reply(200, """{"access":"a2"}""") }
        val result = client().refresh("r1") as ApiResult.Success
        assertEquals("a2", result.value.access)
        assertNull(result.value.refresh)
    }

    @Test
    fun malformedSuccessBodiesAreBadResponses() = runBlocking {
        handler = { it.reply(200, "this is not json") }
        assertEquals(ApiResult.BadResponse, client().login("u", "p"))
        handler = { it.reply(200, """{"unexpected":true}""") }
        assertEquals(ApiResult.BadResponse, client().account("tok"))
    }

    @Test
    fun redirectsAreNotFollowed() = runBlocking {
        handler = { it.reply(302, "", "Location" to "http://127.0.0.1:1/stolen") }
        assertEquals(ApiResult.HttpError(302), client().account("tok"))
    }

    @Test
    fun unreachableServerIsANetworkError() = runBlocking {
        val freePort = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { it.localPort }
        val offline = MpaApiClient("http://127.0.0.1:$freePort/")
        assertEquals(ApiResult.NetworkError, offline.login("u", "p"))
    }

    @Test
    fun plainHttpIsRefusedExceptForLoopback() {
        assertThrows(IllegalArgumentException::class.java) { MpaApiClient("http://example.com/") }
        assertThrows(IllegalArgumentException::class.java) { MpaApiClient("ftp://example.com/") }
        MpaApiClient("https://example.com/")
        MpaApiClient("http://localhost:8000/")
        MpaApiClient("http://127.0.0.1:8000")
    }

    @Test
    fun defaultBaseUrlIsHttps() {
        assertTrue(ApiConfig.BASE_URL.startsWith("https://"))
        assertEquals("https://mpa.pythonanywhere.com/signup/", ApiConfig.website("/signup/"))
        MpaApiClient()
    }
}
