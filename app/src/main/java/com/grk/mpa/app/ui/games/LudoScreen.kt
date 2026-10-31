package com.grk.mpa.app.ui.games

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.grk.mpa.app.R
import com.grk.mpa.app.core.Ludo
import com.grk.mpa.app.core.LudoBoardLayout
import com.grk.mpa.app.core.LudoEvent
import com.grk.mpa.app.core.LudoKind
import com.grk.mpa.app.core.LudoPhase
import com.grk.mpa.app.core.LudoState
import com.grk.mpa.app.data.DataStoreLudoRepository
import com.grk.mpa.app.ui.theme.MpaDimensions
import kotlinx.coroutines.delay
import kotlin.random.Random

private val LUDO_COLORS = listOf(Color(0xFFC62828), Color(0xFF2E7D32), Color(0xFFF9A825), Color(0xFF1565C0))
private val LUDO_COLOR_NAMES = listOf(
    R.string.ludo_color_red, R.string.ludo_color_green, R.string.ludo_color_yellow, R.string.ludo_color_blue
)
private val LUDO_RULES = listOf(
    R.string.ludo_rule_1, R.string.ludo_rule_2, R.string.ludo_rule_3, R.string.ludo_rule_4,
    R.string.ludo_rule_5, R.string.ludo_rule_6, R.string.ludo_rule_7
)

private val ludoViewModelFactory = viewModelFactory {
    initializer {
        LudoViewModel(DataStoreLudoRepository(checkNotNull(this[APPLICATION_KEY])))
    }
}

@StringRes
private fun kindLabel(kind: LudoKind): Int = when (kind) {
    LudoKind.HUMAN -> R.string.ludo_kind_human
    LudoKind.CPU -> R.string.ludo_kind_cpu
    LudoKind.EASY -> R.string.ludo_kind_easy
}



@Composable
fun LudoScreen(
    modifier: Modifier = Modifier,
    viewModel: LudoViewModel = viewModel(factory = ludoViewModelFactory)
) {
    val state = viewModel.state
    var rolling by remember { mutableStateOf(false) }
    var spinFace by remember { mutableIntStateOf(0) }

    // Animate the dice, then apply the roll.
    LaunchedEffect(rolling) {
        if (rolling) {
            repeat(7) {
                spinFace = Random.nextInt(1, 7)
                delay(70)
            }
            viewModel.roll()
            rolling = false
        }
    }

    // Computer turns and forced moves only run after Start Game.
    LaunchedEffect(state, viewModel.isLoaded, viewModel.gameStarted, rolling) {
        if (!viewModel.isLoaded || !viewModel.gameStarted ||
            state.phase == LudoPhase.OVER || rolling
        ) return@LaunchedEffect

        if (state.phase == LudoPhase.ROLL) {
            if (!viewModel.isHumanTurn) {
                delay(750)
                rolling = true
            }
        } else if (!viewModel.canPickToken) {
            delay(if (viewModel.isHumanTurn) 600 else 850)
            viewModel.autoMove()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MpaDimensions.screenHorizontalPadding)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.ludo_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (viewModel.gameStarted) {
                OutlinedButton(
                    onClick = {
                        rolling = false
                        viewModel.newGame()
                    },
                    enabled = viewModel.isLoaded
                ) {
                    Text(stringResource(R.string.ludo_new_game))
                }
            }
        }

        StatusCard(viewModel, false)

        if (!viewModel.gameStarted) {
            SetupSection(viewModel)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Box(Modifier.padding(8.dp)) {
                    LudoBoard(
                        state = state,
                        highlighted = if (viewModel.canPickToken) {
                            viewModel.legalTokens
                        } else {
                            emptyList()
                        },
                        onTokenClick = { seat, token ->
                            viewModel.onTokenClick(seat, token)
                        }
                    )
                }
            }

            DiceRow(viewModel, rolling, spinFace) {
                rolling = true
            }

            if (viewModel.canPickToken) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
                ) {
                    viewModel.legalTokens.forEach { token ->
                        OutlinedButton(
                            onClick = {
                                viewModel.onTokenClick(state.turn, token)
                            }
                        ) {
                            Text(stringResource(R.string.ludo_move_token, token + 1))
                        }
                    }
                }
            }
        }

        HowToPlayCard()
    }
}

