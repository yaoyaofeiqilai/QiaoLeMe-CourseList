package com.sb.courselist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sb.courselist.data.repository.ScheduleRepository

class CourseListViewModelFactory(
    private val repository: ScheduleRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourseListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CourseListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

