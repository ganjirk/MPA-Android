package com.grk.mpa.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

val Destination.icon: ImageVector
    get() = when (this) {
        Destination.HOME -> Icons.Filled.Home
        Destination.GAMES -> Icons.Filled.PlayArrow
        Destination.UTILITIES -> Icons.Filled.Settings
        Destination.PRODUCTIVITY -> Icons.Filled.Check
        Destination.ACCOUNT -> Icons.Filled.Person
    }
