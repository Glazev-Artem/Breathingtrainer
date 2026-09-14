package com.glazev.breathingtrainer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private data class PendingReminder(val hour: Int, val minute: Int, val date: String?)

@Composable
internal fun rememberReminderSetter(
    viewModel: BreathingViewModel
): (hour: Int, minute: Int, date: String?) -> Unit {
    val context = LocalContext.current
    var pendingReminder by remember { mutableStateOf<PendingReminder?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingReminder
        pendingReminder = null
        if (granted && pending != null) {
            viewModel.setReminder(pending.hour, pending.minute, pending.date)
        } else if (!granted) {
            Toast.makeText(
                context,
                "Разрешите уведомления, чтобы получать напоминания",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    return { hour, minute, date ->
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            viewModel.setReminder(hour, minute, date)
        } else {
            pendingReminder = PendingReminder(hour, minute, date)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
