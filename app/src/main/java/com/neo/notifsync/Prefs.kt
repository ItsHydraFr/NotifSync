package com.neo.notifsync

import android.content.Context
import java.security.SecureRandom

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    var server: String
        get() = sp.getString("server", "https://ntfy.sh")!!.trimEnd('/')
        set(v) = sp.edit().putString("server", v.trim()).apply()
    var topic: String
        get() = sp.getString("topic", "")!!
        set(v) = sp.edit().putString("topic", v.trim()).apply()
    var token: String
        get() = sp.getString("token", "")!!
        set(v) = sp.edit().putString("token", v.trim()).apply()
    var forward: Boolean
        get() = sp.getBoolean("forward", true)
        set(v) = sp.edit().putBoolean("forward", v).apply()

    companion object {
        /** Long random topic: on a public server the topic name is the only "password". */
        fun randomTopic(): String {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            val r = SecureRandom()
            return "notifsync-" + (1..24).map { chars[r.nextInt(chars.length)] }.joinToString("")
        }
    }
}
