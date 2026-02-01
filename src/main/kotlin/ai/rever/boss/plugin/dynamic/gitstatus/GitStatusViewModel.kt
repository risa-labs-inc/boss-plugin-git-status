package ai.rever.boss.plugin.dynamic.gitstatus

import ai.rever.boss.plugin.api.GitDataProvider
import ai.rever.boss.plugin.api.GitFileStatusData
import ai.rever.boss.plugin.api.GitOperationResultData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Git Status panel.
 */
class GitStatusViewModel(
    private val gitDataProvider: GitDataProvider?,
    private val windowId: String = ""
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Status from provider
    val fileStatus: StateFlow<List<GitFileStatusData>> = gitDataProvider?.fileStatus
        ?: MutableStateFlow(emptyList())

    val isGitRepository: StateFlow<Boolean> = gitDataProvider?.isGitRepository
        ?: MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> = gitDataProvider?.isLoading
        ?: MutableStateFlow(false)

    // Local state for messages
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Selection state
    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val provider = gitDataProvider ?: return
        scope.launch {
            try {
                provider.refreshStatus()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh: ${e.message}"
            }
        }
    }

    fun stageFile(filePath: String) {
        val provider = gitDataProvider ?: return
        scope.launch {
            when (val result = provider.stage(filePath)) {
                is GitOperationResultData.Success -> {
                    _statusMessage.value = result.message ?: "Staged: $filePath"
                    provider.refreshStatus()
                }
                is GitOperationResultData.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun unstageFile(filePath: String) {
        val provider = gitDataProvider ?: return
        scope.launch {
            when (val result = provider.unstage(filePath)) {
                is GitOperationResultData.Success -> {
                    _statusMessage.value = result.message ?: "Unstaged: $filePath"
                    provider.refreshStatus()
                }
                is GitOperationResultData.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun stageAll() {
        val provider = gitDataProvider ?: return
        scope.launch {
            when (val result = provider.stageAll()) {
                is GitOperationResultData.Success -> {
                    _statusMessage.value = result.message ?: "Staged all files"
                    provider.refreshStatus()
                }
                is GitOperationResultData.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun unstageAll() {
        val provider = gitDataProvider ?: return
        scope.launch {
            when (val result = provider.unstageAll()) {
                is GitOperationResultData.Success -> {
                    _statusMessage.value = result.message ?: "Unstaged all files"
                    provider.refreshStatus()
                }
                is GitOperationResultData.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun discardChanges(filePath: String) {
        val provider = gitDataProvider ?: return
        scope.launch {
            when (val result = provider.discardChanges(filePath)) {
                is GitOperationResultData.Success -> {
                    _statusMessage.value = result.message ?: "Discarded changes: $filePath"
                    provider.refreshStatus()
                }
                is GitOperationResultData.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun openFile(filePath: String) {
        gitDataProvider?.openFile(filePath, windowId)
    }

    fun toggleFileSelection(filePath: String) {
        _selectedFiles.value = if (filePath in _selectedFiles.value) {
            _selectedFiles.value - filePath
        } else {
            _selectedFiles.value + filePath
        }
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }
}
