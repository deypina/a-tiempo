package ec.atiempo.app.e2e

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import ec.atiempo.app.MainActivity
import ec.atiempo.app.R
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end checks of the main flows, run on an emulator in CI (docs/CASOS_DE_PRUEBA.md).
 * Every test starts from a fresh install (clearPackageData in app/build.gradle.kts).
 * Alarms ringing on time, the lock screen and the phone's font size are still checked by hand.
 */
@RunWith(AndroidJUnit4::class)
class MedicationJourneyTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun str(@StringRes id: Int, vararg args: Any): String = rule.activity.getString(id, *args)

    @Test
    fun welcome_savesNameAndOpensEmptyToday() {
        completeWelcome("Rosa")

        waitFor(hasText(str(R.string.today_greeting, "Rosa")))
        waitFor(hasText(str(R.string.today_empty)))
    }

    @Test
    fun medication_addTakeUndoPurchaseAndHistory() {
        completeWelcome("Rosa")

        // Add a medication: 1 pill at 08:00, 10 pills at home.
        tapTab(R.string.tab_meds)
        tap(str(R.string.meds_add))
        field(str(R.string.med_name)).performScrollTo().performTextInput(MED)
        tap(str(R.string.med_add_first_time))
        tap(str(R.string.common_ok))
        field(str(R.string.med_stock)).performScrollTo().performTextReplacement("10")
        tap(str(R.string.med_save))
        waitFor(hasText(MED))

        // "Ya la tomé" from Hoy takes one pill; the list shows the new count.
        tapTab(R.string.tab_today)
        tap(str(R.string.btn_taken))
        waitFor(hasText(str(R.string.btn_undo)) and hasClickAction())
        tapTab(R.string.tab_meds)
        waitFor(hasText(str(R.string.stock_qty_other, "9"), substring = true))

        // "Deshacer" gives it back and the intake waits for "Ya la tomé" again.
        tapTab(R.string.tab_today)
        tap(str(R.string.btn_undo))
        waitFor(hasText(str(R.string.btn_taken)) and hasClickAction())
        tapTab(R.string.tab_meds)
        waitFor(hasText(str(R.string.stock_qty_other, "10"), substring = true))

        // With enough pills the list has no purchase button; "Ver más" opens the detail.
        assertFalse(exists(hasText(str(R.string.purchase_btn)) and hasClickAction()))
        tap(str(R.string.meds_more))

        // A purchase of 20 adds to the count.
        tap(str(R.string.purchase_btn))
        field(str(R.string.purchase_other)).performTextInput("20")
        tap(str(R.string.purchase_save))
        waitFor(hasText(str(R.string.stock_qty_other, "30"), substring = true))

        // The history keeps every movement, the undone intake included.
        tap(str(R.string.history_btn))
        waitFor(hasText(str(R.string.history_taken, str(R.string.qty_pill_one, "1"))))
        waitFor(hasText(str(R.string.history_undo)))
        waitFor(hasText(str(R.string.history_purchase, str(R.string.qty_pill_other, "20"))))
    }

    @Test
    fun about_showsAuthorFeedbackAndDisclaimer() {
        completeWelcome("Rosa")

        tapTab(R.string.tab_settings)
        tap(str(R.string.settings_about))
        waitFor(hasText(str(R.string.about_credit)))
        waitFor(hasText(str(R.string.about_feedback)) and hasClickAction())
        waitFor(hasText(str(R.string.about_repo)) and hasClickAction())
        waitFor(hasText(str(R.string.about_disclaimer)))
    }

    /** Types the name and answers "Ahora no" to every permission the emulator still lacks. */
    private fun completeWelcome(name: String) {
        waitFor(hasText(str(R.string.welcome_title)))
        rule.onNode(hasSetTextAction()).performTextInput(name)
        tap(str(R.string.welcome_continue))

        val later = hasText(str(R.string.perm_later)) and hasClickAction()
        val home = hasText(str(R.string.tab_today)) and hasClickAction()
        repeat(MAX_PERMISSION_STEPS) {
            rule.waitUntil(TIMEOUT_MS) { exists(later) || exists(home) }
            if (exists(home)) return
            rule.onNode(later).performClick()
            rule.waitForIdle()
        }
        waitFor(home)
    }

    private fun tapTab(@StringRes label: Int) {
        rule.onAllNodes(hasText(str(label)) and hasClickAction()).onFirst().performClick()
        rule.waitForIdle()
    }

    /** Taps the first clickable element showing [text], scrolling to it when it sits in a long screen. */
    private fun tap(text: String) {
        val matcher = hasText(text) and hasClickAction()
        waitFor(matcher)
        val node = rule.onAllNodes(matcher).onFirst()
        if (inScrollable(matcher)) node.performScrollTo()
        node.performClick()
        rule.waitForIdle()
    }

    private fun field(label: String): SemanticsNodeInteraction {
        val matcher = hasSetTextAction() and hasText(label)
        waitFor(matcher)
        return rule.onNode(matcher)
    }

    private fun inScrollable(matcher: SemanticsMatcher): Boolean =
        exists(matcher and hasAnyAncestor(hasScrollAction()))

    private fun exists(matcher: SemanticsMatcher): Boolean =
        rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(matcher: SemanticsMatcher) {
        rule.waitUntil(TIMEOUT_MS) { exists(matcher) }
    }

    private companion object {
        const val MED = "Losartán"
        const val TIMEOUT_MS = 10_000L
        const val MAX_PERMISSION_STEPS = 4
    }
}
