package cn.archko.pdf

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import cn.archko.pdf.ui.home.HomePager
import com.google.accompanist.pager.ExperimentalPagerApi

/**
 * Models the screens in the app and any arguments they require.
 */
object Destination {
    const val HOME = "Home"
    const val GANKGIRL = "GankGirl"

    const val VIDEOCATEGORY = "VideoCategory"

    const val GANKDETAIL = "GankDetail"
}

@ExperimentalPagerApi
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
            //val arguments = it.arguments!!.getString("gankStr") ?: ""

        }
    }
}