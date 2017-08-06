package Client


import kotlinx.coroutines.experimental.*
import com.beust.klaxon.*
import com.sun.org.apache.xpath.internal.operations.Bool
import javafx.fxml.FXML
import java.net.HttpURLConnection
import java.net.URL

import kotlinx.coroutines.experimental.javafx.JavaFx as UI
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

import java.io.InputStreamReader
import java.io.BufferedReader
import java.net.ConnectException
import java.net.URLEncoder
import kotlin.concurrent.timer


class Controller {
    @FXML var connectButton: ToggleButton? = null
    @FXML var messagesList: ListView<String>? = null
    @FXML var textArea: TextArea? = null
    @FXML var nickField: TextField? = null

    val defauldUrl = "http://keychatserver.azurewebsites.net"
    var userKey = ""
    var isLogged = false

    fun getResponse(url: URL, jsonObject: JsonObject? = null): String {
        val connection = url.openConnection() as HttpURLConnection
        with(connection) {
            addRequestProperty("Content-Type", "application/json")

            useCaches = false
            if (jsonObject != null) {
                doOutput = true
                requestMethod = "POST"
                outputStream.write(jsonObject.toJsonString().toByteArray())
                outputStream.flush()
                outputStream.close()
            } else {
                requestMethod = "GET"
            }
            val code = responseCode
            //connection.outputStream.close()
            val input = BufferedReader(
                    InputStreamReader(inputStream))
            var inputLine: String?
            val response = StringBuffer()
            inputLine = input.readLine()
            while (inputLine != null) {
                response.append(inputLine)
                inputLine = input.readLine()
            }
            input.close()

            return response.toString()
        }
    }

    fun updateMessages() = launch(CommonPool) {
        val response = getResponse(URL("$defauldUrl/api/chat/history"))
        val messages = Parser().parse(StringBuilder(response)) as JsonArray<JsonObject>

        launch(UI) {
            messagesList!!.items.clear()
            messages.forEach {
                messagesList!!.items.add("${it.string("author_nick")}>${it.string("text")}")
            }
        }
    }

    suspend fun deactivateConnection() = launch(UI) {
        isLogged = false
        connectButton!!.text = "Connect"
        connectButton!!.isSelected = false
        nickField!!.isEditable = true
    }

    @FXML
    fun connectClicked() = launch(CommonPool) {
        if (!isLogged) {
            launch(UI) { connectButton!!.text = "Connecting..." }
            try {
                val loginR = getResponse(URL("$defauldUrl/api/chat/authorize?name=${nickField!!.text}"))
                var credentials = Parser().parse(StringBuilder(loginR)) as JsonObject

                userKey = credentials.string("key") ?: throw Exception()
                isLogged = true

                /*timer(period = 1000) {
                    //updateMessages()
                }*/
                updateMessages()
                listenLongPool()

                launch(UI) {
                    connectButton!!.text = "Connected"
                    nickField!!.isEditable = true
                }
            } catch (ex: Exception) {
                launch(UI) {
                    connectButton!!.text = "Failed."
                    connectButton!!.isSelected = false
                }
            }
        } else {

            deactivateConnection()
        }
    }

    fun sendMessage() = async<Boolean>(CommonPool) {
        val resp = getResponse(URL("$defauldUrl/api/chat/send?key=$userKey&message=${URLEncoder.encode(textArea!!.text)}"))
        val response = Parser().parse(StringBuilder(resp)) as JsonObject

        response.string("result") == "OK"
    }

    suspend fun listenLongPool() {
        launch(CommonPool) {
            try {
                var resp = getResponse(URL("$defauldUrl/api/chat/longpool?ws=10"))
                if (resp != "") {
                    val messages = Parser().parse(StringBuilder(resp)) as JsonArray<JsonObject>
                    launch(UI) {
                        messages.forEach {
                            messagesList!!.items.add("${it.string("author")}>${it.string("text")}")
                        }
                    }
                }
                listenLongPool()
            }catch (ex:ConnectException) {
                deactivateConnection()
            }
        }
    }

    @FXML fun textkeyPressed(args: KeyEvent) = launch(CommonPool) {
        if (args.code == KeyCode.ENTER) {
            val res = sendMessage().await()
            if (res) {
                launch(UI) { textArea!!.text = "" }
            } else {
                deactivateConnection()
            }
            args.consume()
        }
    }
}
