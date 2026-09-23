package net.thunderbird.app.common.feature

import android.content.Context
import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import com.fsck.k9.ui.eink.EinkMailActivity

internal class MessageListLauncher(
    private val context: Context,
) : FeatureLauncherExternalContract.MessageListLauncher {
    override fun launch(accountUuid: String?) {
        if (accountUuid != null) {
            EinkMailActivity.launch(context, accountUuid)
        } else {
            EinkMailActivity.launch(context)
        }
    }
}
