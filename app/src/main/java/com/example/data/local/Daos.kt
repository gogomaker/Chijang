package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GearPack
import com.example.data.model.Item
import com.example.data.model.PPBox
import kotlinx.coroutines.flow.Flow

@Dao
interface PPBoxDao {
    @Query("SELECT * FROM pp_boxes")
    fun getAllPPBoxes(): Flow<List<PPBox>>

    @Query("SELECT * FROM pp_boxes WHERE boxId = :boxId LIMIT 1")
    suspend fun getPPBoxById(boxId: String): PPBox?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPPBox(box: PPBox)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPPBoxes(boxes: List<PPBox>)

    @Query("UPDATE pp_boxes SET managerName = :managerName WHERE boxId = :boxId")
    suspend fun updatePPBoxManager(boxId: String, managerName: String)

    @Query("DELETE FROM pp_boxes WHERE boxId = :boxId")
    suspend fun deletePPBoxById(boxId: String)
}

@Dao
interface GearPackDao {
    @Query("SELECT * FROM gear_packs")
    fun getAllGearPacks(): Flow<List<GearPack>>

    @Query("SELECT * FROM gear_packs WHERE gearId = :gearId LIMIT 1")
    fun getGearPackFlowById(gearId: String): Flow<GearPack?>

    @Query("SELECT * FROM gear_packs WHERE gearId = :gearId LIMIT 1")
    suspend fun getGearPackById(gearId: String): GearPack?

    @Query("SELECT * FROM gear_packs WHERE parentBoxId = :boxId")
    fun getGearPacksByBoxId(boxId: String): Flow<List<GearPack>>

    @Query("SELECT * FROM gear_packs WHERE company LIKE '%' || :query || '%' OR platoon LIKE '%' || :query || '%' OR squad LIKE '%' || :query || '%' OR position LIKE '%' || :query || '%'")
    fun searchGearPacks(query: String): Flow<List<GearPack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGearPack(gear: GearPack)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGearPacks(gears: List<GearPack>)

    @Query("UPDATE gear_packs SET status = :status WHERE gearId = :gearId")
    suspend fun updateGearPackStatus(gearId: String, status: String)

    @Query("UPDATE gear_packs SET managerName = :managerName WHERE parentBoxId = :boxId")
    suspend fun updateGearPacksManagerByBoxId(boxId: String, managerName: String)

    @Query("DELETE FROM gear_packs WHERE gearId = :gearId")
    suspend fun deleteGearPackById(gearId: String)

    @Query("UPDATE gear_packs SET parentBoxId = :newBoxId WHERE parentBoxId = :oldBoxId")
    suspend fun updateGearPackParentBoxId(oldBoxId: String, newBoxId: String)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE gearId = :gearId")
    fun getItemsByGearId(gearId: String): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE itemName LIKE '%' || :itemName || '%'")
    fun getItemsByName(itemName: String): Flow<List<Item>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<Item>)

    @Query("UPDATE items SET gearId = :newGearId WHERE gearId = :oldGearId")
    suspend fun updateItemsGearId(oldGearId: String, newGearId: String)
}
