package com.guiyec.similar

import java.io.ByteArrayInputStream
import java.io.InputStream

data class Response(
    val data: Data,
    val statusCode: Int,
    val headers: Map<String, String>
) {
    interface Data {
        val inputStream: InputStream
        val bytes: ByteArray
        val string: String

        fun isEmpty(): Boolean
    }

    constructor(response: okhttp3.Response) :
            this(OkhttpData(response), response.code, response.headers.toMap())
}

data class ResponseData(override val bytes: ByteArray): Response.Data {
    override val inputStream: InputStream get() = ByteArrayInputStream(bytes)
    override val string: String get() = bytes.toString(Charsets.UTF_8)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ResponseData
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    override fun isEmpty(): Boolean = bytes.isEmpty()
}

data class OkhttpData(private val response: okhttp3.Response): Response.Data {
    override val inputStream: InputStream get() = response.body!!.byteStream()
    override val bytes: ByteArray get() = response.body?.bytes() ?: byteArrayOf()
    override val string: String get() = response.body?.string() ?: ""

    override fun isEmpty(): Boolean = string.isEmpty()
}