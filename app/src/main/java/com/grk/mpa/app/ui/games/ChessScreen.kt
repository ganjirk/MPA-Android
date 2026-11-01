package com.grk.mpa.app.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.grk.mpa.app.R
import com.grk.mpa.app.core.Chess
import com.grk.mpa.app.core.ChessLevel
import com.grk.mpa.app.core.ChessMode
import com.grk.mpa.app.core.ChessResult
import com.grk.mpa.app.data.DataStoreChessRepository
import com.grk.mpa.app.ui.theme.MpaDimensions

private val LIGHT_SQUARE = Color(0xFFF0D9B5)
private val DARK_SQUARE = Color(0xFFB58863)
private val LAST_LIGHT = Color(0xFFD9D86A)
private val LAST_DARK = Color(0xFFB5A03C)
private val WHITE_PIECE = Color.White
private val BLACK_PIECE = Color(0xFF151515)

/** Filled glyphs for both colours; the trailing selector stops Android drawing the pawn as an emoji. */
private val GLYPHS = listOf("", "♟", "♞", "♝", "♜", "♛", "♚").map { if (it.isEmpty()) it else it + "︎" }

private val chessViewModelFactory = viewModelFactory {
    initializer {
        ChessViewModel(DataStoreChessRepository(checkNotNull(this[APPLICATION_KEY])))
    }
}

@Composable
private fun colorName(color: Int): String =
    stringResource(if (color == 1) R.string.chess_white else R.string.chess_black)

@Composable
private fun statusText(vm: ChessViewModel): String {
    val outcome = vm.outcome
    val you = if (vm.vsComputer) vm.side else 0
    if (outcome != null) {
        val winner = outcome.winner
        return when (outcome.result) {
            ChessResult.CHECKMATE -> when {
                you != 0 && winner == you -> stringResource(R.string.chess_status_checkmate_you)
                you != 0 -> stringResource(R.string.chess_status_checkmate_cpu)
                else -> stringResource(R.string.chess_status_checkmate_color, colorName(winner))
            }
            ChessResult.RESIGN -> when {
                you != 0 && winner == you -> stringResource(R.string.chess_status_resigned_you, colorName(vm.resignedColor))
                you != 0 -> stringResource(R.string.chess_status_resigned_cpu, colorName(vm.resignedColor))
                else -> stringResource(R.string.chess_status_resigned_color, colorName(vm.resignedColor), colorName(winner))
            }
            ChessResult.STALEMATE -> stringResource(R.string.chess_status_stalemate)
            ChessResult.REPETITION -> stringResource(R.string.chess_status_repetition)
            ChessResult.FIFTY -> stringResource(R.string.chess_status_fifty)
            else -> stringResource(R.string.chess_status_material)
        }
    }
    if (vm.thinking) return stringResource(R.string.chess_status_thinking)
    return when {
        vm.vsComputer && vm.inCheck -> stringResource(R.string.chess_status_check_your_move, colorName(vm.side))
        vm.vsComputer -> stringResource(R.string.chess_status_your_move, colorName(vm.side))
        vm.inCheck -> stringResource(R.string.chess_status_check_to_move, colorName(vm.position.turn))
        else -> stringResource(R.string.chess_status_to_move, colorName(vm.position.turn))
    }
}

