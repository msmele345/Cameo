package com.mitchmele.cameo.model

import com.google.gson.annotations.SerializedName


data class GalleryItem(
    var title: String = "",
    var id: String = "",
    @SerializedName("url_s")var url: String = ""
)

class PhotoResponse {
    @SerializedName("photo")
    lateinit var galleryItems: List<GalleryItem>
}

class FlickrResponse {
    lateinit var photos: PhotoResponse
}