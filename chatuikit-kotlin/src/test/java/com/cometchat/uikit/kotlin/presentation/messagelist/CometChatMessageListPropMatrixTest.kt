package com.cometchat.uikit.kotlin.presentation.messagelist

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.ViewPropSweep
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import com.cometchat.uikit.kotlin.presentation.messagelist.ui.CometChatMessageList

/**
 * Reflective sweep over the View component's `set*` surface, on an attached instance.
 *
 * Nothing previously constructed this class -- its rendering test drove a ViewModel --
 * so the component sat at 0%.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMessageListPropMatrixTest {

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun tearDown() {
        cometChat.close()
        NonDefaultValues.clearRegistered()
    }

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            Map::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName == "BaseMessage" ||
            paramType.simpleName == "User" ||
            paramType.simpleName == "Group" ||
            paramType.simpleName == "SimpleDateFormat" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("Builder") ||
            paramType.simpleName.endsWith("ViewModel") ||
            paramType.simpleName.endsWith("Alignment") ||
            paramType.simpleName.endsWith("Listener") ||
            paramType.simpleName.endsWith("Callback")

    @Test
    fun mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatMessageList::class.java)
        assertTrue("expected a setter surface, was ${setters.size}", setters.isNotEmpty())

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val component = CometChatMessageList(activity)
            activity.setContentView(component)
            ShadowLooper.idleMainLooper()
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop(
                            "CometChatMessageList",
                            setter.propName,
                            PropKind.VALUE,
                            covered = runCatching { setter.applyNonDefault(component) }.isSuccess,
                        )
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [message list view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size}")
        if (uncovered.isNotEmpty()) println("  [message list view] NOT covered: $uncovered")

        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }
}
