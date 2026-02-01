package ai.rever.boss.plugin.dynamic.gitstatus

import ai.rever.boss.plugin.api.DynamicPlugin
import ai.rever.boss.plugin.api.GitDataProvider
import ai.rever.boss.plugin.api.PluginContext

/**
 * Git Status dynamic plugin - Loaded from external JAR.
 *
 * View working tree status and staged changes with full functionality.
 */
class GitStatusDynamicPlugin : DynamicPlugin {
    override val pluginId: String = "ai.rever.boss.plugin.dynamic.gitstatus"
    override val displayName: String = "Git Status (Dynamic)"
    override val version: String = "1.0.1"
    override val description: String = "View working tree status and staged changes"
    override val author: String = "Risa Labs"
    override val url: String = "https://github.com/risa-labs-inc/boss-plugin-git-status"

    private var gitDataProvider: GitDataProvider? = null

    override fun register(context: PluginContext) {
        gitDataProvider = context.gitDataProvider
        context.panelRegistry.registerPanel(GitStatusInfo) { ctx, panelInfo ->
            GitStatusComponent(ctx, panelInfo, gitDataProvider)
        }
    }
}
