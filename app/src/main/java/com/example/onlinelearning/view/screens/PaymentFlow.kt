package com.example.onlinelearning.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.onlinelearning.model.entity.Course
import com.example.onlinelearning.view.components.CodeBoxes
import com.example.onlinelearning.view.components.NumericKeypad
import com.example.onlinelearning.view.components.PrimaryButton
import com.example.onlinelearning.view.theme.Orange
import com.example.onlinelearning.view.theme.Primary
import com.example.onlinelearning.viewmodel.CourseDetailViewModel.Companion.PIN_LENGTH
import com.example.onlinelearning.viewmodel.PayStep
import com.example.onlinelearning.viewmodel.PaymentState

/**
 * Demo checkout from the design: "Payment Method" (card) → "Payment Password" (6-digit PIN) →
 * "Successful purchase!". The steps and the PIN live in [CourseDetailViewModel]; this View only draws [state].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFlow(
    course: Course,
    state: PaymentState,
    onPayNow: () -> Unit,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit,
    onStartLearning: () -> Unit,
) {
    val step = state.step
    val pin = state.pin
    val processing = state.processing

    if (step == PayStep.SUCCESS) {
        PurchaseSuccess(onStartLearning)
        return
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (step) {
                PayStep.METHOD -> {
                    Text("Payment Method", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    DemoCard(course)
                    Spacer(Modifier.height(12.dp))
                    Text("My card", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) { i ->
                            Box(
                                Modifier
                                    .size(width = if (i == 0) 16.dp else 6.dp, height = 6.dp)
                                    .clip(CircleShape)
                                    .background(if (i == 0) Primary else MaterialTheme.colorScheme.outline),
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(course.title, modifier = Modifier.weight(1f))
                        Text(course.priceLabel, fontWeight = FontWeight.Bold, color = Primary)
                    }
                    Text(
                        "Демо-оплата: деньги не списываются, покупка сохраняется в Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton("Pay Now", onPayNow)
                }
                PayStep.PASSWORD -> {
                    Text("Payment Password", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Please enter the payment password",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Box(contentAlignment = Alignment.Center) {
                        CodeBoxes(pin, PIN_LENGTH, masked = true)
                        if (processing) CircularProgressIndicator(Modifier.size(28.dp))
                    }
                    Text(
                        "Демо: подойдут любые 6 цифр",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    NumericKeypad(onDigit = onDigit, onBackspace = onBackspace)
                }
                PayStep.SUCCESS -> Unit
            }
        }
    }
}

@Composable
private fun DemoCard(course: Course) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Primary, Color(0xFF8E7BFF), Color(0xFFF5A3E0)))),
    ) {
        Box(Modifier.size(180.dp).offset(x = (-40).dp, y = (-60).dp).clip(CircleShape).background(Color(0x5543E0FF)))
        Box(Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-30).dp).clip(CircleShape).background(Color(0x66FF9AE6)))
        Text("Demo card", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(20.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text("••••  ••••  ••••  4829", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("Total ${course.priceLabel}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PurchaseSuccess(onStartLearning: () -> Unit) {
    Dialog(onDismissRequest = onStartLearning, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                    // Confetti dots around the check mark, as in the design.
                    listOf(
                        Triple(-60, -50, Orange), Triple(58, -58, Primary), Triple(-70, 30, Color(0xFFF5A3E0)),
                        Triple(66, 40, Orange), Triple(0, -76, Color(0xFFFFD166)), Triple(-20, 70, Primary),
                    ).forEach { (x, y, c) ->
                        Box(Modifier.offset(x = x.dp, y = y.dp).size(8.dp).clip(CircleShape).background(c))
                    }
                    Box(Modifier.size(84.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Successful purchase!", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Start learning", onStartLearning, Modifier.width(260.dp))
            }
        }
    }
}
