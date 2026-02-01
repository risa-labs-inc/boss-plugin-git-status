package ai.rever.boss.plugin.dynamic.gitstatus

import ai.rever.boss.plugin.api.GitDataProvider
import ai.rever.boss.plugin.api.GitFileStatusData
import ai.rever.boss.plugin.api.GitFileStatusTypeData
import ai.rever.boss.plugin.scrollbar.getPanelScrollbarConfig
import ai.rever.boss.plugin.scrollbar.lazyListScrollbar
import ai.rever.boss.plugin.ui.BossTheme
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun GitStatusContent(
    viewModel: GitStatusViewModel,
    gitDataProvider: GitDataProvider?
) {
    BossTheme {
        if (gitDataProvider == null) {
            NoGitProviderMessage()
        } else {
            GitStatusPanel(viewModel)
        }
    }
}

@Composable
private fun NoGitProviderMessage() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Difference,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Git Status",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Git data provider not available",
                fontSize = 13.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun GitStatusPanel(viewModel: GitStatusViewModel) {
    val fileStatus by viewModel.fileStatus.collectAsState()
    val isGitRepo by viewModel.isGitRepository.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Toolbar
            GitStatusToolbar(
                isLoading = isLoading,
                hasFiles = fileStatus.isNotEmpty(),
                onRefresh = { viewModel.refreshStatus() },
                onStageAll = { viewModel.stageAll() },
                onUnstageAll = { viewModel.unstageAll() }
            )

            Divider(color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f))

            // Toast messages
            AnimatedVisibility(
                visible = statusMessage != null || errorMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                ToastMessage(
                    statusMessage = statusMessage,
                    errorMessage = errorMessage,
                    onDismiss = { viewModel.clearMessages() }
                )
            }

            // Content
            when {
                !isGitRepo -> NotAGitRepoMessage()
                fileStatus.isEmpty() && !isLoading -> CleanWorkingTreeMessage()
                else -> FileStatusList(
                    files = fileStatus,
                    onStage = { viewModel.stageFile(it) },
                    onUnstage = { viewModel.unstageFile(it) },
                    onDiscard = { viewModel.discardChanges(it) },
                    onOpenFile = { viewModel.openFile(it) }
                )
            }
        }
    }
}

@Composable
private fun GitStatusToolbar(
    isLoading: Boolean,
    hasFiles: Boolean,
    onRefresh: () -> Unit,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Changes",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface
        )

        Spacer(modifier = Modifier.weight(1f))

        if (hasFiles) {
            IconButton(
                onClick = onStageAll,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Stage All",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            IconButton(
                onClick = onUnstageAll,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Unstage All",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(24.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colors.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ToastMessage(
    statusMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    LaunchedEffect(statusMessage, errorMessage) {
        delay(3000)
        onDismiss()
    }

    val isError = errorMessage != null
    val message = errorMessage ?: statusMessage ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isError) Color(0xFF5D3A3A) else Color(0xFF3A5D3A))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            fontSize = 11.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                modifier = Modifier.size(12.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun NotAGitRepoMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colors.onBackground.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Not a Git repository",
                fontSize = 13.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CleanWorkingTreeMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Working tree clean",
                fontSize = 13.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun FileStatusList(
    files: List<GitFileStatusData>,
    onStage: (String) -> Unit,
    onUnstage: (String) -> Unit,
    onDiscard: (String) -> Unit,
    onOpenFile: (String) -> Unit
) {
    val stagedFiles = files.filter { it.isStaged }
    val unstagedFiles = files.filter { it.isUnstaged || (!it.isStaged && !it.isUnstaged) }

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .lazyListScrollbar(
                    listState = listState,
                    direction = Orientation.Vertical,
                    config = getPanelScrollbarConfig()
                )
        ) {
            // Staged section
            if (stagedFiles.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Staged Changes",
                        count = stagedFiles.size,
                        color = Color(0xFF4CAF50)
                    )
                }
                items(stagedFiles, key = { "staged-${it.path}" }) { file ->
                    FileStatusRow(
                        file = file,
                        isStaged = true,
                        onToggleStage = { onUnstage(file.path) },
                        onDiscard = { onDiscard(file.path) },
                        onClick = { onOpenFile(file.path) }
                    )
                }
            }

            // Unstaged section
            if (unstagedFiles.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Changes",
                        count = unstagedFiles.size,
                        color = Color(0xFFF57C00)
                    )
                }
                items(unstagedFiles, key = { "unstaged-${it.path}" }) { file ->
                    FileStatusRow(
                        file = file,
                        isStaged = false,
                        onToggleStage = { onStage(file.path) },
                        onDiscard = { onDiscard(file.path) },
                        onClick = { onOpenFile(file.path) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "($count)",
            fontSize = 11.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun FileStatusRow(
    file: GitFileStatusData,
    isStaged: Boolean,
    onToggleStage: () -> Unit,
    onDiscard: () -> Unit,
    onClick: () -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        FileStatusIndicator(
            indexStatus = file.indexStatus,
            workTreeStatus = file.workTreeStatus,
            isStaged = isStaged
        )

        Spacer(modifier = Modifier.width(8.dp))

        // File path
        val fileName = file.path.substringAfterLast("/")
        val dirPath = file.path.substringBeforeLast("/", "")

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (dirPath.isNotEmpty()) {
                Text(
                    text = dirPath,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Actions
        Row {
            // Stage/Unstage button
            IconButton(
                onClick = onToggleStage,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isStaged) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = if (isStaged) "Unstage" else "Stage",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            // Discard button (only for unstaged)
            if (!isStaged && file.workTreeStatus != GitFileStatusTypeData.UNTRACKED) {
                IconButton(
                    onClick = { showDiscardDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Discard Changes",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFEF5350)
                    )
                }
            }
        }
    }

    // Discard confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = {
                Text(
                    "Discard changes to ${file.path.substringAfterLast("/")}?\n\n" +
                    "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDiscard()
                        showDiscardDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFEF5350)
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun FileStatusIndicator(
    indexStatus: GitFileStatusTypeData?,
    workTreeStatus: GitFileStatusTypeData?,
    isStaged: Boolean
) {
    val status = if (isStaged) indexStatus else workTreeStatus

    val (color, letter) = when (status) {
        GitFileStatusTypeData.MODIFIED -> Color(0xFFF57C00) to "M"
        GitFileStatusTypeData.ADDED -> Color(0xFF4CAF50) to "A"
        GitFileStatusTypeData.DELETED -> Color(0xFFEF5350) to "D"
        GitFileStatusTypeData.RENAMED -> Color(0xFF9C27B0) to "R"
        GitFileStatusTypeData.COPIED -> Color(0xFF00BCD4) to "C"
        GitFileStatusTypeData.UNTRACKED -> Color(0xFF9E9E9E) to "?"
        GitFileStatusTypeData.IGNORED -> Color(0xFF757575) to "!"
        GitFileStatusTypeData.UNMERGED -> Color(0xFFFF5722) to "U"
        null -> Color(0xFF9E9E9E) to "?"
    }

    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}
