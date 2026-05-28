package com.chifamba.sonyblegeotag

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar
import java.util.TimeZone

object GpsPayloadPacker {

    /**
     * Constructs the exact 26-byte binary payload required by Sony cameras.
     *
     * Format:
     * - Bytes 0-1:   0x005D (Header/Command ID) - Big-Endian uint16
     * - Byte 2:      Timezone Offset - int8 (Signed hours relative to UTC)
     * - Byte 3:      DST Offset - int8 (Daylight Saving Offset in hours)
     * - Bytes 4-5:   0xFC03 - uint16 (representing little-endian 0x03FC padding)
     * - Bytes 6-10:  Padding bytes - 5s (b'\x00\x00\x10\x10\x10')
     * - Bytes 11-14: Latitude * 10^7 - Big-Endian int32
     * - Bytes 15-18: Longitude * 10^7 - Big-Endian int32
     * - Bytes 19-20: Year - Big-Endian uint16
     * - Byte 21:     Month - uint8 (1-12)
     * - Byte 22:     Day - uint8 (1-31)
     * - Byte 23:     Hour - uint8 (0-23) - UTC
     * - Byte 24:     Minute - uint8 (0-59) - UTC
     * - Byte 25:     Second - uint8 (0-59) - UTC
     */
    fun packPayload(
        latitude: Double,
        longitude: Double,
        timestampMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): ByteArray {
        val buffer = ByteBuffer.allocate(26).apply {
            order(ByteOrder.BIG_ENDIAN)
        }

        // Get offsets in hours
        val rawOffsetMs = timeZone.rawOffset
        val tzOffsetHours = rawOffsetMs / (3600 * 1000)

        // Calculate DST offset active at the given timestamp
        val isDst = timeZone.inDaylightTime(java.util.Date(timestampMs))
        val dstOffsetHours = if (isDst) {
            timeZone.dstSavings / (3600 * 1000)
        } else {
            0
        }

        // Convert coordinates to integer scale 10^7
        val latVal = (latitude * 10_000_000.0).coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toInt()
        val lonVal = (longitude * 10_000_000.0).coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toInt()

        // Extract UTC datetime components
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = timestampMs
        }
        val year = utcCalendar.get(Calendar.YEAR)
        val month = utcCalendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-indexed
        val day = utcCalendar.get(Calendar.DAY_OF_MONTH)
        val hour = utcCalendar.get(Calendar.HOUR_OF_DAY)
        val minute = utcCalendar.get(Calendar.MINUTE)
        val second = utcCalendar.get(Calendar.SECOND)

        // Byte Packing
        buffer.putShort(0x005D.toShort())                  // Bytes 0-1: Header
        buffer.put(tzOffsetHours.toByte())                 // Byte 2: Timezone Offset
        buffer.put(dstOffsetHours.toByte())                // Byte 3: DST Offset
        buffer.putShort(0xFC03.toShort())                  // Bytes 4-5: Status/Padding
        buffer.put(byteArrayOf(0x00, 0x00, 0x10, 0x10, 0x10)) // Bytes 6-10: Control Padding
        buffer.putInt(latVal)                              // Bytes 11-14: Latitude
        buffer.putInt(lonVal)                              // Bytes 15-18: Longitude
        buffer.putShort(year.toShort())                    // Bytes 19-20: Year
        buffer.put(month.toByte())                         // Byte 21: Month
        buffer.put(day.toByte())                           // Byte 22: Day
        buffer.put(hour.toByte())                          // Byte 23: Hour
        buffer.put(minute.toByte())                        // Byte 24: Minute
        buffer.put(second.toByte())                        // Byte 25: Second

        return buffer.array()
    }
}
