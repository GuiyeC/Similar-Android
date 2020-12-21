package com.guiyec.similar

interface Dispatcher {
    fun execute(request: Request): Task<Response>
}

fun <Output> Task<Output>.then(dispatcher: Dispatcher, requestBlock: (Output) -> Request): Task<Response> {
    return then {
        val request = requestBlock(it)
        dispatcher.execute(request)
    }
}