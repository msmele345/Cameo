package com.mitchmele.cameo.util

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.mitchmele.cameo.network.PhotoFetcher
import com.mitchmele.cameo.util.CameoConstants.MESSAGE_DOWNLOAD
import com.mitchmele.cameo.util.CameoConstants.TAG
import com.mitchmele.cameo.util.CameoConstants.THUMBNAIL_TAG
import java.util.concurrent.ConcurrentHashMap

class ThumbnailDownloader<in T>(
    private val responseHandler: Handler,
    private val onThumbnailDownloaded: (T, Bitmap) -> Unit
) : HandlerThread(THUMBNAIL_TAG), LifecycleObserver {

    val fragmentLifecycleObserver: LifecycleObserver =
        object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun setup() {
                Log.i(THUMBNAIL_TAG, "Starting background thread")
                start()
                looper
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun tearDown() {
                Log.i(THUMBNAIL_TAG, "Destroying background thread")
                quit()
            }
        }

    val viewLifeCycleOwner: LifecycleObserver =
        object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun tearDown() {
                Log.i(THUMBNAIL_TAG, "Clearing all requests from queue")
                requestHandler.removeMessages(MESSAGE_DOWNLOAD)
                requestMap.clear()
            }
        }


    private var hasQuit = false
    private lateinit var requestHandler: Handler
    private val requestMap = ConcurrentHashMap<T, String>()
    private val flickrFetchr = PhotoFetcher()

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        super.onLooperPrepared()
        requestHandler =
            object : Handler() {
                override fun handleMessage(msg: Message) {
                    if (msg.what == MESSAGE_DOWNLOAD) {
                        val target = msg.obj as T
                        Log.d(TAG, "GOT A REQUEST FOR URL: ${requestMap[target]}")
                        handleRequest(target)
                    }
                }
            }
    }

    private fun handleRequest(target: T) {
        val url = requestMap[target] ?: return
        val bitmap = flickrFetchr.fetchPhoto(url) ?: return

        responseHandler.post(Runnable {
            if (requestMap[target] != url) {
                return@Runnable
            }
            requestMap.remove(target)
            onThumbnailDownloaded(target, bitmap)
        })
    }

    fun queueThumbnail(target: T, url: String) {
        Log.i(THUMBNAIL_TAG, "GOT A URL: $url")
        requestMap[target] = url
        requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
            .sendToTarget()
    }
}