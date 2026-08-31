package com.granify.app.ui.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granify.app.session.SignInUiState
import com.granify.app.ui.components.InfoCardTone
import com.granify.app.ui.components.OumatjieHeroButton
import com.granify.app.ui.components.OumatjieInfoCard
import com.granify.app.ui.components.OumatjieSecondaryButton

@Composable
fun SignInScreen(
    uiState: SignInUiState,
    onContinueWithGoogle: () -> Unit,
    onTryDemo: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Oumatjie",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                // This screen's title. Every other screen's title ("Your mail", "Settings") is a
                // heading; this one was missed by the 2026-08-25 static audit, which meant a
                // TalkBack user navigating by heading skipped the page title entirely and landed
                // on "Just exploring?" below. Covered by AccessibilitySemanticsTest.
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "A simple, safe way to read your Gmail.",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(36.dp))

            if (uiState.isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Connecting to Google…", style = MaterialTheme.typography.titleLarge)
                }
            } else {
                OumatjieHeroButton(label = "Continue with Google", onClick = onContinueWithGoogle)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Oumatjie never sees or stores your Google password.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(20.dp))
                OumatjieInfoCard(tone = InfoCardTone.Problem) {
                    Text(
                        "Oumatjie could not connect to Google.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(uiState.errorMessage, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = "Just exploring?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            OumatjieSecondaryButton(label = "Try the demo inbox", onClick = onTryDemo)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "The demo inbox uses sample messages. It does not connect to a real mailbox.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
