package pk.groscale.feeregister.data.db

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Dates are stored as epoch day (Long): compact, sortable, and immune to the
 * timezone bugs that come with storing instants for something that is a calendar
 * date, not a moment in time.
 */
class Converters {
    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromEpochDay(day: Long?): LocalDate? = day?.let(LocalDate::ofEpochDay)
}
