package com.mitchmele.cameo

import android.app.Application
import androidx.lifecycle.*
import com.mitchmele.cameo.model.GalleryItem
import com.mitchmele.cameo.network.PhotoFetcher
import com.mitchmele.cameo.network.Resource
import com.mitchmele.cameo.util.QueryPreferences

class CameoViewModel(private val app: Application) : AndroidViewModel(app) {

    var galleryItemLiveData: LiveData<List<GalleryItem>> = MutableLiveData()
    private val mutableSearchTerm = MutableLiveData<String>()

    val searchTerm: String
        get() = mutableSearchTerm.value ?: ""

    private val flickrFetcher = PhotoFetcher()

    init {
        mutableSearchTerm.value = QueryPreferences.getStoredQuery(app)

        galleryItemLiveData =
            Transformations.switchMap(mutableSearchTerm) { searchTerm ->
                when {
                    searchTerm.isBlank() -> {
                      flickrFetcher.fetchPhotos()
                    }
                    else -> flickrFetcher.searchPhotos(searchTerm)
                }
            }
    }

    fun fetchPhotos(query: String = "") {
        QueryPreferences.setStoredQuery(app, query)
        mutableSearchTerm.value = query
    }
}

