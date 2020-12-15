package com.lxr.postdata.common

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object Utils {
    @Throws(IOException::class)
    fun consumeInputStream(inputStream: InputStream): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var count: Int
        while (inputStream.read(buffer).also { count = it } != -1) {
            byteArrayOutputStream.write(buffer, 0, count)
        }
        return byteArrayOutputStream.toByteArray()
    }
}