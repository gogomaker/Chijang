package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gear_packs")
data class GearPack(
    @PrimaryKey val gearId: String,  // NFC 일련번호
    val company: String,            // 중대
    val platoon: String,            // 소대
    val squad: String,              // 분대
    val position: String,           // 직책
    val status: String,             // 지급 현황 ("창고 안" / "창고 밖")
    val parentBoxId: String? = null, // 소속 박스 ID (NFC 일련번호)
    val managerName: String = "미지정" // 관리책임관
)
