//This File is to be used to Google Calendar
package com.example.outfitoftheday

data class GetEventModel(
    var id: Int = 0,
    var summary: String? = "",
    var startDate: String = "",
)