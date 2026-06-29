package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GearPack
import com.example.data.model.Item
import com.example.data.model.PPBox
import com.example.data.model.User
import com.example.data.repository.MilitaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen {
    Login,
    Dashboard,
    LocationSearch,
    ItemSearch,
    NfcTagging,
    DisbursementStatus,
    MetadataEdit
}

data class ItemLocation(
    val item: Item,
    val gearPack: GearPack?,
    val ppBox: PPBox?
)

class MilitaryViewModel(private val repository: MilitaryRepository) : ViewModel() {

    // Auth & Session State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // PPBox State for Metadata screen
    private val _allPPBoxes = MutableStateFlow<List<PPBox>>(emptyList())
    val allPPBoxes: StateFlow<List<PPBox>> = _allPPBoxes.asStateFlow()

    // Location Search State
    private val _locationQuery = MutableStateFlow("")
    val locationQuery: StateFlow<String> = _locationQuery.asStateFlow()

    private val _searchedGearPacks = MutableStateFlow<List<GearPack>>(emptyList())
    val searchedGearPacks: StateFlow<List<GearPack>> = _searchedGearPacks.asStateFlow()

    private val _selectedGearPack = MutableStateFlow<GearPack?>(null)
    val selectedGearPack: StateFlow<GearPack?> = _selectedGearPack.asStateFlow()

    private val _selectedGearPackItems = MutableStateFlow<List<Item>>(emptyList())
    val selectedGearPackItems: StateFlow<List<Item>> = _selectedGearPackItems.asStateFlow()

    // Item Search State
    private val _itemSearchQuery = MutableStateFlow("")
    val itemSearchQuery: StateFlow<String> = _itemSearchQuery.asStateFlow()

    private val _searchedItems = MutableStateFlow<List<Item>>(emptyList())
    val searchedItems: StateFlow<List<Item>> = _searchedItems.asStateFlow()

    private val _selectedItem = MutableStateFlow<Item?>(null)
    val selectedItem: StateFlow<Item?> = _selectedItem.asStateFlow()

    private val _selectedItemLocation = MutableStateFlow<ItemLocation?>(null)
    val selectedItemLocation: StateFlow<ItemLocation?> = _selectedItemLocation.asStateFlow()

    // NFC Tagging State
    private val _scannedGearPack = MutableStateFlow<GearPack?>(null)
    val scannedGearPack: StateFlow<GearPack?> = _scannedGearPack.asStateFlow()

    private val _scannedPPBox = MutableStateFlow<PPBox?>(null)
    val scannedPPBox: StateFlow<PPBox?> = _scannedPPBox.asStateFlow()

    private val _scannedPPBoxGearPacks = MutableStateFlow<List<GearPack>>(emptyList())
    val scannedPPBoxGearPacks: StateFlow<List<GearPack>> = _scannedPPBoxGearPacks.asStateFlow()

    private val _nfcLogs = MutableStateFlow<List<String>>(listOf("NFC 대기 중..."))
    val nfcLogs: StateFlow<List<String>> = _nfcLogs.asStateFlow()

    private val _generatedSn = MutableStateFlow("015438213")
    val generatedSn: StateFlow<String> = _generatedSn.asStateFlow()

    private val _isNfcWriting = MutableStateFlow(false)
    val isNfcWriting: StateFlow<Boolean> = _isNfcWriting.asStateFlow()

    fun setGeneratedSn(sn: String) {
        _generatedSn.value = sn
    }

    fun setIsNfcWriting(writing: Boolean) {
        _isNfcWriting.value = writing
    }

    init {
        // Load initial lists
        loadAllGearPacks()
        loadAllItems()
        loadAllPPBoxes()
    }

    private fun loadAllGearPacks() {
        viewModelScope.launch {
            try {
                repository.getAllGearPacks().collect {
                    if (_locationQuery.value.isEmpty()) {
                        _searchedGearPacks.value = it
                    }
                }
            } catch (e: Exception) {
                logNfc("DB 에러 (군장 조회 실패): ${e.message}")
            }
        }
    }

    private fun loadAllItems() {
        viewModelScope.launch {
            try {
                repository.getAllItems().collect {
                    if (_itemSearchQuery.value.isEmpty()) {
                        _searchedItems.value = it
                    }
                }
            } catch (e: Exception) {
                logNfc("DB 에러 (품목 조회 실패): ${e.message}")
            }
        }
    }

