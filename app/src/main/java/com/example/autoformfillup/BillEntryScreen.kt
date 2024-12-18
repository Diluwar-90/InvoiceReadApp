package com.example.autoformfillup

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import convertMillisToDate
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEntryScreen(viewModel: BillEntryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {

    val scrollState = rememberScrollState()

    var billEntryDate by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf(TextFieldValue("")) }
    var monthYear by remember { mutableStateOf(TextFieldValue("")) }
    var eligibleBillAmount by remember { mutableStateOf(TextFieldValue("")) }
    var billImageUri by remember { mutableStateOf<Uri?>(null) }
    var hasCameraPermission by rememberSaveable { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("-- Select Month --") }
    val options = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    var billDate by remember { mutableStateOf("") }
    var receiptNum by remember { mutableStateOf(TextFieldValue("")) }
    var billPeriodStartDate by remember { mutableStateOf("") }
    var billPeriodEndDate by remember { mutableStateOf("") }

    //var showDatePicker by remember { mutableStateOf(false) }
    var billEntryDatePickerState by remember { mutableStateOf(false) }
    var billDatePickerState by remember { mutableStateOf(false) }
    var billStartDatePickerState by remember { mutableStateOf(false) }
    var billEndDatePickerState by remember { mutableStateOf(false) }

   /* var selectedDate = datePickerState.selectedDateMillis?.let {
       convertMillisToDate(it)
    } ?: ""*/


    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Log.e("BillEntryScreen", "Camera permission denied!")
        }
    }

    val imageCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            Log.d("BillEntryScreen", "Extracted Text: $bitmap")
            bitmap?.let {
                billImageUri = saveImageToCache(context, bitmap)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            billImageUri = uri
            processImageFromUri(context,uri){ extractedText ->
                Log.d("BillEntryScreen", "Extracted Text: $extractedText")
                //fillFormWithExtractedText(extractedText)
                billAmount = TextFieldValue(extractEmployeeName(extractedText))
                monthYear = TextFieldValue(extractMonthYear(extractedText))
                billAmount = TextFieldValue(extractBillAmount(extractedText))
            }
            Log.d("BillEntryScreen", "Extracted Text: $uri")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mobile Bill Reimbursement") },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF6200EA)),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState,true)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            //Bill Entry Date
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = billEntryDate,
                    onValueChange = { billEntryDate = it },
                    label = { Text("Bill Entry Date") },
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { billEntryDatePickerState = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                )

               /* if (showDatePicker) {
                    Popup(
                        onDismissRequest = { showDatePicker = false },
                        alignment = Alignment.TopStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 64.dp)
                                .shadow(elevation = 4.dp)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            DatePicker(
                                state = datePickerState,
                                showModeToggle = true
                            )
                        }
                    }
                }*/

                if (billEntryDatePickerState) {
                    DatePickerModal(
                        onDateSelected = { selectedDateMillis ->
                            selectedDateMillis?.let {
                                billEntryDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                            }
                            billEntryDatePickerState = false
                        },
                        onDismiss = { billEntryDatePickerState = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            //Reimbursement month
            Box {
                OutlinedTextField(
                    value = selectedOption,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            Modifier.clickable { expanded = true }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            onClick = {
                                selectedOption = option
                                expanded = false
                            },
                            text =  {
                                Text(option)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            //Bill Date
            OutlinedTextField(
                value = billDate,
                onValueChange = {billDate = it},
                label = { Text("Bill Date") },
                enabled = true,
                trailingIcon =
                {
                    IconButton(onClick = { billDatePickerState = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendar"
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (billDatePickerState) {
                DatePickerModal(
                    onDateSelected = { selectedDateMillis ->
                        selectedDateMillis?.let {
                            billDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                        }
                        billDatePickerState = false
                    },
                    onDismiss = { billDatePickerState = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            //Receipt Number
            OutlinedTextField(
                value = receiptNum,
                onValueChange = {receiptNum = it},
                label = { Text("Receipt Number") },
                enabled = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            //Bill Period Start Date
            OutlinedTextField(
                value = billPeriodStartDate,
                onValueChange = {billPeriodStartDate = it},
                label = { Text("Bill Period Start Date") },
                enabled = true,
                trailingIcon = {
                IconButton(onClick = { billStartDatePickerState = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar"
                    )
                }
             },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (billStartDatePickerState) {
                DatePickerModal(
                    onDateSelected = { selectedDateMillis ->
                        selectedDateMillis?.let {
                            billPeriodStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                        }
                        billStartDatePickerState = false
                    },
                    onDismiss = { billStartDatePickerState = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            //Bill Period End Dade
            OutlinedTextField(
                value = billPeriodEndDate,
                onValueChange = {billPeriodEndDate = it},
                label = { Text("Bill Period End Date") },
                enabled = true,
                trailingIcon =
                    {
                        IconButton(onClick = { billEndDatePickerState = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Calendar"
                            )
                        }
                    },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (billEndDatePickerState) {
                DatePickerModal(
                    onDateSelected = { selectedDateMillis ->
                        selectedDateMillis?.let {
                            billPeriodEndDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                        }
                        billEndDatePickerState = false
                    },
                    onDismiss = { billEndDatePickerState = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bill Amount
            OutlinedTextField(
                value = billAmount,
                onValueChange = { billAmount = it },
                label = { Text("Bill Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            // Eligible Bill Amount
            OutlinedTextField(
                value = eligibleBillAmount,
                onValueChange = { eligibleBillAmount = it },
                label = { Text("Eligible Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showImageSourceDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upload Bill Image")
            }

            billImageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Bill Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(150.dp)
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = {
                    val mobileBill = MobileBill(
                        employeeName = billAmount.text,
                        monthYear = monthYear.text,
                        billAmount = billAmount.text.toDoubleOrNull() ?: 0.0,
                        billImageUri = billImageUri.toString()
                    )
                    viewModel.submitBill(mobileBill)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }

            if (showImageSourceDialog) {
                AlertDialog(
                    onDismissRequest = { showImageSourceDialog = false },
                    title = { Text("Select Image Source") },
                    text = { Text("Choose an option to upload the image.") },
                    confirmButton = {
                        TextButton(onClick = {
                            if (hasCameraPermission) {
                                imageCaptureLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                            showImageSourceDialog = false
                        }) {
                            Text("Take Photo")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            imagePickerLauncher.launch("image/*")
                            showImageSourceDialog = false
                        }) {
                            Text("Choose from Gallery")
                        }
                    }
                )
            }

        }
    }


}

//fun fillFormWithExtractedText(text: String) {
//    employeeName = TextFieldValue(extractEmployeeName(text))
//    monthYear = TextFieldValue(extractMonthYear(text))
//    billAmount = TextFieldValue(extractBillAmount(text))
//}

private fun extractEmployeeName(text: String): String {
    val nameRegex = Regex("Employee\\s*Name:\\s*([A-Za-z ]+)")
    return nameRegex.find(text)?.groupValues?.get(1) ?: "Unknown"
}

private fun extractMonthYear(text: String): String {
    val dateRegex = Regex("(\\d{2}/\\d{4})")
    return dateRegex.find(text)?.value ?: "MM/YYYY"
}

private fun extractBillAmount(text: String): String {
    val amountRegex = Regex("Amount\\s*:\\s*(\\d+\\.?\\d*)")
    return amountRegex.find(text)?.groupValues?.get(1) ?: "0.0"
}

private fun processImageFromUri(mContext: Context, uri: Uri, onSuccess: (String) -> Unit) {
        val inputImage = InputImage.fromFilePath(mContext, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                onSuccess(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e("BillEntryScreen", "Failed to recognize text: $e")
            }
    }
/**
 * Saves a Bitmap image to the cache directory and returns its URI.
 */
private fun saveImageToCache(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
