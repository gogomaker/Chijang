package com.example

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.repository.MilitaryRepository
import com.example.ui.MilitaryViewModel
import com.example.ui.MilitaryViewModelFactory
import com.example.ui.Screen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.login.LoginScreen
import com.example.ui.metadata.MetadataEditScreen
import com.example.ui.nfc.NfcTaggingScreen
import com.example.ui.search.ItemSearchScreen
import com.example.ui.search.LocationSearchScreen
import com.example.ui.status.DisbursementStatusScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    // Lazy initialization of Database & Repository
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy {
        MilitaryRepository(
            database.ppBoxDao(),
            database.gearPackDao(),
            database.itemDao()
        )
    }

    // ViewModel injection
    private val viewModel: MilitaryViewModel by viewModels {
        MilitaryViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure NFC foreground dispatch
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    // Handles physical NFC tag taps
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
        ) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                val hexId = bytesToHexString(it.id)
                if (hexId.isNotEmpty()) {
                    viewModel.triggerNfcTag(hexId)
                }
            }
        }
    }

    private fun bytesToHexString(src: ByteArray?): String {
        if (src == null || src.isEmpty()) return ""
        val sb = StringBuilder()
        for (b in src) {
            val v = b.toInt() and 0xFF
            val hv = Integer.toHexString(v).uppercase()
            if (hv.length < 2) {
                sb.append(0)
            }
            sb.append(hv)
        }
        return sb.toString()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppContent(viewModel: MilitaryViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentScreen == Screen.Login) {
        LoginScreen(viewModel = viewModel)
    } else {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Rounded container for Logo
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(Color.White, shape = RoundedCornerShape(10.dp))
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_launcher_background),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "치장물자간편관리체계",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentUser?.id ?: "ID"}님, 환영합니다.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Logout Icon button
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("appbar_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "로그아웃",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    val screensList = listOf(
                        Screen.Dashboard,
                        Screen.ItemSearch,
                        Screen.NfcTagging,
                        Screen.DisbursementStatus,
                        Screen.MetadataEdit
                    )

                    val currentSelection = if (currentScreen == Screen.LocationSearch) Screen.Dashboard else currentScreen

                    NavigationBarItem(
                        selected = currentSelection == Screen.Dashboard,
                        onClick = { viewModel.navigateTo(Screen.Dashboard) },
                        icon = { Icon(Icons.Default.Inventory, contentDescription = "재고위치") },
                        label = { Text("재고위치", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("nav_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentSelection == Screen.ItemSearch,
                        onClick = { viewModel.navigateTo(Screen.ItemSearch) },
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = "물자위치") },
                        label = { Text("물자위치", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("nav_item_search")
                    )

                    NavigationBarItem(
                        selected = currentSelection == Screen.NfcTagging,
                        onClick = { viewModel.navigateTo(Screen.NfcTagging) },
                        icon = { Icon(Icons.Default.Nfc, contentDescription = "NFC태깅") },
                        label = { Text("NFC태깅", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("nav_nfc_tagging")
                    )

                    NavigationBarItem(
                        selected = currentSelection == Screen.DisbursementStatus,
                        onClick = { viewModel.navigateTo(Screen.DisbursementStatus) },
                        icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "지급현황") },
                        label = { Text("지급현황", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("nav_disbursement_status")
                    )

                    NavigationBarItem(
                        selected = currentSelection == Screen.MetadataEdit,
                        onClick = { viewModel.navigateTo(Screen.MetadataEdit) },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "제원수정") },
                        label = { Text("제원수정", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("nav_metadata_edit")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val screensList = listOf(
                    Screen.Dashboard,
                    Screen.ItemSearch,
                    Screen.NfcTagging,
                    Screen.DisbursementStatus,
                    Screen.MetadataEdit
                )

                val targetScreen = if (currentScreen == Screen.LocationSearch) Screen.Dashboard else currentScreen
                val currentIndex = screensList.indexOf(targetScreen).coerceAtLeast(0)
                var previousIndex by remember { mutableStateOf(currentIndex) }

                val slideRightToLeft = currentIndex > previousIndex

                LaunchedEffect(currentIndex) {
                    previousIndex = currentIndex
                }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (slideRightToLeft) {
                            slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        } else {
                            slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "ScreenTransition"
                ) { screenState ->
                    when (screenState) {
                        Screen.Login -> {} // already handled
                        Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                        Screen.LocationSearch -> LocationSearchScreen(viewModel = viewModel)
                        Screen.ItemSearch -> ItemSearchScreen(viewModel = viewModel)
                        Screen.NfcTagging -> NfcTaggingScreen(viewModel = viewModel)
                        Screen.DisbursementStatus -> DisbursementStatusScreen(viewModel = viewModel)
                        Screen.MetadataEdit -> MetadataEditScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
