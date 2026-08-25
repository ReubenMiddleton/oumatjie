package com.granify.app.session

sealed interface SessionState {
    data object SignedOut : SessionState
    data object Demo : SessionState
    data class SignedIn(val email: String?) : SessionState
}