@Composable
fun ChessScreen(
    modifier: Modifier = Modifier,
    viewModel: ChessViewModel = viewModel(factory = chessViewModelFactory)
) {
    var confirmResign by remember { mutableStateOf(false) }
    val status = statusText(viewModel)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MpaDimensions.screenHorizontalPadding)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) {
        Text(
            stringResource(R.string.chess_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            stringResource(R.string.chess_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SetupSection(viewModel)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (viewModel.inCheck && viewModel.outcome == null)
                    MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Text(
                status,
                modifier = Modifier.padding(12.dp).defaultMinSize(minHeight = 36.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        viewModel.pendingPromotion?.let {
            PromotionRow(onPick = viewModel::onPromotion, onCancel = viewModel::cancelPromotion)
        }
        val bottomWhite = !viewModel.flipped
        CapturedRow(viewModel, byColor = if (bottomWhite) -1 else 1)
        ChessBoard(viewModel, description = status)
        CapturedRow(viewModel, byColor = if (bottomWhite) 1 else -1)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
        ) {
            OutlinedButton(onClick = viewModel::undo, enabled = viewModel.canUndo) {
                Text(stringResource(R.string.chess_undo))
            }
            OutlinedButton(onClick = viewModel::flip) { Text(stringResource(R.string.chess_flip)) }
            OutlinedButton(onClick = { confirmResign = true }, enabled = viewModel.canResign) {
                Text(stringResource(R.string.chess_resign))
            }
        }
        MovesCard(viewModel)
        Text(
            stringResource(R.string.chess_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (confirmResign) {
        AlertDialog(
            onDismissRequest = { confirmResign = false },
            title = { Text(stringResource(R.string.chess_resign_title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmResign = false
                    viewModel.resign()
                }) { Text(stringResource(R.string.chess_resign)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmResign = false }) { Text(stringResource(R.string.chess_cancel)) }
            }
        )
    }
}

@Composable
private fun SetupSection(vm: ChessViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
        ) {
            FilterChip(
                selected = vm.mode == ChessMode.COMPUTER,
                onClick = { vm.onModeChange(ChessMode.COMPUTER) },
                label = { Text(stringResource(R.string.chess_mode_computer)) }
            )
            FilterChip(
                selected = vm.mode == ChessMode.TWO_PLAYERS,
                onClick = { vm.onModeChange(ChessMode.TWO_PLAYERS) },
                label = { Text(stringResource(R.string.chess_mode_two)) }
            )
        }
        if (vm.vsComputer) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.chess_side), style = MaterialTheme.typography.labelLarge)
                listOf(1, -1).forEach { color ->
                    FilterChip(
                        selected = vm.side == color,
                        onClick = { vm.onSideChange(color) },
                        label = { Text(colorName(color)) }
                    )
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.chess_level), style = MaterialTheme.typography.labelLarge)
                ChessLevel.entries.forEach { level ->
                    FilterChip(
                        selected = vm.level == level,
                        onClick = { vm.onLevelChange(level) },
                        label = {
                            Text(
                                stringResource(
                                    when (level) {
                                        ChessLevel.EASY -> R.string.chess_level_easy
                                        ChessLevel.MEDIUM -> R.string.chess_level_medium
                                        ChessLevel.HARD -> R.string.chess_level_hard
                                    }
                                )
                            )
                        }
                    )
                }
            }
        }
        Button(onClick = vm::newGame) { Text(stringResource(R.string.chess_new_game)) }
    }
}

@Composable
private fun PromotionRow(onPick: (Int) -> Unit, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            Text(
                stringResource(R.string.chess_promote),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
            ) {
                Button(onClick = { onPick(Chess.Q) }) { Text(stringResource(R.string.chess_queen)) }
                Button(onClick = { onPick(Chess.R) }) { Text(stringResource(R.string.chess_rook)) }
                Button(onClick = { onPick(Chess.B) }) { Text(stringResource(R.string.chess_bishop)) }
                Button(onClick = { onPick(Chess.N) }) { Text(stringResource(R.string.chess_knight)) }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.chess_cancel)) }
            }
        }
    }
}

/** Pieces taken by [byColor], biggest first, shown on a board-coloured strip so both colours stay readable. */
@Composable
private fun CapturedRow(vm: ChessViewModel, byColor: Int) {
    val taken = vm.history
        .filter { it.captured != 0 && it.before.turn == byColor }
        .map { Math.abs(it.captured) }
        .sortedDescending()
    // The pieces taken belong to the other side
    val glyphColor = if (byColor == 1) BLACK_PIECE else WHITE_PIECE
    val text = taken.joinToString("") { GLYPHS[it] }
    val description = stringResource(R.string.chess_captured_by, colorName(byColor))
    Text(
        text.ifEmpty { " " },
        modifier = Modifier
            .fillMaxWidth()
            .background(DARK_SQUARE, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = description },
        color = glyphColor,
        fontSize = 20.sp,
        maxLines = 1
    )
}

