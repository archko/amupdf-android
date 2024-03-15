package cn.archko.pdf.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * @author: archko 2024/3/14 :16:20
 */
@Composable
fun <T> rememberPreferenceState(
    key: String,
    defaultValue: T,
    flow: MutableStateFlow<Preferences> = LocalPreferenceFlow.current
): MutableState<T> {
    val valueFlow = remember(key, defaultValue, flow) { flow.map { it[key] ?: defaultValue } }
    return valueFlow.collectAsStateWithLifecycle(flow.value[key] ?: defaultValue).asMutable { value
        ->
        flow.value = flow.value.toMutablePreferences().apply { this[key] = value }
    }
}

private fun <T> State<T>.asMutable(setValue: (T) -> Unit): MutableState<T> =
    AsMutableState(this, setValue)

@Stable
private class AsMutableState<T>(private val state: State<T>, private val setValue: (T) -> Unit) :
    MutableState<T> {
    override var value: T
        get() = state.value
        set(value) {
            setValue(value)
        }

    override fun component1(): T = state.value

    override fun component2(): (T) -> Unit = setValue
}
