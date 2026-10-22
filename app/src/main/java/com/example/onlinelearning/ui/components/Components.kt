package com.example.onlinelearning.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.onlinelearning.data.AvatarCodec
import com.example.onlinelearning.data.model.Course
import com.example.onlinelearning.ui.theme.Orange
import com.example.onlinelearning.ui.theme.PrimaryLight

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().height(50.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth().height(50.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (visible) "Скрыть пароль" else "Показать пароль",
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Course cover. Absolute http(s) URLs are loaded from the network; relative paths such as
 * "covers/visual-design.webp" come from app/src/main/assets, so covers work offline.
 */
@Composable
fun CourseImage(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = if (url.startsWith("http://") || url.startsWith("https://")) url else "file:///android_asset/$url",
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PrimaryLight),
    )
}

/** Row card used in course lists: thumbnail, title, author, price and duration. */
@Composable
fun CourseRow(course: Course, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CourseImage(course.imageUrl, Modifier.size(72.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(course.title, style = MaterialTheme.typography.titleMedium)
                Text(course.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (course.price == 0.0) "Free" else course.priceLabel,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${course.durationMinutes / 60} hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = Orange,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Orange.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

/** Illustrated empty / error state like "No Products" and "No Network" in the design. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    /** Optional picture from assets/illustrations, shown instead of the icon. */
    illustration: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (illustration != null) {
                AssetImage(illustration, Modifier.size(width = 200.dp, height = 144.dp), ContentScale.Fit)
            } else {
                Box(
                    Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(60.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (action != null) {
                Spacer(Modifier.height(24.dp))
                action()
            }
        }
    }
}

/** Picture bundled in app/src/main/assets, e.g. "illustrations/meetup.webp". */
@Composable
fun AssetImage(path: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    AsyncImage(
        model = "file:///android_asset/$path",
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
    )
}

/** "Learned today" card from the Home and My courses screens. */
@Composable
fun LearnedTodayCard(
    minutes: Int,
    goalMinutes: Int,
    modifier: Modifier = Modifier,
    onMyCourses: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Learned today",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                if (onMyCourses != null) {
                    Text(
                        "My courses",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onMyCourses).padding(4.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${minutes}min", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(" / ${goalMinutes}min", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            val progress = (minutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFC9A7), Orange))),
                )
            }
        }
    }
}

/** Round digit boxes for SMS codes and payment PINs; [masked] shows dots instead of digits. */
@Composable
fun CodeBoxes(value: String, length: Int, modifier: Modifier = Modifier, masked: Boolean = false) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(length) { i ->
            val ch = value.getOrNull(i)
            val active = i == value.length
            Box(
                Modifier
                    .size(width = 44.dp, height = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        if (active) 2.dp else 1.dp,
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (ch != null) {
                    if (masked) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface))
                    } else {
                        Text(ch.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

/** On-screen number pad used by phone sign-in and the payment password, like in the design. */
@Composable
fun NumericKeypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf("123", "456", "789", " 0<")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                when (key) {
                                    ' ' -> Modifier
                                    '<' -> Modifier.clickable(onClick = onBackspace)
                                    else -> Modifier.clickable { onDigit(key) }
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (key) {
                            ' ' -> Unit
                            '<' -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Стереть")
                            else -> Text(key.toString(), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Round profile photo; falls back to the first letter of the name when no photo is set. */
@Composable
fun UserAvatar(avatar: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(avatar) { if (avatar.isBlank()) null else AvatarCodec.decode(avatar)?.asImageBitmap() }
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = "Фото профиля", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                name.take(1).uppercase(),
                style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
