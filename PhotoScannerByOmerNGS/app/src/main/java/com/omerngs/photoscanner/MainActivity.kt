package com.omerngs.photoscanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val sharedPrefs = context.getSharedPreferences("PhotoScannerPrefs", Context.MODE_PRIVATE)

            // Tema ve Klasör Yolunun Hafızada Saklanması (Kalıcılık)
            var isDarkTheme by remember { 
                mutableStateOf(sharedPrefs.getBoolean("isDarkTheme", false)) 
            }
            var selectedFolderPath by remember { 
                mutableStateOf(sharedPrefs.getString("folderPath", "Seçilmedi") ?: "Seçilmedi") 
            }

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScannerScreen(
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { 
                            isDarkTheme = it
                            sharedPrefs.edit().putBoolean("isDarkTheme", it).apply()
                        },
                        selectedFolderPath = selectedFolderPath,
                        onFolderSelected = { path ->
                            selectedFolderPath = path
                            sharedPrefs.edit().putString("folderPath", path).apply()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    selectedFolderPath: String,
    onFolderSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var barcodeText by remember { mutableStateOf("") }
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusMessage by remember { mutableStateOf("Klasör seçin ve barkod girin.") }

    // 1. Klasör Seçme Launcher
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = it.path ?: "Seçilen Klasör"
            onFolderSelected(path)
        }
    }

    // 2. Otomatik Kamera İzni İsteme
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        statusMessage = if (isGranted) "Kamera izni verildi." else "Kamera izni reddedildi!"
    }

    // Barkod işleme ve QR Kod üretme Fonksiyonu
    val processAndFindPhoto = {
        if (barcodeText.isNotBlank()) {
            try {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(barcodeText, BarcodeFormat.QR_CODE, 400, 400)
                displayedBitmap = bitmap
                statusMessage = "Barkod için QR Kod oluşturuldu: $barcodeText"
            } catch (e: Exception) {
                statusMessage = "Hata: ${e.message}"
            }
        } else {
            statusMessage = "Lütfen geçerli bir barkod girin."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Üst Başlık ve Tema Değiştirici
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Photo Scanner",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isDarkTheme) "Karanlık" else "Aydınlık",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onThemeToggle
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Klasör Seçme Alanı (Kalıcı)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Klasör: $selectedFolderPath",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { folderLauncher.launch(null) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Folder, contentDescription = "Klasör Seç")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Klasör Seç")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Barkod Giriş ve Simgeli Butonlar (Enter Desteği ile)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = barcodeText,
                onValueChange = { barcodeText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Barkod girin veya okutun") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { processAndFindPhoto() }
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Kamera İzin Butonu
            IconButton(
                onClick = {
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        statusMessage = "Kamera izni zaten verilmiş."
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Kamera İzni",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // QR Kod Oluşturma Butonu
            IconButton(
                onClick = { processAndFindPhoto() },
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "QR / Fotoğraf Getir",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sonuç ve QR Gösterme Alanı (Gereksiz siyah kutu kaldırıldı)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (displayedBitmap != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = displayedBitmap!!.asImageBitmap(),
                            contentDescription = "Oluşturulan QR Kod",
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = statusMessage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = statusMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
