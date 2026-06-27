package com.example.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MilitaryViewModel

@Composable
fun DisbursementStatusScreen(viewModel: MilitaryViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("disbursement_status_screen"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "개발중 ZZZ...",
            fontSize = 42.sp,
            fontWeight = FontWeight.Light,
            color = Color.Gray,
            modifier = Modifier.padding(16.dp)
        )
    }
}