    private fun loadAllPPBoxes() {
        viewModelScope.launch {
            try {
                repository.getAllPPBoxes().collect {
                    _allPPBoxes.value = it
                }
            } catch (e: Exception) {
                logNfc("DB 에러 (P.P박스 조회 실패): ${e.message}")
            }
        }
    }

    // Actions
    fun login(userId: String, pass: String) {
        val user = repository.checkLogin(userId, pass)
        if (user != null) {
            _currentUser.value = user
            _loginError.value = null
            _currentScreen.value = Screen.NfcTagging
            logNfc("로그인 성공: ${user.name} (${user.role})")
        } else {
            _loginError.value = "군번(ID) 또는 비밀번호가 불일치합니다."
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = Screen.Login
        _loginError.value = null
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun onLocationQueryChanged(query: String) {
        _locationQuery.value = query
        viewModelScope.launch {
            try {
                repository.searchGearPacks(query).collect {
                    _searchedGearPacks.value = it
                }
            } catch (e: Exception) {
                logNfc("검색 오류: ${e.message}")
            }
        }
    }

    fun selectGearPack(gearPack: GearPack?) {
        _selectedGearPack.value = gearPack
        if (gearPack != null) {
            viewModelScope.launch {
                try {
                    repository.getItemsByGearId(gearPack.gearId).collect {
                        _selectedGearPackItems.value = it
                    }
                } catch (e: Exception) {
                    logNfc("군장 품목 조회 오류: ${e.message}")
                }
            }
        } else {
            _selectedGearPackItems.value = emptyList()
        }
    }

    fun onItemSearchQueryChanged(query: String) {
        _itemSearchQuery.value = query
        viewModelScope.launch {
            try {
                repository.getItemsByName(query).collect {
                    _searchedItems.value = it
                }
            } catch (e: Exception) {
                logNfc("품목 검색 오류: ${e.message}")
            }
        }
    }

    fun selectItem(item: Item?) {
        _selectedItem.value = item
        if (item != null) {
            viewModelScope.launch {
                try {
                    val gear = repository.getGearPackById(item.gearId)
                    val box = gear?.parentBoxId?.let { repository.getPPBoxById(it) }
                    _selectedItemLocation.value = ItemLocation(item, gear, box)
                } catch (e: Exception) {
                    logNfc("품목 위치 조회 오류: ${e.message}")
                }
            }
        } else {
            _selectedItemLocation.value = null
        }
    }

    fun updateGearStatus(gearId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                repository.updateGearPackStatus(gearId, newStatus)
                // Refresh details
                val currentSelected = _selectedGearPack.value
                if (currentSelected != null && currentSelected.gearId == gearId) {
                    _selectedGearPack.value = currentSelected.copy(status = newStatus)
                }
                val currentScanned = _scannedGearPack.value
                if (currentScanned != null && currentScanned.gearId == gearId) {
                    _scannedGearPack.value = currentScanned.copy(status = newStatus)
                }
                // Update locations in Item Search if active
                val currentLoc = _selectedItemLocation.value
                if (currentLoc != null && currentLoc.gearPack?.gearId == gearId) {
                    _selectedItemLocation.value = currentLoc.copy(
                        gearPack = currentLoc.gearPack.copy(status = newStatus)
                    )
                }
                logNfc("군장 상태 변경 완료: $gearId -> $newStatus")
            } catch (e: Exception) {
                logNfc("상태 변경 오류: ${e.message}")
            }
        }
    }

    fun updatePPBoxManager(boxId: String, managerName: String) {
        viewModelScope.launch {
            try {
                repository.updatePPBoxAndGearsManager(boxId, managerName)
                logNfc("관리책임관 일괄 수정 완료: 박스 $boxId -> $managerName")
            } catch (e: Exception) {
                logNfc("관리책임관 수정 오류: ${e.message}")
            }
        }
    }

