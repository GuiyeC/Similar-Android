package com.guiyec.similar

sealed class RequestError: Error() {
    object NoData: RequestError()
    data class LocalError(val error: Throwable): RequestError()
    data class ServerError(val code: Int, val data: String?): RequestError()
    data class DecodingError(val error: Throwable): RequestError()
}