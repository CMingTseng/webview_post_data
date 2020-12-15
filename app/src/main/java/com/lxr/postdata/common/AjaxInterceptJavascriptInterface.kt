package com.lxr.postdata.common

import android.content.Context
import android.webkit.JavascriptInterface
import org.jsoup.Jsoup
import java.io.IOException

class AjaxInterceptJavascriptInterface(webViewClient: WriteHandlingWebViewClient?) {
    private var mWebViewClient: WriteHandlingWebViewClient? = null

    /**
     * js调用该方法，将post参数给客户端
     *
     * @param ID   key值
     * @param body 参数
     */
    @JavascriptInterface
    fun customAjax(ID: String, body: String) {
        mWebViewClient?.addAjaxRequest(ID, body)
    }

    companion object {
        private var interceptHeader: String? = null
        @Throws(IOException::class)
        fun enableIntercept(context: Context, data: ByteArray?): String {
            if (interceptHeader == null) {
                interceptHeader =
                        Utils.consumeInputStream(context.assets.open("interceptheader.html"))?.let {
                            String(
                                    it
                            )
                        }
            }
            val doc = Jsoup.parse(String(data!!))
            doc.outputSettings().prettyPrint(true)

            // Prefix every script to capture submits
            // Make sure our interception is the first element in the
            // header
            val element = doc.getElementsByTag("head")
            if (element.size > 0) {
                element[0].prepend(interceptHeader)
            }
            return doc.toString()
        }
    }

    init {
        mWebViewClient = webViewClient
    }
}