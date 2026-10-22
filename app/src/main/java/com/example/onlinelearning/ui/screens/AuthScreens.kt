package com.example.onlinelearning.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.onlinelearning.ui.AuthViewModel
import com.example.onlinelearning.ui.components.LabeledField
import com.example.onlinelearning.ui.components.PrimaryButton
import com.example.onlinelearning.ui.components.SecondaryButton
import com.example.onlinelearning.ui.theme.Primary
import kotlinx.coroutines.launch

private data class OnboardingPage(val icon: ImageVector, val title: String, val text: String)

private val onboardingPages = listOf(
    OnboardingPage(Icons.AutoMirrored.Filled.MenuBook, "Numerous free\ntrial courses", "Free courses for you to find your way to learning"),
    OnboardingPage(Icons.Filled.School, "Quick and easy\nlearning", "Easy and fast learning at any time to help you improve various skills"),
    OnboardingPage(Icons.AutoMirrored.Filled.EventNote, "Create your own\nstudy plan", "Study according to the study plan, make study more motivated"),
)

@Composable
fun OnboardingScreen(onSignUp: () -> Unit, onLogIn: () -> Unit, onSkip: () -> Unit) {
    val pager = rememberPagerState { onboardingPages.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == onboardingPages.lastIndex

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
            if (!isLast) TextButton(onClick = onSkip) { Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = onboardingPages[index]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(220.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(page.icon, null, tint = Primary, modifier = Modifier.size(110.dp))
                }
                Spacer(Modifier.height(40.dp))
                Text(page.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    page.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center) {
            repeat(onboardingPages.size) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(5.dp)
                        .width(if (i == pager.currentPage) 24.dp else 8.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i == pager.currentPage) Primary else MaterialTheme.colorScheme.outline),
                )
            }
        }
        if (isLast) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryButton("Sign up", onSignUp, Modifier.weight(1f))
                SecondaryButton("Log in", onLogIn, Modifier.weight(1f))
            }
        } else {
            PrimaryButton("Next", { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } })
        }
    }
}

@Composable
private fun AuthScaffold(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { content() }
        }
    }
}

@Composable
private fun FirebaseWarning() {
    Text(
        "Firebase не настроен: добавьте app/google-services.json (см. README).",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoggedIn: () -> Unit, onSignUp: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.clearMessages() }

    AuthScaffold("Log In", "") {
        if (!viewModel.firebaseConfigured) FirebaseWarning()
        LabeledField("Your Email", email, { email = it }, keyboardType = KeyboardType.Email)
        LabeledField("Password", password, { password = it }, isPassword = true)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { viewModel.resetPassword(email) }) {
                Text("Forget password?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        PrimaryButton("Log In", { viewModel.signIn(email, password, onLoggedIn) }, loading = state.loading)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onSignUp) { Text("Sign up") }
        }
    }
}

@Composable
fun SignUpScreen(viewModel: AuthViewModel, onSignedUp: () -> Unit, onLogIn: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.clearMessages() }

    AuthScaffold("Sign Up", "Enter your details below & free sign up") {
        if (!viewModel.firebaseConfigured) FirebaseWarning()
        LabeledField("Your Name", name, { name = it })
        LabeledField("Your Email", email, { email = it }, keyboardType = KeyboardType.Email)
        LabeledField("Password", password, { password = it }, isPassword = true)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton(
            "Create account",
            { viewModel.signUp(name, email, password, onSignedUp) },
            enabled = agreed,
            loading = state.loading,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = agreed, onCheckedChange = { agreed = it })
            Text(
                "By creating an account you have to agree with our terms & conditions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onLogIn) { Text("Log in") }
        }
    }
}
