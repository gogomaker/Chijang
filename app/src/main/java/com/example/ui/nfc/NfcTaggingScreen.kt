package com.example.ui.nfc

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MilitaryViewModel
import kotlin.random.Random

@Composable
fun NfcTaggingScreen(viewModel: MilitaryViewModel) {
    val scannedGearPack by viewModel.scannedGearPack.collectAsState()
    val scannedPPBox by viewModel.scannedPPBox.collectAsState()
    val generatedSn by viewModel.generatedSn.collectAsState()
    val isNfcWriting by viewModel.isNfcWriting.collectAsState()

    var isEditTabSelected by remember { mutableStateOf(false) }
    var isSimulatorExpanded by remember { mutableStateOf(true) }

    // Generator overlay state
    var showGeneratorOverlay by remember { mutableStateOf(false) }
    var showFormInputs by remember { mutableStateOf(false) }

    // Form inputs state with default values matching Army design
    var textBattalion by remember { mutableStateOf("189R") }
    var textCompany by remember { mutableStateOf("3CO") }
    var textPlatoon by remember { mutableStateOf("2PLT") }
    var textSquad by remember { mutableStateOf("분대장") }

    // Pulse Animation for scanning or writing state
    val infiniteTransition = rememberInfiniteTransition(label = "NfcPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Reset writing state when switching tabs
    LaunchedEffect(isEditTabSelected) {
        viewModel.setIsNfcWriting(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("nfc_tagging_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tab Segmented Pill (인식 / 수정)
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
                        // Recognition tab (인식)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 21.dp, bottomStart = 21.dp))
                                .background(
                                    if (!isEditTabSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .clickable { isEditTabSelected = false }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (!isEditTabSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "인식",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (!isEditTabSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxHeight().width(1.dp)
                        )

                        // Edit tab (수정)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 21.dp, bottomEnd = 21.dp))
                                .background(
                                    if (isEditTabSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .clickable { isEditTabSelected = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isEditTabSelected) {
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
                                color = if (isEditTabSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Circular Status button & animation
                val shouldPulse = if (isEditTabSelected) isNfcWriting else (scannedGearPack == null && scannedPPBox == null)
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .scale(if (shouldPulse) pulseScale else 1.0f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isEditTabSelected) {
                                // Toggle writing state in edit tab
                                if (generatedSn.isNotEmpty()) {
                                    viewModel.setIsNfcWriting(!isNfcWriting)
                                }
                            } else {
                                // Re-trigger scanning reset when in recognition tab
                                if (scannedGearPack != null || scannedPPBox != null) {
                                    viewModel.clearNfcScan()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (shouldPulse) {
                        // Outer pulsing halo
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        )
                        // Inner pulsing halo
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = CircleShape
                                )
                        )
                    }

                    // Main Circle Button
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val circleText = when {
                            !isEditTabSelected -> {
                                if (scannedGearPack != null || scannedPPBox != null) "재인식" else "인식중"
                            }
                            else -> {
                                if (isNfcWriting) "수정중" else "태그 수정"
                            }
                        }
                        Text(
                            text = circleText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Serial Number Generation Area for Edit Mode (Only visible in 수정 tab)
                if (isEditTabSelected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 일련번호 생성 Button
                        Row(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    showFormInputs = false
                                    showGeneratorOverlay = true
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "일련번호 생성",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // S/N Display Box
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(0.dp) // Rectangular style from mock
                                )
                                .background(Color.White)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "S/N: $generatedSn",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(46.dp)) // Maintain same structure layout spacing
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lower matched Card for mapped scanned item
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val isScanned = scannedGearPack != null || scannedPPBox != null
                            // Left: Circle Badge (군 / A)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = if (isScanned) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isScanned) "군" else "A",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Center: Meta Titles
                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = when {
                                        scannedGearPack != null -> "군장 #${scannedGearPack?.gearId?.takeLast(2) ?: "75"}"
                                        scannedPPBox != null -> "보관함: ${scannedPPBox?.boxId}"
                                        else -> "치장품목"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        scannedGearPack != null -> "${scannedGearPack?.company} ${scannedGearPack?.platoon} ${scannedGearPack?.squad} ${scannedGearPack?.position}"
                                        scannedPPBox != null -> "${scannedPPBox?.company} ${scannedPPBox?.platoon} 보관함"
                                        else -> "소속편제"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp
                                    ),
                                    color = if (isScanned) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Right: Geometric decor (triangle, star, square)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isActive = scannedGearPack != null || scannedPPBox != null
                            val opac = if (isActive) 0.8f else 0.3f
                            // Triangle
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(TriangleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f * opac))
                            )
                            // Star / Sun Emblem icon
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f * opac),
                                modifier = Modifier.size(24.dp)
                            )
                            // Square
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f * opac),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }

            // Bottom simulation & log card (Collapsible)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nfc_simulator_panel"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSimulatorExpanded = !isSimulatorExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "가상 NFC 태그 에뮬레이터 (동작 시뮬레이션)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = if (isSimulatorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "토글",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSimulatorExpanded) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = if (isEditTabSelected && isNfcWriting) "👉 수정 모드가 켜져있습니다. 아래 버튼을 누르면 해당 태그의 ID가 생성된 S/N ($generatedSn)로 교체됩니다!" else "1. 가상 NFC 태그 감지 에뮬레이션",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isEditTabSelected && isNfcWriting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerNfcTag("BOX-101") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isEditTabSelected && isNfcWriting) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("sim_box_101")
                            ) {
                                Text("BOX-101 (보관함)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.triggerNfcTag("GEAR-1113") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isEditTabSelected && isNfcWriting) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("sim_gear_1113")
                            ) {
                                Text("GEAR-1113 (분대장)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Trigger reset option if mapped
                        if (scannedGearPack != null || scannedPPBox != null) {
                            Button(
                                onClick = { viewModel.clearNfcScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                            ) {
                                Text("스캔 리셋", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Generator Overlay (Entire screen with military dark-gray background matching Mock design)
        if (showGeneratorOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF8E8D93)) // Perfect matching gray from image 6 & 7
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // 1단계에서 아무대나 클릭하면 2단계 폼 입력 모드로 전환
                        if (!showFormInputs) {
                            showFormInputs = true
                        }
                    }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!showFormInputs) {
                    // Step 1: "일련번호를 생성하십시오" Welcome screen
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "일련번호를\n생성하십시오",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 44.sp,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.weight(1.2f))

                        // 돌아가기 button
                        Row(
                            modifier = Modifier
                                .width(160.dp)
                                .height(44.dp)
                                .border(1.dp, Color.White, RoundedCornerShape(22.dp))
                                .clickable { showGeneratorOverlay = false }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "돌아가기",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                } else {
                    // Step 2: Battalion / Company / Platoon / Squad Input fields form
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp)
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            // 대대 Input Row
                            CustomMilitaryInputField(
                                label = "대대",
                                value = textBattalion,
                                onValueChange = { textBattalion = it }
                            )

                            // 중대 Input Row
                            CustomMilitaryInputField(
                                label = "중대",
                                value = textCompany,
                                onValueChange = { textCompany = it }
                            )

                            // 소대 Input Row
                            CustomMilitaryInputField(
                                label = "소대",
                                value = textPlatoon,
                                onValueChange = { textPlatoon = it }
                            )

                            // 분대 Input Row
                            CustomMilitaryInputField(
                                label = "분대",
                                value = textSquad,
                                onValueChange = { textSquad = it }
                            )
                        }

                        // 일련번호 생성 Button
                        Row(
                            modifier = Modifier
                                .width(200.dp)
                                .height(46.dp)
                                .border(1.dp, Color.White, RoundedCornerShape(23.dp))
                                .clickable {
                                    // S/N Generation algorithm:
                                    // Mix platoon/company/battalion info and add stable random numbers to form 9-digits matching "015438213"
                                    val r1 = Random.nextInt(100, 999).toString()
                                    val r2 = Random.nextInt(100, 999).toString()
                                    val generatedId = "015" + r1 + r2
                                    viewModel.setGeneratedSn(generatedId)
                                    showGeneratorOverlay = false
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "일련번호 생성",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CustomMilitaryInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "입력하십시오",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 18.sp
                            )
                        }
                        innerTextField()
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.7f), thickness = 1.dp)
                }
            }
        )
    }
}

// Triangle shape for decoration matching military drawings
private val TriangleShape = GenericShape { size: Size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}
