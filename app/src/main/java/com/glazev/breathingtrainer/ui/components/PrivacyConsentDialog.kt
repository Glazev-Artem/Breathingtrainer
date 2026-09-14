package com.glazev.breathingtrainer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.glazev.breathingtrainer.AppLinks
import com.glazev.breathingtrainer.openExternalLink
import com.glazev.breathingtrainer.privacy.PrivacyConsent
import com.glazev.breathingtrainer.ui.theme.DarkBlueBg
import com.glazev.breathingtrainer.ui.theme.LightCyan
import com.glazev.breathingtrainer.ui.theme.MainTeal
import com.glazev.breathingtrainer.ui.theme.White

@Composable
fun PrivacyConsentDialog(
    initialConsent: PrivacyConsent,
    onSave: (PrivacyConsent) -> Unit
) {
    val context = LocalContext.current
    var analyticsEnabled by remember(initialConsent) {
        mutableStateOf(initialConsent.analyticsEnabled)
    }
    var personalizedAdsEnabled by remember(initialConsent) {
        mutableStateOf(initialConsent.personalizedAdsEnabled)
    }

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBlueBg)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Конфиденциальность",
                    color = White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Вход, подписки, синхронизация и тренировки работают при любом выборе. Необязательные функции изначально выключены.",
                    color = White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                ConsentSwitch(
                    title = "Аналитика приложения",
                    description = "Помогает находить ошибки и улучшать приложение.",
                    checked = analyticsEnabled,
                    onCheckedChange = { analyticsEnabled = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ConsentSwitch(
                    title = "Персонализация рекламы",
                    description = "Если выключено, реклама всё равно может показываться, но без вашего согласия на персонализацию.",
                    checked = personalizedAdsEnabled,
                    onCheckedChange = { personalizedAdsEnabled = it }
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Открыть политику конфиденциальности",
                    color = LightCyan,
                    fontSize = 13.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable {
                            context.openExternalLink(AppLinks.PRIVACY_POLICY)
                        }
                        .padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSave(
                            PrivacyConsent(
                                isDecided = true,
                                analyticsEnabled = analyticsEnabled,
                                personalizedAdsEnabled = personalizedAdsEnabled
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MainTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить и продолжить", color = White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ConsentSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(3.dp))
            Text(description, color = White.copy(alpha = 0.65f), fontSize = 12.sp)
        }
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
