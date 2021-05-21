package com.guiyec.similar

import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

open class NetworkDispatcher: Dispatcher {
    private val client = OkHttpClient()

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
        requestBuilder.setData(request.method, request.data)
        val task = Task<Response>()
        val okHttpRequest = requestBuilder.build()
        Log.d("NetworkDispatcher", okHttpRequest.toString())
        val call = client.newCall(okHttpRequest)
        task.cancelBlock = { call.cancel() }
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if (response.code !in request.expectedCode) {
                    val responseBody = response.body?.string()
                    Log.e("NetworkDispatcher", "Server error: ${response.code}")
                    Log.e("NetworkDispatcher", "Server error body: $responseBody")
                    task.fail(RequestError.ServerError(response.code, responseBody))
                    return
                }
                if (response.body?.contentLength() == null) {
                    Log.e("NetworkDispatcher", "Empty data")
                    task.fail(RequestError.NoData)
                    return
                }
                task.complete(Response(response))
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                task.fail(RequestError.LocalError(e))
            }
        })
        return task
    }
}

fun okhttp3.Request.Builder.setData(method: HttpMethod, data: Request.Data?) {
    when (data) {
        is Request.Data.Json<*> -> {
            val jsonBody = data.jsonString
            Log.i("NetworkDispatcher", jsonBody)
            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            method(method.value, body)
            addHeader("Content-Type", "application/json")
        }
        is Request.Data.Multipart -> {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            data.parts.forEach { builder.addFormDataPart(it.name, it.fileName, it.data) }
            val requestBody = builder.build()
            method(method.value, requestBody)
        }
        null -> method(method.value, null)
    }
}