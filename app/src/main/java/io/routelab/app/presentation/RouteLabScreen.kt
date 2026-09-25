package io.routelab.app.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.routelab.app.data.sample.SampleRoutes
import io.routelab.app.presentation.components.*
import io.routelab.app.presentation.theme.*

@Composable
fun RouteLabScreen(
    viewModel: RouteLabViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().use { reader -> reader.readText() }
                    viewModel.importGpxContent(content, "Rute-Impor.gpx")
                }
            } catch (e: Exception) {
                // Handled gracefully in ViewModel
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgDark,
        topBar = {
            RouteLabTopBar()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val currentState = state) {
                is RouteLabUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = AccentBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Menganalisis data lintasan rute...",
                                color = TextSecondaryDark,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                is RouteLabUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .background(SurfaceDark, RoundedCornerShape(12.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Terjadi Kesalahan Analisis",
                                color = ColorClimb,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentState.message,
                                color = TextSecondaryDark,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val firstRoute = SampleRoutes.getAvailableRoutes().first()
                                    viewModel.selectRoute(firstRoute)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) {
                                Text("Muat Rute Default", color = Color.White)
                            }
                        }
                    }
                }

                is RouteLabUiState.Content -> {
                    RouteLabContent(
                        content = currentState,
                        onSelectRoute = { viewModel.selectRoute(it) },
                        onScrub = { viewModel.scrubDistance(it) },
                        onTargetHoursChange = { viewModel.updateTargetCotHours(it) },
                        onSelectTab = { viewModel.selectTab(it) },
                        onAskQuestion = { viewModel.askAssistant(it) },
                        onLaunchFilePicker = { filePickerLauncher.launch("*/*") }
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteLabTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(AccentBlue, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ROUTELAB",
                    color = TextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }

            Box(
                modifier = Modifier
                    .background(SurfaceElevated, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "v0.1.0 MVP",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Analyze the Ride Before You Ride",
            color = TextSecondaryDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RouteLabContent(
    content: RouteLabUiState.Content,
    onSelectRoute: (SampleRoutes.SampleRouteDef) -> Unit,
    onScrub: (Double) -> Unit,
    onTargetHoursChange: (Double) -> Unit,
    onSelectTab: (Int) -> Unit,
    onAskQuestion: (String) -> Unit,
    onLaunchFilePicker: () -> Unit
) {
    val tabTitles = listOf(
        "Ikhtisar",
        "Elevasi & Tanjakan",
        "Segmen Rute",
        "Target COT",
        "Asisten AI"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Route Selector Strip
        item {
            RouteSelectorStrip(
                routes = content.availableRoutes,
                selectedRouteId = content.currentRouteDef.id,
                onSelectRoute = onSelectRoute,
                onImportGpx = onLaunchFilePicker
            )
        }

        // 2. Navigation Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = content.activeTabIndex,
                containerColor = SurfaceDark,
                contentColor = AccentBlueBright,
                edgePadding = 0.dp,
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = content.activeTabIndex == index,
                        onClick = { onSelectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (content.activeTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (content.activeTabIndex == index) AccentCyan else TextSecondaryDark
                            )
                        }
                    )
                }
            }
        }

        // 3. Tab Body
        when (content.activeTabIndex) {
            0 -> { // Ikhtisar
                item {
                    InteractiveRouteMap(
                        points = content.analysis.points,
                        climbs = content.analysis.climbs,
                        pois = content.analysis.pois,
                        selectedDistanceMeters = content.selectedDistanceMeters,
                        onSelectDistance = onScrub
                    )
                }
                item {
                    RouteQuickStatsRow(route = content.analysis)
                }
                item {
                    RouteIntelligenceCard(summary = content.analysis.summary)
                }
                item {
                    DifficultyCard(difficulty = content.analysis.difficulty)
                }
                item {
                    RoadCharacteristicsCard(roadInfo = content.analysis.roadInfo)
                }
            }

            1 -> { // Elevasi & Tanjakan
                item {
                    InteractiveElevationProfile(
                        points = content.analysis.points,
                        climbs = content.analysis.climbs,
                        selectedDistanceMeters = content.selectedDistanceMeters,
                        onScrub = onScrub
                    )
                }
                item {
                    InteractiveRouteMap(
                        points = content.analysis.points,
                        climbs = content.analysis.climbs,
                        pois = content.analysis.pois,
                        selectedDistanceMeters = content.selectedDistanceMeters,
                        onSelectDistance = onScrub
                    )
                }
                item {
                    ClimbListCard(
                        climbs = content.analysis.climbs,
                        onSelectClimb = { climb -> onScrub(climb.startKm * 1000.0) }
                    )
                }
            }

            2 -> { // Segmen Rute
                item {
                    RouteSegmentsCard(segments = content.analysis.segments)
                }
                item {
                    RoadCharacteristicsCard(roadInfo = content.analysis.roadInfo)
                }
            }

            3 -> { // Target COT & Nutrisi
                item {
                    CotPlannerCard(
                        cotPlan = content.analysis.cotPlan,
                        targetHours = content.targetCotHours,
                        onTargetHoursChange = onTargetHoursChange
                    )
                }
                item {
                    FuelingScheduleCard(recommendations = content.analysis.restFueling)
                }
            }

            4 -> { // Asisten AI
                item {
                    AiAssistantSection(
                        messages = content.assistantMessages,
                        onAskQuestion = onAskQuestion
                    )
                }
            }
        }

        // Bottom space padding
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RouteSelectorStrip(
    routes: List<SampleRoutes.SampleRouteDef>,
    selectedRouteId: String,
    onSelectRoute: (SampleRoutes.SampleRouteDef) -> Unit,
    onImportGpx: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pilih Rute Analisis",
                color = TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Impor GPX",
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onImportGpx() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            routes.forEach { route ->
                val isSelected = route.id == selectedRouteId
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) AccentBlue.copy(alpha = 0.25f) else SurfaceDark,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) AccentCyan else Color(0x1AFFFFFF),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectRoute(route) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = route.name,
                            color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Text(
                            text = route.location,
                            color = TextMutedDark,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
