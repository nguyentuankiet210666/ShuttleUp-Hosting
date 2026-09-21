package com.hosting.badminton

import android.net.Uri
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.widget.Toast
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var pendingCallback: ValueCallback<Array<Uri>>? = null
    private var pendingUri: Uri? = null
    @Volatile private var stagedPhoto: Uri? = null
    @Volatile private var stagedFromGallery = false
    private var publishedPhoto: Uri? = null

    inner class ReceiptBridge {
        @JavascriptInterface
        @Synchronized
        fun saveConfirmedPhoto(): Boolean {
            val uri = stagedPhoto ?: return false
            if (stagedFromGallery) return true // Existing gallery image: do not duplicate it.
            if (publishedPhoto == uri) return true
            return try {
                saveToGallery(uri)
                publishedPhoto = uri
                true
            } catch (e: Exception) { false }
        }
    }
    private val choosePhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val callback = pendingCallback
        pendingCallback = null
        val valid = uri != null && runCatching {
            contentResolver.getType(uri)?.startsWith("image/") == true
        }.getOrDefault(false)
        if (valid) {
            stagedPhoto = uri
            stagedFromGallery = true
            callback?.onReceiveValue(arrayOf(uri!!))
        } else callback?.onReceiveValue(null)
    }
    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
        else {
            pendingCallback?.onReceiveValue(null)
            pendingCallback = null
            Toast.makeText(this, "Cần quyền lưu trữ để lưu ảnh vào Thư viện trên Android 9 trở xuống.", Toast.LENGTH_LONG).show()
        }
    }
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingUri
        val callback = pendingCallback
        pendingCallback = null
        pendingUri = null
        if (success && uri != null) {
            stagedPhoto = uri
            stagedFromGallery = false
            callback?.onReceiveValue(arrayOf(uri))
        } else callback?.onReceiveValue(null)
    }

    private fun launchCamera() {
        try {
            val dir = File(cacheDir, "receipt-camera").apply { mkdirs() }
            val file = File.createTempFile("receipt-", ".jpg", dir)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            pendingUri = uri
            takePhoto.launch(uri)
        } catch (e: Exception) {
            pendingCallback?.onReceiveValue(null)
            pendingCallback = null
            pendingUri = null
            Toast.makeText(this, "Không mở được camera. Kiểm tra cấu hình FileProvider và ứng dụng Camera.", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveToGallery(source: Uri) {
        val name = "Hosting_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Hosting")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val target = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Cannot create gallery image")
            try {
                val input = contentResolver.openInputStream(source) ?: error("Cannot read photo")
                input.use { stream ->
                    val output = contentResolver.openOutputStream(target) ?: error("Cannot write photo")
                    output.use { stream.copyTo(it) }
                }
                val published = contentResolver.update(target, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null)
                check(published > 0)
            } catch (e: Exception) {
                contentResolver.delete(target, null, null)
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Hosting")
            check(dir.isDirectory || dir.mkdirs())
            val target = File(dir, name)
            try {
                val input = contentResolver.openInputStream(source) ?: error("Cannot read photo")
                input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
                MediaScannerConnection.scanFile(this, arrayOf(target.absolutePath), arrayOf("image/jpeg"), null)
            } catch (e: Exception) { target.delete(); throw e }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The HTML keeps a compressed copy after confirmation. Remove old temporary captures.
        val photoDir = File(cacheDir, "receipt-camera").apply { mkdirs() }
        photoDir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000L }
            ?.forEach { it.delete() }
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowContentAccess = true
            addJavascriptInterface(ReceiptBridge(), "HostingReceipt")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return request?.url?.toString() != "file:///android_asset/index.html"
                }
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return url != "file:///android_asset/index.html"
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView?, callback: ValueCallback<Array<Uri>>?,
                    params: FileChooserParams?
                ): Boolean {
                    if (view?.url != "file:///android_asset/index.html") return false
                    if (pendingCallback != null) { callback?.onReceiveValue(null); return true }
                    pendingCallback = callback
                    if (params?.isCaptureEnabled != true) {
                        try { choosePhoto.launch("image/*") }
                        catch (e: Exception) {
                            pendingCallback?.onReceiveValue(null)
                            pendingCallback = null
                            Toast.makeText(this@MainActivity, "Không mở được Thư viện ảnh.", Toast.LENGTH_LONG).show()
                        }
                        return true
                    }
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else launchCamera()
                    return true
                }
            }
        }
        // Inset the WebView's actual bounds: WebView padding alone does not
        // reliably keep fixed HTML elements clear of Android system bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val container = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(18, 60, 98))
            addView(webView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }
        setContentView(container)
        WindowCompat.getInsetsController(window, container).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(container)
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onDestroy() {
        pendingCallback?.onReceiveValue(null)
        pendingCallback = null
        webView.destroy()
        super.onDestroy()
    }
}
