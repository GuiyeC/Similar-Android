package com.guiyec.similar

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException


class NetworkDispatcher: Dispatcher {
    private val client = OkHttpClient()

    override fun execute(request: Request): Task<String> {
        val urlBuilder = request.path.toHttpUrlOrNull()?.newBuilder() ?: run {
            throw RuntimeException("Invalid base url")
        }
        urlBuilder.addPathSegments(request.path)
        request.parameters?.forEach {
            urlBuilder.addQueryParameter(it.key, it.value)
        }
        val requestBuilder = okhttp3.Request.Builder()
            .url(request.path)
        request.headers?.forEach {
            requestBuilder.addHeader(it.key, it.value)
        }
        requestBuilder.addHeader("Accept", "application/json")
        requestBuilder.setData(request.method, request.data)
        val task = Task<String>()
        val okHttpRequest = requestBuilder.build()
        Log.d("NetworkDispatcher", okHttpRequest.toString())
        val call = client.newCall(okHttpRequest)
        task.cancelBlock = { call.cancel() }
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.code !in request.expectedCode) {
                    Log.e("NetworkDispatcher", "Server error: ${response.code}")
                    Log.e("NetworkDispatcher", "Server error: $responseBody")
                    task.fail(RequestError.ServerError(response.code, responseBody))
                    return
                }
                if (responseBody == null) {
                    Log.e("NetworkDispatcher", "Empty data")
                    task.fail(RequestError.NoData)
                    return
                }
                Log.i("NetworkDispatcher", responseBody)
                task.complete(responseBody)
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
        is Request.Data.Json -> {
            val jsonBody = data.gson.toJson(data.data)
            Log.i("NetworkDispatcher", jsonBody)
            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            method(method.value, body)
            addHeader("Content-Type", "application/json")
        }
        is Request.Data.Multipart -> TODO()
        null -> method(method.value, null)
    }
}