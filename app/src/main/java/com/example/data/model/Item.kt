package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey val itemId: String, // 일련번호
    val itemName: String,          // 품목명 (예: 모포, 탄창, 방독면 등)
    val productionYear: String,    // 생산년도
    val gearId: String             // 소속 군장 ID
)
