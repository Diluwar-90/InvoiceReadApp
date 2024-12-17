package com.example.autoformfillup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEntryScreen(viewModel: BillEntryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {

    var employeeName by remember { mutableStateOf(TextFieldValue("")) }
    var monthYear by remember { mutableStateOf(TextFieldValue("")) }
    var billAmount by remember { mutableStateOf(TextFieldValue("")) }
    var billImageUri by remember { mutableStateOf<Uri?>(null) }
    var hasCameraPermission by rememberSaveable { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

/*    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("BillEntryScreen", "Image URI: $uri")
            billImageUri = uri
        }
    }*/

    /*val context = LocalContext.current
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }

    // Check if Camera permission is granted
    permissionGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // Handle permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    // Launch camera to capture photo and get result
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            capturedImageUri = saveImageToCache(context, bitmap)
        }
    }
*/

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
            /*processImageFromUri(uri) { extractedText ->
                Log.d("BillEntryScreen", "Extracted Text: $extractedText")
                fillFormFromExtractedText(extractedText)
            }*/
            processImageFromUri(context,uri){ extractedText ->
                Log.d("BillEntryScreen", "Extracted Text: $extractedText")
                //fillFormWithExtractedText(extractedText)
                employeeName = TextFieldValue(extractEmployeeName(extractedText))
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
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF6200EA))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Employee Name
            OutlinedTextField(
                value = employeeName,
                onValueChange = { employeeName = it },
                label = { Text("Employee Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Month and Year
            OutlinedTextField(
                value = monthYear,
                onValueChange = { monthYear = it },
                label = { Text("Month & Year (MM/YYYY)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bill Amount
            OutlinedTextField(
                value = billAmount,
                onValueChange = { billAmount = it },
                label = { Text("Bill Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Image Picker
//            Button(
//                //onClick = { imagePickerLauncher.launch("image/*") },
//                onClick = { imagePickerLauncher.launch("image/*") },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Upload Bill Image")
//            }

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
                        employeeName = employeeName.text,
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


       /*     // Button to request permission
            if (!permissionGranted) {
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Request Camera Permission")
                }
            } else {
                // Button to open the camera
                Button(onClick = {
                    cameraLauncher.launch(null)
                }) {
                    Text("Take Photo")
                }
            }*/

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
