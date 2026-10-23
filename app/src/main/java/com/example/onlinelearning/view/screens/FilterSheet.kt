package com.example.onlinelearning.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.onlinelearning.view.components.PrimaryButton
import com.example.onlinelearning.view.components.SecondaryButton
import com.example.onlinelearning.view.theme.Primary
import com.example.onlinelearning.viewmodel.CourseFilter
import com.example.onlinelearning.viewmodel.DurationRange
import kotlin.math.roundToInt

/** "Search Filter" bottom sheet from the design: categories, price range and duration. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    categories: List<String>,
    maxPrice: Float,
    current: CourseFilter,
    onApply: (CourseFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val top = maxPrice.coerceAtLeast(1f)
    var selectedCategories by remember { mutableStateOf(current.categories) }
    var range by remember { mutableStateOf(current.minPrice.coerceIn(0f, top)..(current.maxPrice ?: top).coerceIn(0f, top)) }
    var durations by remember { mutableStateOf(current.durations) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Box(Modifier.fillMaxWidth()) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Filled.Close, "Закрыть")
                }
                Text("Search Filter", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
            }

            SheetLabel("Categories")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { c ->
                    ToggleChip(c, c in selectedCategories) {
                        selectedCategories = if (c in selectedCategories) selectedCategories - c else selectedCategories + c
                    }
                }
            }

            SheetLabel("Price")
            RangeSlider(
                value = range,
                onValueChange = { range = it },
                valueRange = 0f..top,
                colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary),
            )
            Row(Modifier.fillMaxWidth()) {
                Text("$${range.start.roundToInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text("$${range.endInclusive.roundToInt()}", style = MaterialTheme.typography.bodySmall)
            }

            SheetLabel("Duration")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DurationRange.entries.forEach { d ->
                    ToggleChip(d.label, d in durations) {
                        durations = if (d in durations) durations - d else durations + d
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Clear",
                    {
                        selectedCategories = emptySet()
                        range = 0f..top
                        durations = emptySet()
                    },
                    Modifier.weight(1f),
                )
                PrimaryButton(
                    "Apply Filter",
                    {
                        onApply(
                            CourseFilter(
                                categories = selectedCategories,
                                minPrice = range.start,
                                maxPrice = range.endInclusive.takeIf { it < top },
                                durations = durations,
                            )
                        )
                    },
                    Modifier.weight(2f),
                )
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
}

@Composable
private fun ToggleChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
