package vn.chungha.flowbus

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

inline fun <reified T : Any> busEvent(
    valueBus: T,
    delayPost: Long = 0L
) = ViewModelAppProvider.getApplicationScope(FlowBusViewModel::class.java).busEvent(
    eventName = T::class.java.name,
    valuePost = valueBus,
    delayPost = delayPost
)

inline fun <reified T : Any> busEvent(
    scope: ViewModelStoreOwner,
    valueBus: T,
    delayPost: Long = 0L
) {
    ViewModelProvider(scope)[FlowBusViewModel::class.java].busEvent(
        eventName = T::class.java.name,
        valuePost = valueBus,
        delayPost = delayPost
    )
}