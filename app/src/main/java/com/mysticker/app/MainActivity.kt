package com.mysticker.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var ivPreview: ImageView
    private lateinit var btnTelegram: Button
    private lateinit var btnZalo: Button
    private lateinit var btnMessenger: Button
    private var stickerFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivPreview = findViewById(R.id.ivPreview)
        btnTelegram = findViewById(R.id.btnTelegram)
        btnZalo = findViewById(R.id.btnZalo)
        btnMessenger = findViewById(R.id.btnMessenger)

        handleIncomingIntent(intent)

        findViewById<Button>(R.id.btnSelect).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 100)
        }

        btnTelegram.setOnClickListener { sendToTelegram() }
        btnZalo.setOnClickListener { sendToZalo() }
        btnMessenger.setOnClickListener { sendToMessenger() }
    }

    private fun handleIncomingIntent(intent: Intent) {
        if (Intent.ACTION_SEND == intent.action && intent.type?.startsWith("image/") == true) {
            val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
            uri?.let { loadAndPrepareSticker(it) }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { loadAndPrepareSticker(it) }
        }
    }

    private fun loadAndPrepareSticker(uri: android.net.Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return
        val prepared = prepareStickerBitmap(bitmap)
        ivPreview.setImageBitmap(prepared)

        val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "sticker_${System.currentTimeMillis()}.png"

        stickerFile = File(cacheDir, fileName)
        FileOutputStream(stickerFile).use {
            prepared.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        btnTelegram.isEnabled = true
        btnZalo.isEnabled = true
        btnMessenger.isEnabled = true
        Toast.makeText(this, "Sticker đã sẵn sàng!", Toast.LENGTH_SHORT).show()
    }

    private fun prepareStickerBitmap(bitmap: Bitmap): Bitmap {
        val size = 512
        return Bitmap.createScaledBitmap(bitmap, size, size, true)
    }

    private fun sendToTelegram() {
        val file = stickerFile ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            setPackage("org.telegram.messenger")
            putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file))
            putExtra(Intent.EXTRA_TEXT, "Gửi cho @Stickers → /newpack để tạo gói")
        }
        startActivity(Intent.createChooser(intent, "Gửi đến Telegram"))
    }

    private fun sendToZalo() {
        val file = stickerFile ?: return
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file))
        }
        startActivity(Intent.createChooser(share, "Lưu → Mở Zalo → Sticker của tôi → +"))
        Toast.makeText(this, "Zalo: Sticker → Sticker của tôi → dấu +", Toast.LENGTH_LONG).show()
    }

    private fun sendToMessenger() {
        val file = stickerFile ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            setPackage("com.facebook.orca")
            putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file))
        }
        startActivity(Intent.createChooser(intent, "Lưu → Mở Messenger → Tạo sticker"))
        Toast.makeText(this, "Messenger: 😊 → Sticker → + → Tạo", Toast.LENGTH_LONG).show()
    }
}
