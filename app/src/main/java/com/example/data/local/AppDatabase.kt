package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.GearPack
import com.example.data.model.Item
import com.example.data.model.PPBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase

@Database(entities = [PPBox::class, GearPack::class, Item::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ppBoxDao(): PPBoxDao
    abstract fun gearPackDao(): GearPackDao
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    // Try to load SQLCipher native libraries and build encrypted DB
                    SQLiteDatabase.loadLibs(context)
                    val passphrase = "military_secret_key_passphrase_2026".toByteArray()
                    val factory = SupportFactory(passphrase)
                    
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "military_gear_secure.db"
                    )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()
                } catch (t: Throwable) {
                    // Fallback to standard unencrypted Room database if SQLCipher fails to load/initialize
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "military_gear_unsecure.db"
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()
                }
                INSTANCE = instance
                instance
            }
        }
    }
}

class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.beginTransaction()
        try {
            // Seed PPBoxes using raw SQL to avoid synchronization deadlock on AppDatabase.getDatabase
            db.execSQL("INSERT OR REPLACE INTO pp_boxes (boxId, company, platoon, managerName) VALUES ('BOX-101', '1중대', '1소대', '미지정')")
            db.execSQL("INSERT OR REPLACE INTO pp_boxes (boxId, company, platoon, managerName) VALUES ('BOX-102', '1중대', '2소대', '미지정')")
            db.execSQL("INSERT OR REPLACE INTO pp_boxes (boxId, company, platoon, managerName) VALUES ('BOX-201', '2중대', '1소대', '미지정')")
            
            // Seed GearPacks using raw SQL to avoid synchronization deadlock
            db.execSQL("INSERT OR REPLACE INTO gear_packs (gearId, company, platoon, squad, position, status, parentBoxId, managerName) VALUES ('GEAR-1111', '1중대', '1소대', '1분대', '소대장', '창고 안', 'BOX-101', '미지정')")
            db.execSQL("INSERT OR REPLACE INTO gear_packs (gearId, company, platoon, squad, position, status, parentBoxId, managerName) VALUES ('GEAR-1112', '1중대', '1소대', '1분대', '부소대장', '창고 안', 'BOX-101', '미지정')")
            db.execSQL("INSERT OR REPLACE INTO gear_packs (gearId, company, platoon, squad, position, status, parentBoxId, managerName) VALUES ('GEAR-1113', '1중대', '1소대', '1분대', '분대장', '창고 안', 'BOX-101', '미지정')")
            db.execSQL("INSERT OR REPLACE INTO gear_packs (gearId, company, platoon, squad, position, status, parentBoxId, managerName) VALUES ('GEAR-1114', '1중대', '1소대', '1분대', '소총수1', '창고 밖', 'BOX-101', '미지정')")
            db.execSQL("INSERT OR REPLACE INTO gear_packs (gearId, company, platoon, squad, position, status, parentBoxId, managerName) VALUES ('GEAR-1115', '1중대', '1소대', '1분대', '소총수2', '창고 밖', 'BOX-101', '미지정')")
            db.execSQL("INSERT OR REPLACE INTO gear_packs (gearId, company, platoon, squad, position, status, parentBoxId, managerName) VALUES ('GEAR-1121', '1중대', '2소대', '1분대', '분대장', '창고 안', 'BOX-102', '미지정')")
            db.execSQL("INSERT OR REPLACE INTO gear_packs (gearId, company, platoon, squad, position, status, parentBoxId, managerName) VALUES ('GEAR-2111', '2중대', '1소대', '1분대', '분대장', '창고 안', 'BOX-201', '미지정')")
            
            // Seed Items using raw SQL to avoid synchronization deadlock
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-A1', '모포', '2021', 'GEAR-1111')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-A2', '방독면', '2020', 'GEAR-1111')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-A3', '탄창', '2023', 'GEAR-1111')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-A4', '야전삽', '2019', 'GEAR-1111')")
            
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-B1', '모포', '2021', 'GEAR-1112')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-B2', '방독면', '2021', 'GEAR-1112')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-B3', '탄창', '2023', 'GEAR-1112')")
            
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-C1', '모포', '2022', 'GEAR-1113')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-C2', '방독면', '2021', 'GEAR-1113')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-C3', '탄창', '2023', 'GEAR-1113')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-C4', '무전기', '2024', 'GEAR-1113')")
            
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-D1', '모포', '2022', 'GEAR-1114')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-D2', '방독면', '2022', 'GEAR-1114')")
            
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-E1', '모포', '2023', 'GEAR-1115')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-E2', '방독면', '2022', 'GEAR-1115')")
            
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-F1', '모포', '2020', 'GEAR-1121')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-F2', '탄창', '2022', 'GEAR-1121')")
            
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-G1', '모포', '2019', 'GEAR-2111')")
            db.execSQL("INSERT OR REPLACE INTO items (itemId, itemName, productionYear, gearId) VALUES ('ITEM-G2', '방독면', '2018', 'GEAR-2111')")
            
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }
}
