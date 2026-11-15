package io.github.petrf22.vernostnikarta.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.petrf22.vernostnikarta.ui.detail.DetailScreen
import io.github.petrf22.vernostnikarta.ui.edit.EditScreen
import io.github.petrf22.vernostnikarta.ui.list.CardListScreen
import io.github.petrf22.vernostnikarta.ui.scan.ScanScreen
import io.github.petrf22.vernostnikarta.ui.settings.SettingsScreen

object Routes {
    const val LIST = "list"
    const val SCAN = "scan"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"
    const val EDIT = "edit/{id}?code={code}&format={format}"

    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long, code: String = "", format: String = "") =
        "edit/$id?code=${Uri.encode(code)}&format=${Uri.encode(format)}"
}

/**
 * @param debugRoute a route to jump to, sent over ADB in debug builds (see MainActivity)
 */
@Composable
fun AppNavigation(debugRoute: String? = null, onDebugRouteConsumed: () -> Unit = {}) {
    val navController = rememberNavController()

    LaunchedEffect(debugRoute) {
        if (debugRoute != null) {
            runCatching { navController.navigate(debugRoute) }
            onDebugRouteConsumed()
        }
    }

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            CardListScreen(
                onAdd = { navController.navigate(Routes.SCAN) },
                onOpen = { navController.navigate(Routes.detail(it)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onResult = { code, format ->
                    navController.navigate(Routes.edit(0, code, format)) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                },
                onManual = {
                    navController.navigate(Routes.edit(0)) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("code") { type = NavType.StringType; defaultValue = "" },
                navArgument("format") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val args = entry.arguments
            EditScreen(
                cardId = args?.getLong("id") ?: 0L,
                initialCode = args?.getString("code").orEmpty(),
                initialFormat = args?.getString("format").orEmpty(),
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            DetailScreen(
                cardId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.edit(id)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
