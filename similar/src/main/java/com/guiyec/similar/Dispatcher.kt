package com.guiyec.similar

interface Dispatcher {
    fun execute(request: Request): Task<String>
}

fun <Output> Task<Output>.then(dispatcher: Dispatcher, requestBlock: (Output) -> Request): Task<String> {
    return then {
        val request = requestBlock(it)
        dispatcher.execute(request)
    }
}