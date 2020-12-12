package com.guiyec.similar

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
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

open class Repository<Output: Any>(
    val request: Request,
    var dispatcher: Dispatcher,
    private var transformBlock: ((String) -> Output)
) {
    constructor(serializer: KSerializer<Output>, json: Json, request: Request, dispatcher: Dispatcher) :
            this(request, dispatcher, { json.decodeFromString(serializer, it) })

    constructor(serializer: KSerializer<Output>, request: Request, dispatcher: Dispatcher) :
            this(serializer, Similar.defaultJson, request, dispatcher)

    constructor(serializer: KSerializer<Output>, json: Json, path: String, dispatcher: Dispatcher) :
            this(serializer, json, Request(path), dispatcher)

    constructor(serializer: KSerializer<Output>, path: String, dispatcher: Dispatcher) :
            this(serializer, Similar.defaultJson, Request(path), dispatcher)


    constructor(type: Type, gson: Gson, request: Request, dispatcher: Dispatcher) :
            this(request, dispatcher, { gson.fromJson<Output>(it, type) })

    constructor(type: Type, request: Request, dispatcher: Dispatcher) :
            this(type, Similar.defaultGson, request, dispatcher)

    constructor(type: Type, gson: Gson, path: String, dispatcher: Dispatcher) :
            this(Request(path), dispatcher, { gson.fromJson<Output>(it, type) })

    constructor(type: Type, path: String, dispatcher: Dispatcher) :
            this(type, Similar.defaultGson, path, dispatcher)


    constructor(type: KClass<Output>, gson: Gson, request: Request, dispatcher: Dispatcher) :
            this(type.java, gson, request, dispatcher)

    constructor(type: KClass<Output>, request: Request, dispatcher: Dispatcher) :
            this(type, Similar.defaultGson, request, dispatcher)

    constructor(type: KClass<Output>, gson: Gson, path: String, dispatcher: Dispatcher) :
            this(type.java, gson, Request(path), dispatcher)

    constructor(type: KClass<Output>, path: String, dispatcher: Dispatcher) :
            this(type, Similar.defaultGson, Request(path), dispatcher)


    var data: Output? = null
        set(value) {
            field = value
            updatedDate = if (value == null) null else Date()
        }
    var updatedDate: Date? = null
        internal set
    private var updateTask: Task<String>? = null
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
        updateTask = dispatcher.execute(request)
            .sink(this::handleData)
            .catch(this::handleError)
            .always { updateTask = null }
    }

    private fun handleData(data: String) {
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

    private fun handleError(error: RequestError) {
        val currentTasks = this.currentTasks.toList()
        this.currentTasks.clear()
        currentTasks.forEach { it.fail(error) }
    }

    fun <NewOutput: Any> map(mapBlock: (Output) -> NewOutput): Repository<NewOutput> {
        return Repository(request, dispatcher) { mapBlock(transformBlock(it)) }
    }

    fun sink(block: (Output) -> Unit): Repository<Output> = sink(null, block)

    fun sink(looper: Looper?, block: (Output) -> Unit): Repository<Output> {
        val previousTransformBlock = transformBlock
        val newBlock = if (looper == null) block else { output ->
            Handler(looper).post { block.invoke(output) }
        }
        transformBlock = {
            val output = previousTransformBlock(it)
            newBlock(output)
            output
        }
        return this
    }

    fun assign(property: KMutableProperty0<Output>): Repository<Output> = assign(property, null)

    fun assign(property: KMutableProperty0<Output>, looper: Looper?): Repository<Output> {
        return sink(looper) { property.set(it) }
    }

    fun <Root> assign(property: KMutableProperty1<Root, Output>, instance: Root): Repository<Output> = assign(property, instance, null)

    fun <Root> assign(property: KMutableProperty1<Root, Output>, instance: Root, looper: Looper?): Repository<Output> {
        return sink(looper) { property.set(instance, it) }
    }
}