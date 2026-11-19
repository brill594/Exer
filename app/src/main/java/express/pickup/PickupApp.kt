package express.pickup

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

class PickupApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val mode = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
        DynamicColors.applyToActivitiesIfAvailable(this)
        CookieManager.getInstance().setAcceptCookie(true)
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    companion object {
        const val PREFS = "pickup"
        const val KEY_THEME = "theme_mode"
    }
}
