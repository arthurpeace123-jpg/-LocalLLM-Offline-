package com.example.localllm.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromModelType(value: ModelType): String = value.name
    
    @TypeConverter
    fun toModelType(value: String): ModelType = ModelType.valueOf(value)
    
    @TypeConverter
    fun fromRole(value: MessageRole): String = value.name
    
    @TypeConverter
    fun toRole(value: String): MessageRole = MessageRole.valueOf(value)
}
