package vn.chungha.flowbus

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

//Should be listen MainThread,Follow by lifecycle owner
@MainThread
inline fun <reified T> LifecycleOwner.collectFlowBus(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    isSticky: Boolean = false,
    noinline onReceived: (T) -> Unit
): Job = ViewModelAppProvider.getApplicationScope(FlowBusViewModel::class.java).collectFlowBus(
    lifecycleOwner = this,
    eventName = T::class.java.name,
    minState = minActiveState,
    dispatcher = dispatcher,
    isSticky = isSticky,
    onReceived = onReceived
)

// Follow by Fragment Scope
inline fun <reified T> collectFlowBus(
    scope: Fragment,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    isSticky: Boolean = false,
    noinline onReceived: (T) -> Unit
) = ViewModelAppProvider.getApplicationScope(FlowBusViewModel::class.java).collectFlowBus(
    lifecycleOwner = scope,
    eventName = T::class.java.name,
    minState = minActiveState,
    dispatcher = dispatcher,
    isSticky = isSticky,
    onReceived = onReceived
)

//Follow by collect without lifecycle
@MainThread
inline fun <reified T> collectFlowBus(
    coroutineScope: CoroutineScope,
    isSticky: Boolean = false,
    noinline onReceived: (T) -> Unit
): Job {
    return coroutineScope.launch {
        ViewModelAppProvider.getApplicationScope(FlowBusViewModel::class.java)
            .collectWithoutLifecycle(
                T::class.java.name,
                isSticky,
                onReceived
            )
    }
}
