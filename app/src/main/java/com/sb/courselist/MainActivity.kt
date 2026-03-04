package com.sb.courselist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sb.courselist.ui.CourseListApp
import com.sb.courselist.ui.viewmodel.CourseListViewModel
import com.sb.courselist.ui.viewmodel.CourseListViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as CourseListApplication).appContainer.scheduleRepository

        setContent {
            val viewModel: CourseListViewModel = viewModel(
                factory = CourseListViewModelFactory(repository),
            )
            CourseListApp(viewModel = viewModel)
        }
    }
}

