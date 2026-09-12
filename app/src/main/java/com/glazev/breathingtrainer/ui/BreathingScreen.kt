package com.glazev.breathingtrainer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glazev.breathingtrainer.R
import com.glazev.breathingtrainer.ui.theme.*
import com.glazev.breathingtrainer.ui.components.InfoDialog
import com.glazev.breathingtrainer.ui.components.SettingsDialog
import com.glazev.breathingtrainer.ui.components.HistoryDialog
import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.glazev.breathingtrainer.model.PhaseType
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlin.math.ceil

@Suppress("DEPRECATION")
@Composable
fun BreathingScreen(
    viewModel: BreathingViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // Перехватываем системную кнопку "Назад" и жесты
    BackHandler {
        onBackClick()
    }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.signInWithGoogle(it) }
            } catch (_: Exception) {}
        }
    }
    
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Гарантируем остановку всех звуков и таймеров при выходе с экрана
            viewModel.stopTraining()
        }
    }
    
    // Параметры анимации масштаба
    val animationData = remember(uiState.currentPhaseType, uiState.isRunning, uiState.isCountingDown, uiState.isWimHofRetentionPhase, uiState.isWimHofRecoveryPhase) {
        val targetScale: Float
        val duration: Int
        
        when {
            uiState.isCountingDown -> {
                targetScale = 0.7f
                duration = 1000 // Держим минимальный размер во время подготовки
            }
            uiState.isWimHofRetentionPhase -> {
                targetScale = 0.7f
                duration = 1000 // Плавное сжатие в начале задержки
            }
            uiState.isWimHofRecoveryPhase -> {
                targetScale = 1.5f
                duration = (uiState.wimHofRecoveryTime * 1000).toInt()
            }
            !uiState.isRunning -> {
                targetScale = 0.7f // В покое тоже держим минимальный масштаб
                duration = 1000
            }
            else -> {
                when (uiState.currentPhaseType) {
                    PhaseType.INHALE -> {
                        targetScale = 1.5f
                        duration = (uiState.inhaleTime * 1000).toInt()
                    }
                    PhaseType.HOLD_IN -> {
                        targetScale = 1.5f
                        duration = 0 // Стоим на месте
                    }
                    PhaseType.EXHALE -> {
                        targetScale = 0.7f
                        duration = (uiState.exhaleTime * 1000).toInt()
                    }
                    PhaseType.HOLD_OUT -> {
                        targetScale = 0.7f
                        duration = 0 // Стоим на месте
                    }
                    else -> {
                        targetScale = 1.0f
                        duration = 1000
                    }
                }
            }
        }
        Pair(targetScale, duration)
    }

    val scale by animateFloatAsState(
        targetValue = animationData.first,
        animationSpec = if (animationData.second > 0) {
            tween(durationMillis = animationData.second, easing = LinearEasing)
        } else {
            snap() // Мгновенное переключение или фиксация, если длительность 0
        },
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationClockwise by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(20000, easing = LinearEasing)),
        label = "clockwise"
    )
    val rotationCounterClockwise by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(animation = tween(25000, easing = LinearEasing)),
        label = "counter_clockwise"
    )

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Box(modifier = Modifier.fillMaxSize()
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                if (uiState.isRunning) {
                    if (uiState.isWimHofRetentionPhase) {
                        viewModel.finishRetention()
                    }
                }
            }
        )
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_main_gradient),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
            Image(
                painter = painterResource(id = R.drawable.ic_circle_bg),
                contentDescription = null,
                modifier = Modifier.offset(y = 40.dp).size(280.dp).scale(pulseScale),
                alpha = 0.4f
            )
        }

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // ВЕРХНЯЯ ПАНЕЛЬ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { showInfoDialog = true }, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                    Image(painter = painterResource(id = R.drawable.ic_info), contentDescription = "Инфо", modifier = Modifier.size(32.dp))
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    Image(painter = painterResource(id = R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Тренажер дыхания", 
                        color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = BroadleafFontFamily, textAlign = TextAlign.Center, 
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.align(Alignment.CenterEnd).size(40.dp)) {
                    Image(painter = painterResource(id = R.drawable.icon_setings), contentDescription = "Настройки", modifier = Modifier.size(32.dp))
                }
            }
            
            HorizontalDivider(color = White.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Стрик (дни подряд)
                if (uiState.streakCount > 0) {
                    Row(
                        modifier = Modifier
                            .background(MainTeal.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .clickable { showHistoryDialog = true }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${uiState.streakCount} ${getDayWord(uiState.streakCount)} подряд",
                            color = LightCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = if (uiState.isWimHofMode) "Метод Вима Хофа" else (uiState.activeTechnique?.name ?: "Пользовательский"),
                    color = LightCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = BroadleafFontFamily
                )
                
                if (!uiState.isWimHofMode) {
                    val timing = "${uiState.inhaleTime.toInt()} - ${uiState.inhaleHoldTime.toInt()} - ${uiState.exhaleTime.toInt()} - ${uiState.exhaleHoldTime.toInt()}"
                    Text(text = timing, color = LightCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = BroadleafFontFamily)
                    Text(text = "Повторение: ${uiState.currentRepetition}/${uiState.repetitions}", color = White.copy(alpha = 0.7f), fontSize = 16.sp)
                } else {
                    Text(text = "Круг: ${uiState.currentCycle}/${uiState.cycles}", color = LightCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = BroadleafFontFamily)
                    if (!uiState.isWimHofRetentionPhase && !uiState.isWimHofRecoveryPhase && uiState.isRunning) {
                        Text(text = "Повторение: ${uiState.currentRepetition}/${uiState.repetitions}", color = White.copy(alpha = 0.7f), fontSize = 16.sp)
                    }
                }
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.ic_breath_shape_1), contentDescription = null, modifier = Modifier.size(260.dp).scale(scale).rotate(rotationClockwise), alpha = 0.6f)
                        Image(painter = painterResource(id = R.drawable.ic_breath_shape_2), contentDescription = null, modifier = Modifier.size(240.dp).scale(scale * 0.9f).rotate(rotationCounterClockwise), alpha = 0.4f)
                        
                        if (!uiState.isRunning && !uiState.isCountingDown) {
                            Image(painter = painterResource(id = R.drawable.ic_meditation), contentDescription = "Продолжить", modifier = Modifier.size(105.dp).clickable { viewModel.startTrainingWithAd(context as Activity, onAdDismissed = { viewModel.startTraining() }) })
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val title = when {
                                uiState.isWimHofRetentionPhase -> "ЗАДЕРЖКА"
                                uiState.isWimHofRecoveryPhase -> "ВОССТАНОВЛЕНИЕ"
                                uiState.isRunning -> {
                                    when(uiState.currentPhaseType) {
                                        PhaseType.INHALE -> "ВДОХ"
                                        PhaseType.EXHALE -> "ВЫДОХ"
                                        PhaseType.HOLD_IN, PhaseType.HOLD_OUT -> "ЗАДЕРЖКА"
                                        else -> ""
                                    }
                                }
                                else -> ""
                            }
                            
                            val seconds = if (uiState.isCountingDown) uiState.countdownSeconds.toFloat() else uiState.remainingSeconds

                            if (uiState.isCountingDown) {
                                Text(
                                    text = "ГОТОВНОСТЬ",
                                    color = White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BroadleafFontFamily
                                )
                                Text(
                                    text = seconds.toInt().toString(),
                                    color = White,
                                    fontSize = 80.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (uiState.isRunning) {
                                Text(
                                    text = title,
                                    color = White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BroadleafFontFamily
                                )
                                if (uiState.isWimHofRecoveryPhase) {
                                    Text(
                                        text = "ВДОХ",
                                        color = White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = BroadleafFontFamily
                                    )
                                }
                                val displaySeconds = ceil(seconds.toDouble()).toInt()
                                Text(
                                    text = displaySeconds.toString(),
                                    color = White,
                                    fontSize = 80.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                if (uiState.isWimHofRetentionPhase) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Нажмите для вдоха",
                                        color = White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }

            // --- НИЖНЯЯ ПАНЕЛЬ (ВТОРОЙ ЭКРАН - ТРЕНИРОВКА) ---
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
                    IconButton(onClick = onBackClick, modifier = Modifier.size(44.dp)) {
                        Image(painter = painterResource(id = R.drawable.ic_back), modifier = Modifier.size(32.dp), contentDescription = "Назад")
                    }

                    // Статистика справа (как на первом экране)
                    IconButton(onClick = { showHistoryDialog = true }, modifier = Modifier.size(44.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.icon_grafik_trenirovok),
                            modifier = Modifier.size(32.dp),
                            contentDescription = "Статистика"
                        )
                    }
                }

                // Центральная кнопка Плей/Пауза (того же размера и положения, что и Старт на первом экране)
                Box(modifier = Modifier.padding(bottom = 40.dp).size(105.dp), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.knopka_krug), 
                        contentDescription = "Плей Пауза", 
                        modifier = Modifier.fillMaxSize().clickable(
                            interactionSource = interactionSource, 
                            indication = null, 
                            onClick = { 
                                if (uiState.isRunning) {
                                    if (uiState.isWimHofRetentionPhase) viewModel.finishRetention()
                                    else viewModel.stopTraining()
                                }
                                else viewModel.startTrainingWithAd(context as Activity, onAdDismissed = { viewModel.startTraining() }) 
                            }
                        ), 
                        contentScale = ContentScale.Fit
                    )
                    Image(
                        painter = painterResource(id = if (uiState.isRunning) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = null,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        if (showInfoDialog) {
            InfoDialog(onDismiss = { showInfoDialog = false })
        }

        if (showSettingsDialog) {
            SettingsDialog(
                isPremium = uiState.isPremium,
                userEmail = uiState.userEmail,
                isSyncing = uiState.isSyncing,
                monthlyPrice = uiState.monthlyPrice,
                lifetimePrice = uiState.lifetimePrice,
                musicVolume = uiState.musicVolume,
                breathVolume = uiState.breathVolume,
                vibrationEnabled = uiState.vibrationEnabled,
                reminderTime = uiState.reminderTime,
                finalSound = uiState.finalSound,
                finalSoundVolume = uiState.finalSoundVolume,
                availableFinalSounds = viewModel.finalSounds,
                onMusicVolumeChange = { viewModel.updateMusicVolume(it) },
                onBreathVolumeChange = { viewModel.updateBreathVolume(it) },
                onVibrationEnabledChange = { viewModel.updateVibrationEnabled(it) },
                onPurchaseMonthly = { viewModel.purchaseMonthly() },
                onPurchaseLifetime = { viewModel.purchaseLifetime() },
                onRestorePurchases = { viewModel.checkPurchases() },
                onOpenSubscriptions = { viewModel.openRuStoreSubscriptions() },
                onSignInGoogle = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
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
    }
}

fun getDayWord(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "дней"
        mod10 == 1 -> "день"
        mod10 in 2..4 -> "дня"
        else -> "дней"
    }
}
