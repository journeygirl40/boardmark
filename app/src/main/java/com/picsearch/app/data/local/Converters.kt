package com.picsearch.app.data.local

import androidx.room.TypeConverter
import com.picsearch.app.domain.model.FetchStatus
import java.time.Instant

class Converters {

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(epochMilli: Long?): Instant? = epochMilli?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromFetchStatus(status: FetchStatus): String = status.name

    @TypeConverter
    fun toFetchStatus(name: String): FetchStatus = FetchStatus.valueOf(name)
}
