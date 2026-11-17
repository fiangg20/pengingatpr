package com.bases.pengingatpr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
private fun Tile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    shape: Shape,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.height(172.dp).clip(shape).background(container).clickable(onClick = onClick).padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(52.dp).clip(cookieShape(8, 0.1f)).background(content.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = content) }
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, color = content)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = content)
        }
    }
}

@Composable
fun MoreScreen(nav: Nav) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Lainnya", style = MaterialTheme.typography.headlineLarge) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(
                    "Mode fokus", "Timer belajar dan istirahat", Icons.Rounded.Timer,
                    cs.primaryContainer, cs.onPrimaryContainer,
                    RoundedCornerShape(topStart = 48.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                    Modifier.weight(1f),
                ) { nav.push(Route.Focus(null)) }
                Tile(
                    "Statistik", "Perkembangan dan kebiasaanmu", Icons.Rounded.BarChart,
                    cs.tertiaryContainer, cs.onTertiaryContainer,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 48.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                    Modifier.weight(1f),
                ) { nav.push(Route.Stats) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(
                    "Impor jadwal", "Dari PDF aSc Timetables", Icons.Rounded.PictureAsPdf,
                    cs.secondaryContainer, cs.onSecondaryContainer,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 48.dp),
                    Modifier.weight(1f),
                ) { nav.push(Route.Import) }
                Tile(
                    "Pengaturan", "Tema, pengingat, cadangan", Icons.Rounded.Settings,
                    cs.surfaceContainerHigh, cs.onSurface,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 48.dp, bottomStart = 16.dp),
                    Modifier.weight(1f),
                ) { nav.push(Route.Settings) }
            }
        }
    }
}
