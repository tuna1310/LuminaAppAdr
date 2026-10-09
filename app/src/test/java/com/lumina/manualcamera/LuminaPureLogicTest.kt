package com.lumina.manualcamera

import com.lumina.manualcamera.advanced.BracketingManager
import com.lumina.manualcamera.advanced.EnhancedRawCalculator
import com.lumina.manualcamera.data.model.ExposureBiasParameter
import com.lumina.manualcamera.data.model.ShutterSpeed
import com.lumina.manualcamera.data.model.WhiteBalanceParameter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LuminaPureLogicTest {

    // 1. Test định dạng Shutter Speed
    @Test
    fun testShutterSpeedFormatting() {
        val sAuto = ShutterSpeed(16_666_667L, isAuto = true)
        assertEquals("AUTO", sAuto.displayString())

        val sFast = ShutterSpeed(1_000_000L, isAuto = false) // 1/1000s
        assertEquals("1/1000s", sFast.displayString())

        val sQuarter = ShutterSpeed(250_000_000L, isAuto = false) // 1/4s
        assertEquals("1/4s", sQuarter.displayString())

        val sOneSecond = ShutterSpeed(1_000_000_000L, isAuto = false) // 1s
        assertEquals("1s", sOneSecond.displayString())

        val sTwoSeconds = ShutterSpeed(2_000_000_000L, isAuto = false) // 2s
        assertEquals("2s", sTwoSeconds.displayString())
    }

    // 2. Test Quy đổi Kelvin sang RGGB Vector thuần
    @Test
    fun testKelvinToRggbVector() {
        // Ánh sáng vàng ấm (Tungsten 2800K): Red Gain phải cao hơn Blue Gain
        val warmWb = WhiteBalanceParameter(2800, isAuto = false)
        val warmGains = warmWb.toRggbGains()
        assertTrue("Ở 2800K, Red Gain phải lớn hơn Blue Gain", warmGains.red > warmGains.blue)

        // Ánh sáng lạnh (Shade 8000K): Blue Gain phải cao hơn Red Gain
        val coolWb = WhiteBalanceParameter(8000, isAuto = false)
        val coolGains = coolWb.toRggbGains()
        assertTrue("Ở 8000K, Blue Gain phải lớn hơn Red Gain", coolGains.blue > coolGains.red)
    }

    // 3. Test Tính toán Exposure Bias (EV)
    @Test
    fun testExposureBiasCalculation() {
        val zeroEv = ExposureBiasParameter(0, 1f / 3f)
        assertEquals("0.0 EV", zeroEv.displayString())

        val plusOneEv = ExposureBiasParameter(3, 1f / 3f)
        assertEquals("+1.0 EV", plusOneEv.displayString())

        val minusTwoThirdsEv = ExposureBiasParameter(-2, 1f / 3f)
        assertEquals("-0.7 EV", minusTwoThirdsEv.displayString())
    }

    // 4. Test Thuật toán Enhanced RAW dựa trên Gyroscope
    @Test
    fun testEnhancedRawAlgorithm() {
        // Khi máy rất tĩnh (ví dụ Tripod: gyro = 0.01 rad/s)
        val tripodSetting = EnhancedRawCalculator.calculateOptimalExposure(
            gyroVarianceRadPerSec = 0.01f,
            minIso = 100,
            maxIso = 3200
        )
        // Cho phép phơi sáng lâu (1/8s = 125,000,000 ns) và ISO cực thấp
        assertEquals(125_000_000L, tripodSetting.shutterSpeedNs)
        assertTrue("ISO phải hạ xuống mức tối thiểu khi máy tĩnh", tripodSetting.iso <= 100)

        // Khi tay rung mạnh (gyro = 0.6 rad/s)
        val shakySetting = EnhancedRawCalculator.calculateOptimalExposure(
            gyroVarianceRadPerSec = 0.60f,
            minIso = 100,
            maxIso = 3200
        )
        // Phải đẩy shutter cực nhanh (1/500s = 2,000,000 ns) để chống rung mờ
        assertEquals(2_000_000L, shakySetting.shutterSpeedNs)
        assertTrue("ISO phải tăng lên bù sáng khi shutter cực nhanh", shakySetting.iso > tripodSetting.iso)
    }

    // 5. Test Thuật toán Bracketing HDR
    @Test
    fun testBracketingManager() {
        val baseShutterNs = 16_666_667L // 1/60s
        val baseIso = 200

        val bracketShots = BracketingManager.create3ShotBracket(baseShutterNs, baseIso)
        assertEquals(3, bracketShots.size)

        // Kiểm tra ISO được khóa cố định trên cả 3 tấm
        assertEquals(baseIso, bracketShots[0].iso)
        assertEquals(baseIso, bracketShots[1].iso)
        assertEquals(baseIso, bracketShots[2].iso)

        // Kiểm tra EV: tấm 1 thiếu sáng (-2 EV), tấm 2 chuẩn (0 EV), tấm 3 dư sáng (+2 EV)
        assertTrue(bracketShots[0].shutterSpeedNs < bracketShots[1].shutterSpeedNs)
        assertTrue(bracketShots[1].shutterSpeedNs < bracketShots[2].shutterSpeedNs)
    }
}
