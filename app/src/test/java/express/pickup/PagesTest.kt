package express.pickup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagesTest {
    @Test
    fun pagesStayOnTheWebSoAnOldPhoneDoesNotNeedTheShoppingApps() {
        assertEquals(4, Pages.all.size)
        assertEquals(
            listOf("菜鸟身份码", "淘宝身份码", "同学代取", "多多身份码"),
            Pages.all.map { it.title },
        )
        for (page in Pages.all) {
            assertTrue(page.title, page.url.startsWith("https://"))
            assertTrue(page.url, !page.url.contains("://com."))
        }
        assertTrue(Pages.all.none { it.url.contains("m-end-school-tab") })
        assertTrue(Pages.all.none { it.url.startsWith("pinduoduo:") })
        assertEquals(
            "https://mobile.yangkeduo.com/mdkd/package?tab=ID_CODE&entry_source=11",
            Pages.all.last().url,
        )
    }
}
