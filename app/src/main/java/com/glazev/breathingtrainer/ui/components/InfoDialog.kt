package com.glazev.breathingtrainer.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.glazev.breathingtrainer.AppLinks
import com.glazev.breathingtrainer.R
import com.glazev.breathingtrainer.BuildConfig
import com.glazev.breathingtrainer.openExternalLink
import com.glazev.breathingtrainer.privacy.PrivacyConsent
import com.glazev.breathingtrainer.ui.BackgroundMusic
import com.glazev.breathingtrainer.ui.theme.*
import java.util.Calendar

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = DarkBlueBg
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("ИНСТРУКЦИЯ И ПРАВИЛА", color = LightCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = BroadleafFontFamily)
                Spacer(modifier = Modifier.height(16.dp))

                InfoSection("Как пользоваться приложением", 
                    "1. Выберите готовую технику из списка или настройте параметры вдоха, выдоха и задержек вручную.\n" +
                    "2. Выберите фоновую музыку и голос сопровождения для комфортной практики.\n" +
                    "3. Нажмите центральную кнопку для старта. Следуйте анимации и звуковым командам.\n" +
                    "4. В режиме Вима Хофа: выполняйте активные вдохи, после чего делайте задержку на выдохе (Retention) и восстановительный вдох (Recovery).")

                InfoSection("Функции и возможности", 
                    "• 🔥 ОГОНЕК (СЕРИЯ): Отображает количество дней непрерывных тренировок. Не пропускайте занятия, чтобы поддерживать стрик!\n" +
                    "• 🎵 СВОЯ МУЗЫКА: Вы можете добавить любой аудиофайл со своего устройства в качестве фона.\n" +
                    "• ☁️ СИНХРОНИЗАЦИЯ: Войдите через Google, чтобы ваши пресеты, история и настройки сохранялись в облаке и были доступны на других устройствах.\n" +
                    "• 🔔 НАПОМИНАНИЯ: Настройте общее время уведомлений в настройках или установите индивидуальное время для любого дня через календарь.\n" +
                    "• 📳 ВИБРООТКЛИК: Приложение сигнализирует о смене фаз дыхания вибрацией, что позволяет тренироваться с закрытыми глазами.")

                InfoSection("Техники дыхания", 
                    "• Квадратное дыхание (4-4-4-4): Балансирует нервную систему, снимает стресс.\n" +
                    "• Успокоение (4-2-8-2): Помогает быстро заснуть и снизить тревожность.\n" +
                    "• Энергия (4-0-2-0): Пробуждает организм и повышает концентрацию.\n" +
                    "• Вим Хоф: Укрепляет иммунитет и тренирует сосуды через контролируемую гипервентиляцию.")

                InfoSection("Медицинские ограничения", 
                    "Дыхательные практики противопоказаны при:\n" +
                    "• Эпилепсии и тяжелых неврологических расстройствах.\n" +
                    "• Беременности (особенно задержки дыхания и метод Вима Хофа).\n" +
                    "• Сердечно-сосудистых заболеваниях в стадии обострения.\n" +
                    "• Недавних операциях на брюшной или грудной полости.\n" +
                    "\n• КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО выполнять любые упражнения во время управления автомобилем, при нахождении в воде или в любых других ситуациях, где временная потеря контроля над собой может быть опасна для жизни.\n" +
                    "\nВАЖНО: Если во время практики вы чувствуете сильную боль, звон в ушах или предобморочное состояние — немедленно прекратите упражнение.")

                InfoSection("Отказ от ответственности", 
                    "Данное приложение не является медицинским устройством и не предназначено для диагностики или лечения заболеваний. Использование техник осуществляется пользователем на свой страх и риск. Разработчик не несет ответственности за любые последствия для здоровья. Перед началом регулярных тренировок проконсультируйтесь с лечащим врачом.")

                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Лавка приложений",
                        color = LightCyan,
                        fontSize = 12.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                context.openExternalLink(AppLinks.RUSTORE_DEVELOPER_PAGE)
                            }
                            .padding(vertical = 4.dp)
                    )
                    Text("Версия ${BuildConfig.VERSION_NAME}", color = White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text(
                        text = "Политика конфиденциальности",
                        color = LightCyan,
                        fontSize = 12.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                context.openExternalLink(AppLinks.PRIVACY_POLICY)
                            }
                            .padding(vertical = 4.dp)
                    )
                   // Text("Автор: Путилов Денис, Глазьев Артём", color = White.copy(alpha = 0.5f), fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Заказать свое приложение:",
                        color = White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Телеграм @Applavka",
                        color = LightCyan,
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                context.openExternalLink(AppLinks.TELEGRAM)
                            }
                            .padding(vertical = 4.dp)
                    )
                    Text(
                        text = "ВКонтакте: https://vk.ru/applavka",
                        color = LightCyan,
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable {
                                context.openExternalLink(AppLinks.VK)
                            }
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss, 
                    modifier = Modifier.fillMaxWidth().height(50.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = MainTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Я ПРИНИМАЮ ПРАВИЛА", color = White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(title.uppercase(), color = MainTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(content, color = White.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun SettingsDialog(
    isPremium: Boolean,
    userEmail: String?,
    authProviderLabel: String?,
    isSyncing: Boolean,
    monthlyPrice: String,
    lifetimePrice: String,
    musicVolume: Float,
    breathVolume: Float,
    vibrationEnabled: Boolean,
    reminderTime: String?,
    finalSound: BackgroundMusic?,
    finalSoundVolume: Float,
    availableFinalSounds: List<BackgroundMusic>,
    privacyConsent: PrivacyConsent,
    onMusicVolumeChange: (Float) -> Unit,
    onBreathVolumeChange: (Float) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onPurchaseMonthly: () -> Unit,
    onPurchaseLifetime: () -> Unit,
    onRestorePurchases: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onSignInGoogle: () -> Unit,
    onSignInYandex: () -> Unit,
    onSignInVK: () -> Unit,
    onSignOut: () -> Unit,
    onSetReminder: (Int, Int) -> Unit,
    onCancelReminder: () -> Unit,
    onAddCustomFinalSound: (Uri) -> Unit,
    onUpdateFinalSound: (BackgroundMusic) -> Unit,
    onUpdateFinalSoundVolume: (Float) -> Unit,
    onPrivacyConsentChange: (PrivacyConsent) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showFinalSoundMenu by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val customSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onAddCustomFinalSound(it) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBlueBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Настройки", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(24.dp))

                // РАЗДЕЛ: ВИБРАЦИЯ
                Text("ОБРАТНАЯ СВЯЗЬ", color = LightCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Вибрация", color = White, fontSize = 14.sp)
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = onVibrationEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LightCyan,
                            checkedTrackColor = MainTeal
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = White.copy(alpha = 0.1f))

                // РАЗДЕЛ: ЗВУК ЗАВЕРШЕНИЯ
                Text("ЗВУК ЗАВЕРШЕНИЯ", color = LightCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showFinalSoundMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(finalSound?.name ?: "Не выбрано", color = White, fontSize = 14.sp)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = LightCyan)
                        }
                    }
                    DropdownMenu(
                        expanded = showFinalSoundMenu,
                        onDismissRequest = { showFinalSoundMenu = false },
                        modifier = Modifier.background(DarkBlueBg).fillMaxWidth(0.7f)
                    ) {
                        availableFinalSounds.forEach { sound ->
                            DropdownMenuItem(
                                text = { Text(sound.name, color = White) },
                                onClick = { onUpdateFinalSound(sound); showFinalSoundMenu = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Свой звук", color = LightCyan, fontWeight = FontWeight.Bold) },
                            onClick = { customSoundLauncher.launch(arrayOf("audio/*")); showFinalSoundMenu = false }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Громкость финала", color = White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                Slider(
                    value = finalSoundVolume,
                    onValueChange = onUpdateFinalSoundVolume,
                    colors = SliderDefaults.colors(thumbColor = LightCyan, activeTrackColor = LightCyan)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = White.copy(alpha = 0.1f))

                // РАЗДЕЛ: НАПОМИНАНИЯ
                Text("ЕЖЕДНЕВНЫЕ УВЕДОМЛЕНИЯ", color = LightCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (reminderTime != null) "Время: $reminderTime" else "Выключены",
                        color = White, fontSize = 14.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (reminderTime != null) {
                            IconButton(onClick = onCancelReminder, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close, 
                                    contentDescription = "Удалить", 
                                    tint = Color.Red,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(
                            onClick = { showTimePicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MainTeal),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (reminderTime == null) "Включить" else "Изменить", 
                                fontSize = 12.sp, 
                                color = White
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = White.copy(alpha = 0.1f))

                // РАЗДЕЛ: СИНХРОНИЗАЦИЯ И ВХОД
                Text("СИНХРОНИЗАЦИЯ И ВХОД", color = LightCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (userEmail == null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSignInYandex,
                            colors = ButtonDefaults.buttonColors(containerColor = TranslucentWhite),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Войти через Яндекс", color = White, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onSignInVK,
                            colors = ButtonDefaults.buttonColors(containerColor = TranslucentWhite),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Войти через VK ID", color = White, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onSignInGoogle,
                            colors = ButtonDefaults.buttonColors(containerColor = TranslucentWhite),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_logo), contentDescription = null, tint = White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Войти через Google", color = White)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MainTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = authProviderLabel?.let { "Аккаунт $it:" } ?: "Аккаунт:",
                                color = White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                            Text(userEmail, color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onSignOut) {
                            Text("Выйти", color = Color.Red.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                    if (isSyncing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 4.dp), color = LightCyan)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = White.copy(alpha = 0.1f))

                Text("КОНФИДЕНЦИАЛЬНОСТЬ", color = LightCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                SettingsPrivacySwitch(
                    title = "Аналитика приложения",
                    checked = privacyConsent.analyticsEnabled,
                    onCheckedChange = { enabled ->
                        onPrivacyConsentChange(privacyConsent.copy(isDecided = true, analyticsEnabled = enabled))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsPrivacySwitch(
                    title = "Персонализация рекламы",
                    checked = privacyConsent.personalizedAdsEnabled,
                    onCheckedChange = { enabled ->
                        onPrivacyConsentChange(privacyConsent.copy(isDecided = true, personalizedAdsEnabled = enabled))
                    }
                )
                Text(
                    text = "При выключенной персонализации реклама может оставаться, но согласие на персонализацию не передаётся.",
                    color = White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Политика конфиденциальности",
                    color = LightCyan,
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable {
                            context.openExternalLink(AppLinks.PRIVACY_POLICY)
                        }
                        .padding(top = 10.dp, bottom = 2.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = White.copy(alpha = 0.1f))

                // РАЗДЕЛ: ГРОМКОСТЬ
                Text("ГРОМКОСТЬ", color = LightCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Музыка", color = White, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                Slider(value = musicVolume, onValueChange = onMusicVolumeChange, colors = SliderDefaults.colors(thumbColor = LightCyan, activeTrackColor = LightCyan))
                
                Text("Дыхание / Голос", color = White, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                Slider(value = breathVolume, onValueChange = onBreathVolumeChange, colors = SliderDefaults.colors(thumbColor = LightCyan, activeTrackColor = LightCyan))

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = White.copy(alpha = 0.1f))

                // РАЗДЕЛ: PREMIUM
                Text("PREMIUM", color = MainTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (!isPremium) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Отключите рекламу и поддержите проект", color = White.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = onPurchaseMonthly, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63).copy(alpha = 0.8f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).padding(4.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Месяц", fontSize = 12.sp, color = White)
                                    if (monthlyPrice.isNotEmpty()) Text(monthlyPrice, fontSize = 10.sp, color = White)
                                }
                            }
                            Button(onClick = { onPurchaseLifetime() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.8f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).padding(4.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Навсегда", fontSize = 12.sp, color = White)
                                    if (lifetimePrice.isNotEmpty()) Text(lifetimePrice, fontSize = 10.sp, color = White)
                                }
                            }
                        }
                        TextButton(onClick = onRestorePurchases) { Text("Восстановить покупки", color = White.copy(alpha = 0.6f), fontSize = 11.sp) }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().background(MainTeal.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("💎 PREMIUM АКТИВИРОВАН", color = MainTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MainTeal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Закрыть", color = White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance()
        val h = reminderTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
        val m = reminderTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)

        AppTimePicker(
            initialHour = h,
            initialMinute = m,
            onConfirm = { hour, minute ->
                onSetReminder(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun SettingsPrivacySwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LightCyan,
                checkedTrackColor = MainTeal
            )
        )
    }
}
