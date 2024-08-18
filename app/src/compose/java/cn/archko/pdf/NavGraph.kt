package cn.archko.pdf

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.archko.pdf.ui.home.HomePager

object Destination {
    const val HOME = "Home"
}

@Composable
fun NavGraph(
    changeTheme: (Boolean) -> Unit,
    darkTheme: Boolean,
    up: () -> Unit,
    startDestination: String = Destination.HOME,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Destination.HOME) {
            HomePager(changeTheme, darkTheme, up, navController)
        }
    }
}