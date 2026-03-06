package com.wifitracker.util

import java.util.Locale

/**
 * A Monday-first locale used for all date pickers in the app so that the
 * calendar week consistently starts on Monday regardless of the device locale.
 */
val MondayFirstLocale: Locale = Locale.forLanguageTag("en-GB")
