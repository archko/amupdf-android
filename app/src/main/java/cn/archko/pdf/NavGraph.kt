package cn.archko.pdf

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.archko.pdf.theme.AppThemeState
import cn.archko.pdf.ui.home.HomePager
import com.google.accompanist.pager.ExperimentalPagerApi

object Destination {
    const val HOME = "Home"
    const val GANKDETAIL = "GankDetail"
}

@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun NavGraph(
    changeTheme: (Boolean) -> Unit,
    appThemeState: MutableState<AppThemeState>,
    up: () -> Unit,
    startDestination: String = Destination.HOME
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Destination.HOME) {
            HomePager(changeTheme, appThemeState, up, navController)
        }
        composable(
            "${Destination.GANKDETAIL}/{gankStr}"
            //arguments = listOf(navArgument(COURSE_DETAIL_ID_KEY) { type = NavType.LongType })
        ) {
            //val arguments = it.arguments!!.getString("gankStr") ?: ""

        }
    }
}