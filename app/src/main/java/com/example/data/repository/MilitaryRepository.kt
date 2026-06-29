package com.example.data.repository

import com.example.data.local.GearPackDao
import com.example.data.local.ItemDao
import com.example.data.local.PPBoxDao
import com.example.data.model.GearPack
import com.example.data.model.Item
import com.example.data.model.PPBox
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

class MilitaryRepository(
    private val ppBoxDao: PPBoxDao,
    private val gearPackDao: GearPackDao,
    private val itemDao: ItemDao
) {
    // 지정 관리자 계정 하드코딩 검증 로직
    private val preDefinedUsers = listOf(
        User("25-72026329", "관리자", "관리자")
    )

    private val userPasswords = mapOf(
        "25-72026329" to "thsgkdyd1004!"
    )

    fun checkLogin(userId: String, password: String): User? {
        val expectedPassword = userPasswords[userId]
        return if (expectedPassword != null && expectedPassword == password) {
            preDefinedUsers.find { it.id == userId }
        } else {
            null
        }
    }

    // PP Box
    fun getAllPPBoxes(): Flow<List<PPBox>> = ppBoxDao.getAllPPBoxes()
    
    suspend fun getPPBoxById(boxId: String): PPBox? = ppBoxDao.getPPBoxById(boxId)

    // Gear Pack
    fun getAllGearPacks(): Flow<List<GearPack>> = gearPackDao.getAllGearPacks()
    
    fun getGearPackFlowById(gearId: String): Flow<GearPack?> = gearPackDao.getGearPackFlowById(gearId)
    
    suspend fun getGearPackById(gearId: String): GearPack? = gearPackDao.getGearPackById(gearId)
    
    fun getGearPacksByBoxId(boxId: String): Flow<List<GearPack>> = gearPackDao.getGearPacksByBoxId(boxId)
    
    fun searchGearPacks(query: String): Flow<List<GearPack>> = gearPackDao.searchGearPacks(query)
    
    suspend fun updateGearPackStatus(gearId: String, status: String) = gearPackDao.updateGearPackStatus(gearId, status)

    suspend fun updatePPBoxAndGearsManager(boxId: String, managerName: String) {
        ppBoxDao.updatePPBoxManager(boxId, managerName)
        gearPackDao.updateGearPacksManagerByBoxId(boxId, managerName)
    }

    suspend fun updatePPBoxId(oldBoxId: String, newBoxId: String): Boolean {
        val oldBox = ppBoxDao.getPPBoxById(oldBoxId) ?: return false
        val newBox = oldBox.copy(boxId = newBoxId)
        ppBoxDao.insertPPBox(newBox)
        gearPackDao.updateGearPackParentBoxId(oldBoxId, newBoxId)
        ppBoxDao.deletePPBoxById(oldBoxId)
        return true
    }

    suspend fun updateGearPackId(oldGearId: String, newGearId: String): Boolean {
        val oldGear = gearPackDao.getGearPackById(oldGearId) ?: return false
        val newGear = oldGear.copy(gearId = newGearId)
        gearPackDao.insertGearPack(newGear)
        itemDao.updateItemsGearId(oldGearId, newGearId)
        gearPackDao.deleteGearPackById(oldGearId)
        return true
    }

    // Items
    fun getAllItems(): Flow<List<Item>> = itemDao.getAllItems()
    
    fun getItemsByGearId(gearId: String): Flow<List<Item>> = itemDao.getItemsByGearId(gearId)
    
    fun getItemsByName(itemName: String): Flow<List<Item>> = itemDao.getItemsByName(itemName)

    suspend fun insertItem(item: Item) = itemDao.insertItem(item)

    suspend fun insertGearPack(gear: GearPack) = gearPackDao.insertGearPack(gear)

    suspend fun insertPPBox(box: PPBox) = ppBoxDao.insertPPBox(box)
}