@Composable
private fun SetupSection(viewModel: LudoViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
        ) {
            Text(
                stringResource(R.string.ludo_setup_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                stringResource(R.string.ludo_players),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
                (2..4).forEach { count ->
                    FilterChip(
                        selected = viewModel.playerCount == count,
                        onClick = { viewModel.onPlayerCountChange(count) },
                        enabled = viewModel.isLoaded && !viewModel.gameStarted,
                        label = { Text(count.toString()) }
                    )
                }
            }

            Ludo.coloursFor(viewModel.playerCount).forEach { color ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.size(14.dp)
                                .background(LUDO_COLORS[color], CircleShape)
                        )
                        Text(
                            stringResource(LUDO_COLOR_NAMES[color]),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
                    ) {
                        LudoKind.entries.forEach { kind ->
                            FilterChip(
                                selected = viewModel.kinds[color] == kind,
                                onClick = {
                                    viewModel.onKindChange(color, kind)
                                },
                                enabled = viewModel.isLoaded && !viewModel.gameStarted,
                                label = { Text(stringResource(kindLabel(kind))) }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.startGame() },
                enabled = viewModel.isLoaded && !viewModel.gameStarted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ludo_start_game))
            }
        }
    }
}



@Composable
private fun colorName(color: Int): String = stringResource(LUDO_COLOR_NAMES[color])


