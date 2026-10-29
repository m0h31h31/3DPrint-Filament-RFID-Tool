package com.m0h31h31.bamburfidreader.cloud

enum class BambuCloudRegion(
    val storedValue: String,
    val apiBaseUrl: String,
    val mqttUrl: String,
    val captchaBaseUrl: String
) {
    CHINA("china", "https://api.bambulab.cn", "ssl://cn.mqtt.bambulab.com:8883", "https://bambulab.cn/"),
    GLOBAL("global", "https://api.bambulab.com", "ssl://us.mqtt.bambulab.com:8883", "https://bambulab.com/");

    companion object {
        // Sessions saved before region support belong to the China service.
        fun fromStoredValue(value: String?): BambuCloudRegion =
            entries.firstOrNull { it.storedValue == value } ?: CHINA
    }
}
