package com.lxr.postdata.common

import android.net.Uri
import android.webkit.WebResourceRequest

class WriteHandlingWebResourceRequest internal constructor(
        private val originalWebResourceRequest: WebResourceRequest,
        val requestBody: String?,
        uri: Uri?
) : WebResourceRequest {
    private var uri: Uri? = null

    init {
        if (uri != null) {
            this.uri = uri
        } else {
            this.uri = originalWebResourceRequest.url
        }
    }

    override fun getUrl(): Uri {
        return uri!!
    }

    override fun isForMainFrame(): Boolean {
        return originalWebResourceRequest.isForMainFrame
    }

    override fun isRedirect(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun hasGesture(): Boolean {
        return originalWebResourceRequest.hasGesture()
    }

    override fun getMethod(): String {
        return originalWebResourceRequest.method
    }

    override fun getRequestHeaders(): Map<String, String> {
        return originalWebResourceRequest.requestHeaders
    }

    fun getAjaxData(): String? {
        return requestBody
    }

    fun hasAjaxData(): Boolean {
        return requestBody != null
    }
}
