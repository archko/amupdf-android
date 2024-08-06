package vn.chungha.flowbus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

object ViewModelAppProvider : ViewModelStoreOwner {
    private val eventViewModel: ViewModelStore = ViewModelStore()

    private val applicationProvider: ViewModelProvider by lazy {
        ViewModelProvider(
            ViewModelAppProvider,
            ViewModelProvider.AndroidViewModelFactory.getInstance(FlowBusInitApplication.application)
        )
    }

    fun <T : ViewModel> getApplicationScope(model: Class<T>): T = applicationProvider[model]
    override val viewModelStore: ViewModelStore
        get() = eventViewModel

}