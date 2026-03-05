package com.sb.courselist.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sb.courselist.data.repository.ScheduleRepository
import com.sb.courselist.domain.model.CourseEntry
import com.sb.courselist.domain.model.ParsedSchedule
import com.sb.courselist.domain.model.ScheduleMeta
import com.sb.courselist.parser.IssueLevel
import com.sb.courselist.parser.ParseIssue
import com.sb.courselist.parser.ParseResult
import com.sb.courselist.ui.state.BottomTab
import com.sb.courselist.ui.state.CourseListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CourseListViewModel(
    private val repository: ScheduleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CourseListUiState())
    val uiState: StateFlow<CourseListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCurrentSchedule().collect { schedule ->
                _uiState.update { state ->
                    state.copy(currentSchedule = schedule)
                }
            }
        }
    }

    fun switchTab(tab: BottomTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun importPdf(uri: Uri, fileName: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isParsing = true,
                    selectedFileName = fileName,
                    importIssues = emptyList(),
                    message = null,
                )
            }

            when (val result = repository.importAndParse(uri)) {
                is ParseResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isParsing = false,
                            previewSchedule = result.schedule,
                            importIssues = result.issues,
                            message = "解析完成，请核对后保存。",
                            activeTab = BottomTab.IMPORT,
                        )
                    }
                }

                is ParseResult.FallbackRequired -> {
                    val manualSchedule = buildManualSeedSchedule(result.reason)
                    _uiState.update { state ->
                        state.copy(
                            isParsing = false,
                            previewSchedule = manualSchedule,
                            importIssues = result.issues + ParseIssue(
                                level = IssueLevel.WARNING,
                                message = result.reason,
                            ),
                            message = "自动解析不完整，请手动补充后保存。",
                            activeTab = BottomTab.IMPORT,
                        )
                    }
                }

                is ParseResult.Failure -> {
                    val manualSchedule = buildManualSeedSchedule(result.reason)
                    _uiState.update { state ->
                        state.copy(
                            isParsing = false,
                            previewSchedule = manualSchedule,
                            importIssues = listOf(
                                ParseIssue(
                                    level = IssueLevel.ERROR,
                                    message = result.reason,
                                ),
                            ),
                            message = "自动解析失败，已进入手动编辑模式。",
                            activeTab = BottomTab.IMPORT,
                        )
                    }
                }
            }
        }
    }

    fun savePreviewSchedule(termStartEpochDay: Long?) {
        val preview = _uiState.value.previewSchedule ?: return
        if (termStartEpochDay == null || termStartEpochDay <= 0L) {
            _uiState.update { state ->
                state.copy(message = "请先设置开学日期后再保存课表。")
            }
            return
        }

        val previewWithTermStart = preview.withTermStartEpochDay(termStartEpochDay)
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            repository.saveSchedule(previewWithTermStart)
            _uiState.update { state ->
                state.copy(
                    isSaving = false,
                    previewSchedule = null,
                    importIssues = emptyList(),
                    activeTab = BottomTab.TIMETABLE,
                    message = "课表已保存。",
                )
            }
        }
    }

    fun updatePreviewCourse(course: CourseEntry) {
        val preview = _uiState.value.previewSchedule ?: return
        val updatedCourses = preview.courses.map { existing ->
            if (existing.id == course.id) course else existing
        }
        _uiState.update { state ->
            state.copy(previewSchedule = preview.copy(courses = updatedCourses))
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearPreview() {
        _uiState.update {
            it.copy(
                previewSchedule = null,
                importIssues = emptyList(),
                selectedFileName = null,
            )
        }
    }

    fun updatePersistedCourse(course: CourseEntry) {
        viewModelScope.launch {
            repository.updateCourse(course)
        }
    }

    fun addPersistedCourse(course: CourseEntry) {
        viewModelScope.launch {
            repository.addCourse(course)
        }
    }

    fun deletePersistedCourse(courseId: Long) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
        }
    }

    private fun buildManualSeedSchedule(reason: String): ParsedSchedule {
        return ParsedSchedule(
            meta = ScheduleMeta(
                termName = "手动课表",
                totalWeeks = 20,
                importedAt = System.currentTimeMillis(),
                templateVersion = "manual-seed-v1",
                sourceTag = "manual-fallback",
            ),
            courses = listOf(
                CourseEntry(
                    id = -1L,
                    name = "点击编辑，添加第一门课程",
                    teacher = "",
                    location = reason.take(48),
                    dayOfWeek = 1,
                    startPeriod = 1,
                    endPeriod = 2,
                    weekPattern = "all",
                    rawText = reason,
                    sourceConfidence = 0.2f,
                ),
            ),
        )
    }

    private fun ParsedSchedule.withTermStartEpochDay(termStartEpochDay: Long): ParsedSchedule {
        return copy(
            meta = meta.copy(termStartEpochDay = termStartEpochDay),
        )
    }
}
