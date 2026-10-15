package com.omerngs.photoscanner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScannerScreen(
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(isDarkTheme: Boolean, onThemeToggle: (Boolean) -> Unit) {
    var barcodeText by remember { mutableStateOf("") }
    var selectedFolderPath by remember { mutableStateOf("Seçilmedi") }
    var capturedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Klasör Seçme Launcher (Storage Access Framework)
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            selectedFolderPath = it.path ?: "Seçilen Klasör"
        }
    }

    // Kamera Açma Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                capturedImageBitmap = bitmap
            }
        }
    }

    // Otomatik Fotoğraf Getir / QR Oluştur Fonksiyonu
    val processBarcode = {
        if (barcodeText.isNotBlank()) {
            qrCodeBitmap = createSimpleQrPlaceholder(barcodeText)
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

        // Klasör Seçme Alanı
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

        // Barkod Giriş ve Simgeli Butonlar
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
                    onDone = { processBarcode() }
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Kamera Açma Butonu
            IconButton(
                onClick = {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(intent)
                },
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Kamera Aç",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // QR Kod Oluşturma Butonu
            IconButton(
                onClick = { processBarcode() },
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "QR Oluştur",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dikey Uyumlu Küçük Tarayıcı Alanı (Altıgen / 4 Çizgili Kılavuz)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Kamera / Barkod Alanı (Dikey)",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )

            // 4 Çizgili / Altıgen Tarama Kılavuzu
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(90.dp)
                    .border(2.dp, Color.Green, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sonuç ve Görsel Alanı
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (capturedImageBitmap != null) {
                    Image(
                        bitmap = capturedImageBitmap!!.asImageBitmap(),
                        contentDescription = "Çekilen Fotoğraf",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = if (barcodeText.isNotBlank()) "Barkod İşlendi: $barcodeText" else "Klasör seçin, barkod girin veya kamerayı açın.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

fun createSimpleQrPlaceholder(text: String): Bitmap {
    val width = 200
    val height = 200
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    bmp.eraseColor(android.graphics.Color.WHITE)
    return bmp
}
