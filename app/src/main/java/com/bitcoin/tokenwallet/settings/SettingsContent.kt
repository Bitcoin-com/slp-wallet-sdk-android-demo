package com.bitcoin.tokenwallet.settings

import android.content.Context
import com.bitcoin.tokenwallet.R
import java.util.ArrayList
import java.util.HashMap

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 */
class SettingsContent(context: Context) {

    /**
     * An item representing a piece of content.
     */
    data class SettingsItem(val destinationNavId: Int, val content: String) {
        override fun toString(): String = content
    }

    /**
     * An array of sample (dummy) items.
     */
    val ITEMS: MutableList<SettingsItem> = ArrayList()

    init {
        addItem(SettingsItem(R.id.backupFragment, context.resources.getString(R.string.setting_backup)))
        addItem(SettingsItem(R.id.restoreFragment, context.resources.getString(R.string.setting_restore)))
    }

    private fun addItem(item: SettingsItem) {
        ITEMS.add(item)
    }

    private fun makeDetails(position: Int): String {
        val builder = StringBuilder()
        builder.append("Details about Item: ").append(position)
        for (i in 0..position - 1) {
            builder.append("\nMore details information here.")
        }
        return builder.toString()
    }


}
