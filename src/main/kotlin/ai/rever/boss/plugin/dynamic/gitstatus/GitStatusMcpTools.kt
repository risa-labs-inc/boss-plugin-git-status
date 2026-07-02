package ai.rever.boss.plugin.dynamic.gitstatus

import ai.rever.boss.plugin.api.GitDataProvider
import ai.rever.boss.plugin.api.GitFileStatusTypeData
import ai.rever.boss.plugin.api.GitOperationResultData
import ai.rever.boss.plugin.api.McpToolArgs
import ai.rever.boss.plugin.api.McpToolDefinition
import ai.rever.boss.plugin.api.McpToolHandler
import ai.rever.boss.plugin.api.McpToolProvider
import ai.rever.boss.plugin.api.McpToolResult

/**
 * MCP tools contributed by the Git Status plugin.
 *
 * Registered in [GitStatusDynamicPlugin.register] via
 * `context.registerMcpToolProvider(...)`, so the `git_*` tools appear on the
 * `boss` MCP server while this plugin is active and are removed automatically
 * when it is disabled/unloaded. Each handler drives the same [GitDataProvider]
 * the panel UI uses.
 */
internal class GitStatusMcpToolProvider(
    override val providerId: String,
    private val gitDataProvider: GitDataProvider?,
) : McpToolProvider {

    override fun tools(): List<McpToolDefinition> = listOf(
        McpToolDefinition(
            name = "git_status",
            description = "Show the working-tree status (staged, unstaged, and untracked files) of " +
                "the current BOSS project, in a git-porcelain-style format (XY path).",
            handler = McpToolHandler { statusText() },
        ),
        McpToolDefinition(
            name = "git_stage",
            description = "Stage a file for commit in the current project.",
            inputSchema = PATH_SCHEMA,
            readOnly = false,
            handler = McpToolHandler { args -> pathOp(args) { gp, p -> gp.stage(p) } },
        ),
        McpToolDefinition(
            name = "git_unstage",
            description = "Unstage a previously staged file in the current project.",
            inputSchema = PATH_SCHEMA,
            readOnly = false,
            handler = McpToolHandler { args -> pathOp(args) { gp, p -> gp.unstage(p) } },
        ),
        McpToolDefinition(
            name = "git_stage_all",
            description = "Stage all changed files in the current project.",
            readOnly = false,
            handler = McpToolHandler { withProvider { it.stageAll() } },
        ),
        McpToolDefinition(
            name = "git_unstage_all",
            description = "Unstage all staged files in the current project.",
            readOnly = false,
            handler = McpToolHandler { withProvider { it.unstageAll() } },
        ),
        McpToolDefinition(
            name = "git_discard",
            description = "Discard working-tree changes to a file (irreversible). Use with care.",
            inputSchema = PATH_SCHEMA,
            readOnly = false,
            handler = McpToolHandler { args -> pathOp(args) { gp, p -> gp.discardChanges(p) } },
        ),
        McpToolDefinition(
            name = "git_checkout",
            description = "Checkout a commit, branch, or tag in the current project.",
            inputSchema = REF_SCHEMA,
            readOnly = false,
            handler = McpToolHandler { args ->
                val gp = gitDataProvider ?: return@McpToolHandler unavailable()
                val ref = args.string("ref")
                    ?: return@McpToolHandler McpToolResult("Missing required argument: ref", isError = true)
                toResult(gp.checkout(ref))
            },
        ),
    )

    private suspend fun statusText(): McpToolResult {
        val gp = gitDataProvider ?: return unavailable()
        gp.refreshStatus()
        val project = gp.getCurrentProjectPath() ?: "(no project selected)"
        if (!gp.isGitRepository.value) {
            return McpToolResult("Project: $project\nNot a git repository.")
        }
        val files = gp.fileStatus.value
        if (files.isEmpty()) return McpToolResult("Project: $project\nWorking tree clean.")
        val body = files.joinToString("\n") { f -> "${code(f.indexStatus)}${code(f.workTreeStatus)} ${f.path}" }
        return McpToolResult("Project: $project\n$body")
    }

    private suspend fun pathOp(
        args: McpToolArgs,
        op: suspend (GitDataProvider, String) -> GitOperationResultData,
    ): McpToolResult {
        val gp = gitDataProvider ?: return unavailable()
        val path = args.string("path")
            ?: return McpToolResult("Missing required argument: path", isError = true)
        return toResult(op(gp, path))
    }

    private suspend fun withProvider(op: suspend (GitDataProvider) -> GitOperationResultData): McpToolResult {
        val gp = gitDataProvider ?: return unavailable()
        return toResult(op(gp))
    }

    private fun toResult(result: GitOperationResultData): McpToolResult = when (result) {
        is GitOperationResultData.Success -> McpToolResult(result.message ?: "OK")
        is GitOperationResultData.Error -> McpToolResult(result.message, isError = true)
    }

    private fun unavailable(): McpToolResult =
        McpToolResult("Git data provider unavailable in this context.", isError = true)

    private fun code(type: GitFileStatusTypeData?): Char = when (type) {
        GitFileStatusTypeData.MODIFIED -> 'M'
        GitFileStatusTypeData.ADDED -> 'A'
        GitFileStatusTypeData.DELETED -> 'D'
        GitFileStatusTypeData.RENAMED -> 'R'
        GitFileStatusTypeData.COPIED -> 'C'
        GitFileStatusTypeData.UNTRACKED -> '?'
        GitFileStatusTypeData.IGNORED -> '!'
        GitFileStatusTypeData.UNMERGED -> 'U'
        null -> ' '
    }

    private companion object {
        const val PATH_SCHEMA =
            """{"type":"object","properties":{"path":{"type":"string","description":"File path (repo-relative or absolute)."}},"required":["path"]}"""
        const val REF_SCHEMA =
            """{"type":"object","properties":{"ref":{"type":"string","description":"Commit hash, branch name, or tag to checkout."}},"required":["ref"]}"""
    }
}
