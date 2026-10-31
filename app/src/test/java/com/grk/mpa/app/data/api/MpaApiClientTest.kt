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
    private var lastQuery: String? = null
    private var lastMethod: String? = null
    private var lastAuth: String? = null
    private var lastBody: String? = null

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/") { exchange ->
            lastPath = exchange.requestURI.path
            lastQuery = exchange.requestURI.rawQuery
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
    fun tasksListUsesBearerTokenAndParsesServerFields() = runBlocking {
        handler = {
            it.reply(
                200,
                """[{"id":17,"text":"Prepare release","notes":"QA first","priority":"high","completed":false,"created_date":"2026-10-30T10:00:00Z","due_date":"2026-11-02","completed_on":null}]"""
            )
        }
        val result = client().tasks("access-1")
        val expected = RemoteTask(
            id = 17,
            text = "Prepare release",
            notes = "QA first",
            priority = "high",
            completed = false,
            createdDate = "2026-10-30T10:00:00Z",
            dueDate = "2026-11-02",
            completedOn = null
        )
        assertEquals(ApiResult.Success(listOf(expected)), result)
        assertEquals("GET", lastMethod)
        assertEquals("/api/v1/tasks/", lastPath)
        assertEquals("Bearer access-1", lastAuth)
    }

    @Test
    fun createTaskPostsOnlyTaskFieldsAndParsesCreatedTask() = runBlocking {
        handler = {
            it.reply(
                201,
                """{"id":18,"text":"Write tests","notes":"","priority":"medium","completed":false,"created_date":"2026-10-31T08:00:00Z","due_date":null,"completed_on":null}"""
            )
        }
        val result = client().createTask(
            "access-2",
            CreateTaskRequest(text = "Write tests", priority = "medium")
        )
        assertTrue(result is ApiResult.Success)
        assertEquals("POST", lastMethod)
        assertEquals("/api/v1/tasks/", lastPath)
        assertEquals("Bearer access-2", lastAuth)
        assertTrue(lastBody!!.contains("\"text\":\"Write tests\""))
        assertTrue(lastBody!!.contains("\"priority\":\"medium\""))
        assertTrue(!lastBody!!.contains("user"))
        assertTrue(!lastBody!!.contains("id"))
    }

    @Test
    fun updateTaskUsesPatchAndCanExplicitlyClearDueDate() = runBlocking {
        handler = {
            it.reply(
                200,
                """{"id":18,"text":"Write tests","notes":"","priority":"","completed":true,"created_date":"2026-10-31T08:00:00Z","due_date":null,"completed_on":"2026-10-31T09:00:00Z"}"""
            )
        }
        val result = client().updateTask(
            "access-3",
            18,
            UpdateTaskRequest(
                completed = true,
                dueDate = kotlinx.serialization.json.JsonNull
            )
        )
        assertTrue(result is ApiResult.Success)
        assertEquals("PATCH", lastMethod)
        assertEquals("/api/v1/tasks/18/", lastPath)
        assertEquals("Bearer access-3", lastAuth)
        assertTrue(lastBody!!.contains("\"completed\":true"))
        assertTrue(lastBody!!.contains("\"due_date\":null"))
        assertTrue(!lastBody!!.contains("\"text\":"))
    }

    @Test
    fun deleteTaskUsesDeleteAndAcceptsNoContent() = runBlocking {
        handler = { it.reply(204, "") }
        assertEquals(ApiResult.Success(Unit), client().deleteTask("access-4", 19))
        assertEquals("DELETE", lastMethod)
        assertEquals("/api/v1/tasks/19/", lastPath)
        assertEquals("Bearer access-4", lastAuth)
    }

    @Test
    fun taskOperationsRejectNonPositiveIds() {
        val api = client()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { api.updateTask("token", 0, UpdateTaskRequest(completed = true)) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { api.deleteTask("token", -1) }
        }
    }

    @Test
    fun expensesCanBeListedForAMonthWithBearerAuth() = runBlocking {
        handler = {
            it.reply(
                200,
                """[{"id":5,"amount":"125.50","category":"food","description":"Lunch","spent_on":"2026-10-05","created_at":"2026-10-05T12:00:00Z"}]"""
            )
        }
        val result = client().expenses("expense-token", "2026-10")
        assertEquals(
            ApiResult.Success(listOf(RemoteExpense(5, "125.50", "food", "Lunch", "2026-10-05", "2026-10-05T12:00:00Z"))),
            result
        )
        assertEquals("/api/v1/expenses/", lastPath)
        assertEquals("GET", lastMethod)
        assertEquals("Bearer expense-token", lastAuth)
        assertEquals("month=2026-10", serverAddressQuery())
    }

    @Test
    fun createExpensePostsOnlyExpenseFields() = runBlocking {
        handler = {
            it.reply(
                201,
                """{"id":9,"amount":"45.25","category":"food","description":"Lunch","spent_on":"2026-10-10","created_at":"2026-10-10T09:00:00Z"}"""
            )
        }
        val result = client().createExpense(
            "expense-token",
            CreateExpenseRequest("45.25", "food", "Lunch", "2026-10-10")
        )
        assertEquals("POST", lastMethod)
        assertEquals("/api/v1/expenses/", lastPath)
        assertEquals("Bearer expense-token", lastAuth)
        assertTrue(lastBody!!.contains("\"amount\":\"45.25\""))
        assertTrue(lastBody!!.contains("\"spent_on\":\"2026-10-10\""))
        assertTrue(!lastBody!!.contains("user"))
        assertTrue(!lastBody!!.contains("\"id\""))
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun updateAndDeleteExpenseUseDetailEndpoint() = runBlocking {
        handler = {
            it.reply(
                200,
                """{"id":9,"amount":"50.00","category":"transport","description":"Bus","spent_on":"2026-10-11","created_at":"2026-10-10T09:00:00Z"}"""
            )
        }
        assertTrue(
            client().updateExpense("expense-token", 9, UpdateExpenseRequest(description = "Bus")) is ApiResult.Success
        )
        assertEquals("PATCH", lastMethod)
        assertEquals("/api/v1/expenses/9/", lastPath)
        assertTrue(lastBody!!.contains("\"description\":\"Bus\""))
        assertTrue(!lastBody!!.contains("\"amount\""))

        handler = { it.reply(204, "") }
        assertEquals(ApiResult.Success(Unit), client().deleteExpense("expense-token", 9))
        assertEquals("DELETE", lastMethod)
        assertEquals("/api/v1/expenses/9/", lastPath)
    }

    @Test
    fun expenseSummaryParsesMonthlyTotalsAndBreakdown() = runBlocking {
        handler = {
            it.reply(
                200,
                """{"month":"2026-10-01","expense_count":2,"total_spent":"50.00","budget_amount":"100.00","remaining_budget":"50.00","category_breakdown":[{"category":"food","label":"Food & dining","amount":"30.00"},{"category":"transport","label":"Transport","amount":"20.00"}]}"""
            )
        }
        val result = client().expenseSummary("expense-token", "2026-10")
        assertEquals(
            ApiResult.Success(
                RemoteExpenseSummary(
                    "2026-10-01", 2, "50.00", "100.00", "50.00",
                    listOf(
                        RemoteExpenseCategoryTotal("food", "Food & dining", "30.00"),
                        RemoteExpenseCategoryTotal("transport", "Transport", "20.00")
                    )
                )
            ),
            result
        )
        assertEquals("/api/v1/expenses/summary/", lastPath)
        assertEquals("month=2026-10", serverAddressQuery())
    }

    @Test
    fun budgetsCanBeListedAndSavedForTheSameMonth() = runBlocking {
        handler = {
            it.reply(
                200,
                """[{"id":4,"month":"2026-10-01","amount":"1000.00","updated_at":"2026-10-01T00:00:00Z"}]"""
            )
        }
        val listed = client().budgets("expense-token")
        assertTrue(listed is ApiResult.Success)
        assertEquals("/api/v1/budgets/", lastPath)
        assertEquals("GET", lastMethod)

        handler = {
            it.reply(
                200,
                """{"id":4,"month":"2026-10-01","amount":"1500.00","updated_at":"2026-10-31T00:00:00Z"}"""
            )
        }
        val saved = client().saveBudget("expense-token", SaveMonthlyBudgetRequest("2026-10-01", "1500.00"))
        assertTrue(saved is ApiResult.Success)
        assertEquals("POST", lastMethod)
        assertEquals("/api/v1/budgets/", lastPath)
        assertTrue(lastBody!!.contains("\"month\":\"2026-10-01\""))
        assertTrue(lastBody!!.contains("\"amount\":\"1500.00\""))
        assertTrue(!lastBody!!.contains("user"))
    }

    @Test
    fun expenseAndBudgetMethodsValidateIdsAndMonthFormats() {
        val api = client()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { api.updateExpense("token", 0, UpdateExpenseRequest(description = "x")) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { api.deleteExpense("token", -1) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { api.expenses("token", "2026-13") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { api.expenseSummary("token", "October") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { api.saveBudget("token", SaveMonthlyBudgetRequest("2026-10-15", "100.00")) }
        }
    }

    private fun serverAddressQuery(): String? = lastQuery

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
