package io.github.cotrin1208.model.feature

import io.github.cotrin1208.util.LineSignature
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

val LineSignatureVerification = createRouteScopedPlugin(
    name = "LineSignatureVerificationPlugin",
    createConfiguration = ::Configuration
) {
    suspend fun ApplicationCall.verifySignature(channelSecret: String): Boolean {
        val signatureFromHeader = request.header(HttpHeaders.LineSignature) ?: return false
        val key = SecretKeySpec(channelSecret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(key)
        }
        val source = receiveText().toByteArray(Charsets.UTF_8)
        val calculatedSignature = Base64.getEncoder().encodeToString(mac.doFinal(source))
        return signatureFromHeader == calculatedSignature
    }

    pluginConfig.apply {
        onCall { call ->
            if (!call.verifySignature(channelSecret)) {
                call.respond(HttpStatusCode.Forbidden)
                return@onCall
            }
        }
    }
}

class Configuration {
    lateinit var channelSecret: String
}
