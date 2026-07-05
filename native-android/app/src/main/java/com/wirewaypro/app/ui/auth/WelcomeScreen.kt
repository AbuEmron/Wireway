package com.wirewaypro.app.ui.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wirewaypro.app.ui.components.WirewayLogomark
import com.wirewaypro.app.ui.components.pressScale
import com.wirewaypro.app.ui.components.rememberWirewayHaptics
import com.wirewaypro.app.ui.components.riseIn
import com.wirewaypro.app.ui.theme.BrandGradients
import com.wirewaypro.app.ui.theme.GradientBlue

private val WirewayCyan = Color(0xFF22D3FF)

/**
 * The signed-out entry point: the Wireway mark, wordmark and tagline on the brand
 * blue→purple gradient, with two clear actions — "Get started" (→ sign up) and
 * "I already have an account" (→ sign in). This is the auth graph's home; Login
 * and Sign Up branch off from here.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
) {
    // The dashboard hero's slow "live current" sweep, full-bleed.
    val transition = rememberInfiniteTransition(label = "welcome-gradient")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "welcome-sweep",
    )
    val haptics = rememberWirewayHaptics()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = BrandGradients.animated(sweep)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // Transparent W-cable mark straight on the gradient hero — the screen
            // draws its own WIREWAY PRO wordmark below, so the tile would double it.
            Box(Modifier.riseIn(0)) {
                WirewayLogomark(size = 104.dp)
            }
            Spacer(Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.riseIn(1)) {
                Text(
                    text = "WIREWAY",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 34.sp,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "PRO",
                    fontWeight = FontWeight.Bold,
                    color = WirewayCyan,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Electrical estimating · Powered by Precision",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.riseIn(2),
            )

            Spacer(Modifier.weight(1.15f))

            // Primary — solid white pill so it pops on the gradient.
            Button(
                onClick = { haptics.confirm(); onGetStarted() },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = GradientBlue,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .riseIn(3)
                    .pressScale(pressedScale = 0.97f),
            ) {
                Text(
                    text = "Get started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Secondary — ghost pill outlined in white.
            OutlinedButton(
                onClick = { haptics.tap(); onSignIn() },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .riseIn(4)
                    .pressScale(pressedScale = 0.97f),
            ) {
                Text(
                    text = "I already have an account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
