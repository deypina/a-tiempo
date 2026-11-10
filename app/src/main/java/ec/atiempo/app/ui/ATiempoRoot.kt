package ec.atiempo.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import ec.atiempo.app.R
import ec.atiempo.app.ui.meds.EditMedicationScreen
import ec.atiempo.app.ui.meds.EditMedicationViewModel
import ec.atiempo.app.ui.meds.MedsScreen
import ec.atiempo.app.ui.today.TodayScreen

private enum class Tab(
    val route: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    Today("today", R.string.tab_today, R.drawable.ic_tab_today),
    Meds("meds", R.string.tab_meds, R.drawable.ic_tab_meds),
    Settings("settings", R.string.tab_settings, R.drawable.ic_tab_settings),
}

private const val EDIT_MED_ROUTE =
    "meds/edit?${EditMedicationViewModel.ARG_MEDICATION_ID}={${EditMedicationViewModel.ARG_MEDICATION_ID}}"

private fun editMedRoute(id: Long?) = "meds/edit?${EditMedicationViewModel.ARG_MEDICATION_ID}=${id ?: -1L}"

@Composable
fun ATiempoRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    // "meds/edit..." also belongs to the Meds tab.
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == tab.route || it.route?.startsWith("${tab.route}/") == true
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // The label below already names the tab for TalkBack.
                        icon = {
                            Icon(
                                painter = painterResource(tab.icon),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                        },
                        label = { Text(stringResource(tab.label)) },
                        modifier = Modifier.heightIn(min = 56.dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Tab.Today.route) {
                TodayScreen()
            }
            composable(Tab.Meds.route) {
                MedsScreen(
                    onAdd = { navController.navigate(editMedRoute(null)) },
                    onOpen = { id -> navController.navigate(editMedRoute(id)) },
                )
            }
            composable(
                route = EDIT_MED_ROUTE,
                arguments = listOf(
                    navArgument(EditMedicationViewModel.ARG_MEDICATION_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) {
                EditMedicationScreen(onClose = { navController.popBackStack() })
            }
            composable(Tab.Settings.route) {
                PlaceholderScreen(
                    title = stringResource(R.string.settings_title),
                    body = stringResource(R.string.disclaimer),
                )
            }
        }
    }
}

/** Temporary screen body until each tab is built in later phases. */
@Composable
private fun PlaceholderScreen(title: String, body: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        if (body != null) {
            Text(text = body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
