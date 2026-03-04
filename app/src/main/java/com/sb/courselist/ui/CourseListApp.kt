package com.sb.courselist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sb.courselist.ui.screen.ImportScreen
import com.sb.courselist.ui.screen.TimetableScreen
import com.sb.courselist.ui.state.BottomTab
import com.sb.courselist.ui.theme.SBCourseTheme
import com.sb.courselist.ui.theme.SkyBg
import com.sb.courselist.ui.viewmodel.CourseListViewModel

@Composable
fun CourseListApp(viewModel: CourseListViewModel) {
    SBCourseTheme(darkTheme = false) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.message) {
            val message = uiState.message ?: return@LaunchedEffect
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = uiState.activeTab == BottomTab.TIMETABLE,
                        onClick = { viewModel.switchTab(BottomTab.TIMETABLE) },
                        icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = "Timetable") },
                        label = { androidx.compose.material3.Text("Timetable") },
                    )
                    NavigationBarItem(
                        selected = uiState.activeTab == BottomTab.IMPORT,
                        onClick = { viewModel.switchTab(BottomTab.IMPORT) },
                        icon = { Icon(Icons.Rounded.UploadFile, contentDescription = "Import") },
                        label = { androidx.compose.material3.Text("Import") },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SkyBg)
                    .padding(padding),
            ) {
                when (uiState.activeTab) {
                    BottomTab.TIMETABLE -> TimetableScreen(
                        schedule = uiState.currentSchedule,
                        onNavigateImport = { viewModel.switchTab(BottomTab.IMPORT) },
                        onUpdateCourse = viewModel::updatePersistedCourse,
                    )

                    BottomTab.IMPORT -> ImportScreen(
                        uiState = uiState,
                        onImportSelected = viewModel::importPdf,
                        onSavePreview = viewModel::savePreviewSchedule,
                        onUpdatePreviewCourse = viewModel::updatePreviewCourse,
                        onClearPreview = viewModel::clearPreview,
                    )
                }
            }
        }
    }
}
