package com.aunt13psychot1c.shredder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONObject

/**
 * Chaos Shredder is the web page in assets/www running inside a WebView. Everything happens
 * on the phone, no internet needed. This class only fills the gaps a web page can't cover
 * inside an app: saving files, picking .txt files, copying, opening links in the browser,
 * and catching text that was highlighted or shared from another app.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private var pageReady = false
    private var pendingText: String? = null
    private var pendingSave: String? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.setBackgroundColor(0xFF000000.toInt())
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true // remembers Grey / Rainbow Bright and light / dark
        web.settings.allowFileAccess = true
        web.addJavascriptInterface(Bridge(), "Android")
        web.webViewClient = object : WebViewClient() {
            // Links (GitHub, pizza) open in the real browser, not inside the app.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.scheme == "file") return false
                openOutside(url)
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                pageReady = true
                pendingText?.let { deliver(it) }
                pendingText = null
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            // "Upload .txt" opens the phone's file picker.
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val pick = Intent(Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("text/*")
                return try {
                    startActivityForResult(pick, PICK_FILE)
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    false
                }
            }
        }
        setContentView(web)
        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("file:///android_asset/www/index.html")
        takeText(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeText(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    @Deprecated("Back goes back inside the page first")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    /** Text highlighted ("Chaos Shredder" in the menu) or shared from another app. */
    private fun takeText(intent: Intent?) {
        val text = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        } ?: return
        if (pageReady) deliver(text) else pendingText = text
    }

    private fun deliver(text: String) {
        // JSONObject.quote makes any text safe to hand to JavaScript.
        web.evaluateJavascript("window.receiveText && window.receiveText(${JSONObject.quote(text)})", null)
    }

    private fun openOutside(url: Uri) {
        try { startActivity(Intent(Intent.ACTION_VIEW, url)) } catch (_: Exception) {}
    }

    @Deprecated("Simple and fine for two pickers")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_FILE -> {
                val uri = if (resultCode == RESULT_OK) data?.data else null
                fileCallback?.onReceiveValue(uri?.let { arrayOf(it) })
                fileCallback = null
            }
            SAVE_FILE -> {
                val uri = if (resultCode == RESULT_OK) data?.data else null
                val text = pendingSave
                pendingSave = null
                if (uri != null && text != null) {
                    try {
                        contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                        toast("Saved ✨")
                    } catch (_: Exception) {
                        toast("Couldn't save that file")
                    }
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    /** What the page can ask the app to do, as window.Android.* in app.js. */
    inner class Bridge {
        @JavascriptInterface
        fun copy(text: String) {
            runOnUiThread {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Chaos Shredder", text))
            }
        }

        /** "Download Text": the phone asks where to save chaos-shredder.txt. */
        @JavascriptInterface
        fun download(text: String) {
            runOnUiThread {
                pendingSave = text
                val save = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TITLE, "chaos-shredder.txt")
                try { startActivityForResult(save, SAVE_FILE) } catch (_: Exception) { toast("No file app to save with") }
            }
        }

        @JavascriptInterface
        fun openUrl(url: String) {
            runOnUiThread { openOutside(Uri.parse(url)) }
        }

        @JavascriptInterface
        fun version(): String = BuildConfig.VERSION_NAME

        /** Paints the phone's status and navigation bars to match the page. */
        @JavascriptInterface
        fun barColor(color: String, light: Boolean) {
            runOnUiThread {
                try {
                    val c = android.graphics.Color.parseColor(color)
                    window.statusBarColor = c
                    window.navigationBarColor = c
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = if (light && android.os.Build.VERSION.SDK_INT >= 26)
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
                    web.setBackgroundColor(c)
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        private const val PICK_FILE = 1
        private const val SAVE_FILE = 2
    }
}
