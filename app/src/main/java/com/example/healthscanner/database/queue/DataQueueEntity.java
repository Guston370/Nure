package com.example.healthscanner.database.queue;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "data_queue")
public class DataQueueEntity {

    @PrimaryKey
    @NonNull
    public String uuid;

    public String label;
    public String barcode;
    public String localImagePath;
    public long timestamp;
    public String source;
    public String deviceId;
    
    // Status can be: 'pending', 'uploading', 'failed'
    public String status;

    public DataQueueEntity(@NonNull String uuid, String label, String barcode, 
                           String localImagePath, long timestamp, String source, 
                           String deviceId, String status) {
        this.uuid = uuid;
        this.label = label;
        this.barcode = barcode;
        this.localImagePath = localImagePath;
        this.timestamp = timestamp;
        this.source = source;
        this.deviceId = deviceId;
        this.status = status;
    }
}
