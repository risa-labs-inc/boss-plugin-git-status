package ai.rever.boss.plugin.dynamic.gitstatus

import ai.rever.boss.plugin.api.Panel.Companion.left
import ai.rever.boss.plugin.api.Panel.Companion.bottom
import ai.rever.boss.plugin.api.PanelId
import ai.rever.boss.plugin.api.PanelInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Difference

object GitStatusInfo : PanelInfo {
    override val id = PanelId("git-status", 14)
    override val displayName = "Git Status"
    override val icon = Icons.Outlined.Difference
    override val defaultSlotPosition = left.bottom
}
