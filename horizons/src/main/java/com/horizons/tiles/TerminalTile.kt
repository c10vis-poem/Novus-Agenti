package com.horizons.tiles

import android.content.Intent
import android.service.quicksettings.TileService
import com.horizons.MainActivity

class TerminalTile : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_LAUNCH_TAB, MainActivity.TAB_TERMINAL)
        }
        startActivityAndCollapse(intent)
    }
}
