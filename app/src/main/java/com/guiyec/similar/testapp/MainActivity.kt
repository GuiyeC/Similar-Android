package com.guiyec.similar.testapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import com.guiyec.similar.*
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LoginRequest(
    @Required val email: String = "peter@klaven"
)

@Serializable
data class LoginRequest2(
    @Required val email: String = "eve.holt@reqres.in",
    val password: String? = null
)

@Serializable
data class SuccessResponse(
    val token: String
)

data class ErrorResponse(
    val error: String
)
@Serializable
data class Project(@Required val name: String = "ho", val language: String)
data class Item(val id: Int, val name: String)

data class Response(val data: List<Item>)

class MainActivity : AppCompatActivity() {
    val dispatcher = NetworkDispatcher()
    val repo = Repository(Response::class, "https://reqres.in/api/unknown", dispatcher)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val data = Project(language =  "Kotlin")
        Log.d("MainAc", "JSON: ${Json.encodeToString(data)}")
        Log.d("MainAc", "JSON: ${Json.encodeToString(LoginRequest())}")
        Log.d("MainAc", "JSON2: ${Json.encodeToString(LoginRequest2(password = "cityslicka"))}")

        val request = Request("https://reqres.in/api/login", method = HttpMethod.Post, parameters = mapOf(Pair("page", "2"), Pair("page_size", "2c"), Pair("page", "B")), data = Request.Data.Json(LoginRequest2(password = "cityslicka"), LoginRequest2.serializer()))
//        val file = assets.open("large-file.json")
//        val request = Request("https://reqres.in/api/login", method = HttpMethod.Post, parameters = mapOf(Pair("page", "2"), Pair("page_size", "2c"), Pair("page", "B")), data = Request.Data.Multipart(
//            listOf(
//                Request.DataPart("thins", data = file.readBytes()),
//                Request.DataPart("thinngcs", data = assets.open("large-file.json").readBytes()),
//                Request.DataPart("gregre", data = "fewfewfewfewfew"),
//                Request.DataPart("th4ins", data = assets.open("large-file.json").readBytes())
//            )))
//        dispatcher.execute(request)
//            .progress { Log.d("NetworkDispatcher", "Progress $it") }
//            .decode(SuccessResponse.serializer())
//            .print()
//            .sink(Looper.getMainLooper()) {
//                findViewById<TextView>(R.id.textView).text = it.token
//            }
//            .catch(ErrorResponse::class) { code, error ->
//                print("Code: $code")
//                print("$error")
//            }
//
//        repo.sink {  }

        dispatcher.execute(Request("http://ipv4.download.thinkbroadband.com/20MB.zip"))
            .progress { Log.d("NetworkDispatcher", "Progress $it") }
            .sink {
                it.data.string
                Log.d("NetworkDispatcher","Complete")
            }
            .catch { Log.d("NetworkDispatcher","Failed") }
            .print()
    }
}