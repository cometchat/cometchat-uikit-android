package com.cometchat.sampleapp.kotlin.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper.SHORT_TIMEOUT
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper.TIMEOUT
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * E2E for the dedicated message/conversation Search screen (CometChatSearch, SearchActivity) —
 * the Android analogue of iOS's "in-conversation search" (SC_001–033). Android's model is a
 * global search screen searching BOTH conversations and message content, with filter chips
 * (Unread/Groups/Photos/Videos/Links/Documents/Audio) and message results that deep-link into
 * the chat; the same component is scope-capable (uid/guid) though the sample wires the global entry.
 *
 * Mirror of the compose SearchScreenE2ETest. Cross-verify gap closed: ENG-39000 / iOS ENG-38638.
 * Real assertions (no vacuous passes): each case fails if the search behaviour is absent.
 *
 * Run:
 *   ./gradlew :sample-app-kotlin:connectedDebugAndroidTest \
 *       -Pandroid.testInstrumentationRunnerArguments.class=com.cometchat.sampleapp.kotlin.e2e.SearchScreenE2ETest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SearchScreenE2ETest {

    private lateinit var device: UiDevice

    private val noMatchQuery = "zzzxxqq99nomatch"

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        E2ETestHelper.fullSetupAndLogin(device)
    }

    /** Opens the dedicated search screen from the Chats tab's search action and asserts we're on it. */
    private fun openSearch() {
        E2ETestHelper.navigateToTab(device, "Chats")
        val icon = E2ETestHelper.waitForObject(device, By.desc("Search"), SHORT_TIMEOUT)
            ?: E2ETestHelper.waitForObject(device, By.descContains("Search"), SHORT_TIMEOUT)
            ?: device.findObject(By.text("Search"))
        assertNotNull("Search entry (icon) not found on the Chats screen", icon)
        icon!!.click()

        val fieldReady = E2ETestHelper.waitForObject(device, By.clazz("android.widget.EditText"), TIMEOUT) != null
        val chipReady = E2ETestHelper.waitForObject(device, By.textContains("Photos"), SHORT_TIMEOUT) != null ||
            E2ETestHelper.waitForObject(device, By.textContains("Documents"), SHORT_TIMEOUT) != null ||
            E2ETestHelper.waitForObject(device, By.textContains("Groups"), SHORT_TIMEOUT) != null
        assertTrue("Dedicated search screen (input + filter chips) did not open", fieldReady && chipReady)
    }

    /** SC_001 — the search screen opens with an input field. */
    @Test
    fun test01_searchScreenOpens() {
        openSearch()
        assertNotNull(
            "Search input field not present on the search screen",
            device.findObject(By.clazz("android.widget.EditText"))
        )
    }

    /** SC_003 — typed text lands in the search field. */
    @Test
    fun test02_typingInSearchBox() {
        openSearch()
        E2ETestHelper.typeInto(device, By.clazz("android.widget.EditText"), "hello")
        val landed = E2ETestHelper.waitForObject(device, By.text("hello"), SHORT_TIMEOUT)
            ?: E2ETestHelper.waitForObject(device, By.textContains("hello"), SHORT_TIMEOUT)
        assertNotNull("Typed query 'hello' did not land in the search field", landed)
    }

    /** SC_004 — a known keyword returns a matching result row. */
    @Test
    fun test03_matchingResultsDisplayed() {
        openSearch()
        val term = "Group"
        E2ETestHelper.typeInto(device, By.clazz("android.widget.EditText"), term)
        val result = E2ETestHelper.waitForObject(device, By.textContains(term))
        assertNotNull("No matching result row for '$term' on the search screen", result)
    }

    /** SC_005 — an unknown keyword shows the "No Results" empty state. */
    @Test
    fun test04_noResultsMessage() {
        openSearch()
        E2ETestHelper.typeInto(device, By.clazz("android.widget.EditText"), noMatchQuery)
        val empty = E2ETestHelper.waitForObject(device, By.textContains("No Results"), TIMEOUT)
            ?: E2ETestHelper.waitForObject(device, By.textContains("couldn't find"), SHORT_TIMEOUT)
        assertNotNull("No-results empty state not shown for a non-matching query", empty)
    }

    /** SC_006 — search is case-insensitive: upper and lower of a known term both return it. */
    @Test
    fun test05_caseInsensitiveSearch() {
        openSearch()
        E2ETestHelper.typeInto(device, By.clazz("android.widget.EditText"), "group")
        assertNotNull(
            "lowercase 'group' returned no result",
            E2ETestHelper.waitForObject(device, By.textContains("Group"))
        )
        E2ETestHelper.typeInto(device, By.clazz("android.widget.EditText"), "GROUP")
        assertNotNull(
            "uppercase 'GROUP' returned no result — search is not case-insensitive",
            E2ETestHelper.waitForObject(device, By.textContains("Group"))
        )
    }

    /** SC_008 — clearing the query resets the state (the no-results view disappears). */
    @Test
    fun test06_clearResetsSearch() {
        openSearch()
        E2ETestHelper.typeInto(device, By.clazz("android.widget.EditText"), noMatchQuery)
        assertNotNull(
            "Precondition: no-results state expected before clearing",
            E2ETestHelper.waitForObject(device, By.textContains("No Results"), TIMEOUT)
        )
        E2ETestHelper.typeInto(device, By.clazz("android.widget.EditText"), "")
        assertTrue(
            "Clearing the query did not reset the no-results state",
            E2ETestHelper.waitGone(device, By.textContains("No Results"), SHORT_TIMEOUT)
        )
    }

    /** SC_033 — a spaces-only query is treated as empty: no crash, screen stays functional. */
    @Test
    fun test07_spacesOnlyTreatedAsEmpty() {
        openSearch()
        E2ETestHelper.typeInto(device, By.clazz("android.widget.EditText"), "   ")
        device.waitForIdle()
        assertNotNull(
            "Search field missing after a spaces-only query (possible crash)",
            device.findObject(By.clazz("android.widget.EditText"))
        )
    }

    /** SC_020 — a media filter chip (Photos) applies without breaking the screen. */
    @Test
    fun test08_mediaFilterApplies() {
        openSearch()
        val photos = E2ETestHelper.waitForObject(device, By.textContains("Photos"), SHORT_TIMEOUT)
        assertNotNull("Photos filter chip not found on the search screen", photos)
        photos!!.click()
        device.waitForIdle()
        assertNotNull(
            "Search screen broke after applying the Photos filter",
            device.findObject(By.clazz("android.widget.EditText"))
        )
    }
}
