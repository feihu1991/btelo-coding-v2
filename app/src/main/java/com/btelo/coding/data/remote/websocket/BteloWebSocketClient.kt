package com.btelo.coding.data.remote.websocket

import com.btelo.coding.data.remote.encryption.CryptoManager
import com.google.crypto.tink.subtle.ChaCha20Poly1305
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class BteloWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val cryptoManager: CryptoManager
) {
    private val protocol = MessageProtocol(gson)
    private var webSocket: WebSocket? = null
    private val _messages = Channel<BteloMessage>(Channel.BUFFERED)

    val messages: Flow<BteloMessage> = _messages.receiveAsFlow()

    private var keyPair = cryptoManager.generateKeyPair()
    private var cipher: ChaCha20Poly1305? = null
    private var isEncrypted = false

    fun connect(url: String, token: String) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val publicKeyBase64 = android.util.Base64.encodeToString(
                    keyPair.publicKey, android.util.Base64.NO_WRAP
                )
                webSocket.send(protocol.serialize(BteloMessage.PublicKey(publicKeyBase64)))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = protocol.deserialize(text)
                when (message) {
                    is BteloMessage.PublicKey -> {
                        val remotePublicKey = android.util.Base64.decode(
                            message.key, android.util.Base64.NO_WRAP
                        )
                        val sharedSecret = cryptoManager.deriveSharedSecret(
                            keyPair.privateKey, remotePublicKey
                        )
                        cipher = cryptoManager.createCipherFromSharedSecret(sharedSecret)
                        isEncrypted = true
                        _messages.trySend(BteloMessage.Status(connected = true))
                    }
                    is BteloMessage.Output -> {
                        if (isEncrypted && cipher != null) {
                            try {
                                val encryptedData = android.util.Base64.decode(
                                    message.data, android.util.Base64.NO_WRAP
                                )
                                val decryptedData = cryptoManager.decrypt(encryptedData, cipher!!)
                                val decryptedMessage = message.copy(
                                    data = String(decryptedData)
                                )
                                _messages.trySend(decryptedMessage)
                            } catch (e: Exception) {
                                _messages.trySend(message)
                            }
                        } else {
                            _messages.trySend(message)
                        }
                    }
                    else -> {
                        if (message != null) {
                            _messages.trySend(message)
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _messages.trySend(BteloMessage.Status(connected = false))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _messages.trySend(BteloMessage.Status(connected = false))
            }
        })
    }

    fun send(message: BteloMessage): Boolean {
        val messageToSend = if (isEncrypted && cipher != null && message is BteloMessage.Command) {
            val encryptedData = cryptoManager.encrypt(
                message.content.toByteArray(), cipher!!
            )
            val encryptedBase64 = android.util.Base64.encodeToString(
                encryptedData, android.util.Base64.NO_WRAP
            )
            message.copy(content = encryptedBase64)
        } else {
            message
        }
        val json = protocol.serialize(messageToSend)
        return webSocket?.send(json) ?: false
    }

    fun disconnect(): Boolean {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isEncrypted = false
        cipher = null
        return true
    }
}
