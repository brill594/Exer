package express.pickup

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.shape.ShapeAppearanceModel
import express.pickup.databinding.ActivityMainBinding
import express.pickup.databinding.ItemPageBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
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

        binding.settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val density = resources.displayMetrics.density
        val outerRadius = 24f * density
        val innerRadius = 6f * density
        val pages = Pages.all

        pages.forEachIndexed { index, page ->
            val item = ItemPageBinding.inflate(layoutInflater, binding.pages, true)
            item.title.text = page.title
            item.detail.text = page.detail
            item.iconBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, page.containerColorRes),
            )
            item.icon.setImageResource(page.iconRes)
            item.icon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, page.contentColorRes),
            )

            val topCorner = if (index == 0) outerRadius else innerRadius
            val bottomCorner = if (index == pages.lastIndex) outerRadius else innerRadius
            item.card.shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setTopLeftCornerSize(topCorner)
                .setTopRightCornerSize(topCorner)
                .setBottomLeftCornerSize(bottomCorner)
                .setBottomRightCornerSize(bottomCorner)
                .build()

            if (index == pages.lastIndex) {
                item.card.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = 0
                }
            }

            item.card.setOnClickListener {
                startActivity(
                    Intent(this, PageActivity::class.java)
                        .putExtra(PageActivity.EXTRA_TITLE, page.title)
                        .putExtra(PageActivity.EXTRA_URL, page.url),
                )
            }
        }
    }
}
