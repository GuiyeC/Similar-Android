package com.guiyec.similar

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass

fun <T: Any> KClass<T>.listType(): Type {
    return TypeToken.getParameterized(List::class.java, java).type
}

open class Repository<Output: Any>(
    val request: Request,
    var dispatcher: Dispatcher,
    val transformBlock: ((String) -> Output)
) {
    constructor(type: Type, path: String, gson: Gson = Similar.defaultGson, dispatcher: Dispatcher) :
            this(Request(path), dispatcher, { gson.fromJson<Output>(it, type) })

    constructor(type: KClass<Output>, path: String, gson: Gson = Similar.defaultGson, dispatcher: Dispatcher) :
            this(type.java, path, gson, dispatcher)

    constructor(type: Type, request: Request, gson: Gson = Similar.defaultGson, dispatcher: Dispatcher) :
            this(request, dispatcher, { gson.fromJson<Output>(it, type) })

    constructor(type: KClass<Output>, request: Request, gson: Gson = Similar.defaultGson, dispatcher: Dispatcher) :
            this(type.java, request, gson, dispatcher)

    var data: Output? = null
        set(value) {
            field = value
            updatedDate = if (value == null) null else Date()
        }
    var updatedDate: Date? = null
    var updateTask: Task<String>? = null
    var currentTasks: MutableList<Task<Output>> = mutableListOf()

    fun fetch(forceUpdate: Boolean = false): Task<Output> {
        if (forceUpdate) {
            data = null
        }
        // Check if data is ready, check expiration?
        data?.let {
            return Task(it)
        }
        // Save task
        val task = Task<Output>()
        currentTasks.add(task)
        task.cancelBlock = {
            currentTasks.remove(task)
        }
        updateIfNecessary()
        return task
    }

    fun updateIfNecessary() {
        if (updateTask != null) return
        updateTask = dispatcher.execute(request)
            .sink(this::handleData)
            .catch(this::handleError)
    }

    fun handleData(data: String) {
        try {
            val parsedData = transformBlock(data)
            this.data = parsedData
            val currentTasks = this.currentTasks.toList()
            this.currentTasks.clear()
            currentTasks.forEach { it.complete(parsedData) }
        } catch (error: Exception) {
            handleError(RequestError.DecodingError(error))
        }
    }

    fun handleError(error: RequestError) {
        val currentTasks = this.currentTasks.toList()
        this.currentTasks.clear()
        currentTasks.forEach { it.fail(error) }
    }

    fun <NewOutput: Any> map(mapBlock: (Output) -> NewOutput): Repository<NewOutput> {
        return Repository(request, dispatcher) { mapBlock(transformBlock(it)) }
    }

    fun sink(sinkBlock: (Output) -> Unit): Repository<Output> {
        return Repository(request, dispatcher) {
            val output = transformBlock(it)
            sinkBlock(output)
            output
        }
    }
}