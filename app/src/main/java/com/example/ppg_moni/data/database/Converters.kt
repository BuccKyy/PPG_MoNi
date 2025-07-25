package com.example.ppg_moni.data.database

import androidx.room.TypeConverter
import com.example.ppg_moni.data.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    
    @TypeConverter
    fun fromFloatArray(value: FloatArray?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toFloatArray(value: String): FloatArray? {
        return Gson().fromJson(value, FloatArray::class.java)
    }

    @TypeConverter
    fun fromGender(gender: Gender): String {
        return gender.name
    }

    @TypeConverter
    fun toGender(gender: String): Gender {
        return Gender.valueOf(gender)
    }

    @TypeConverter
    fun fromActivityLevel(level: ActivityLevel): String {
        return level.name
    }

    @TypeConverter
    fun toActivityLevel(level: String): ActivityLevel {
        return ActivityLevel.valueOf(level)
    }

    @TypeConverter
    fun fromSmokingStatus(status: SmokingStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSmokingStatus(status: String): SmokingStatus {
        return SmokingStatus.valueOf(status)
    }

    @TypeConverter
    fun fromAlcoholConsumption(consumption: AlcoholConsumption): String {
        return consumption.name
    }

    @TypeConverter
    fun toAlcoholConsumption(consumption: String): AlcoholConsumption {
        return AlcoholConsumption.valueOf(consumption)
    }

    @TypeConverter
    fun fromSignalQuality(quality: SignalQuality): String {
        return quality.name
    }

    @TypeConverter
    fun toSignalQuality(quality: String): SignalQuality {
        return SignalQuality.valueOf(quality)
    }

    @TypeConverter
    fun fromBPCategory(category: BPCategory): String {
        return category.name
    }

    @TypeConverter
    fun toBPCategory(category: String): BPCategory {
        return BPCategory.valueOf(category)
    }

    @TypeConverter
    fun fromRiskLevel(level: RiskLevel): String {
        return level.name
    }

    @TypeConverter
    fun toRiskLevel(level: String): RiskLevel {
        return RiskLevel.valueOf(level)
    }
} 