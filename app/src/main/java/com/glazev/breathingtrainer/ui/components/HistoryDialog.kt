package com.glazev.breathingtrainer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.glazev.breathingtrainer.R
import com.glazev.breathingtrainer.ui.TrainingRecord
import com.glazev.breathingtrainer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryDialog(
    history: List<TrainingRecord>,
    reminderTime: String? = null,
    dayReminders: Map<String, String> = emptyMap(),
    onSetReminder: (Int, Int, String?) -> Unit,
    onCancelReminder: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDayInfo by remember { mutableStateOf<Pair<List<TrainingRecord>, String>?>(null) }
    val scrollState = rememberScrollState()

    var showTimePickerForDate by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = DarkBlueBg
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Статистика / Напоминания",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BroadleafFontFamily,
                        maxLines = 1
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(painter = painterResource(id = R.drawable.ic_back), contentDescription = null, tint = White, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val wimHofHistory = history.filter { it.techniqueName == "Вим Хоф" && it.retentions.isNotEmpty() }
                if (wimHofHistory.isNotEmpty()) {
                    val retentionRecord = wimHofHistory.flatMap { it.retentions }.maxOrNull() ?: 0
                    val recordMinutes = retentionRecord / 60
                    val recordSeconds = retentionRecord % 60
                    Text(
                        text = "ПРОГРЕСС ЗАДЕРЖКИ (СЕК)",
                        color = LightCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = if (recordMinutes > 0) {
                            "Личный рекорд: ${recordMinutes}м ${recordSeconds}с"
                        } else {
                            "Личный рекорд: ${recordSeconds}с"
                        },
                        color = White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    RetentionChart(wimHofHistory)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                CalendarHeader(
                    currentMonth = currentMonth,
                    onMonthChange = { delta ->
                        val newMonth = currentMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, delta)
                        currentMonth = newMonth
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CalendarGrid(
                    currentMonth = currentMonth,
                    history = history,
                    dayReminders = dayReminders,
                    onDayClick = { records, dateStr ->
                        selectedDayInfo = records to dateStr
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LightCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Закрыть", color = DarkBlueBg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    selectedDayInfo?.let { info ->
        DayDetailsDialog(
            records = info.first,
            dateStr = info.second,
            reminderTime = dayReminders[info.second],
            onSetReminderClick = { showTimePickerForDate = info.second },
            onCancelReminderClick = { onCancelReminder(info.second) },
            onDismiss = { selectedDayInfo = null }
        )
    }

    if (showTimePickerForDate != null) {
        val dateStr = showTimePickerForDate
        val calendar = Calendar.getInstance()
        val currentVal = if (dateStr == null) reminderTime else dayReminders[dateStr]
        val h = currentVal?.split(":")?.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY)
        val m = currentVal?.split(":")?.getOrNull(1)?.toIntOrNull() ?: calendar.get(Calendar.MINUTE)

        AppTimePicker(
            initialHour = h,
            initialMinute = m,
            onConfirm = { hour, minute ->
                onSetReminder(hour, minute, dateStr)
                showTimePickerForDate = null
            },
            onDismiss = { showTimePickerForDate = null }
        )
    }
}

@Composable
fun RetentionChart(history: List<TrainingRecord>) {
    val lastSeven = history.sortedBy { it.date }.takeLast(7)
    val maxRetention = (lastSeven.flatMap { it.retentions }.maxOrNull() ?: 100).toFloat().coerceAtLeast(60f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spacing = width / (lastSeven.size.coerceAtLeast(2) - 1).coerceAtLeast(1)

                val points = lastSeven.mapIndexed { index, record ->
                    val avgRetention = record.retentions.average().toFloat()
                    val x = index * spacing
                    val y = height - (avgRetention / maxRetention * height)
                    Offset(x, y)
                }

                val lines = 3
                for (i in 0..lines) {
                    val y = height / lines * i
                    drawLine(
                        color = White.copy(alpha = 0.1f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (points.size >= 2) {
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(path = path, color = LightCyan, style = Stroke(width = 3.dp.toPx()))
                    points.forEach { point ->
                        drawCircle(color = MainTeal, radius = 4.dp.toPx(), center = point)
                        drawCircle(color = White, radius = 2.dp.toPx(), center = point)
                    }
                } else if (points.size == 1) {
                    drawCircle(color = LightCyan, radius = 6.dp.toPx(), center = points[0])
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(currentMonth: Calendar, onMonthChange: (Int) -> Unit) {
    val monthName = SimpleDateFormat("LLLL yyyy", Locale.forLanguageTag("ru"))
        .format(currentMonth.time)
        .replaceFirstChar { it.uppercase() }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMonthChange(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = LightCyan) }
        Text(text = monthName, color = LightCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = BroadleafFontFamily)
        IconButton(onClick = { onMonthChange(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = LightCyan) }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: Calendar, 
    history: List<TrainingRecord>, 
    dayReminders: Map<String, String>,
    onDayClick: (List<TrainingRecord>, String) -> Unit
) {
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val dayOfWeekOffset = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val days = (0 until 42).toList()

    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(280.dp), userScrollEnabled = false) {
            items(days) { index ->
                val dayNumber = index - dayOfWeekOffset + 1
                if (dayNumber in 1..daysInMonth) {
                    val dateCal = currentMonth.clone() as Calendar
                    dateCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", 
                        dateCal.get(Calendar.YEAR), dateCal.get(Calendar.MONTH) + 1, dateCal.get(Calendar.DAY_OF_MONTH))
                    
                    val dayRecords = history.filter { record ->
                        val recCal = Calendar.getInstance().apply { timeInMillis = record.date }
                        recCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) && recCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
                    }
                    
                    val hasReminder = dayReminders.containsKey(dateStr)
                    
                    DayCell(
                        dayNumber = dayNumber, 
                        hasTraining = dayRecords.isNotEmpty(), 
                        hasReminder = hasReminder,
                        onClick = { onDayClick(dayRecords, dateStr) }
                    )
                } else { Spacer(modifier = Modifier.aspectRatio(1f)) }
            }
        }
    }
}

@Composable
fun DayCell(dayNumber: Int, hasTraining: Boolean, hasReminder: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.aspectRatio(1f).padding(2.dp).clip(RoundedCornerShape(8.dp))
            .background(if (hasTraining) MainTeal.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber.toString(), 
            color = if (hasTraining) LightCyan else White.copy(alpha = 0.7f), 
            fontSize = 14.sp, 
            fontWeight = if (hasTraining) FontWeight.Bold else FontWeight.Normal
        )
        if (hasTraining) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo), 
                contentDescription = null, 
                modifier = Modifier.size(12.dp).align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 2.dp)
            )
        } else if (hasReminder) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo), 
                contentDescription = null, 
                modifier = Modifier.size(12.dp).align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 2.dp),
                colorFilter = ColorFilter.tint(Color.Gray)
            )
        }
    }
}

