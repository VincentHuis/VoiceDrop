package com.vincent.voicedrop.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vincent.voicedrop.R
import com.vincent.voicedrop.ui.theme.AmberGold
import com.vincent.voicedrop.ui.theme.BalticBlue
import com.vincent.voicedrop.ui.theme.InkDark
import com.vincent.voicedrop.ui.theme.Platinum
import com.vincent.voicedrop.ui.theme.SteelAzure
import com.vincent.voicedrop.ui.theme.VibrantCoral
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }

    BackHandler { if (page > 0) page-- }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (page == index) 24.dp else 8.dp)
                                .background(
                                    color = if (page == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
                Button(onClick = { if (page < 2) page++ else onDone() }) {
                    Text(
                        if (page < 2) stringResource(R.string.onboarding_next)
                        else stringResource(R.string.onboarding_done)
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState > initialState)
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                else
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            },
            label = "onboarding_page"
        ) { currentPage ->
            when (currentPage) {
                0 -> SpeakPage(padding)
                1 -> CategoriesPage(padding)
                else -> PermissionsPage(padding)
            }
        }
    }
}

@Composable
private fun SpeakPage(padding: PaddingValues) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            Box(
                modifier = Modifier
                    .size((140f * pulseScale).dp)
                    .alpha(pulseAlpha)
                    .background(SteelAzure, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(SteelAzure, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        Text(
            stringResource(R.string.onboarding_page1_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.onboarding_page1_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoriesPage(padding: PaddingValues) {
    val categories = listOf(
        R.string.category_groceries to VibrantCoral,
        R.string.category_todo to BalticBlue,
        R.string.category_ideas to AmberGold,
        R.string.category_reminders to SteelAzure,
        R.string.category_other to Platinum
    )
    val visible = remember { mutableStateListOf(*Array(5) { false }) }
    LaunchedEffect(Unit) {
        categories.indices.forEach { i ->
            delay(130L * i)
            visible[i] = true
        }
    }

    @Composable
    fun Chip(index: Int) {
        val (labelRes, color) = categories[index]
        AnimatedVisibility(
            visible = visible[index],
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.7f)
        ) {
            Surface(shape = RoundedCornerShape(50), color = color) {
                Text(
                    stringResource(labelRes),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    color = if (color == Platinum) InkDark else Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip(0); Chip(1); Chip(2)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip(3); Chip(4)
        }
        Spacer(Modifier.height(36.dp))
        Text(
            stringResource(R.string.onboarding_page2_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.onboarding_page2_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionsPage(padding: PaddingValues) {
    val context = LocalContext.current

    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    var calendarGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { r -> locationGranted = r[Manifest.permission.ACCESS_FINE_LOCATION] == true }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { r -> calendarGranted = r[Manifest.permission.READ_CALENDAR] == true }

    fun openSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(
            Icons.Filled.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = AmberGold
        )
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.onboarding_page3_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.onboarding_page3_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        PermissionCard(
            icon = { Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.onboarding_perm_location),
            description = stringResource(R.string.onboarding_perm_location_desc),
            deniedMessage = stringResource(R.string.onboarding_perm_location_denied),
            granted = locationGranted,
            onAllow = {
                locationLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            },
            onOpenSettings = ::openSettings
        )
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            icon = { Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.onboarding_perm_notif),
            description = stringResource(R.string.onboarding_perm_notif_desc),
            deniedMessage = stringResource(R.string.onboarding_perm_notif_denied),
            granted = notifGranted,
            onAllow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            } else null,
            onOpenSettings = ::openSettings
        )
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            icon = { Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.onboarding_perm_calendar),
            description = stringResource(R.string.onboarding_perm_calendar_desc),
            deniedMessage = stringResource(R.string.onboarding_perm_calendar_denied),
            granted = calendarGranted,
            onAllow = {
                calendarLauncher.launch(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                )
            },
            onOpenSettings = ::openSettings
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    deniedMessage: String,
    granted: Boolean,
    onAllow: (() -> Unit)?,
    onOpenSettings: () -> Unit
) {
    var askedOnce by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.padding(top = 2.dp, end = 12.dp)) { icon() }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (granted) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!granted && onAllow != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        deniedMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (!askedOnce) {
                                askedOnce = true
                                onAllow()
                            } else {
                                onOpenSettings()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            if (!askedOnce) stringResource(R.string.onboarding_allow)
                            else stringResource(R.string.onboarding_open_settings)
                        )
                    }
                }
            }
        }
    }
}
