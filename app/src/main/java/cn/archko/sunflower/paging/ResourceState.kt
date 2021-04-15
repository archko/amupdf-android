package cn.archko.sunflower.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * @author: archko 2021/4/4 :9:35 下午
 */
class ResourceState(initial: Int = IDLE) {

    var value: Int by mutableStateOf(initial, structuralEqualityPolicy())
        public set

    companion object {

        val INIT = 0
        val IDLE = 1
        val LOADING = 2
        val FINISHED = 3
        val ERROR = -1

        val Saver: Saver<ResourceState, *> = Saver(
            save = { it.value },
            restore = { ResourceState(it) }
        )
    }
}

@Composable
fun rememberResourceState(initial: Int = ResourceState.IDLE): ResourceState {
    return rememberSaveable(saver = ResourceState.Saver) {
        ResourceState(initial)
    }
}