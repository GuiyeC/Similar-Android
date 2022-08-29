package com.guiyec.similar

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

fun <T: Any> KClass<T>.listType(): Type {
    return TypeToken.getParameterized(List::class.java, java).type
}

fun <T: Any> KSerializer<T>.list(): KSerializer<List<T>> {
    return ListSerializer(this)
}

open class Repository<Output: Any?>(
    private var taskBuilder: (() -> Task<Response>?),
    private var transformBlock: ((Response.Data) -> Output)
) {
    constructor(request: Request, dispatcher: Dispatcher, transformBlock: ((Response.Data) -> Output)) :
            this({ dispatcher.execute(request) }, transformBlock)

    constructor(path: String, dispatcher: Dispatcher, transformBlock: ((Response.Data) -> Output)) :
            this(Request(path), dispatcher, transformBlock)


    constructor(serializer: KSerializer<Output>, json: Json, taskBuilder: (() -> Task<Response>?)) :
            this(taskBuilder, { json.decodeFromString(serializer, it.string) })

    constructor(serializer: KSerializer<Output>, taskBuilder: (() -> Task<Response>?)) :
            this(serializer, Similar.defaultJson, taskBuilder)

    constructor(serializer: KSerializer<Output>, json: Json, request: Request, dispatcher: Dispatcher) :
            this({ dispatcher.execute(request) }, { json.decodeFromString(serializer, it.string) })

    constructor(serializer: KSerializer<Output>, request: Request, dispatcher: Dispatcher) :
            this(serializer, Similar.defaultJson, request, dispatcher)

    constructor(serializer: KSerializer<Output>, json: Json, path: String, dispatcher: Dispatcher) :
            this(serializer, json, Request(path), dispatcher)

    constructor(serializer: KSerializer<Output>, path: String, dispatcher: Dispatcher) :
            this(serializer, Similar.defaultJson, Request(path), dispatcher)


    constructor(type: Type, gson: Gson, request: Request, dispatcher: Dispatcher) :
            this({ dispatcher.execute(request) }, { gson.fromJson<Output>(it.string, type) })

    constructor(type: Type, request: Request, dispatcher: Dispatcher) :
            this(type, Similar.defaultGson, request, dispatcher)

    constructor(type: Type, gson: Gson, path: String, dispatcher: Dispatcher) :
            this(type, gson, Request(path), dispatcher)

    constructor(type: Type, path: String, dispatcher: Dispatcher) :
            this(type, Similar.defaultGson, Request(path), dispatcher)


    var data: Output? = null
        set(value) {
            field = value
            updatedDate = if (value == null) null else Date()
        }
    var updatedDate: Date? = null
        internal set
    private var updateTask: Task<Response>? = null
    private var currentTasks: MutableList<Task<Output>> = mutableListOf()

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

    private fun updateIfNecessary() {
        if (updateTask != null) return
        val task = taskBuilder() ?: run {
            Log.d("Repository", "Task not available, will not update")
            return
        }
        updateTask = task
            .sink(this::handleResponse)
            .catch(this::handleError)
            .always { updateTask = null }
    }

    private fun handleResponse(response: Response) {
        try {
            val parsedData = transformBlock(response.data)
            this.data = parsedData
            val currentTasks = this.currentTasks.toList()
            this.currentTasks.clear()
            currentTasks.forEach { it.complete(parsedData) }
        } catch (error: Exception) {
            handleError(RequestError.DecodingError(error))
        }
    }

    private fun handleError(error: RequestError) {
        val currentTasks = this.currentTasks.toList()
        this.currentTasks.clear()
        currentTasks.forEach { it.fail(error) }
    }

    fun <NewOutput: Any> map(mapBlock: (Output) -> NewOutput): Repository<NewOutput> {
        return Repository(taskBuilder) { mapBlock(transformBlock(it)) }
    }

    fun sink(block: (Output) -> Unit): Repository<Output> {
        val previousTransformBlock = transformBlock
        transformBlock = {
            val output = previousTransformBlock(it)
            block(output)
            output
        }
        return this
    }

    fun sink(looper: Looper, block: (Output) -> Unit): Repository<Output> {
        return sink { output ->
            Handler(looper).post { block.invoke(output) }
        }
    }

    fun sink(scope: CoroutineScope, block: suspend (Output) -> Unit): Repository<Output> {
        return sink { output ->
            scope.launch { block.invoke(output) }
        }
    }

    fun assign(property: KMutableProperty0<Output>) =
        sink { property.set(it) }

    fun assign(property: KMutableProperty0<Output>, looper: Looper) =
        sink(looper) { property.set(it) }

    fun assign(property: KMutableProperty0<Output>, scope: CoroutineScope) =
        sink(scope) { property.set(it) }

    fun <Root> assign(property: KMutableProperty1<Root, Output>, instance: Root) =
        sink { property.set(instance, it) }

    fun <Root> assign(property: KMutableProperty1<Root, Output>, instance: Root, looper: Looper) =
        sink(looper) { property.set(instance, it) }

    fun <Root> assign(property: KMutableProperty1<Root, Output>, instance: Root, scope: CoroutineScope) =
        sink(scope) { property.set(instance, it) }

    companion object {
        inline fun<reified Output: Any?> build(json: Json, request: Request, dispatcher: Dispatcher): Repository<Output> {
            return Repository(serializer(), json, request, dispatcher)
        }

        inline fun<reified Output: Any?> build(request: Request, dispatcher: Dispatcher): Repository<Output> {
            return Repository(serializer(), request, dispatcher)
        }

        inline fun<reified Output: Any?> build(json: Json, path: String, dispatcher: Dispatcher): Repository<Output> {
            return Repository(serializer(), json, Request(path), dispatcher)
        }

        inline fun<reified Output: Any?> build(path: String, dispatcher: Dispatcher): Repository<Output> {
            return Repository(serializer(), Similar.defaultJson, Request(path), dispatcher)
        }
    }
}