@Composable
fun DayDetailsDialog(
    records: List<TrainingRecord>, 
    dateStr: String,
    reminderTime: String?,
    onSetReminderClick: () -> Unit,
    onCancelReminderClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sdfDate = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("ru"))
    val calendar = Calendar.getInstance().apply {
        val parts = dateStr.split("-")
        set(Calendar.YEAR, parts[0].toInt())
        set(Calendar.MONTH, parts[1].toInt() - 1)
        set(Calendar.DAY_OF_MONTH, parts[2].toInt())
    }
    val dateFormatted = sdfDate.format(calendar.time)

    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f), shape = RoundedCornerShape(24.dp), color = DarkBlueBg) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = dateFormatted, color = LightCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = BroadleafFontFamily, modifier = Modifier.padding(bottom = 8.dp))
                
                if (reminderTime != null) {
                    Text(
                        text = "Напоминание: $reminderTime", 
                        color = White, 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Bold, 
                        fontFamily = BroadleafFontFamily,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (records.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(records.sortedByDescending { it.date }) { record ->
                            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val minutes = record.durationTotalSeconds / 60
                            val seconds = record.durationTotalSeconds % 60
                            val durationStr = if (minutes > 0) "${minutes}м ${seconds}с" else "${seconds}с"
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = TranslucentWhite), shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = record.techniqueName, color = LightCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = timeSdf.format(Date(record.date)), color = White.copy(alpha = 0.6f), fontSize = 14.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "Циклов: ${record.circles}", color = White, fontSize = 14.sp)
                                        Text(text = "Время: $durationStr", color = White, fontSize = 14.sp)
                                    }
                                    if (record.retentions.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Задержки:", color = LightCyan.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        FlowRowInternal(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            record.retentions.forEachIndexed { index, sec ->
                                                val m = sec / 60
                                                val s = sec % 60
                                                Text(text = if (m > 0) "${index + 1}: ${m}м ${s}с" else "${index + 1}: ${s}с", color = White, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Нет тренировок за этот день", color = White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onSetReminderClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = TranslucentWhite)) {
                    Icon(painter = painterResource(id = R.drawable.ic_info), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (reminderTime == null) "Установить напоминание" else "Изменить напоминание", color = White)
                }
                
                if (reminderTime != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onCancelReminderClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.3f))) {
                        Text("Отменить напоминание", color = White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MainTeal)) { Text("ОК", color = White) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowInternal(modifier: Modifier = Modifier, horizontalArrangement: Arrangement.Horizontal = Arrangement.Start, content: @Composable () -> Unit) {
    FlowRow(modifier = modifier, horizontalArrangement = horizontalArrangement) { content() }
}
