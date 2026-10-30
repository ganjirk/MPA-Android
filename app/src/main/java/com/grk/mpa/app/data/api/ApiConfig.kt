package com.grk.mpa.app.data.api

/** Where the MPA website and its mobile API live. */
object ApiConfig {
    const val BASE_URL = "https://mpa.pythonanywhere.com/"

    /** A page on the MPA website, e.g. `website("signup/")`. */
    fun website(path: String): String = BASE_URL + path.trimStart('/')
}
