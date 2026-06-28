package com.wirewaypro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "WIREWAY [PRO]" wordmark — the brand lockup from the web app's tokens.css,
 * rebuilt in Compose. Tracked caps + an accent "PRO" chip.
 */
@Composable
fun WirewayWordmark(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = "WIREWAY",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.4.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "PRO",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .padding(start = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(5.dp),
                )
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}