@Composable
private fun StatusCard(viewModel: LudoViewModel, newGameStarted: Boolean) {
    val state = viewModel.state

    val who = colorName(state.seats[state.turn].color)
    val human = viewModel.isHumanTurn

    val hint = when {
        !viewModel.isLoaded ->
            stringResource(R.string.ludo_loading_game)

        !viewModel.gameStarted ->
            stringResource(R.string.ludo_setup_hint)

        state.phase == LudoPhase.OVER ->
            stringResource(
                R.string.ludo_hint_over,
                colorName(state.seats[state.winner].color)
            )

        state.phase == LudoPhase.ROLL ->
            if (human) {
                stringResource(R.string.ludo_hint_roll_human, who)
            } else {
                stringResource(R.string.ludo_hint_roll_cpu, who)
            }

        human ->
            if (viewModel.canPickToken) {
                stringResource(R.string.ludo_hint_pick, who, state.dice)
            } else {
                stringResource(R.string.ludo_hint_moving, who)
            }

        else ->
            stringResource(R.string.ludo_hint_cpu_choosing, who)
    }

    val last = when {
        !viewModel.isLoaded -> ""
        !viewModel.gameStarted -> ""
        viewModel.restored -> stringResource(R.string.ludo_welcome_back)
        else -> eventText(state)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                hint,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (last.isNotEmpty()) {
                Text(
                    last,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun eventText(state: LudoState): String {
    val parts = state.events.map { e ->
        when (e) {
            is LudoEvent.Rolled -> stringResource(R.string.ludo_ev_roll, colorName(state.seats[e.seat].color), e.value)
            is LudoEvent.NoMove -> stringResource(R.string.ludo_ev_nomove)
            is LudoEvent.TripleSix -> stringResource(R.string.ludo_ev_triple)
            is LudoEvent.Moved ->
                if (e.from == -1) stringResource(R.string.ludo_ev_out, colorName(state.seats[e.seat].color), e.token + 1)
                else stringResource(R.string.ludo_ev_moved, colorName(state.seats[e.seat].color), e.token + 1, e.value)
            is LudoEvent.ReachedHome -> stringResource(R.string.ludo_ev_home, e.token + 1)
            is LudoEvent.Captured -> stringResource(R.string.ludo_ev_capture, colorName(state.seats[e.victimSeat].color), e.token + 1)
            is LudoEvent.Won -> ""
        }
    }
    return parts.filter { it.isNotEmpty() }.joinToString(" ")
}

@Composable
private fun DiceRow(viewModel: LudoViewModel, rolling: Boolean, spinFace: Int, onRoll: () -> Unit) {
    val state = viewModel.state
    val canRoll = viewModel.canRoll && !rolling
    val face = if (rolling) spinFace else state.dice
    val color = LUDO_COLORS[state.seats[state.turn].color]
    val label = when {
        state.phase == LudoPhase.OVER -> R.string.ludo_dice_done
        canRoll -> R.string.ludo_dice_roll
        viewModel.isHumanTurn -> if (state.phase == LudoPhase.MOVE) R.string.ludo_dice_move else R.string.ludo_dice_wait
        else -> R.string.ludo_dice_computer
    }
    val description = when {
        canRoll -> stringResource(R.string.ludo_dice_cd_roll, colorName(state.seats[state.turn].color))
        state.dice > 0 -> stringResource(R.string.ludo_dice_cd_last, state.dice)
        else -> stringResource(R.string.ludo_dice_cd)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onRoll,
            enabled = canRoll,
            modifier = Modifier.semantics { contentDescription = description }
        ) {
            Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
        }
        DiceFace(face, color)
    }
}

@Composable
private fun DiceFace(value: Int, accent: Color) {
    Canvas(
        Modifier
            .size(56.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(3.dp, accent, RoundedCornerShape(10.dp))
    ) {
        val pips = when (value) {
            1 -> listOf(1 to 1)
            2 -> listOf(0 to 0, 2 to 2)
            3 -> listOf(0 to 0, 1 to 1, 2 to 2)
            4 -> listOf(0 to 0, 0 to 2, 2 to 0, 2 to 2)
            5 -> listOf(0 to 0, 0 to 2, 1 to 1, 2 to 0, 2 to 2)
            6 -> listOf(0 to 0, 1 to 0, 2 to 0, 0 to 2, 1 to 2, 2 to 2)
            else -> emptyList()
        }
        val step = size.width / 4f
        pips.forEach { (r, c) ->
            drawCircle(Color(0xFF263238), radius = step * 0.28f, center = Offset(step * (c + 1), step * (r + 1)))
        }
    }
}

@Composable
private fun LudoBoard(
    state: LudoState,
    highlighted: List<Int>,
    onTokenClick: (seat: Int, token: Int) -> Unit
) {
    val measurer = rememberTextMeasurer()
    val placements = LudoBoardLayout.placements(state)
    val currentPlacements by rememberUpdatedState(placements)
    val currentHighlighted by rememberUpdatedState(highlighted)
    val currentTurn by rememberUpdatedState(state.turn)
    val currentClick by rememberUpdatedState(onTokenClick)
    val description = stringResource(R.string.ludo_board_description)

    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val cell = size.width / Ludo.BOARD_SIZE.toFloat()
                    val hit = currentPlacements
                        .filter { it.seat == currentTurn && it.token in currentHighlighted }
                        .minByOrNull {
                            val dx = it.x * cell - tap.x
                            val dy = it.y * cell - tap.y
                            dx * dx + dy * dy
                        }
                    if (hit != null) {
                        val dx = hit.x * cell - tap.x
                        val dy = hit.y * cell - tap.y
                        val reach = maxOf(hit.size, 0.9f) * cell / 2f
                        if (dx * dx + dy * dy <= reach * reach) currentClick(hit.seat, hit.token)
                    }
                }
            }
    ) {
        drawBoard(measurer, state)
        val cell = size.width / Ludo.BOARD_SIZE
        placements.forEach { p ->
            val color = state.seats[p.seat].color
            val center = Offset(p.x * cell, p.y * cell)
            val radius = p.size * cell / 2f
            val canMove = p.seat == state.turn && p.token in highlighted
            if (canMove) {
                drawCircle(Color.Black, radius + cell * 0.16f, center)
                drawCircle(Color.White, radius + cell * 0.09f, center)
            }
            drawCircle(LUDO_COLORS[color], radius, center)
            drawCircle(Color.White, radius, center, style = Stroke(width = cell * 0.07f))
            val style = TextStyle(
                color = if (color == 2) Color(0xFF111111) else Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (radius * 1.1f).toSp()
            )
            val text = measurer.measure((p.token + 1).toString(), style)
            drawText(text, topLeft = Offset(center.x - text.size.width / 2f, center.y - text.size.height / 2f))
        }
    }
}

private fun DrawScope.drawBoard(measurer: TextMeasurer, state: LudoState) {
    val cell = size.width / Ludo.BOARD_SIZE
    val line = Color(0xFF90A4AE)
    val frame = Color(0xFF37474F)
    drawRect(Color.White, Offset.Zero, size)

    fun cellTopLeft(row: Int, col: Int) = Offset(col * cell, row * cell)
    fun fillCell(row: Int, col: Int, color: Color) {
        drawRect(color, cellTopLeft(row, col), Size(cell, cell))
    }
    fun outlineCell(row: Int, col: Int) {
        drawRect(line, cellTopLeft(row, col), Size(cell, cell), style = Stroke(width = 1f))
    }

    val corners = listOf(0 to 0, 0 to 9, 9 to 9, 9 to 0)
    for (c in 0..3) {
        val (r0, c0) = corners[c]
        val origin = cellTopLeft(r0, c0)
        drawRect(LUDO_COLORS[c].copy(alpha = if (c in state.seats.map { it.color }) 1f else 0.3f), origin, Size(cell * 6, cell * 6))
        drawRect(Color.White, origin + Offset(cell, cell), Size(cell * 4, cell * 4))
        Ludo.BASE[c].forEach { b ->
            drawCircle(LUDO_COLORS[c], cell * 0.4f, Offset((b.col + 0.5f) * cell, (b.row + 0.5f) * cell))
            drawCircle(Color(0x40000000), cell * 0.4f, Offset((b.col + 0.5f) * cell, (b.row + 0.5f) * cell), style = Stroke(2f))
        }
        Ludo.HOME[c].forEach { h ->
            fillCell(h.row, h.col, LUDO_COLORS[c].copy(alpha = 0.82f))
            outlineCell(h.row, h.col)
        }
        drawRect(frame, origin, Size(cell * 6, cell * 6), style = Stroke(width = 2f))
    }
    // The seat whose turn it is gets a dark frame around its base
    if (!state.isOver) {
        val (r0, c0) = corners[state.seats[state.turn].color]
        drawRect(Color.Black, cellTopLeft(r0, c0), Size(cell * 6, cell * 6), style = Stroke(width = cell * 0.18f))
    }
    Ludo.TRACK.forEachIndexed { i, sq ->
        val owner = Ludo.START.indexOf(i)
        if (owner >= 0) fillCell(sq.row, sq.col, LUDO_COLORS[owner])
        outlineCell(sq.row, sq.col)
        if (owner >= 0 || i in Ludo.STARS) {
            val text = measurer.measure(
                "★",
                TextStyle(
                    color = if (owner == 2) Color(0xFF111111) else if (owner >= 0) Color.White else Color(0xFF455A64),
                    fontSize = (cell * 0.6f).toSp()
                )
            )
            drawText(
                text,
                topLeft = Offset(sq.col * cell + (cell - text.size.width) / 2f, sq.row * cell + (cell - text.size.height) / 2f)
            )
        }
    }
    // Centre block: one triangle per colour
    val left = 6 * cell
    val top = 6 * cell
    val right = 9 * cell
    val bottom = 9 * cell
    val mid = Offset(7.5f * cell, 7.5f * cell)
    fun triangle(color: Color, a: Offset, b: Offset) {
        val path = Path().apply {
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(mid.x, mid.y)
            close()
        }
        drawPath(path, color)
    }
    triangle(LUDO_COLORS[1], Offset(left, top), Offset(right, top))
    triangle(LUDO_COLORS[2], Offset(right, top), Offset(right, bottom))
    triangle(LUDO_COLORS[3], Offset(right, bottom), Offset(left, bottom))
    triangle(LUDO_COLORS[0], Offset(left, bottom), Offset(left, top))
    drawRect(frame, Offset(left, top), Size(cell * 3, cell * 3), style = Stroke(width = 2f))
    drawRect(frame, Offset.Zero, size, style = Stroke(width = 6f))
}

@Composable
private fun HowToPlayCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.ludo_how_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            LUDO_RULES.forEach { rule ->
                Text(
                    "• " + stringResource(rule),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.ludo_saved_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
