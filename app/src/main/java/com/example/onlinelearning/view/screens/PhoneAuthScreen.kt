package com.example.onlinelearning.view.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.onlinelearning.view.components.AssetImage
import com.example.onlinelearning.view.components.CodeBoxes
import com.example.onlinelearning.view.components.NumericKeypad
import com.example.onlinelearning.view.components.PrimaryButton
import com.example.onlinelearning.view.theme.Primary
import com.example.onlinelearning.viewmodel.PhoneAuthViewModel
import com.example.onlinelearning.viewmodel.PhoneStep

fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Phone sign-in from the design: "Continue with Phone" → "Verify Phone" → "Success". */
@Composable
fun PhoneAuthScreen(viewModel: PhoneAuthViewModel, onDone: () -> Unit, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()
    val close = {
        viewModel.reset()
        onClose()
    }
    BackHandler { if (!viewModel.back()) close() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (state.step == PhoneStep.CODE) {
            Box(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp)) {
                IconButton(onClick = { viewModel.back() }, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Filled.Close, "Назад")
                }
                Text("Verify Phone", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
            }
            Text(
                "Code is sent to ${state.phone}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp),
            )
            CodeBoxes(state.code, PhoneAuthViewModel.CODE_LENGTH, Modifier.align(Alignment.CenterHorizontally))
            PrimaryButton(
                "Verify and Create Account",
                { viewModel.verify() },
                Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp),
                loading = state.loading,
            )
            TextButton(
                onClick = { activity?.let { viewModel.sendCode(it, resend = true) } },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Resend code") }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .statusBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().height(56.dp)) {
                    IconButton(onClick = close, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Filled.Close, "Закрыть")
                    }
                    Text("Continue with Phone", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
                }
                AssetImage("illustrations/phone_verify.webp", Modifier.size(width = 200.dp, height = 150.dp))
            }
            Text(
                "Enter Your Phone Number",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
            )
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(52.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (state.phoneDigits.isEmpty()) {
                        Text("+7 900 000 00 00", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(state.phone, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { activity?.let { viewModel.sendCode(it) } },
                    enabled = !state.loading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Continue")
                    }
                }
            }
        }
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        NumericKeypad(
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }

    if (state.step == PhoneStep.DONE) {
        SuccessDialog(
            title = "Success",
            text = "Congratulations, you have completed your registration!",
            button = "Done",
            onClick = {
                viewModel.reset()
                onDone()
            },
        )
    }
}

/** White card with a blue check, used after registration. */
@Composable
fun SuccessDialog(title: String, text: String, button: String, onClick: () -> Unit) {
    Dialog(onDismissRequest = onClick) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = Primary, modifier = Modifier.size(64.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                PrimaryButton(button, onClick)
            }
        }
    }
}
