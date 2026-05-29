package com.chifamba.sonyblegeotag

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.util.TimeZone

class GpsPayloadPackerTest {

    @Test
    fun testPackPayload_coordinatesAndDateTime() {
        // Arrange
        val latitude = 37.7749
        val longitude = -122.4194
        
        // 2026-05-28 20:30:15 UTC
        // timestamp: 1780000215000L
        val timestampMs = 1780000215000L 
        val timeZone = TimeZone.getTimeZone("UTC")

        // Act
        val payload = GpsPayloadPacker.packPayload(latitude, longitude, timestampMs, timeZone)

        // Assert
        assertEquals(26, payload.size)
        
        val buffer = ByteBuffer.wrap(payload)
        
        // Header
        assertEquals(0x005D.toShort(), buffer.short)
        
        // Timezone & DST Offsets (UTC has 0 offsets)
        assertEquals(0.toByte(), buffer.get())
        assertEquals(0.toByte(), buffer.get())
        
        // Status/Padding and Control Padding
        assertEquals(0xFC03.toShort(), buffer.short)
        
        val padding = ByteArray(5)
        buffer.get(padding)
        assertEquals(0x00.toByte(), padding[0])
        assertEquals(0x00.toByte(), padding[1])
        assertEquals(0x10.toByte(), padding[2])
        assertEquals(0x10.toByte(), padding[3])
        assertEquals(0x10.toByte(), padding[4])
        
        // Latitude (37.7749 * 10^7 = 377749000)
        assertEquals(377749000, buffer.int)
        
        // Longitude (-122.4194 * 10^7 = -1224194000)
        assertEquals(-1224194000, buffer.int)
        
        // Year
        assertEquals(2026.toShort(), buffer.short)
        
        // Month (May -> 5)
        assertEquals(5.toByte(), buffer.get())
        
        // Day
        assertEquals(28.toByte(), buffer.get())
        
        // Hour
        assertEquals(20.toByte(), buffer.get())
        
        // Minute
        assertEquals(30.toByte(), buffer.get())
        
        // Second
        assertEquals(15.toByte(), buffer.get())
    }
}
