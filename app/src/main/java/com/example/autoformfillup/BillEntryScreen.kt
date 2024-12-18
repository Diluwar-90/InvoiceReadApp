package com.example.autoformfillup

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEntryScreen(viewModel: BillEntryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {

    val scrollState = rememberScrollState()

    var billEntryDate by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var monthYear by remember { mutableStateOf("") }
    var eligibleBillAmount by remember { mutableStateOf("") }
    var billImageUri by remember { mutableStateOf<Uri?>(null) }
    var hasCameraPermission by rememberSaveable { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("-- Select Month --") }
    //val options = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val options = getPreviousThreeMonths()
    var billDate by remember { mutableStateOf("") }
    var receiptNum by remember { mutableStateOf("") }
    var billPeriodStartDate by remember { mutableStateOf("") }
    var billPeriodEndDate by remember { mutableStateOf("") }

    //var showDatePicker by remember { mutableStateOf(false) }
    var billEntryDatePickerState by remember { mutableStateOf(false) }
    var billDatePickerState by remember { mutableStateOf(false) }
    var billStartDatePickerState by remember { mutableStateOf(false) }
    var billEndDatePickerState by remember { mutableStateOf(false) }


    // Logic to enforce max eligible amount
    fun handleBillAmountChange(value: String) {
        billAmount = value

        // Convert input to number safely
        val enteredAmount = value.toIntOrNull() ?: 0
        val maxEligibleAmount = 750

        eligibleBillAmount = when {
            enteredAmount >= maxEligibleAmount -> maxEligibleAmount.toString()
            else -> enteredAmount.toString()
        }
    }

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

                val billingDate = extractBillingDate(extractedText)
                val result = extractMonthAndYear(billingDate)

                if (result != null) {
                    println("Month: ${result.first}") // Output will be "Sep"
                    println("Year: ${result.second}") // Output will be "2024"
                    monthYear = "${result.first}-${result.second}"
                    selectedOption = monthYear
                } else {
                    println("No match found.")
                }

                Log.d("BillEntryScreen", "Extracted Text: $extractedText")
                //fillFormWithExtractedText(extractedText)
                billEntryDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                //billDate = ""
                receiptNum = TextFieldValue(extractReceiptNumber(extractedText)).text
                billDate = convertToISODate(billingDate).toString()
                billPeriodStartDate = convertToISODate(billingDate).toString()
                billAmount = extractBillAmount(extractedText)?.let { TextFieldValue(it).text }.toString()
                handleBillAmountChange(billAmount)
            }
            Log.d("BillEntryScreen", "Extracted Text: $uri")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    text = "Mobile Bill Reimbursement",
                    color = MaterialTheme.colorScheme.onPrimary
                ) },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
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
                    label = { Text("Reimbursement Month")},
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
                onValueChange = { handleBillAmountChange(it) },
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
                enabled = false,
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
                        employeeName = billAmount,
                        monthYear = monthYear,
                        billAmount = billAmount.toDoubleOrNull() ?: 0.0,
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

private fun extractReceiptNumber(text: String): String {
    val receiptNumberRegex = Regex("""\d{18,}""")
    return receiptNumberRegex.find(text)?.value ?: ""
}

private fun extractBillingDate(text: String): String {
    val dateRegex = Regex("""\d{1,2}\s\w{3}\s\d{4}""")
    return dateRegex.find(text)?.value ?: ""
}

private fun extractBillAmount(text: String): String? {
    // Regex to handle multi-line 'paid from T838' scenarios
    val regex = Regex("""paid from\s+T?(\d{1,6})""", RegexOption.IGNORE_CASE)
    val matchResult = regex.find(text)

    return matchResult?.groupValues?.get(1) // Extract only the number after "paid from"
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

@RequiresApi(Build.VERSION_CODES.O)
fun getPreviousThreeMonths(): List<String> {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MMM-yyyy", Locale.US)
    return (0..2).map {
        today.minusMonths(it.toLong()).format(formatter)
    }.reversed() // Reverse to show chronological order (earliest -> current)
}

fun convertToISODate(input: String): String? {
    return try {
        // Parse the input date string
        val inputFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val parsedDate = inputFormat.parse(input)

        // Reformat the date into the desired format
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        outputFormat.format(parsedDate!!)
    } catch (e: Exception) {
        null // Return null if parsing fails
    }
}

fun extractMonthAndYear(text: String): Pair<String, String>? {
    // Regex pattern to match dates like "22 Sep 2024"
    val regex = Regex("""\b(\d{1,2})\s([A-Z][a-z]+)\s(\d{4})\b""")
    val matchResult = regex.find(text)

    return if (matchResult != null) {
        val month = matchResult.groupValues[2] // Extract month name e.g. "Sep"
        val year = matchResult.groupValues[3] // Extract year e.g. "2024"
        Pair(month, year)
    } else {
        null
    }
}
