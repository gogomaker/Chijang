package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pp_boxes")
data class PPBox(
    @PrimaryKey val boxId: String, // NFC 일련번호
    val company: String,          // 중대
    val platoon: String,           // 소대
    val managerName: String = "미지정" // 관리책임관
)
