package com.doomly.app

import android.content.Context

object SetupPreferences {
    fun completed(context: Context): Boolean = context.getSharedPreferences("doomly_setup", Context.MODE_PRIVATE)
        .getBoolean("complete", DoomStatsStore.snapshot(context).totalReels > 0)
    fun finish(context: Context) { context.getSharedPreferences("doomly_setup", Context.MODE_PRIVATE).edit().putBoolean("complete", true).apply() }
}
