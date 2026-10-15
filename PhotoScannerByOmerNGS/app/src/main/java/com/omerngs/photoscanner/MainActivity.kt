package com.omerngs.photoscanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.documentfile.provider.DocumentFile
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val sharedPrefs = context.getSharedPreferences("PhotoScannerPrefs", Context.MODE_PRIVATE)

            var isDarkTheme by remember { 
                mutableStateOf(sharedPrefs.getBoolean("isDarkTheme", false)) 
            }
            var selectedFolderPath by remember { 
                mutableStateOf(sharedPrefs.getString("folderPath", "Seçilmedi") ?: "Seçilmedi") 
            }
            var folderUriString by remember { 
                mutableStateOf(sharedPrefs.getString("folderUri", "") ?: "") 
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
                        folderUriString = folderUriString,
                        onFolderSelected = { path, uriStr ->
                            selectedFolderPath = path
                            folderUriString = uriStr
                            sharedPrefs.edit().putString("folderPath", path).putString("folderUri", uriStr).apply()
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
    folderUriString: String,
    onFolderSelected: (String, String) -> Unit
) {
    val context = LocalContext.current
    var barcodeText by remember { mutableStateOf("") }
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusMessage by remember { mutableStateOf("Klasör seçin ve barkod girin.") }

    // 1. Klasör Seçme Launcher (Kalıcı okuma izni ile)
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val path = it.path ?: "Seçilen Klasör"
            onFolderSelected(path, it.toString())
            statusMessage = "Klasör seçildi: $path"
        }
    }

    // 2. ZXing Canlı Kamera Tarayıcı Launcher
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            barcodeText = result.contents
            statusMessage = "Barkod okundu: ${result.contents}"
            searchPhotoInFolder(context, folderUriString, barcodeText) { bitmap, msg ->
                displayedBitmap = bitmap
                statusMessage = msg
            }
        }
    }

    // Kamera İzni Kontrolü ve Tarayıcıyı Başlatma
    val startCameraScan = {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt("Barkodu kamera kılavuzuna ortalayın")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            setOrientationLocked(true)
        }
        scanLauncher.launch(options)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            startCameraScan()
        } else {
            statusMessage = "Kamera izni reddedildi!"
        }
    }

    // Barkod arama ve fotoğraf eşleme mantığı
    val processAndFindPhoto = {
        if (barcodeText.isNotBlank()) {
            searchPhotoInFolder(context, folderUriString, barcodeText) { bitmap, msg ->
                displayedBitmap = bitmap
                statusMessage = msg
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
                    onDone = { processAndFindPhoto() }
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Kamera Tarayıcı Açma Butonu
            IconButton(
                onClick = {
                    if (hasCameraPermission) {
                        startCameraScan()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Barkod Tarayıcı",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // QR Kod / Fotoğraf Getir Butonu
            IconButton(
                onClick = { processAndFindPhoto() },
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Fotoğraf Getir",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sonuç ve Görsel Gösterme Alanı
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
                            contentDescription = "Bulunan Fotoğraf",
                            modifier = Modifier
                                .fillMaxHeight(0.8f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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

// Seçilen klasör içerisinde barkod adıyla eşleşen görseli arayıp getiren fonksiyon
fun searchPhotoInFolder(context: Context, folderUriStr: String, barcode: String, onResult: (Bitmap?, String) -> Unit) {
    if (folderUriStr.isBlank()) {
        // Klasör seçilmediyse doğrudan barkodun QR kodunu göster
        try {
            val encoder = BarcodeEncoder()
            val bmp = encoder.encodeBitmap(barcode, BarcodeFormat.QR_CODE, 400, 400)
            onResult(bmp, "Klasör seçilmediği için QR kod üretildi: $barcode")
        } catch (e: Exception) {
            onResult(null, "Hata: ${e.message}")
        }
        return
    }

    try {
        val dirUri = Uri.parse(folderUriStr)
        val rootDoc = DocumentFile.fromTreeUri(context, dirUri)
        if (rootDoc == null || !rootDoc.isDirectory) {
            onResult(null, "Geçersiz klasör.")
            return
        }

        // Klasördeki dosyalar arasında barkod adıyla eşleşen görseli ara (örn: 86940573.jpg, 86940573.png)
        var foundFile: DocumentFile? = null
        for (file in rootDoc.listFiles()) {
            val fileName = file.name ?: continue
            val nameWithoutExt = fileName.substringBeforeLast(".")
            if (nameWithoutExt.equals(barcode, ignoreCase = true)) {
                foundFile = file
                break
            }
        }

        if (foundFile != null) {
            val inputStream = context.contentResolver.openInputStream(foundFile.uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                onResult(bitmap, "Fotoğraf başarıyla getirildi: ${foundFile.name}")
            } else {
                onResult(null, "Dosya okunamadı veya geçerli bir görsel değil.")
            }
        } else {
            // Fotoğraf bulunamazsa yedek olarak QR kod üretip gösterelim
            val encoder = BarcodeEncoder()
            val bmp = encoder.encodeBitmap(barcode, BarcodeFormat.QR_CODE, 400, 400)
            onResult(bmp, "Klasörde '$barcode' adında fotoğraf bulunamadı, QR kod üretildi.")
        }
    } catch (e: Exception) {
        onResult(null, "Arama hatası: ${e.message}")
    }
}
