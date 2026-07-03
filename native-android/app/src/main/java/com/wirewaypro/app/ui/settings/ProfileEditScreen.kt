package com.wirewaypro.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.SaveTopBar
import com.wirewaypro.app.ui.components.SectionCard

/** Default proposal accent (matches QuotePdfGenerator's built-in blue). */
private const val DEFAULT_ACCENT_HEX = "#3AA9FF"

/** A tasteful set of professional proposal accents the contractor can pick from. */
private val ProposalAccentColors = listOf(
    DEFAULT_ACCENT_HEX, // brand blue (default)
    "#3A57E8",          // deep blue
    "#7B4DF0",          // purple
    "#22C55E",          // green
    "#F5A524",          // amber
    "#E5484D",          // red
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onClose: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes != null) viewModel.uploadLogo(bytes, context.contentResolver.getType(uri))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SaveTopBar(
                title = "Profile & business",
                onBack = onClose,
                onSave = viewModel::save,
                saveEnabled = !state.isSaving,
                saving = state.isSaving,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "You") {
                FormField(state.fullName, viewModel::setFullName, "Your name")
            }

            SectionCard(title = "Business (shown on proposals & PDFs)") {
                FormField(state.companyName, viewModel::setCompanyName, "Business name")
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.companyPhone, viewModel::setCompanyPhone, "Phone", keyboardType = KeyboardType.Phone)
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.companyEmail, viewModel::setCompanyEmail, "Email", keyboardType = KeyboardType.Email)
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.companyLicense, viewModel::setCompanyLicense, "License #")
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.companyAddress, viewModel::setCompanyAddress, "Address")
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.companyWebsite, viewModel::setCompanyWebsite, "Website")
            }

            SectionCard(title = "Business logo") {
                Text(
                    if (state.hasLogo) "Logo on file — shown on your quote PDFs and customer pages."
                    else "Add your logo to brand quote PDFs and customer-facing pages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 12.dp))
                GradientButton(
                    text = if (state.hasLogo) "Replace logo" else "Upload business logo",
                    onClick = { pickLogo.launch("image/*") },
                    loading = state.uploadingLogo,
                    enabled = !state.uploadingLogo,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.logoError?.let { message ->
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (state.logoRetryAvailable && !state.uploadingLogo) {
                        TextButton(onClick = viewModel::retryLogoUpload) { Text("Retry upload") }
                    }
                }
            }

            SectionCard(title = "Proposal accent color") {
                Text(
                    "Sets the accent on your quote & invoice PDFs — match your brand.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProposalAccentColors.forEach { hex ->
                        val selected = state.brandColor.equals(hex, ignoreCase = true) ||
                            (state.brandColor.isBlank() && hex == DEFAULT_ACCENT_HEX)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onBackground
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                )
                                .clickable { viewModel.setBrandColor(hex) },
                        )
                    }
                }
            }

            SectionCard(title = "Baseline rates (your starting point)") {
                FormField(state.hourlyRate, viewModel::setHourlyRate, "Hourly rate $", keyboardType = KeyboardType.Number)
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.flatRate, viewModel::setFlatRate, "Flat-rate baseline $ (optional)", keyboardType = KeyboardType.Number)
                Spacer(Modifier.padding(top = 8.dp))
                Text(
                    "These prefill new quotes and give the AI a starting point. You can always override per quote.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val band = state.rateSuggestion
                if (band != null) {
                    Spacer(Modifier.padding(top = 12.dp))
                    Text(
                        "Typical in ${band.stateName}: $${band.low}\u2013$${band.high}/hr",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Approximate, from public wage data \u2014 your market may vary.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = viewModel::useSuggestedRate) {
                        Text("Use $${band.typical}/hr as my default")
                    }
                }
            }

            SectionCard(title = "Reviews") {
                FormField(state.reviewLink, viewModel::setReviewLink, "Review link (Google/Yelp, optional)")
                Spacer(Modifier.padding(top = 8.dp))
                Text(
                    "After a job is complete, one tap texts your client a review request with this link. More 5-stars = more work.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = "Client financing (pay over time)") {
                FormField(state.financingLink, viewModel::setFinancingLink, "Financing application link (optional)")
                Spacer(Modifier.padding(top = 8.dp))
                Text(
                    "Offering monthly payments helps clients say yes to bigger jobs. Sign up with a financing partner " +
                        "(e.g. Wisetack), paste your application link here, and your proposals will invite clients to apply. " +
                        "Leave blank if you don't offer financing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = "Notifications") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Job & payment alerts", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = state.notificationsEnabled, onCheckedChange = viewModel::setNotifications)
                }
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
