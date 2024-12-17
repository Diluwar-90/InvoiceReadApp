package com.example.autoformfillup

data class MobileBill(
    val employeeName: String,
    val monthYear: String,
    val billAmount: Double,
    val billImageUri: String? = null
)
