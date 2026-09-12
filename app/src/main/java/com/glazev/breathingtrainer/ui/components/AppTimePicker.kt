package com.glazev.breathingtrainer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glazev.breathingtrainer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = DarkBlueBg,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Выберите время",
                    style = MaterialTheme.typography.labelMedium,
                    color = LightCyan,
                    modifier = Modifier.padding(bottom = 20.dp).align(Alignment.Start)
                )

                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = TranslucentWhite,
                        clockDialSelectedContentColor = White,
                        clockDialUnselectedContentColor = White.copy(alpha = 0.8f),
                        selectorColor = MainTeal,
                        containerColor = DarkBlueBg,
                        periodSelectorBorderColor = MainTeal,
                        periodSelectorSelectedContainerColor = MainTeal,
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = White,
                        periodSelectorUnselectedContentColor = White,
                        timeSelectorSelectedContainerColor = MainTeal.copy(alpha = 0.3f),
                        timeSelectorUnselectedContainerColor = TranslucentWhite,
                        timeSelectorSelectedContentColor = LightCyan,
                        timeSelectorUnselectedContentColor = White
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ОТМЕНА", color = White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                        Text("OK", color = LightCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
