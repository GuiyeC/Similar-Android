package com.guiyec.similar

import com.google.gson.Gson

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
        data class Multipart(val name: String, val mimeType: String, val fileName: String, val fileData: ByteArray): Data()
    }
}