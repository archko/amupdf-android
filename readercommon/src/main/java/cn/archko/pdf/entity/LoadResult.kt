package cn.archko.pdf.entity

data class LoadResult<Data : Any, Value : Any>(
    var state: State,
    var obj: Data? = null,
    var list: List<Value>? = listOf(),
    var prevKey: Int? = 0,
    var nextKey: Int? = 0,
) {
    /*fun copyResult(
        state: State = this.state,
        obj: Data? = this.obj,
        list: List<Value>? = this.list,
        prevKey: Int? = this.prevKey,
        nextKey: Int? = this.nextKey,
    ) = LoadResult(
        state = state,
        obj = obj,
        list = list,
        prevKey = prevKey,
        nextKey = nextKey,
    )*/
}

sealed class State {

    object INIT : State()
    object LOADING : State()
    object FINISHED : State()
    object ERROR : State()
    object PASS : State()
}