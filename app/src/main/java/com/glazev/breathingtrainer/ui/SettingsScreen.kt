package com.glazev.breathingtrainer.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glazev.breathingtrainer.R
import com.glazev.breathingtrainer.model.DefaultTechniques
import com.glazev.breathingtrainer.ui.components.HistoryDialog
import com.glazev.breathingtrainer.ui.components.InfoDialog
import com.glazev.breathingtrainer.ui.components.SettingsDialog
import com.glazev.breathingtrainer.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun SettingsScreen(
    viewModel: BreathingViewModel,
    onStartClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    var showPresets by remember { mutableStateOf(false) }
    var showMusicPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                account.idToken?.let { viewModel.signInWithGoogle(it) }
            } catch (e: Exception) {}
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.addCustomMusic(it) }
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.bg_main_gradient), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
            Image(painter = painterResource(id = R.drawable.ic_circle_bg), contentDescription = null, modifier = Modifier.offset(y = 40.dp).size(280.dp).scale(pulseScale), alpha = 0.4f)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { showInfoDialog = true }, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                    Image(painter = painterResource(id = R.drawable.ic_info), modifier = Modifier.size(32.dp), contentDescription = null)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 48.dp)) {
                    Image(painter = painterResource(id = R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Тренажер дыхания", color = White, fontSize = 22.sp, fontFamily = BroadleafFontFamily, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.align(Alignment.CenterEnd).size(40.dp)) {
                    Image(painter = painterResource(id = R.drawable.icon_setings), modifier = Modifier.size(32.dp), contentDescription = null)
                }
            }
            
            HorizontalDivider(color = White.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Box {
                    val name = if (uiState.isWimHofMode) "Вим Хоф" else (uiState.activeTechnique?.name ?: "Пользовательский")
                    Button(onClick = { showPresets = true }, colors = ButtonDefaults.buttonColors(containerColor = MainTeal.copy(alpha = 0.6f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(painter = painterResource(id = R.drawable.ic_arrow_down), contentDescription = null, tint = White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = name, color = White, fontSize = 20.sp, fontFamily = BroadleafFontFamily, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = showPresets, onDismissRequest = { showPresets = false }, modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 450.dp).background(DarkBlueBg)) {
                        DropdownMenuItem(
                            text = { Text("Вим Хоф", color = LightCyan, fontFamily = BroadleafFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                            onClick = { viewModel.setWimHofMode(true); showPresets = false }
                        )
                        HorizontalDivider(color = White.copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                        DropdownMenuItem(text = { Text("Ручная настройка", color = White, fontFamily = BroadleafFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) }, onClick = { viewModel.setWimHofMode(false); showPresets = false })
                        DefaultTechniques.list.forEach { technique ->
                            DropdownMenuItem(text = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(technique.name, color = White, modifier = Modifier.weight(1f), fontFamily = BroadleafFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp); val formattedDesc = technique.description.replace("-", " - "); Text(formattedDesc, color = LightCyan.copy(alpha = 0.7f), fontSize = 12.sp) } }, onClick = { viewModel.selectPreset(technique); showPresets = false })
                        }
                        if (uiState.userPresets.isNotEmpty()) {
                            HorizontalDivider(color = White.copy(alpha = 0.1f))
                            uiState.userPresets.forEach { technique ->
                                DropdownMenuItem(text = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(technique.name, color = LightCyan, modifier = Modifier.weight(1f), fontFamily = BroadleafFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp); IconButton(onClick = { viewModel.deleteUserPreset(technique) }, modifier = Modifier.size(24.dp)) { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) } } }, onClick = { viewModel.selectPreset(technique); showPresets = false })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                if (!uiState.isWimHofMode) {
                    BreathingSlider("Вдох / продолжительность", uiState.inhaleTime, { viewModel.updateInhale(it) }, 0f..18f)
                    BreathingSlider("Вдох (задержка) / продолжительность", uiState.inhaleHoldTime, { viewModel.updateInhaleHold(it) }, 0f..18f)
                    BreathingSlider("Выдох / продолжительность", uiState.exhaleTime, { viewModel.updateExhale(it) }, 0f..18f)
                    BreathingSlider("Выдох (задержка) / продолжительность", uiState.exhaleHoldTime, { viewModel.updateExhaleHold(it) }, 0f..18f)
                    Button(onClick = { showSaveDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = TranslucentWhite), shape = RoundedCornerShape(8.dp)) { Text("Сохранить как пресет", color = White, fontSize = 12.sp) }
                } else {
                    Text("Режим Вима Хофа", color = LightCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val retentionValues = remember { val list = mutableListOf<Int>(); for (i in 30..300 step 10) list.add(i); for (i in 360..1200 step 60) list.add(i); list }
                    val currentIndex = retentionValues.indexOf(uiState.wimHofRetentionTime.toInt()).coerceAtLeast(0)
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("Задержка после выдоха", color = White, fontSize = 12.sp)
                        Slider(value = currentIndex.toFloat(), onValueChange = { index -> viewModel.updateWimHofRetention(retentionValues[index.toInt()].toFloat()) }, valueRange = 0f..(retentionValues.size - 1).toFloat(), colors = SliderDefaults.colors(thumbColor = LightCyan, activeTrackColor = LightCyan, inactiveTrackColor = GraySlider))
                        val totalSec = uiState.wimHofRetentionTime.toInt(); val displayTime = if (totalSec < 60) "$totalSec сек" else "${totalSec / 60} мин ${totalSec % 60} сек"
                        Text(displayTime, color = LightCyan, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    BreathingSlider("Количество циклов", uiState.cycles.toFloat(), { viewModel.updateCycles(it.toInt()) }, 1f..10f, isInt = true)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Количество повторений", color = White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    TextField(value = uiState.repetitions.toString(), onValueChange = { viewModel.updateRepetitions(it.toIntOrNull() ?: 0) }, modifier = Modifier.width(80.dp), colors = TextFieldDefaults.colors(focusedContainerColor = TranslucentWhite, unfocusedContainerColor = TranslucentWhite, focusedTextColor = White, unfocusedTextColor = White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(8.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Фоновая музыка", color = White, fontSize = 12.sp)
                        Box {
                            Button(onClick = { showMusicPicker = true }, colors = ButtonDefaults.buttonColors(containerColor = LightCyan), shape = RoundedCornerShape(16.dp)) { Text(uiState.selectedMusic?.name ?: "Выбрать", fontSize = 12.sp, color = DarkBlueBg); Spacer(modifier = Modifier.width(4.dp)); Icon(painter = painterResource(id = R.drawable.ic_arrow_down), contentDescription = null, modifier = Modifier.size(16.dp), tint = DarkBlueBg) }
                            DropdownMenu(expanded = showMusicPicker, onDismissRequest = { showMusicPicker = false }, modifier = Modifier.background(DarkBlueBg).heightIn(max = 400.dp)) {
                                viewModel.defaultMusic.forEach { music -> DropdownMenuItem(text = { Text(music.name, color = White) }, onClick = { viewModel.selectMusic(music); showMusicPicker = false }) }
                                if (uiState.customMusicList.isNotEmpty()) {
                                    HorizontalDivider(color = White.copy(alpha = 0.2f))
                                    uiState.customMusicList.forEach { music -> DropdownMenuItem(text = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(music.name, color = LightCyan, modifier = Modifier.weight(1f)); IconButton(onClick = { viewModel.deleteCustomMusic(music) }, modifier = Modifier.size(24.dp)) { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) } } }, onClick = { viewModel.selectMusic(music); showMusicPicker = false }) }
                                }
                                HorizontalDivider(color = White.copy(alpha = 0.2f))
                                DropdownMenuItem(text = { Text("+ Добавить свою", color = LightCyan, fontWeight = FontWeight.Bold) }, onClick = { launcher.launch(arrayOf("audio/*")); showMusicPicker = false } )
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Голос", color = White, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = uiState.selectedVoice == "Male", onCheckedChange = { viewModel.updateVoice("Male") }, colors = CheckboxDefaults.colors(checkedColor = LightCyan, uncheckedColor = White))
                            Text("муж", color = White, fontSize = 12.sp)
                            Checkbox(checked = uiState.selectedVoice == "Female", onCheckedChange = { viewModel.updateVoice("Female") }, colors = CheckboxDefaults.colors(checkedColor = LightCyan, uncheckedColor = White))
                            Text("жен", color = White, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Огонек и стрик
                Row(
                    modifier = Modifier
                        .background(MainTeal.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .clickable { showHistoryDialog = true }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔥 Серия: ${uiState.streakCount} ${getDaysWord(uiState.streakCount)}", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = BroadleafFontFamily)
                }
                Spacer(modifier = Modifier.height(130.dp))
            }
            
            // --- НИЖНЯЯ ПАНЕЛЬ (ФИКСИРОВАННАЯ) ---
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Волна теперь имеет строго фиксированную высоту 90dp
                Image(
                    painter = painterResource(id = R.drawable.bg_bottom_wave), 
                    contentDescription = null, 
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
                
                // Кнопки по углам позиционируются относительно низа волны
                Row(
                    modifier = Modifier.fillMaxWidth().height(90.dp).padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(onClick = { (context as Activity).finish() }, modifier = Modifier.size(44.dp)) {
                        Image(painter = painterResource(id = R.drawable.ic_back), modifier = Modifier.size(32.dp), contentDescription = "Назад")
                    }

                    IconButton(onClick = { showHistoryDialog = true }, modifier = Modifier.size(44.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.icon_grafik_trenirovok),
                            modifier = Modifier.size(32.dp),
                            contentDescription = "Статистика"
                        )
                    }
                }

                // Центральная кнопка Старт
                val interactionSource = remember { MutableInteractionSource() }
                Box(modifier = Modifier.padding(bottom = 40.dp).size(105.dp), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.btn_start), 
                        contentDescription = "Старт",
                        modifier = Modifier.fillMaxSize().clickable(interactionSource = interactionSource, indication = null, onClick = { viewModel.startTrainingWithAd(context as Activity) { onStartClick() } }), 
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        if (showSaveDialog) {
            AlertDialog(onDismissRequest = { showSaveDialog = false }, title = { Text("Сохранить пресет") }, text = { TextField(value = newPresetName, onValueChange = { newPresetName = it }, placeholder = { Text("Название пресета") }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { Button(onClick = { if (newPresetName.isNotBlank()) { viewModel.saveUserPreset(newPresetName); showSaveDialog = false; newPresetName = "" } }) { Text("ОК") } }, dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Отмена") } })
        }

        if (showInfoDialog) { InfoDialog(onDismiss = { showInfoDialog = false }) }
        if (showHistoryDialog) { 
            HistoryDialog(
                history = uiState.trainingHistory,
                reminderTime = uiState.reminderTime,
                dayReminders = uiState.dayReminders,
                onSetReminder = { h, m, date -> viewModel.setReminder(h, m, date) },
                onCancelReminder = { date -> viewModel.cancelReminder(date) },
                onDismiss = { showHistoryDialog = false }
            ) 
        }
        if (showSettingsDialog) {
            SettingsDialog(
                isPremium = uiState.isPremium, userEmail = uiState.userEmail, isSyncing = uiState.isSyncing,
                monthlyPrice = uiState.monthlyPrice, lifetimePrice = uiState.lifetimePrice,
                musicVolume = uiState.musicVolume, breathVolume = uiState.breathVolume,
                vibrationEnabled = uiState.vibrationEnabled,
                reminderTime = uiState.reminderTime,
                finalSound = uiState.finalSound,
                finalSoundVolume = uiState.finalSoundVolume,
                availableFinalSounds = viewModel.finalSounds,
                onMusicVolumeChange = { viewModel.updateMusicVolume(it) }, onBreathVolumeChange = { viewModel.updateBreathVolume(it) },
                onVibrationEnabledChange = { viewModel.updateVibrationEnabled(it) },
                onPurchaseMonthly = { viewModel.purchaseMonthly() }, onPurchaseLifetime = { viewModel.purchaseLifetime() },
                onRestorePurchases = { viewModel.checkPurchases() }, onOpenSubscriptions = { viewModel.openRuStoreSubscriptions() },
                onSignInGoogle = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id)).requestEmail().build()
                    val client = GoogleSignIn.getClient(context, gso)
                    googleAuthLauncher.launch(client.signInIntent)
                },
                onSignInYandex = { viewModel.signInWithYandex() },
                onSignInVK = { viewModel.signInWithVK() },
                onSignOut = { viewModel.signOut() },
                onSetReminder = { h, m -> viewModel.setReminder(h, m) },
                onCancelReminder = { viewModel.cancelReminder() },
                onAddCustomFinalSound = { viewModel.addCustomFinalSound(it) },
                onUpdateFinalSound = { viewModel.updateFinalSound(it) },
                onUpdateFinalSoundVolume = { viewModel.updateFinalSoundVolume(it) },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

private fun getDaysWord(count: Int): String {
    val lastDigit = count % 10
    val lastTwoDigits = count % 100
    return when {
        lastTwoDigits in 11..19 -> "дней"
        lastDigit == 1 -> "день"
        lastDigit in 2..4 -> "дня"
        else -> "дней"
    }
}

@Composable
fun BreathingSlider(label: String, value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, isInt: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = White, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${range.start.toInt()} ${if (isInt) "" else "сек"}", color = White, fontSize = 10.sp)
            Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = LightCyan, activeTrackColor = LightCyan, inactiveTrackColor = GraySlider))
            Text("${range.endInclusive.toInt()} ${if (isInt) "" else "сек"}", color = White, fontSize = 10.sp)
        }
        Text("${value.toInt()} ${if (isInt) "" else "сек"}", color = LightCyan, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
