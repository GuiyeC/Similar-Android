package com.guiyec.similar

import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException


open class NetworkDispatcher: Dispatcher {
    private val client = OkHttpClient()

    override fun execute(request: Request): Task<String> {
        val urlBuilder = request.path.toHttpUrlOrNull()?.newBuilder()
        require(urlBuilder != null) { "Invalid url" }
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
        is Request.Data.Multipart -> {
            val fileBody = data.file.asRequestBody(data.mimeType.toMediaTypeOrNull())
            val requestBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(data.name, data.fileName, fileBody)
                .build()
            method(method.value, requestBody)
        }
        null -> method(method.value, null)
    }
}