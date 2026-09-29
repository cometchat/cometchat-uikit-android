package com.cometchat.sampleapp.compose.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestHelper
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestHelper.PACKAGE
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestHelper.SHORT_TIMEOUT
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.regex.Pattern

/**
 * ENG-39110 — accessibility audit E2E for sample-app-compose (Android parity with iOS
 * E2EAccessibilityAuditTests). Mirror of the kotlin suite, Option C (UI-Automator, uniform):
 * Compose renders into the SAME AccessibilityNodeInfo tree UI Automator reads, so the same
 * per-screen audit applies. For each actionable node: assert an accessible name (own or a
 * descendant's text/contentDescription — Compose exposes labels as semantics) and >= 48dp leaf
 * tap targets. Each main screen at default font; main tabs re-audited at the largest font.
 *
 * Not covered by Option C (no pixel access): contrast, precise truncation.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AccessibilityAuditE2ETest {

    private lateinit var device: UiDevice
    private val anyText = Pattern.compile(".+", Pattern.DOTALL)
    private val minTargetPx: Int
        get() = (48 * InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density).toInt()

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        E2ETestHelper.fullSetupAndLogin(device)
    }

    @After
    fun resetFont() {
        device.executeShellCommand("settings put system font_scale 1.0")
    }

    private fun isContainer(o: UiObject2): Boolean = try {
        val c = o.className ?: ""
        c.contains("Layout") || c.contains("RecyclerView") || c.contains("ScrollView") ||
            c.contains("ViewGroup") || c.contains("ViewPager")
    } catch (_: Exception) { true }

    private fun hasAccessibleName(o: UiObject2): Boolean = try {
        // A text input's accessible name is its hint; other controls use text/contentDescription
        // (own or a descendant's).
        !o.text.isNullOrBlank() || !o.contentDescription.isNullOrBlank() ||
            !o.hint.isNullOrBlank() ||
            o.findObjects(By.text(anyText)).isNotEmpty() ||
            o.findObjects(By.desc(anyText)).isNotEmpty()
    } catch (_: Exception) { true }

    private fun auditCurrentScreen(screen: String): List<String> {
        device.waitForIdle()
        Thread.sleep(1500)
        val violations = mutableListOf<String>()
        for (o in device.findObjects(By.pkg(PACKAGE).clickable(true))) {
            try {
                val b = o.visibleBounds
                val w = b.width()
                val h = b.height()
                if (w <= 0 || h <= 0) continue
                val id = (o.resourceName ?: o.className ?: "?").substringAfterLast('/')
                if (!hasAccessibleName(o) && !isContainer(o)) {
                    violations.add("[$screen] unlabeled actionable: $id @ ${w}x${h}px")
                }
                val isLeaf = o.findObjects(By.clickable(true)).isEmpty()
                if (isLeaf && (w < minTargetPx || h < minTargetPx)) {
                    violations.add("[$screen] small tap target: $id ${w}x${h}px (< ${minTargetPx}px / 48dp)")
                }
            } catch (_: Exception) { /* stale — skip */ }
        }
        return violations
    }

    private fun assertScreenClean(screen: String, navigate: () -> Unit) {
        navigate()
        val v = auditCurrentScreen(screen)
        assertTrue(
            "Accessibility violations on \"$screen\" (${v.size}):\n  " + v.joinToString("\n  "),
            v.isEmpty()
        )
    }

    private fun openSearch() {
        E2ETestHelper.navigateToTab(device, "Chats")
        val icon = E2ETestHelper.waitFor(device, By.desc("Search"), SHORT_TIMEOUT)
            ?: E2ETestHelper.waitFor(device, By.descContains("Search"), SHORT_TIMEOUT)
            ?: device.findObject(By.text("Search"))
        assertNotNull("Search entry not found", icon)
        icon!!.click()
        E2ETestHelper.waitFor(device, By.clazz("android.widget.EditText"), SHORT_TIMEOUT)
    }

    @Test fun test01_chatsScreen() = assertScreenClean("Chats") { E2ETestHelper.navigateToTab(device, "Chats") }

    @Test fun test02_usersScreen() = assertScreenClean("Users") { E2ETestHelper.navigateToTab(device, "Users") }

    @Test fun test03_groupsScreen() = assertScreenClean("Groups") { E2ETestHelper.navigateToTab(device, "Groups") }

    @Test fun test04_callsScreen() = assertScreenClean("Calls") { E2ETestHelper.navigateToTab(device, "Calls") }

    @Test fun test05_searchScreen() = assertScreenClean("Search") { openSearch() }

    @Test
    fun test10_largestFontMainTabs() {
        device.executeShellCommand("settings put system font_scale 1.30")
        E2ETestHelper.launchApp(device)
        device.waitForIdle()
        Thread.sleep(2500)
        val v = mutableListOf<String>()
        for (tab in listOf("Chats", "Users", "Groups", "Calls")) {
            E2ETestHelper.navigateToTab(device, tab)
            v += auditCurrentScreen("$tab @1.3x")
        }
        assertTrue(
            "Accessibility violations at largest font (${v.size}):\n  " + v.joinToString("\n  "),
            v.isEmpty()
        )
    }
}
