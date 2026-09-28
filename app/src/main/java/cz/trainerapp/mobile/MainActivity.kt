package cz.trainerapp.mobile

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
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

private val TrainerAppNavy = Color(0xFF0B1B45)
private val TrainerAppYellow = Color(0xFFFFC107)
private val TrainerAppLight = Color(0xFFF5F7FA)
private val TrainerAppGray = Color(0xFF718096)

private const val PREFS_NAME = "trainerapp_session"
private const val PREF_TOKEN = "token"
private const val PREF_TYPE = "account_type"
private const val PREF_NAME = "trainer_name"
private const val PREF_ID = "trainer_id"
private const val PREF_BASE_URL = "base_url"

private const val DEFAULT_BASE_URL = "http://10.0.2.2/TrainerApp_v.3/api/mobile"
private const val ONLINE_BASE_URL = "https://www.reservio.online/api/mobile"

private fun getApiBaseUrl(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(PREF_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
}

private fun setApiBaseUrl(context: Context, url: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(PREF_BASE_URL, url.trimEnd('/')).apply()
}

data class AthleteItem(
    val id: String,
    val name: String,
    val detail: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = ""
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
    val endHour: Int = 10
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
    val series: List<SessionSeriesItem> = emptyList()
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
    val time: String
)

data class ChatConversation(
    val id: String,
    val name: String,
    val subtitle: String,
    val icon: String,
    val isAdmin: Boolean = false,
    val unreadCount: Int = 0,
    val initialMessages: List<ChatMessage> = emptyList()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)

        setContent {
            TrainerAppTheme {
                TrainerAppRoot(this)
            }
        }
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

            list.add(AthleteItem(id, fullName, detailText, email, phone, photo))
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
    var lastLoginScreen by remember { mutableStateOf("trainer_login") }
    var currentBaseUrl by remember { mutableStateOf(getApiBaseUrl(context)) }

    var athletesList by remember { mutableStateOf<List<AthleteItem>>(emptyList()) }
    var workoutSetsList by remember { mutableStateOf<List<WorkoutSetItem>>(emptyList()) }
    var venuesList by remember { mutableStateOf<List<VenueItem>>(emptyList()) }
    var trainingsList by remember { mutableStateOf<List<TrainingItem>>(emptyList()) }
    var conversationsList by remember { mutableStateOf<List<ChatConversation>>(emptyList()) }
    var activeSessionId by remember { mutableIntStateOf(0) }
    var selectedChatConversationId by remember { mutableStateOf<String?>(null) }
    var isLoadingData by remember { mutableStateOf(false) }

    var lastSeenUnreadCount by remember { mutableIntStateOf(0) }
    var lastSeenCalendarCount by remember { mutableIntStateOf(0) }

    val totalUnreadChatCount = remember(conversationsList) {
        conversationsList.sumOf { it.unreadCount }
    }

    fun refreshApiData() {
        if (token.isBlank()) return
        isLoadingData = true

        fetchUserDataFromApi(
            context = context,
            token = token,
            onSuccess = { fetchedName ->
                if (fetchedName.isNotBlank()) {
                    trainerName = fetchedName
                    prefs.edit().putString(PREF_NAME, fetchedName).apply()
                }
            },
            onError = {}
        )

        fetchAthletesFromApi(
            context = context,
            token = token,
            onSuccess = { athletes ->
                athletesList = athletes

                fetchCalendarFromApi(
                    context = context,
                    token = token,
                    onSuccess = { calEvents ->
                        if (lastSeenCalendarCount > 0 && calEvents.size > lastSeenCalendarCount) {
                            sendSystemNotification(context, "Kalendář TrainerApp", "Máte novou událost nebo změnu v kalendáři.", 2002)
                        }
                        lastSeenCalendarCount = calEvents.size
                        trainingsList = calEvents
                        isLoadingData = false
                    }
                )
            },
            onError = { isLoadingData = false }
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

        fetchChatConversationsFromApi(
            context = context,
            token = token,
            onSuccess = { convs ->
                if (convs.isNotEmpty()) {
                    conversationsList = convs
                    val newUnread = convs.sumOf { it.unreadCount }
                    if (newUnread > lastSeenUnreadCount && lastSeenUnreadCount >= 0) {
                        sendSystemNotification(context, "TrainerApp Chat", "Máte novou nepřečtenou zprávu v chatu.", 2001)
                    }
                    lastSeenUnreadCount = newUnread
                }
            }
        )
    }

    LaunchedEffect(token, currentBaseUrl) {
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

    var screen by remember { mutableStateOf(if (token.isNotBlank()) "dashboard" else "home") }

    when (screen) {
        "home" -> HomeScreen(
            currentBaseUrl = currentBaseUrl,
            onBaseUrlChange = { newUrl ->
                currentBaseUrl = newUrl
                setApiBaseUrl(context, newUrl)
            },
            onTrainer = { screen = "trainer_login" },
            onAthlete = { screen = "athlete_login" }
        )

        "trainer_login" -> {
            lastLoginScreen = "trainer_login"
            LoginScreen(
                context = context,
                subtitle = "Přihlášení pro trenéry",
                description = "Zadejte své trenérské přihlašovací údaje (server: $currentBaseUrl).",
                currentBaseUrl = currentBaseUrl,
                onBaseUrlChange = { newUrl ->
                    currentBaseUrl = newUrl
                    setApiBaseUrl(context, newUrl)
                },
                onBack = { screen = "home" },
                onForgotPassword = { screen = "forgot_password" },
                onLoginSuccess = { name, id, newToken, jsonResp ->
                    trainerName = name
                    trainerId = id
                    token = newToken
                    accountType = "trainer"

                    val parsedAthletes = parseAthletesFromJson(jsonResp)
                    if (parsedAthletes.isNotEmpty()) athletesList = parsedAthletes

                    prefs.edit()
                        .putString(PREF_TOKEN, newToken)
                        .putString(PREF_TYPE, "trainer")
                        .putString(PREF_NAME, name)
                        .putString(PREF_ID, id)
                        .apply()

                    screen = "dashboard"
                }
            )
        }

        "athlete_login" -> {
            lastLoginScreen = "athlete_login"
            LoginScreen(
                context = context,
                subtitle = "Přihlášení pro sportovce",
                description = "Zadejte své přihlašovací údaje sportovce (server: $currentBaseUrl).",
                currentBaseUrl = currentBaseUrl,
                onBaseUrlChange = { newUrl ->
                    currentBaseUrl = newUrl
                    setApiBaseUrl(context, newUrl)
                },
                onBack = { screen = "home" },
                onForgotPassword = { screen = "forgot_password" },
                onLoginSuccess = { name, id, newToken, jsonResp ->
                    trainerName = name
                    trainerId = id
                    token = newToken
                    accountType = "athlete"

                    val parsedAthletes = parseAthletesFromJson(jsonResp)
                    if (parsedAthletes.isNotEmpty()) athletesList = parsedAthletes

                    prefs.edit()
                        .putString(PREF_TOKEN, newToken)
                        .putString(PREF_TYPE, "athlete")
                        .putString(PREF_NAME, name)
                        .putString(PREF_ID, id)
                        .apply()

                    screen = "dashboard"
                }
            )
        }

        "forgot_password" -> ForgotPasswordScreen(onBack = { screen = lastLoginScreen })

        "dashboard" -> TrainerDashboard(
            trainerName = trainerName,
            accountType = accountType,
            trainings = trainingsList,
            unreadChatCount = totalUnreadChatCount,
            isLoading = isLoadingData,
            currentBaseUrl = currentBaseUrl,
            onBaseUrlChange = { newUrl ->
                currentBaseUrl = newUrl
                setApiBaseUrl(context, newUrl)
                refreshApiData()
            },
            onRefresh = { refreshApiData() },
            onNavigate = { screen = it },
            onLogout = {
                if (token.isNotBlank()) logoutFromApi(context, token)
                prefs.edit().clear().apply()
                token = ""
                trainerName = ""
                trainerId = ""
                accountType = "trainer"
                athletesList = emptyList()
                workoutSetsList = emptyList()
                trainingsList = emptyList()
                conversationsList = emptyList()
                screen = "home"
            }
        )

        "athletes" -> AthletesSectionScreen(
            athletes = athletesList,
            token = token,
            context = context,
            unreadChatCount = totalUnreadChatCount,
            isLoading = isLoadingData,
            onRefresh = { refreshApiData() },
            onNavigate = { screen = it },
            onOpenChatWithAthlete = { athleteId ->
                selectedChatConversationId = athleteId
                screen = "chat"
            }
        )

        "calendar" -> CalendarSectionScreen(
            trainings = trainingsList,
            athletes = athletesList,
            venues = venuesList,
            token = token,
            context = context,
            unreadChatCount = totalUnreadChatCount,
            isLoading = isLoadingData,
            onRefresh = { refreshApiData() },
            onNavigate = { screen = it }
        )

        "chat" -> ChatSectionScreen(
            athletes = athletesList,
            initialConversations = conversationsList,
            selectedConversationId = selectedChatConversationId,
            unreadChatCount = totalUnreadChatCount,
            token = token,
            context = context,
            onNavigate = { screen = it }
        )

        "training" -> TrainingScreen(
            athletes = athletesList,
            workoutSets = workoutSetsList,
            venues = venuesList,
            token = token,
            context = context,
            activeSessionId = activeSessionId,
            unreadChatCount = totalUnreadChatCount,
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

        else -> { screen = "dashboard" }
    }
}

@Composable
private fun HomeScreen(currentBaseUrl: String, onBaseUrlChange: (String) -> Unit, onTrainer: () -> Unit, onAthlete: () -> Unit) {
    var showServerDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppNavy) {
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
            TextButton(onClick = { showServerDialog = true }) {
                AppText("⚙️ Server: ${if (currentBaseUrl.contains("reservio.online")) "Online (reservio.online)" else "Lokální WAMP"}", color = TrainerAppYellow, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            AppText("Mobilní aplikace TrainerApp", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
        }
    }

    if (showServerDialog) {
        ServerConfigDialog(
            currentBaseUrl = currentBaseUrl,
            onDismiss = { showServerDialog = false },
            onSelect = { url ->
                onBaseUrlChange(url)
                showServerDialog = false
            }
        )
    }
}

@Composable
private fun ServerConfigDialog(currentBaseUrl: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var customUrl by remember { mutableStateOf(currentBaseUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Nastavení serveru (API Base URL)", color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                AppText("Vyberte připojení k serveru:", fontSize = 13.sp, color = TrainerAppGray)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onSelect(DEFAULT_BASE_URL) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppNavy)
                ) {
                    AppText("Lokální WAMP (Emulator: http://10.0.2.2/...)", color = Color.White, fontSize = 12.sp)
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { onSelect(ONLINE_BASE_URL) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
                ) {
                    AppText("Online (https://www.reservio.online/...)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { AppText("Vlastní URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(customUrl) }, colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)) {
                AppText("Použít", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AppText("Zrušit") } }
    )
}

@Composable
private fun LoginScreen(
    context: Context,
    subtitle: String,
    description: String,
    currentBaseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    onBack: () -> Unit,
    onForgotPassword: () -> Unit,
    onLoginSuccess: (String, String, String, JSONObject) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showServerDialog by remember { mutableStateOf(false) }

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
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { showServerDialog = true }, modifier = Modifier.fillMaxWidth()) {
                AppText("⚙️ Server: ${if (currentBaseUrl.contains("reservio.online")) "Online" else "Lokální WAMP"}", fontSize = 13.sp)
            }
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMessage = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { AppText("Uživatelské jméno / E-mail") },
                singleLine = true,
                enabled = !loading
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { AppText("Heslo") },
                singleLine = true,
                enabled = !loading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        AppText(if (passwordVisible) "🙈" else "👁", fontSize = 20.sp)
                    }
                }
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val cleanUser = username.trim()
                    if (cleanUser.isEmpty()) errorMessage = "Zadejte uživatelské jméno."
                    else if (password.isEmpty()) errorMessage = "Zadejte heslo."
                    else {
                        loading = true
                        errorMessage = ""
                        loginToApi(username = cleanUser, password = password, context = context,
                            onSuccess = { name, id, newToken, jsonResp ->
                                loading = false
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

    if (showServerDialog) {
        ServerConfigDialog(
            currentBaseUrl = currentBaseUrl,
            onDismiss = { showServerDialog = false },
            onSelect = { url ->
                onBaseUrlChange(url)
                showServerDialog = false
            }
        )
    }
}

@Composable
private fun ForgotPasswordScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppNavy) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.Center) {
            AppText("Obnova hesla", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            AppText("Zadejte e-mail, na který vám zašleme instrukce.", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
            Spacer(Modifier.height(25.dp))

            Column(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(20.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it; infoMessage = "" }, modifier = Modifier.fillMaxWidth(), label = { AppText("E-mail") }, singleLine = true)
                if (infoMessage.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isError) Color(0xFFFFE3E3) else Color(0xFFE6F4EA)), shape = RoundedCornerShape(10.dp)) {
                        AppText(infoMessage, color = if (isError) Color(0xFFB00020) else Color(0xFF137333), modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        val clean = email.trim()
                        if (clean.isEmpty() || !clean.contains("@")) { isError = true; infoMessage = "Zadejte platný e-mail." }
                        else { isError = false; infoMessage = "Instrukce k obnově hesla byly odeslány na e-mail." }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
                ) { AppText("Odeslat instrukce", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { AppText("Zpět na přihlášení") }
            }
        }
    }
}

@Composable
private fun TrainerDashboard(
    trainerName: String,
    accountType: String,
    trainings: List<TrainingItem>,
    unreadChatCount: Int,
    isLoading: Boolean,
    currentBaseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val subtitleText = if (accountType == "athlete") "Sportovní dashboard" else "Trenérský dashboard"
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time) }
    var showServerDialog by remember { mutableStateOf(false) }

    val todayEvents = remember(trainings, todayDateStr) {
        trainings.filter { (it.date == todayDateStr || it.date.isBlank()) && !it.isLocked }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(modifier = Modifier.fillMaxWidth().background(TrainerAppNavy).padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        AppText("Dobrý den${if (trainerName.isNotBlank()) ", $trainerName" else ""} 👋", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        AppText(subtitleText, color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showServerDialog = true }) { AppText("⚙️", fontSize = 18.sp) }
                        IconButton(onClick = onRefresh) { AppText("🔄", fontSize = 20.sp) }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                AppText("Dnešní události v kalendáři", color = TrainerAppNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TrainerAppNavy)
                    }
                } else if (todayEvents.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            AppText("📅", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            AppText("Na dnešek nemáte naplánované žádné události v kalendáři.", color = TrainerAppNavy, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(4.dp))
                            AppText("Server: $currentBaseUrl", color = TrainerAppGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    todayEvents.forEach { training ->
                        TodayTrainingCard(training.time, training.athleteName, training.detail) { onNavigate("calendar") }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Spacer(Modifier.height(22.dp))
                AppText("Rychlý přístup", color = TrainerAppNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardTile("👥", "Sportovci", Modifier.weight(1f)) { onNavigate("athletes") }
                    DashboardTile("📅", "Kalendář", Modifier.weight(1f)) { onNavigate("calendar") }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardTile("💬", "Chat", Modifier.weight(1f), badgeCount = unreadChatCount) { onNavigate("chat") }
                    DashboardTile("🏋️", "Spustit trénink", Modifier.weight(1f)) { onNavigate("training") }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { AppText("Odhlásit se") }
            }

            BottomNavigationBar(current = "dashboard", unreadChatCount = unreadChatCount, onNavigate = onNavigate)
        }
    }

    if (showServerDialog) {
        ServerConfigDialog(
            currentBaseUrl = currentBaseUrl,
            onDismiss = { showServerDialog = false },
            onSelect = { url ->
                onBaseUrlChange(url)
                showServerDialog = false
            }
        )
    }
}

@Composable
private fun AthletesSectionScreen(
    athletes: List<AthleteItem>,
    token: String,
    context: Context,
    unreadChatCount: Int,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenChatWithAthlete: (String) -> Unit
) {
    var selectedAthleteDetail by remember { mutableStateOf<AthleteFullDetail?>(null) }
    var isLoadingDetail by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().background(TrainerAppNavy).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                AppText("👥", fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    AppText("Sportovci", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    AppText("Seznam registrovaných sportovců z reservio.online", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
                IconButton(onClick = onRefresh) { AppText("🔄", fontSize = 20.sp) }
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TrainerAppNavy)
                    }
                } else if (athletes.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
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
                        AthleteCard(athlete) {
                            isLoadingDetail = true
                            fetchAthleteFullDetailApi(context, token, athlete.id,
                                onSuccess = { fullDetail ->
                                    isLoadingDetail = false
                                    selectedAthleteDetail = fullDetail
                                },
                                onError = {
                                    isLoadingDetail = false
                                    selectedAthleteDetail = AthleteFullDetail(athlete.id, athlete.name, athlete.email, athlete.phone)
                                }
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            BottomNavigationBar(current = "athletes", unreadChatCount = unreadChatCount, onNavigate = onNavigate)
        }
    }

    if (selectedAthleteDetail != null) {
        AthleteDetailDialog(
            detail = selectedAthleteDetail!!,
            onDismiss = { selectedAthleteDetail = null },
            onOpenChat = { athleteId ->
                selectedAthleteDetail = null
                onOpenChatWithAthlete(athleteId)
            }
        )
    }
}

@Composable
private fun AthleteCard(athlete: AthleteItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(TrainerAppNavy), contentAlignment = Alignment.Center) {
                    AppText(athlete.name.take(1).uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    AppText(athlete.name, color = TrainerAppNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (athlete.email.isNotBlank()) AppText(athlete.email, color = TrainerAppGray, fontSize = 13.sp)
                }
                AppText("▶", color = TrainerAppGray, fontSize = 14.sp)
            }
            if (athlete.phone.isNotBlank() || athlete.detail.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                if (athlete.phone.isNotBlank()) AppText("📞 ${athlete.phone}", color = TrainerAppNavy, fontSize = 13.sp)
                if (athlete.detail.isNotBlank()) AppText("📝 ${athlete.detail}", color = TrainerAppGray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AthleteDetailDialog(
    detail: AthleteFullDetail,
    onDismiss: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText("👤", fontSize = 24.sp)
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
                AppText("Historie váhy", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (detail.weightLogs.isEmpty()) {
                    AppText("Žádné záznamy o váze.", fontSize = 12.sp, color = TrainerAppGray)
                } else {
                    detail.weightLogs.take(5).forEach { log ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            AppText(log.date, fontSize = 12.sp, color = TrainerAppGray)
                            AppText("${log.weightKg} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                AppText("Historie tréninků", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (detail.trainings.isEmpty()) {
                    AppText("Žádná historie tréninků.", fontSize = 12.sp, color = TrainerAppGray)
                } else {
                    detail.trainings.take(8).forEach { tr ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                            Column(Modifier.padding(10.dp)) {
                                AppText(tr.time, fontSize = 11.sp, color = TrainerAppGray)
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
    var selectedEventForAction by remember { mutableStateOf<TrainingItem?>(null) }
    var selectedActionHour by remember { mutableIntStateOf(0) }

    val hours = (5..21).toList()

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF9F6F0)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(weekHeader, color = TrainerAppNavy, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { weekOffset-- }, modifier = Modifier.size(36.dp)) {
                        AppText("←", color = TrainerAppNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { weekOffset++ }, modifier = Modifier.size(36.dp)) {
                        AppText("→", color = TrainerAppNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showCreateDialog = true }, modifier = Modifier.size(36.dp)) {
                        AppText("➕", fontSize = 18.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEachIndexed { index, (dayLabel, dateStr) ->
                    val isSelected = index == selectedDayIndex
                    val isToday = dateStr == todayDateStr
                    val hasEvents = trainings.any { it.date == dateStr }

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
                                        isToday -> Color(0xFFFFECB3)
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

                        if (hasEvents) {
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) TrainerAppNavy else TrainerAppYellow)
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

                        Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFFF2EFE8)).border(0.5.dp, Color(0xFFE0DCD3))) {
                            if (hourEvents.isNotEmpty()) {
                                hourEvents.forEach { ev ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clickable { 
                                                selectedEventForAction = ev 
                                                selectedActionHour = hour
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = when {
                                                ev.isLocked -> Color(0xFFD6D3CC)
                                                ev.approvalStatus == "pending" -> Color(0xFFFFECB3)
                                                else -> TrainerAppYellow
                                            }
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(Modifier.padding(6.dp)) {
                                            AppText(
                                                text = if (ev.isLocked) "🔒 ${ev.detail}" else "${ev.time} - ${ev.athleteName}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TrainerAppNavy
                                            )
                                            if (!ev.isLocked && ev.detail.isNotBlank()) {
                                                AppText(ev.detail, fontSize = 10.sp, color = TrainerAppNavy.copy(alpha = 0.8f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            BottomNavigationBar(current = "calendar", unreadChatCount = unreadChatCount, onNavigate = onNavigate)
        }
    }

    if (showCreateDialog) {
        CreateCalendarEventDialog(
            athletes = athletes,
            venues = venues,
            token = token,
            context = context,
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
    athletes: List<AthleteItem>,
    initialConversations: List<ChatConversation>,
    selectedConversationId: String?,
    unreadChatCount: Int,
    token: String,
    context: Context,
    onNavigate: (String) -> Unit
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
        ChatDetailScreen(conversation = selectedConversation!!, token = token, context = context, onBack = { selectedConversation = null })
    } else {
        ChatListScreen(
            conversations = if (conversations.isNotEmpty()) conversations else createDefaultConversations(athletes),
            unreadChatCount = unreadChatCount,
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
    unreadChatCount: Int,
    onSessionStarted: (Int) -> Unit,
    onSessionCompleted: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var selectedAthlete by remember { mutableStateOf<AthleteItem?>(athletes.firstOrNull()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var pendingSetToStart by remember { mutableStateOf<WorkoutSetItem?>(null) }

    val activeSets = if (workoutSets.isNotEmpty()) workoutSets else listOf(
        WorkoutSetItem(1, "Silový trénink", "60 min • 8 cviků", 8, isGlobal = false),
        WorkoutSetItem(2, "Full Body", "55 min • 10 cviků", 10, isGlobal = false),
        WorkoutSetItem(3, "Hyrox", "75 min • běh + stanoviště", 12, isGlobal = false),
        WorkoutSetItem(4, "Kondiční trénink", "45 min • 6 cviků", 6, isGlobal = false)
    )

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
            Row(modifier = Modifier.fillMaxWidth().background(TrainerAppNavy).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                AppText("🏋️", fontSize = 30.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    AppText("Spustit trénink", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    AppText("Vyberte sportovce a tréninkovou sadu", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp)) {
                AppText("Vyberte sportovce", color = TrainerAppNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                if (athletes.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                        AppText("Žádní sportovci nenačteni. Nejprve obnovte seznam sportovců.", color = TrainerAppGray, modifier = Modifier.padding(14.dp), fontSize = 14.sp)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppText("👤", fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                AppText(selectedAthlete?.name ?: "Vyberte sportovce", color = TrainerAppNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                } else {
                    activeSets.forEach { set ->
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

            BottomNavigationBar(current = "", unreadChatCount = unreadChatCount, onNavigate = onNavigate)
        }
    }

    if (pendingSetToStart != null) {
        val set = pendingSetToStart!!
        AlertDialog(
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
                            onError = { err -> isStarting = false; isError = true; statusMessage = err }
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
    var showCompleteDialog by remember { mutableStateOf(false) }

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var allAvailableExercises by remember { mutableStateOf<List<ExerciseItem>>(emptyList()) }
    var isAddingExercise by remember { mutableStateOf(false) }

    fun loadSession() {
        isLoading = true
        fetchActiveSessionDetailApi(context, token, sessionId,
            onSuccess = { sessObj, exList ->
                isLoading = false
                sessionData = sessObj
                exercises = exList
            },
            onError = { isLoading = false }
        )
    }

    LaunchedEffect(sessionId) { loadSession() }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().background(TrainerAppNavy).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    AppText("Aktivní trénink #${sessionId}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (sessionData != null) {
                        val athName = sessionData!!.optString("athlete_name", "")
                        val setName = sessionData!!.optString("workout_set_name", "")
                        AppText("$athName • $setName", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
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
                } else if (exercises.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
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
                            fetchExercisesApi(context, token) { result ->
                                allAvailableExercises = result
                            }
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

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            AppText("${ex.exerciseOrder}. ${ex.exerciseName}", color = TrainerAppNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (ex.series.isNotEmpty()) {
                ex.series.forEach { s ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        AppText("Série ${s.seriesOrder}:", fontSize = 13.sp, color = TrainerAppGray)
                        AppText("${s.weight} kg × ${s.reps} op", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TrainerAppNavy)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = weightInput, onValueChange = { weightInput = it }, label = { AppText("Váha (kg)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = repsInput, onValueChange = { repsInput = it }, label = { AppText("Opakování") }, modifier = Modifier.weight(1f), singleLine = true)
                Button(
                    onClick = {
                        val w = weightInput.toFloatOrNull() ?: 0f
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
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
                ) {
                    AppText("➕ Série", fontWeight = FontWeight.Bold)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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

private fun fetchUserDataFromApi(context: Context, token: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    if (token.isBlank()) return
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/me.php?token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }

            if (response.isBlank()) { runOnMain { onSuccess("") }; return@execute }
            val json = JSONObject(response)
            val coachObj = json.optJSONObject("coach") ?: json.optJSONObject("user")
            val name = coachObj?.optString("name", "") ?: json.optString("name", "")
            runOnMain { onSuccess(name) }
        } catch (e: Exception) {
            runOnMain { onError("Chyba: ${e.message}") }
        } finally {
            connection?.disconnect()
            executor.shutdown()
        }
    }
}

private fun fetchAthletesFromApi(context: Context, token: String, onSuccess: (List<AthleteItem>) -> Unit, onError: (String) -> Unit) {
    if (token.isBlank()) return
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/athletes.php?token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("X-Token", token)

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }

            if (response.isBlank()) { runOnMain { onSuccess(emptyList()) }; return@execute }
            val json = JSONObject(response)
            val list = parseAthletesFromJson(json)
            runOnMain { onSuccess(list) }
        } catch (e: Exception) {
            runOnMain { onError("Chyba při načítání sportovců: ${e.message}") }
        } finally {
            connection?.disconnect()
            executor.shutdown()
        }
    }
}

private fun fetchAthleteFullDetailApi(context: Context, token: String, athleteId: String, onSuccess: (AthleteFullDetail) -> Unit, onError: (String) -> Unit) {
    if (token.isBlank()) return
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/athlete.php?id=$athleteId&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
                if (response.isNotBlank()) {
                    val json = JSONObject(response)
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
                                    id = t.optString("id", ""),
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
                    return@execute
                }
            }
        } catch (_: Exception) {} finally {
            connection?.disconnect()
            executor.shutdown()
        }
        runOnMain { onError("Chyba") }
    }
}

private fun fetchWorkoutSetsFromApi(context: Context, token: String, onSuccess: (List<WorkoutSetItem>) -> Unit, onError: (String) -> Unit) {
    if (token.isBlank()) return
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/workout_sets.php?token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }

            if (response.isBlank()) { runOnMain { onSuccess(emptyList()) }; return@execute }
            val json = JSONObject(response)
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
            runOnMain { onError("Chyba: ${e.message}") }
        } finally {
            connection?.disconnect()
            executor.shutdown()
        }
    }
}

private fun fetchVenuesFromApi(context: Context, token: String, onSuccess: (List<VenueItem>) -> Unit) {
    if (token.isBlank()) return
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/venues.php?token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
                if (response.isNotBlank()) {
                    val json = JSONObject(response)
                    val array = json.optJSONArray("venues")
                    val items = mutableListOf<VenueItem>()
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            items.add(VenueItem(obj.optInt("id", 0), obj.optString("name", "Sportoviště"), obj.optString("address", "")))
                        }
                    }
                    runOnMain { onSuccess(items) }
                    return@execute
                }
            }
        } catch (_: Exception) {} finally { connection?.disconnect(); executor.shutdown() }
        runOnMain { onSuccess(emptyList()) }
    }
}

private fun fetchCalendarFromApi(context: Context, token: String, onSuccess: (List<TrainingItem>) -> Unit) {
    if (token.isBlank()) { onSuccess(emptyList()); return }
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/calendar.php?token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
                if (response.isNotBlank()) {
                    val json = JSONObject(response)
                    val array = json.optJSONArray("events") ?: json.optJSONArray("items")
                    val items = mutableListOf<TrainingItem>()
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            val id = obj.optString("id", i.toString())
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
                            val detailStr = if (location.isNotBlank()) "$title • $location" else title

                            items.add(TrainingItem(id, timeLabel, dateLabel, athleteName, detailStr, status, approvalStatus, isLocked, startHour, endHour))
                        }
                    }
                    runOnMain { onSuccess(items) }
                    return@execute
                }
            }
        } catch (_: Exception) {} finally {
            connection?.disconnect()
            executor.shutdown()
        }
        runOnMain { onSuccess(emptyList()) }
    }
}

private fun createCalendarEventApi(context: Context, token: String, athleteId: Int?, title: String, location: String, startsAt: String, endsAt: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/calendar.php?action=create&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val jsonBody = JSONObject().apply {
                if (athleteId != null && athleteId > 0) put("athlete_id", athleteId)
                put("title", title)
                put("location", location)
                put("starts_at", startsAt)
                put("ends_at", endsAt)
            }

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody.toString()); it.flush() }
            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }

            if (response.isNotBlank()) {
                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    runOnMain { onSuccess() }
                    return@execute
                } else {
                    val err = json.optString("error", "Chyba vytvoření události.")
                    runOnMain { onError(err) }
                    return@execute
                }
            }
            runOnMain { onError("Chyba vytvoření události") }
        } catch (e: Exception) { runOnMain { onError("Chyba: ${e.message}") } }
        finally { connection?.disconnect(); executor.shutdown() }
    }
}

private fun approveCalendarEventApi(context: Context, token: String, eventId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/calendar.php?action=approve&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val jsonBody = JSONObject().apply { put("event_id", eventId) }
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody.toString()); it.flush() }
            if (connection.responseCode in 200..299) runOnMain { onSuccess() }
            else runOnMain { onError("Chyba schválení") }
        } catch (e: Exception) { runOnMain { onError("Chyba: ${e.message}") } }
        finally { connection?.disconnect(); executor.shutdown() }
    }
}

private fun deleteCalendarEventApi(context: Context, token: String, eventId: Int, startsAt: String = "", endsAt: String = "", onSuccess: () -> Unit, onError: (String) -> Unit) {
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val actionName = if (eventId >= 200000 && startsAt.isNotBlank()) "unlock" else "delete"
            val url = URL("$baseUrl/calendar.php?action=$actionName&event_id=$eventId&starts_at=${URLEncoder.encode(startsAt, "UTF-8")}&ends_at=${URLEncoder.encode(endsAt, "UTF-8")}&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val jsonBody = JSONObject().apply {
                put("event_id", eventId)
                if (startsAt.isNotBlank()) put("starts_at", startsAt)
                if (endsAt.isNotBlank()) put("ends_at", endsAt)
            }
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody.toString()); it.flush() }
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
                if (response.isNotBlank()) {
                    val json = JSONObject(response)
                    if (json.optBoolean("success", false)) {
                        runOnMain { onSuccess() }
                        return@execute
                    }
                }
            }
            runOnMain { onError("Chyba mazání") }
        } catch (e: Exception) { runOnMain { onError("Chyba: ${e.message}") } }
        finally { connection?.disconnect(); executor.shutdown() }
    }
}

private fun fetchExercisesApi(context: Context, token: String, onSuccess: (List<ExerciseItem>) -> Unit) {
    if (token.isBlank()) return
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/exercises.php?token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
                if (response.isNotBlank()) {
                    val json = JSONObject(response)
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
                    return@execute
                }
            }
        } catch (_: Exception) {} finally { connection?.disconnect(); executor.shutdown() }
        runOnMain { onSuccess(emptyList()) }
    }
}

