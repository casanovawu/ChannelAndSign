package com.wpf.util.common

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

val json = Json { encodeDefaults = true }
val settings = Settings()