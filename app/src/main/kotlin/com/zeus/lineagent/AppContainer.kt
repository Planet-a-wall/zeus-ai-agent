package com.zeus.lineagent

import android.content.Context
import com.zeus.lineagent.data.SettingsRepository
import com.zeus.lineagent.network.AsanaRepository
import com.zeus.lineagent.network.LineRepository
import com.zeus.lineagent.network.ZapierRepository

class AppContainer(context: Context) {
    val settings = SettingsRepository(context.applicationContext)
    val line = LineRepository()
    val asana = AsanaRepository()
    val zapier = ZapierRepository()
}
