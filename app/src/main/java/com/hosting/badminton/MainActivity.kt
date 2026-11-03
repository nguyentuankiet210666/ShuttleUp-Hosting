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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import org.json.JSONObject
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseDb: FirebaseFirestore? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var sessionRegistration: ListenerRegistration? = null
    private var connectedSessionDate: String? = null
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

    inner class AuthBridge {
        @JavascriptInterface
        fun requestState() = sendAuthState()

        @JavascriptInterface
        fun signUp(name: String, email: String, password: String) {
            val auth = requireAuth() ?: return
            val cleanName = name.trim()
            val cleanEmail = email.trim()
            if (cleanName.isBlank() || cleanEmail.isBlank() || password.length < 6) {
                sendAuthResult(false, "Vui lòng nhập họ tên, email và mật khẩu từ 6 ký tự.")
                return
            }
            auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener { result ->
                    val user = result.user ?: run {
                        sendAuthResult(false, "Không tạo được tài khoản. Vui lòng thử lại.")
                        return@addOnSuccessListener
                    }
                    val profile = UserProfileChangeRequest.Builder().setDisplayName(cleanName).build()
                    user.updateProfile(profile).addOnCompleteListener {
                        user.sendEmailVerification().addOnCompleteListener {
                            sendAuthResult(
                                true,
                                "Đã tạo tài khoản. Hãy kiểm tra email để xác minh.",
                                currentUserJson()
                            )
                        }
                    }
                }
                .addOnFailureListener { sendAuthError(it) }
        }

        @JavascriptInterface
        fun signIn(email: String, password: String) {
            val auth = requireAuth() ?: return
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank() || password.isBlank()) {
                sendAuthResult(false, "Vui lòng nhập email và mật khẩu.")
                return
            }
            auth.signInWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener {
                    sendAuthResult(true, "Đăng nhập thành công.", currentUserJson())
                }
                .addOnFailureListener { sendAuthError(it) }
        }

        @JavascriptInterface
        fun signOut() {
            firebaseAuth?.signOut()
            sendAuthResult(true, "Đã đăng xuất.", JSONObject.NULL)
        }

        @JavascriptInterface
        fun sendVerification() {
            val user = firebaseAuth?.currentUser
            if (user == null) {
                sendAuthResult(false, "Bạn chưa đăng nhập.")
                return
            }
            user.sendEmailVerification()
                .addOnSuccessListener {
                    sendAuthResult(true, "Đã gửi lại email xác minh.", currentUserJson())
                }
                .addOnFailureListener { sendAuthError(it) }
        }
    }

    private fun requireAuth(): FirebaseAuth? {
        val auth = firebaseAuth
        if (auth == null) {
            sendAuthResult(
                false,
                "Firebase chưa được cấu hình. Hãy thêm app/google-services.json và bật Email/Mật khẩu."
            )
        }
        return auth
    }

    private fun currentUserJson(): Any {
        val user = firebaseAuth?.currentUser ?: return JSONObject.NULL
        return JSONObject().apply {
            put("uid", user.uid)
            put("name", user.displayName ?: "")
            put("email", user.email ?: "")
            put("verified", user.isEmailVerified)
        }
    }

    private fun sendAuthState() {
        sendAuthResult(true, "", currentUserJson())
    }

    private fun sendAuthError(error: Exception) {
        val message = when (error) {
            is FirebaseAuthWeakPasswordException -> "Mật khẩu cần ít nhất 6 ký tự."
            is FirebaseAuthUserCollisionException -> "Email này đã có tài khoản."
            is FirebaseAuthInvalidUserException -> "Không tìm thấy tài khoản này."
            is FirebaseAuthInvalidCredentialsException -> "Email hoặc mật khẩu chưa đúng."
            is FirebaseNetworkException -> "Không có kết nối mạng. Vui lòng thử lại."
            else -> "Không thực hiện được. Vui lòng thử lại sau."
        }
        sendAuthResult(false, message)
    }

    private fun sendAuthResult(ok: Boolean, message: String, user: Any? = null) {
        if (!::webView.isInitialized) return
        val payload = JSONObject().apply {
            put("ok", ok)
            put("message", message)
            if (user != null) put("user", user)
        }.toString()
        webView.post {
            webView.evaluateJavascript(
                "window.HostingAuth&&window.HostingAuth.handleResult(${JSONObject.quote(payload)})",
                null
            )
        }
    }

    inner class CloudBridge {
        @JavascriptInterface
        fun connect(date: String, payload: String) {
            val user = firebaseAuth?.currentUser
            val db = firebaseDb
            if (user == null || db == null) {
                sendCloudError("Chưa thể kết nối Firestore.")
                return
            }
            if (!validCloudPayload(date, payload)) return
            val groupId = user.uid
            val groupRef = db.collection("groups").document(groupId)
            groupRef.get()
                .addOnSuccessListener { group ->
                    val defaultName = "Nhóm của " + (user.displayName?.takeIf { it.isNotBlank() } ?: "Hosting")
                    val batch = db.batch()
                    batch.set(
                        db.collection("users").document(user.uid),
                        mapOf(
                            "displayName" to (user.displayName ?: ""),
                            "email" to (user.email ?: ""),
                            "defaultGroupId" to groupId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    if (!group.exists()) {
                        batch.set(
                            groupRef,
                            mapOf(
                                "name" to defaultName,
                                "area" to "",
                                "areaKey" to "",
                                "isPublic" to false,
                                "ownerUid" to user.uid,
                                "memberCount" to 1,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.set(
                        groupRef.collection("members").document(user.uid),
                        mapOf(
                            "uid" to user.uid,
                            "displayName" to (user.displayName ?: ""),
                            "role" to "owner",
                            "joinedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    batch.commit()
                        .addOnSuccessListener {
                            sendGroup(
                                group.getString("name") ?: defaultName,
                                group.getString("area") ?: "",
                                group.getBoolean("isPublic") ?: false
                            )
                            connectSession(db, groupId, date)
                        }
                        .addOnFailureListener { sendCloudFailure(it) }
                }
                .addOnFailureListener { sendCloudFailure(it) }
        }

        @JavascriptInterface
        fun saveSession(date: String, payload: String) {
            val user = firebaseAuth?.currentUser
            val db = firebaseDb
            if (user == null || db == null) {
                sendCloudError("Bạn cần đăng nhập để đồng bộ.")
                return
            }
            if (!validCloudPayload(date, payload)) return
            db.collection("groups").document(user.uid).collection("sessions").document(date)
                .set(
                    mapOf(
                        "date" to date,
                        "payload" to payload,
                        "creatorUid" to user.uid,
                        "updatedBy" to user.uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener { sendCloudStatus(true, "Đã đồng bộ") }
                .addOnFailureListener { sendCloudFailure(it) }
        }

        @JavascriptInterface
        fun deleteSession(date: String) {
            val user = firebaseAuth?.currentUser
            val db = firebaseDb
            if (user == null || db == null) {
                sendCloudError("Bạn cần đăng nhập để xóa buổi chơi.")
                return
            }
            if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date)) {
                sendCloudError("Ngày buổi chơi không hợp lệ.")
                return
            }
            if (connectedSessionDate == date) {
                sessionRegistration?.remove()
                sessionRegistration = null
                connectedSessionDate = null
            }
            db.collection("groups").document(user.uid).collection("sessions").document(date)
                .delete()
                .addOnSuccessListener { sendCloudStatus(true, "Đã xóa buổi chơi") }
                .addOnFailureListener { sendCloudFailure(it) }
        }

        @JavascriptInterface
        fun updateGroup(name: String, area: String, isPublic: Boolean) {
            val user = firebaseAuth?.currentUser
            val db = firebaseDb
            val cleanName = name.trim()
            val cleanArea = area.trim()
            if (user == null || db == null) {
                sendCloudError("Bạn cần đăng nhập để sửa nhóm.")
                return
            }
            if (cleanName.length !in 2..70 || cleanArea.length > 80 || (isPublic && cleanArea.length < 2)) {
                sendCloudError("Tên hoặc khu vực nhóm chưa hợp lệ.")
                return
            }
            db.collection("groups").document(user.uid)
                .set(
                    mapOf(
                        "name" to cleanName,
                        "area" to cleanArea,
                        "areaKey" to cleanArea.lowercase(),
                        "isPublic" to isPublic,
                        "ownerUid" to user.uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener {
                    sendGroup(cleanName, cleanArea, isPublic)
                    sendCloudStatus(true, "Đã lưu nhóm")
                }
                .addOnFailureListener { sendCloudFailure(it) }
        }

        @JavascriptInterface
        fun disconnect() {
            sessionRegistration?.remove()
            sessionRegistration = null
            connectedSessionDate = null
        }
    }

    private fun validCloudPayload(date: String, payload: String): Boolean {
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date) || payload.length > 750_000) {
            sendCloudError("Dữ liệu buổi chơi không hợp lệ hoặc quá lớn.")
            return false
        }
        return try {
            JSONObject(payload).getString("date") == date
        } catch (_: Exception) {
            sendCloudError("Không đọc được dữ liệu buổi chơi.")
            false
        }
    }

    private fun connectSession(db: FirebaseFirestore, groupId: String, date: String) {
        sessionRegistration?.remove()
        sessionRegistration = null
        connectedSessionDate = date
        val ref = db.collection("groups").document(groupId).collection("sessions").document(date)
        ref.get()
            .addOnSuccessListener { document ->
                if (connectedSessionDate != date) return@addOnSuccessListener
                if (document.exists()) {
                    document.getString("payload")?.let { sendCloudSession(date, it) }
                    if (!document.contains("creatorUid")) {
                        ref.set(
                            mapOf(
                                "creatorUid" to groupId,
                                "updatedBy" to groupId,
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                    }
                    attachSessionListener(ref, date)
                } else {
                    // Connecting is read-only. A missing cloud document must not cause
                    // cached data from another account to be uploaded automatically.
                    sendCloudStatus(true, "Chưa có dữ liệu trên cloud")
                    attachSessionListener(ref, date)
                }
            }
            .addOnFailureListener { sendCloudFailure(it) }
    }

    private fun attachSessionListener(
        ref: com.google.firebase.firestore.DocumentReference,
        date: String
    ) {
        sessionRegistration?.remove()
        sessionRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                sendCloudFailure(error)
                return@addSnapshotListener
            }
            if (connectedSessionDate == date && snapshot?.exists() == true) {
                snapshot.getString("payload")?.let { sendCloudSession(date, it) }
            }
        }
    }

    private fun sendGroup(name: String, area: String, isPublic: Boolean) {
        sendCloudResult(JSONObject().apply {
            put("type", "group")
            put("name", name)
            put("area", area)
            put("isPublic", isPublic)
        })
    }

    private fun sendCloudSession(date: String, payload: String) {
        sendCloudResult(JSONObject().apply {
            put("type", "session")
            put("date", date)
            put("payload", payload)
        })
    }

    private fun sendCloudStatus(ok: Boolean, message: String) {
        sendCloudResult(JSONObject().apply {
            put("type", "status")
            put("ok", ok)
            put("message", message)
        })
    }

    private fun sendCloudFailure(error: Exception) {
        val message = if (error is FirebaseNetworkException) {
            "Mất kết nối; dữ liệu vẫn được giữ trên máy."
        } else {
            "Chưa đồng bộ được. Hãy kiểm tra Firestore và thử lại."
        }
        sendCloudError(message)
    }

    private fun sendCloudError(message: String) {
        sendCloudResult(JSONObject().apply {
            put("type", "error")
            put("message", message)
        })
    }

    private fun sendCloudResult(payload: JSONObject) {
        if (!::webView.isInitialized) return
        webView.post {
            webView.evaluateJavascript(
                "window.HostingCloud&&window.HostingCloud.handleResult(${JSONObject.quote(payload.toString())})",
                null
            )
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
        firebaseAuth = if (FirebaseApp.getApps(this).isNotEmpty()) {
            runCatching { FirebaseAuth.getInstance() }.getOrNull()
        } else null
        firebaseDb = if (FirebaseApp.getApps(this).isNotEmpty()) {
            runCatching { FirebaseFirestore.getInstance() }.getOrNull()
        } else null
        // The HTML keeps a compressed copy after confirmation. Remove old temporary captures.
        val photoDir = File(cacheDir, "receipt-camera").apply { mkdirs() }
        photoDir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000L }
            ?.forEach { it.delete() }
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowContentAccess = true
            addJavascriptInterface(ReceiptBridge(), "HostingReceipt")
            addJavascriptInterface(AuthBridge(), "HostingAuthNative")
            addJavascriptInterface(CloudBridge(), "HostingCloudNative")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url == "file:///android_asset/index.html") sendAuthState()
                }
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
        firebaseAuth?.let {
            authStateListener = FirebaseAuth.AuthStateListener { auth ->
                if (auth.currentUser == null) {
                    sessionRegistration?.remove()
                    sessionRegistration = null
                    connectedSessionDate = null
                }
                sendAuthState()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val auth = firebaseAuth
        val listener = authStateListener
        if (auth != null && listener != null) auth.addAuthStateListener(listener)
    }

    override fun onStop() {
        val auth = firebaseAuth
        val listener = authStateListener
        if (auth != null && listener != null) auth.removeAuthStateListener(listener)
        super.onStop()
    }

    override fun onDestroy() {
        sessionRegistration?.remove()
        sessionRegistration = null
        pendingCallback?.onReceiveValue(null)
        pendingCallback = null
        webView.destroy()
        super.onDestroy()
    }
}