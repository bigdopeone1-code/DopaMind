package com.dopamind.app.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.dopamind.app.core.theme.DopaMindTheme
import com.dopamind.app.feature.dashboard.ui.DashboardScreen
import org.junit.Rule
import org.junit.Test

class DashboardScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun dashboard() {
        paparazzi.snapshot {
            DopaMindTheme {
                DashboardScreen(onNavigate = {})
            }
        }
    }
}
