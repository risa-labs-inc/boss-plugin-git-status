package ai.rever.boss.plugin.dynamic.gitstatus

import ai.rever.boss.plugin.api.GitDataProvider
import ai.rever.boss.plugin.api.PanelComponentWithUI
import ai.rever.boss.plugin.api.PanelInfo
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext

/**
 * Git Status panel component (Dynamic Plugin)
 *
 * Full implementation using GitDataProvider from PluginContext.
 */
class GitStatusComponent(
    ctx: ComponentContext,
    override val panelInfo: PanelInfo,
    private val gitDataProvider: GitDataProvider?
) : PanelComponentWithUI, ComponentContext by ctx {

    private val viewModel = GitStatusViewModel(gitDataProvider)

    @Composable
    override fun Content() {
        GitStatusContent(viewModel, gitDataProvider)
    }
}
