package com.grk.mpa.app.data.api

import com.grk.mpa.app.core.ContactDraft
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface ContactResult {
    /** The message was saved. [reference] is the ID to quote later, when the website gave one. */
    data class Sent(val reference: String?) : ContactResult

    /** The website did not accept the values (for example a message that is too short). */
    data object Rejected : ContactResult
    data object RateLimited : ContactResult
    data object NetworkError : ContactResult
    data class Failed(val code: Int) : ContactResult
}

interface ContactApi {
    suspend fun send(draft: ContactDraft): ContactResult
}

/**
 * Sends a message through the website's public Contact form, the same way a browser does: it loads the page to get
 * the security token, posts the form, and reads the reference ID from the confirmation page.
 * Messages are therefore sent as a visitor (name and email typed in), not tied to a signed-in account.
 * HTTPS only (plain http just for loopback, for tests). Redirects are followed by hand and only on the same host.
 */
class WebsiteContactClient(baseUrl: String = ApiConfig.BASE_URL) : ContactApi {
    private val base: HttpUrl = (baseUrl.trimEnd('/') + "/").also {
        require(isSecure(it)) { "Contact must be reached over https" }
    }.toHttpUrl()

    private class MemoryCookies : CookieJar {
        private val cookies = mutableListOf<Cookie>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { fresh ->
                this.cookies.removeAll { it.name == fresh.name && it.domain == fresh.domain && it.path == fresh.path }
                this.cookies += fresh
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.filter { it.matches(url) }
    }

    private fun newClient(): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(MemoryCookies())
        .connectTimeout(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build()

    override suspend fun send(draft: ContactDraft): ContactResult = withContext(Dispatchers.IO) {
        try {
            val http = newClient()
            val formUrl = base.resolve("contactUs/")!!

            val page = http.newCall(Request.Builder().url(formUrl).header("Accept", "text/html").build())
                .execute().use { response ->
                    if (response.code == 429) return@withContext ContactResult.RateLimited
                    if (response.code != 200) return@withContext ContactResult.Failed(response.code)
                    response.body?.string().orEmpty()
                }
            val token = TOKEN.find(page)?.groupValues?.get(1) ?: return@withContext ContactResult.Failed(0)

            val form = FormBody.Builder()
                .add("csrfmiddlewaretoken", token)
                .add("nickname", "") // the website's hidden bot trap: people leave it empty
                .add("name", draft.name)
                .add("email", draft.email)
                .add("topic", draft.topic.wire)
                .add("subject", draft.subject)
                .add("message", draft.message)
                .build()
            val post = Request.Builder()
                .url(formUrl)
                .header("Referer", formUrl.toString())
                .header("Origin", "${base.scheme}://${base.host}" + if (base.port !in listOf(80, 443)) ":${base.port}" else "")
                .post(form)
                .build()
            http.newCall(post).execute().use { response ->
                when (response.code) {
                    200 -> ContactResult.Rejected
                    429 -> ContactResult.RateLimited
                    302, 303 -> {
                        val next = response.header("Location")?.let { formUrl.resolve(it) }
                        ContactResult.Sent(next?.takeIf { it.host == base.host }?.let { readReference(http, it) })
                    }
                    else -> ContactResult.Failed(response.code)
                }
            }
        } catch (e: IOException) {
            ContactResult.NetworkError
        }
    }

    private fun readReference(http: OkHttpClient, url: HttpUrl): String? = try {
        http.newCall(Request.Builder().url(url).header("Accept", "text/html").build()).execute().use { response ->
            if (response.code != 200) null
            else REFERENCE.find(response.body?.string().orEmpty())?.groupValues?.get(1)
        }
    } catch (e: IOException) {
        null // the message was already saved; only the reference could not be read
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000L
        val TOKEN = Regex("name=\"csrfmiddlewaretoken\"\\s+value=\"([^\"]+)\"")
        val REFERENCE = Regex("id=\"sentReference\">\\s*([A-Z0-9-]+)\\s*<")
        val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")

        fun isSecure(url: String): Boolean {
            val uri = runCatching { URI(url) }.getOrNull() ?: return false
            return uri.scheme == "https" || (uri.scheme == "http" && uri.host in LOOPBACK_HOSTS)
        }
    }
}
