package com.guiyec.similar

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.lang.Exception
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

enum class TaskState {
    Alive,
    Completed,
    Failed,
    Cancelled
}

internal data class TaskBlock<Output>(
    val outputBlock: ((Output) -> Unit)? = null,
    val errorBlock: ((RequestError) -> Unit)? = null,
    val alwaysBlock: (() -> Unit)? = null
) {
    fun invoke(output: Output) {
        if (outputBlock != null) {
            outputBlock.invoke(output)
        } else {
            alwaysBlock?.invoke()
        }
    }

    fun invoke(error: RequestError) {
        if (errorBlock != null) {
            errorBlock.invoke(error)
        } else {
            alwaysBlock?.invoke()
        }
    }
}

class Task<Output> {
    var state: TaskState = TaskState.Alive
        private set
    private var output: Output? = null
    private var error: RequestError? = null
    var progress: Double? = null
        set(value) {
            if (field == value) return
            field = value
            value?.let { progressBlock?.invoke(it) }
        }
    internal val blocks: MutableList<TaskBlock<Output>> = mutableListOf()
    internal var progressBlock: ((Double) -> Unit)? = null
    var cancelBlock: (() -> Unit)? = null

    constructor()

    constructor(output: Output): this() {
        complete(output)
    }

    constructor(error: RequestError): this() {
        fail(error)
    }

    fun complete(output: Output) {
        if (state == TaskState.Cancelled) return
        if (state != TaskState.Alive) throw RuntimeException("Invalid state: $state")
        this.output = output
        this.state = TaskState.Completed
        blocks.forEach { it.invoke(output = output) }
        clearBlocks()
    }

    fun fail(error: RequestError) {
        if (state == TaskState.Cancelled) return
        if (state != TaskState.Alive) throw RuntimeException("Invalid state: $state")
        this.error = error
        this.state = TaskState.Failed
        blocks.forEach { it.invoke(error = error) }
        clearBlocks()
    }

    internal fun clearBlocks() {
        blocks.clear()
        cancelBlock = null
    }

    fun progress(block: ((Double) -> Unit)): Task<Output> = progress(null, block)

    fun progress(looper: Looper?, block: ((Double) -> Unit)): Task<Output> {
        if (state != TaskState.Alive) return this
        val newBlock: ((Double) -> Unit) = if (looper == null) { block } else {
            { Handler(looper).post { block.invoke(it) } }
        }
        val previousBlock = progressBlock
        if (previousBlock != null) {
            progressBlock = {
                previousBlock.invoke(it)
                newBlock.invoke(it)
            }
        } else {
            progressBlock = newBlock
        }
        return this
    }

    // Sink

    fun sink(block: ((Output) -> Unit)): Task<Output> {
        if (state == TaskState.Failed || state == TaskState.Cancelled) return this
        val output = output
        if (output != null) {
            block.invoke(output)
        } else {
            blocks.add(TaskBlock(outputBlock = block))
        }
        return this
    }

    fun sink(looper: Looper, block: ((Output) -> Unit)): Task<Output> {
        return sink { output ->
            Handler(looper).post { block.invoke(output) }
        }
    }

    fun sink(scope: CoroutineScope, block: suspend ((Output) -> Unit)): Task<Output> {
        return sink { output ->
            scope.launch { block.invoke(output) }
        }
    }

    // Catch

    fun catch(block: ((RequestError) -> Unit)): Task<Output> {
        if (state == TaskState.Completed || state == TaskState.Cancelled) return this
        val error = error
        if (error != null) {
            block.invoke(error)
        } else {
            blocks.add(TaskBlock(errorBlock = block))
        }
        return this
    }

    fun catch(looper: Looper, block: ((RequestError) -> Unit)): Task<Output> {
        return catch { error ->
            Handler(looper).post { block.invoke(error) }
        }
    }

    fun catch(scope: CoroutineScope, block: suspend ((RequestError) -> Unit)): Task<Output> {
        return catch { error ->
            scope.launch { block.invoke(error) }
        }
    }

    // Always

    fun always(block: (() -> Unit)): Task<Output> {
        if (state == TaskState.Cancelled) return this
        if (output != null || error != null) {
            block.invoke()
        } else {
            blocks.add(TaskBlock(alwaysBlock = block))
        }
        return this
    }

    fun always(looper: Looper, block: (() -> Unit)): Task<Output> {
        return always {
            Handler(looper).post(block)
        }
    }

    fun always(scope: CoroutineScope, block: suspend (() -> Unit)): Task<Output> {
        return always {
            scope.launch { block() }
        }
    }