@Composable
private fun ChessBoard(vm: ChessViewModel, description: String) {
    val measurer = rememberTextMeasurer()
    val position = vm.position
    val flipped = vm.flipped
    val last = vm.history.lastOrNull()?.move
    val selected = vm.selected
    val targets = if (selected >= 0) vm.legalMoves.filter { it.from == selected }.map { it.to }.toSet() else emptySet()
    val kingSquare = if (vm.inCheck) (0 until 64).firstOrNull { position.board[it] == position.turn * Chess.K } ?: -1 else -1
    val onSquare by rememberUpdatedState(vm::onSquare)
    val currentFlipped by rememberUpdatedState(flipped)
    val boardDescription = stringResource(R.string.chess_board_description, description)

    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(3.dp, Color(0xFF5B3A1E), RoundedCornerShape(6.dp))
            .semantics { contentDescription = boardDescription }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val cell = size.width / 8f
                    val col = (tap.x / cell).toInt().coerceIn(0, 7)
                    val row = (tap.y / cell).toInt().coerceIn(0, 7)
                    val idx = row * 8 + col
                    onSquare(if (currentFlipped) 63 - idx else idx)
                }
            }
    ) {
        val cell = size.width / 8f
        val coordStyle = { color: Color -> TextStyle(color = color, fontSize = (cell * 0.2f).toSp(), fontWeight = FontWeight.Bold) }
        for (idx in 0 until 64) {
            val sq = if (flipped) 63 - idx else idx
            val r = sq shr 3
            val f = sq and 7
            val dark = ((r + f) and 1) == 1
            val topLeft = Offset((idx and 7) * cell, (idx shr 3) * cell)
            val isLast = last != null && (sq == last.from || sq == last.to)
            val base = when {
                isLast && dark -> LAST_DARK
                isLast -> LAST_LIGHT
                dark -> DARK_SQUARE
                else -> LIGHT_SQUARE
            }
            drawRect(base, topLeft, Size(cell, cell))
            if (sq == kingSquare) {
                drawCircle(Color(0xAAE53935), cell * 0.48f, topLeft + Offset(cell / 2, cell / 2))
            }
            if (sq == selected) {
                drawRect(Color(0xFF1D4ED8), topLeft, Size(cell, cell), style = Stroke(width = cell * 0.08f))
            }
            val coordColor = if (dark) LIGHT_SQUARE else DARK_SQUARE
            if ((idx and 7) == 0) {
                drawText(measurer.measure("${8 - r}", coordStyle(coordColor)), topLeft = topLeft + Offset(cell * 0.05f, cell * 0.03f))
            }
            if ((idx shr 3) == 7) {
                val label = measurer.measure("${"abcdefgh"[f]}", coordStyle(coordColor))
                drawText(label, topLeft = topLeft + Offset(cell - label.size.width - cell * 0.05f, cell - label.size.height - cell * 0.03f))
            }
            val p = position.board[sq]
            if (p != 0) drawPiece(measurer, p, topLeft, cell)
            if (sq in targets) {
                if (p == 0) drawCircle(Color(0x80143C14), cell * 0.14f, topLeft + Offset(cell / 2, cell / 2))
                else drawCircle(Color(0x8C143C14), cell * 0.46f, topLeft + Offset(cell / 2, cell / 2), style = Stroke(width = cell * 0.08f))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPiece(
    measurer: androidx.compose.ui.text.TextMeasurer,
    piece: Int,
    topLeft: Offset,
    cell: Float
) {
    val glyph = GLYPHS[Math.abs(piece)]
    val white = piece > 0
    val size = (cell * 0.78f).toSp()
    val outline = measurer.measure(
        glyph,
        TextStyle(
            color = if (white) Color.Black else Color(0x99FFFFFF),
            fontSize = size,
            drawStyle = Stroke(width = cell * if (white) 0.06f else 0.025f)
        )
    )
    val fill = measurer.measure(glyph, TextStyle(color = if (white) WHITE_PIECE else BLACK_PIECE, fontSize = size))
    val at = topLeft + Offset((cell - fill.size.width) / 2f, (cell - fill.size.height) / 2f)
    drawText(outline, topLeft = at)
    drawText(fill, topLeft = at)
}

@Composable
private fun MovesCard(vm: ChessViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.chess_moves),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (vm.history.isEmpty()) {
                Text(
                    stringResource(R.string.chess_no_moves),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            vm.history.chunked(2).forEachIndexed { i, pair ->
                Text(
                    "${i + 1}.  ${pair[0].san}   ${pair.getOrNull(1)?.san.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
