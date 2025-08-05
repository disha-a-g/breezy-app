package com.whirlyfan.breezy.api

import android.content.Context
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

fun parseSupabaseError(errorString: String): String {
    val urlIndex = errorString.indexOf("URL:")
    return if (urlIndex > 0) {
        errorString.substring(0, urlIndex).trim()
    } else {
        errorString.trim()
    }
}

class BreezyAPI(
    context: Context,
) {
    private val supabaseUrl = context.getString(com.whirlyfan.breezy.R.string.supabase_url)
    private val supabaseKey = context.getString(com.whirlyfan.breezy.R.string.supabase_api_key)

    val client =
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey,
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
}
