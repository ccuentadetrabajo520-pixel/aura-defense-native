package com.aura.defense.quicktile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.aura.defense.MainActivity

class AuraQuickTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply { label = "Aura"; state = Tile.STATE_INACTIVE; contentDescription = "Abrir Aura"; updateTile() }
    }

    override fun onClick() {
        super.onClick()
        runCatching {
            val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivityAndCollapse(intent)
        }
    }
}
