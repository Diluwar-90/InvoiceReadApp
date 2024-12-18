package com.example.autoformfillup

import DatePickerDocked
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import com.example.autoformfillup.ui.theme.AutoFormFillUpTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoFormFillUpTheme {
                BillEntryScreen()
                //DatePickerDocked()
            }
        }
    }
}
