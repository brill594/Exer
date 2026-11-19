package express.pickup

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import express.pickup.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val initialLeft = binding.content.paddingLeft
        val initialTop = binding.content.paddingTop
        val initialRight = binding.content.paddingRight
        val initialBottom = binding.content.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            binding.content.updatePadding(
                left = initialLeft + bars.left,
                top = initialTop + bars.top,
                right = initialRight + bars.right,
                bottom = initialBottom + bars.bottom,
            )
            insets
        }

        binding.back.setOnClickListener { finish() }

        binding.version.text = getString(R.string.version_label, BuildConfig.VERSION_NAME)

        val prefs = getSharedPreferences(PickupApp.PREFS, MODE_PRIVATE)
        val current = prefs.getInt(PickupApp.KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        binding.themeToggle.check(
            when (current) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.theme_light
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.theme_dark
                else -> R.id.theme_system
            },
        )
        binding.themeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            prefs.edit { putInt(PickupApp.KEY_THEME, mode) }
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        binding.clear.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_login)
                .setMessage(R.string.clear_login_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.clear_login) { _, _ -> clearWebData() }
                .show()
        }
    }

    private fun clearWebData() {
        val appContext = applicationContext
        val cache = cacheDir
        val cookies = CookieManager.getInstance()
        cookies.removeAllCookies {
            cookies.flush()
            WebStorage.getInstance().deleteAllData()
            Thread {
                cache.listFiles()?.forEach { it.deleteRecursively() }
                runOnUiThread {
                    Toast.makeText(appContext, R.string.cleared, Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }
}
