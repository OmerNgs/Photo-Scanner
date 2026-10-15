package com.omerngs.photoscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Karanlık/Aydınlık Tema State yönetimi
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
    var loadedImageUrl by remember { mutableStateOf<String?>(null) }

    // Otomatik fotoğraf getirme fonksiyonu (Enter'a basıldığında veya barkod okutulduğunda tetiklenir)
    val fetchPhoto = {
        if (barcodeText.isNotBlank()) {
            // Örnek görsel getirme mantığı (Burayı kendi API / URL yapınıza göre bağlayabilirsiniz)
            loadedImageUrl = "https://via.placeholder.com/300?text=Barkod:+${barcodeText}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Üst Başlık ve Tema Değiştirme Butonu
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

        Spacer(modifier = Modifier.height(16.dp))

        // Barkod Giriş Alanı ve Yanındaki Barkod / Kamera Simgeli Görsel Buton
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = barcodeText,
                onValueChange = { newValue ->
                    barcodeText = newValue
                    // İsteğe bağlı: Barkod her değiştiğinde otomatik tetiklemek isterseniz buraya ekleyebilirsiniz
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Barkod girin veya okutun") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // Klavye üzerinden Enter / Done tuşuna basıldığında otomatik fotoğraf getirir
                        fetchPhoto()
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Barkod / Kamera Simgeli Buton (4. Madde)
            IconButton(
                onClick = {
                    // Butona basıldığında da manuel arama yapabilir veya kamera taramayı tetikleyebilirsiniz
                    fetchPhoto()
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Barkod Okut",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dikey Uyumlu, Küçük Barkod Tarayıcı Alanı ve İstediğin Altıgen / 4 Çizgili Kılavuz Tasarımı (3. Madde)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Kamera Önizleme Alanı Simülasyonu
            Text(
                text = "Kamera / Barkod Alanı (Dikey)",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )

            // İstediğin Altıgen Şekilli / 4 Çizgili Tarama Çerçevesi Kılavuzu
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(100.dp)
                    .border(2.dp, Color.Green, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // İçerideki 4 çizgili/köşeli simgesel yapı
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fotoğrafın Otomatik Gösterileceği Alan
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
                if (loadedImageUrl != null) {
                    Text(
                        text = "Fotoğraf Yüklendi!\nBarkod: $barcodeText",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text = "Barkod okutulduğunda veya Enter tuşuna basıldığında fotoğraf otomatik gelecektir.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