    // NFC Tag ID 수정 비즈니스 로직
    fun updateNfcTagId(oldTagId: String, newTagId: String) {
        viewModelScope.launch {
            try {
                logNfc("NFC 태그 ID 수정 시도: $oldTagId -> $newTagId")
                
                // 1. PP Box 인지 확인
                val box = repository.getPPBoxById(oldTagId)
                if (box != null) {
                    val success = repository.updatePPBoxId(oldTagId, newTagId)
                    if (success) {
                        logNfc("보관함 ID 수정 성공! ($oldTagId -> $newTagId)")
                        val updatedBox = repository.getPPBoxById(newTagId)
                        _scannedPPBox.value = updatedBox
                        _scannedGearPack.value = null
                        _isNfcWriting.value = false
                    } else {
                        logNfc("보관함 ID 수정 실패")
                    }
                    return@launch
                }

                // 2. Gear Pack 인지 확인
                val gear = repository.getGearPackById(oldTagId)
                if (gear != null) {
                    val success = repository.updateGearPackId(oldTagId, newTagId)
                    if (success) {
                        logNfc("군장 ID 수정 성공! ($oldTagId -> $newTagId)")
                        val updatedGear = repository.getGearPackById(newTagId)
                        _scannedGearPack.value = updatedGear
                        _scannedPPBox.value = null
                        _isNfcWriting.value = false
                    } else {
                        logNfc("군장 ID 수정 실패")
                    }
                    return@launch
                }

                logNfc("수정 실패: 대상을 찾을 수 없음 ($oldTagId)")
                _isNfcWriting.value = false
            } catch (e: Exception) {
                logNfc("NFC 태그 ID 수정 오류: ${e.message}")
                _isNfcWriting.value = false
            }
        }
    }

    // NFC Trigger Logic (Both simulator and real NFC receiver call this)
    fun triggerNfcTag(tagId: String) {
        logNfc("NFC 태그 감지: $tagId")
        if (_isNfcWriting.value) {
            val newSn = _generatedSn.value
            if (newSn.isNotEmpty() && tagId != newSn) {
                updateNfcTagId(tagId, newSn)
                return
            }
        }
        viewModelScope.launch {
            try {
                // Check if it is a PP Box
                val box = repository.getPPBoxById(tagId)
                if (box != null) {
                    logNfc("P.P 박스 확인됨: ${box.company} ${box.platoon} (NFC: ${box.boxId})")
                    _scannedPPBox.value = box
                    _scannedGearPack.value = null
                    repository.getGearPacksByBoxId(box.boxId).collect { gears ->
                        _scannedPPBoxGearPacks.value = gears
                    }
                    _currentScreen.value = Screen.NfcTagging
                    return@launch
                }

                // Check if it is a Gear Pack
                val gear = repository.getGearPackById(tagId)
                if (gear != null) {
                    logNfc("군장 확인됨: ${gear.company} ${gear.platoon} ${gear.squad} ${gear.position} (NFC: ${gear.gearId})")
                    _scannedGearPack.value = gear
                    _scannedPPBox.value = null
                    repository.getItemsByGearId(gear.gearId).collect { items ->
                        _selectedGearPackItems.value = items
                    }
                    _currentScreen.value = Screen.NfcTagging
                    return@launch
                }

                // Unknown tag
                logNfc("미등록 NFC 태그: $tagId")
                _scannedGearPack.value = null
                _scannedPPBox.value = null
                _scannedPPBoxGearPacks.value = emptyList()
                _currentScreen.value = Screen.NfcTagging
            } catch (e: Exception) {
                logNfc("NFC 태그 처리 오류: ${e.message}")
            }
        }
    }

    fun clearNfcScan() {
        _scannedGearPack.value = null
        _scannedPPBox.value = null
        _scannedPPBoxGearPacks.value = emptyList()
    }

    // Exposed Flows for 제원등록/수정
    val allGearPacksFlow = repository.getAllGearPacks()
    val allItemsFlow = repository.getAllItems()
    val allPPBoxesFlow = repository.getAllPPBoxes()

    fun saveItem(item: Item, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.insertItem(item)
                logNfc("DB 품목 저장 완료: ${item.itemName} (${item.itemId})")
                onSuccess()
            } catch (e: Exception) {
                logNfc("DB 에러 (품목 저장 실패): ${e.message}")
            }
        }
    }

    fun saveGearPack(gear: GearPack, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.insertGearPack(gear)
                logNfc("DB 군장 저장 완료: ${gear.company} ${gear.platoon} (${gear.gearId})")
                onSuccess()
            } catch (e: Exception) {
                logNfc("DB 에러 (군장 저장 실패): ${e.message}")
            }
        }
    }

    fun savePPBox(box: PPBox, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.insertPPBox(box)
                logNfc("DB 보관함 저장 완료: ${box.company} ${box.platoon} (${box.boxId})")
                onSuccess()
            } catch (e: Exception) {
                logNfc("DB 에러 (보관함 저장 실패): ${e.message}")
            }
        }
    }

    private fun logNfc(message: String) {
        val currentList = _nfcLogs.value.toMutableList()
        currentList.add(0, "[${System.currentTimeMillis() % 100000}] $message")
        _nfcLogs.value = currentList.take(50)
    }
}

class MilitaryViewModelFactory(private val repository: MilitaryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MilitaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MilitaryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
