package cn.archko.sunflower.ui.utils

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key.Companion.Home
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.jetnews.utils.getMutableStateOf

/**
 * Screen names (used for serialization)
 */
enum class ScreenName { Home, VideoCategory, FileList, GankGirl, GankDetail, BookSheet }

/**
 * Class defining the screens we have in the app: home, article details and interests
 */
sealed class Screen(val id: ScreenName) {
    object Home : Screen(ScreenName.Home)
    object GankGirl : Screen(ScreenName.GankGirl)
    object FileList : Screen(ScreenName.FileList)
    data class BookSheet(val fileBean: String) : Screen(ScreenName.BookSheet)
    data class VideoDetail(val postId: String) : Screen(ScreenName.VideoCategory)
    data class GankDetail(val gankBean: String) : Screen(ScreenName.GankDetail)
}
//
///**
// * Helpers for saving and loading a [Screen] object to a [Bundle].
// *
// * This allows us to persist navigation across process death, for example caused by a long video
// * call.
// */
//private const val SIS_SCREEN = "sis_screen"
//private const val SIS_NAME = "screen_name"
//private const val SIS_POST = "post"
//
///**
// * Convert a screen to a bundle that can be stored in [SavedStateHandle]
// */
//private fun Screen.toBundle(): Bundle {
//    return bundleOf(SIS_NAME to id.name).also {
//        // add extra keys for various types here
//        if (this is Screen.VideoDetail) {
//            it.putString(SIS_POST, postId)
//        }
//    }
//}
//
///**
// * Read a bundle stored by [Screen.toBundle] and return desired screen.
// *
// * @return the parsed [Screen]
// * @throws IllegalArgumentException if the bundle could not be parsed
// */
//private fun Bundle.toScreen(): Screen {
//    val screenName = ScreenName.valueOf(getStringOrThrow(SIS_NAME))
//    return when (screenName) {
//        ScreenName.GankGirl -> Screen.GankGirl
//        ScreenName.FileList -> Screen.FileList
//        ScreenName.VideoCategory -> {
//            val postId = getStringOrThrow(SIS_POST)
//            Screen.VideoDetail(postId)
//        }
//        else -> Screen.GankGirl
//    }
//}
//
///**
// * Throw [IllegalArgumentException] if key is not in bundle.
// *
// * @see Bundle.getString
// */
//private fun Bundle.getStringOrThrow(key: String) =
//    requireNotNull(getString(key)) { "Missing key '$key' in $this" }
//
///**
// * This is expected to be replaced by the navigation component, but for now handle navigation
// * manually.
// *
// * Instantiate this ViewModel at the scope that is fully-responsible for navigation, which in this
// * application is [MainActivity].
// *
// * This app has simplified navigation; the back stack is always [Home] or [Home, dest] and more
// * levels are not allowed. To use a similar pattern with a longer back stack, use a [StateList] to
// * hold the back stack state.
// */
//class NavigationViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
//    /**
//     * Hold the current screen in an observable, restored from savedStateHandle after process
//     * death.
//     *
//     * mutableStateOf is an observable similar to LiveData that's designed to be read by compose. It
//     * supports observability via property delegate syntax as shown here.
//     */
//    var currentScreen: Screen by savedStateHandle.getMutableStateOf<Screen>(
//        key = SIS_SCREEN,
//        default = Screen.Home,
//        save = { it.toBundle() },
//        restore = { it.toScreen() }
//    )
//        private set // limit the writes to only inside this class.
//
//    /**
//     * Go back (always to [Home]).
//     *
//     * Returns true if this call caused user-visible navigation. Will always return false
//     * when [currentScreen] is [Home].
//     */
//    @MainThread
//    fun onBack(): Boolean {
//        val wasHandled = currentScreen != Screen.Home
//        currentScreen = Screen.Home
//        return wasHandled
//    }
//
//    /**
//     * Navigate to requested [Screen].
//     *
//     * If the requested screen is not [Home], it will always create a back stack with one element:
//     * ([Home] -> [screen]). More back entries are not supported in this app.
//     */
//    @MainThread
//    fun navigateTo(screen: Screen) {
//        currentScreen = screen
//    }
//}