private fun addExerciseToSessionApi(context: Context, token: String, sessionId: Int, exerciseId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/training_session.php?action=add_exercise&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val jsonBody = JSONObject().apply {
                put("session_id", sessionId)
                put("exercise_id", exerciseId)
            }
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody.toString()); it.flush() }
            if (connection.responseCode in 200..299) runOnMain { onSuccess() }
            else runOnMain { onError("Chyba přidání cviku") }
        } catch (e: Exception) { runOnMain { onError("Chyba: ${e.message}") } }
        finally { connection?.disconnect(); executor.shutdown() }
    }
}

private fun fetchActiveSessionDetailApi(context: Context, token: String, sessionId: Int, onSuccess: (JSONObject, List<SessionExerciseItem>) -> Unit, onError: (String) -> Unit) {
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/training_session.php?id=$sessionId&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
                if (response.isNotBlank()) {
                    val json = JSONObject(response)
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
                            exList.add(SessionExerciseItem(exId, order, name, "standard", false, sList))
                        }
                    }
                    runOnMain { onSuccess(sessObj, exList) }
                    return@execute
                }
            }
        } catch (_: Exception) {} finally { connection?.disconnect(); executor.shutdown() }
        runOnMain { onError("Chyba") }
    }
}

private fun saveSeriesApi(context: Context, token: String, sessionId: Int, exerciseId: Int, weight: Float, reps: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/training_session.php?action=save_series&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val jsonBody = JSONObject().apply {
                put("session_id", sessionId)
                put("exercise_id", exerciseId)
                put("weight", weight)
                put("reps", reps)
            }
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody.toString()); it.flush() }
            if (connection.responseCode in 200..299) runOnMain { onSuccess() }
            else runOnMain { onError("Chyba uložení série") }
        } catch (e: Exception) { runOnMain { onError("Chyba: ${e.message}") } }
        finally { connection?.disconnect(); executor.shutdown() }
    }
}

