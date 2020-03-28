package com.mitchmele.cameo.network

import retrofit2.Response
import retrofit2.http.Url

abstract class BaseResponseService {

    suspend fun <T> getData(call: suspend () -> Response<T>): Resource<T> {
        call().let { response ->
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null)
                    return Resource.success(data = body, source = Source.NETWORK)
            }
            return showError("${response.code()} ${response.message()}")
        }
    }
    private fun <T> showError(errorMessage: String): Resource<T> =
        Resource.error("Network call has failed for a following reason: $errorMessage", data = null)
}

