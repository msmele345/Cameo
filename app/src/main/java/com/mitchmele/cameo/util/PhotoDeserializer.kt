package com.mitchmele.cameo.util

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.mitchmele.cameo.model.GalleryItem
import com.mitchmele.cameo.model.PhotoResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class PhotoDeserializer : JsonDeserializer<PhotoResponse> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PhotoResponse {

        val valueType: Type =
            (typeOfT as ParameterizedType).actualTypeArguments[0]


        val list = mutableListOf<GalleryItem?>()

        json?.asJsonArray?.forEach {
            list.add(context?.deserialize(it, valueType))
        }

        return PhotoResponse().apply {
            galleryItems = list.toList().filterNotNull()
        }
        //pull photos outside of JsonElement
        //and convert to PhotoResponse
    }
}