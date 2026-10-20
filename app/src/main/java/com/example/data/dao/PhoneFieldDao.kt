package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.PhoneField
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneFieldDao {

    @Query("SELECT * FROM phone_fields WHERE phoneId = :phoneId ORDER BY columnIndex ASC")
    fun getFieldsForPhone(phoneId: Long): Flow<List<PhoneField>>

    @Query("SELECT * FROM phone_fields WHERE phoneId = :phoneId ORDER BY columnIndex ASC")
    suspend fun getFieldsForPhoneSync(phoneId: Long): List<PhoneField>

    @Query("SELECT * FROM phone_fields WHERE phoneId = :phoneId AND category = :category ORDER BY columnIndex ASC")
    fun getFieldsByCategory(phoneId: Long, category: String): Flow<List<PhoneField>>

    @Query("SELECT * FROM phone_fields WHERE phoneId = :phoneId AND category = :category ORDER BY columnIndex ASC")
    suspend fun getFieldsByCategorySync(phoneId: Long, category: String): List<PhoneField>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFields(fields: List<PhoneField>)

    @Query("DELETE FROM phone_fields WHERE phoneId = :phoneId")
    suspend fun deleteFieldsForPhone(phoneId: Long)

    @Update
    suspend fun updateField(field: PhoneField)

    @Query("UPDATE phone_fields SET usageCount = usageCount + 1 WHERE id = :fieldId")
    suspend fun incrementFieldUsage(fieldId: Long)

    @Query("UPDATE phone_fields SET isFavorite = :isFavorite WHERE id = :fieldId")
    suspend fun setFieldFavorite(fieldId: Long, isFavorite: Boolean)

    @Query("""
        SELECT * FROM phone_fields 
        WHERE fieldName LIKE '%' || :query || '%' 
           OR fieldValue LIKE '%' || :query || '%'
        ORDER BY usageCount DESC LIMIT 50
    """)
    fun searchFields(query: String): Flow<List<PhoneField>>

    @Query("""
        SELECT * FROM phone_fields 
        WHERE fieldName LIKE '%' || :query || '%' 
           OR fieldValue LIKE '%' || :query || '%'
        ORDER BY usageCount DESC LIMIT 50
    """)
    suspend fun searchFieldsSync(query: String): List<PhoneField>

    @Query("SELECT * FROM phone_fields WHERE isFavorite = 1 ORDER BY usageCount DESC")
    fun getFavoriteFields(): Flow<List<PhoneField>>
}
