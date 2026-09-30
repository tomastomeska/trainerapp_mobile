package cz.trainerapp.mobile

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

private val TrainerAppNavy = Color(0xFF2A3140)
private val TrainerAppYellow = Color(0xFFD4AF37)
private val TrainerAppLight = Color(0xFFE6E8ED)
private val TrainerAppGray = Color(0xFF6E7580)
private val MetalCard = Color(0xFFF6F4EF)
private val MetalHeaderBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF6A7382), Color(0xFF1C2230), Color(0xFF3D4656))
)
private val MetalGoldBrush = Brush.linearGradient(
    colors = listOf(Color(0xFFF6E7B2), Color(0xFFD4AF37), Color(0xFF8C6E1F), Color(0xFFE7D48A))
)

private const val PREFS_NAME = "trainerapp_session"
private const val PREF_TOKEN = "token"
private const val PREF_TYPE = "account_type"
private const val PREF_NAME = "trainer_name"
private const val PREF_ID = "trainer_id"
private const val PREF_LAST_PENDING = "last_pending"
private const val PREF_LAST_EVENTS = "last_events"
private const val PREF_LAST_UNREAD = "last_unread"
private const val PREF_EXACT_ALARM_ASKED = "exact_alarm_asked"
private const val PREF_FORCE_PASSWORD = "force_password_change"
private const val PREF_PHOTO = "profile_photo"

private const val API_BASE_URL = "https://www.reservio.online/api/mobile"
private const val WEB_LOGIN_URL = "https://www.reservio.online/login.php"

private fun resolvedApiBase(@Suppress("UNUSED_PARAMETER") context: Context): String = API_BASE_URL

data class RecentTraining(
    val id: String,
    val date: String,
    val setName: String
)

data class AthleteItem(
    val id: String,
    val name: String,
    val detail: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val recentTrainings: List<RecentTraining> = emptyList()
)

data class AthleteFullDetail(
    val id: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val birthDate: String = "",
    val age: Int? = null,
    val photoUrl: String = "",
    val trainings: List<TrainingItem> = emptyList(),
    val weightLogs: List<WeightLogItem> = emptyList()
)

data class WeightLogItem(
    val id: Int,
    val date: String,
    val weightKg: Float
)

data class TrainingItem(
    val id: String,
    val time: String,
    val date: String = "",
    val athleteName: String,
    val detail: String,
    val status: String = "Potvrzeno",
    val approvalStatus: String = "approved",
    val isLocked: Boolean = false,
    val startHour: Int = 9,
    val endHour: Int = 10,
    val athleteId: Int = 0,
    val title: String = "",
    val location: String = "",
    val startsAt: String = "",
    val endsAt: String = "",
    val colorKey: String = "green",
    val secondAthleteId: Int = 0,
    val seriesId: String = "",
    val awaitingReschedule: Boolean = false
)

data class OpenSession(
    val id: Int,
    val athleteId: Int,
    val athleteName: String,
    val setName: String,
    val startedAt: String
)

data class WorkoutSetItem(
    val id: Int,
    val name: String,
    val description: String = "",
    val exerciseCount: Int = 0,
    val isActive: Boolean = true,
    val isGlobal: Boolean = false
)

data class VenueItem(
    val id: Int,
    val name: String,
    val address: String = ""
)

data class ExerciseItem(
    val id: Int,
    val name: String,
    val sportType: String
)

data class SessionExerciseItem(
    val exerciseId: Int,
    val exerciseOrder: Int,
    val exerciseName: String,
    val sportType: String = "standard",
    val isTimed: Boolean = false,
    val series: List<SessionSeriesItem> = emptyList(),
    val previousSeries: List<SessionSeriesItem> = emptyList(),
    val previousLabel: String = ""
)

data class SessionSeriesItem(
    val id: Int,
    val seriesOrder: Int,
    val weight: Float,
    val reps: Int,
    val durationSeconds: Int? = null
)

data class ChatMessage(
    val id: String,
    val senderName: String,
    val isMe: Boolean,
    val isRead: Boolean = false,
    val text: String,
    val time: String,
    val wasUnread: Boolean = false
)

data class ChatConversation(
    val id: String,
    val name: String,
    val subtitle: String,
    val icon: String,
    val isAdmin: Boolean = false,
    val unreadCount: Int = 0,
    val initialMessages: List<ChatMessage> = emptyList(),
    val photoUrl: String = ""
)

private object AppVisibility {
    @Volatile
    var isForeground: Boolean = false
}

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppVisibility.isForeground = true
        WindowCompat.setDecorFitsSystemWindows(window, false)
        createNotificationChannel(this)
        requestNotificationPermission()
        maybeRequestExactAlarms(this)

        setContent {
            TrainerAppTheme {
                Box(Modifier.fillMaxSize().imePadding()) {
                    TrainerAppRoot(this@MainActivity)
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.isForeground = true
        cancelAlertSync(this)
    }

    override fun onStop() {
        AppVisibility.isForeground = false
        scheduleAlertSync(this, 15_000L)
        super.onStop()
    }
}

private const val CHANNEL_ID = "trainerapp_alerts"

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TrainerApp Upozornění",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikace na nové zprávy v chatu a změny v kalendáři"
            enableVibration(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

@Suppress("MissingPermission")
private fun sendSystemNotification(context: Context, title: String, message: String, notificationId: Int = 1001) {
    if (AppVisibility.isForeground) return
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = NotificationManagerCompat.from(context)
        @SuppressLint("MissingPermission")
        manager.notify(notificationId, builder.build())
    } catch (_: Throwable) {}
}

private fun cancelSystemNotification(context: Context, notificationId: Int) {
    try {
        NotificationManagerCompat.from(context).cancel(notificationId)
    } catch (_: Throwable) {}
}

private fun publishAlertChanges(context: Context, prefs: SharedPreferences, pending: Int, realEvents: Int, unread: Int) {
    val lastPending = prefs.getInt(PREF_LAST_PENDING, -1)
    val lastEvents = prefs.getInt(PREF_LAST_EVENTS, -1)
    val lastUnread = prefs.getInt(PREF_LAST_UNREAD, -1)
    val pendingGrew = lastPending >= 0 && pending > lastPending
    val eventsGrew = lastEvents >= 0 && realEvents > lastEvents
    if (pending == 0) cancelSystemNotification(context, 2003)
    if (unread == 0) cancelSystemNotification(context, 2001)
    if (pendingGrew) {
        sendSystemNotification(context, "Žádost ke schválení", "V kalendáři je nová žádost ke schválení.", 2003)
    } else if (eventsGrew) {
        sendSystemNotification(context, "Kalendář TrainerApp", "V kalendáři je nová změna.", 2002)
    }
    if (lastUnread >= 0 && unread > lastUnread) {
        sendSystemNotification(context, "TrainerApp Chat", "Máte novou zprávu v chatu.", 2001)
    }
    prefs.edit()
        .putInt(PREF_LAST_PENDING, pending)
        .putInt(PREF_LAST_EVENTS, realEvents)
        .putInt(PREF_LAST_UNREAD, unread)
        .apply()
}

private fun alertCountsFromCalendar(json: JSONObject): Pair<Int, Int> {
    val array = json.optJSONArray("events") ?: json.optJSONArray("items")
    var pending = 0
    var real = 0
    if (array != null) {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val locked = obj.optBoolean("is_locked", false)
            if (locked) continue
            real++
            if (obj.optString("approval_status", "approved").equals("pending", true)) pending++
        }
    }
    return pending to real
}

private fun unreadFromChat(json: JSONObject): Int {
    val array = json.optJSONArray("conversations") ?: return 0
    var unread = 0
    for (i in 0 until array.length()) {
        unread += array.optJSONObject(i)?.optInt("unread_count", 0) ?: 0
    }
    return unread
}

private const val ALERT_SYNC_ACTION = "cz.trainerapp.mobile.ALERT_SYNC"

private fun alertSyncPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, AlertSyncReceiver::class.java).setAction(ALERT_SYNC_ACTION)
    return PendingIntent.getBroadcast(
        context,
        2401,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
}

private fun maybeRequestExactAlarms(activity: ComponentActivity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (canScheduleExactAlarms(activity)) return
    val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.getString(PREF_TOKEN, "").isNullOrBlank()) return
    if (prefs.getBoolean(PREF_EXACT_ALARM_ASKED, false)) return
    prefs.edit().putBoolean(PREF_EXACT_ALARM_ASKED, true).apply()
    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${activity.packageName}")
    }
    try {
        activity.startActivity(intent)
    } catch (_: Exception) {}
}

private fun scheduleAlertSync(context: Context, delayMs: Long) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.getString(PREF_TOKEN, "").isNullOrBlank()) return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerAt = SystemClock.elapsedRealtime() + delayMs
    val pendingIntent = alertSyncPendingIntent(context)
    if (canScheduleExactAlarms(context)) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
    } else {
        alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
    }
}

private fun cancelAlertSync(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(alertSyncPendingIntent(context))
}

private fun runAlertSync(context: Context) {
    if (AppVisibility.isForeground) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val token = prefs.getString(PREF_TOKEN, "") ?: ""
    if (token.isBlank()) return
    createNotificationChannel(context)
    val calendar = httpRequest(context, "calendar.php?token=${tokenQuery(token)}", token = token)
    val chat = httpRequest(context, "chat.php?action=conversations&token=${tokenQuery(token)}", token = token)
    val calendarJson = calendar.jsonOrNull()
    val chatJson = chat.jsonOrNull()
    if (calendar.code !in 200..299 || calendarJson == null || chat.code !in 200..299 || chatJson == null) return
    val (pending, realEvents) = alertCountsFromCalendar(calendarJson)
    publishAlertChanges(context, prefs, pending, realEvents, unreadFromChat(chatJson))
}

class AlertSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ALERT_SYNC_ACTION) return
        val appContext = context.applicationContext
        if (!AppVisibility.isForeground) {
            scheduleAlertSync(appContext, 2 * 60 * 1000L)
        }
        val pendingResult = goAsync()
        Thread {
            try {
                runAlertSync(appContext)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}

class AlertBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        scheduleAlertSync(context.applicationContext, 60_000L)
    }
}

private fun jsonId(obj: JSONObject, key: String = "id"): String {
    if (!obj.has(key) || obj.isNull(key)) return ""
    val raw = obj.opt(key)
    return when (raw) {
        is Number -> raw.toInt().toString()
        else -> raw?.toString().orEmpty()
    }
}

private fun formatKg(value: Float): String {
    return if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
}

private fun parseAthletesFromJson(json: JSONObject): List<AthleteItem> {
    val list = mutableListOf<AthleteItem>()
    val array = json.optJSONArray("athletes") ?: json.optJSONArray("sportovci") ?: json.optJSONArray("clients") ?: json.optJSONArray("data")
    if (array != null) {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", i.toString())
            val fullName = item.optString("full_name", "").takeIf { it.isNotBlank() }
                ?: (item.optString("first_name", "") + " " + item.optString("last_name", "")).trim().takeIf { it.isNotBlank() }
                ?: item.optString("name", "Sportovec")
            val email = item.optString("email", "")
            val phone = item.optString("phone", item.optString("telephone", item.optString("phone_contact", "")))
            val photo = item.optString("photo", item.optString("photo_url", item.optString("avatar", "")))
            val note = item.optString("note", item.optString("detail", ""))
            val lastTraining = item.optString("last_training_at", "")
            val detailText = if (lastTraining.isNotBlank()) "Poslední trénink: $lastTraining" else note
            val recent = mutableListOf<RecentTraining>()
            val recentArray = item.optJSONArray("recent_trainings")
            if (recentArray != null) {
                for (r in 0 until recentArray.length()) {
                    val row = recentArray.optJSONObject(r) ?: continue
                    recent.add(
                        RecentTraining(
                            id = jsonId(row),
                            date = row.optString("date", row.optString("started_at", "")),
                            setName = row.optString("set_name", "Trénink")
                        )
                    )
                }
            }

            list.add(AthleteItem(id, fullName, detailText, email, phone, photo, recent))
        }
    }
    return list
}

