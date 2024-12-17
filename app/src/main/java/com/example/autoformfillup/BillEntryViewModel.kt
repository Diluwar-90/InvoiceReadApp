package com.example.autoformfillup

import android.util.Log
import androidx.lifecycle.ViewModel

class BillEntryViewModel : ViewModel() {

    private val _bills = mutableListOf<MobileBill>()

    fun submitBill(mobileBill: MobileBill) {
        _bills.add(mobileBill)
        Log.d("BillEntryViewModel", "Bill submitted successfully: $mobileBill")
    }
}