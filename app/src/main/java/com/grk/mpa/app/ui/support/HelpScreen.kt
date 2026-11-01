package com.grk.mpa.app.ui.support

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grk.mpa.app.R
import com.grk.mpa.app.data.api.ApiConfig
import com.grk.mpa.app.ui.theme.MpaDimensions

private val FAQ = listOf(
    R.string.help_q1 to R.string.help_a1,
    R.string.help_q2 to R.string.help_a2,
    R.string.help_q3 to R.string.help_a3,
    R.string.help_q4 to R.string.help_a4,
    R.string.help_q5 to R.string.help_a5,
    R.string.help_q6 to R.string.help_a6,
    R.string.help_q7 to R.string.help_a7
)

private val TIPS = listOf(R.string.help_tip_1, R.string.help_tip_2, R.string.help_tip_3, R.string.help_tip_4)

@Composable
fun HelpScreen(onBack: () -> Unit, onContact: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    var openQuestion by rememberSaveable { mutableStateOf(-1) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MpaDimensions.screenHorizontalPadding)
            .padding(top = MpaDimensions.compactSpacing, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.support_back)) }
            OutlinedButton(onClick = onContact) { Text(stringResource(R.string.support_contact_link)) }
        }
        Text(
            stringResource(R.string.help_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            stringResource(R.string.help_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        InfoCard(stringResource(R.string.help_about_title)) {
            listOf(
                R.string.help_about_games, R.string.help_about_utilities,
                R.string.help_about_productivity, R.string.help_about_account
            ).forEach { Body(stringResource(it)) }
        }

        InfoCard(stringResource(R.string.help_quick_title)) {
            Body(stringResource(R.string.help_quick_body))
            Body(stringResource(R.string.help_quick_examples))
            Text(
                stringResource(R.string.help_tips_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TIPS.forEach { Body("• " + stringResource(it)) }
            TextButton(onClick = { uriHandler.openUri(ApiConfig.website("")) }) {
                Text(stringResource(R.string.help_open_website))
            }
        }

        InfoCard(stringResource(R.string.help_faq_title)) {
            FAQ.forEachIndexed { index, (question, answer) ->
                val open = openQuestion == index
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { openQuestion = if (open) -1 else index }
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        (if (open) "▾  " else "▸  ") + stringResource(question),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (open) Body(stringResource(answer))
                }
            }
            Body(stringResource(R.string.help_still_stuck))
            Button(onClick = onContact) { Text(stringResource(R.string.support_contact_link)) }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}