private fun completeActiveSessionApi(context: Context, token: String, sessionId: Int, location: String, notes: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        try {
            val url = URL("$baseUrl/training_session.php?action=complete&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val jsonBody = JSONObject().apply {
                put("session_id", sessionId)
                put("location", location)
                put("notes", notes)
            }
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody.toString()); it.flush() }
            val responseCode = connection.responseCode
            inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }

            if (responseCode in 200..299) runOnMain { onSuccess() }
            else runOnMain { onError("Chyba dokončení: $response") }
        } catch (e: Exception) { runOnMain { onError("Chyba: ${e.message}") } }
        finally { 
            try { inputStream?.close() } catch(_: Exception) {}
            connection?.disconnect()
            executor.shutdown() 
        }
    }
}

private fun fetchChatConversationsFromApi(context: Context, token: String, onSuccess: (List<ChatConversation>) -> Unit) {
    if (token.isBlank()) return
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/chat.php?action=conversations&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
                if (response.isNotBlank()) {
                    val json = JSONObject(response)
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

                            items.add(ChatConversation(id, name, subtitle, icon, isAdmin, unreadCount, if (lastMsg.isNotBlank()) listOf(ChatMessage("1", if (isAdmin) "Administrátor" else name, false, false, lastMsg, lastTime)) else emptyList()))
                        }
                    }
                    runOnMain { onSuccess(items) }
                    return@execute
                }
            }
        } catch (_: Exception) {} finally { connection?.disconnect(); executor.shutdown() }
    }
}

