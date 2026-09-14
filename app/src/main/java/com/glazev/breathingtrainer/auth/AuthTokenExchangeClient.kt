package com.glazev.breathingtrainer.auth

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal enum class ExternalAuthProvider(val wireName: String, val fallbackLabel: String) {
    YANDEX("yandex", "Яндекс ID"),
    VK("vk", "VK ID")
}

internal data class ExchangedAuthToken(
    val firebaseCustomToken: String,
    val displayName: String?
)

internal class AuthTokenExchangeClient(private val endpoint: String) {
    fun exchange(provider: ExternalAuthProvider, accessToken: String): Result<ExchangedAuthToken> =
        if (!hasSecureEndpoint()) insecureEndpointFailure() else post(
            JSONObject()
                .put("provider", provider.wireName)
                .put("accessToken", accessToken)
        )

    fun exchangeVkAuthorizationCode(
        authorizationCode: String,
        deviceId: String,
        proof: VkAuthorizationProof
    ): Result<ExchangedAuthToken> = if (!hasSecureEndpoint()) insecureEndpointFailure() else post(
        JSONObject()
            .put("provider", ExternalAuthProvider.VK.wireName)
            .put("authorizationCode", authorizationCode)
            .put("deviceId", deviceId)
            .put("codeVerifier", proof.codeVerifier)
            .put("state", proof.state)
    )

    private fun hasSecureEndpoint(): Boolean = endpoint.startsWith("https://")

    private fun insecureEndpointFailure(): Result<ExchangedAuthToken> =
        Result.failure(IllegalArgumentException("Сервер авторизации не настроен"))

    private fun post(request: JSONObject): Result<ExchangedAuthToken> = runCatching {
        require(hasSecureEndpoint()) { "Сервер авторизации не настроен" }

        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")

            val requestBody = request.toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(requestBody) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(responseCode in 200..299) { "Сервер отклонил авторизацию ($responseCode)" }
            parseResponse(body)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseResponse(body: String): ExchangedAuthToken {
        val json = JSONObject(body)
        val token = json.optString("firebaseCustomToken")
        require(token.isNotBlank()) { "Сервер не вернул Firebase custom token" }
        return ExchangedAuthToken(
            firebaseCustomToken = token,
            displayName = json.optString("displayName").takeIf(String::isNotBlank)
        )
    }
}
