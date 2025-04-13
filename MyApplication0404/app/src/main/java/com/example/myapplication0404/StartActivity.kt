package com.example.myapplication0404

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class StartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StartScreen(
                    onStartClick = {
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                    onExitClick = {
                        finish()
                    },
                    onGoogleSignInClick = {
                        // TODO: Реализовать вход через Google
                    }
                )
            }
        }
    }
}

@Composable
fun StartScreen(
    onStartClick: () -> Unit,
    onExitClick: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    var showLogo by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }

    // Плавная бесконечная анимация "дыхания"
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val logoPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Анимация запуска
    LaunchedEffect(Unit) {
        delay(300)
        showLogo = true
        delay(700)
        showText = true
        delay(700)
        showButtons = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Фоновое изображение
        Image(
            painter = painterResource(id = R.drawable.splash_image),
            contentDescription = "Splash Screen",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.3f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ЛОГОТИП + ТЕКСТ
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(visible = showLogo) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .scale(logoPulseScale) // пульсация логотипа
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = showText,
                    enter = fadeIn(tween(1000)) + slideInVertically(tween(1000)) { it / 2 },
                    exit = fadeOut()
                ) {
                    Text(
                        text = "GLANCE",
                        color = Color.DarkGray,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // КНОПКИ
            AnimatedVisibility(
                visible = showButtons,
                enter = fadeIn(tween(1000)) + slideInVertically(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StyledButton(text = "Старт", color = Color(0xFF00C853), onClick = onStartClick)
                    Spacer(modifier = Modifier.height(12.dp))

                    StyledButton(
                        text = "Вход в аккаунт Google",
                        color = Color.White,
                        textColor = Color.Black,
                        icon = R.drawable.ic_google,
                        onClick = onGoogleSignInClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    StyledButton(text = "Выйти", color = Color(0xFFD50000), onClick = onExitClick)
                }
            }
        }
    }
}

@Composable
fun StyledButton(
    text: String,
    color: Color,
    textColor: Color = Color.White,
    icon: Int? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        if (icon != null) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, color = textColor, fontSize = 18.sp)
    }
}
