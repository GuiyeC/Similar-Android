package com.guiyec.similar

import java.io.InputStream

data class Response(
    val data: Data,
    val statusCode: Int,
    val headers: Map<String, String>
) {
    data class Data(private val response: okhttp3.Response) {
        val inputStream: InputStream get() = response.body!!.byteStream()
        val bytes: ByteArray get() = response.body!!.bytes()
        val string: String get() = response.body!!.string()
    }

    constructor(response: okhttp3.Response) :
            this(Data(response), response.code, response.headers.toMap())
}