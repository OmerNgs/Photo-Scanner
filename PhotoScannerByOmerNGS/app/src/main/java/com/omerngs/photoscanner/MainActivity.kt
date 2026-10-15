package com.omerngs.photoscanner

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var db: BarcodeDb
    private lateinit var search: EditText
    private lateinit var status: TextView
    private lateinit var image: ImageView
    private lateinit var barcodeLabel: TextView
    private lateinit var qrButton: Button
    private val worker = Executors.newSingleThreadExecutor()
    private val PICK_FOLDER = 4001
    private var currentBarcode = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = BarcodeDb(this)
        buildUi()
        status.text = "Önce fotoğraf klasörünü seçip indeks oluşturun."
        val saved = getSharedPreferences("prefs", MODE_PRIVATE).getString("tree", null)
        if (saved != null && db.count() > 0) status.text = "Hazır: ${db.count()} fotoğraf indekslendi."
    }

    private fun buildUi() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 16)
            setBackgroundColor(Color.WHITE)
        }
        val title = TextView(this).apply {
            text = "Photo Scanner by OmerNGS"
            textSize = 22f; setTextColor(Color.rgb(25, 65, 110)); gravity = Gravity.CENTER
        }
        outer.addView(title, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        search = EditText(this).apply {
            hint = "Barkod numarasını yaz"
            textSize = 18f
            singleLine = true
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        outer.addView(search, lp(-1, -2))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        row.addView(button("Fotoğraf bul") { lookup(search.text.toString()) }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(button("Barkod okut") { scanBarcode() }, LinearLayout.LayoutParams(0, -2, 1f))
        outer.addView(row)
        outer.addView(button("Fotoğraf klasörünü seç / yeniden tara") { chooseFolder() }, lp(-1, -2))
        status = TextView(this).apply { textSize = 14f; setTextColor(Color.DKGRAY); setPadding(0, 12, 0, 8) }
        outer.addView(status)
        image = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER }
        outer.addView(image, LinearLayout.LayoutParams(-1, 0, 1f))
        barcodeLabel = TextView(this).apply {
            textSize = 18f; gravity = Gravity.CENTER; setTextColor(Color.BLACK); setPadding(0, 8, 0, 8)
        }
        outer.addView(barcodeLabel, lp(-1, -2))
        qrButton = button("Bu barkodun QR kodunu göster") { showQr(currentBarcode) }
        qrButton.isEnabled = false
        outer.addView(qrButton, lp(-1, -2))
        setContentView(outer)
    }

    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(w, h)
    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun chooseFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, PICK_FOLDER)
    }

    @Deprecated("Deprecated in Android API but supported")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(uri, flags)
            getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("tree", uri.toString()).apply()
            indexFolder(uri)
        } else if (requestCode == IntentIntegrator.REQUEST_CODE) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result?.contents != null) {
                val value = result.contents.trim()
                search.setText(value)
                lookup(value)
            }
        }
    }

    private fun indexFolder(treeUri: Uri) {
        status.text = "Klasör taranıyor… 50.000 fotoğraf ilk seferde biraz sürebilir."
        image.setImageDrawable(null)
        worker.execute {
            val root = DocumentFile.fromTreeUri(this, treeUri)
            var count = 0
            var errors = 0
            if (root != null) {
                db.beginIndexing()
                try {
                db.clear()
                val stack = ArrayDeque<DocumentFile>()
                stack.add(root)
                while (stack.isNotEmpty()) {
                    val dir = stack.removeLast()
                    val children = try { dir.listFiles() } catch (_: Exception) { errors++; emptyArray() }
                    for (f in children) {
                        if (f.isDirectory) stack.add(f)
                        else if (f.isFile && isImage(f.name ?: "")) {
                            val name = f.name ?: continue
                            val barcode = name.substringBeforeLast('.', name).trim()
                            if (barcode.isNotEmpty()) {
                                db.insert(barcode, f.uri.toString(), name)
                                count++
                            }
                        }
                    }
                    if (count % 500 == 0) runOnUiThread { status.text = "İndeksleniyor… $count fotoğraf" }
                }
                } finally {
                    db.finishIndexing()
                }
            }
            runOnUiThread { status.text = "Tamamlandı: $count fotoğraf indekslendi." + if (errors > 0) " ($errors klasör okunamadı)" else "" }
        }
    }

    private fun isImage(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif")
    }

    private fun lookup(raw: String) {
        val code = raw.trim()
        if (code.isEmpty()) { status.text = "Bir barkod numarası yazın."; return }
        worker.execute {
            val matches = db.find(code)
            runOnUiThread {
                if (matches.isEmpty()) {
                    currentBarcode = code
                    image.setImageDrawable(null)
                    barcodeLabel.text = code
                    qrButton.isEnabled = true
                    status.text = "Bu barkoda ait fotoğraf bulunamadı. Dosya adı barkodla aynı olmalı."
                } else if (matches.size == 1) {
                    showImage(code, matches[0].uri, matches[0].name)
                } else {
                    val names = matches.map { it.name }.toTypedArray()
                    AlertDialog.Builder(this).setTitle("Birden fazla eşleşme")
                        .setItems(names) { _, which -> showImage(code, matches[which].uri, matches[which].name) }
                        .setNegativeButton("İptal", null).show()
                }
            }
        }
    }

    private fun showImage(code: String, uriString: String, filename: String) {
        currentBarcode = code
        barcodeLabel.text = "Barkod: $code\nDosya: $filename"
        qrButton.isEnabled = true
        try {
            image.setImageURI(Uri.parse(uriString))
            status.text = "Ürün fotoğrafı bulundu."
        } catch (_: Exception) {
            status.text = "Fotoğraf açılamadı. Klasör iznini yeniden vermeyi deneyin."
        }
    }

    private fun scanBarcode() {
        IntentIntegrator(this).apply {
            setPrompt("Ürün barkodunu kameraya tutun")
            setBeepEnabled(false)
            setOrientationLocked(false)
            setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            initiateScan()
        }
    }

    private fun showQr(code: String) {
        if (code.isBlank()) return
        try {
            val size = 720
            val matrix = MultiFormatWriter().encode(code, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
            val view = ImageView(this).apply { setImageBitmap(bmp); adjustViewBounds = true; setPadding(12, 12, 12, 12) }
            AlertDialog.Builder(this).setTitle("QR Kod: $code").setView(view)
                .setPositiveButton("Kapat", null).show()
        } catch (e: Exception) {
            Toast.makeText(this, "QR kod oluşturulamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        worker.shutdown()
        db.close()
    }
}
