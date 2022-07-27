package com.guiyec.similar

import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import okhttp3.ResponseBody
import okhttp3.OkHttpClient
import okio.*
import okio.BufferedSink
import kotlin.math.max
import kotlin.math.min

open class NetworkDispatcher: Dispatcher {
    private var tasks: MutableMap<okhttp3.Request, Task<Response>> = mutableMapOf()
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(Interceptor { chain: Interceptor.Chain ->
            val task = tasks.remove(chain.call().request())
            val originalResponse = chain.proceed(chain.request())
            val responseBody = originalResponse.body
            if (task == null || responseBody == null)
                originalResponse
            else
                originalResponse.newBuilder()
                    .body(ProgressResponseBody(responseBody, task))
                    .build()
        })
        .build()

    override fun execute(request: Request): Task<Response> {
        val urlBuilder = request.path.toHttpUrlOrNull()?.newBuilder()
        require(urlBuilder != null) { "Invalid url" }
        request.parameters?.forEach {
            urlBuilder.addQueryParameter(it.key, it.value)
        }
        val requestBuilder = okhttp3.Request.Builder()
            .url(urlBuilder.build())
        request.headers?.forEach {
            requestBuilder.addHeader(it.key, it.value)
        }
        requestBuilder.addHeader("Accept", "application/json")
        val task = Task<Response>()
        requestBuilder.setData(request.method, request.data, task)
        val okHttpRequest = requestBuilder.build()
        if (request.data !is Request.Data.Multipart) {
            tasks[okHttpRequest] = task
        }
        val call = client.newCall(okHttpRequest)
        task.cancelBlock = { call.cancel() }
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                tasks.remove(okHttpRequest)
                if (response.code !in request.expectedCode) {
                    val responseBody = response.body?.string()
                    val similarResponse: Response = if (responseBody == null) Response(
                        data = ResponseData(byteArrayOf()),
                        statusCode = response.code,
                        headers = response.headers.toMap()
                    ) else Response(response)
                    task.fail(RequestError.ServerError(response.code, similarResponse))
                    return
                }
                if (response.body?.contentLength() == null) {
                    task.fail(RequestError.NoData)
                    return
                }
                task.complete(Response(response))
            }

            override fun onFailure(call: Call, e: IOException) {
                tasks.remove(okHttpRequest)
                e.printStackTrace()
                task.fail(RequestError.LocalError(e))
            }
        })
        return task
    }
}

fun okhttp3.Request.Builder.setData(method: HttpMethod, data: Request.Data?, task: Task<Response>) {
    when (data) {
        is Request.Data.Json<*> -> {
            val jsonBody = data.jsonString
            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            method(method.value, body)
            addHeader("Content-Type", "application/json")
        }
        is Request.Data.Multipart -> {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            data.parts.forEach { builder.addFormDataPart(it.name, it.fileName, it.data) }
            val body = builder.build()
            method(method.value, ProgressRequestBody(body, task))
        }
        null -> method(method.value, null)
    }
}

private class ProgressRequestBody(
    val requestBody: RequestBody,
    val task: Task<Response>
) : RequestBody() {
    private var forwardingSink: ForwardingSink? = null

    override fun contentType(): MediaType? = requestBody.contentType()

    override fun contentLength(): Long {
        return try {
            requestBody.contentLength()
        } catch(e: IOException) {
            e.printStackTrace()
            -1
        }
    }

    override fun writeTo(sink: BufferedSink) {
        forwardingSink = object : ForwardingSink(sink) {
            var totalBytesWritten: Long = 0L

            @Throws(IOException::class)
            override fun write(source: Buffer, byteCount: Long) {
                val contentLength = contentLength()
                val chunkSize: Long = max(contentLength / 20, 2048)
                var byteCountLeft: Long = byteCount
                while (byteCountLeft > 0) {
                    val bytesToWrite = min(byteCountLeft, chunkSize)
                    byteCountLeft -= bytesToWrite
                    super.write(source, bytesToWrite)
                    totalBytesWritten += bytesToWrite
                    task.progress =
                        if (totalBytesWritten >= contentLength) 1.0
                        else totalBytesWritten.toDouble() / contentLength.toDouble()
                }
            }
        }
        val bufferedSink = forwardingSink!!.buffer()
        requestBody.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}


private class ProgressResponseBody(
    val responseBody: ResponseBody,
    val task: Task<Response>
) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? = responseBody.contentType()

    override fun contentLength(): Long = responseBody.contentLength()

    override fun source(): BufferedSource {
        return bufferedSource ?: run {
            val bufferedSource = source(responseBody.source()).buffer()
            this.bufferedSource = bufferedSource
            bufferedSource
        }
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead: Long = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead: Long = super.read(sink, byteCount)
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                val contentLength = contentLength()
                val progress = totalBytesRead.toDouble() / contentLength.toDouble()
                task.progress = progress
                return bytesRead
            }
        }
    }
}
