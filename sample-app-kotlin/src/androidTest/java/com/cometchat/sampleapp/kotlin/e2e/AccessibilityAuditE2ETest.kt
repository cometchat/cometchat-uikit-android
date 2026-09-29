package com.cometchat.sampleapp.kotlin.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper.PACKAGE
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper.SHORT_TIMEOUT
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
 * ENG-39110 — accessibility audit E2E (Android parity with iOS E2EAccessibilityAuditTests).
 *
 * Option C (UI-Automator, uniform across toolkits): walks the on-screen accessibility node tree
 * — the SAME AccessibilityNodeInfo tree TalkBack consumes, for both View and Compose UIKits — and
 * for every actionable (clickable) node asserts:
 *   1. it has an accessible name (its own text/contentDescription, or a descendant's — so a
 *      labelled row is not a false positive), and
 *   2. leaf tap targets are >= 48dp x 48dp.
 * Each main 3B screen is audited at the default font; the main tabs are re-audited at the largest
 * system font (Dynamic-Type equivalent).
 *
 * Not covered by Option C (no pixel access via UI Automator): contrast, precise truncation.
 *
 * A finding here is usually a UIKit fix (add the missing semantics), not a test fix.
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

    // ─── The audit ────────────────────────────────────────────────────────────────

    /** Layout/scroll containers are audited via their labelled children, not themselves. */
    private fun isContainer(o: UiObject2): Boolean = try {
        val c = o.className ?: ""
        c.contains("Layout") || c.contains("RecyclerView") || c.contains("ScrollView") ||
            c.contains("ViewGroup") || c.contains("ViewPager")
    } catch (_: Exception) { true }

    /** True if [o] or any descendant exposes a non-blank text or contentDescription. */
    private fun hasAccessibleName(o: UiObject2): Boolean = try {
        // A text input's accessible name is its hint; other controls use text/contentDescription
        // (own or a descendant's).
        !o.text.isNullOrBlank() || !o.contentDescription.isNullOrBlank() ||
            !o.hint.isNullOrBlank() ||
            o.findObjects(By.text(anyText)).isNotEmpty() ||
            o.findObjects(By.desc(anyText)).isNotEmpty()
    } catch (_: Exception) {
        true // node went stale — don't raise a false violation
    }

    /** Audits the current screen; returns the list of violations (empty = clean). */
    private fun auditCurrentScreen(screen: String): List<String> {
        device.waitForIdle()
        Thread.sleep(1500)
        val violations = mutableListOf<String>()
        // Scope to the app under test (skip system UI / status bar).
        val clickables = device.findObjects(By.pkg(PACKAGE).clickable(true))
        for (o in clickables) {
            try {
                val b = o.visibleBounds
                val w = b.width()
                val h = b.height()
                if (w <= 0 || h <= 0) continue // not actually on screen
                val id = (o.resourceName ?: o.className ?: "?").substringAfterLast('/')
                if (!hasAccessibleName(o) && !isContainer(o)) {
                    violations.add("[$screen] unlabeled actionable: $id @ ${w}x${h}px")
                }
                // Touch-target check only for leaf actionables (a clickable container is fine).
                val isLeaf = o.findObjects(By.clickable(true)).isEmpty()
                if (isLeaf && (w < minTargetPx || h < minTargetPx)) {
                    violations.add("[$screen] small tap target: $id ${w}x${h}px (< ${minTargetPx}px / 48dp)")
                }
            } catch (_: Exception) { /* stale node — skip */ }
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
        val icon = E2ETestHelper.waitForObject(device, By.desc("Search"), SHORT_TIMEOUT)
            ?: E2ETestHelper.waitForObject(device, By.descContains("Search"), SHORT_TIMEOUT)
            ?: device.findObject(By.text("Search"))
        assertNotNull("Search entry not found", icon)
        icon!!.click()
        E2ETestHelper.waitForObject(device, By.clazz("android.widget.EditText"), SHORT_TIMEOUT)
    }

    // ─── Per-screen audits at default font ─────────────────────────────────────────
    @Test fun test01_chatsScreen() = assertScreenClean("Chats") { E2ETestHelper.navigateToTab(device, "Chats") }

    @Test fun test02_usersScreen() = assertScreenClean("Users") { E2ETestHelper.navigateToTab(device, "Users") }

    @Test fun test03_groupsScreen() = assertScreenClean("Groups") { E2ETestHelper.navigateToTab(device, "Groups") }

    @Test fun test04_callsScreen() = assertScreenClean("Calls") { E2ETestHelper.navigateToTab(device, "Calls") }

    @Test fun test05_searchScreen() = assertScreenClean("Search") { openSearch() }

    // ─── Largest font (Dynamic-Type equivalent) across the main tabs ───────────────
    @Test
    fun test10_largestFontMainTabs() {
        device.executeShellCommand("settings put system font_scale 1.30")
        E2ETestHelper.launchApp(device) // relaunch so the app re-renders at the new scale
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
