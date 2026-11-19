package express.pickup

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import express.pickup.databinding.ActivityPageBinding

class PageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPageBinding
    private lateinit var pageUrl: String
    private lateinit var browserUserAgent: String

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL)
        val title = intent.getStringExtra(EXTRA_TITLE)
        if (url == null || !url.startsWith("https://") || title == null) {
            finish()
            return
        }
        pageUrl = url
        browserUserAgent = buildCleanBrowserUserAgent()

        binding = ActivityPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val topBarInitialLeft = binding.topBar.paddingLeft
        val topBarInitialTop = binding.topBar.paddingTop
        val topBarInitialRight = binding.topBar.paddingRight
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val imeAndBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime(),
            )
            binding.topBar.updatePadding(
                left = topBarInitialLeft + safe.left,
                top = topBarInitialTop + safe.top,
                right = topBarInitialRight + safe.right,
            )
            binding.root.updatePadding(bottom = imeAndBars.bottom)
            insets
        }

        binding.title.text = title
        binding.host.text = pageUrl.toUri().host.orEmpty()

        binding.back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.refresh.setOnClickListener {
            syncPinduoduoCookiesIfNeeded(binding.web.url ?: pageUrl)
            val shouldLoadInitial = binding.error.isVisible ||
                binding.web.url.isNullOrEmpty() ||
                binding.web.url == "about:blank"
            showError(false)
            if (shouldLoadInitial) {
                binding.web.loadUrl(pageUrl)
            } else {
                binding.web.reload()
            }
        }

        binding.retry.setOnClickListener {
            showError(false)
            syncPinduoduoCookiesIfNeeded(pageUrl)
            binding.web.loadUrl(pageUrl)
        }

        val web = binding.web
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        configureWebSettings(web.settings)
        syncPinduoduoCookiesIfNeeded(pageUrl)

        web.webViewClient = PageClient()
        web.webChromeClient = PageChrome()
        web.setDownloadListener { _, _, _, _, _ ->
            toast(R.string.stay_on_page)
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (web.canGoBack()) web.goBack() else finish()
                }
            },
        )

        if (!isOnline()) toast(R.string.offline)
        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState)
        } else {
            web.loadUrl(pageUrl)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebSettings(settings: WebSettings) {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            mediaPlaybackRequiresUserGesture = true
            allowFileAccess = false
            allowContentAccess = false
            userAgentString = browserUserAgent
        }
    }

    private fun buildCleanBrowserUserAgent(): String {
        val raw = runCatching { WebSettings.getDefaultUserAgent(this) }.getOrNull()
        if (raw.isNullOrBlank()) return FALLBACK_MOBILE_BROWSER
        return raw
            .replace("; wv)", ")")
            .replace("; wv;", ";")
            .replace("Version/4.0 ", "")
    }

    /**
     * Pinduoduo web login runs on `mobile.yangkeduo.com` (setting `PDDAccessToken` on `.yangkeduo.com`),
     * whereas the pickup-code page (`mdkd/package`) queries `https://api.pinduoduo.com/api/...`.
     * Mirroring the session cookies across Pinduoduo's two domains with `SameSite=None; Secure`
     * allows cross-origin XHR with credentials to carry the login state.
     */
    private fun syncPinduoduoCookiesIfNeeded(url: String?) {
        val host = url?.toUri()?.host?.lowercase() ?: return
        if (!host.endsWith("yangkeduo.com") && !host.endsWith("pinduoduo.com")) return

        val cm = CookieManager.getInstance()
        val ykdRaw = cm.getCookie("https://mobile.yangkeduo.com") ?: return
        val ykdMap = parseCookieMap(ykdRaw)
        if (!ykdMap.containsKey("PDDAccessToken")) return

        val pddMap = parseCookieMap(cm.getCookie("https://api.pinduoduo.com").orEmpty())
        var updated = false
        for ((name, value) in ykdMap) {
            if (value.isEmpty() || pddMap[name] == value) continue
            val httpOnly = if (name == "PDDAccessToken" || name == "jrpl") "; HttpOnly" else ""
            cm.setCookie(
                "https://api.pinduoduo.com",
                "$name=$value; Domain=.pinduoduo.com; Path=/; Secure; SameSite=None$httpOnly",
            )
            updated = true
        }
        if (updated) {
            cm.flush()
            debugLog("Synced yangkeduo.com session cookies to .pinduoduo.com")
        }
    }

    private fun parseCookieMap(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val map = LinkedHashMap<String, String>()
        for (part in raw.split(';')) {
            val trimmed = part.trim()
            val eq = trimmed.indexOf('=')
            if (eq <= 0) continue
            val name = trimmed.substring(0, eq).trim()
            val value = trimmed.substring(eq + 1).trim()
            if (name.isNotEmpty()) {
                map[name] = value
            }
        }
        return map
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::binding.isInitialized) binding.web.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) binding.web.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::binding.isInitialized) binding.web.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        CookieManager.getInstance().flush()
        if (::binding.isInitialized) {
            binding.web.apply {
                stopLoading()
                webViewClient = WebViewClient()
                webChromeClient = null
                (parent as? ViewGroup)?.removeView(this)
                destroy()
            }
        }
        super.onDestroy()
    }

    private fun showError(show: Boolean) {
        binding.error.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun isOnline(): Boolean {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return true
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun toast(message: Int) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    private fun openOutside(uri: Uri, hasGesture: Boolean): Boolean {
        CookieManager.getInstance().flush()
        val view = if (uri.scheme == "intent") {
            try {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
            } catch (_: Exception) {
                null
            }
        } else {
            Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
        }

        if (view != null) {
            try {
                startActivity(view)
                return true
            } catch (_: ActivityNotFoundException) {
                val intentFallback = view.getStringExtra("browser_fallback_url")
                if (intentFallback != null && intentFallback.startsWith("https://")) {
                    binding.web.loadUrl(intentFallback)
                    return true
                }
            }
        }
        if (hasGesture) {
            toast(R.string.stay_on_page)
        }
        return true
    }

    private inner class PageClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            val scheme = uri.scheme?.lowercase()
            debugLog("shouldOverrideUrlLoading gesture=${request.hasGesture()} uri=$uri")
            if (scheme == "https" || scheme == "http") {
                syncPinduoduoCookiesIfNeeded(uri.toString())
                return false
            }
            if (scheme == "about") return false
            if (scheme == "data" || scheme == "blob" || scheme == "javascript") return true
            return openOutside(uri, request.hasGesture())
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            showError(false)
            binding.progress.visibility = View.VISIBLE
            syncPinduoduoCookiesIfNeeded(url)
            url?.let {
                val host = it.toUri().host
                if (!host.isNullOrEmpty()) binding.host.text = host
            }
            if (view != null && url?.toUri()?.host?.endsWith("yangkeduo.com") == true) {
                view.evaluateJavascript(PDD_CREDENTIALS_BRIDGE_JS, null)
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            binding.progress.visibility = View.GONE
            syncPinduoduoCookiesIfNeeded(url)
            CookieManager.getInstance().flush()
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            debugLog("onReceivedError main=${request.isForMainFrame} code=${error.errorCode} url=${request.url}")
            if (request.isForMainFrame) {
                binding.progress.visibility = View.GONE
                showError(true)
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse,
        ) {
            debugLog("onReceivedHttpError main=${request.isForMainFrame} status=${errorResponse.statusCode} url=${request.url}")
        }
    }

    private inner class PageChrome : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            binding.progress.setProgressCompat(newProgress, true)
            binding.progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
        }

        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message,
        ): Boolean {
            val popup = WebView(view.context)
            CookieManager.getInstance().setAcceptThirdPartyCookies(popup, true)
            configureWebSettings(popup.settings)
            popup.settings.setSupportMultipleWindows(false)
            popup.webViewClient = object : WebViewClient() {
                private var handled = false

                private fun routeAndClose(popupView: WebView, targetUri: Uri): Boolean {
                    if (handled) return true
                    handled = true
                    val scheme = targetUri.scheme?.lowercase()
                    if (scheme == "https" || scheme == "http") {
                        syncPinduoduoCookiesIfNeeded(targetUri.toString())
                        view.loadUrl(targetUri.toString())
                    } else if (scheme != "about" && scheme != "javascript") {
                        openOutside(targetUri, isUserGesture)
                    }
                    popupView.post { popupView.destroy() }
                    return true
                }

                override fun shouldOverrideUrlLoading(popupView: WebView, request: WebResourceRequest): Boolean {
                    return routeAndClose(popupView, request.url)
                }

                override fun onPageStarted(popupView: WebView, url: String?, favicon: Bitmap?) {
                    if (!url.isNullOrBlank() && url != "about:blank") {
                        routeAndClose(popupView, url.toUri())
                    }
                }
            }
            val transport = resultMsg.obj as WebView.WebViewTransport
            transport.webView = popup
            resultMsg.sendToTarget()
            return true
        }
    }

    companion object {
        private const val TAG = "PickupWeb"
        const val EXTRA_TITLE = "title"
        const val EXTRA_URL = "url"
        const val FALLBACK_MOBILE_BROWSER =
            "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

        /**
         * Ensures cross-origin requests from `mobile.yangkeduo.com` to `https://api.pinduoduo.com/`
         * include credentials so the synced `.pinduoduo.com` session cookies are sent.
         */
        private val PDD_CREDENTIALS_BRIDGE_JS = """
            (function() {
              if (window.__pddCredBridge) return;
              window.__pddCredBridge = true;
              var isPddApi = function(u) {
                return typeof u === 'string' && (u.indexOf('https://api.pinduoduo.com/') === 0 || u.indexOf('//api.pinduoduo.com/') === 0);
              };
              var origOpen = XMLHttpRequest.prototype.open;
              XMLHttpRequest.prototype.open = function(m, u) {
                var res = origOpen.apply(this, arguments);
                if (isPddApi(u)) {
                  try { this.withCredentials = true; } catch (e) {}
                }
                return res;
              };
              if (window.fetch) {
                var origFetch = window.fetch;
                window.fetch = function(input, init) {
                  var u = typeof input === 'string' ? input : (input && input.url);
                  if (isPddApi(u)) {
                    init = Object.assign({}, init || {}, { credentials: 'include' });
                  }
                  return origFetch.call(this, input, init);
                };
              }
            })();
        """.trimIndent()
    }
}
