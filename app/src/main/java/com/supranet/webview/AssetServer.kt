package com.supranet.webview

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream

class AssetServer(private val context: Context) : NanoHTTPD(8080) {
    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        return try {
            val assetStream: InputStream = context.assets.open(uri.removePrefix("/"))
            newChunkedResponse(Response.Status.OK, getMimeType(uri), assetStream)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
    }

    private fun getMimeType(uri: String): String {
        return when {
            uri.endsWith(".html") -> "text/html"
            uri.endsWith(".css") -> "text/css"
            uri.endsWith(".js") -> "application/javascript"
            uri.endsWith(".png") -> "image/png"
            uri.endsWith(".jpg") || uri.endsWith(".jpeg") -> "image/jpeg"
            else -> "application/octet-stream"
        }
    }
}