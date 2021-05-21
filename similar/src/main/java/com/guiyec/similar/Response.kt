package com.guiyec.similar

import java.io.InputStream

data class Response(private val response: okhttp3.Response) {
    data class Data(private val response: okhttp3.Response) {
        val inputStream: InputStream get() = response.body!!.byteStream()
        val bytes: ByteArray get() = response.body!!.bytes()
        val string: String get() = response.body!!.string()
    }

    val data: Data get() = Data(response)
    val statusCode: Int get() = response.code
    val headers: Map<String, String> get() = response.headers.toMap()
}