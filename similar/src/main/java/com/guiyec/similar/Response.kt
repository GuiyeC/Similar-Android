package com.guiyec.similar

import okio.IOException
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
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    override fun isEmpty(): Boolean = bytes.isEmpty()
}

data class OkhttpData(private val response: okhttp3.Response): Response.Data {
    override val inputStream: InputStream get() = response.body.byteStream()
    private var _bytes: ByteArray? = null
    override val bytes: ByteArray
        get() {
            _bytes?.let { return it }
            _string?.let { return it.toByteArray(Charsets.UTF_8) }
            val bytes = try {
                response.body.bytes()
            } catch (e: IOException) {
                e.printStackTrace()
                byteArrayOf()
            }
            _bytes = bytes

            return bytes
        }
    private var _string: String? = null
    override val string: String
        get() {
            _string?.let { return it }
            _bytes?.let { return bytes.toString(Charsets.UTF_8) }
            val string = try {
                response.body.string()
            } catch (e: IOException) {
                e.printStackTrace()
                ""
            }
            _string = string
            return string
        }

    override fun isEmpty(): Boolean = string.isEmpty()
}