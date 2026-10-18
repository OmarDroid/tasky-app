package com.omaroid.tasky.core.presentation.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Material3's [androidx.compose.material3.DropdownMenu] plays a short shrink/fade exit
 * transition (~75ms) when its `expanded` flag flips to `false`. When a menu item's action
 * navigates, deletes, or opens another overlay in the SAME frame, the menu's host can leave
 * composition before that transition runs — producing the "menu blinks out / sticks for a
 * frame during the transition" jank.
 *
 * The fix: close the menu first (flip `expanded` to false so the exit starts), wait this long,
 * then run the action. The value sits above the Material out-transition (~75ms) for headroom.
 */
const val DROPDOWN_MENU_EXIT_MILLIS = 120

/**
 * Runs [action] after a dropdown menu's exit transition has had time to play. The call site
 * closes the menu first (flip the local `expanded` flag or dispatch the dismiss action), then
 * `scope.afterMenuExit { navigate() }` so the menu animates away cleanly before the action tears
 * the host out of composition. Returns the [Job] so callers can cancel if needed (navigation that
 * unmounts the host cancels the scope automatically, which also debounces double-taps).
 */
fun CoroutineScope.afterMenuExit(action: () -> Unit): Job =
    launch {
        delay(DROPDOWN_MENU_EXIT_MILLIS.milliseconds)
        action()
    }
