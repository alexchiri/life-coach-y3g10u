package com.kroslabs.lifecoach.data.database

import androidx.room.TypeConverter
import com.kroslabs.lifecoach.data.model.ExperimentStatus
import com.kroslabs.lifecoach.data.model.ThemeMode
import com.kroslabs.lifecoach.data.model.TrackingMethod

class Converters {
    @TypeConverter
    fun fromThemeMode(value: ThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): ThemeMode = ThemeMode.valueOf(value)

    @TypeConverter
    fun fromExperimentStatus(value: ExperimentStatus): String = value.name

    @TypeConverter
    fun toExperimentStatus(value: String): ExperimentStatus = ExperimentStatus.valueOf(value)

    @TypeConverter
    fun fromTrackingMethod(value: TrackingMethod): String = value.name

    @TypeConverter
    fun toTrackingMethod(value: String): TrackingMethod = TrackingMethod.valueOf(value)
}
