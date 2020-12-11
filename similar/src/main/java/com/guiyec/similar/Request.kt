package com.guiyec.similar

import com.google.gson.Gson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json as KotlinJson
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
        data class Json<T: Any>(val jsonBlock: () -> String): Data() {
            constructor(jsonString: String) : this({ jsonString })
            constructor(data: T, gson: Gson = Similar.defaultGson) : this({ gson.toJson(data) })
            constructor(data: T, serializer: KSerializer<T>) : this({ KotlinJson.encodeToString(serializer, data) })

            val jsonString: String get() = jsonBlock()
        }
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