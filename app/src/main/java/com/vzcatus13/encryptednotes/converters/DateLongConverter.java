package com.vzcatus13.encryptednotes.converters;

import androidx.room.TypeConverter;

import java.util.Date;


public class DateLongConverter {

    /**
     * Converter for RoomDB from long to Date
     * @param value Long to be converted
     * @return Date instance
     */
    @TypeConverter
    public static Date fromLong(Long value) {
        return value == null ? null : new Date(value);
    }

    /**
     * Converter for RoomDB from Date to long
     * @param date Date to be converted
     * @return Long value of date
     */
    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }
}
