package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrimeManagerApp(viewModel: CrimeViewModel) {
    val context = LocalContext.current
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var activeTab by remember { mutableStateOf("dashboard") }
    var showAddCaseDialog by remember { mutableStateOf(false) }
    var showAddSuspectDialog by remember { mutableStateOf(false) }
    var showEvidenceDialogCaseId by remember { mutableStateOf<Int?>(null) }
    
    // Selected case for details
    val selectedCaseId by viewModel.selectedCaseId.collectAsState()
    val selectedCase by viewModel.selectedCase.collectAsState()

    // Handle incoming Toast-like messages
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            // Smoothly clear it after showing
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = TacticalSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Intel") },
                    selected = activeTab == "dashboard",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TacticalGold,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = TacticalGold,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = TacticalSurfaceVariant
                    ),
                    onClick = { activeTab = "dashboard" },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Folder, contentDescription = "Cases") },
                    label = { Text("Cases") },
                    selected = activeTab == "cases",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TacticalGold,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = TacticalGold,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = TacticalSurfaceVariant
                    ),
                    onClick = { activeTab = "cases" },
                    modifier = Modifier.testTag("nav_cases")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Security, contentDescription = "Suspects") },
                    label = { Text("Suspects") },
                    selected = activeTab == "suspects",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TacticalGold,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = TacticalGold,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = TacticalSurfaceVariant
                    ),
                    onClick = { activeTab = "suspects" },
                    modifier = Modifier.testTag("nav_suspects")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = activeTab == "settings",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TacticalGold,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = TacticalGold,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = TacticalSurfaceVariant
                    ),
                    onClick = { activeTab = "settings" },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TacticalDarkBg)
                .padding(innerPadding)
        ) {
            // Top Status Header Banner
            AgencyHeaderBanner(isOnline = isOnline, apiBaseUrl = apiBaseUrl, statusMessage = statusMessage, onRefresh = { viewModel.syncData() })

            // Main Tab content switching with crisp fades
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (activeTab) {
                    "dashboard" -> DashboardTab(
                        viewModel = viewModel,
                        onFileCaseClick = { showAddCaseDialog = true },
                        onAddSuspectClick = { showAddSuspectDialog = true }
                    )
                    "cases" -> CasesTab(
                        viewModel = viewModel,
                        onFileCaseClick = { showAddCaseDialog = true }
                    )
                    "suspects" -> SuspectsTab(
                        viewModel = viewModel,
                        onAddSuspectClick = { showAddSuspectDialog = true }
                    )
                    "settings" -> SettingsTab(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. File Case Dialog
    if (showAddCaseDialog) {
        AddCaseDialog(
            onDismiss = { showAddCaseDialog = false },
            onSubmit = { crimeType, jurisdiction ->
                viewModel.createCase(crimeType, jurisdiction)
                showAddCaseDialog = false
            }
        )
    }

    // 2. Add Suspect Dialog
    if (showAddSuspectDialog) {
        AddSuspectDialog(
            onDismiss = { showAddSuspectDialog = false },
            onSubmit = { first, last, nat, dob, hist, warrant ->
                viewModel.createSuspect(first, last, nat, dob, hist, warrant)
                showAddSuspectDialog = false
            }
        )
    }

    // 3. Log Evidence Dialog
    showEvidenceDialogCaseId?.let { caseId ->
        LogEvidenceDialog(
            caseId = caseId,
            onDismiss = { showEvidenceDialogCaseId = null },
            onSubmit = { type, loc, custody, officer ->
                viewModel.addEvidence(caseId, type, loc, custody, officer)
                showEvidenceDialogCaseId = null
            }
        )
    }

    // 4. Full Screen Case Details Dialog
    selectedCase?.let { caseItem ->
        CaseDetailDialog(
            caseItem = caseItem,
            viewModel = viewModel,
            onDismiss = { viewModel.selectCase(null) },
            onLogEvidenceClick = { showEvidenceDialogCaseId = caseItem.id }
        )
    }
}

// --- Header Banner Component ---
@Composable
fun AgencyHeaderBanner(isOnline: Boolean, apiBaseUrl: String, statusMessage: String?, onRefresh: () -> Unit) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TacticalGold)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield Icon",
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = "CRIMECONTROL RMS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DISTRICT PRECINCT 04",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isOnline) TacticalGreen else TacticalRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOnline) "SYNCED" else "OFFLINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) TacticalGreen else TacticalRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(36.dp)
                        .background(TacticalSurfaceVariant, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Data",
                        tint = TacticalGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            HorizontalDivider(color = BorderColor, thickness = 1.dp)
        }
    }
    
    // Smooth toast notification overlay
    statusMessage?.let { msg ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TacticalGold.copy(alpha = 0.08f))
                .border(BorderStroke(1.dp, TacticalGold.copy(alpha = 0.15f)))
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Intel", tint = TacticalGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = msg, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// --- TAB 1: Dashboard Intel ---
@Composable
fun DashboardTab(
    viewModel: CrimeViewModel,
    onFileCaseClick: () -> Unit,
    onAddSuspectClick: () -> Unit
) {
    val cases by viewModel.allCases.collectAsState()
    val openCases by viewModel.openCases.collectAsState()
    val suspects by viewModel.allSuspects.collectAsState()
    val warrants by viewModel.activeWarrants.collectAsState()
    val detectives by viewModel.allDetectives.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Tactical Radar Canvas decoration
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "CRIME RADAR & INTELLIGENCE REPORT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalGold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Authorized access only. Real-time logging synced with municipal databases and investigator assignments.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Stats grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Cases",
                    value = cases.size.toString(),
                    subtext = "${openCases.size} open / review",
                    icon = Icons.Default.Folder,
                    color = TacticalCyan,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Warrants",
                    value = warrants.size.toString(),
                    subtext = "Custody: ${suspects.size - warrants.size} held",
                    icon = Icons.Default.Warning,
                    color = TacticalRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick action dispatch keys
        item {
            Text(
                text = "RAPID TASK DISPATCH",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TacticalGold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onFileCaseClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                    border = BorderStroke(1.dp, TacticalGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_case")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = TacticalGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("File New Case", color = TextPrimary)
                }
                Button(
                    onClick = onAddSuspectClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                    border = BorderStroke(1.dp, TacticalGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_suspect")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TacticalGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Suspect", color = TextPrimary)
                }
            }
        }

        // Detective squad list to display workload (Stored Proc demo)
        item {
            Text(
                text = "DETECTIVE WORKLOAD (STORED PROC WORK DISTRIBUTION)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TacticalGold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    detectives.forEachIndexed { index, det ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = TacticalGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "Det. ${det.firstName} ${det.lastName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Badge: ${det.badgeNumber} • ${det.rank}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            // Display workload bar/badge
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (det.currentWorkload >= 3) TacticalRed.copy(alpha = 0.15f) else TacticalGreen.copy(alpha = 0.15f)
                                ),
                                border = BorderStroke(
                                    1.dp, 
                                    if (det.currentWorkload >= 3) TacticalRed.copy(alpha = 0.4f) else TacticalGreen.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = "Active: ${det.currentWorkload} Cases",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (det.currentWorkload >= 3) TacticalRed else TacticalGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (index < detectives.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isRed = color == TacticalRed
    val bg = if (isRed) HighDensityRoseLight else HighDensityIndigoLight
    val borderCol = if (isRed) Color(0xFFFFC5C5) else Color(0xFFD6E4FF)
    val contentCol = if (isRed) HighDensityRose else HighDensityIndigo
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, borderCol),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    color = contentCol,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(icon, contentDescription = null, tint = contentCol, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = contentCol)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtext, fontSize = 10.sp, color = contentCol.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
        }
    }
}

// --- TAB 2: Cases Database ---
@Composable
fun CasesTab(
    viewModel: CrimeViewModel,
    onFileCaseClick: () -> Unit
) {
    val cases by viewModel.allCases.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredCases = cases.filter {
        it.crimeType.contains(searchQuery, ignoreCase = true) ||
        it.jurisdiction.contains(searchQuery, ignoreCase = true) ||
        it.status.contains(searchQuery, ignoreCase = true) ||
        it.id.toString() == searchQuery
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Search textfield filled design
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search case files by type, precinct, status...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TacticalGold) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TacticalGold,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = TacticalSurface,
                    unfocusedContainerColor = TacticalSurface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("case_search_input")
            )

            if (filteredCases.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching records found.", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) {
                    items(filteredCases) { caseItem ->
                        CaseCard(caseItem = caseItem, onClick = { viewModel.selectCase(caseItem.id) })
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onFileCaseClick,
            containerColor = TacticalGold,
            contentColor = TacticalDarkBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("file_case_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "File New Case")
        }
    }
}

@Composable
fun CaseCard(caseItem: CaseEntity, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("case_card_${caseItem.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CASE FILE #${caseItem.id}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TacticalGold
                )
                // Case status badge
                val badgeColors = getStatusBadgeColor(caseItem.status)
                Card(
                    colors = CardDefaults.cardColors(containerColor = badgeColors.first),
                    border = BorderStroke(1.dp, badgeColors.second),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = caseItem.status.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColors.second,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = caseItem.crimeType,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Map, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = caseItem.jurisdiction, fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Filed: ${caseItem.dateFiled ?: "N/A"}",
                fontSize = 11.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

fun getStatusBadgeColor(status: String): Pair<Color, Color> {
    return when (status.lowercase()) {
        "open" -> Pair(HighDensityIndigoLight, HighDensityIndigo)
        "under review" -> Pair(HighDensityAmberLight, HighDensityAmber)
        "closed" -> Pair(HighDensityGreenLight, HighDensityGreen)
        else -> Pair(HighDensitySurfaceVariant, TextSecondary)
    }
}

// --- TAB 3: Suspect Database ---
@Composable
fun SuspectsTab(
    viewModel: CrimeViewModel,
    onAddSuspectClick: () -> Unit
) {
    val suspects by viewModel.allSuspects.collectAsState()
    val warrants by viewModel.activeWarrants.collectAsState()
    val scores by viewModel.suspectRiskScores.collectAsState()

    var showWarrantsOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSuspects = suspects.filter {
        val matchesSearch = "${it.firstName} ${it.lastName}".contains(searchQuery, ignoreCase = true) ||
                it.nationality?.contains(searchQuery, ignoreCase = true) == true
        val matchesWarrant = !showWarrantsOnly || it.hasActiveWarrant
        matchesSearch && matchesWarrant
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Toggle switches & search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search suspects by name or origin...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TacticalGold) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TacticalGold,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = TacticalSurface,
                unfocusedContainerColor = TacticalSurface
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().testTag("suspect_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showWarrantsOnly,
                    onCheckedChange = { showWarrantsOnly = it },
                    colors = CheckboxDefaults.colors(checkedColor = TacticalGold, checkmarkColor = TacticalDarkBg)
                )
                Text("Active Warrants Only", color = TextPrimary, fontSize = 14.sp)
            }
            Button(
                onClick = onAddSuspectClick,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                border = BorderStroke(1.dp, TacticalGold),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("add_suspect_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = TacticalGold)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Suspect", color = TextPrimary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredSuspects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No suspect files found.", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                items(filteredSuspects) { suspect ->
                    val riskScore = scores[suspect.id]
                    SuspectCard(
                        suspect = suspect,
                        riskScore = riskScore,
                        onCalculateRisk = { viewModel.calculateSuspectRisk(suspect.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SuspectCard(
    suspect: SuspectEntity,
    riskScore: Int?,
    onCalculateRisk: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth().testTag("suspect_card_${suspect.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BIOMETRIC PROFILE #${suspect.id}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TacticalGold
                )
                if (suspect.hasActiveWarrant) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalRed.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, TacticalRed),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ACTIVE WARRANT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${suspect.firstName} ${suspect.lastName}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Nationality: ${suspect.nationality ?: "Unknown"} • DOB: ${suspect.dateOfBirth ?: "N/A"}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Criminal History tags
            suspect.criminalHistory?.let { history ->
                if (history.isNotEmpty()) {
                    Text(text = "CRIMINAL RECORD HISTORY:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        history.split(",").forEach { crime ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TacticalSurfaceVariant),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = crime.trim(),
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Risk Score calculator block (DB scalar function replication)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "DB RISK SCORE INDEX", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Text(
                        text = riskScore?.let { "$it PTS" } ?: "UNASSESSED",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = riskScore?.let { if (it >= 50) TacticalRed else TacticalCyan } ?: TextSecondary
                    )
                }
                Button(
                    onClick = onCalculateRisk,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                    border = BorderStroke(1.dp, TacticalGold),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(36.dp).testTag("calculate_risk_btn_${suspect.id}")
                ) {
                    Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(16.dp), tint = TacticalGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Evaluate DB Index", fontSize = 11.sp, color = TextPrimary)
                }
            }
        }
    }
}

// --- TAB 4: Settings ---
@Composable
fun SettingsTab(viewModel: CrimeViewModel) {
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    var urlInput by remember { mutableStateOf(apiBaseUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "INVESTIGATION ENDPOINTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TacticalGold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configure the base API server address matching your running Node.js backend. From local emulator, use 'http://10.0.2.2:3000/api/' to reference host machine.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Server Base URL", color = TacticalGold) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalGold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = TacticalDarkBg,
                            unfocusedContainerColor = TacticalDarkBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("api_url_input")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.updateServerUrl(urlInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_api_url_btn")
                    ) {
                        Text("Connect & Save Connection", color = TacticalDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "DATABASE SCHEMA SUMMARY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TacticalGold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SchemaItem(table = "Cases", description = "Local persistence mapping SQL Cases table, keeping logs of status, crime type, location, and dates.")
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                    SchemaItem(table = "Suspects", description = "Stores biological fingerprints, origin passport records, active warrant keys, and past convict indices.")
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                    SchemaItem(table = "Evidence", description = "Keeps secure tracks of physical, forensic, and digital proofs and seals with Chain of Custody tags.")
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                    SchemaItem(table = "Detectives", description = "Maps local and SQL personnel workload rosters to route new cases appropriately.")
                }
            }
        }
    }
}

@Composable
fun SchemaItem(table: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.Storage, contentDescription = null, tint = TacticalGold, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = "tbl_$table", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Text(text = description, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// --- DIALOG COMPONENT: Case Details & Gemini AI Analysis ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailDialog(
    caseItem: CaseEntity,
    viewModel: CrimeViewModel,
    onDismiss: () -> Unit,
    onLogEvidenceClick: () -> Unit
) {
    val evidenceList by viewModel.selectedCaseEvidence.collectAsState()
    val hearingsList by viewModel.selectedCaseHearings.collectAsState()
    val vehiclesList by viewModel.selectedCaseVehicles.collectAsState()
    val aiState by viewModel.aiAnalysisState.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().background(TacticalDarkBg),
            color = TacticalDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
            ) {
                // Modal Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TacticalSurface)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TacticalGold)
                    }
                    Text(
                        text = "CASE STUDY #${caseItem.id}",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile study block
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = caseItem.crimeType, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = "Jurisdiction: ${caseItem.jurisdiction}", fontSize = 13.sp, color = TextSecondary)
                                    }
                                    val badgeColors = getStatusBadgeColor(caseItem.status)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = badgeColors.first),
                                        border = BorderStroke(1.dp, badgeColors.second),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = caseItem.status.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColors.second,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Date Logged: ${caseItem.dateFiled ?: "N/A"}",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Call Stored Procedure Button: Assign Detective
                                Button(
                                    onClick = { viewModel.assignDetectiveToCase(caseItem.id, caseItem.crimeType) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TacticalGold),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("assign_detective_btn")
                                ) {
                                    Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = TacticalDarkBg)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Assign Available Detective (SP)", color = TacticalDarkBg, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Log Evidence block
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "EVIDENCE FILE ROOM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TacticalGold, fontFamily = FontFamily.Monospace)
                            Button(
                                onClick = onLogEvidenceClick,
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                                border = BorderStroke(1.dp, TacticalGold),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp).testTag("log_evidence_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = TacticalGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Proof", fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (evidenceList.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = TacticalSurface.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Text(
                                    text = "No evidence logged for this case. Log physical, forensic, or digital files to proceed.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                evidenceList.forEach { ev ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when(ev.type.lowercase()) {
                                                        "forensic" -> Icons.Default.Science
                                                        "digital" -> Icons.Default.Computer
                                                        else -> Icons.Default.Inventory
                                                    },
                                                    contentDescription = null,
                                                    tint = TacticalCyan,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(text = ev.type.uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(text = "Location: ${ev.storageLocation ?: "Unknown"}", fontSize = 11.sp, color = TextSecondary)
                                                    Text(text = "Custody: ${ev.chainOfCustody ?: "Unknown"}", fontSize = 11.sp, color = TextSecondary)
                                                }
                                            }
                                            if (ev.isVerified) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = TacticalGreen.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "SEALED & VERIFIED",
                                                        fontSize = 8.sp,
                                                        color = TacticalGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Gemini AI analysis brief report panel
                    item {
                        Text(text = "SHERLOCK AI CRIME ANALYST", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TacticalGold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSurfaceVariant),
                            border = BorderStroke(1.dp, TacticalGold.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TacticalGold, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "AI Investigation Intelligence Brief", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Harness the power of server-side Gemini AI to evaluate physical evidence cross-references, calculate escape timelines, and draft suspect profiling sheets.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                when (val state = aiState) {
                                    is AiAnalysisState.Idle -> {
                                        Button(
                                            onClick = { viewModel.runAiCaseAnalysis(caseItem) },
                                            colors = ButtonDefaults.buttonColors(containerColor = TacticalGold),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("trigger_ai_brief_btn")
                                        ) {
                                            Text("Generate AI Case Brief", color = TacticalDarkBg, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    is AiAnalysisState.Loading -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(color = TacticalGold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Sherlock AI analyzing crime files...", color = TacticalGold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    is AiAnalysisState.Success -> {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                                            border = BorderStroke(1.dp, BorderColor),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = state.report,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = { viewModel.runAiCaseAnalysis(caseItem) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                                                    border = BorderStroke(1.dp, BorderColor),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.align(Alignment.End)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TacticalGold, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Re-Analyze", color = TextPrimary, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- DIALOG COMPONENT: Add Case ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCaseDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var crimeType by remember { mutableStateOf("") }
    var jurisdiction by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Crime Case", color = TacticalGold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = crimeType,
                    onValueChange = { crimeType = it },
                    label = { Text("Crime Classification", color = TacticalGold) },
                    placeholder = { Text("e.g. Armed robbery, Grand Larceny") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TacticalGold, unfocusedBorderColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_case_type_input")
                )
                OutlinedTextField(
                    value = jurisdiction,
                    onValueChange = { jurisdiction = it },
                    label = { Text("Jurisdiction Precinct", color = TacticalGold) },
                    placeholder = { Text("e.g. East District, Downtown Sector") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TacticalGold, unfocusedBorderColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_case_jurisdiction_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if(crimeType.isNotBlank() && jurisdiction.isNotBlank()) onSubmit(crimeType, jurisdiction) },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalGold),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("submit_case_btn")
            ) {
                Text("File Case", color = TacticalDarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = TacticalSurface
    )
}

// --- DIALOG COMPONENT: Add Suspect ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSuspectDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, Boolean) -> Unit
) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var history by remember { mutableStateOf("") }
    var hasActiveWarrant by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Biometric Suspect", color = TacticalGold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = first,
                        onValueChange = { first = it },
                        label = { Text("First Name", color = TacticalGold) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TacticalGold, unfocusedBorderColor = BorderColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("add_suspect_first_input")
                    )
                    OutlinedTextField(
                        value = last,
                        onValueChange = { last = it },
                        label = { Text("Last Name", color = TacticalGold) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TacticalGold, unfocusedBorderColor = BorderColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("add_suspect_last_input")
                    )
                }
                OutlinedTextField(
                    value = nationality,
                    onValueChange = { nationality = it },
                    label = { Text("Nationality", color = TacticalGold) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TacticalGold, unfocusedBorderColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_suspect_nat_input")
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("DOB (YYYY-MM-DD)", color = TacticalGold) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TacticalGold, unfocusedBorderColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_suspect_dob_input")
                )
                OutlinedTextField(
                    value = history,
                    onValueChange = { history = it },
                    label = { Text("Prior History (comma separated)", color = TacticalGold) },
                    placeholder = { Text("e.g. Theft, Assault, Rioting") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TacticalGold, unfocusedBorderColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_suspect_history_input")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasActiveWarrant,
                        onCheckedChange = { hasActiveWarrant = it },
                        colors = CheckboxDefaults.colors(checkedColor = TacticalGold, checkmarkColor = TacticalDarkBg)
                    )
                    Text("Issue Active Warrant", color = TextPrimary, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if(first.isNotBlank() && last.isNotBlank()) onSubmit(first, last, nationality, dob, history, hasActiveWarrant) },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalGold),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("submit_suspect_btn")
            ) {
                Text("Register Profile", color = TacticalDarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = TacticalSurface
    )
}

// --- DIALOG COMPONENT: Log Evidence ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEvidenceDialog(
    caseId: Int,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    var type by remember { mutableStateOf("Physical") }
    var location by remember { mutableStateOf("") }
    var custody by remember { mutableStateOf("") }
    var officer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Secure Proof & Evidence", color = TacticalGold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Evidence Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Physical", "Digital", "Forensic").forEach { item ->
                        val selected = type == item
                        Button(
                            onClick = { type = item },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) TacticalGold else TacticalSurfaceVariant
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = item, color = if (selected) TacticalDarkBg else TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Secure Storage Location", color = TacticalGold) },
                    placeholder = { Text("e.g. Locker B12, Server Vault 1") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TacticalGold, unfocusedBorderColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("evidence_loc_input")
                )
                OutlinedTextField(
                    value = custody,
                    onValueChange = { custody = it },
                    label = { Text("Chain of Custody Log", color = TacticalGold) },
                    placeholder = { Text("e.g. Officer Davis -> Vault Custodian") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TacticalGold, unfocusedBorderColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("evidence_custody_input")
                )
                OutlinedTextField(
                    value = officer,
                    onValueChange = { officer = it },
                    label = { Text("Officer Name / Badge", color = TacticalGold) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TacticalGold, unfocusedBorderColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("evidence_officer_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if(location.isNotBlank()) onSubmit(type, location, custody, officer) },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalGold),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("submit_evidence_btn")
            ) {
                Text("Log Proof", color = TacticalDarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = TacticalSurface
    )
}
