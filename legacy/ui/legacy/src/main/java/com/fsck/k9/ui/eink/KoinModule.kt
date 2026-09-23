package com.fsck.k9.ui.eink

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val einkUiModule = module {
    viewModel {
        EinkViewModel(
            context = androidContext(),
            accountManager = get(),
            folderRepository = get(),
            messageListLiveDataFactory = get(),
            messagingController = get(),
            messageViewInfoExtractorFactory = get(),
            generalSettingsManager = get(),
        )
    }
}
