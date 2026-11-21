package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.viewmodel.EditorToolTab
import com.example.viewmodel.HomeViewModel
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class EditorRoute(
    val projectId: String,
    val initialToolName: String? = null
)

@Composable
fun ThyCutNavGraph(
    homeViewModel: HomeViewModel = viewModel()
) {
    val navController = rememberNavController()
    val language by homeViewModel.language.collectAsStateWithLifecycle()
    val projects by homeViewModel.projects.collectAsStateWithLifecycle()
    val exports by homeViewModel.exports.collectAsStateWithLifecycle()
    val isCreating by homeViewModel.isCreatingProject.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            HomeScreen(
                language = language,
                projects = projects,
                exports = exports,
                isCreatingProject = isCreating,
                onToggleLanguage = { homeViewModel.toggleLanguage() },
                onCreateProject = { uris, template, initialTool ->
                    homeViewModel.createProjectFromUris(uris, template) { projectId ->
                        navController.navigate(
                            EditorRoute(
                                projectId = projectId,
                                initialToolName = initialTool?.name
                            )
                        )
                    }
                },
                onOpenProject = { projectId ->
                    navController.navigate(EditorRoute(projectId = projectId))
                },
                onRenameProject = { project, newTitle ->
                    homeViewModel.renameProject(project, newTitle)
                },
                onDuplicateProject = { project ->
                    homeViewModel.duplicateProject(project)
                },
                onDeleteProject = { projectId ->
                    homeViewModel.deleteProject(projectId)
                },
                onDeleteExport = { record ->
                    homeViewModel.deleteExport(record)
                }
            )
        }

        composable<EditorRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EditorRoute>()
            val initialTool = route.initialToolName?.let { name ->
                runCatching { EditorToolTab.valueOf(name) }.getOrNull()
            }
            EditorScreen(
                projectId = route.projectId,
                initialTool = initialTool,
                language = language,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
