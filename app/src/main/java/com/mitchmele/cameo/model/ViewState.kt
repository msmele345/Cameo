package com.mitchmele.cameo.model

sealed class ViewState {
    object Data : ViewState()
    object Failure : ViewState()
    object Loading : ViewState()
    object Empty : ViewState()
}
