package com.guiyec.similar

enum class HttpMethod(val value: String) {
    Post("POST"),
    Options("OPTIONS"),
    Get("GET"),
    Head("HEAD"),
    Put("PUT"),
    Patch("PATCH"),
    Delete("DELETE"),
    Trace("TRACE"),
    Connect("CONNECT")
}