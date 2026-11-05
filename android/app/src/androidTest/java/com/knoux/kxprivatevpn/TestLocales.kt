package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Flow tests launch the real activity and pin the app appearance to English, so their
 * assertions have to read the same locale instead of the device locale. On an Arabic-locale
 * acceptance device `context.getString` alone would compare Arabic text against an English
 * render and fail for formatting reasons only.
 */
internal fun strings(context: Context, language: String = "en"): Context {
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(language))
    return context.createConfigurationContext(configuration)
}
