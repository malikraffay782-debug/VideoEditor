package com.videoeditor.project

import com.videoeditor.core.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory project store for Phase 2.
 * Room persistence will be added in a later phase without changing this API.
 */
class ProjectRepository {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _current = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _current.asStateFlow()

    fun createProject(name: String): Project {
        val project = Project(name = name)
        _projects.value = _projects.value + project
        _current.value = project
        return project
    }

    fun openProject(id: String): Project? {
        val p = _projects.value.find { it.id == id }
        _current.value = p
        return p
    }

    fun saveProject(project: Project) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        _projects.value = _projects.value.map {
            if (it.id == updated.id) updated else it
        }
        if (_current.value?.id == updated.id) {
            _current.value = updated
        }
    }

    fun deleteProject(id: String) {
        _projects.value = _projects.value.filter { it.id != id }
        if (_current.value?.id == id) {
            _current.value = null
        }
    }

    fun setCurrent(project: Project) {
        _current.value = project
        // also ensure it is in the list
        if (_projects.value.none { it.id == project.id }) {
            _projects.value = _projects.value + project
        } else {
            saveProject(project)
        }
    }
}
