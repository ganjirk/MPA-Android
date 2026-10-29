package com.grk.mpa.app.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grk.mpa.app.R
import com.grk.mpa.app.core.FixedStepClock
import com.grk.mpa.app.core.PongSide

@Composable
fun PongScreen(
    modifier: Modifier = Modifier,
    viewModel: PongViewModel = viewModel()
) {
    val state = viewModel.state
    val config = state.config

    // Drive the game at a fixed 60 steps per second, whatever the screen refresh rate is.
    LaunchedEffect(viewModel) {
        val clock = FixedStepClock()
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) viewModel.onFrame(clock.advance(now - last))
                last = now
            }
        }
    }

    val fieldColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val lineColor = MaterialTheme.colorScheme.outline
    val paddleColor = MaterialTheme.colorScheme.primary
    val ballColor = MaterialTheme.colorScheme.secondary

    Column(
        modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            stringResource(R.string.pong_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScoreBox(stringResource(R.string.pong_player_left), state.leftScore)
            ScoreBox(stringResource(R.string.pong_player_right), state.rightScore)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(config.width / config.height)
                .clip(RoundedCornerShape(16.dp))
                .background(fieldColor)
                .pointerInput(viewModel) {
                    // Touching the left or right third of the field moves that player's paddle.
                    // Both thirds can be touched at once, so two players can share the screen.
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    val fraction = change.position.x / size.width
                                    val fieldY = change.position.y / size.height * config.height
                                    when {
                                        fraction < 1f / 3 -> viewModel.movePaddle(PongSide.LEFT, fieldY)
                                        fraction > 2f / 3 -> viewModel.movePaddle(PongSide.RIGHT, fieldY)
                                    }
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val scale = size.width / config.width
            drawLine(
                color = lineColor,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f))
            )
            val paddleSize = Size(config.paddleWidth * scale, config.paddleHeight * scale)
            drawRect(
                paddleColor,
                topLeft = Offset(0f, (state.leftPaddleY - config.paddleHeight / 2) * scale),
                size = paddleSize
            )
            drawRect(
                paddleColor,
                topLeft = Offset(
                    (config.width - config.paddleWidth) * scale,
                    (state.rightPaddleY - config.paddleHeight / 2) * scale
                ),
                size = paddleSize
            )
            drawCircle(
                ballColor,
                radius = config.ballSize / 2 * scale,
                center = Offset(state.ballX * scale, state.ballY * scale)
            )
        }
        Text(
            stringResource(R.string.pong_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::togglePause) {
                Text(stringResource(if (viewModel.isPaused) R.string.pong_resume else R.string.pong_pause))
            }
            OutlinedButton(onClick = viewModel::reset) {
                Text(stringResource(R.string.pong_new_game))
            }
        }
    }
}

@Composable
private fun ScoreBox(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            score.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
