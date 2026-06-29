package com.example.ui.metadata

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GearPack
import com.example.data.model.Item
import com.example.data.model.PPBox
import com.example.ui.MilitaryViewModel
import kotlinx.coroutines.launch

sealed class SearchResult {
    data class ItemResult(val item: Item) : SearchResult()
    data class GearPackResult(val gearPack: GearPack) : SearchResult()
    data class PPBoxResult(val ppBox: PPBox) : SearchResult()
}

@Composable
fun MetadataEditScreen(viewModel: MilitaryViewModel) {
    val gearPacks by viewModel.allGearPacksFlow.collectAsState(initial = emptyList())
    val items by viewModel.allItemsFlow.collectAsState(initial = emptyList())
    val ppBoxes by viewModel.allPPBoxesFlow.collectAsState(initial = emptyList())

    var isRegisterTabSelected by remember { mutableStateOf(false) } // false = 수정, true = 등록
    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialog State
    var selectedEditResult by remember { mutableStateOf<SearchResult?>(null) }

    // Dropdown items lists
    val predefinedItemNames = listOf("전투배낭", "반탄모", "방독면", "야전삽", "모포", "침낭", "전투조끼", "탄창", "전투화")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab Segmented Pill (수정 / 등록)
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(42.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(21.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(21.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Edit Tab (수정)
                    Row(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 21.dp, bottomStart = 21.dp))
                            .background(
                                if (!isRegisterTabSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable { isRegisterTabSelected = false }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!isRegisterTabSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "수정",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (!isRegisterTabSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    VerticalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxHeight().width(1.dp)
                    )

                    // Register Tab (등록)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 21.dp, bottomEnd = 21.dp))
                            .background(
                                if (isRegisterTabSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable { isRegisterTabSelected = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isRegisterTabSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "등록",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isRegisterTabSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!isRegisterTabSelected) {
                // ================== [수정 탭] ==================
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("metadata_search_field"),
                    placeholder = { Text("시리얼 넘버(S/N) 또는 명칭 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "지우기")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                val filteredResults = remember(items, gearPacks, ppBoxes, searchQuery) {
                    val list = mutableListOf<SearchResult>()
                    if (searchQuery.isBlank()) {
                        // Show all or first few of each if query is empty
                        list.addAll(items.map { SearchResult.ItemResult(it) })
                        list.addAll(gearPacks.map { SearchResult.GearPackResult(it) })
                        list.addAll(ppBoxes.map { SearchResult.PPBoxResult(it) })
                    } else {
                        val q = searchQuery.trim()
                        items.filter { it.itemId.contains(q, ignoreCase = true) || it.itemName.contains(q, ignoreCase = true) }
                            .forEach { list.add(SearchResult.ItemResult(it)) }
                        gearPacks.filter { it.gearId.contains(q, ignoreCase = true) || it.company.contains(q, ignoreCase = true) || it.platoon.contains(q, ignoreCase = true) }
                            .forEach { list.add(SearchResult.GearPackResult(it)) }
                        ppBoxes.filter { it.boxId.contains(q, ignoreCase = true) || it.company.contains(q, ignoreCase = true) || it.platoon.contains(q, ignoreCase = true) }
                            .forEach { list.add(SearchResult.PPBoxResult(it)) }
                    }
                    list
                }

                if (filteredResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "일치하는 시리얼 넘버가 없습니다.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("metadata_search_results"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredResults) { result ->
                            SearchResultCard(result = result, onClick = {
                                selectedEditResult = result
                            })
                        }
                    }
                }
            } else {
                // ================== [등록 탭] ==================
                var registerType by remember { mutableStateOf(0) } // 0 = 세부 품목, 1 = 군장, 2 = 보관함
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "등록 대상 종류 선택",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    // Target Selector Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val targets = listOf("세부 품목", "군장", "보관함")
                        targets.forEachIndexed { index, label ->
                            OutlinedButton(
                                onClick = { registerType = index },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (registerType == index) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (registerType == index) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    width = if (registerType == index) 1.5.dp else 1.dp
                                )
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Dynamic Fields based on registerType
                    when (registerType) {
                        0 -> {
                            // --- 세부 품목 등록 폼 ---
                            var itemPredefinedName by remember { mutableStateOf(predefinedItemNames[0]) }
                            var itemCustomName by remember { mutableStateOf("") }
                            var isCustomItemName by remember { mutableStateOf(false) }

                            var itemIdInput by remember { mutableStateOf("") }
                            var itemYearInput by remember { mutableStateOf("2024") }
                            var itemGearIdInput by remember { mutableStateOf("") }

                            var isNameDropdownExpanded by remember { mutableStateOf(false) }
                            var isGearDropdownExpanded by remember { mutableStateOf(false) }

                            var showIdError by remember { mutableStateOf(false) }

                            // Item Name dropdown or custom name
                            Text("품목명", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isCustomItemName,
                                    onCheckedChange = { isCustomItemName = it }
                                )
                                Text("직접 입력", style = MaterialTheme.typography.bodyMedium)
                            }

                            if (isCustomItemName) {
                                OutlinedTextField(
                                    value = itemCustomName,
                                    onValueChange = { itemCustomName = it },
                                    label = { Text("품목명 직접 입력 (예: 방탄복)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = itemPredefinedName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("품목명 선택") },
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            IconButton(onClick = { isNameDropdownExpanded = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = isNameDropdownExpanded,
                                        onDismissRequest = { isNameDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        predefinedItemNames.forEach { name ->
                                            DropdownMenuItem(
                                                text = { Text(name) },
                                                onClick = {
                                                    itemPredefinedName = name
                                                    isNameDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // S/N Field
                            OutlinedTextField(
                                value = itemIdInput,
                                onValueChange = {
                                    itemIdInput = it
                                    if (it.isNotEmpty()) showIdError = false
                                },
                                label = { Text("시리얼 넘버 (S/N) *") },
                                isError = showIdError,
                                supportingText = { if (showIdError) Text("시리얼 넘버를 입력하십시오.", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth().testTag("reg_item_sn_field"),
                                singleLine = true
                            )

                            // Year Field
                            OutlinedTextField(
                                value = itemYearInput,
                                onValueChange = { itemYearInput = it },
                                label = { Text("생산년도") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Parent Gear Selection dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = itemGearIdInput,
                                    onValueChange = { itemGearIdInput = it },
                                    label = { Text("소속 군장 S/N") },
                                    placeholder = { Text("지정 또는 입력하십시오.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        if (gearPacks.isNotEmpty()) {
                                            IconButton(onClick = { isGearDropdownExpanded = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }
                                    }
                                )
                                if (gearPacks.isNotEmpty()) {
                                    DropdownMenu(
                                        expanded = isGearDropdownExpanded,
                                        onDismissRequest = { isGearDropdownExpanded = false }
                                    ) {
                                        gearPacks.forEach { gp ->
                                            DropdownMenuItem(
                                                text = { Text("${gp.gearId} (${gp.company} ${gp.platoon} ${gp.position})") },
                                                onClick = {
                                                    itemGearIdInput = gp.gearId
                                                    isGearDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Submit Button
                            Button(
                                onClick = {
                                    if (itemIdInput.isBlank()) {
                                        showIdError = true
                                    } else {
                                        val finalName = if (isCustomItemName) itemCustomName else itemPredefinedName
                                        val newItem = Item(
                                            itemId = itemIdInput.trim(),
                                            itemName = finalName.ifBlank { "미지정품목" },
                                            productionYear = itemYearInput.trim(),
                                            gearId = itemGearIdInput.trim()
                                        )
                                        viewModel.saveItem(newItem) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("품목이 성공적으로 등록되었습니다!")
                                            }
                                            itemIdInput = ""
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("reg_submit_item_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("물자(품목) 등록 완료", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        1 -> {
                            // --- 군장 등록 폼 ---
                            var gearIdInput by remember { mutableStateOf("") }
                            var gearCompany by remember { mutableStateOf("3CO") }
                            var gearPlatoon by remember { mutableStateOf("2PLT") }
                            var gearSquad by remember { mutableStateOf("1SQ") }
                            var gearPosition by remember { mutableStateOf("소총수") }
                            var gearStatus by remember { mutableStateOf("창고 안") }
                            var gearManager by remember { mutableStateOf("관리자") }
                            var gearParentBoxId by remember { mutableStateOf("") }

                            var showIdError by remember { mutableStateOf(false) }
                            var isStatusDropdownExpanded by remember { mutableStateOf(false) }
                            var isBoxDropdownExpanded by remember { mutableStateOf(false) }

                            OutlinedTextField(
                                value = gearIdInput,
                                onValueChange = {
                                    gearIdInput = it
                                    if (it.isNotEmpty()) showIdError = false
                                },
                                label = { Text("군장 시리얼 넘버 (S/N) *") },
                                isError = showIdError,
                                supportingText = { if (showIdError) Text("군장 S/N를 입력하십시오.", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth().testTag("reg_gear_sn_field"),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = gearCompany,
                                    onValueChange = { gearCompany = it },
                                    label = { Text("중대") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = gearPlatoon,
                                    onValueChange = { gearPlatoon = it },
                                    label = { Text("소대") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = gearSquad,
                                    onValueChange = { gearSquad = it },
                                    label = { Text("분대") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = gearPosition,
                                    onValueChange = { gearPosition = it },
                                    label = { Text("직책") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = gearStatus,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("지급 현황") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { isStatusDropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = isStatusDropdownExpanded,
                                    onDismissRequest = { isStatusDropdownExpanded = false }
                                ) {
                                    listOf("창고 안", "창고 밖").forEach { status ->
                                        DropdownMenuItem(
                                            text = { Text(status) },
                                            onClick = {
                                                gearStatus = status
                                                isStatusDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = gearManager,
                                onValueChange = { gearManager = it },
                                label = { Text("관리책임관") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Parent PP Box Selection dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = gearParentBoxId,
                                    onValueChange = { gearParentBoxId = it },
                                    label = { Text("소속 보관함 S/N") },
                                    placeholder = { Text("지정 또는 입력하십시오.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        if (ppBoxes.isNotEmpty()) {
                                            IconButton(onClick = { isBoxDropdownExpanded = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }
                                    }
                                )
                                if (ppBoxes.isNotEmpty()) {
                                    DropdownMenu(
                                        expanded = isBoxDropdownExpanded,
                                        onDismissRequest = { isBoxDropdownExpanded = false }
                                    ) {
                                        ppBoxes.forEach { box ->
                                            DropdownMenuItem(
                                                text = { Text("${box.boxId} (${box.company} ${box.platoon} 보관함)") },
                                                onClick = {
                                                    gearParentBoxId = box.boxId
                                                    isBoxDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (gearIdInput.isBlank()) {
                                        showIdError = true
                                    } else {
                                        val newGear = GearPack(
                                            gearId = gearIdInput.trim(),
                                            company = gearCompany.trim(),
                                            platoon = gearPlatoon.trim(),
                                            squad = gearSquad.trim(),
                                            position = gearPosition.trim(),
                                            status = gearStatus,
                                            managerName = gearManager.trim(),
                                            parentBoxId = gearParentBoxId.trim().ifEmpty { null }
                                        )
                                        viewModel.saveGearPack(newGear) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("군장이 성공적으로 등록되었습니다!")
                                            }
                                            gearIdInput = ""
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("reg_submit_gear_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("군장 등록 완료", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        2 -> {
                            // --- 보관함 등록 폼 ---
                            var boxIdInput by remember { mutableStateOf("") }
                            var boxCompany by remember { mutableStateOf("3CO") }
                            var boxPlatoon by remember { mutableStateOf("2PLT") }
                            var boxManager by remember { mutableStateOf("관리자") }

                            var showIdError by remember { mutableStateOf(false) }

                            OutlinedTextField(
                                value = boxIdInput,
                                onValueChange = {
                                    boxIdInput = it
                                    if (it.isNotEmpty()) showIdError = false
                                },
                                label = { Text("보관함 시리얼 넘버 (S/N) *") },
                                isError = showIdError,
                                supportingText = { if (showIdError) Text("보관함 S/N를 입력하십시오.", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth().testTag("reg_box_sn_field"),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = boxCompany,
                                    onValueChange = { boxCompany = it },
                                    label = { Text("중대") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = boxPlatoon,
                                    onValueChange = { boxPlatoon = it },
                                    label = { Text("소대") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = boxManager,
                                onValueChange = { boxManager = it },
                                label = { Text("관리책임관") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (boxIdInput.isBlank()) {
                                        showIdError = true
                                    } else {
                                        val newBox = PPBox(
                                            boxId = boxIdInput.trim(),
                                            company = boxCompany.trim(),
                                            platoon = boxPlatoon.trim(),
                                            managerName = boxManager.trim()
                                        )
                                        viewModel.savePPBox(newBox) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("보관함이 성공적으로 등록되었습니다!")
                                            }
                                            boxIdInput = ""
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("reg_submit_box_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("보관함 등록 완료", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // ================== [수정 팝업 다이얼로그] ==================
    selectedEditResult?.let { result ->
        var showSaveConfirm by remember { mutableStateOf(false) }

        when (result) {
            is SearchResult.ItemResult -> {
                val item = result.item
                var editedName by remember { mutableStateOf(item.itemName) }
                var editedYear by remember { mutableStateOf(item.productionYear) }
                var editedGearId by remember { mutableStateOf(item.gearId) }

                AlertDialog(
                    onDismissRequest = { selectedEditResult = null },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val updated = item.copy(
                                    itemName = editedName.trim(),
                                    productionYear = editedYear.trim(),
                                    gearId = editedGearId.trim()
                                )
                                viewModel.saveItem(updated) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("세부 품목 정보가 수정되었습니다.")
                                    }
                                }
                                selectedEditResult = null
                            }
                        ) {
                            Text("저장", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedEditResult = null }) {
                            Text("취소")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("품목 제원 수정", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "S/N: ${item.itemId}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("품목명") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = editedYear,
                                onValueChange = { editedYear = it },
                                label = { Text("생산년도") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = editedGearId,
                                onValueChange = { editedGearId = it },
                                label = { Text("소속 군장 S/N") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                )
            }
            is SearchResult.GearPackResult -> {
                val gp = result.gearPack
                var editedCompany by remember { mutableStateOf(gp.company) }
                var editedPlatoon by remember { mutableStateOf(gp.platoon) }
                var editedSquad by remember { mutableStateOf(gp.squad) }
                var editedPosition by remember { mutableStateOf(gp.position) }
                var editedStatus by remember { mutableStateOf(gp.status) }
                var editedManager by remember { mutableStateOf(gp.managerName) }
                var editedParentBoxId by remember { mutableStateOf(gp.parentBoxId ?: "") }

                var isStatusMenuExpanded by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { selectedEditResult = null },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val updated = gp.copy(
                                    company = editedCompany.trim(),
                                    platoon = editedPlatoon.trim(),
                                    squad = editedSquad.trim(),
                                    position = editedPosition.trim(),
                                    status = editedStatus,
                                    managerName = editedManager.trim(),
                                    parentBoxId = editedParentBoxId.trim().ifEmpty { null }
                                )
                                viewModel.saveGearPack(updated) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("군장 정보가 수정되었습니다.")
                                    }
                                }
                                selectedEditResult = null
                            }
                        ) {
                            Text("저장", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedEditResult = null }) {
                            Text("취소")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("군장 제원 수정", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "군장 S/N: ${gp.gearId}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editedCompany,
                                    onValueChange = { editedCompany = it },
                                    label = { Text("중대") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editedPlatoon,
                                    onValueChange = { editedPlatoon = it },
                                    label = { Text("소대") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editedSquad,
                                    onValueChange = { editedSquad = it },
                                    label = { Text("분대") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editedPosition,
                                    onValueChange = { editedPosition = it },
                                    label = { Text("직책") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = editedStatus,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("지급 현황") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { isStatusMenuExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = isStatusMenuExpanded,
                                    onDismissRequest = { isStatusMenuExpanded = false }
                                ) {
                                    listOf("창고 안", "창고 밖").forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s) },
                                            onClick = {
                                                editedStatus = s
                                                isStatusMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = editedManager,
                                onValueChange = { editedManager = it },
                                label = { Text("관리책임관") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = editedParentBoxId,
                                onValueChange = { editedParentBoxId = it },
                                label = { Text("소속 보관함 S/N") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                )
            }
            is SearchResult.PPBoxResult -> {
                val box = result.ppBox
                var editedCompany by remember { mutableStateOf(box.company) }
                var editedPlatoon by remember { mutableStateOf(box.platoon) }
                var editedManager by remember { mutableStateOf(box.managerName) }

                AlertDialog(
                    onDismissRequest = { selectedEditResult = null },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val updated = box.copy(
                                    company = editedCompany.trim(),
                                    platoon = editedPlatoon.trim(),
                                    managerName = editedManager.trim()
                                )
                                viewModel.savePPBox(updated) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("보관함 정보가 수정되었습니다.")
                                    }
                                }
                                selectedEditResult = null
                            }
                        ) {
                            Text("저장", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedEditResult = null }) {
                            Text("취소")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("보관함 제원 수정", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "보관함 S/N: ${box.boxId}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editedCompany,
                                    onValueChange = { editedCompany = it },
                                    label = { Text("중대") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editedPlatoon,
                                    onValueChange = { editedPlatoon = it },
                                    label = { Text("소대") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = editedManager,
                                onValueChange = { editedManager = it },
                                label = { Text("관리책임관") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SearchResultCard(result: SearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Badge Icon container
                val (badgeColor, badgeText, badgeIcon) = when (result) {
                    is SearchResult.ItemResult -> Triple(MaterialTheme.colorScheme.secondary, "품목", Icons.Default.Inventory)
                    is SearchResult.GearPackResult -> Triple(MaterialTheme.colorScheme.primary, "군장", Icons.Default.Backpack)
                    is SearchResult.PPBoxResult -> Triple(MaterialTheme.colorScheme.tertiary, "보관함", Icons.Default.AllInbox)
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = badgeColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }

                        Text(
                            text = when (result) {
                                is SearchResult.ItemResult -> result.item.itemName
                                is SearchResult.GearPackResult -> "${result.gearPack.company} ${result.gearPack.platoon} ${result.gearPack.squad} ${result.gearPack.position}"
                                is SearchResult.PPBoxResult -> "${result.ppBox.company} ${result.ppBox.platoon} 보관함"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "S/N: " + when (result) {
                            is SearchResult.ItemResult -> result.item.itemId
                            is SearchResult.GearPackResult -> result.gearPack.gearId
                            is SearchResult.PPBoxResult -> result.ppBox.boxId
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Secondary detail line
                    val detailLine = when (result) {
                        is SearchResult.ItemResult -> "생산: ${result.item.productionYear}년 | 소속군장: ${result.item.gearId}"
                        is SearchResult.GearPackResult -> "지급: ${result.gearPack.status} | 책임관: ${result.gearPack.managerName}"
                        is SearchResult.PPBoxResult -> "책임관: ${result.ppBox.managerName}"
                    }
                    Text(
                        text = detailLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "수정",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
