package com.grk.mpa.app.ui.navigation

import androidx.annotation.StringRes
import com.grk.mpa.app.R

/** Top-level destinations shown in the bottom navigation bar. */
enum class Destination(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
) {
    HOME("home", R.string.nav_home, R.string.desc_home),
    GAMES("games", R.string.nav_games, R.string.desc_games),
    UTILITIES("utilities", R.string.nav_utilities, R.string.desc_utilities),
    PRODUCTIVITY("productivity", R.string.nav_productivity, R.string.desc_productivity),
    ACCOUNT("account", R.string.nav_account, R.string.desc_account);

    companion object {
        /** Destinations reachable from the home screen cards (everything except home). */
        val sections: List<Destination> = entries.filter { it != HOME }
    }
}
