package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GearPack
import com.example.data.model.Item
import com.example.ui.MilitaryViewModel

// Navigation level enum
enum class ExplorerLevel {
    BATTALION,  // Level 1: Select Battalion (1BN, 2BN, 3BN, HQ)
    COMPANY,    // Level 2: Select Company/Unit
    PLATOON,    // Level 3: Select Platoon (1PLT, 2PLT, 3PLT, HQ, 포반, 화생방)
    GEAR_LIST,  // Level 4: List of gears or chemical types
    DETAIL      // Level 5: Detailed view of selected gear/item
}

// Navigation state node
data class ExplorerState(
    val level: ExplorerLevel = ExplorerLevel.BATTALION,
    val selectedBN: String? = null,
    val selectedCO: String? = null,
    val selectedPLT: String? = null,
    val selectedGear: String? = null
)

@Composable
fun DashboardScreen(viewModel: MilitaryViewModel) {
    // Collect DB states in case we map to real data
    val gearPacks by viewModel.searchedGearPacks.collectAsState()
    val dbItems by viewModel.selectedGearPackItems.collectAsState()

    // Navigation stack for back navigation support
    val navStack = remember { mutableStateListOf<ExplorerState>(ExplorerState()) }
    val currentState = navStack.last()

    // Helper to determine path string dynamically
    val pathString = remember(currentState) {
        val sb = StringBuilder("> 189R")
        currentState.selectedBN?.let { sb.append(" > $it") }
        currentState.selectedCO?.let { sb.append(" > $it") }
        currentState.selectedPLT?.let { sb.append(" > $it") }
        currentState.selectedGear?.let { sb.append(" > $it") }
        sb.toString()
    }

    // Checking "정보 없음" conditions:
    // "4,8,12,HQ,정보,통신,포병은 들어가면 '정보 없음'이라고 화면에 회색 글씨를 띄운다."
    val isNoInfoState = remember(currentState) {
        val noInfoUnits = listOf("4CO", "8CO", "12CO", "HQ", "정보", "통신", "포병")
        (currentState.selectedCO != null && noInfoUnits.contains(currentState.selectedCO)) ||
        (currentState.selectedPLT != null && noInfoUnits.contains(currentState.selectedPLT))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("dashboard_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header containing Name, Back Button, and dynamic Path
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "치장 군장의 위치 찾기",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Undo (Back) button to navigate up a level in the folder explorer
                    if (navStack.size > 1) {
                        IconButton(
                            onClick = { navStack.removeLast() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("explorer_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "이전 단계로",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Dynamic gray breadcrumb path
                Text(
                    text = pathString,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main explorer content based on hierarchy state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isNoInfoState) {
                    // Center-aligned grey "정보 없음" view
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "정보 없음",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray,
                                fontSize = 28.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    when (currentState.level) {
                        ExplorerLevel.BATTALION -> {
                            BattalionGrid(
                                onSelect = { bn ->
                                    navStack.add(
                                        currentState.copy(
                                            level = ExplorerLevel.COMPANY,
                                            selectedBN = bn
                                        )
                                    )
                                }
                            )
                        }
                        ExplorerLevel.COMPANY -> {
                            CompanyGrid(
                                bnName = currentState.selectedBN ?: "1BN",
                                onSelect = { co ->
                                    navStack.add(
                                        currentState.copy(
                                            level = ExplorerLevel.PLATOON,
                                            selectedCO = co
                                        )
                                    )
                                }
                            )
                        }
                        ExplorerLevel.PLATOON -> {
                            PlatoonGrid(
                                onSelect = { plt ->
                                    navStack.add(
                                        currentState.copy(
                                            level = ExplorerLevel.GEAR_LIST,
                                            selectedPLT = plt
                                        )
                                    )
                                }
                            )
                        }
                        ExplorerLevel.GEAR_LIST -> {
                            if (currentState.selectedPLT == "화생방") {
                                // CBRN special category selection grid
                                ChemicalCategoryGrid(
                                    onSelect = { category ->
                                        navStack.add(
                                            currentState.copy(
                                                level = ExplorerLevel.DETAIL,
                                                selectedGear = category
                                            )
                                        )
                                    }
                                )
                            } else {
                                // Standard Platoon gear list
                                GearPositionList(
                                    onSelect = { position ->
                                        navStack.add(
                                            currentState.copy(
                                                level = ExplorerLevel.DETAIL,
                                                selectedGear = position
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        ExplorerLevel.DETAIL -> {
                            if (currentState.selectedPLT == "화생방") {
                                CBRNDetailView(
                                    category = currentState.selectedGear ?: "보호덧신",
                                    company = currentState.selectedCO ?: "3CO"
                                )
                            } else {
                                GearPackDetailView(
                                    viewModel = viewModel,
                                    gearPacks = gearPacks,
                                    dbItems = dbItems,
                                    bn = currentState.selectedBN ?: "1BN",
                                    co = currentState.selectedCO ?: "3CO",
                                    plt = currentState.selectedPLT ?: "2PLT",
                                    position = currentState.selectedGear ?: "분대장"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Level 1: Battalion selection grid
@Composable
fun BattalionGrid(onSelect: (String) -> Unit) {
    val battalions = listOf("1BN", "2BN", "3BN", "HQ")
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(battalions) { bn ->
            ExplorerCard(title = bn, onClick = { onSelect(bn) })
        }
    }
}

// Level 2: Company selection grid based on selected battalion
@Composable
fun CompanyGrid(bnName: String, onSelect: (String) -> Unit) {
    val companies = remember(bnName) {
        when (bnName) {
            "1BN" -> listOf("1CO", "2CO", "3CO", "4CO", "HQ", "정보")
            "2BN" -> listOf("5CO", "6CO", "7CO", "8CO", "HQ", "정보")
            "3BN" -> listOf("9CO", "10CO", "11CO", "12CO", "HQ", "정보")
            else -> listOf("통신", "포병", "본부") // HQ options
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(companies) { co ->
            ExplorerCard(title = co, onClick = { onSelect(co) })
        }
    }
}

// Level 3: Platoon selection grid ("나머지 중대는 1,2,3,포반,HQ,화생방을 띄운다.")
@Composable
fun PlatoonGrid(onSelect: (String) -> Unit) {
    val platoons = listOf("1PLT", "2PLT", "3PLT", "HQ", "포반", "화생방")
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(platoons) { plt ->
            ExplorerCard(title = plt, onClick = { onSelect(plt) })
        }
    }
}

// Level 4 Case A: Standard platoon gear list
@Composable
fun GearPositionList(onSelect: (String) -> Unit) {
    val positions = listOf("분대장", "부분대장", "소총수1", "소총수2", "소총수3", "소총수4", "소총수5")
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        items(positions) { pos ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(pos) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "군장",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = pos,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        }
    }
}

// Level 4 Case B: Chemical gear categories
@Composable
fun ChemicalCategoryGrid(onSelect: (String) -> Unit) {
    val categories = listOf("방독면", "보호의", "정화통", "보호덧신", "KD-1", "KMARK-1")
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(categories) { cat ->
            ExplorerCard(title = cat, onClick = { onSelect(cat) })
        }
    }
}

// Reusable card for grids matching the high contrast, spacious design
@Composable
fun ExplorerCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1.2f)
            .clickable { onClick() }
            .testTag("explorer_grid_card_$title"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Level 5 Detail Case A: Standard Gear Pack Detail
@Composable
fun GearPackDetailView(
    viewModel: MilitaryViewModel,
    gearPacks: List<GearPack>,
    dbItems: List<Item>,
    bn: String,
    co: String,
    plt: String,
    position: String
) {
    // Attempt to map selected items to database seeds
    val korCo = mapCoToKorean(co)
    val korPlt = mapPltToKorean(plt)

    // Find if we have a matching seeded gear pack in database
    val matchedGearPack = remember(gearPacks, korCo, korPlt, position) {
        gearPacks.find {
            it.company == korCo && it.platoon == korPlt && it.position == position
        }
    }

    // Trigger loading items if a matched pack is found
    LaunchedEffect(matchedGearPack) {
        viewModel.selectGearPack(matchedGearPack)
    }

    // Define fallback items in case this particular platoon is not fully seeded in the DB
    val itemsToShow = remember(matchedGearPack, dbItems) {
        if (matchedGearPack != null && dbItems.isNotEmpty()) {
            dbItems.map { Pair(it.itemName, it.productionYear) }
        } else {
            listOf(
                Pair("모포", "2020"),
                Pair("침낭", "2019"),
                Pair("수통", "2021"),
                Pair("야전삽", "2018"),
                Pair("전투조끼", "2022")
            )
        }
    }

    val displayId = matchedGearPack?.gearId ?: "GEAR-${position.hashCode().coerceAtLeast(0) % 9000 + 1000}"

    Card(
        modifier = Modifier
            .fillMaxSize()
            .testTag("gear_detail_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "군장 #$displayId",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "<바15 3층 2단>",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = Color.DarkGray
                    )
                }
            }

            // Beautiful coral pink box matching Screenshot 5
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFD99491)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            // List of nested equipment/items loaded inside this gear pack
            items(itemsToShow) { (itemName, year) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = itemName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "생산년도: ${year}년",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "시효기간: 20년",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "관리책임관: ${co}중대장 ~~~~",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }
        }
    }
}

// Level 5 Detail Case B: CBRN Category Detail
@Composable
fun CBRNDetailView(category: String, company: String) {
    val cbrnItems = listOf(
        Pair("$category #1", "2020"),
        Pair("$category #2", "2015"),
        Pair("$category #3", "2018")
    )

    Card(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cbrn_detail_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "<바16 2층 1~2단>",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = Color.DarkGray
                    )
                }
            }

            // Beautiful coral pink box matching Screenshot 8
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFD99491)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            // List of actual chemical equipment
            items(cbrnItems) { (itemName, year) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = itemName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "생산년도: ${year}년",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "시효기간: 20년",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "관리책임관: ${company}중대장 ~~~~",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }
        }
    }
}

// Translators to match user abbreviations with DB Korean seeds
private fun mapCoToKorean(co: String): String {
    return when(co) {
        "1CO" -> "1중대"
        "2CO" -> "2중대"
        "3CO" -> "3중대"
        "4CO" -> "4중대"
        "5CO" -> "5중대"
        "6CO" -> "6중대"
        "7CO" -> "7중대"
        "8CO" -> "8중대"
        "9CO" -> "9중대"
        "10CO" -> "10중대"
        "11CO" -> "11중대"
        "12CO" -> "12중대"
        else -> co
    }
}

private fun mapPltToKorean(plt: String): String {
    return when(plt) {
        "1PLT" -> "1소대"
        "2PLT" -> "2소대"
        "3PLT" -> "3소대"
        else -> plt
    }
}
