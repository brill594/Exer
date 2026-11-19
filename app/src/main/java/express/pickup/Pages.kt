package express.pickup

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

/**
 * Official mobile pages for showing a pickup identity code.
 * Each address is https so the page opens inside this app. None of them
 * are app deep links, and the campus-station page is intentionally absent.
 */
data class PickupPage(
    val title: String,
    val detail: String,
    val url: String,
    @ColorRes val containerColorRes: Int,
    @ColorRes val contentColorRes: Int,
    @DrawableRes val iconRes: Int,
)

object Pages {
    val all: List<PickupPage> = listOf(
        PickupPage(
            title = "菜鸟身份码",
            detail = "菜鸟驿站扫这个码",
            url = "https://page.cainiao.com/cn-app-web/identity-code/index.html#/?source=&bizEntry=ALIPAY_GUOGUO",
            containerColorRes = R.color.tone_cainiao_bg,
            contentColorRes = R.color.tone_cainiao_fg,
            iconRes = R.drawable.ic_qr_code,
        ),
        PickupPage(
            title = "淘宝身份码",
            detail = "淘宝包裹的身份码",
            url = "https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code?x-ssr=true",
            containerColorRes = R.color.tone_taobao_bg,
            contentColorRes = R.color.tone_taobao_fg,
            iconRes = R.drawable.ic_bag,
        ),
        PickupPage(
            title = "同学代取",
            detail = "把取件授权给同学",
            url = "https://page.cainiao.com/mcn/agent-pickup/index.html?disableNav=YES#/",
            containerColorRes = R.color.tone_agent_bg,
            contentColorRes = R.color.tone_agent_fg,
            iconRes = R.drawable.ic_group,
        ),
        PickupPage(
            title = "多多身份码",
            detail = "拼多多包裹的身份码",
            url = "https://mobile.yangkeduo.com/mdkd/package?tab=ID_CODE&entry_source=11",
            containerColorRes = R.color.tone_pdd_bg,
            contentColorRes = R.color.tone_pdd_fg,
            iconRes = R.drawable.ic_parcel,
        ),
    )
}
