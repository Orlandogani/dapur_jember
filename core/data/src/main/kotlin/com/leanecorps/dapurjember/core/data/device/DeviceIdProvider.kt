package com.leanecorps.dapurjember.core.data.device

import android.content.Context
import com.leanecorps.dapurjember.core.common.id.UuidV7
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The stable id of this install, written on every row's `device_id` and every `change_log`
 * entry. Generated once (UUIDv7) and kept in a plain SharedPreferences file — it is not a
 * secret, it just needs to be constant for the life of the install and unique across
 * terminals (docs/2-architecture §4.3).
 */
@Singleton
class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val value: String by lazy {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY, null) ?: UuidV7.generate().also { prefs.edit().putString(KEY, it).apply() }
    }

    fun deviceId(): String = value

    private companion object {
        const val PREFS_NAME = "dapurjember_device"
        const val KEY = "device_id"
    }
}