private fun fetchChatMessagesFromApi(context: Context, token: String, conversationId: String, onSuccess: (List<ChatMessage>) -> Unit) {
    if (token.isBlank()) return
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/chat.php?action=thread&conversation_id=${URLEncoder.encode(conversationId, "UTF-8")}&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
                if (response.isNotBlank()) {
                    val json = JSONObject(response)
                    val array = json.optJSONArray("messages")
                    val items = mutableListOf<ChatMessage>()
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            val id = obj.optString("id", i.toString())
                            val senderName = obj.optString("sender_name", "")
                            val isMe = obj.optBoolean("is_me", false)
                            val isRead = obj.optBoolean("is_read", false)
                            val text = obj.optString("text", "")
                            val time = obj.optString("time", "")

                            items.add(ChatMessage(id, senderName, isMe, isRead, text, time))
                        }
                    }
                    runOnMain { onSuccess(items) }
                    return@execute
                }
            }
        } catch (_: Exception) {} finally { connection?.disconnect(); executor.shutdown() }
    }
}

private fun sendChatMessageApi(context: Context, token: String, conversationId: String, body: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val baseUrl = getApiBaseUrl(context)
    if (token.isBlank()) return
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/chat.php?action=send&token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val jsonBody = JSONObject().apply {
                put("conversation_id", conversationId)
                put("body", body)
            }
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody.toString()); it.flush() }
            if (connection.responseCode in 200..299) runOnMain { onSuccess() }
            else runOnMain { onError("Chyba odeslání") }
        } catch (e: Exception) { runOnMain { onError("Chyba: ${e.message}") } }
        finally { connection?.disconnect(); executor.shutdown() }
    }
}

