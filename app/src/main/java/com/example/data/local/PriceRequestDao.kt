package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PriceRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceRequestDao {

    @Query("SELECT * FROM price_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<PriceRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: PriceRequest)

    @Delete
    suspend fun deleteRequest(request: PriceRequest)

    @Query("SELECT * FROM price_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): PriceRequest?
}
