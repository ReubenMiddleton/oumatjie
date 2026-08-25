package com.granify.app.session

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.granify.app.auth.AuthManager
import com.granify.app.auth.AuthorizeOutcome
import com.granify.app.data.gmail.GmailMailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pendingResolution: PendingIntent? = null,
)

/**
 * Owns whether the user is browsing the offline demo or a real, signed-in Gmail account.
 *
 * Also owns session persistence (docs/ROADMAP.md's retrospective, "Session persistence was
 * deferred too readily"): on creation, if [sessionRepository] remembers a previous Google
 * sign-in, this attempts one silent [AuthManager.authorize] call before the sign-in screen is
 * ever shown. That call either succeeds silently (scopes were already granted — no UI, no
 * account picker) or fails, in which case the user simply sees the normal sign-in screen, same
 * as before this feature existed. Nothing is ever launched (no account picker, no browser tab)
 * without an explicit, visible user action first.
 */
class SessionViewModel(
    private val authManager: AuthManager,
    private val gmailMailRepository: GmailMailRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _session = MutableStateFlow<SessionState>(SessionState.SignedOut)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _signInState = MutableStateFlow(SignInUiState())
    val signInState: StateFlow<SignInUiState> = _signInState.asStateFlow()

    init {
        attemptSilentSignIn()
    }

    private fun attemptSilentSignIn() {
        viewModelScope.launch {
            if (!sessionRepository.hasSignedInBefore()) return@launch

            _signInState.update { it.copy(isLoading = true) }
            when (val outcome = authManager.authorize(GmailMailRepository.REQUIRED_SCOPES)) {
                is AuthorizeOutcome.Granted -> handleOutcome(outcome)
                is AuthorizeOutcome.Failed -> {
                    // A genuine, resolved failure (e.g. access was revoked) — stop trying on
                    // every future cold start and fall back to the ordinary sign-in screen.
                    sessionRepository.clear()
                    _signInState.value = SignInUiState()
                }
                is AuthorizeOutcome.ResolutionRequired -> {
                    // Needs the user's own confirmation (e.g. the account was removed from the
                    // device). Never launch the account picker before the user has done
                    // anything — fall back to the sign-in screen; tapping "Continue with
                    // Google" resolves it the ordinary, explicit way. The flag is kept, since
                    // this isn't proof the account is gone for good.
                    _signInState.value = SignInUiState()
                }
            }
        }
    }

    fun enterDemo() {
        _session.value = SessionState.Demo
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _signInState.update { it.copy(isLoading = true, errorMessage = null) }
            handleOutcome(authManager.authorize(GmailMailRepository.REQUIRED_SCOPES))
        }
    }

    /** Call after the sign-in launcher returns, with the resulting [Intent]. */
    fun onAuthorizationResolved(data: Intent?) {
        viewModelScope.launch {
            _signInState.update { it.copy(isLoading = true) }
            handleOutcome(authManager.resolveAuthorizationResult(data))
        }
    }

    /** Call once the pending resolution has been launched, so it is not launched twice. */
    fun dismissResolution() {
        _signInState.update { it.copy(pendingResolution = null) }
    }

    fun signOut() {
        _session.value = SessionState.SignedOut
        _signInState.value = SignInUiState()
        viewModelScope.launch { sessionRepository.clear() }
    }

    private suspend fun handleOutcome(outcome: AuthorizeOutcome) {
        when (outcome) {
            is AuthorizeOutcome.Granted -> {
                val email = runCatching { gmailMailRepository.fetchAccountEmail() }.getOrNull()
                sessionRepository.recordSignedIn()
                _signInState.value = SignInUiState()
                _session.value = SessionState.SignedIn(email)
            }
            is AuthorizeOutcome.ResolutionRequired -> {
                _signInState.update {
                    it.copy(isLoading = false, pendingResolution = outcome.pendingIntent)
                }
            }
            is AuthorizeOutcome.Failed -> {
                _signInState.update {
                    it.copy(isLoading = false, errorMessage = outcome.message, pendingResolution = null)
                }
            }
        }
    }

    companion object {
        fun factory(
            authManager: AuthManager,
            gmailMailRepository: GmailMailRepository,
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SessionViewModel(authManager, gmailMailRepository, sessionRepository) as T
        }
    }
}
