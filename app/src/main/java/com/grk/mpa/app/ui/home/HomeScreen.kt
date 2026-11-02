package com.grk.mpa.app.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grk.mpa.app.R
import com.grk.mpa.app.data.auth.EncryptedAuthStorage
import com.grk.mpa.app.ui.navigation.Destination
import com.grk.mpa.app.ui.navigation.icon
import com.grk.mpa.app.ui.theme.MpaPortalDarkEnd
import com.grk.mpa.app.ui.theme.MpaPortalDarkStart
import com.grk.mpa.app.ui.theme.MpaPortalGradientEnd
import com.grk.mpa.app.ui.theme.MpaPortalGradientStart
import com.grk.mpa.app.ui.theme.MpaPortalTopBar
import com.grk.mpa.app.ui.theme.MpaPortalTopBarDark

@StringRes
private fun Destination.featureSubtitleRes(): Int = when (this) {
    Destination.GAMES -> R.string.feature_games
    Destination.UTILITIES -> R.string.feature_utilities
    Destination.PRODUCTIVITY -> R.string.feature_productivity
    Destination.ACCOUNT -> R.string.feature_account
    Destination.HOME -> R.string.desc_home
}

/** The home screen follows the website: gradient page, navy bar, white hero text and one tile per app. */
@Composable
fun HomeScreen(
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    onOpenHelp: () -> Unit = {},
    onOpenContact: () -> Unit = {},
    onOpenRecap: () -> Unit = {}
) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    var signedIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        signedIn = runCatching { EncryptedAuthStorage(context.applicationContext).load() != null }.getOrDefault(false)
    }
    var query by rememberSaveable { mutableStateOf("") }

    val gradient = Brush.linearGradient(
        if (dark) listOf(MpaPortalDarkStart, MpaPortalDarkEnd) else listOf(MpaPortalGradientStart, MpaPortalGradientEnd)
    )

    Column(modifier.fillMaxSize().background(gradient)) {
        TopBar(dark)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Hero(signedIn, onSignIn = { onSelect(Destination.ACCOUNT) })
            QuickLinks(onOpenRecap, onOpenHelp, onOpenContact)
            SearchField(query) { query = it }

            val tiles = Destination.sections.filter {
                HomeSearch.matches(
                    query,
                    stringResource(it.titleRes),
                    stringResource(it.featureSubtitleRes())
                )
            }
            tiles.forEach { AppTile(it, dark) { onSelect(it) } }
            if (tiles.isEmpty()) {
                Text(
                    stringResource(R.string.home_no_match),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TopBar(dark: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(if (dark) MpaPortalTopBarDark else MpaPortalTopBar)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            stringResource(R.string.home_brand),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun Hero(signedIn: Boolean, onSignIn: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.home_headline),
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            stringResource(R.string.home_welcome),
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.bodyMedium
        )
        if (!signedIn) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSignIn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MpaPortalTopBar)
                ) { Text(stringResource(R.string.home_signin), fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = onSignIn,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text(stringResource(R.string.home_create_account)) }
            }
        }
    }
}

@Composable
private fun QuickLinks(onRecap: () -> Unit, onHelp: () -> Unit, onContact: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(stringResource(R.string.recap_link), onRecap)
        Pill(stringResource(R.string.support_help_link), onHelp)
        Pill(stringResource(R.string.support_contact_link), onContact)
    }
}

@Composable
private fun Pill(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.home_search_hint)) },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun AppTile(destination: Destination, dark: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 2.dp
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (dark) Color(0x24 shl 24 or 0x90CDF4) else Color(0x17 shl 24 or 0x003399)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    destination.icon,
                    contentDescription = null,
                    tint = if (dark) Color(0xFF4A90E2) else Color(0xFF003399)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(destination.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(destination.featureSubtitleRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
