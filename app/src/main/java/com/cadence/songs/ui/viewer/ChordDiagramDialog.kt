package com.cadence.songs.ui.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadence.songs.music.ChordShapes
import com.cadence.songs.music.Shape

/** Fenêtre « diagramme d'accord » : guitare ou ukulélé, affichée quand on touche un accord. */
@Composable
fun ChordDiagramDialog(
    chord: String,
    ukulele: Boolean,
    onInstrument: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val shape = ChordShapes.shapeFor(chord, ukulele)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(chord) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !ukulele,
                        onClick = { onInstrument(false) },
                        label = { Text("Guitare") }
                    )
                    FilterChip(
                        selected = ukulele,
                        onClick = { onInstrument(true) },
                        label = { Text("Ukulélé") }
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (shape != null) {
                    ChordDiagram(
                        shape = shape,
                        strings = if (ukulele) ChordShapes.ukuleleStrings.size else ChordShapes.guitarStrings.size,
                        modifier = Modifier.size(width = 190.dp, height = 220.dp)
                    )
                    if (shape.approx) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Forme approchée (sans l'extension de l'accord).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        "Pas de diagramme pour cet accord.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@Composable
private fun ChordDiagram(shape: Shape, strings: Int, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onSurface
    val dot = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val played = shape.frets.filter { it > 0 }
    val minFret = played.minOrNull() ?: 1
    val maxFret = played.maxOrNull() ?: 1
    val rows = 5
    // Case de départ : 1 si tout tient dans les 5 premières cases, sinon on « monte » le manche.
    val startFret = if (maxFret <= rows) 1 else minFret

    Canvas(modifier) {
        val left = 26.dp.toPx()
        val right = size.width - 26.dp.toPx()
        val top = 40.dp.toPx()
        val bottom = size.height - 12.dp.toPx()
        val gapX = (right - left) / (strings - 1)
        val gapY = (bottom - top) / rows
        val stringWidth = 1.5.dp.toPx()

        // Sillet épais si on est en première position
        if (startFret == 1) {
            drawLine(ink, Offset(left, top), Offset(right, top), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        } else {
            drawLine(ink.copy(alpha = 0.5f), Offset(left, top), Offset(right, top), strokeWidth = stringWidth)
            val label = measurer.measure(startFret.toString() + "fr", TextStyle(fontSize = 13.sp, color = muted))
            drawText(label, topLeft = Offset(0f, top + gapY / 2 - label.size.height / 2f))
        }
        for (r in 1..rows) {
            val y = top + gapY * r
            drawLine(ink.copy(alpha = 0.35f), Offset(left, y), Offset(right, y), strokeWidth = stringWidth)
        }
        for (s in 0 until strings) {
            val x = left + gapX * s
            drawLine(ink.copy(alpha = 0.55f), Offset(x, top), Offset(x, bottom), strokeWidth = stringWidth)
        }

        // Barré : même case sur plusieurs cordes, depuis la plus grave
        val barreFret = shape.frets.filter { it > 0 }.minOrNull()
        if (barreFret != null && barreFret >= startFret) {
            val idx = shape.frets.withIndex().filter { it.value == barreFret }.map { it.index }
            val openInside = idx.isNotEmpty() && (idx.first()..idx.last()).any { shape.frets[it] == 0 }
            if (idx.size >= 2 && idx.last() - idx.first() >= 3 && !openInside) {
                val y = top + gapY * (barreFret - startFret + 0.5f)
                drawLine(
                    dot,
                    Offset(left + gapX * idx.first(), y),
                    Offset(left + gapX * idx.last(), y),
                    strokeWidth = gapY * 0.62f,
                    cap = StrokeCap.Round
                )
            }
        }

        shape.frets.forEachIndexed { s, f ->
            val x = left + gapX * s
            when {
                f < 0 -> {
                    val t = measurer.measure("×", TextStyle(fontSize = 18.sp, color = muted))
                    drawText(t, topLeft = Offset(x - t.size.width / 2f, top - 8.dp.toPx() - t.size.height))
                }
                f == 0 -> drawCircle(
                    muted, radius = 6.dp.toPx(), center = Offset(x, top - 16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                else -> {
                    val y = top + gapY * (f - startFret + 0.5f)
                    drawCircle(dot, radius = gapY * 0.34f, center = Offset(x, y))
                }
            }
        }
    }
}
