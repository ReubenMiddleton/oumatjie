package com.granify.app.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.granify.app.data.MailRepository
import com.granify.app.data.attachments.AttachmentDownloader
import com.granify.app.di.AppContainer
import com.granify.app.session.SessionState
import com.granify.app.session.SessionViewModel
import com.granify.app.ui.mail.MailRoute
import com.granify.app.ui.settings.SettingsScreen
import com.granify.app.ui.signin.SignInScreen

@Composable
fun OumatjieNavHost(container: AppContainer, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModel.factory(
            container.authManager,
            container.gmailMailRepository,
            container.sessionRepository,
        ),
    )
    val session by sessionViewModel.session.collectAsStateWithLifecycle()

    // A plain crossfade instead of Navigation Compose's default slide, so moving between
    // screens never relies on spatial motion (docs/PRODUCT_PRINCIPLES.md: "reduced motion").
    val fadeSpec = tween<Float>(150)
    NavHost(
        navController = navController,
        startDestination = Destinations.SIGN_IN,
        modifier = modifier,
        enterTransition = { fadeIn(fadeSpec) },
        exitTransition = { fadeOut(fadeSpec) },
        popEnterTransition = { fadeIn(fadeSpec) },
        popExitTransition = { fadeOut(fadeSpec) },
    ) {
        composable(Destinations.SIGN_IN) {
            val uiState by sessionViewModel.signInState.collectAsStateWithLifecycle()

            LaunchedEffect(session) {
                if (session != SessionState.SignedOut) {
                    navController.navigate(Destinations.INBOX) {
                        popUpTo(Destinations.SIGN_IN) { inclusive = true }
                    }
                }
            }

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { result -> sessionViewModel.onAuthorizationResolved(result.data) }

            LaunchedEffect(uiState.pendingResolution) {
                uiState.pendingResolution?.let { pendingIntent ->
                    sessionViewModel.dismissResolution()
                    launcher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                }
            }

            SignInScreen(
                uiState = uiState,
                onContinueWithGoogle = sessionViewModel::signInWithGoogle,
                onTryDemo = sessionViewModel::enterDemo,
            )
        }

        composable(Destinations.INBOX) {
            MailRoute(
                repository = mailRepositoryFor(session, container),
                attachmentDownloader = attachmentDownloaderFor(session, container),
                knownSendersRepository = container.knownSendersRepository,
                settingsRepository = container.settingsRepository,
                demoAiProvider = container.demoAiProvider,
                realAiProviderFor = container::anthropicAiProvider,
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                settingsRepository = container.settingsRepository,
                session = session,
                onSignOut = {
                    sessionViewModel.signOut()
                    navController.navigate(Destinations.SIGN_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun mailRepositoryFor(session: SessionState, container: AppContainer): MailRepository =
    if (session is SessionState.SignedIn) container.gmailMailRepository else container.mockMailRepository

private fun attachmentDownloaderFor(session: SessionState, container: AppContainer): AttachmentDownloader =
    if (session is SessionState.SignedIn) container.gmailAttachmentDownloader else container.mockAttachmentDownloader
