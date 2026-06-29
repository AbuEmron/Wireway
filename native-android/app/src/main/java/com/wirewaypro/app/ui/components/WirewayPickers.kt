package com.wirewaypro.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * A read-only field that looks like a text input but opens a picker on tap.
 * Used for dates/times so the stored string format is always valid.
 */
@Composable
fun PickerField(
    label: String,
    value: String,
    placeholder: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(icon, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent overlay captures taps (a disabled field ignores clicks).
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

/** Date field bound to a "yyyy-MM-dd" string. */
@Composable
fun DateField(label: String, value: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    PickerField(
        label = label,
        value = if (value.isBlank()) "" else value,
        placeholder = "YYYY-MM-DD",
        icon = Icons.Filled.CalendarMonth,
        onClick = { open = true },
        modifier = modifier,
    )
    if (open) {
        WirewayDatePickerDialog(
            initial = value,
            onConfirm = { open = false; onPick(it) },
            onDismiss = { open = false },
        )
    }
}

/** Time field bound to an "HH:mm" string. */
@Composable
fun TimeField(label: String, value: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    PickerField(
        label = label,
        value = value,
        placeholder = "HH:MM",
        icon = Icons.Filled.Schedule,
        onClick = { open = true },
        modifier = modifier,
    )
    if (open) {
        WirewayTimePickerDialog(
            initial = value,
            onConfirm = { open = false; onPick(it) },
            onDismiss = { open = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirewayDatePickerDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val initialMillis = runCatching {
        LocalDate.parse(initial.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }.getOrNull()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    onConfirm(date.toString())
                } else {
                    onDismiss()
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirewayTimePickerDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val parsed = runCatching { LocalTime.parse(initial.take(5)) }.getOrNull()
    val state = rememberTimePickerState(
        initialHour = parsed?.hour ?: 9,
        initialMinute = parsed?.minute ?: 0,
        is24Hour = false,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a time") },
        text = {
            Column(Modifier.padding(top = 8.dp)) { TimePicker(state = state) }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm("%02d:%02d".format(state.hour, state.minute))
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