private fun startTrainingApi(context: Context, token: String, athleteId: Int, workoutSetId: Int, onSuccess: (JSONObject) -> Unit, onError: (String) -> Unit) {
    val baseUrl = getApiBaseUrl(context)
    if (token.isBlank()) return
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/training_start.php?token=${URLEncoder.encode(token, "UTF-8")}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val jsonBody = JSONObject().apply {
                put("athlete_id", athleteId)
                put("workout_set_id", workoutSetId)
            }
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody.toString()); it.flush() }
            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }

            if (response.isBlank()) { runOnMain { onError("Server vrátil prázdnou odpověď.") }; return@execute }
            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                val err = json.optString("error", "Spuštění tréninku se nepodařilo.")
                runOnMain { onError(err) }
                return@execute
            }
            runOnMain { onSuccess(json) }
        } catch (e: Exception) { runOnMain { onError("Chyba: ${e.message}") } }
        finally { connection?.disconnect(); executor.shutdown() }
    }
}

private fun loginToApi(username: String, password: String, context: Context, onSuccess: (String, String, String, JSONObject) -> Unit, onError: (String) -> Unit) {
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/login.php")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.useCaches = false
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")

            val jsonBody = JSONObject().apply {
                put("username", username)
                put("password", password)
            }
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody.toString()); it.flush() }
            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }

            if (response.isBlank()) { runOnMain { onError("Server vrátil prázdnou odpověď. HTTP $responseCode ($baseUrl)") }; return@execute }
            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                val error = json.optString("error", "Nesprávné uživatelské jméno nebo heslo.")
                runOnMain { onError(error) }
                return@execute
            }
            val coach = json.optJSONObject("coach") ?: json.optJSONObject("user")
            val name = coach?.optString("name", "")?.takeIf { it.isNotBlank() } ?: json.optString("name", username)
            val id = coach?.optString("id", "")?.takeIf { it.isNotBlank() } ?: json.optString("id", "")
            val newToken = json.optString("token", "")

            if (newToken.isBlank()) { runOnMain { onError("Přihlášení proběhlo, ale server nevrátil token.") }; return@execute }
            runOnMain { onSuccess(name, id, newToken, json) }
        } catch (e: Exception) {
            val userMsg = when {
                e is UnknownHostException || e.cause is UnknownHostException -> "Nelze se připojit k serveru ($baseUrl). Zkontrolujte adresu serveru v nastavení ⚙️."
                e is SocketTimeoutException -> "Vypršel časový limit připojení k serveru."
                else -> "Chyba připojení: ${e.message ?: "neznámá"}"
            }
            runOnMain { onError(userMsg) }
        } finally { connection?.disconnect(); executor.shutdown() }
    }
}

