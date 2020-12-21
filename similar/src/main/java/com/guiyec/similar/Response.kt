package com.guiyec.similar

data class Response(
    val data: String,
    val statusCode: Int,
    val headers: Map<String, String>
) {
    internal constructor(data: String, response: okhttp3.Response): this(data, response.code, response.headers.toMap())
}