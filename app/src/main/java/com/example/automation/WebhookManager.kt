package com.example.automation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebhookManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun executeWebhook(
        url: String,
        method: String = "POST",
        headersJson: String = "{}",
        payloadJson: String = ""
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val logBuilder = StringBuilder()
        logBuilder.append("[HTTP Webhook Dispatcher]\n")
        logBuilder.append("Method: ${method.uppercase()}\n")
        logBuilder.append("Target URL: $url\n")

        try {
            val requestBuilder = Request.Builder().url(url)

            // Parse headers
            if (headersJson.isNotBlank() && headersJson.trim().startsWith("{")) {
                try {
                    val jsonHeaders = JSONObject(headersJson)
                    val keys = jsonHeaders.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = jsonHeaders.optString(key)
                        if (key.isNotBlank() && value.isNotBlank()) {
                            requestBuilder.addHeader(key, value)
                            logBuilder.append("Header: $key -> $value\n")
                        }
                    }
                } catch (e: Exception) {
                    logBuilder.append("[!] Warning: Failed to parse custom headers JSON: ${e.message}\n")
                }
            }

            // Body for POST/PUT
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = if (method.equals("POST", ignoreCase = true) || method.equals("PUT", ignoreCase = true)) {
                payloadJson.toRequestBody(mediaType)
            } else {
                null
            }

            when (method.uppercase()) {
                "GET" -> requestBuilder.get()
                "POST" -> requestBuilder.post(requestBody ?: "".toRequestBody(mediaType))
                "PUT" -> requestBuilder.put(requestBody ?: "".toRequestBody(mediaType))
                "DELETE" -> requestBuilder.delete(requestBody)
                else -> requestBuilder.get()
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""
            val duration = System.currentTimeMillis() - startTime
            val statusCode = response.code

            logBuilder.append("\n[+] Response Code: $statusCode ${response.message}\n")
            logBuilder.append("Latency: ${duration}ms\n")
            logBuilder.append("Response Payload:\n$responseBody\n")

            ExecutionResult(
                isSuccess = response.isSuccessful,
                output = logBuilder.toString().trim(),
                durationMs = duration,
                exitCode = if (response.isSuccessful) 0 else statusCode
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logBuilder.append("\n[-] Webhook Failed: ${e.localizedMessage ?: e.message}\n")
            ExecutionResult(
                isSuccess = false,
                output = logBuilder.toString().trim(),
                durationMs = duration,
                exitCode = -1
            )
        }
    }
}
