package cn.archko.pdf

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import cn.archko.pdf.ui.home.GankDetail
import cn.archko.pdf.ui.home.HomePager
import cn.archko.pdf.utils.Screen

/**
 * Models the screens in the app and any arguments they require.
 */
object Destination {
    const val HOME = "Home"
    const val GANKGIRL = "GankGirl"

    const val VIDEOCATEGORY = "VideoCategory"

    const val GANKDETAIL = "GankDetail"
}

@ExperimentalMaterialApi
@Composable
fun NavGraph(startDestination: String = Destination.HOME) {
    val navController = rememberNavController()

    //val actions = remember(navController) { MainActions(navController) }
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Destination.HOME) {
            HomePager(navController)
        }
        composable(
            "${Destination.GANKDETAIL}/{gankStr}"
            //arguments = listOf(navArgument(COURSE_DETAIL_ID_KEY) { type = NavType.LongType })
        ) {
            val arguments = it.arguments!!.getString("gankStr") ?: ""
            GankDetail(
                gankStr = arguments,
                upPress = navController::navigateUp
            )
        }
    }
}

/**
 * Models the navigation actions in the app.
 */
class MainActions(navController: NavHostController) {
    val homeAction: (Screen) -> Unit = {
        navController.navigate(Destination.HOME)
    }
    val gankDetailAction: (Screen) -> Unit = {
        navController.navigate(Destination.GANKDETAIL)
    }

    //val VideoCategory: (Screen) -> Unit = { courseId: Long ->
    //    navController.navigate("${Destination.VideoCategory}/$courseId")
    //}
    val upPress: () -> Unit = {
        navController.navigateUp()
    }
}
