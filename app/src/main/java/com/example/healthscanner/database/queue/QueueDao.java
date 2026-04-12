package com.example.healthscanner.database.queue;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface QueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DataQueueEntity entity);

    @Update
    void update(DataQueueEntity entity);

    @Delete
    void delete(DataQueueEntity entity);

    @Query("SELECT * FROM data_queue WHERE status = 'pending' OR status = 'failed' ORDER BY timestamp ASC")
    List<DataQueueEntity> getPendingUploads();
    
    @Query("SELECT COUNT(*) FROM data_queue WHERE barcode = :barcode AND timestamp > :timeThreshold")
    int getRecentUploadsCount(String barcode, long timeThreshold);
}
