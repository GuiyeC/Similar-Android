package com.guiyec.similar

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class Task<Output> {
    var isCancelled: Boolean = false
        private set
    var cancelBlock: (() -> Unit)? = null
    private var alwaysBlock: (() -> Unit)? = null
        set(value) {
            field = value
            if (isCancelled) return
            if (output != null || error != null) {
                alwaysBlock?.invoke()
            }
    }
    private var output: Output? = null
        set(value) {
            if (field != null || error != null || value == null) {
                throw RuntimeException("Invalid output value")
            }
            field = value
            if (isCancelled) return
            completionBlock?.invoke(value)
            alwaysBlock?.invoke()
        }
    private var completionBlock: ((Output) -> Unit)? = null
        set(value) {
            field = value
            if (isCancelled) return
            output?.let { completionBlock?.invoke(it) }
        }
    private var error: RequestError? = null
        set(value) {
            if (field != null || output != null || value == null) {
                throw RuntimeException("Invalid output value")
            }
            field = value
            if (isCancelled) return
            errorBlock?.invoke(value)
            alwaysBlock?.invoke()
        }

    private var errorBlock: ((RequestError) -> Unit)? = null
        set(value) {
            field = value
            if (isCancelled) return
            error?.let { errorBlock?.invoke(it) }
        }

    constructor()

    constructor(output: Output): this() {
        complete(output)
    }

    constructor(error: RequestError): this() {
        fail(error)
    }

    fun complete(output: Output) {
        this.output = output
    }

    fun fail(error: RequestError) {
        this.error = error
    }

    fun sink(block: ((Output) -> Unit)): Task<Output> = sink(null, block)

    fun sink(looper: Looper?, block: ((Output) -> Unit)): Task<Output> {
        val previousSinkBlock = completionBlock
        val newBlock = if (looper == null) block else { output ->
            Handler(looper).post { block.invoke(output) }
        }
        completionBlock = {
            previousSinkBlock?.invoke(it)
            newBlock(it)
        }
        return this
    }

    fun catch(block: ((RequestError) -> Unit)): Task<Output> = catch(null, block)

    fun catch(looper: Looper?, block: ((RequestError) -> Unit)): Task<Output> {
        val previousErrorBlock = errorBlock
        val newBlock = if (looper == null) block else { error ->
            Handler(looper).post { block.invoke(error) }
        }
        errorBlock = {
            previousErrorBlock?.invoke(it)
            newBlock(it)
        }
        return this
    }

    fun always(block: (() -> Unit)): Task<Output> = always(null, block)

    fun always(looper: Looper?, block: (() -> Unit)): Task<Output> {
        val previousAlwaysBlock = this.alwaysBlock
        val newBlock = if (looper == null) block else { ->
            Handler(looper).post(block)
        }
        this.alwaysBlock = {
            previousAlwaysBlock?.invoke()
            newBlock()
        }
        return this
    }

    fun cancel() {
        if (isCancelled) return
        isCancelled = true
        cancelBlock?.invoke()
    }

    fun <T> wrap(sinkBlock: ((Output, Task<T>) -> Unit),
                 catchBlock: ((RequestError, Task<T>) -> Unit) = { error, task -> task.fail(error) }): Task<T> {
        val task = Task<T>()
        sink { sinkBlock(it, task) }
        `catch` { catchBlock(it, task) }
        task.cancelBlock = this::cancel
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

    fun assign(property: KMutableProperty0<Output>): Task<Output> = assign(property, null)

    fun assign(property: KMutableProperty0<Output>, looper: Looper?): Task<Output> {
        return sink(looper) { property.set(it) }
    }

    fun <Root> assign(property: KMutableProperty1<Root, Output>, instance: Root): Task<Output> = assign(property, instance, null)

    fun <Root> assign(property: KMutableProperty1<Root, Output>, instance: Root, looper: Looper?): Task<Output> {
        return sink(looper) { property.set(instance, it) }
    }

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
            if (it is RequestError.ServerError && it.data != null) {
                val decodedError = json.decodeFromString(serializer, it.data)
                block.invoke(it.code, decodedError)
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
            if (it is RequestError.ServerError && it.data != null) {
                val decodedError = gson.fromJson<Error>(it.data, type)
                block.invoke(it.code, decodedError)
            }
        }
    }
}

fun <NewOutput: Any> Task<Response>.decode(serializer: KSerializer<NewOutput>, json: Json = Similar.defaultJson): Task<NewOutput> {
    return wrap({ response, task ->
        val entity = json.decodeFromString(serializer, response.data)
        Log.i("Task Decode", entity.toString())
        task.complete(entity)
    })
}

fun <NewOutput: Any> Task<Response>.decode(targetClass: KClass<NewOutput>, gson: Gson = Similar.defaultGson): Task<NewOutput> {
    return decode(targetClass.java, gson)
}

fun <NewOutput: Any> Task<Response>.decode(type: Type, gson: Gson = Similar.defaultGson): Task<NewOutput> {
    return wrap({ response, task ->
        val entity = gson.fromJson<NewOutput>(response.data, type)
        Log.i("Task Decode", entity.toString())
        task.complete(entity)
    })
}

