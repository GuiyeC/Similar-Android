package com.guiyec.similar

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString
import java.io.File

data class Request(
    var path: String,
    var method: HttpMethod = HttpMethod.Get,
    var expectedCode: IntRange = (200..299),
    var headers: Map<String, String>? = null,
    var parameters: Map<String, String>? = null,
    var data: Data? = null
) {
    sealed class Data {
        data class Json(val data: Any, val gson: Gson = Similar.defaultGson): Data()
        data class Multipart(val parts: List<DataPart>): Data()
    }

    data class DataPart internal constructor(val name: String, val fileName: String? = null, val data: RequestBody) {
        constructor(name: String, fileName: String? = null, mimeType: String? = null, data: String) :
                this(name, fileName, data.toRequestBody(mimeType?.toMediaTypeOrNull()))

        constructor(name: String, fileName: String? = null, mimeType: String? = null, data: ByteString) :
                this(name, fileName, data.toRequestBody(mimeType?.toMediaTypeOrNull()))

        constructor(name: String, fileName: String? = null, mimeType: String? = null, data: ByteArray) :
                this(name, fileName, data.toRequestBody(mimeType?.toMediaTypeOrNull()))

        constructor(name: String, fileName: String? = null, mimeType: String? = null, data: File) :
                this(name, fileName, data.asRequestBody(mimeType?.toMediaTypeOrNull()))
    }
}