@Composable
private fun TrainerAppRoot(context: Context) {
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var token by remember { mutableStateOf(prefs.getString(PREF_TOKEN, "") ?: "") }
    var trainerName by remember { mutableStateOf(prefs.getString(PREF_NAME, "") ?: "") }
    var trainerId by remember { mutableStateOf(prefs.getString(PREF_ID, "") ?: "") }
    var accountType by remember { mutableStateOf(prefs.getString(PREF_TYPE, "trainer") ?: "trainer") }
    var forcePasswordChange by remember { mutableStateOf(prefs.getBoolean(PREF_FORCE_PASSWORD, false)) }
    var profilePhoto by remember { mutableStateOf(prefs.getString(PREF_PHOTO, "") ?: "") }
    var lastLoginScreen by remember { mutableStateOf("trainer_login") }

    var athletesList by remember { mutableStateOf<List<AthleteItem>>(emptyList()) }
    var workoutSetsList by remember { mutableStateOf<List<WorkoutSetItem>>(emptyList()) }
    var venuesList by remember { mutableStateOf<List<VenueItem>>(emptyList()) }
    var trainingsList by remember { mutableStateOf<List<TrainingItem>>(emptyList()) }
    var conversationsList by remember { mutableStateOf<List<ChatConversation>>(emptyList()) }
    var activeSessionId by remember { mutableIntStateOf(0) }
    var openSessions by remember { mutableStateOf<List<OpenSession>>(emptyList()) }
    var selectedChatConversationId by remember { mutableStateOf<String?>(null) }
    var isLoadingData by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }
    var dataError by remember { mutableStateOf("") }

    val totalUnreadChatCount = remember(conversationsList) {
        conversationsList.sumOf { it.unreadCount }
    }
    val pendingCalendarCount = remember(trainingsList) {
        trainingsList.count { it.approvalStatus.equals("pending", true) && !it.isLocked }
    }

    fun refreshApiData() {
        if (token.isBlank() || accountType == "athlete") {
            isLoadingData = false
            hasLoadedOnce = true
            return
        }
        if (!hasLoadedOnce) isLoadingData = true

        fetchUserDataFromApi(
            context = context,
            token = token,
            onSuccess = { fetchedName, fetchedPhoto ->
                if (fetchedName.isNotBlank()) {
                    trainerName = fetchedName
                    prefs.edit().putString(PREF_NAME, fetchedName).apply()
                }
                if (fetchedPhoto.isNotBlank()) {
                    profilePhoto = fetchedPhoto
                    prefs.edit().putString(PREF_PHOTO, fetchedPhoto).apply()
                }
            },
            onError = {}
        )

        fetchAthletesFromApi(
            context = context,
            token = token,
            onSuccess = { athletes ->
                athletesList = athletes
                dataError = ""

                fetchCalendarFromApi(
                    context = context,
                    token = token,
                    onSuccess = { calEvents ->
                        val pending = calEvents.count { it.approvalStatus.equals("pending", true) && !it.isLocked }
                        val realEvents = calEvents.count { !it.isLocked }
                        val unreadNow = prefs.getInt(PREF_LAST_UNREAD, conversationsList.sumOf { it.unreadCount })
                        publishAlertChanges(context, prefs, pending, realEvents, unreadNow)
                        trainingsList = calEvents
                        hasLoadedOnce = true
                        isLoadingData = false
                    },
                    onError = { message ->
                        dataError = message
                        hasLoadedOnce = true
                        isLoadingData = false
                    }
                )
            },
            onError = { message ->
                dataError = message
                hasLoadedOnce = true
                isLoadingData = false
            }
        )

        fetchWorkoutSetsFromApi(
            context = context,
            token = token,
            onSuccess = { sets -> workoutSetsList = sets },
            onError = {}
        )

        fetchVenuesFromApi(
            context = context,
            token = token,
            onSuccess = { vList -> venuesList = vList }
        )

        fetchOpenSessions(context, token) { sessions -> openSessions = sessions }

        fetchChatConversationsFromApi(
            context = context,
            token = token,
            onSuccess = { convs ->
                conversationsList = convs
                val newUnread = convs.sumOf { it.unreadCount }
                val pendingNow = prefs.getInt(PREF_LAST_PENDING, trainingsList.count { it.approvalStatus.equals("pending", true) && !it.isLocked })
                val eventsNow = prefs.getInt(PREF_LAST_EVENTS, trainingsList.count { !it.isLocked })
                publishAlertChanges(context, prefs, pendingNow, eventsNow, newUnread)
            }
        )
    }

    LaunchedEffect(token) {
        if (token.isNotBlank()) {
            refreshApiData()
            while (true) {
                delay(15000)
                if (token.isNotBlank()) {
                    refreshApiData()
                }
            }
        }
    }

    var screen by remember {
        mutableStateOf(
            when {
                token.isBlank() -> "home"
                accountType == "athlete" && forcePasswordChange -> "athlete_password"
                else -> "dashboard"
            }
        )
    }
    var showBetaNotice by remember { mutableStateOf(false) }

    when (screen) {
        "home" -> HomeScreen(
            onTrainer = { screen = "trainer_login" },
            onAthlete = { screen = "athlete_login" }
        )

        "trainer_login" -> {
            lastLoginScreen = "trainer_login"
            LoginScreen(
                context = context,
                subtitle = "Přihlášení pro trenéry",
                description = "Zadejte své trenérské přihlašovací údaje.",
                onBack = { screen = "home" },
                onForgotPassword = { screen = "forgot_password" },
                onLoginSuccess = { name, id, newToken, jsonResp ->
                    trainerName = name
                    trainerId = id
                    token = newToken
                    accountType = "trainer"
                    profilePhoto = jsonResp.optJSONObject("coach")?.optString("photo", "").orEmpty()

                    val parsedAthletes = parseAthletesFromJson(jsonResp)
                    if (parsedAthletes.isNotEmpty()) athletesList = parsedAthletes

                    prefs.edit()
                        .putString(PREF_TOKEN, newToken)
                        .putString(PREF_TYPE, "trainer")
                        .putString(PREF_NAME, name)
                        .putString(PREF_ID, id)
                        .putString(PREF_PHOTO, profilePhoto)
                        .apply()

                    showBetaNotice = true
                    screen = "dashboard"
                }
            )
        }

        "athlete_login" -> {
            lastLoginScreen = "athlete_login"
            LoginScreen(
                context = context,
                subtitle = "Přihlášení pro sportovce",
                description = "Zadejte e-mail a heslo sportovce.",
                accountType = "athlete",
                onBack = { screen = "home" },
                onForgotPassword = { screen = "forgot_password" },
                onLoginSuccess = { name, id, newToken, jsonResp ->
                    trainerName = name
                    trainerId = id
                    token = newToken
                    accountType = "athlete"
                    profilePhoto = jsonResp.optJSONObject("athlete")?.optString("photo", "").orEmpty()
                    forcePasswordChange = jsonResp.optBoolean("force_password_change", false)

                    prefs.edit()
                        .putString(PREF_TOKEN, newToken)
                        .putString(PREF_TYPE, "athlete")
                        .putString(PREF_NAME, name)
                        .putString(PREF_ID, id)
                        .putBoolean(PREF_FORCE_PASSWORD, forcePasswordChange)
                        .putString(PREF_PHOTO, profilePhoto)
                        .apply()

                    if (!forcePasswordChange) showBetaNotice = true
                    screen = if (forcePasswordChange) "athlete_password" else "dashboard"
                }
            )
        }

        "forgot_password" -> ForgotPasswordScreen(context = context, onBack = { screen = lastLoginScreen })

        "athlete_password" -> AthletePasswordScreen(
            context = context,
            token = token,
            onChanged = {
                forcePasswordChange = false
                prefs.edit().putBoolean(PREF_FORCE_PASSWORD, false).apply()
                showBetaNotice = true
                screen = "dashboard"
            },
            onLogout = {
                if (token.isNotBlank()) logoutFromApi(context, token)
                cancelAlertSync(context)
                prefs.edit().clear().apply()
                token = ""
                trainerName = ""
                trainerId = ""
                accountType = "trainer"
                forcePasswordChange = false
                screen = "home"
            }
        )

        "dashboard" -> if (accountType == "athlete") {
            AthleteSection(
                screen = "dashboard",
                name = trainerName,
                photoUrl = profilePhoto,
                token = token,
                context = context,
                onNavigate = { screen = it },
                onLogout = {
                    if (token.isNotBlank()) logoutFromApi(context, token)
                    cancelAlertSync(context)
                    prefs.edit().clear().apply()
                    token = ""
                    trainerName = ""
                    trainerId = ""
                    accountType = "trainer"
                    profilePhoto = ""
                    forcePasswordChange = false
                    screen = "home"
                },
                onPhoto = { url ->
                    profilePhoto = url
                    prefs.edit().putString(PREF_PHOTO, url).apply()
                }
            )
        } else TrainerDashboard(
            trainerName = trainerName,
            photoUrl = profilePhoto,
            accountType = accountType,
            trainings = trainingsList,
            unreadChatCount = totalUnreadChatCount,
            pendingCalendarCount = pendingCalendarCount,
            isLoading = isLoadingData,
            dataError = dataError,
            onRefresh = { refreshApiData() },
            openSessions = openSessions,
            onOpenSession = { sessionId ->
                activeSessionId = sessionId
                screen = "training"
            },
            onNavigate = { screen = it },
            onLogout = {
                if (token.isNotBlank()) logoutFromApi(context, token)
                cancelAlertSync(context)
                prefs.edit().clear().apply()
                token = ""
                trainerName = ""
                trainerId = ""
                accountType = "trainer"
                forcePasswordChange = false
                athletesList = emptyList()
                workoutSetsList = emptyList()
                trainingsList = emptyList()
                conversationsList = emptyList()
                openSessions = emptyList()
                activeSessionId = 0
                screen = "home"
            }
        )

        "athletes" -> AthletesSectionScreen(
            athletes = athletesList,
            token = token,
            context = context,
            unreadChatCount = totalUnreadChatCount,
            pendingCalendarCount = pendingCalendarCount,
            isLoading = isLoadingData,
            onRefresh = { refreshApiData() },
            onNavigate = { screen = it },
            onOpenChatWithAthlete = { athleteId ->
                selectedChatConversationId = athleteId
                screen = "chat"
            }
        )

        "calendar" -> if (accountType == "athlete") {
            AthleteSection(screen = "calendar", name = trainerName, photoUrl = profilePhoto, token = token, context = context, onNavigate = { screen = it }, onLogout = {
                if (token.isNotBlank()) logoutFromApi(context, token)
                cancelAlertSync(context)
                prefs.edit().clear().apply()
                token = ""
                trainerName = ""
                accountType = "trainer"
                profilePhoto = ""
                screen = "home"
            })
        } else CalendarSectionScreen(
            trainings = trainingsList,
            athletes = athletesList,
            venues = venuesList,
            token = token,
            context = context,
            unreadChatCount = totalUnreadChatCount,
            pendingCalendarCount = pendingCalendarCount,
            isLoading = isLoadingData,
            onRefresh = { refreshApiData() },
            onNavigate = { screen = it }
        )

        "chat" -> if (accountType == "athlete") {
            AthleteSection(screen = "chat", name = trainerName, photoUrl = profilePhoto, token = token, context = context, onNavigate = { screen = it }, onLogout = {
                if (token.isNotBlank()) logoutFromApi(context, token)
                cancelAlertSync(context)
                prefs.edit().clear().apply()
                token = ""
                trainerName = ""
                accountType = "trainer"
                profilePhoto = ""
                screen = "home"
            })
        } else ChatSectionScreen(
            initialConversations = conversationsList,
            selectedConversationId = selectedChatConversationId,
            unreadChatCount = totalUnreadChatCount,
            pendingCalendarCount = pendingCalendarCount,
            token = token,
            context = context,
            onNavigate = { screen = it },
            onConversationRead = {
                cancelSystemNotification(context, 2001)
                fetchChatConversationsFromApi(context, token, onSuccess = { convs ->
                    conversationsList = convs
                    prefs.edit().putInt(PREF_LAST_UNREAD, convs.sumOf { it.unreadCount }).apply()
                    if (convs.sumOf { it.unreadCount } == 0) cancelSystemNotification(context, 2001)
                })
            }
        )

        "training" -> TrainingScreen(
            athletes = athletesList,
            workoutSets = workoutSetsList,
            venues = venuesList,
            token = token,
            context = context,
            activeSessionId = activeSessionId,
            openSessions = openSessions,
            unreadChatCount = totalUnreadChatCount,
            pendingCalendarCount = pendingCalendarCount,
            onSessionStarted = { newSessionId ->
                activeSessionId = newSessionId
                refreshApiData()
            },
            onSessionCompleted = {
                activeSessionId = 0
                refreshApiData()
            },
            onBack = { screen = "dashboard" },
            onNavigate = { screen = it }
        )

        "payments", "weight" -> if (accountType == "athlete") {
            AthleteSection(screen = screen, name = trainerName, photoUrl = profilePhoto, token = token, context = context, onNavigate = { screen = it }, onLogout = {
                if (token.isNotBlank()) logoutFromApi(context, token)
                cancelAlertSync(context)
                prefs.edit().clear().apply()
                token = ""
                trainerName = ""
                accountType = "trainer"
                profilePhoto = ""
                screen = "home"
            })
        } else {
            screen = "dashboard"
        }

        else -> { screen = "dashboard" }
    }

    if (showBetaNotice) {
        AlertDialog(
            onDismissRequest = { showBetaNotice = false },
            title = { AppText("Vítejte v aplikaci TrainerApp", color = TrainerAppNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                AppText(
                    "Jedná se o betaverzi a zkušební provoz, prosím hlaste všechny chyby a kontrolujte si stav dat s aplikací na webu https://reservio.online",
                    color = TrainerAppNavy,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showBetaNotice = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
                ) { AppText("Rozumím", fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
private fun HomeScreen(onTrainer: () -> Unit, onAthlete: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MetalHeaderBrush)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.trainerapp_hero),
                contentDescription = "TrainerApp",
                modifier = Modifier.fillMaxWidth().height(190.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(8.dp))
            AppText("TRAINERAPP", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onTrainer,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy),
                shape = RoundedCornerShape(13.dp)
            ) {
                AppText("Přihlášení trenéra", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onAthlete,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                AppText("Přihlášení sportovce", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            AppText("Mobilní aplikace TrainerApp", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun LoginScreen(
    context: Context,
    subtitle: String,
    description: String,
    onBack: () -> Unit,
    accountType: String = "trainer",
    onForgotPassword: () -> Unit,
    onLoginSuccess: (String, String, String, JSONObject) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var rememberLogin by remember { mutableStateOf(true) }
    val autofillManager = LocalAutofillManager.current

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Image(
                painter = painterResource(id = R.drawable.trainerapp_hero),
                contentDescription = "TrainerApp",
                modifier = Modifier.fillMaxWidth().height(210.dp),
                contentScale = ContentScale.Fit
            )
            AppText(subtitle, color = TrainerAppNavy, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            AppText(description, color = TrainerAppGray, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMessage = "" },
                modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username + ContentType.EmailAddress },
                label = { AppText("Uživatelské jméno / E-mail") },
                singleLine = true,
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
                label = { AppText("Heslo") },
                singleLine = true,
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        AppText(if (passwordVisible) "🙈" else "👁", fontSize = 20.sp)
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rememberLogin, onCheckedChange = { rememberLogin = it })
                AppText("Neodhlašovat", color = TrainerAppNavy, fontSize = 15.sp)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val cleanUser = username.trim()
                    if (cleanUser.isEmpty()) errorMessage = "Zadejte uživatelské jméno."
                    else if (password.isEmpty()) errorMessage = "Zadejte heslo."
                    else {
                        loading = true
                        errorMessage = ""
                        loginToApi(username = cleanUser, password = password, context = context, accountType = accountType, remember = rememberLogin,
                            onSuccess = { name, id, newToken, jsonResp ->
                                loading = false
                                autofillManager?.commit()
                                onLoginSuccess(name, id, newToken, jsonResp)
                            },
                            onError = { msg -> loading = false; errorMessage = msg }
                        )
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(25.dp), color = TrainerAppNavy, strokeWidth = 3.dp)
                else AppText("Přihlásit se", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (errorMessage.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE3E3)), shape = RoundedCornerShape(12.dp)) {
                    AppText(errorMessage, color = Color(0xFFB00020), modifier = Modifier.padding(14.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onForgotPassword, enabled = !loading) {
                AppText("Zapomenuté heslo", color = TrainerAppNavy, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                AppText("Zpět")
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ForgotPasswordScreen(context: Context, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MetalHeaderBrush)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.Center) {
            AppText("Obnova hesla", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            AppText("Obnovu hesla dokončíte na webu reservio.online. Mobilní API pro odeslání e-mailu nemá.", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
            Spacer(Modifier.height(25.dp))

            Column(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(20.dp)) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WEB_LOGIN_URL)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
                ) { AppText("Otevřít obnovu na webu", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { AppText("Zpět na přihlášení") }
            }
        }
    }
}

@Composable
private fun TrainerDashboard(
    trainerName: String,
    photoUrl: String,
    accountType: String,
    trainings: List<TrainingItem>,
    unreadChatCount: Int,
    pendingCalendarCount: Int,
    openSessions: List<OpenSession>,
    isLoading: Boolean,
    dataError: String,
    onRefresh: () -> Unit,
    onOpenSession: (Int) -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time) }

    val todayEvents = remember(trainings, todayDateStr) {
        trainings.filter { (it.date == todayDateStr || it.date.isBlank()) && !it.isLocked }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(MetalHeaderBrush).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfilePhoto(photoUrl, trainerName, 42.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    AppText(trainerName.ifBlank { "Uživatel" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    AppText(if (accountType == "athlete") "sportovec" else "trenér", color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp)
                }
                TextButton(onClick = onLogout) { AppText("Odhlásit", color = Color.White, fontWeight = FontWeight.Bold) }
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                val todayCountLabel = when (todayEvents.size) {
                    0 -> "žádná událost"
                    1 -> "1 událost"
                    2, 3, 4 -> "${todayEvents.size} události"
                    else -> "${todayEvents.size} událostí"
                }
                var todayExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = todayEvents.isNotEmpty()) {
                        todayExpanded = !todayExpanded
                    },
                    colors = CardDefaults.cardColors(containerColor = MetalCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppText("📅", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                AppText("Dnešní události", color = TrainerAppNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                AppText(todayCountLabel, color = TrainerAppGray, fontSize = 13.sp)
                            }
                            if (todayEvents.isNotEmpty()) {
                                AppText(if (todayExpanded) "▲" else "▼", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (isLoading) {
                            Spacer(Modifier.height(12.dp))
                            CircularProgressIndicator(color = TrainerAppNavy, modifier = Modifier.size(22.dp))
                        } else if (todayEvents.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            AppText("Na dnešek nemáte naplánované žádné události.", color = TrainerAppGray, fontSize = 13.sp)
                        } else if (todayExpanded) {
                            Spacer(Modifier.height(12.dp))
                            todayEvents.forEach { training ->
                                TodayTrainingCard(training.time, training.athleteName, training.detail) { onNavigate("calendar") }
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }

                if (dataError.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE3E3)), shape = RoundedCornerShape(12.dp)) {
                        AppText(dataError, color = Color(0xFFB00020), modifier = Modifier.padding(14.dp), fontSize = 14.sp)
                    }
                }

                if (openSessions.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    AppText("Rozpracované tréninky", color = TrainerAppNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    openSessions.forEach { session ->
                        OpenSessionCard(session) { onOpenSession(session.id) }
                    }
                }

                Spacer(Modifier.height(22.dp))
                AppText("Rychlý přístup", color = TrainerAppNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardTile("👥", "Sportovci", Modifier.weight(1f)) { onNavigate("athletes") }
                    DashboardTile("📅", "Kalendář", Modifier.weight(1f), badgeCount = pendingCalendarCount) { onNavigate("calendar") }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardTile("💬", "Chat", Modifier.weight(1f), badgeCount = unreadChatCount) { onNavigate("chat") }
                    DashboardTile("🏋️", "Spustit trénink", Modifier.weight(1f)) { onNavigate("training") }
                }
            }

            BottomNavigationBar(current = "dashboard", unreadChatCount = unreadChatCount, pendingCalendarCount = pendingCalendarCount, onNavigate = onNavigate)
        }
    }
}

@Composable
private fun AthletesSectionScreen(
    athletes: List<AthleteItem>,
    token: String,
    context: Context,
    unreadChatCount: Int,
    pendingCalendarCount: Int,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenChatWithAthlete: (String) -> Unit
) {
    var selectedAthleteDetail by remember { mutableStateOf<AthleteFullDetail?>(null) }
    var openSessionId by remember { mutableStateOf<Int?>(null) }
    var isLoadingDetail by remember { mutableStateOf(false) }
    var showAddAthlete by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().background(MetalHeaderBrush).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                AppText("👥", fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    AppText("Sportovci", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    AppText("Seznam registrovaných sportovců z reservio.online", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
                IconButton(onClick = { showAddAthlete = true }) { AppText("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                IconButton(onClick = onRefresh) { AppText("🔄", fontSize = 20.sp) }
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TrainerAppNavy)
                    }
                } else if (athletes.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MetalCard), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            AppText("👥", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            AppText("Na účtu nebyli nalezeni žádní registrovaní sportovci.", color = TrainerAppNavy, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)) {
                                AppText("Obnovit data z vícero zdrojů", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    athletes.forEach { athlete ->
                        AthleteCard(
                            athlete = athlete,
                            onClick = {
                                isLoadingDetail = true
                                fetchAthleteFullDetailApi(context, token, athlete.id,
                                    onSuccess = { fullDetail ->
                                        isLoadingDetail = false
                                        val fallback = recentTrainingsOf(athlete)
                                        selectedAthleteDetail = if (fullDetail.trainings.isNotEmpty()) fullDetail else fullDetail.copy(trainings = fallback)
                                    },
                                    onError = {
                                        isLoadingDetail = false
                                        selectedAthleteDetail = AthleteFullDetail(
                                            athlete.id, athlete.name, athlete.email, athlete.phone,
                                            trainings = recentTrainingsOf(athlete)
                                        )
                                    }
                                )
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            BottomNavigationBar(current = "athletes", unreadChatCount = unreadChatCount, pendingCalendarCount = pendingCalendarCount, onNavigate = onNavigate)
        }
    }

    if (selectedAthleteDetail != null) {
        AthleteDetailDialog(
            detail = selectedAthleteDetail!!,
            onDismiss = { selectedAthleteDetail = null },
            onOpenChat = { athleteId ->
                selectedAthleteDetail = null
                onOpenChatWithAthlete(athleteId)
            },
            onOpenTraining = { sessionId ->
                selectedAthleteDetail = null
                openSessionId = sessionId
            }
        )
    }

    if (openSessionId != null) {
        PastTrainingDialog(
            sessionId = openSessionId!!,
            token = token,
            context = context,
            onDismiss = { openSessionId = null }
        )
    }

    if (showAddAthlete) {
        AddAthleteDialog(
            token = token,
            context = context,
            onDismiss = { showAddAthlete = false },
            onCreated = {
                showAddAthlete = false
                onRefresh()
            }
        )
    }
}

private fun recentTrainingsOf(athlete: AthleteItem): List<TrainingItem> {
    return athlete.recentTrainings.map { training ->
        TrainingItem(id = training.id, time = training.date, athleteName = athlete.name, detail = training.setName)
    }
}

@Composable
private fun AthleteCard(athlete: AthleteItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MetalCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
                ProfilePhoto(athlete.photoUrl, athlete.name, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    AppText(athlete.name, color = TrainerAppNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (athlete.email.isNotBlank()) AppText(athlete.email, color = TrainerAppGray, fontSize = 13.sp)
                }
                AppText("▶", color = TrainerAppGray, fontSize = 14.sp)
            }
            if (athlete.phone.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AppText("📞 ${athlete.phone}", color = TrainerAppNavy, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AthleteDetailDialog(
    detail: AthleteFullDetail,
    onDismiss: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenTraining: (Int) -> Unit
) {
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhoto(detail.photoUrl, detail.name, 36.dp)
                Spacer(Modifier.width(10.dp))
                AppText(detail.name, color = TrainerAppNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (detail.birthDate.isNotBlank() || detail.age != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (detail.birthDate.isNotBlank()) AppText("Narození: ${detail.birthDate}", fontSize = 13.sp, color = TrainerAppGray)
                        if (detail.age != null) AppText("Věk: ${detail.age} let", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (detail.email.isNotBlank()) AppText("📧 ${detail.email}", fontSize = 13.sp, color = TrainerAppNavy)
                if (detail.phone.isNotBlank()) AppText("📞 ${detail.phone}", fontSize = 13.sp, color = TrainerAppNavy)

                Spacer(Modifier.height(16.dp))
                AppText("Poslední váha", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (detail.weightLogs.isEmpty()) {
                    AppText("Žádné záznamy o váze.", fontSize = 12.sp, color = TrainerAppGray)
                } else {
                    detail.weightLogs.take(1).forEach { log ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            AppText(log.date, fontSize = 12.sp, color = TrainerAppGray)
                            AppText("${log.weightKg} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                AppText("Poslední tréninky", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (detail.trainings.isEmpty()) {
                    AppText("Žádná historie tréninků.", fontSize = 12.sp, color = TrainerAppGray)
                } else {
                    detail.trainings.take(3).forEach { tr ->
                        val sessionId = tr.id.toIntOrNull()
                        val canOpen = sessionId != null && sessionId in 1..99999
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(enabled = canOpen) { if (sessionId != null) onOpenTraining(sessionId) },
                            colors = CardDefaults.cardColors(containerColor = TrainerAppLight)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                AppText(formatTrainingDate(tr.time), fontSize = 12.sp, color = TrainerAppGray)
                                AppText(tr.detail, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onOpenChat(detail.id) }, colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)) {
                AppText("💬 Otevřít chat", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { AppText("Zavřít") }
        }
    )
}

private fun dateStringToUtcMillis(date: String): Long? {
    val parts = date.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    cal.set(year, month - 1, day, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun utcMillisToDateString(millis: Long): String {
    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = millis
    return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

private fun formatTrainingDate(raw: String): String {
    val datePart = raw.trim().take(10)
    val parts = datePart.split("-")
    if (parts.size != 3) return raw.ifBlank { "Bez data" }
    val day = parts[2].toIntOrNull() ?: return raw
    val month = parts[1].toIntOrNull() ?: return raw
    return "$day. $month. ${parts[0]}"
}

@Composable
private fun PastTrainingDialog(
    sessionId: Int,
    token: String,
    context: Context,
    onDismiss: () -> Unit
) {
    var sessionData by remember { mutableStateOf<JSONObject?>(null) }
    var exercises by remember { mutableStateOf<List<SessionExerciseItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf("") }

    LaunchedEffect(sessionId) {
        fetchActiveSessionDetailApi(
            context, token, sessionId,
            onSuccess = { sess, list ->
                isLoading = false
                sessionData = sess
                exercises = list
            },
            onError = {
                isLoading = false
                errorText = it
            }
        )
    }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Column {
                AppText(sessionData?.optString("workout_set_name", "Trénink") ?: "Trénink", color = TrainerAppNavy, fontWeight = FontWeight.Bold)
                val whenText = sessionData?.optString("completed_at", "")?.ifBlank { sessionData?.optString("started_at", "") ?: "" } ?: ""
                if (whenText.isNotBlank()) AppText(formatTrainingDate(whenText), color = TrainerAppGray, fontSize = 13.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (isLoading) {
                    CircularProgressIndicator(color = TrainerAppNavy)
                } else if (errorText.isNotBlank()) {
                    AppText(errorText, color = Color(0xFFB00020), fontSize = 14.sp)
                } else if (exercises.isEmpty()) {
                    AppText("V tomhle tréninku nejsou uložené série.", color = TrainerAppGray, fontSize = 14.sp)
                } else {
                    exercises.forEach { exercise ->
                        AppText("${exercise.exerciseOrder}. ${exercise.exerciseName}", color = TrainerAppNavy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (exercise.series.isEmpty()) {
                            AppText("Bez sérií", color = TrainerAppGray, fontSize = 13.sp)
                        } else {
                            exercise.series.forEach { series ->
                                AppText(
                                    "Série ${series.seriesOrder}: ${series.weight} kg × ${series.reps} op",
                                    color = TrainerAppGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { AppText("Zavřít") } }
    )
}

private fun getWeekDays(weekOffset: Int): List<Pair<String, String>> {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    val dayNames = listOf("po", "út", "st", "čt", "pá", "so", "ne")
    val result = mutableListOf<Pair<String, String>>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    for (i in 0..6) {
        val dayNum = cal.get(Calendar.DAY_OF_MONTH)
        val dateStr = sdf.format(cal.time)
        val label = "${dayNames[i]}\n$dayNum"
        result.add(Pair(label, dateStr))
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return result
}

private fun getWeekHeaderLabel(weekOffset: Int): String {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    val csLocale = Locale.forLanguageTag("cs-CZ")
    val sdfMonth = SimpleDateFormat("LLLL yyyy", csLocale)
    return sdfMonth.format(cal.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(csLocale) else it.toString() }
}

@Composable
private fun CalendarSectionScreen(
    trainings: List<TrainingItem>,
    athletes: List<AthleteItem>,
    venues: List<VenueItem>,
    token: String,
    context: Context,
    unreadChatCount: Int,
    pendingCalendarCount: Int,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    val weekDays = remember(weekOffset) { getWeekDays(weekOffset) }
    val weekHeader = remember(weekOffset) { getWeekHeaderLabel(weekOffset) }
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time) }

    val todayIndexInWeek = remember(weekDays, todayDateStr) {
        val idx = weekDays.indexOfFirst { it.second == todayDateStr }
        if (idx >= 0) idx else 0
    }

    var selectedDayIndex by remember { mutableIntStateOf(todayIndexInWeek) }

    LaunchedEffect(weekOffset) {
        val idx = weekDays.indexOfFirst { it.second == todayDateStr }
        selectedDayIndex = if (idx >= 0) idx else 0
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var createDate by remember { mutableStateOf("") }
    var createHour by remember { mutableIntStateOf(9) }
    var selectedEventForAction by remember { mutableStateOf<TrainingItem?>(null) }
    var selectedActionHour by remember { mutableIntStateOf(0) }

    val hours = (5..21).toList()

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(weekHeader, color = TrainerAppNavy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).clickable { weekOffset-- },
                            contentAlignment = Alignment.Center
                        ) { AppText("‹", color = TrainerAppNavy, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).clickable { weekOffset++ },
                            contentAlignment = Alignment.Center
                        ) { AppText("›", color = TrainerAppNavy, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MetalGoldBrush)
                        .clickable {
                            createDate = weekDays[selectedDayIndex].second
                            createHour = 9
                            showCreateDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) { AppText("+", color = TrainerAppNavy, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }

            val pendingBlink = rememberInfiniteTransition(label = "pendingDot")
            val pendingAlpha by pendingBlink.animateFloat(
                initialValue = 1f,
                targetValue = 0.15f,
                animationSpec = infiniteRepeatable(tween(durationMillis = 520), RepeatMode.Reverse),
                label = "pendingDotAlpha"
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEachIndexed { index, (dayLabel, dateStr) ->
                    val isSelected = index == selectedDayIndex
                    val isToday = dateStr == todayDateStr
                    val needsAttention = trainings.any {
                        it.date == dateStr && !it.isLocked && (it.approvalStatus.equals("pending", true) || it.awaitingReschedule)
                    }
                    val hasEvents = trainings.any { it.date == dateStr && !it.isLocked }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSelected -> TrainerAppYellow
                                        isToday -> Color(0xFFF3E6C4)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { selectedDayIndex = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = dayLabel,
                                color = TrainerAppNavy,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (needsAttention || hasEvents) {
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(if (needsAttention) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (needsAttention) Color(0xFFD32F2F).copy(alpha = pendingAlpha)
                                        else if (isSelected) TrainerAppNavy
                                        else TrainerAppYellow
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                hours.forEach { hour ->
                    val hourStr = String.format(Locale.getDefault(), "%02d:00", hour)
                    val hourEvents = trainings.filter { tr ->
                        if (tr.date != weekDays[selectedDayIndex].second) return@filter false
                        if (tr.startHour == tr.endHour) {
                            tr.startHour == hour
                        } else {
                            hour >= tr.startHour && hour < tr.endHour
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().height(60.dp).border(0.5.dp, Color(0xFFE0DCD3)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.width(55.dp).padding(start = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            AppText(hourStr, fontSize = 12.sp, color = TrainerAppGray)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color(0xFFF2EFE8))
                                .border(0.5.dp, Color(0xFFE0DCD3))
                                .clickable(enabled = hourEvents.isEmpty()) {
                                    createDate = weekDays[selectedDayIndex].second
                                    createHour = hour
                                    showCreateDialog = true
                                }
                        ) {
                            if (hourEvents.isNotEmpty()) {
                                hourEvents.forEach { ev ->
                                    CalendarSlotEvent(ev) {
                                        selectedEventForAction = ev
                                        selectedActionHour = hour
                                    }
                                }
                            }
                        }
                    }
                }
            }

            BottomNavigationBar(current = "calendar", unreadChatCount = unreadChatCount, pendingCalendarCount = pendingCalendarCount, onNavigate = onNavigate)
        }
    }

    if (showCreateDialog) {
        CreateCalendarEventDialog(
            athletes = athletes,
            venues = venues,
            token = token,
            context = context,
            initialDate = createDate.ifBlank { weekDays[selectedDayIndex].second },
            initialStartHour = createHour,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                onRefresh()
            }
        )
    }

    if (selectedEventForAction != null) {
        CalendarEventActionDialog(
            event = selectedEventForAction!!,
            athletes = athletes,
            actionHour = selectedActionHour,
            token = token,
            context = context,
            onDismiss = { selectedEventForAction = null },
            onActionDone = {
                selectedEventForAction = null
                onRefresh()
            }
        )
    }
}

@Composable
private fun ChatSectionScreen(
    initialConversations: List<ChatConversation>,
    selectedConversationId: String?,
    unreadChatCount: Int,
    pendingCalendarCount: Int,
    token: String,
    context: Context,
    onNavigate: (String) -> Unit,
    onConversationRead: () -> Unit
) {
    var conversations by remember(initialConversations) { mutableStateOf(initialConversations) }
    var selectedConversation by remember { mutableStateOf<ChatConversation?>(null) }

    LaunchedEffect(token) {
        if (token.isNotBlank()) {
            fetchChatConversationsFromApi(context, token, onSuccess = { convs ->
                if (convs.isNotEmpty()) {
                    conversations = convs
                    if (selectedConversationId != null) {
                        selectedConversation = convs.firstOrNull { it.id == selectedConversationId }
                    }
                }
            })
        }
    }

    LaunchedEffect(selectedConversationId, conversations) {
        if (selectedConversationId != null && conversations.isNotEmpty()) {
            val found = conversations.firstOrNull { it.id == selectedConversationId }
            if (found != null) selectedConversation = found
        }
    }

    if (selectedConversation != null) {
        ChatDetailScreen(
            conversation = selectedConversation!!,
            token = token,
            context = context,
            unreadChatCount = unreadChatCount,
            pendingCalendarCount = pendingCalendarCount,
            onBack = { selectedConversation = null },
            onNavigate = onNavigate,
            onConversationRead = onConversationRead
        )
    } else {
        ChatListScreen(
            conversations = conversations,
            unreadChatCount = unreadChatCount,
            pendingCalendarCount = pendingCalendarCount,
            onSelectConversation = { selectedConversation = it },
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun TrainingScreen(
    athletes: List<AthleteItem>,
    workoutSets: List<WorkoutSetItem>,
    venues: List<VenueItem>,
    token: String,
    context: Context,
    activeSessionId: Int,
    openSessions: List<OpenSession>,
    unreadChatCount: Int,
    pendingCalendarCount: Int,
    onSessionStarted: (Int) -> Unit,
    onSessionCompleted: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var selectedAthlete by remember { mutableStateOf<AthleteItem?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var pendingSetToStart by remember { mutableStateOf<WorkoutSetItem?>(null) }
    var knownOpenSessions by remember(openSessions) { mutableStateOf(openSessions) }
    var conflictSessionId by remember { mutableIntStateOf(0) }
    var conflictMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        fetchOpenSessions(context, token) { knownOpenSessions = it }
    }

    if (activeSessionId > 0) {
        ActiveSessionView(
            sessionId = activeSessionId,
            venues = venues,
            token = token,
            context = context,
            onCompleted = onSessionCompleted,
            onBack = onBack
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().background(MetalHeaderBrush).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                AppText("🏋️", fontSize = 30.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    AppText("Spustit trénink", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    AppText("Vyberte sportovce a tréninkovou sadu", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp)) {
                if (knownOpenSessions.isNotEmpty()) {
                    AppText("Rozpracované tréninky", color = TrainerAppNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    AppText("Než půjde spustit nový trénink, tenhle je potřeba dokončit.", color = TrainerAppGray, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    knownOpenSessions.forEach { session ->
                        OpenSessionCard(session) { onSessionStarted(session.id) }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                AppText("Vyberte sportovce", color = TrainerAppNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                if (athletes.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MetalCard), shape = RoundedCornerShape(12.dp)) {
                        AppText("Žádní sportovci nenačteni. Nejprve obnovte seznam sportovců.", color = TrainerAppGray, modifier = Modifier.padding(14.dp), fontSize = 14.sp)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true }, colors = CardDefaults.cardColors(containerColor = MetalCard), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppText("👤", fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                AppText(selectedAthlete?.name ?: "Vyberte", color = if (selectedAthlete == null) TrainerAppGray else TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                AppText("▼", color = TrainerAppGray, fontSize = 12.sp)
                            }
                        }
                        DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            athletes.forEach { ath ->
                                DropdownMenuItem(text = { AppText(ath.name, fontWeight = FontWeight.Bold) }, onClick = { selectedAthlete = ath; dropdownExpanded = false })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                AppText("Tréninkové sady", color = TrainerAppNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))

                if (statusMessage.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isError) Color(0xFFFFE3E3) else Color(0xFFE6F4EA)), shape = RoundedCornerShape(12.dp)) {
                        AppText(statusMessage, color = if (isError) Color(0xFFB00020) else Color(0xFF137333), modifier = Modifier.padding(14.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                }

                if (isStarting) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TrainerAppNavy)
                    }
                } else if (workoutSets.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MetalCard), shape = RoundedCornerShape(12.dp)) {
                        AppText("Na účtu nejsou žádné tréninkové sady.", color = TrainerAppGray, modifier = Modifier.padding(14.dp), fontSize = 14.sp)
                    }
                } else {
                    workoutSets.forEach { set ->
                        TrainingSetCard(set) {
                            if (selectedAthlete == null) {
                                isError = true
                                statusMessage = "Vyberte sportovce, pro kterého chcete trénink spustit."
                            } else {
                                pendingSetToStart = set
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { AppText("Zpět") }
            }

            BottomNavigationBar(current = "", unreadChatCount = unreadChatCount, pendingCalendarCount = pendingCalendarCount, onNavigate = onNavigate)
        }
    }

    if (pendingSetToStart != null) {
        val set = pendingSetToStart!!
        AlertDialog(
        modifier = Modifier.imePadding(),
            onDismissRequest = { pendingSetToStart = null },
            title = { AppText("Spustit trénink?", color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
            text = { AppText("Opravdu chcete spustit trénink '${set.name}' pro sportovce ${selectedAthlete?.name}?", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        val setToRun = pendingSetToStart!!
                        pendingSetToStart = null
                        val athId = selectedAthlete!!.id.toIntOrNull() ?: 0
                        isStarting = true
                        statusMessage = ""
                        startTrainingApi(context, token, athId, setToRun.id,
                            onSuccess = { json ->
                                isStarting = false
                                isError = false
                                val sessObj = json.optJSONObject("session")
                                val newSessId = sessObj?.optInt("id", 0) ?: 0
                                if (newSessId > 0) {
                                    onSessionStarted(newSessId)
                                } else {
                                    statusMessage = "Trénink byl spuštěn!"
                                }
                            },
                            onError = { err -> isStarting = false; isError = true; statusMessage = err },
                            onExistingSession = { existingId, message ->
                                isStarting = false
                                isError = false
                                statusMessage = ""
                                conflictSessionId = existingId
                                conflictMessage = message
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
                ) {
                    AppText("Spustit trénink", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingSetToStart = null }) { AppText("Zrušit") } }
        )
    }

    if (conflictSessionId > 0) {
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { conflictSessionId = 0 },
            title = { AppText("Rozpracovaný trénink", color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
            text = { AppText(conflictMessage.ifBlank { "Sportovec už má rozpracovaný trénink. Otevřete ho a dokončete, nebo v něm pokračujte." }, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        val sessionId = conflictSessionId
                        conflictSessionId = 0
                        onSessionStarted(sessionId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
                ) {
                    AppText("Pokračovat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { conflictSessionId = 0 }) { AppText("Zavřít") } }
        )
    }
}

@Composable
private fun OpenSessionCard(session: OpenSession, onContinue: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onContinue),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                AppText(session.athleteName, color = TrainerAppNavy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                val whenText = formatTrainingDate(session.startedAt)
                AppText("${session.setName} · $whenText", color = TrainerAppGray, fontSize = 12.sp)
            }
            AppText("Pokračovat", color = TrainerAppNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActiveSessionView(
    sessionId: Int,
    venues: List<VenueItem>,
    token: String,
    context: Context,
    onCompleted: () -> Unit,
    onBack: () -> Unit
) {
    var sessionData by remember { mutableStateOf<JSONObject?>(null) }
    var exercises by remember { mutableStateOf<List<SessionExerciseItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var sessionError by remember { mutableStateOf("") }
    var showCompleteDialog by remember { mutableStateOf(false) }

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var allAvailableExercises by remember { mutableStateOf<List<ExerciseItem>>(emptyList()) }
    var isAddingExercise by remember { mutableStateOf(false) }

    fun loadSession() {
        isLoading = true
        sessionError = ""
        fetchActiveSessionDetailApi(context, token, sessionId,
            onSuccess = { sessObj, exList ->
                isLoading = false
                sessionData = sessObj
                exercises = exList
            },
            onError = { message ->
                isLoading = false
                sessionError = message
            }
        )
    }

    LaunchedEffect(sessionId) { loadSession() }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().background(MetalHeaderBrush).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    AppText("Aktivní trénink #${sessionId}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (sessionData != null) {
                        val athName = sessionData!!.optString("athlete_name", "")
                        val setName = sessionData!!.optString("workout_set_name", "")
                        AppText("$athName • $setName", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        if (sessionData!!.has("athlete_weight_kg") && !sessionData!!.isNull("athlete_weight_kg")) {
                            val weight = sessionData!!.optDouble("athlete_weight_kg")
                            val measured = sessionData!!.optString("athlete_weight_at", "").take(10)
                            val whenText = if (measured.isNotBlank()) " (${formatTrainingDate(measured)})" else ""
                            AppText("Poslední váha: ${formatKg(weight.toFloat())} kg$whenText", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                        }
                    }
                }
                Button(onClick = { showCompleteDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)) {
                    AppText("Ukončit", fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TrainerAppNavy)
                    }
                } else if (sessionError.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE3E3))) {
                        AppText(sessionError, color = Color(0xFFB00020), modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                    }
                } else if (exercises.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MetalCard)) {
                        AppText("Žádné cviky v tréninku.", modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                    }
                } else {
                    exercises.forEach { ex ->
                        ActiveExerciseCard(ex = ex, sessionId = sessionId, token = token, context = context, onSeriesSaved = { loadSession() })
                        Spacer(Modifier.height(12.dp))
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        showAddExerciseDialog = true
                        if (allAvailableExercises.isEmpty()) {
                            fetchExercisesApi(context, token, onSuccess = { result ->
                                allAvailableExercises = result
                            }, onError = { sessionError = it })
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppNavy, contentColor = Color.White)
                ) {
                    AppText("➕ Přidat cvik", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showAddExerciseDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredExercises = allAvailableExercises.filter { it.name.contains(searchQuery, ignoreCase = true) }

        AlertDialog(
        modifier = Modifier.imePadding(),
            onDismissRequest = { showAddExerciseDialog = false },
            title = { AppText("Přidat cvik", color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { AppText("Hledat cvik") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    if (allAvailableExercises.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().height(250.dp).verticalScroll(rememberScrollState())) {
                            filteredExercises.forEach { ex ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                        if (!isAddingExercise) {
                                            isAddingExercise = true
                                            addExerciseToSessionApi(
                                                context, token, sessionId, ex.id,
                                                onSuccess = {
                                                    isAddingExercise = false
                                                    showAddExerciseDialog = false
                                                    loadSession()
                                                },
                                                onError = { isAddingExercise = false }
                                            )
                                        }
                                    },
                                    colors = CardDefaults.cardColors(containerColor = TrainerAppLight)
                                ) {
                                    AppText(ex.name, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) {
                    AppText("Zavřít")
                }
            }
        )
    }

    if (showCompleteDialog) {
        CompleteTrainingDialog(
            sessionId = sessionId,
            venues = venues,
            token = token,
            context = context,
            onDismiss = { showCompleteDialog = false },
            onCompleted = {
                showCompleteDialog = false
                onCompleted()
            }
        )
    }
}

@Composable
private fun ActiveExerciseCard(
    ex: SessionExerciseItem,
    sessionId: Int,
    token: String,
    context: Context,
    onSeriesSaved: () -> Unit
) {
    var weightInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val compactField = TextStyle(fontSize = 14.sp, color = TrainerAppNavy)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MetalCard), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            AppText("${ex.exerciseOrder}. ${ex.exerciseName}", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (ex.previousSeries.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                AppText(
                    "Minule${if (ex.previousLabel.isNotBlank()) " · ${ex.previousLabel}" else ""}",
                    fontSize = 11.sp,
                    color = TrainerAppGray
                )
                AppText(
                    ex.previousSeries.joinToString("   ") { "${it.seriesOrder}. ${formatKg(it.weight)} kg × ${it.reps}" },
                    fontSize = 12.sp,
                    color = TrainerAppNavy
                )
            }
            if (ex.series.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                AppText(
                    "Teď: " + ex.series.joinToString("   ") { "${it.seriesOrder}. ${formatKg(it.weight)} kg × ${it.reps}" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrainerAppNavy
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    placeholder = { AppText("kg", fontSize = 12.sp) },
                    textStyle = compactField,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    placeholder = { AppText("opak.", fontSize = 12.sp) },
                    textStyle = compactField,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        val w = weightInput.replace(',', '.').toFloatOrNull() ?: 0f
                        val r = repsInput.toIntOrNull() ?: 0
                        if (r > 0 || w > 0f) {
                            isSaving = true
                            saveSeriesApi(context, token, sessionId, ex.exerciseId, w, r,
                                onSuccess = { isSaving = false; weightInput = ""; repsInput = ""; onSeriesSaved() },
                                onError = { isSaving = false }
                            )
                        }
                    },
                    enabled = !isSaving,
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
                ) {
                    AppText(if (isSaving) "…" else "Přidat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CompleteTrainingDialog(
    sessionId: Int,
    venues: List<VenueItem>,
    token: String,
    context: Context,
    onDismiss: () -> Unit,
    onCompleted: () -> Unit
) {
    var selectedVenue by remember { mutableStateOf<VenueItem?>(venues.firstOrNull()) }
    var venueDropdown by remember { mutableStateOf(false) }
    var customLocation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { AppText("Ukončit a uložit trénink", color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                AppText("Místo konání:", fontSize = 12.sp, color = TrainerAppGray)
                Box {
                    Card(modifier = Modifier.fillMaxWidth().clickable { venueDropdown = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                        AppText(selectedVenue?.name ?: "Vlastní místo...", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = venueDropdown, onDismissRequest = { venueDropdown = false }) {
                        venues.forEach { v ->
                            DropdownMenuItem(text = { AppText(v.name) }, onClick = { selectedVenue = v; venueDropdown = false })
                        }
                        DropdownMenuItem(text = { AppText("➕ Vlastní místo") }, onClick = { selectedVenue = null; venueDropdown = false })
                    }
                }
                if (selectedVenue == null) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = customLocation, onValueChange = { customLocation = it }, label = { AppText("Zadejte vlastní místo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { AppText("Poznámky k tréninku") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    val finalLoc = if (selectedVenue != null) selectedVenue!!.name else customLocation
                    completeActiveSessionApi(context, token, sessionId, finalLoc, notes,
                        onSuccess = { isSubmitting = false; onCompleted() },
                        onError = { isSubmitting = false }
                    )
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
            ) {
                AppText("Uložit a dokončit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AppText("Zrušit") } }
    )
}

@Composable
private fun TrainingSetCard(set: WorkoutSetItem, onClick: () -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MetalCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(TrainerAppYellow), contentAlignment = Alignment.Center) {
                    AppText("▶", color = TrainerAppNavy, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    AppText(set.name, color = TrainerAppNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (set.isGlobal) {
                        AppText("Globální sada", color = TrainerAppGray, fontSize = 12.sp)
                    }
                }
                if (set.isGlobal || set.description.isNotBlank()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        AppText(if (expanded) "▲" else "▼", color = TrainerAppNavy, fontSize = 14.sp)
                    }
                }
            }

            if (expanded && set.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AppText(set.description, color = TrainerAppGray, fontSize = 13.sp)
            }
        }
    }
}

private data class HttpResult(val code: Int, val body: String)

private fun HttpResult.jsonOrNull(): JSONObject? {
    if (body.isBlank()) return null
    return try {
        JSONObject(body)
    } catch (_: Exception) {
        null
    }
}

private fun apiErrorText(result: HttpResult, fallback: String): String {
    val error = result.jsonOrNull()?.optString("error", "").orEmpty()
    if (error.isNotBlank()) return error
    return if (result.code > 0) "$fallback (HTTP ${result.code})" else fallback
}

private fun friendlyNetworkError(e: Exception): String = when {
    e is UnknownHostException || e.cause is UnknownHostException ->
        "Nelze se připojit k serveru reservio.online."
    e is SocketTimeoutException || e.cause is SocketTimeoutException ->
        "Vypršel časový limit připojení k serveru."
    else -> "Chyba připojení: ${e.message ?: "neznámá"}"
}

private fun runInBackground(block: () -> Unit) {
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        try {
            block()
        } finally {
            executor.shutdown()
        }
    }
}

private fun httpRequest(
    context: Context,
    path: String,
    method: String = "GET",
    token: String = "",
    jsonBody: JSONObject? = null,
): HttpResult {
    val connection = (URL("${resolvedApiBase(context)}/$path").openConnection() as HttpURLConnection)
    try {
        connection.requestMethod = method
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/json")
        if (token.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        if (jsonBody != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
                it.write(jsonBody.toString())
                it.flush()
            }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = if (stream != null) {
            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        } else {
            ""
        }
        return HttpResult(code, body)
    } finally {
        connection.disconnect()
    }
}

private fun tokenQuery(token: String): String = URLEncoder.encode(token, "UTF-8")

private fun fetchUserDataFromApi(context: Context, token: String, onSuccess: (String, String) -> Unit, onError: (String) -> Unit) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val result = httpRequest(context, "me.php?token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null) {
                runOnMain { onError(apiErrorText(result, "Nepodařilo se načíst profil.")) }
                return@runInBackground
            }
            val coachObj = json.optJSONObject("coach") ?: json.optJSONObject("user")
            val name = coachObj?.optString("name", "") ?: json.optString("name", "")
            val photo = coachObj?.optString("photo", "") ?: ""
            runOnMain { onSuccess(name, photo) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun fetchAthletesFromApi(context: Context, token: String, onSuccess: (List<AthleteItem>) -> Unit, onError: (String) -> Unit) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val result = httpRequest(context, "athletes.php?token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null) {
                runOnMain { onError(apiErrorText(result, "Chyba při načítání sportovců.")) }
                return@runInBackground
            }
            if (json.has("success") && !json.optBoolean("success", false)) {
                runOnMain { onError(json.optString("error", "Chyba při načítání sportovců.")) }
                return@runInBackground
            }
            runOnMain { onSuccess(parseAthletesFromJson(json)) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun fetchAthleteFullDetailApi(context: Context, token: String, athleteId: String, onSuccess: (AthleteFullDetail) -> Unit, onError: (String) -> Unit) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val result = httpRequest(context, "athlete.php?id=$athleteId&token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null) {
                runOnMain { onError(apiErrorText(result, "Detail sportovce se nepodařilo načíst.")) }
                return@runInBackground
            }
            val athObj = json.optJSONObject("athlete")
            val name = athObj?.optString("full_name", "")?.takeIf { it.isNotBlank() }
                ?: ((athObj?.optString("first_name", "") ?: "") + " " + (athObj?.optString("last_name", "") ?: "")).trim()
            val email = athObj?.optString("email", "") ?: ""
            val phone = athObj?.optString("phone", athObj?.optString("phone_contact", "") ?: "") ?: ""
            val birthDate = athObj?.optString("birth_date", "") ?: ""
            val age = if (athObj?.has("age") == true && !athObj.isNull("age")) athObj.optInt("age") else null
            val photoUrl = athObj?.optString("photo", athObj.optString("photo_url", athObj.optString("avatar", ""))) ?: ""

            val trainArr = json.optJSONArray("trainings")
            val trainings = mutableListOf<TrainingItem>()
            if (trainArr != null) {
                for (i in 0 until trainArr.length()) {
                    val t = trainArr.optJSONObject(i) ?: continue
                    trainings.add(
                        TrainingItem(
                            id = jsonId(t),
                            time = t.optString("started_at", ""),
                            athleteName = name,
                            detail = t.optString("set_name", "Trénink")
                        )
                    )
                }
            }

            val weightArr = json.optJSONArray("weight_logs")
            val weightLogs = mutableListOf<WeightLogItem>()
            if (weightArr != null) {
                for (i in 0 until weightArr.length()) {
                    val w = weightArr.optJSONObject(i) ?: continue
                    weightLogs.add(WeightLogItem(w.optInt("id", 0), w.optString("measured_at", ""), w.optDouble("weight_kg", 0.0).toFloat()))
                }
            }

            runOnMain { onSuccess(AthleteFullDetail(athleteId, name.trim(), email, phone, birthDate, age, photoUrl, trainings, weightLogs)) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun createAthleteApi(
    context: Context,
    token: String,
    firstName: String,
    lastName: String,
    birthDate: String,
    phone: String,
    email: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                put("action", "create")
                put("first_name", firstName)
                put("last_name", lastName)
                put("birth_date", birthDate)
                put("phone", phone)
                put("email", email)
            }
            val result = httpRequest(context, "athletes.php?action=create&token=${tokenQuery(token)}", "POST", token, jsonBody)
            val json = result.jsonOrNull()
            if (result.code in 200..299 && json?.optBoolean("success", false) == true) {
                runOnMain { onSuccess() }
            } else {
                runOnMain { onError(apiErrorText(result, "Sportovce se nepodařilo uložit.")) }
            }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

@Composable
private fun AddAthleteDialog(
    token: String,
    context: Context,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { AppText("Nový sportovec", color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { AppText("Jméno") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { AppText("Příjmení") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                    Column(Modifier.padding(12.dp)) {
                        AppText("Datum narození", fontSize = 12.sp, color = TrainerAppGray)
                        AppText(if (birthDate.isBlank()) "Vybrat" else formatTrainingDate(birthDate), fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { AppText("Telefon") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { AppText("E-mail") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                if (errorMsg.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    AppText(errorMsg, color = Color(0xFFB00020), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (firstName.isBlank() || lastName.isBlank() || birthDate.isBlank()) {
                        errorMsg = "Vyplňte jméno, příjmení a datum narození."
                        return@Button
                    }
                    isSaving = true
                    errorMsg = ""
                    createAthleteApi(context, token, firstName.trim(), lastName.trim(), birthDate, phone.trim(), email.trim(),
                        onSuccess = { isSaving = false; onCreated() },
                        onError = { message -> isSaving = false; errorMsg = message }
                    )
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
            ) { AppText(if (isSaving) "Ukládám…" else "Uložit", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AppText("Zrušit") } }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateStringToUtcMillis(birthDate.ifBlank { "2000-01-01" }))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { birthDate = utcMillisToDateString(it) }
                    showDatePicker = false
                }) { AppText("Vybrat", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { AppText("Zrušit") } }
        ) { DatePicker(state = pickerState) }
    }
}

private fun fetchWorkoutSetsFromApi(context: Context, token: String, onSuccess: (List<WorkoutSetItem>) -> Unit, onError: (String) -> Unit) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val result = httpRequest(context, "workout_sets.php?token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null) {
                runOnMain { onError(apiErrorText(result, "Tréninkové sady se nepodařilo načíst.")) }
                return@runInBackground
            }
            val array = json.optJSONArray("workout_sets")
            val sets = mutableListOf<WorkoutSetItem>()
            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val isGlob = obj.optBoolean("is_global", false) || obj.optInt("is_global", 0) == 1
                    sets.add(WorkoutSetItem(obj.optInt("id", 0), obj.optString("name", "Tréninková sada"), obj.optString("description", ""), obj.optInt("exercise_count", 0), obj.optBoolean("is_active", true), isGlobal = isGlob))
                }
            }
            runOnMain { onSuccess(sets) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun fetchVenuesFromApi(context: Context, token: String, onSuccess: (List<VenueItem>) -> Unit, onError: (String) -> Unit = {}) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val result = httpRequest(context, "venues.php?token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null) {
                runOnMain { onError(apiErrorText(result, "Sportoviště se nepodařilo načíst.")) }
                return@runInBackground
            }
            val array = json.optJSONArray("venues")
            val items = mutableListOf<VenueItem>()
            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    items.add(VenueItem(obj.optInt("id", 0), obj.optString("name", "Sportoviště"), obj.optString("address", "")))
                }
            }
            runOnMain { onSuccess(items) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private data class EventColors(val background: Color, val text: Color)

private fun webPalette(key: String): EventColors = when (key) {
    "blue" -> EventColors(Color(0xFF0EA5E9), Color.White)
    "green" -> EventColors(Color(0xFF22C55E), Color.White)
    "red" -> EventColors(Color(0xFFEF4444), Color.White)
    "orange" -> EventColors(Color(0xFFF97316), Color.White)
    "teal" -> EventColors(Color(0xFF14B8A6), Color.White)
    "yellow" -> EventColors(Color(0xFFFACC15), Color(0xFF111827))
    "purple" -> EventColors(Color(0xFF8B5CF6), Color.White)
    "gray" -> EventColors(Color(0xFF6B7280), Color.White)
    else -> EventColors(Color(0xFF22C55E), Color.White)
}

private fun webEventColors(ev: TrainingItem): EventColors {
    if (ev.athleteId < 0) return EventColors(Color(0xFF1F2937), Color.White)
    if (ev.isLocked) return EventColors(Color(0xFFD6D3CC), TrainerAppNavy)
    if (ev.approvalStatus.equals("pending", ignoreCase = true)) return webPalette("orange")
    if (ev.athleteId > 0 && ev.secondAthleteId > 0) return webPalette("blue")
    if (ev.athleteId > 0) return webPalette("green")
    return webPalette(ev.colorKey)
}

private fun isRescheduleRequest(seriesId: String): Boolean =
    Regex("^reschedule:\\d+$").matches(seriesId.trim())

private fun markRescheduleOrigins(items: List<TrainingItem>): List<TrainingItem> {
    val originIds = items.mapNotNull { ev ->
        if (!ev.approvalStatus.equals("pending", ignoreCase = true)) return@mapNotNull null
        Regex("^reschedule:(\\d+)$").matchEntire(ev.seriesId.trim())?.groupValues?.getOrNull(1)
    }.toSet()
    if (originIds.isEmpty()) return items
    return items.map { ev ->
        if (!ev.isLocked && originIds.contains(ev.id) && !ev.approvalStatus.equals("pending", ignoreCase = true)) {
            ev.copy(awaitingReschedule = true)
        } else {
            ev
        }
    }
}

@Composable
private fun CalendarSlotEvent(ev: TrainingItem, onClick: () -> Unit) {
    val pending = !ev.isLocked && ev.approvalStatus.equals("pending", ignoreCase = true)
    val origin = ev.awaitingReschedule
    if (pending || origin) {
        PulsingCalendarSlotEvent(ev, pending, onClick)
    } else {
        StaticCalendarSlotEvent(ev, onClick)
    }
}

@Composable
private fun PulsingCalendarSlotEvent(ev: TrainingItem, pending: Boolean, onClick: () -> Unit) {
    val colors = webEventColors(ev)
    val transition = rememberInfiniteTransition(label = "eventPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (pending) 720 else 950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eventPulseValue"
    )
    val borderColor = if (pending) {
        Color(0xFFFFF7ED)
    } else {
        Color(0xFFFB923C).copy(alpha = 0.55f + 0.45f * pulse)
    }
    CalendarSlotEventCard(
        ev = ev,
        colors = colors,
        borderColor = borderColor,
        borderWidth = if (pending) 2.dp else 3.dp,
        scale = 1f + (if (pending) 0.03f else 0.025f) * pulse,
        statusLabel = if (pending) {
            if (isRescheduleRequest(ev.seriesId)) "Žádost o změnu" else "Ke schválení"
        } else {
            "Čeká na změnu"
        },
        onClick = onClick
    )
}

@Composable
private fun StaticCalendarSlotEvent(ev: TrainingItem, onClick: () -> Unit) {
    CalendarSlotEventCard(
        ev = ev,
        colors = webEventColors(ev),
        borderColor = Color.Transparent,
        borderWidth = 0.dp,
        scale = 1f,
        statusLabel = "",
        onClick = onClick
    )
}

@Composable
private fun CalendarSlotEventCard(
    ev: TrainingItem,
    colors: EventColors,
    borderColor: Color,
    borderWidth: androidx.compose.ui.unit.Dp,
    scale: Float,
    statusLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.background),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(6.dp)) {
            AppText(
                text = if (ev.isLocked) "🔒 ${ev.detail}" else "${ev.time} - ${ev.athleteName}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )
            if (!ev.isLocked && ev.detail.isNotBlank()) {
                AppText(ev.detail, fontSize = 10.sp, color = colors.text.copy(alpha = 0.9f))
            }
            if (statusLabel.isNotBlank()) {
                AppText(statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.text)
            }
        }
    }
}

private fun fetchCalendarFromApi(context: Context, token: String, onSuccess: (List<TrainingItem>) -> Unit, onError: (String) -> Unit = {}) {
    if (token.isBlank()) { onSuccess(emptyList()); return }
    runInBackground {
        try {
            val result = httpRequest(context, "calendar.php?token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null) {
                runOnMain { onError(apiErrorText(result, "Kalendář se nepodařilo načíst.")) }
                return@runInBackground
            }
            val array = json.optJSONArray("events") ?: json.optJSONArray("items")
            val items = mutableListOf<TrainingItem>()
            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = jsonId(obj).ifBlank { i.toString() }
                    val title = obj.optString("title", "Trénink")
                    val athleteName = obj.optString("athlete_name", obj.optString("athlete_label", "Sportovec"))
                    val timeLabel = obj.optString("time_label", "09:00")
                    val dateLabel = obj.optString("date_label", "")
                    val status = obj.optString("status", obj.optString("status_label", "Potvrzeno"))
                    val approvalStatus = obj.optString("approval_status", "approved")
                    val isLocked = obj.optBoolean("is_locked", false)
                    val location = obj.optString("location", "")
                    val startHour = obj.optInt("start_hour", 9)
                    val endHour = obj.optInt("end_hour", startHour + 1)
                    val athleteId = obj.optInt("athlete_id", 0)
                    val detailStr = if (location.isNotBlank()) "$title • $location" else title
                    items.add(TrainingItem(id, timeLabel, dateLabel, athleteName, detailStr, status, approvalStatus, isLocked, startHour, endHour, athleteId, title, location, obj.optString("starts_at", ""), obj.optString("ends_at", ""), obj.optString("color_key", "green"), obj.optInt("second_athlete_id", 0), obj.optString("series_id", "")))
                }
            }
            runOnMain { onSuccess(markRescheduleOrigins(items)) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun createCalendarEventApi(context: Context, token: String, athleteId: Int?, title: String, location: String, startsAt: String, endsAt: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                if (athleteId != null && athleteId > 0) put("athlete_id", athleteId)
                put("title", title)
                put("location", location)
                put("starts_at", startsAt)
                put("ends_at", endsAt)
            }
            val result = httpRequest(context, "calendar.php?action=create&token=${tokenQuery(token)}", "POST", token, jsonBody)
            val json = result.jsonOrNull()
            if (result.code in 200..299 && json?.optBoolean("success", false) == true) {
                runOnMain { onSuccess() }
            } else {
                runOnMain { onError(apiErrorText(result, "Chyba vytvoření události.")) }
            }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun lockCalendarApi(context: Context, token: String, note: String, startsAt: String, endsAt: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                put("note", note)
                put("starts_at", startsAt)
                put("ends_at", endsAt)
            }
            val result = httpRequest(context, "calendar.php?action=lock&token=${tokenQuery(token)}", "POST", token, jsonBody)
            val json = result.jsonOrNull()
            if (result.code in 200..299 && json?.optBoolean("success", false) == true) {
                runOnMain { onSuccess() }
            } else {
                runOnMain { onError(apiErrorText(result, "Chyba uzamčení.")) }
            }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun approveCalendarEventApi(
    context: Context,
    token: String,
    eventId: Int,
    startsAt: String = "",
    endsAt: String = "",
    title: String? = null,
    location: String? = null,
    athleteId: Int? = null,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                put("event_id", eventId)
                if (startsAt.isNotBlank()) put("starts_at", startsAt)
                if (endsAt.isNotBlank()) put("ends_at", endsAt)
                if (title != null) put("title", title)
                if (location != null) put("location", location)
                if (athleteId != null) put("athlete_id", athleteId)
            }
            val result = httpRequest(context, "calendar.php?action=approve&token=${tokenQuery(token)}", "POST", token, jsonBody)
            val json = result.jsonOrNull()
            if (result.code in 200..299 && json?.optBoolean("success", false) == true) runOnMain { onSuccess() }
            else runOnMain { onError(apiErrorText(result, "Schválení se nepodařilo.")) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun deleteCalendarEventApi(context: Context, token: String, eventId: Int, startsAt: String = "", endsAt: String = "", reject: Boolean = false, onSuccess: () -> Unit, onError: (String) -> Unit) {
    runInBackground {
        try {
            val actionName = when {
                reject -> "reject"
                eventId >= 200000 && startsAt.isNotBlank() -> "unlock"
                else -> "delete"
            }
            val query = buildString {
                append("calendar.php?action=$actionName&event_id=$eventId&token=${tokenQuery(token)}")
                if (startsAt.isNotBlank() && endsAt.isNotBlank()) {
                    append("&starts_at=${URLEncoder.encode(startsAt, "UTF-8")}")
                    append("&ends_at=${URLEncoder.encode(endsAt, "UTF-8")}")
                }
            }
            val path = query
            val jsonBody = JSONObject().apply {
                put("event_id", eventId)
                if (startsAt.isNotBlank()) put("starts_at", startsAt)
                if (endsAt.isNotBlank()) put("ends_at", endsAt)
            }
            val result = httpRequest(context, path, "POST", token, jsonBody)
            val json = result.jsonOrNull()
            if (result.code in 200..299 && json?.optBoolean("success", false) == true) {
                runOnMain { onSuccess() }
            } else {
                runOnMain { onError(apiErrorText(result, "Chyba mazání.")) }
            }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun fetchExercisesApi(context: Context, token: String, onSuccess: (List<ExerciseItem>) -> Unit, onError: (String) -> Unit = {}) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val result = httpRequest(context, "exercises.php?token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null) {
                runOnMain { onError(apiErrorText(result, "Cviky se nepodařilo načíst.")) }
                return@runInBackground
            }
            val array = json.optJSONArray("exercises")
            val items = mutableListOf<ExerciseItem>()
            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    items.add(
                        ExerciseItem(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name", "Cvik"),
                            sportType = obj.optString("sport_type", "standard")
                        )
                    )
                }
            }
            runOnMain { onSuccess(items) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun addExerciseToSessionApi(context: Context, token: String, sessionId: Int, exerciseId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                put("session_id", sessionId)
                put("exercise_id", exerciseId)
            }
            val result = httpRequest(context, "training_session.php?action=add_exercise&token=${tokenQuery(token)}", "POST", token, jsonBody)
            val json = result.jsonOrNull()
            val explicitFail = json != null && json.has("success") && !json.optBoolean("success")
            if (result.code in 200..299 && !explicitFail) runOnMain { onSuccess() }
            else runOnMain { onError(apiErrorText(result, "Chyba přidání cviku.")) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun fetchActiveSessionDetailApi(context: Context, token: String, sessionId: Int, onSuccess: (JSONObject, List<SessionExerciseItem>) -> Unit, onError: (String) -> Unit) {
    runInBackground {
        try {
            val result = httpRequest(context, "training_session.php?id=$sessionId&token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null) {
                runOnMain { onError(apiErrorText(result, "Trénink se nepodařilo načíst.")) }
                return@runInBackground
            }
            val sessObj = json.optJSONObject("session") ?: JSONObject()
            val exArr = json.optJSONArray("exercises")
            val exList = mutableListOf<SessionExerciseItem>()
            if (exArr != null) {
                for (i in 0 until exArr.length()) {
                    val item = exArr.optJSONObject(i) ?: continue
                    val exId = item.optInt("exercise_id", 0)
                    val order = item.optInt("exercise_order", i + 1)
                    val name = item.optString("exercise_name", "Cvik")
                    val sArr = item.optJSONArray("series")
                    val sList = mutableListOf<SessionSeriesItem>()
                    if (sArr != null) {
                        for (j in 0 until sArr.length()) {
                            val s = sArr.optJSONObject(j) ?: continue
                            sList.add(SessionSeriesItem(s.optInt("id", 0), s.optInt("series_order", j + 1), s.optDouble("weight", 0.0).toFloat(), s.optInt("reps", 0)))
                        }
                    }
                    val prevArr = item.optJSONArray("previous_series")
                    val prevList = mutableListOf<SessionSeriesItem>()
                    if (prevArr != null) {
                        for (j in 0 until prevArr.length()) {
                            val s = prevArr.optJSONObject(j) ?: continue
                            prevList.add(SessionSeriesItem(s.optInt("id", 0), s.optInt("series_order", j + 1), s.optDouble("weight", 0.0).toFloat(), s.optInt("reps", 0)))
                        }
                    }
                    exList.add(SessionExerciseItem(exId, order, name, item.optString("sport_type", "standard"), item.optBoolean("is_timed", false), sList, prevList, item.optString("previous_label", "")))
                }
            }
            runOnMain { onSuccess(sessObj, exList) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun saveSeriesApi(context: Context, token: String, sessionId: Int, exerciseId: Int, weight: Float, reps: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                put("session_id", sessionId)
                put("exercise_id", exerciseId)
                put("weight", weight)
                put("reps", reps)
            }
            val result = httpRequest(context, "training_session.php?action=save_series&token=${tokenQuery(token)}", "POST", token, jsonBody)
            val json = result.jsonOrNull()
            val explicitFail = json != null && json.has("success") && !json.optBoolean("success")
            if (result.code in 200..299 && !explicitFail) runOnMain { onSuccess() }
            else runOnMain { onError(apiErrorText(result, "Chyba uložení série.")) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun completeActiveSessionApi(context: Context, token: String, sessionId: Int, location: String, notes: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                put("session_id", sessionId)
                put("location", location)
                put("notes", notes)
            }
            val result = httpRequest(context, "training_session.php?action=complete&token=${tokenQuery(token)}", "POST", token, jsonBody)
            val json = result.jsonOrNull()
            val explicitFail = json != null && json.has("success") && !json.optBoolean("success")
            if (result.code in 200..299 && !explicitFail) runOnMain { onSuccess() }
            else runOnMain { onError(apiErrorText(result, "Chyba dokončení.")) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun fetchChatConversationsFromApi(context: Context, token: String, onSuccess: (List<ChatConversation>) -> Unit, onError: (String) -> Unit = {}) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val result = httpRequest(context, "chat.php?action=conversations&token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null) {
                runOnMain { onError(apiErrorText(result, "Chat se nepodařilo načíst.")) }
                return@runInBackground
            }
            val array = json.optJSONArray("conversations")
            val items = mutableListOf<ChatConversation>()
            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    val name = obj.optString("name", "")
                    val subtitle = obj.optString("subtitle", "")
                    val icon = obj.optString("icon", "👤")
                    val isAdmin = obj.optBoolean("is_admin", false)
                    val unreadCount = obj.optInt("unread_count", 0)
                    val lastMsg = obj.optString("last_message", "")
                    val lastTime = obj.optString("last_time", "")
                    items.add(ChatConversation(id, name, subtitle, icon, isAdmin, unreadCount, if (lastMsg.isNotBlank()) listOf(ChatMessage("1", if (isAdmin) "Administrátor" else name, false, false, lastMsg, lastTime)) else emptyList(), obj.optString("photo", "")))
                }
            }
            runOnMain { onSuccess(items) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun fetchChatMessagesFromApi(context: Context, token: String, conversationId: String, onSuccess: (List<ChatMessage>) -> Unit, onError: (String) -> Unit = {}) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val result = httpRequest(context, "chat.php?action=thread&conversation_id=${URLEncoder.encode(conversationId, "UTF-8")}&token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null) {
                runOnMain { onError(apiErrorText(result, "Zprávy se nepodařilo načíst.")) }
                return@runInBackground
            }
            val array = json.optJSONArray("messages")
            val items = mutableListOf<ChatMessage>()
            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    items.add(
                        ChatMessage(
                            id = obj.optString("id", i.toString()),
                            senderName = obj.optString("sender_name", ""),
                            isMe = obj.optBoolean("is_me", false),
                            isRead = obj.optBoolean("is_read", false),
                            wasUnread = obj.optBoolean("coach_unread", false),
                            text = obj.optString("text", ""),
                            time = obj.optString("time", "")
                        )
                    )
                }
            }
            runOnMain { onSuccess(items) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun sendChatMessageApi(context: Context, token: String, conversationId: String, body: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                put("conversation_id", conversationId)
                put("body", body)
            }
            val result = httpRequest(context, "chat.php?action=send&token=${tokenQuery(token)}", "POST", token, jsonBody)
            val json = result.jsonOrNull()
            val explicitFail = json != null && json.has("success") && !json.optBoolean("success")
            if (result.code in 200..299 && !explicitFail) runOnMain { onSuccess() }
            else runOnMain { onError(apiErrorText(result, "Chyba odeslání.")) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun fetchOpenSessions(context: Context, token: String, onSuccess: (List<OpenSession>) -> Unit) {
    if (token.isBlank()) {
        onSuccess(emptyList())
        return
    }
    runInBackground {
        try {
            val result = httpRequest(context, "training_session.php?action=open&token=${tokenQuery(token)}", token = token)
            val json = result.jsonOrNull()
            if (result.code !in 200..299 || json == null || !json.optBoolean("success", false)) {
                runOnMain { onSuccess(emptyList()) }
                return@runInBackground
            }
            val array = json.optJSONArray("sessions")
            val items = mutableListOf<OpenSession>()
            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optInt("id", 0)
                    if (id <= 0) continue
                    items.add(
                        OpenSession(
                            id = id,
                            athleteId = obj.optInt("athlete_id", 0),
                            athleteName = obj.optString("athlete_name", "Sportovec"),
                            setName = obj.optString("workout_set_name", "Trénink"),
                            startedAt = obj.optString("started_at", "")
                        )
                    )
                }
            }
            runOnMain { onSuccess(items) }
        } catch (_: Exception) {
            runOnMain { onSuccess(emptyList()) }
        }
    }
}

private fun startTrainingApi(
    context: Context,
    token: String,
    athleteId: Int,
    workoutSetId: Int,
    onSuccess: (JSONObject) -> Unit,
    onError: (String) -> Unit,
    onExistingSession: (Int, String) -> Unit = { _, message -> onError(message) }
) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                put("athlete_id", athleteId)
                put("workout_set_id", workoutSetId)
            }
            val result = httpRequest(context, "training_start.php?token=${tokenQuery(token)}", "POST", token, jsonBody)
            val json = result.jsonOrNull()
            val existingId = json?.optInt("existing_session_id", 0) ?: 0
            if (existingId > 0 && (result.code == 409 || json?.optBoolean("success", false) != true)) {
                runOnMain { onExistingSession(existingId, apiErrorText(result, "Sportovec už má rozpracovaný trénink.")) }
                return@runInBackground
            }
            if (result.code !in 200..299 || json == null || !json.optBoolean("success", false)) {
                runOnMain { onError(apiErrorText(result, "Spuštění tréninku se nepodařilo.")) }
                return@runInBackground
            }
            runOnMain { onSuccess(json) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

@Composable
private fun AthletePasswordScreen(
    context: Context,
    token: String,
    onChanged: () -> Unit,
    onLogout: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp)
        ) {
            AppText("Nastavte nové heslo", color = TrainerAppNavy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            AppText("Při prvním přihlášení je potřeba heslo změnit. Musí mít alespoň 8 znaků.", color = TrainerAppGray, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it; errorMessage = "" }, modifier = Modifier.fillMaxWidth(), label = { AppText("Aktuální heslo") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !loading)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = newPassword, onValueChange = { newPassword = it; errorMessage = "" }, modifier = Modifier.fillMaxWidth(), label = { AppText("Nové heslo") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !loading)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it; errorMessage = "" }, modifier = Modifier.fillMaxWidth(), label = { AppText("Potvrzení nového hesla") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !loading)
            if (errorMessage.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                AppText(errorMessage, color = Color(0xFFB00020), fontSize = 14.sp)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    when {
                        currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> errorMessage = "Vyplňte všechna pole."
                        newPassword.length < 8 -> errorMessage = "Nové heslo musí mít alespoň 8 znaků."
                        newPassword != confirmPassword -> errorMessage = "Nová hesla se neshodují."
                        else -> {
                            loading = true
                            errorMessage = ""
                            changeAthletePasswordApi(context, token, currentPassword, newPassword, confirmPassword,
                                onSuccess = { loading = false; onChanged() },
                                onError = { message -> loading = false; errorMessage = message }
                            )
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = TrainerAppNavy, strokeWidth = 3.dp)
                else AppText("Uložit nové heslo", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { AppText("Odhlásit", color = TrainerAppNavy) }
        }
    }
}

private fun changeAthletePasswordApi(context: Context, token: String, currentPassword: String, newPassword: String, confirmPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                put("current_password", currentPassword)
                put("new_password", newPassword)
                put("new_password_confirm", confirmPassword)
            }
            val result = httpRequest(context, "athlete_password.php?token=${tokenQuery(token)}", "POST", token, jsonBody)
            val json = result.jsonOrNull()
            if (result.code in 200..299 && json?.optBoolean("success", false) == true) {
                runOnMain { onSuccess() }
            } else {
                runOnMain { onError(apiErrorText(result, "Heslo se nepodařilo změnit.")) }
            }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun loginToApi(username: String, password: String, context: Context, accountType: String = "trainer", remember: Boolean = true, onSuccess: (String, String, String, JSONObject) -> Unit, onError: (String) -> Unit) {
    runInBackground {
        try {
            val jsonBody = JSONObject().apply {
                put("username", username)
                put("password", password)
                put("remember", remember)
            }
            val path = if (accountType == "athlete") "athlete_login.php" else "login.php"
            val result = httpRequest(context, path, "POST", jsonBody = jsonBody)
            val json = result.jsonOrNull()
            if (json == null) {
                runOnMain { onError(apiErrorText(result, "Server vrátil prázdnou odpověď.")) }
                return@runInBackground
            }
            if (!json.optBoolean("success", false)) {
                runOnMain { onError(json.optString("error", "Nesprávné uživatelské jméno nebo heslo.")) }
                return@runInBackground
            }
            val coach = json.optJSONObject("athlete") ?: json.optJSONObject("coach") ?: json.optJSONObject("user")
            val name = coach?.optString("name", "")?.takeIf { it.isNotBlank() } ?: json.optString("name", username)
            val id = coach?.opt("id")?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: json.optString("id", "")
            val newToken = json.optString("token", "")
            if (newToken.isBlank()) {
                runOnMain { onError("Přihlášení proběhlo, ale server nevrátil token.") }
                return@runInBackground
            }
            runOnMain { onSuccess(name, id, newToken, json) }
        } catch (e: Exception) {
            runOnMain { onError(friendlyNetworkError(e)) }
        }
    }
}

private fun logoutFromApi(context: Context, token: String) {
    if (token.isBlank()) return
    runInBackground {
        try {
            val jsonBody = JSONObject().apply { put("token", token) }
            httpRequest(context, "logout.php?token=${tokenQuery(token)}", "POST", token, jsonBody)
        } catch (_: Exception) {
        }
    }
}

private fun runOnMain(block: () -> Unit) {
    Handler(Looper.getMainLooper()).post { block() }
}

@Composable
private fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign
    )
}

@Composable
private fun TrainerAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
private fun TodayTrainingCard(time: String, athlete: String, detail: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).clickable { onClick() }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(15.dp)).background(TrainerAppYellow), contentAlignment = Alignment.Center) {
            AppText("▶", color = TrainerAppNavy, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            AppText(time, color = TrainerAppGray, fontSize = 13.sp)
            AppText(athlete, color = TrainerAppNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            AppText(detail, color = TrainerAppGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DashboardTile(icon: String, title: String, modifier: Modifier, badgeCount: Int = 0, onClick: () -> Unit) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().height(125.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).clickable { onClick() }.padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AppText(icon, fontSize = 34.sp)
            Spacer(Modifier.height(8.dp))
            AppText(title, color = TrainerAppNavy, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB00020))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                AppText("$badgeCount", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class AthleteUpcoming(
    val title: String,
    val location: String,
    val startsAt: String,
    val endsAt: String,
    val status: String
)

private data class AthleteSlot(
    val id: Int,
    val title: String,
    val location: String,
    val startsAt: String,
    val endsAt: String,
    val dateLabel: String,
    val timeLabel: String,
    val status: String,
    val mine: Boolean,
    val foreign: Boolean,
    val canChange: Boolean,
    val canCancel: Boolean,
    val canEdit: Boolean,
    val locked: Boolean,
    val startHour: Int,
    val endHour: Int,
    val seriesId: String
)

private data class AthleteChatLine(val id: Int, val body: String, val createdAt: String, val mine: Boolean)
private data class AthletePayTraining(
    val title: String,
    val location: String,
    val startsAt: String,
    val endsAt: String,
    val paired: Boolean,
    val makeup: Boolean,
    val amount: String,
    val note: String
)

private data class AthletePayRow(
    val month: String,
    val sessions: Int,
    val amount: String,
    val status: String,
    val paidAt: String,
    val trainings: List<AthletePayTraining> = emptyList()
)
private data class AthleteWeightRow(val measuredAt: String, val weightKg: Float)

private fun athletePortal(
    context: Context,
    token: String,
    action: String,
    method: String = "GET",
    body: JSONObject? = null,
    extraQuery: String = ""
): HttpResult {
    val path = buildString {
        append("athlete_portal.php?action=")
        append(URLEncoder.encode(action, "UTF-8"))
        append("&token=")
        append(tokenQuery(token))
        if (extraQuery.isNotBlank()) {
            append("&")
            append(extraQuery)
        }
    }
    return httpRequest(context, path, method = method, token = token, jsonBody = body)
}

private fun athleteRange(start: String, end: String): String {
    val day = start.take(10).split("-")
    val label = if (day.size == 3) {
        "${day[2].toIntOrNull() ?: day[2]}. ${day[1].toIntOrNull() ?: day[1]}. ${day[0]}"
    } else {
        start.take(10)
    }
    val from = start.drop(11).take(5)
    val to = end.drop(11).take(5)
    return if (from.isBlank()) label else "$label   $from–$to"
}

private fun athleteStatusLabel(status: String): String = when (status) {
    "pending" -> "Ke schválení"
    "approved" -> "Naplánováno"
    else -> if (status.isBlank()) "" else status
}

private fun athletePaymentStatus(status: String): Pair<String, Color> = when (status) {
    "paid" -> "Uhrazeno" to Color(0xFF15803D)
    "pending" -> "Čeká na úhradu" to Color(0xFFB45309)
    else -> "Čeká na vystavení výzvy" to TrainerAppGray
}

private fun athleteMoney(value: Double, missing: Boolean): String {
    if (missing) return "—"
    return if (value % 1.0 == 0.0) "${value.toInt()} Kč" else String.format(Locale.forLanguageTag("cs-CZ"), "%.2f Kč", value)
}

private fun athleteMonthLabel(raw: String): String {
    val normalized = if (raw.length == 7) "$raw-01" else raw.take(10)
    val parsed = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(normalized) }.getOrNull() ?: return raw
    val label = SimpleDateFormat("LLLL yyyy", Locale.forLanguageTag("cs-CZ")).format(parsed)
    return label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("cs-CZ")) else it.toString() }
}

@Composable
private fun ProfilePhoto(url: String, name: String, size: Dp) {
    val fullUrl = when {
        url.isBlank() -> ""
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("/") -> "https://www.reservio.online$url"
        else -> "https://www.reservio.online/$url"
    }
    var bitmap by remember(fullUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(fullUrl) {
        bitmap = if (fullUrl.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val connection = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        instanceFollowRedirects = true
                    }
                    connection.inputStream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
    val letter = name.trim().take(1).uppercase().ifBlank { "?" }
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(MetalHeaderBrush),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(bitmap = loaded, contentDescription = name, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            AppText(letter, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AthleteSection(
    screen: String,
    name: String,
    photoUrl: String,
    token: String,
    context: Context,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onPhoto: (String) -> Unit = {}
) {
    var shownPhoto by remember(photoUrl) { mutableStateOf(photoUrl) }
    var unread by remember { mutableIntStateOf(0) }
    var adminUnread by remember { mutableIntStateOf(0) }
    var coachUnread by remember { mutableIntStateOf(0) }
    var upcoming by remember { mutableStateOf<List<AthleteUpcoming>>(emptyList()) }
    var latestWeight by remember { mutableStateOf("") }
    var coachName by remember { mutableStateOf("Trenér") }
    var coachPhoto by remember { mutableStateOf("") }
    var homeLoading by remember { mutableStateOf(true) }
    var homeError by remember { mutableStateOf("") }
    var homeTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(homeTick, token) {
        if (token.isBlank()) return@LaunchedEffect
        homeLoading = true
        withContext(Dispatchers.IO) {
            try {
                val result = athletePortal(context, token, "home")
                val json = result.jsonOrNull()
                if (result.code !in 200..299 || json == null || !json.optBoolean("success", false)) {
                    runOnMain {
                        homeError = apiErrorText(result, "Přehled se nepodařilo načíst.")
                        homeLoading = false
                    }
                    return@withContext
                }
                val photo = json.optJSONObject("athlete")?.optString("photo", "").orEmpty()
                val coach = json.optJSONObject("coach")
                val weight = json.optJSONObject("weight")
                val weightText = if (weight == null) "" else "${weight.optDouble("weight_kg")} kg · ${weight.optString("measured_at", "").take(10)}"
                val items = json.optJSONArray("upcoming").toUpcoming()
                val adminUnreadCount = json.optInt("admin_unread", 0)
                val coachUnreadCount = json.optInt("coach_unread", json.optInt("unread_chat", 0))
                val unreadCount = adminUnreadCount + coachUnreadCount
                runOnMain {
                    upcoming = items
                    unread = unreadCount
                    adminUnread = adminUnreadCount
                    coachUnread = coachUnreadCount
                    latestWeight = weightText
                    if (coach != null) {
                        val fetchedCoach = coach.optString("name", "").trim()
                        if (fetchedCoach.isNotBlank()) coachName = fetchedCoach
                        coachPhoto = coach.optString("photo", "")
                    }
                    homeError = ""
                    homeLoading = false
                    if (photo.isNotBlank() && photo != shownPhoto) {
                        shownPhoto = photo
                        onPhoto(photo)
                    }
                }
            } catch (e: Exception) {
                runOnMain {
                    homeError = friendlyNetworkError(e)
                    homeLoading = false
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(MetalHeaderBrush).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfilePhoto(shownPhoto, name, 42.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    AppText(name.ifBlank { "Sportovec" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    AppText("sportovec", color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp)
                }
                TextButton(onClick = onLogout) { AppText("Odhlásit", color = Color.White, fontWeight = FontWeight.Bold) }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (screen) {
                    "calendar" -> AthleteCalendarPane(token, context)
                    "chat" -> AthleteChatPane(token, context, coachName, coachPhoto, adminUnread, coachUnread) { admin, coach ->
                        adminUnread = admin
                        coachUnread = coach
                        unread = admin + coach
                    }
                    "payments" -> AthletePaymentsPane(token, context)
                    "weight" -> AthleteWeightPane(token, context) { homeTick += 1 }
                    else -> AthleteHomePane(
                        upcoming = upcoming,
                        latestWeight = latestWeight,
                        unread = unread,
                        loading = homeLoading,
                        error = homeError,
                        onNavigate = onNavigate
                    )
                }
            }

            AthleteBottomBar(current = screen, unread = unread, onNavigate = onNavigate)
        }
    }
}

private fun JSONArray?.toUpcoming(): List<AthleteUpcoming> {
    if (this == null) return emptyList()
    return List(length()) { index ->
        val item = getJSONObject(index)
        AthleteUpcoming(
            title = item.optString("title", "Trénink"),
            location = item.optString("location", ""),
            startsAt = item.optString("starts_at", ""),
            endsAt = item.optString("ends_at", ""),
            status = item.optString("approval_status", "approved")
        )
    }
}

@Composable
private fun AthleteHomePane(
    upcoming: List<AthleteUpcoming>,
    latestWeight: String,
    unread: Int,
    loading: Boolean,
    error: String,
    onNavigate: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val first = upcoming.firstOrNull()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(enabled = upcoming.size > 1) { expanded = !expanded },
            colors = CardDefaults.cardColors(containerColor = MetalCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText("📅", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        AppText("Nejbližší trénink", color = TrainerAppNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        AppText(
                            if (upcoming.isEmpty()) "žádný naplánovaný" else if (upcoming.size == 1) "1 naplánovaný" else "${upcoming.size} naplánované",
                            color = TrainerAppGray,
                            fontSize = 13.sp
                        )
                    }
                    if (upcoming.size > 1) AppText(if (expanded) "▲" else "▼", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                if (loading) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(color = TrainerAppNavy, modifier = Modifier.size(22.dp))
                } else if (first == null) {
                    Spacer(Modifier.height(8.dp))
                    AppText("Nemáte žádný nadcházející trénink.", color = TrainerAppGray, fontSize = 13.sp)
                } else {
                    Spacer(Modifier.height(12.dp))
                    AthleteUpcomingRow(first)
                    if (expanded) {
                        upcoming.drop(1).forEach { item ->
                            Spacer(Modifier.height(10.dp))
                            AthleteUpcomingRow(item)
                        }
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE3E3)), shape = RoundedCornerShape(12.dp)) {
                AppText(error, color = Color(0xFFB00020), modifier = Modifier.padding(14.dp), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(22.dp))
        AppText("Rychlý přístup", color = TrainerAppNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardTile("📅", "Kalendář", Modifier.weight(1f)) { onNavigate("calendar") }
            DashboardTile("💬", "Chat", Modifier.weight(1f), badgeCount = unread) { onNavigate("chat") }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardTile("💳", "Platby", Modifier.weight(1f)) { onNavigate("payments") }
            DashboardTile("⚖", "Váha", Modifier.weight(1f)) { onNavigate("weight") }
        }

        if (latestWeight.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigate("weight") },
                colors = CardDefaults.cardColors(containerColor = MetalCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    AppText("Poslední váha", color = TrainerAppGray, fontSize = 13.sp)
                    AppText(latestWeight, color = TrainerAppNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AthleteUpcomingRow(item: AthleteUpcoming) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp)) {
        AppText(athleteRange(item.startsAt, item.endsAt), color = TrainerAppGray, fontSize = 13.sp)
        AppText(item.title.ifBlank { "Trénink" }, color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (item.location.isNotBlank()) AppText(item.location, color = TrainerAppGray, fontSize = 13.sp)
        AppText(athleteStatusLabel(item.status), color = if (item.status == "pending") Color(0xFFC2410C) else Color(0xFF15803D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun AthleteSlot.toTraining(): TrainingItem {
    val end = if (endHour <= startHour) startHour + 1 else endHour
    return TrainingItem(
        id = id.toString(),
        time = timeLabel.ifBlank { String.format(Locale.US, "%02d:00", startHour) },
        date = dateLabel,
        athleteName = when {
            locked -> title.ifBlank { "Uzamčeno" }
            foreign -> "Obsazeno"
            else -> title.ifBlank { "Trénink" }
        },
        detail = if (foreign || locked) "" else location,
        approvalStatus = if (locked || foreign) "approved" else status,
        isLocked = locked,
        startHour = startHour,
        endHour = end,
        athleteId = if (foreign && !locked) -1 else if (mine) 1 else 0,
        title = title,
        location = location,
        startsAt = startsAt,
        endsAt = endsAt,
        seriesId = seriesId
    )
}

@Composable
private fun AthleteCalendarPane(token: String, context: Context) {
    var weekOffset by remember { mutableIntStateOf(0) }
    val weekDays = remember(weekOffset) { getWeekDays(weekOffset) }
    val weekHeader = remember(weekOffset) { getWeekHeaderLabel(weekOffset) }
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time) }
    val todayIndexInWeek = remember(weekDays, todayDateStr) {
        weekDays.indexOfFirst { it.second == todayDateStr }.let { if (it < 0) 0 else it }
    }
    var selectedDayIndex by remember { mutableIntStateOf(todayIndexInWeek) }
    LaunchedEffect(weekOffset) {
        val idx = weekDays.indexOfFirst { it.second == todayDateStr }
        selectedDayIndex = if (idx >= 0) idx else 0
    }
    var slots by remember { mutableStateOf<List<AthleteSlot>>(emptyList()) }
    var venues by remember { mutableStateOf<List<VenueItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var createDate by remember { mutableStateOf("") }
    var createHour by remember { mutableIntStateOf(9) }
    var actionSlot by remember { mutableStateOf<AthleteSlot?>(null) }
    val hours = (5..21).toList()
    val trainings = remember(slots) { markRescheduleOrigins(slots.map { it.toTraining() }) }

    LaunchedEffect(weekOffset, reload, token) {
        loading = true
        val from = weekDays.firstOrNull()?.second.orEmpty()
        val to = weekDays.lastOrNull()?.second.orEmpty()
        withContext(Dispatchers.IO) {
            try {
                val extra = "from=${URLEncoder.encode(from, "UTF-8")}&to=${URLEncoder.encode(to, "UTF-8")}"
                val result = athletePortal(context, token, "calendar", extraQuery = extra)
                val json = result.jsonOrNull()
                if (result.code !in 200..299 || json == null || !json.optBoolean("success", false)) {
                    runOnMain {
                        error = apiErrorText(result, "Kalendář se nepodařilo načíst.")
                        loading = false
                    }
                    return@withContext
                }
                val parsed = mutableListOf<AthleteSlot>()
                val events = json.optJSONArray("events")
                if (events != null) {
                    for (i in 0 until events.length()) {
                        val item = events.getJSONObject(i)
                        parsed.add(item.toAthleteSlot(locked = false))
                    }
                }
                val locks = json.optJSONArray("locks")
                if (locks != null) {
                    for (i in 0 until locks.length()) {
                        parsed.add(locks.getJSONObject(i).toAthleteSlot(locked = true))
                    }
                }
                val venueItems = mutableListOf<VenueItem>()
                val venueArray = json.optJSONArray("venues")
                if (venueArray != null) {
                    for (i in 0 until venueArray.length()) {
                        val item = venueArray.getJSONObject(i)
                        venueItems.add(VenueItem(item.optInt("id"), item.optString("name", ""), item.optString("address", "")))
                    }
                }
                runOnMain {
                    slots = parsed.sortedBy { it.startsAt }
                    venues = venueItems
                    error = ""
                    loading = false
                }
            } catch (e: Exception) {
                runOnMain {
                    error = friendlyNetworkError(e)
                    loading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(weekHeader, color = TrainerAppNavy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).clickable { weekOffset-- },
                        contentAlignment = Alignment.Center
                    ) { AppText("‹", color = TrainerAppNavy, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).clickable { weekOffset++ },
                        contentAlignment = Alignment.Center
                    ) { AppText("›", color = TrainerAppNavy, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.weight(1f))
            if (loading) {
                CircularProgressIndicator(color = TrainerAppNavy, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MetalGoldBrush)
                    .clickable {
                        createDate = weekDays.getOrNull(selectedDayIndex)?.second.orEmpty()
                        createHour = 9
                        showCreate = true
                    },
                contentAlignment = Alignment.Center
            ) { AppText("+", color = TrainerAppNavy, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        }

        val pendingBlink = rememberInfiniteTransition(label = "athletePendingDot")
        val pendingAlpha by pendingBlink.animateFloat(
            initialValue = 1f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 520), RepeatMode.Reverse),
            label = "athletePendingDotAlpha"
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDays.forEachIndexed { index, (dayLabel, dateStr) ->
                val isSelected = index == selectedDayIndex
                val isToday = dateStr == todayDateStr
                val needsAttention = trainings.any {
                    it.date == dateStr && !it.isLocked && (it.approvalStatus.equals("pending", true) || it.awaitingReschedule)
                }
                val hasEvents = trainings.any { it.date == dateStr && !it.isLocked }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(when {
                                isSelected -> TrainerAppYellow
                                isToday -> Color(0xFFF3E6C4)
                                else -> Color.Transparent
                            })
                            .clickable { selectedDayIndex = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = dayLabel,
                            color = TrainerAppNavy,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (needsAttention || hasEvents) {
                        Spacer(Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .size(if (needsAttention) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (needsAttention) Color(0xFFD32F2F).copy(alpha = pendingAlpha)
                                    else if (isSelected) TrainerAppNavy
                                    else TrainerAppYellow
                                )
                        )
                    }
                }
            }
        }
        if (error.isNotBlank()) {
            AppText(error, color = Color(0xFFB00020), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        Spacer(Modifier.height(6.dp))
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            val selectedDate = weekDays.getOrNull(selectedDayIndex)?.second.orEmpty()
            hours.forEach { hour ->
                val hourStr = String.format(Locale.getDefault(), "%02d:00", hour)
                val hourEvents = trainings.filter { tr ->
                    if (tr.date != selectedDate) return@filter false
                    if (tr.startHour == tr.endHour) tr.startHour == hour else hour >= tr.startHour && hour < tr.endHour
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(60.dp).border(0.5.dp, Color(0xFFE0DCD3)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(55.dp).padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
                        AppText(hourStr, fontSize = 12.sp, color = TrainerAppGray)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(Color(0xFFF2EFE8))
                            .border(0.5.dp, Color(0xFFE0DCD3))
                            .clickable(enabled = hourEvents.isEmpty()) {
                                createDate = selectedDate
                                createHour = hour
                                showCreate = true
                            }
                    ) {
                        hourEvents.forEach { ev ->
                            CalendarSlotEvent(ev) {
                                actionSlot = slots.firstOrNull { it.id.toString() == ev.id }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AthleteReserveDialog(
            mode = "create",
            title = "Rezervovat termín",
            confirm = "Rezervovat",
            initialDate = createDate.ifBlank { weekDays.getOrNull(selectedDayIndex)?.second.orEmpty() },
            initialHour = createHour,
            initialMinute = 0,
            initialLocation = "",
            initialEventTitle = "Trénink",
            eventId = 0,
            venues = venues,
            token = token,
            context = context,
            onDismiss = { showCreate = false },
            onDone = {
                showCreate = false
                reload += 1
            }
        )
    }
    val slot = actionSlot
    if (slot != null && slot.canEdit) {
        AthleteReserveDialog(
            mode = "update_request",
            title = "Upravit požadavek",
            confirm = "Uložit změny",
            initialDate = slot.dateLabel,
            initialHour = slot.startHour,
            initialMinute = slot.timeLabel.drop(3).take(2).toIntOrNull() ?: 0,
            initialLocation = slot.location,
            initialEventTitle = slot.title,
            eventId = slot.id,
            venues = venues,
            token = token,
            context = context,
            onDismiss = { actionSlot = null },
            onDone = {
                actionSlot = null
                reload += 1
            }
        )
    } else if (slot != null && slot.canChange) {
        AthleteReserveDialog(
            mode = "request_change",
            title = "Požádat o změnu termínu",
            confirm = "Odeslat požadavek",
            initialDate = slot.dateLabel,
            initialHour = slot.startHour,
            initialMinute = slot.timeLabel.drop(3).take(2).toIntOrNull() ?: 0,
            initialLocation = slot.location,
            initialEventTitle = slot.title,
            eventId = slot.id,
            venues = venues,
            token = token,
            context = context,
            onDismiss = { actionSlot = null },
            onDone = {
                actionSlot = null
                reload += 1
            }
        )
    } else if (slot != null && slot.canCancel) {
        AlertDialog(
            onDismissRequest = { actionSlot = null },
            title = { AppText("Žádost o změnu", color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
            text = { AppText("Zrušením se smaže jen tato žádost. Původní schválený termín zůstane.", color = TrainerAppGray, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        val eventId = slot.id
                        actionSlot = null
                        runInBackground {
                            try {
                                val result = athletePortal(context, token, "cancel_request", "POST", JSONObject().put("event_id", eventId))
                                val json = result.jsonOrNull()
                                runOnMain {
                                    if (json?.optBoolean("success", false) == true) reload += 1
                                    else error = json?.optString("error", "Žádost se nepodařilo zrušit.").orEmpty()
                                }
                            } catch (e: Exception) {
                                runOnMain { error = friendlyNetworkError(e) }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
                ) { AppText("Zrušit žádost", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { actionSlot = null }) { AppText("Zavřít") } }
        )
    }
}

private fun JSONObject.toAthleteSlot(locked: Boolean): AthleteSlot {
    val starts = optString("starts_at", "")
    val ends = optString("ends_at", "")
    return AthleteSlot(
        id = optInt("id"),
        title = optString("title", if (locked) "Uzamčeno" else "Trénink"),
        location = optString("location", ""),
        startsAt = starts,
        endsAt = ends,
        dateLabel = optString("date_label", starts.take(10)),
        timeLabel = optString("time_label", starts.drop(11).take(5)),
        status = if (locked) "locked" else optString("approval_status", "approved"),
        mine = optBoolean("is_mine", false),
        foreign = if (locked) true else optBoolean("is_foreign", false),
        canChange = optBoolean("can_request_change", false),
        canCancel = optBoolean("can_cancel_request", false),
        canEdit = optBoolean("can_edit", false),
        locked = locked || optBoolean("is_locked", false),
        startHour = optInt("start_hour", starts.drop(11).take(2).toIntOrNull() ?: 9),
        endHour = optInt("end_hour", ends.drop(11).take(2).toIntOrNull() ?: 10),
        seriesId = optString("series_id", "")
    )
}

@Composable
private fun AthleteReserveDialog(
    mode: String,
    title: String,
    confirm: String,
    initialDate: String,
    initialHour: Int,
    initialMinute: Int,
    initialLocation: String,
    initialEventTitle: String,
    eventId: Int,
    venues: List<VenueItem>,
    token: String,
    context: Context,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    var date by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf(listOf(0, 15, 30, 45).minBy { kotlin.math.abs(it - initialMinute) }) }
    var eventTitle by remember { mutableStateOf(when (initialEventTitle.trim().lowercase()) {
        "konzultační hodina", "konzultacni hodina" -> "Konzultační hodina"
        "jiné", "jine" -> "Jiné"
        else -> "Trénink"
    }) }
    var titleMenu by remember { mutableStateOf(false) }
    val locationChoices = remember(venues, initialLocation) {
        val items = venues.filter { it.name.isNotBlank() }.toMutableList()
        if (initialLocation.isNotBlank() && items.none { it.name == initialLocation }) {
            items.add(0, VenueItem(id = -1, name = initialLocation))
        }
        items
    }
    var selectedVenue by remember(locationChoices, initialLocation) {
        mutableStateOf(locationChoices.firstOrNull { it.name == initialLocation } ?: locationChoices.firstOrNull())
    }
    var venueMenu by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = { if (!busy) onDismiss() },
        title = { AppText(title, color = TrainerAppNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AppText("Délka je vždy 60 minut.", color = TrainerAppGray, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                AppText("Datum", fontSize = 12.sp, color = TrainerAppGray)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    colors = CardDefaults.cardColors(containerColor = TrainerAppLight)
                ) {
                    AppText(formatTrainingDate(date), modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                }
                if (showDatePicker) {
                    val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateStringToUtcMillis(date))
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                pickerState.selectedDateMillis?.let { date = utcMillisToDateString(it) }
                                showDatePicker = false
                            }) { AppText("Vybrat", fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = { TextButton(onClick = { showDatePicker = false }) { AppText("Zrušit") } }
                    ) { DatePicker(state = pickerState) }
                }
                Spacer(Modifier.height(8.dp))
                QuarterHourDropdown("Začátek", hour, minute, { hour = it }, { minute = it })
                Spacer(Modifier.height(8.dp))
                AppText("Typ události", fontSize = 12.sp, color = TrainerAppGray)
                Box {
                    Card(modifier = Modifier.fillMaxWidth().clickable { titleMenu = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                        AppText(eventTitle, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = titleMenu, onDismissRequest = { titleMenu = false }) {
                        listOf("Trénink", "Konzultační hodina", "Jiné").forEach { option ->
                            DropdownMenuItem(text = { AppText(option) }, onClick = { eventTitle = option; titleMenu = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                AppText("Místo", fontSize = 12.sp, color = TrainerAppGray)
                Box {
                    Card(modifier = Modifier.fillMaxWidth().clickable { venueMenu = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                        AppText(selectedVenue?.name ?: "Vyberte místo", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                    }
                    DropdownMenu(expanded = venueMenu, onDismissRequest = { venueMenu = false }) {
                        if (locationChoices.isEmpty()) {
                            DropdownMenuItem(text = { AppText("V databázi není žádné místo") }, onClick = { venueMenu = false })
                        }
                        locationChoices.forEach { venue ->
                            DropdownMenuItem(text = { AppText(if (venue.address.isBlank()) venue.name else "${venue.name} - ${venue.address}") }, onClick = {
                                selectedVenue = venue
                                venueMenu = false
                            })
                        }
                    }
                }
                AppText("Místo se bere z katalogu sportovišť.", color = TrainerAppGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    AppText(error, color = Color(0xFFB00020), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val place = selectedVenue?.name?.trim().orEmpty()
                    if (date.isBlank()) {
                        error = "Vyberte datum."
                        return@Button
                    }
                    if (place.isBlank()) {
                        error = "Vyberte místo z databáze."
                        return@Button
                    }
                    busy = true
                    error = ""
                    val stamp = String.format(Locale.US, "%s %02d:%02d:00", date, hour, minute)
                    runInBackground {
                        try {
                            val body = JSONObject().put("starts_at", stamp).put("location", place).put("title", eventTitle)
                            if (eventId > 0) body.put("event_id", eventId)
                            val result = athletePortal(context, token, mode, "POST", body)
                            val json = result.jsonOrNull()
                            runOnMain {
                                busy = false
                                if (json?.optBoolean("success", false) == true) onDone()
                                else error = json?.optString("error", apiErrorText(result, "Termín se nepodařilo uložit.")).orEmpty()
                            }
                        } catch (e: Exception) {
                            runOnMain {
                                busy = false
                                error = friendlyNetworkError(e)
                            }
                        }
                    }
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
            ) { AppText(confirm, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { AppText("Zavřít") } }
    )
}


@Composable
private fun AthleteChatPane(
    token: String,
    context: Context,
    coachName: String,
    coachPhoto: String,
    adminUnread: Int,
    coachUnread: Int,
    onUnread: (Int, Int) -> Unit
) {
    var channel by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf<List<AthleteChatLine>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val adminConversation = ChatConversation(
        id = "admin",
        name = "Administrátor TrainerApp",
        subtitle = "Systémová podpora a administrace",
        icon = "🛠️",
        isAdmin = true,
        unreadCount = adminUnread
    )
    val coachConversation = ChatConversation(
        id = "coach",
        name = coachName.ifBlank { "Trenér" },
        subtitle = "Chat s trenérem",
        icon = "👤",
        unreadCount = coachUnread,
        photoUrl = coachPhoto
    )

    LaunchedEffect(reload, token, channel) {
        if (channel != "admin" && channel != "coach") return@LaunchedEffect
        loading = lines.isEmpty()
        withContext(Dispatchers.IO) {
            try {
                val result = athletePortal(context, token, "chat", extraQuery = "with=$channel")
                val json = result.jsonOrNull()
                if (result.code !in 200..299 || json == null || !json.optBoolean("success", false)) {
                    runOnMain {
                        error = apiErrorText(result, "Chat se nepodařilo načíst.")
                        loading = false
                    }
                    return@withContext
                }
                val parsed = mutableListOf<AthleteChatLine>()
                val messages = json.optJSONArray("messages")
                if (messages != null) {
                    for (i in 0 until messages.length()) {
                        val item = messages.getJSONObject(i)
                        parsed.add(AthleteChatLine(item.optInt("id"), item.optString("body", ""), item.optString("created_at", ""), item.optBoolean("is_me", false)))
                    }
                }
                runOnMain {
                    lines = parsed
                    error = ""
                    loading = false
                    if (channel == "admin") onUnread(0, coachUnread) else onUnread(adminUnread, 0)
                }
            } catch (e: Exception) {
                runOnMain {
                    error = friendlyNetworkError(e)
                    loading = false
                }
            }
        }
    }
    LaunchedEffect(lines.size, channel) {
        if (channel.isNotBlank() && lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    if (channel.isBlank()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            AppText("Administrace a podpora", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            ConversationCard(adminConversation) {
                lines = emptyList()
                error = ""
                channel = "admin"
                reload += 1
            }
            Spacer(Modifier.height(20.dp))
            AppText("Chat s trenérem", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            ConversationCard(coachConversation) {
                lines = emptyList()
                error = ""
                channel = "coach"
                reload += 1
            }
        }
        return
    }

    val title = if (channel == "admin") "Administrátor TrainerApp" else coachName.ifBlank { "Trenér" }
    val subtitle = if (channel == "admin") "Systémová podpora a administrace" else "Chat s trenérem"
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { channel = ""; lines = emptyList(); draft = ""; error = "" }) { AppText("‹", color = TrainerAppNavy, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
            if (channel == "admin") {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(TrainerAppYellow), contentAlignment = Alignment.Center) {
                    AppText("🛠️", fontSize = 18.sp)
                }
            } else {
                ProfilePhoto(coachPhoto, title, 36.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                AppText(title, color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                AppText(subtitle, color = TrainerAppGray, fontSize = 12.sp)
            }
        }
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TrainerAppNavy, modifier = Modifier.size(22.dp))
            }
        }
        if (error.isNotBlank()) AppText(error, color = Color(0xFFB00020), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            items(lines, key = { "${channel}-${it.id}" }) { line ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (line.mine) Arrangement.End else Arrangement.Start) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (line.mine) TrainerAppNavy else Color.White)
                            .padding(12.dp)
                    ) {
                        AppText(line.body, color = if (line.mine) Color.White else TrainerAppNavy, fontSize = 15.sp)
                        AppText(line.createdAt.drop(11).take(5), color = if (line.mine) Color.White.copy(alpha = 0.7f) else TrainerAppGray, fontSize = 11.sp)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { AppText(if (channel == "admin") "Zpráva administrátorovi" else "Zpráva trenérovi") },
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val text = draft.trim()
                    if (text.isBlank()) return@Button
                    draft = ""
                    val target = channel
                    runInBackground {
                        try {
                            val result = athletePortal(context, token, "send", "POST", JSONObject().put("body", text).put("with", target))
                            val json = result.jsonOrNull()
                            runOnMain {
                                if (json?.optBoolean("success", false) == true) reload += 1
                                else error = json?.optString("error", "Zprávu se nepodařilo odeslat.").orEmpty()
                            }
                        } catch (e: Exception) {
                            runOnMain { error = friendlyNetworkError(e) }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
            ) { AppText("Odeslat", fontWeight = FontWeight.Bold) }
        }
    }
}


@Composable
private fun AthletePaymentsPane(token: String, context: Context) {
    var rows by remember { mutableStateOf<List<AthletePayRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var openMonth by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(token) {
        withContext(Dispatchers.IO) {
            try {
                val result = athletePortal(context, token, "payments")
                val json = result.jsonOrNull()
                if (result.code !in 200..299 || json == null || !json.optBoolean("success", false)) {
                    runOnMain {
                        error = apiErrorText(result, "Platby se nepodařilo načíst.")
                        loading = false
                    }
                    return@withContext
                }
                val parsed = mutableListOf<AthletePayRow>()
                val payments = json.optJSONArray("payments")
                if (payments != null) {
                    for (i in 0 until payments.length()) {
                        val item = payments.getJSONObject(i)
                        val missing = item.isNull("amount")
                        val trainings = mutableListOf<AthletePayTraining>()
                        val trainingArray = item.optJSONArray("trainings")
                        if (trainingArray != null) {
                            for (t in 0 until trainingArray.length()) {
                                val training = trainingArray.getJSONObject(t)
                                trainings.add(
                                    AthletePayTraining(
                                        title = training.optString("title", "Trénink"),
                                        location = training.optString("location", ""),
                                        startsAt = training.optString("starts_at", ""),
                                        endsAt = training.optString("ends_at", ""),
                                        paired = training.optBoolean("is_paired", false),
                                        makeup = training.optBoolean("is_makeup", false),
                                        amount = if (training.isNull("amount")) "—" else athleteMoney(training.optDouble("amount", 0.0), false),
                                        note = training.optString("note", "")
                                    )
                                )
                            }
                        }
                        parsed.add(
                            AthletePayRow(
                                month = item.optString("month", ""),
                                sessions = item.optInt("sessions", 0),
                                amount = athleteMoney(item.optDouble("amount", 0.0), missing),
                                status = item.optString("status", ""),
                                paidAt = item.optString("paid_at", ""),
                                trainings = trainings
                            )
                        )
                    }
                }
                runOnMain {
                    rows = parsed
                    error = ""
                    loading = false
                }
            } catch (e: Exception) {
                runOnMain {
                    error = friendlyNetworkError(e)
                    loading = false
                }
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        AppText("Měsíční platby", color = TrainerAppNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (loading) CircularProgressIndicator(color = TrainerAppNavy, modifier = Modifier.size(22.dp))
        if (error.isNotBlank()) AppText(error, color = Color(0xFFB00020), fontSize = 14.sp)
        if (!loading && rows.isEmpty() && error.isBlank()) AppText("Zatím tu nejsou žádné platby.", color = TrainerAppGray, fontSize = 14.sp)
        rows.forEach { row ->
            val opened = openMonth == row.month
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { openMonth = if (opened) null else row.month },
                colors = CardDefaults.cardColors(containerColor = MetalCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            AppText(athleteMonthLabel(row.month), color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        AppText(row.amount, color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        AppText(if (opened) "▲" else "▼", color = TrainerAppNavy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    AppText("${row.sessions} tréninků", color = TrainerAppGray, fontSize = 13.sp)
                    val paymentStatus = athletePaymentStatus(row.status)
                    AppText(paymentStatus.first, color = paymentStatus.second, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (row.status == "paid" && row.paidAt.isNotBlank()) AppText(formatPaidAt(row.paidAt), color = TrainerAppGray, fontSize = 12.sp)
                    if (opened) {
                        Spacer(Modifier.height(10.dp))
                        AppText("Tréninky v platbě", color = TrainerAppNavy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        if (row.trainings.isEmpty()) {
                            AppText("V tomto období nejsou uvedené schválené tréninky.", color = TrainerAppGray, fontSize = 13.sp)
                        } else {
                            row.trainings.forEach { training ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        AppText(athleteRange(training.startsAt, training.endsAt), color = TrainerAppNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        AppText(training.amount, color = TrainerAppNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (training.title.isNotBlank() && training.title != "Trénink") {
                                        AppText(training.title, color = TrainerAppNavy, fontSize = 13.sp)
                                    }
                                    AppText(training.location.ifBlank { "—" }, color = TrainerAppGray, fontSize = 12.sp)
                                    val kind = buildString {
                                        append(if (training.paired) "Párový" else "Individuální")
                                        append(if (training.makeup) " · náhradní" else " · běžný")
                                    }
                                    AppText(kind, color = TrainerAppGray, fontSize = 12.sp)
                                    if (training.note.isNotBlank()) AppText(training.note, color = Color(0xFFB45309), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AthleteWeightPane(token: String, context: Context, onSaved: () -> Unit) {
    var weight by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var range by remember { mutableStateOf("month") }
    var logs by remember { mutableStateOf<List<AthleteWeightRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload, token) {
        withContext(Dispatchers.IO) {
            try {
                val result = athletePortal(context, token, "weight")
                val json = result.jsonOrNull()
                if (result.code !in 200..299 || json == null || !json.optBoolean("success", false)) {
                    runOnMain {
                        error = apiErrorText(result, "Váhu se nepodařilo načíst.")
                        loading = false
                    }
                    return@withContext
                }
                val parsed = mutableListOf<AthleteWeightRow>()
                val items = json.optJSONArray("logs")
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        parsed.add(AthleteWeightRow(item.optString("measured_at", "").take(10), item.optDouble("weight_kg", 0.0).toFloat()))
                    }
                }
                runOnMain {
                    logs = parsed
                    error = ""
                    loading = false
                }
            } catch (e: Exception) {
                runOnMain {
                    error = friendlyNetworkError(e)
                    loading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        AppText("Zadat váhu", color = TrainerAppNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            modifier = Modifier.fillMaxWidth(),
            label = { AppText("Hmotnost v kg") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
            colors = CardDefaults.cardColors(containerColor = TrainerAppLight)
        ) {
            Column(Modifier.padding(12.dp)) {
                AppText("Datum", fontSize = 12.sp, color = TrainerAppGray)
                AppText(formatTrainingDate(date), fontWeight = FontWeight.Bold, color = TrainerAppNavy)
            }
        }
        if (showDatePicker) {
            val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateStringToUtcMillis(date))
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { date = utcMillisToDateString(it) }
                        showDatePicker = false
                    }) { AppText("Vybrat", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { AppText("Zrušit") }
                }
            ) { DatePicker(state = pickerState) }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                saving = true
                error = ""
                runInBackground {
                    try {
                        val body = JSONObject().put("weight_kg", weight.trim()).put("measured_at", date.trim())
                        val result = athletePortal(context, token, "save_weight", "POST", body)
                        val json = result.jsonOrNull()
                        runOnMain {
                            saving = false
                            if (json?.optBoolean("success", false) == true) {
                                weight = ""
                                reload += 1
                                onSaved()
                            } else {
                                error = json?.optString("error", apiErrorText(result, "Váhu se nepodařilo uložit.")).orEmpty()
                            }
                        }
                    } catch (e: Exception) {
                        runOnMain {
                            saving = false
                            error = friendlyNetworkError(e)
                        }
                    }
                }
            },
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
        ) { AppText(if (saving) "Ukládám…" else "Uložit váhu", fontWeight = FontWeight.Bold) }
        if (error.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            AppText(error, color = Color(0xFFB00020), fontSize = 14.sp)
        }
        Spacer(Modifier.height(18.dp))
        if (loading) {
            CircularProgressIndicator(color = TrainerAppNavy, modifier = Modifier.size(22.dp))
        } else {
            AthleteWeightChart(logs.sortedBy { it.measuredAt }, range) { range = it }
        }
    }
}

private fun formatPaidAt(raw: String): String {
    val date = raw.take(10)
    val time = raw.drop(11).take(5)
    val label = formatTrainingDate(date)
    return if (time.isBlank()) label else "$label $time"
}

private fun formatWeightKg(value: Float): String {
    val rounded = kotlin.math.round(value * 10f) / 10f
    return if (rounded % 1f == 0f) "${rounded.toInt()}" else String.format(Locale.forLanguageTag("cs-CZ"), "%.1f", rounded)
}

private fun filterWeightRows(rows: List<AthleteWeightRow>, range: String): List<AthleteWeightRow> {
    if (rows.size < 2 || range == "all") return rows
    val last = rows.last().measuredAt
    val parts = last.split("-")
    if (parts.size != 3) return rows
    val end = Calendar.getInstance()
    end.set(parts[0].toIntOrNull() ?: return rows, (parts[1].toIntOrNull() ?: return rows) - 1, parts[2].toIntOrNull() ?: return rows, 0, 0, 0)
    val start = end.clone() as Calendar
    when (range) {
        "week" -> start.add(Calendar.DAY_OF_YEAR, -7)
        "quarter" -> start.add(Calendar.MONTH, -4)
        "year" -> start.add(Calendar.MONTH, -12)
        else -> start.add(Calendar.DAY_OF_YEAR, -30)
    }
    return rows.filter { row ->
        val day = row.measuredAt.split("-")
        if (day.size != 3) return@filter false
        val point = Calendar.getInstance()
        point.set(day[0].toIntOrNull() ?: return@filter false, (day[1].toIntOrNull() ?: return@filter false) - 1, day[2].toIntOrNull() ?: return@filter false, 0, 0, 0)
        !point.before(start)
    }
}

private fun weightTrend(rows: List<AthleteWeightRow>): Pair<String, String> {
    if (rows.size < 2) return "Nedostatek dat" to ""
    val diff = rows.last().weightKg - rows.first().weightKg
    val label = when {
        kotlin.math.abs(diff) <= 1.5f -> "Stabilní váha"
        diff <= -1.51f -> "Hubnutí"
        else -> "Přibírání na váze"
    }
    val sign = if (diff > 0f) "+" else ""
    return label to "$sign${formatWeightKg(diff)} kg"
}

@Composable
private fun AthleteWeightChart(logs: List<AthleteWeightRow>, range: String, onRange: (String) -> Unit) {
    val ranges = listOf("week" to "Týden", "month" to "1 měsíc", "quarter" to "Čtvrtletí", "year" to "Rok", "all" to "Vše")
    val visible = filterWeightRows(logs, range)
    val trend = weightTrend(visible)
    var menu by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MetalCard), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            AppText("Vývoj tělesné hmotnosti", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            if (logs.isEmpty()) {
                AppText("Zatím nemáte zadané žádné váhové záznamy.", color = TrainerAppGray, fontSize = 14.sp)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Card(modifier = Modifier.clickable { menu = true }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            AppText("Období: ${ranges.firstOrNull { it.first == range }?.second ?: "1 měsíc"}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = TrainerAppNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            ranges.forEach { option ->
                                DropdownMenuItem(text = { AppText(option.second) }, onClick = { onRange(option.first); menu = false })
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        AppText(trend.first, color = TrainerAppNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (trend.second.isNotBlank()) AppText(trend.second, color = TrainerAppGray, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                WeightLineChart(visible)
            }
        }
    }
}

@Composable
private fun WeightLineChart(points: List<AthleteWeightRow>) {
    if (points.isEmpty()) {
        AppText("V tomto období není žádný záznam.", color = TrainerAppGray, fontSize = 13.sp)
        return
    }
    val minW = points.minOf { it.weightKg }
    val maxW = points.maxOf { it.weightKg }
    val span = (maxW - minW).let { if (it < 1f) 1f else it }
    val pad = span * 0.18f
    val yMin = minW - pad
    val yMax = maxW + pad
    val line = Color(0xFF10B981)
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.height(180.dp).padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
            AppText(formatWeightKg(maxW), color = TrainerAppGray, fontSize = 11.sp)
            AppText(formatWeightKg(minW), color = TrainerAppGray, fontSize = 11.sp)
        }
        Column(Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val top = 8.dp.toPx()
                val bottom = 8.dp.toPx()
                val plotH = size.height - top - bottom
                val plotW = size.width
                fun xAt(index: Int): Float = if (points.size == 1) plotW / 2f else plotW * index / (points.size - 1)
                fun yAt(value: Float): Float = top + plotH * (1f - (value - yMin) / (yMax - yMin))
                val linePath = Path()
                val fillPath = Path()
                points.forEachIndexed { index, point ->
                    val x = xAt(index)
                    val y = yAt(point.weightKg)
                    if (index == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(xAt(points.lastIndex), top + plotH)
                fillPath.lineTo(xAt(0), top + plotH)
                fillPath.close()
                drawPath(fillPath, line.copy(alpha = 0.18f))
                drawPath(linePath, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                points.forEachIndexed { index, point ->
                    drawCircle(line, radius = 3.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(xAt(index), yAt(point.weightKg)))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppText(formatTrainingDate(points.first().measuredAt), color = TrainerAppGray, fontSize = 11.sp)
                if (points.size > 1) AppText(formatTrainingDate(points.last().measuredAt), color = TrainerAppGray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AthleteBottomBar(current: String, unread: Int, onNavigate: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 5.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        NavigationItem("⌂", "Domů", current == "dashboard") { onNavigate("dashboard") }
        NavigationItem("📅", "Kalendář", current == "calendar") { onNavigate("calendar") }
        NavigationItem("💬", "Chat", current == "chat", badgeCount = unread) { onNavigate("chat") }
        NavigationItem("💳", "Platby", current == "payments") { onNavigate("payments") }
    }
}


@Composable
private fun BottomNavigationBar(current: String, unreadChatCount: Int = 0, pendingCalendarCount: Int = 0, onNavigate: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 5.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        NavigationItem("⌂", "Domů", current == "dashboard") { onNavigate("dashboard") }
        NavigationItem("👥", "Sportovci", current == "athletes") { onNavigate("athletes") }
        NavigationItem("📅", "Kalendář", current == "calendar", badgeCount = pendingCalendarCount) { onNavigate("calendar") }
        NavigationItem("💬", "Chat", current == "chat", badgeCount = unreadChatCount) { onNavigate("chat") }
    }
}

@Composable
private fun NavigationItem(icon: String, label: String, active: Boolean, badgeCount: Int = 0, onClick: () -> Unit) {
    Box(modifier = Modifier.clickable { onClick() }.padding(horizontal = 8.dp, vertical = 3.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppText(icon, fontSize = 21.sp)
            AppText(label, color = if (active) TrainerAppNavy else TrainerAppGray, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFB00020))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                AppText("$badgeCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuarterHourDropdown(
    label: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    var hourMenu by remember { mutableStateOf(false) }
    var minuteMenu by remember { mutableStateOf(false) }
    val minutes = listOf(0, 15, 30, 45)

    Column(modifier = Modifier.fillMaxWidth()) {
        AppText(label, fontSize = 12.sp, color = TrainerAppGray)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                Card(modifier = Modifier.fillMaxWidth().clickable { hourMenu = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                    AppText(String.format(Locale.getDefault(), "%02d h", hour), modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = hourMenu, onDismissRequest = { hourMenu = false }) {
                    (5..22).forEach { value ->
                        DropdownMenuItem(text = { AppText(String.format(Locale.getDefault(), "%02d", value)) }, onClick = { onHourChange(value); hourMenu = false })
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                Card(modifier = Modifier.fillMaxWidth().clickable { minuteMenu = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                    AppText(String.format(Locale.getDefault(), "%02d min", minute), modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = minuteMenu, onDismissRequest = { minuteMenu = false }) {
                    minutes.forEach { value ->
                        DropdownMenuItem(text = { AppText(String.format(Locale.getDefault(), "%02d", value)) }, onClick = { onMinuteChange(value); minuteMenu = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCalendarEventDialog(
    athletes: List<AthleteItem>,
    venues: List<VenueItem>,
    token: String,
    context: Context,
    initialDate: String,
    initialStartHour: Int,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var availableVenues by remember(venues) { mutableStateOf(venues) }

    LaunchedEffect(token) {
        if (token.isNotBlank()) {
            fetchVenuesFromApi(context, token, onSuccess = { fetched -> if (fetched.isNotEmpty()) availableVenues = fetched })
        }
    }

    var mode by remember { mutableStateOf("event") }
    val eventTypes = listOf("Trénink", "Konzultace", "Skupinová lekce", "Jiné")
    var selectedEventType by remember { mutableStateOf("Trénink") }
    var eventTypeDropdown by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf("") }
    var lockNote by remember { mutableStateOf("Uzamčený čas") }

    var selectedVenue by remember(availableVenues) { mutableStateOf<VenueItem?>(availableVenues.firstOrNull()) }
    var venueDropdown by remember { mutableStateOf(false) }
    var customLocation by remember { mutableStateOf("") }

    var athleteChosen by remember { mutableStateOf(false) }
    var selectedAthlete by remember { mutableStateOf<AthleteItem?>(null) }
    var athleteDropdown by remember { mutableStateOf(false) }

    var dateStr by remember { mutableStateOf(initialDate) }
    var startHour by remember { mutableIntStateOf(initialStartHour.coerceIn(5, 21)) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf((initialStartHour + 1).coerceIn(6, 22)) }
    var endMinute by remember { mutableIntStateOf(0) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { AppText(if (mode == "lock") "Uzamknout čas" else "Nová událost v kalendáři", color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { mode = "event" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mode == "event") TrainerAppYellow else TrainerAppLight,
                            contentColor = TrainerAppNavy
                        )
                    ) { AppText("Událost", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { mode = "lock" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mode == "lock") TrainerAppNavy else TrainerAppLight,
                            contentColor = if (mode == "lock") Color.White else TrainerAppNavy
                        )
                    ) { AppText("Uzamčení", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(10.dp))

                var showDatePicker by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    colors = CardDefaults.cardColors(containerColor = TrainerAppLight)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        AppText("Datum", fontSize = 12.sp, color = TrainerAppGray)
                        AppText(formatTrainingDate(dateStr), fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                    }
                }
                if (showDatePicker) {
                    val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateStringToUtcMillis(dateStr))
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                pickerState.selectedDateMillis?.let { dateStr = utcMillisToDateString(it) }
                                showDatePicker = false
                            }) { AppText("Vybrat", fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { AppText("Zrušit") }
                        }
                    ) {
                        DatePicker(state = pickerState)
                    }
                }
                Spacer(Modifier.height(8.dp))
                QuarterHourDropdown("Od", startHour, startMinute, { startHour = it }, { startMinute = it })
                Spacer(Modifier.height(8.dp))
                QuarterHourDropdown("Do", endHour, endMinute, { endHour = it }, { endMinute = it })
                Spacer(Modifier.height(8.dp))

                if (mode == "lock") {
                    OutlinedTextField(value = lockNote, onValueChange = { lockNote = it }, label = { AppText("Poznámka") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                } else {
                    AppText("Typ události:", fontSize = 12.sp, color = TrainerAppGray)
                    Box {
                        Card(modifier = Modifier.fillMaxWidth().clickable { eventTypeDropdown = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                            AppText(selectedEventType, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = eventTypeDropdown, onDismissRequest = { eventTypeDropdown = false }) {
                            eventTypes.forEach { type ->
                                DropdownMenuItem(text = { AppText(type) }, onClick = { selectedEventType = type; eventTypeDropdown = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (selectedEventType == "Jiné" || selectedEventType == "Konzultace") {
                        OutlinedTextField(value = customTitle, onValueChange = { customTitle = it }, label = { AppText("Vlastní název události") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                    }

                    AppText("Místo konání (Sportoviště):", fontSize = 12.sp, color = TrainerAppGray)
                    Box {
                        Card(modifier = Modifier.fillMaxWidth().clickable { venueDropdown = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                            AppText(selectedVenue?.name ?: if (customLocation.isNotBlank()) customLocation else "Vyberte místo...", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = venueDropdown, onDismissRequest = { venueDropdown = false }) {
                            availableVenues.forEach { v ->
                                DropdownMenuItem(text = { AppText(v.name) }, onClick = { selectedVenue = v; venueDropdown = false })
                            }
                            DropdownMenuItem(text = { AppText("➕ Vlastní místo konání") }, onClick = { selectedVenue = null; venueDropdown = false })
                        }
                    }
                    if (selectedVenue == null) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(value = customLocation, onValueChange = { customLocation = it }, label = { AppText("Zadejte vlastní místo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    Spacer(Modifier.height(8.dp))

                    AppText("Sportovec:", fontSize = 12.sp, color = TrainerAppGray)
                    Box {
                        val athleteLabel = when {
                            !athleteChosen -> "Vyberte"
                            selectedAthlete == null -> "Bez sportovce"
                            else -> selectedAthlete!!.name
                        }
                        Card(modifier = Modifier.fillMaxWidth().clickable { athleteDropdown = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                            AppText(athleteLabel, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = if (athleteChosen) TrainerAppNavy else TrainerAppGray)
                        }
                        DropdownMenu(expanded = athleteDropdown, onDismissRequest = { athleteDropdown = false }) {
                            DropdownMenuItem(text = { AppText("Bez sportovce") }, onClick = { selectedAthlete = null; athleteChosen = true; athleteDropdown = false })
                            athletes.forEach { ath ->
                                DropdownMenuItem(text = { AppText(ath.name) }, onClick = { selectedAthlete = ath; athleteChosen = true; athleteDropdown = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    AppText("Bez sportovce je termín jen váš a sportovcům se tento čas zablokuje.", fontSize = 12.sp, color = TrainerAppGray)
                }

                if (errorMsg.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    AppText(errorMsg, color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startTotal = startHour * 60 + startMinute
                    val endTotal = endHour * 60 + endMinute
                    if (endTotal <= startTotal) {
                        errorMsg = "Konec musí být později než začátek."
                        return@Button
                    }
                    if (mode == "event" && !athleteChosen) {
                        errorMsg = "Vyberte sportovce, nebo zvolte Bez sportovce."
                        return@Button
                    }
                    isSaving = true
                    errorMsg = ""
                    val startsAt = String.format(Locale.getDefault(), "%s %02d:%02d:00", dateStr, startHour, startMinute)
                    val endsAt = String.format(Locale.getDefault(), "%s %02d:%02d:00", dateStr, endHour, endMinute)
                    if (mode == "lock") {
                        lockCalendarApi(context, token, lockNote.ifBlank { "Uzamčený čas" }, startsAt, endsAt,
                            onSuccess = { isSaving = false; onCreated() },
                            onError = { err -> isSaving = false; errorMsg = err }
                        )
                    } else {
                        val athId = selectedAthlete?.id?.toIntOrNull()
                        val finalTitle = if (customTitle.isNotBlank()) customTitle else selectedEventType
                        val finalLocation = if (selectedVenue != null) selectedVenue!!.name else customLocation
                        createCalendarEventApi(context, token, athId, finalTitle, finalLocation, startsAt, endsAt,
                            onSuccess = { isSaving = false; onCreated() },
                            onError = { err -> isSaving = false; errorMsg = err }
                        )
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
            ) {
                AppText(if (mode == "lock") "Uzamknout" else "Vytvořit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AppText("Zrušit") } }
    )
}

private data class SlotTime(val date: String, val hour: Int, val minute: Int)

private fun parseSlotTime(raw: String, fallbackDate: String, fallbackHour: Int): SlotTime {
    val date = raw.trim().take(10).ifBlank { fallbackDate }
    val hour = raw.drop(11).take(2).toIntOrNull() ?: fallbackHour
    val minuteRaw = raw.drop(14).take(2).toIntOrNull() ?: 0
    val minute = listOf(0, 15, 30, 45).minBy { value -> if (value >= minuteRaw) value - minuteRaw else minuteRaw - value }
    return SlotTime(date, hour.coerceIn(5, 22), minute)
}

private fun slotStamp(date: String, hour: Int, minute: Int): String {
    return String.format(Locale.US, "%s %02d:%02d:00", date, hour, minute)
}

@Composable
private fun CalendarEventActionDialog(
    event: TrainingItem,
    athletes: List<AthleteItem>,
    actionHour: Int,
    token: String,
    context: Context,
    onDismiss: () -> Unit,
    onActionDone: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val startSlot = remember(event.id) { parseSlotTime(event.startsAt, event.date, event.startHour) }
    val endSlot = remember(event.id) { parseSlotTime(event.endsAt, event.date, event.endHour) }
    var dateStr by remember(event.id) { mutableStateOf(startSlot.date.ifBlank { event.date }) }
    var startHour by remember(event.id) { mutableIntStateOf(startSlot.hour) }
    var startMinute by remember(event.id) { mutableIntStateOf(startSlot.minute) }
    var endHour by remember(event.id) { mutableIntStateOf(endSlot.hour) }
    var endMinute by remember(event.id) { mutableIntStateOf(endSlot.minute) }
    var title by remember(event.id) { mutableStateOf(event.title.ifBlank { event.detail.substringBefore(" • ") }) }
    var location by remember(event.id) { mutableStateOf(event.location) }
    var athleteId by remember(event.id) { mutableIntStateOf(event.athleteId) }
    var showDatePicker by remember { mutableStateOf(false) }
    var athleteMenu by remember { mutableStateOf(false) }
    val pending = event.approvalStatus.equals("pending", true) && !event.isLocked

    fun approve(withEdits: Boolean) {
        val numId = event.id.toIntOrNull() ?: 0
        if (numId <= 0) {
            actionError = "Událost nemá platné ID."
            return
        }
        val starts = if (withEdits) slotStamp(dateStr, startHour, startMinute) else ""
        val ends = if (withEdits) slotStamp(dateStr, endHour, endMinute) else ""
        if (withEdits && ends <= starts) {
            actionError = "Konec musí být později než začátek."
            return
        }
        isProcessing = true
        actionError = ""
        approveCalendarEventApi(
            context, token, numId,
            startsAt = starts,
            endsAt = ends,
            title = if (withEdits) title.trim() else null,
            location = if (withEdits) location.trim() else null,
            athleteId = if (withEdits) athleteId else null,
            onSuccess = { isProcessing = false; onActionDone() },
            onError = { message -> isProcessing = false; actionError = message }
        )
    }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { AppText(if (event.isLocked) "Odemknout uzamčený čas" else if (pending) "Žádost sportovce" else event.detail, color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (event.isLocked) {
                    AppText("Uzamčený úsek: ${event.time}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    AppText("Chcete odemknout tento časový úsek (${event.time})?", fontSize = 13.sp, color = TrainerAppGray)
                } else if (!editing) {
                    AppText("Sportovec: ${event.athleteName}", fontSize = 14.sp)
                    AppText("Čas: ${event.time}", fontSize = 13.sp, color = TrainerAppGray)
                    if (event.detail.isNotBlank()) AppText(event.detail, fontSize = 13.sp, color = TrainerAppGray)
                    if (pending) {
                        Spacer(Modifier.height(8.dp))
                        AppText("Můžete žádost schválit, zamítnout, nebo upravit termín a pak schválit.", fontSize = 13.sp, color = TrainerAppNavy)
                    }
                } else {
                    AppText("Úprava před schválením", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                        Column(Modifier.padding(12.dp)) {
                            AppText("Datum", fontSize = 12.sp, color = TrainerAppGray)
                            AppText(formatTrainingDate(dateStr), fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    QuarterHourDropdown("Začátek", startHour, startMinute, { startHour = it }, { startMinute = it })
                    Spacer(Modifier.height(8.dp))
                    QuarterHourDropdown("Konec", endHour, endMinute, { endHour = it }, { endMinute = it })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { AppText("Název") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { AppText("Místo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    AppText("Sportovec", fontSize = 12.sp, color = TrainerAppGray)
                    Box {
                        Card(modifier = Modifier.fillMaxWidth().clickable { athleteMenu = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                            val selectedName = athletes.firstOrNull { it.id == athleteId.toString() }?.name ?: event.athleteName
                            AppText(selectedName.ifBlank { "Beze změny" }, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                        }
                        DropdownMenu(expanded = athleteMenu, onDismissRequest = { athleteMenu = false }) {
                            athletes.forEach { athlete ->
                                DropdownMenuItem(
                                    text = { AppText(athlete.name) },
                                    onClick = {
                                        athleteId = athlete.id.toIntOrNull() ?: athleteId
                                        athleteMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (actionError.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    AppText(actionError, fontSize = 13.sp, color = Color(0xFFB00020))
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (pending && !editing) {
                    Button(
                        onClick = { approve(false) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF137333))
                    ) { AppText("Schválit", color = Color.White) }
                    Button(
                        onClick = { editing = true; actionError = "" },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
                    ) { AppText("Schválit s úpravou", fontWeight = FontWeight.Bold) }
                }
                if (pending && editing) {
                    Button(
                        onClick = { approve(true) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF137333))
                    ) { AppText(if (isProcessing) "Schvaluji…" else "Uložit a schválit", color = Color.White) }
                    TextButton(onClick = { editing = false }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                        AppText("Zpět", color = TrainerAppNavy)
                    }
                }
                if (!editing) {
                    Button(
                        onClick = {
                            isProcessing = true
                            actionError = ""
                            val numId = event.id.toIntOrNull() ?: 0
                            val startTsStr = "${event.date} ${String.format(Locale.getDefault(), "%02d", actionHour)}:00:00"
                            val endTsStr = "${event.date} ${String.format(Locale.getDefault(), "%02d", actionHour + 1)}:00:00"
                            val rangeStart = if (event.isLocked) startTsStr else ""
                            val rangeEnd = if (event.isLocked) endTsStr else ""
                            deleteCalendarEventApi(context, token, numId, rangeStart, rangeEnd, reject = !event.isLocked,
                                onSuccess = { isProcessing = false; onActionDone() },
                                onError = { message -> isProcessing = false; actionError = message }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                    ) {
                        AppText(
                            when {
                                event.isLocked -> "Odemknout tuto hodinu"
                                pending -> "Zamítnout"
                                else -> "Smazat"
                            },
                            color = Color.White
                        )
                    }
                }
                if (event.isLocked && !editing) {
                    Button(
                        onClick = {
                            isProcessing = true
                            actionError = ""
                            val numId = event.id.toIntOrNull() ?: 0
                            deleteCalendarEventApi(context, token, numId, "", "",
                                onSuccess = { isProcessing = false; onActionDone() },
                                onError = { message -> isProcessing = false; actionError = message }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = TrainerAppNavy)
                    ) { AppText("Odemknout celý úsek", color = Color.White) }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { AppText("Zavřít", color = TrainerAppNavy) }
            }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateStringToUtcMillis(dateStr.ifBlank { event.date }))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateStr = utcMillisToDateString(it) }
                    showDatePicker = false
                }) { AppText("Vybrat", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { AppText("Zrušit") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun ChatListScreen(
    conversations: List<ChatConversation>,
    unreadChatCount: Int,
    pendingCalendarCount: Int,
    onSelectConversation: (ChatConversation) -> Unit,
    onNavigate: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().background(MetalHeaderBrush).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                AppText("💬", fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    AppText("Chat & Zprávy", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    AppText("Komunikace s administrátorem a sportovci", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                AppText("Administrace & Podpora", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val adminConv = conversations.firstOrNull { it.isAdmin }
                if (adminConv != null) ConversationCard(adminConv) { onSelectConversation(adminConv) }
                Spacer(Modifier.height(20.dp))
                AppText("Chat se sportovci", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val athleteConvs = conversations.filter { !it.isAdmin }
                if (athleteConvs.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MetalCard), shape = RoundedCornerShape(16.dp)) {
                        AppText("Pro započetí chatu se sportovci je potřeba mít načtené sportovce z účtu.", color = TrainerAppGray, modifier = Modifier.padding(16.dp), fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    athleteConvs.forEach { conv -> ConversationCard(conv) { onSelectConversation(conv) }; Spacer(Modifier.height(8.dp)) }
                }
            }
            BottomNavigationBar(current = "chat", unreadChatCount = unreadChatCount, pendingCalendarCount = pendingCalendarCount, onNavigate = onNavigate)
        }
    }
}

@Composable
private fun ConversationCard(conversation: ChatConversation, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MetalCard), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (conversation.photoUrl.isNotBlank()) {
                ProfilePhoto(conversation.photoUrl, conversation.name, 46.dp)
            } else {
                Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(if (conversation.isAdmin) TrainerAppYellow else TrainerAppNavy), contentAlignment = Alignment.Center) {
                    AppText(conversation.icon, fontSize = 22.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(conversation.name, color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (conversation.unreadCount > 0) {
                        Box(modifier = Modifier.clip(CircleShape).background(Color(0xFFB00020)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            AppText("${conversation.unreadCount}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                AppText(conversation.subtitle, color = TrainerAppGray, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            AppText("▶", color = TrainerAppGray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ChatBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val path = Path().apply {
                moveTo(size.width * 0.68f, size.height * 0.16f)
                lineTo(size.width * 0.30f, size.height * 0.50f)
                lineTo(size.width * 0.68f, size.height * 0.84f)
            }
            drawPath(path, color = Color.White, style = stroke)
        }
    }
}

@Composable
private fun ChatDetailScreen(
    conversation: ChatConversation,
    token: String,
    context: Context,
    unreadChatCount: Int,
    pendingCalendarCount: Int,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onConversationRead: () -> Unit
) {
    var messages by remember(conversation.id) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf("") }
    var didInitialScroll by remember(conversation.id) { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(conversation.id) {
        cancelSystemNotification(context, 2001)
        onConversationRead()
    }

    LaunchedEffect(conversation.id, token) {
        if (token.isNotBlank()) {
            fetchChatMessagesFromApi(
                context, token, conversation.id,
                onSuccess = { fetchedMsgs -> messages = fetchedMsgs; chatError = "" },
                onError = { chatError = it }
            )
            while (true) {
                delay(5000)
                fetchChatMessagesFromApi(
                    context, token, conversation.id,
                    onSuccess = { updated -> messages = updated; chatError = "" },
                    onError = { chatError = it }
                )
            }
        }
    }

    LaunchedEffect(messages.size, conversation.id) {
        if (messages.isEmpty()) return@LaunchedEffect
        val target = if (!didInitialScroll) {
            didInitialScroll = true
            val unreadIndex = messages.indexOfFirst { it.wasUnread && !it.isMe }
            if (unreadIndex >= 0) unreadIndex else messages.lastIndex
        } else if (messages.lastOrNull()?.isMe == true) {
            messages.lastIndex
        } else {
            return@LaunchedEffect
        }
        listState.animateScrollToItem(target)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().background(MetalHeaderBrush).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                ChatBackButton(onClick = onBack)
                Spacer(Modifier.width(6.dp))
                if (conversation.photoUrl.isNotBlank()) {
                    ProfilePhoto(conversation.photoUrl, conversation.name, 40.dp)
                } else {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (conversation.isAdmin) TrainerAppYellow else Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        AppText(conversation.icon, fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    AppText(conversation.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    AppText(if (conversation.isAdmin) "Aktivní podpora" else "Aktivní chat", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                if (chatError.isNotBlank()) {
                    item { AppText(chatError, color = Color(0xFFB00020), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                }
                itemsIndexed(messages, key = { index, msg -> "${msg.id}-$index" }) { _, msg ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start) {
                        Card(colors = CardDefaults.cardColors(containerColor = if (msg.isMe) TrainerAppNavy else Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(0.82f)) {
                            Column(Modifier.padding(12.dp)) {
                                AppText(
                                    text = if (msg.isMe) "Trenér (Vy)" else msg.senderName,
                                    color = if (msg.isMe) TrainerAppYellow else TrainerAppNavy,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(3.dp))
                                AppText(msg.text, color = if (msg.isMe) Color.White else TrainerAppNavy, fontSize = 14.sp)
                                Spacer(Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                    AppText(msg.time, color = if (msg.isMe) Color.White.copy(alpha = 0.65f) else TrainerAppGray, fontSize = 10.sp)
                                    if (msg.isMe) {
                                        Spacer(Modifier.width(4.dp))
                                        AppText(if (msg.isRead) "✓✓" else "✓", color = if (msg.isRead) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.65f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = messageText, onValueChange = { messageText = it }, modifier = Modifier.weight(1f), placeholder = { AppText("Napište zprávu...") }, singleLine = true, enabled = !isSending, shape = RoundedCornerShape(20.dp))
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val clean = messageText.trim()
                        if (clean.isNotEmpty() && !isSending) {
                            isSending = true
                            val tempMsg = ChatMessage(System.currentTimeMillis().toString(), "Trenér (Vy)", true, false, clean, "Nyní")
                            messages = messages + tempMsg
                            messageText = ""
                            sendChatMessageApi(context, token, conversation.id, clean,
                                onSuccess = {
                                    isSending = false
                                    chatError = ""
                                    fetchChatMessagesFromApi(
                                        context, token, conversation.id,
                                        onSuccess = { updated -> messages = updated },
                                        onError = { chatError = it }
                                    )
                                },
                                onError = { err -> isSending = false; chatError = err }
                            )
                        }
                    },
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (isSending) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TrainerAppNavy, strokeWidth = 2.dp)
                    else AppText("Odeslat", fontWeight = FontWeight.Bold)
                }
            }
            BottomNavigationBar(current = "chat", unreadChatCount = unreadChatCount, pendingCalendarCount = pendingCalendarCount, onNavigate = onNavigate)
        }
    }
}
