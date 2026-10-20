package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.PhoneRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneDao {

    @Query("SELECT * FROM phones ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllPhones(): Flow<List<PhoneRecord>>

    @Query("SELECT * FROM phones ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllPhonesSync(): List<PhoneRecord>

    @Query("SELECT * FROM phones WHERE brand = :brand ORDER BY model ASC")
    fun getPhonesByBrand(brand: String): Flow<List<PhoneRecord>>

    @Query("SELECT * FROM phones WHERE brand = :brand ORDER BY model ASC")
    suspend fun getPhonesByBrandSync(brand: String): List<PhoneRecord>

    @Query("SELECT DISTINCT brand FROM phones ORDER BY brand ASC")
    fun getAllBrands(): Flow<List<String>>

    @Query("SELECT DISTINCT brand FROM phones ORDER BY brand ASC")
    suspend fun getAllBrandsSync(): List<String>

    @Query("SELECT * FROM phones WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoritePhones(): Flow<List<PhoneRecord>>

    @Query("SELECT * FROM phones WHERE usageCount > 0 ORDER BY usageCount DESC, updatedAt DESC LIMIT 20")
    fun getRecentPhones(): Flow<List<PhoneRecord>>

    @Query("SELECT * FROM phones WHERE id = :id LIMIT 1")
    fun getPhoneById(id: Long): Flow<PhoneRecord?>

    @Query("SELECT * FROM phones WHERE id = :id LIMIT 1")
    suspend fun getPhoneByIdSync(id: Long): PhoneRecord?

    @Query("""
        SELECT * FROM phones 
        WHERE (LOWER(brand) = LOWER(:brand) AND LOWER(model) = LOWER(:model))
           OR LOWER(fullName) = LOWER(:fullName)
        LIMIT 1
    """)
    suspend fun findDuplicate(brand: String, model: String, fullName: String): PhoneRecord?

    @Query("""
        SELECT * FROM phones 
        WHERE brand LIKE '%' || :query || '%' 
           OR model LIKE '%' || :query || '%' 
           OR fullName LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchPhones(query: String): Flow<List<PhoneRecord>>

    @Query("""
        SELECT DISTINCT p.* FROM phones p
        LEFT JOIN phone_fields f ON p.id = f.phoneId
        WHERE p.brand LIKE '%' || :query || '%'
           OR p.model LIKE '%' || :query || '%'
           OR p.fullName LIKE '%' || :query || '%'
           OR f.fieldName LIKE '%' || :query || '%'
           OR f.fieldValue LIKE '%' || :query || '%'
        ORDER BY p.isPinned DESC, p.updatedAt DESC
    """)
    fun searchPhonesWithSpecs(query: String): Flow<List<PhoneRecord>>

    @Query("""
        SELECT DISTINCT p.* FROM phones p
        LEFT JOIN phone_fields f ON p.id = f.phoneId
        WHERE p.brand LIKE '%' || :query || '%'
           OR p.model LIKE '%' || :query || '%'
           OR p.fullName LIKE '%' || :query || '%'
           OR f.fieldName LIKE '%' || :query || '%'
           OR f.fieldValue LIKE '%' || :query || '%'
        ORDER BY p.isPinned DESC, p.updatedAt DESC
    """)
    suspend fun searchPhonesWithSpecsSync(query: String): List<PhoneRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhone(phone: PhoneRecord): Long

    @Update
    suspend fun updatePhone(phone: PhoneRecord)

    @Delete
    suspend fun deletePhone(phone: PhoneRecord)

    @Query("DELETE FROM phones WHERE id = :id")
    suspend fun deletePhoneById(id: Long)

    @Query("UPDATE phones SET usageCount = usageCount + 1, updatedAt = :time WHERE id = :id")
    suspend fun incrementPhoneUsage(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE phones SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE phones SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("DELETE FROM phones")
    suspend fun clearAllPhones()
}