private fun logoutFromApi(context: Context, token: String) {
    if (token.isBlank()) return
    val baseUrl = getApiBaseUrl(context)
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/logout.php?token=${URLEncoder.encode(token, "UTF-8")}")
            connection = URL(url.toString()).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val jsonBody = JSONObject().apply { put("token", token) }
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody.toString()); it.flush() }
            connection.responseCode
        } catch (_: Exception) {} finally { connection?.disconnect(); executor.shutdown() }
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

@Composable
private fun BottomNavigationBar(current: String, unreadChatCount: Int = 0, onNavigate: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 5.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        NavigationItem("⌂", "Domů", current == "dashboard") { onNavigate("dashboard") }
        NavigationItem("👥", "Sportovci", current == "athletes") { onNavigate("athletes") }
        NavigationItem("📅", "Kalendář", current == "calendar") { onNavigate("calendar") }
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
private fun CreateCalendarEventDialog(
    athletes: List<AthleteItem>,
    venues: List<VenueItem>,
    token: String,
    context: Context,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var availableVenues by remember(venues) { mutableStateOf(venues) }

    LaunchedEffect(token) {
        if (token.isNotBlank()) {
            fetchVenuesFromApi(context, token, onSuccess = { fetched -> if (fetched.isNotEmpty()) availableVenues = fetched })
        }
    }

    val eventTypes = listOf("Trénink", "Konzultace", "Skupinová lekce", "Jiné")
    var selectedEventType by remember { mutableStateOf("Trénink") }
    var eventTypeDropdown by remember { mutableStateOf(false) }

    var customTitle by remember { mutableStateOf("") }

    var selectedVenue by remember(availableVenues) { mutableStateOf<VenueItem?>(availableVenues.firstOrNull()) }
    var venueDropdown by remember { mutableStateOf(false) }
    var customLocation by remember { mutableStateOf("") }

    var selectedAthlete by remember { mutableStateOf<AthleteItem?>(athletes.firstOrNull()) }
    var athleteDropdown by remember { mutableStateOf(false) }

    var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)) }
    var startTimeStr by remember { mutableStateOf("09:00") }
    var endTimeStr by remember { mutableStateOf("10:00") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Nová událost v kalendáři", color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
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
                    Card(modifier = Modifier.fillMaxWidth().clickable { athleteDropdown = true }, colors = CardDefaults.cardColors(containerColor = TrainerAppLight)) {
                        AppText(selectedAthlete?.name ?: "Bez sportovce", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = athleteDropdown, onDismissRequest = { athleteDropdown = false }) {
                        athletes.forEach { ath ->
                            DropdownMenuItem(text = { AppText(ath.name) }, onClick = { selectedAthlete = ath; athleteDropdown = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { AppText("Datum (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startTimeStr, onValueChange = { startTimeStr = it }, label = { AppText("Od (HH:MM)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = endTimeStr, onValueChange = { endTimeStr = it }, label = { AppText("Do (HH:MM)") }, modifier = Modifier.weight(1f), singleLine = true)
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
                    isSaving = true
                    errorMsg = ""
                    val startsAt = "$dateStr $startTimeStr:00"
                    val endsAt = "$dateStr $endTimeStr:00"
                    val athId = selectedAthlete?.id?.toIntOrNull()
                    val finalTitle = if (customTitle.isNotBlank()) customTitle else selectedEventType
                    val finalLocation = if (selectedVenue != null) selectedVenue!!.name else customLocation

                    createCalendarEventApi(context, token, athId, finalTitle, finalLocation, startsAt, endsAt,
                        onSuccess = { isSaving = false; onCreated() },
                        onError = { err -> isSaving = false; errorMsg = err }
                    )
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = TrainerAppYellow, contentColor = TrainerAppNavy)
            ) {
                AppText("Vytvořit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AppText("Zrušit") } }
    )
}

@Composable
private fun CalendarEventActionDialog(
    event: TrainingItem,
    actionHour: Int,
    token: String,
    context: Context,
    onDismiss: () -> Unit,
    onActionDone: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText(if (event.isLocked) "Odemknout uzamčený čas" else event.detail, color = TrainerAppNavy, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (event.isLocked) {
                    AppText("Uzamčený úsek: ${event.time}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    AppText("Chcete odemknout tento časový úsek (${event.time})?", fontSize = 13.sp, color = TrainerAppGray)
                } else {
                    AppText("Sportovec: ${event.athleteName}", fontSize = 14.sp)
                    AppText("Čas: ${event.time}", fontSize = 13.sp, color = TrainerAppGray)
                    if (event.detail.isNotBlank()) AppText(event.detail, fontSize = 13.sp, color = TrainerAppGray)
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (event.approvalStatus == "pending") {
                    Button(
                        onClick = {
                            isProcessing = true
                            approveCalendarEventApi(context, token, event.id.toIntOrNull() ?: 0, onSuccess = { isProcessing = false; onActionDone() }, onError = { isProcessing = false })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF137333))
                    ) {
                        AppText("Schválit", color = Color.White)
                    }
                }
                Button(
                    onClick = {
                        isProcessing = true
                        val numId = event.id.toIntOrNull() ?: 0
                        val startTsStr = "${event.date} ${String.format(Locale.getDefault(), "%02d", actionHour)}:00:00"
                        val endTsStr = "${event.date} ${String.format(Locale.getDefault(), "%02d", actionHour + 1)}:00:00"

                        deleteCalendarEventApi(context, token, numId, startTsStr, endTsStr,
                            onSuccess = { isProcessing = false; onActionDone() },
                            onError = { isProcessing = false }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) {
                    AppText(if (event.isLocked) "Odemknout tuto hodinu" else "Smazat", color = Color.White)
                }
                if (event.isLocked) {
                    Button(
                        onClick = {
                            isProcessing = true
                            val numId = event.id.toIntOrNull() ?: 0
                            deleteCalendarEventApi(context, token, numId, "", "",
                                onSuccess = { isProcessing = false; onActionDone() },
                                onError = { isProcessing = false }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = TrainerAppNavy)
                    ) {
                        AppText("Odemknout celý úsek", color = Color.White)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AppText("Zavřít") } }
    )
}

@Composable
private fun ChatListScreen(
    conversations: List<ChatConversation>,
    unreadChatCount: Int,
    onSelectConversation: (ChatConversation) -> Unit,
    onNavigate: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().background(TrainerAppNavy).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        AppText("Pro započetí chatu se sportovci je potřeba mít načtené sportovce z účtu.", color = TrainerAppGray, modifier = Modifier.padding(16.dp), fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    athleteConvs.forEach { conv -> ConversationCard(conv) { onSelectConversation(conv) }; Spacer(Modifier.height(8.dp)) }
                }
            }
            BottomNavigationBar(current = "chat", unreadChatCount = unreadChatCount, onNavigate = onNavigate)
        }
    }
}

@Composable
private fun ConversationCard(conversation: ChatConversation, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(if (conversation.isAdmin) TrainerAppYellow else TrainerAppNavy), contentAlignment = Alignment.Center) {
                AppText(conversation.icon, fontSize = 22.sp)
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
private fun ChatDetailScreen(conversation: ChatConversation, token: String, context: Context, onBack: () -> Unit) {
    var messages by remember(conversation.id) { mutableStateOf(conversation.initialMessages) }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(conversation.id, token) {
        if (token.isNotBlank()) {
            fetchChatMessagesFromApi(context, token, conversation.id, onSuccess = { fetchedMsgs -> if (fetchedMsgs.isNotEmpty()) messages = fetchedMsgs })
            while (true) {
                delay(5000)
                fetchChatMessagesFromApi(context, token, conversation.id, onSuccess = { updated -> if (updated.isNotEmpty()) messages = updated })
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TrainerAppLight) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().background(TrainerAppNavy).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { AppText("←", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (conversation.isAdmin) TrainerAppYellow else Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    AppText(conversation.icon, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    AppText(conversation.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    AppText(if (conversation.isAdmin) "Aktivní podpora" else "Aktivní chat", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.Bottom) {
                messages.forEach { msg ->
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
                                onSuccess = { isSending = false; fetchChatMessagesFromApi(context, token, conversation.id, onSuccess = { updated -> if (updated.isNotEmpty()) messages = updated }) },
                                onError = { isSending = false }
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
        }
    }
}

private fun createDefaultConversations(athletes: List<AthleteItem>): List<ChatConversation> {
    val admin = ChatConversation(
        id = "admin",
        name = "Administrátor TrainerApp",
        subtitle = "Systémová podpora a administrace",
        icon = "🛠️",
        isAdmin = true,
        unreadCount = 0,
        initialMessages = listOf(
            ChatMessage(
                id = "1",
                senderName = "Administrátor",
                isMe = false,
                isRead = false,
                text = "Dobrý den! Vítáme vás v aplikaci TrainerApp.",
                time = "09:00"
            )
        )
    )
    val athleteConvs = athletes.map { ath ->
        ChatConversation(
            id = ath.id,
            name = ath.name,
            subtitle = ath.email.ifBlank { "Sportovec" },
            icon = "👤",
            isAdmin = false,
            unreadCount = 0,
            initialMessages = listOf(
                ChatMessage(
                    id = "1",
                    senderName = ath.name,
                    isMe = false,
                    isRead = false,
                    text = "Dobrý den, posílám zprávu z aplikace.",
                    time = "Nyní"
                )
            )
        )
    }
    return listOf(admin) + athleteConvs
}
