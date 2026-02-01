package ai.rever.boss.plugin.dynamic.gitstatus

import ai.rever.boss.plugin.api.DynamicPlugin
import ai.rever.boss.plugin.api.PluginContext

/**
 * Git Status dynamic plugin - Loaded from external JAR.
 *
 * View working tree status and staged changes
 */
class GitStatusDynamicPlugin : DynamicPlugin {
    override val pluginId: String = "ai.rever.boss.plugin.dynamic.gitstatus"
    override val displayName: String = "Git Status (Dynamic)"
    override val version: String = "1.0.0"
    override val description: String = "View working tree status and staged changes"
    override val author: String = "Risa Labs"
    override val url: String = "https://github.com/risa-labs-inc/boss-plugin-git-status"

    override fun register(context: PluginContext) {
        context.panelRegistry.registerPanel(GitStatusInfo) { ctx, panelInfo ->
            GitStatusComponent(ctx, panelInfo)
        }
    }
}
