package com.mitchmele.cameo.network.api

import com.mitchmele.cameo.util.CameoConstants.API_KEY
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class PhotoInterceptor : Interceptor {

    /* Add to photofetcher init if needed
         var galleryItemListType =
        object : TypeToken<List<GalleryItem?>?>() {}.type

    val gson = GsonBuilder()
        .run {
            registerTypeAdapter(galleryItemListType, PhotoDeserializer())
        }.create()
     */


    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()

        val newUrl: HttpUrl = originalRequest
            .url()
            .newBuilder().run {
                addQueryParameter("api_key", API_KEY)
                addQueryParameter("format", "json")
                addQueryParameter("nojsoncallback", "1")
                addQueryParameter("extras", "url_s")
                addQueryParameter("safesearch", "1")
                build()
            }

        val newRequest: Request = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}