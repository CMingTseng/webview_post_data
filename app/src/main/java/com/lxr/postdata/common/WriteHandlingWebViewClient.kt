package com.lxr.postdata.common

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.OkHttpClient
import okhttp3.OkUrlFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset
open class WriteHandlingWebViewClient(webView: WebView) : WebViewClient() {
    private val MARKER = "AJAXINTERCEPT"

    /**
     * 请求参数Map集
     */
    private val ajaxRequestContents: MutableMap<String, String>  = mutableMapOf()
    init {
        val ajaxInterface = AjaxInterceptJavascriptInterface(this)
        webView.addJavascriptInterface(ajaxInterface, "interception")
    }

    fun addAjaxRequest(id: String, body: String) {
        ajaxRequestContents.put(id, body)
    }
    /*
    ** This here is the "fixed" shouldInterceptRequest method that you should override.
    ** It receives a WriteHandlingWebResourceRequest instead of a WebResourceRequest.
    */
    fun shouldInterceptRequest(view: WebView?, request: WriteHandlingWebResourceRequest): WebResourceResponse? {
        val client = OkHttpClient()
        try {
            // Our implementation just parses the response and visualizes it. It does not properly handle
            // redirects or HTTP errors at the moment. It only serves as a demo for intercepting POST requests
            // as a starting point for supporting multiple types of HTTP requests in a full fletched browser
            // Construct request
            val conn = OkUrlFactory(client).open(URL(request.url.toString()))
            conn.requestMethod = request.method
            if ("POST" == request.method) {
                val os = conn.outputStream
                try {
                    os.write(request.getAjaxData()?.toByteArray())
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
                os.close()
            }
            // Read input
            val charset =
                    if (conn.contentEncoding != null) conn.contentEncoding else Charset.defaultCharset()
                            .displayName()
            val mime = conn.contentType
            val pageContents: ByteArray = Utils.consumeInputStream(conn.inputStream)!!

            // Convert the contents and return
            val isContents: InputStream = ByteArrayInputStream(pageContents)
            return WebResourceResponse(mime, charset, isContents)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    /**
     * 拦截了webview中的所有请求
     *
     * @param view
     * @param request
     * @return
     */
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
        var requestBody: String? = null
        var uri = request.url
        // 判断是否为Ajax请求（只要链接中包含AJAXINTERCEPT即是）
        if (isAjaxRequest(request)) {
            // 获取post请求参数
            requestBody = getRequestBody(request)
            // 获取原链接
            uri = getOriginalRequestUri(request, MARKER)
        }

        // 重新构造请求，并获取response

        // 重新构造请求，并获取response
        val webResourceResponse =
                shouldInterceptRequest(view, WriteHandlingWebResourceRequest(request, requestBody, uri))
        return if (webResourceResponse == null) {
            webResourceResponse
        } else {
            injectIntercept(webResourceResponse, view!!.context)
        }
    }

    /**
     * 获取原链接
     *
     * @param request
     * @param marker
     * @return
     */
    private fun getOriginalRequestUri(request: WebResourceRequest, marker: String): Uri? {
        val urlString = getUrlSegments(request, marker)!![0]!!
        return Uri.parse(urlString)
    }

    /**
     * 获取post请求参数
     *
     * @param request
     * @return
     */
    private fun getRequestBody(request: WebResourceRequest): String? {
        val requestID: String = getAjaxRequestID(request)!!
        return getAjaxRequestBodyByID(requestID)
    }

    /**
     * 判断是否为Ajax请求
     *
     * @param request
     * @return
     */
    private fun isAjaxRequest(request: WebResourceRequest): Boolean {
        return request.url.toString().contains(MARKER)
    }

    /**
     * 通过请求id获取请求参数
     *
     * @param requestID
     * @return
     */
    private fun getAjaxRequestBodyByID(requestID: String): String? {
        val body = ajaxRequestContents[requestID]
        ajaxRequestContents.remove(requestID)
        return body
    }

    /**
     * 获取请求的id
     *
     * @param request
     * @return
     */
    private fun getAjaxRequestID(request: WebResourceRequest): String? {
        return getUrlSegments(request, MARKER)!!.get(1)
    }

    private fun getUrlSegments(request: WebResourceRequest, divider: String): Array<String?>? {
        val urlString = request.url.toString()
        return urlString.split(divider.toRegex()).toTypedArray()
    }

    /**
     * 如果请求是网页，则html注入
     *
     * @param response
     * @param context
     * @return
     */
    private fun injectIntercept(
            response: WebResourceResponse,
            context: Context
    ): WebResourceResponse? {
        val encoding = response.encoding
        var mime = response.mimeType

        // WebResourceResponse的mime必须为"text/html",不能是"text/html; charset=utf-8"
        if (mime != null && mime.contains("text/html")) {
            mime = "text/html"
        }
        val responseData = response.data
        val injectedResponseData: InputStream? = injectInterceptToStream(
                context,
                responseData,
                mime,
                encoding
        )
        return WebResourceResponse(mime, encoding, injectedResponseData)
    }

    /**
     * 如果请求是网页，则html注入
     *
     * @param context
     * @param is
     * @param mime
     * @param charset
     * @return
     */
    private fun injectInterceptToStream(
            context: Context,
            `is`: InputStream,
            mime: String?,
            charset: String
    ): InputStream? {
        return try {
            var pageContents: ByteArray? =  Utils.consumeInputStream(`is`)
            if (mime != null && mime.contains("text/html")) {
                pageContents = AjaxInterceptJavascriptInterface .enableIntercept(
                        context,
                        pageContents
                ).toByteArray(charset(charset))
            }
            ByteArrayInputStream(pageContents)
        } catch (e: java.lang.Exception) {
            throw java.lang.RuntimeException(e.message)
        }
    }
}