    fun cancel() {
        if (state != TaskState.Alive) {
            Log.i("Task", "Task could not be cancelled, task state: $state)")
            return
        }
        state = TaskState.Cancelled
        cancelBlock?.invoke()
        clearBlocks()
    }

    fun <T> wrap(sinkBlock: ((Output, Task<T>) -> Unit),
                 catchBlock: ((RequestError, Task<T>) -> Unit) = { error, task -> task.fail(error) }): Task<T> {
        val task = Task<T>()
        sink { sinkBlock(it, task) }
        `catch` { catchBlock(it, task) }
        task.cancelBlock = cancelBlock
        task.progressBlock = progressBlock
        progressBlock = { task.progress = it }
        return task
    }

    fun guard(guardBlock: (Output) -> Boolean, errorBlock: (Output) -> Error): Task<Output> {
        return wrap({ data, task ->
            if (guardBlock(data)) {
                task.complete(data)
            } else {
                val error = errorBlock(data)
                if (error is RequestError) {
                    task.fail(error)
                } else {
                    task.fail(RequestError.LocalError(error))
                }
            }
        })
    }

    fun guard(guardBlock: (Output) -> Boolean, error: Error): Task<Output> {
        return guard(guardBlock, { error })
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

    fun <NewOutput> then(taskBlock: (Output) -> Task<NewOutput>): Task<NewOutput> {
        return wrap({ data, task ->
            val newTask = taskBlock(data)
                .sink(task::complete)
                .catch(task::fail)
            val oldCancelBlock = newTask.cancelBlock
            newTask.cancelBlock = {
                oldCancelBlock?.invoke()
                task.cancel()
            }
        })
    }

    fun <NewOutput> map(block: (Output) -> NewOutput): Task<NewOutput> {
        return wrap({ data, task ->
            val newData = block(data)
            task.complete(newData)
        })
    }

    fun print(): Task<Output> {
        sink { Log.i("Task", it.toString()) }
        `catch`{ it.printStackTrace() }
        return this
    }

    fun eraseType(): Task<Unit> {
        return wrap({ _, task -> task.complete(Unit) })
    }

    fun <Error: Any> catch(serializer: KSerializer<Error>, json: Json = Similar.defaultJson, block: ((Int, Error) -> Unit)): Task<Output> {
        return catch {
            if (it is RequestError.ServerError && !it.response.data.isEmpty()) {
                try {
                    val decodedError = json.decodeFromString(serializer, it.response.data.string)
                    block.invoke(it.code, decodedError)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    fun <Error: Any> catch(targetClass: KClass<Error>, block: ((Int, Error) -> Unit)): Task<Output> {
        return catch(targetClass, Similar.defaultGson, block)
    }

    fun <Error: Any> catch(targetClass: KClass<Error>, gson: Gson, block: ((Int, Error) -> Unit)): Task<Output> {
        return catch(targetClass.java, gson, block)
    }

    fun <Error: Any> catch(type: Type, block: ((Int, Error) -> Unit)): Task<Output> {
        return catch(type, Similar.defaultGson, block)
    }

    fun <Error: Any> catch(type: Type, gson: Gson = Similar.defaultGson, block: ((Int, Error) -> Unit)): Task<Output> {
        return catch {
            if (it is RequestError.ServerError && !it.response.data.isEmpty()) {
                try {
                    val decodedError = gson.fromJson<Error>(it.response.data.string, type)
                    block.invoke(it.code, decodedError)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }
}

fun <Output: Any> Task<Output?>.ignoreNull(): Task<Output> {
    return wrap(sinkBlock = { output, task ->
        output?.let { task.complete(it) }
    })
}

inline fun <reified NewOutput: Any> Task<Response>.decode(json: Json = Similar.defaultJson): Task<NewOutput> {
    return decode(serializer(), json = json)
}

fun <NewOutput: Any> Task<Response>.decode(serializer: KSerializer<NewOutput>, json: Json = Similar.defaultJson): Task<NewOutput> {
    return wrap({ response, task ->
        try {
            val entity = json.decodeFromString(serializer, response.data.string)
            task.complete(entity)
        } catch (e: Exception) {
            task.fail(RequestError.DecodingError(e))
        }
    })
}

fun <NewOutput: Any> Task<Response>.decode(targetClass: KClass<NewOutput>, gson: Gson = Similar.defaultGson): Task<NewOutput> {
    return decode(targetClass.java, gson)
}

fun <NewOutput: Any> Task<Response>.decode(type: Type, gson: Gson = Similar.defaultGson): Task<NewOutput> {
    return wrap({ response, task ->
        try {
            val entity = gson.fromJson<NewOutput>(response.data.string, type)
            task.complete(entity)
        } catch (e: Exception) {
            task.fail(RequestError.DecodingError(e))
        }
    })
}

