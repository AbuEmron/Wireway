package com.wirewaypro.app.ui.crew

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.CrewMember
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.UpgradePrompt
import com.wirewaypro.app.ui.components.rememberWirewayHaptics
import com.wirewaypro.app.ui.components.riseIn
import com.wirewaypro.app.ui.util.Format

/**
 * The Elite crew roster: add/edit/deactivate crew members and their hourly cost
 * rate — the cost side of true job costing. Free/Pro see the feature's value
 * behind a contextual Elite prompt (never a nag wall), per WIREWAY_PRICING_TIERS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewScreen(
    onBack: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: CrewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberWirewayHaptics()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title = "Crew", onBack = onBack) },
        floatingActionButton = {
            if (state.isElite && !state.isLoading) {
                FloatingActionButton(
                    onClick = { haptics.tap(); viewModel.addCrew() },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) { Icon(Icons.Filled.Add, contentDescription = "Add crew member") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                state.isLoading -> {
                    Spacer(Modifier.height(48.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }

                !state.isElite -> CrewUpsell(onOpenSubscription = onOpenSubscription)

                else -> {
                    Text(
                        "Your crew and what each person costs you per hour. Logging their " +
                            "hours on a job turns into real labor cost in Job Costing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.error?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    if (state.crew.isEmpty()) {
                        SectionCard {
                            Text(
                                "No crew yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Add your first crew member — name, role, and their hourly cost — " +
                                    "then log their hours against a job.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        state.crew.forEachIndexed { i, member ->
                            CrewRow(
                                member = member,
                                modifier = Modifier.riseIn(i),
                                onClick = { haptics.tap(); viewModel.editCrew(member) },
                            )
                        }
                    }
                }
            }
        }
    }

    state.editing?.let { draft ->
        CrewEditorDialog(
            draft = draft,
            onChange = viewModel::updateDraft,
            onSave = viewModel::saveCrew,
            onDelete = draft.id?.let { id -> { viewModel.deleteCrew(id) } },
            onDismiss = viewModel::closeEditor,
        )
    }
}

@Composable
private fun CrewRow(member: CrewMember, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Groups,
                contentDescription = null,
                tint = if (member.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(start = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val sub = buildString {
                    member.role?.takeIf { it.isNotBlank() }?.let { append(it) }
                    if (!member.active) {
                        if (isNotEmpty()) append(" · ")
                        append("Inactive")
                    }
                }
                if (sub.isNotBlank()) {
                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${Format.money(member.hourlyCostRate)}/hr",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text("cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The contextual Free/Pro → Elite moment for the crew feature. */
@Composable
private fun CrewUpsell(onOpenSubscription: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(start = 10.dp))
            Text("Bring on a helper?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Add your crew with each person's hourly cost, then log their hours against a " +
                "job. Wireway turns those hours into real labor cost so you see true profit " +
                "— actual labor and materials vs. what you estimated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(14.dp))
    UpgradePrompt(
        hook = "Crew time tracking & true job costing",
        detail = "See exactly where labor and material dollars went on every job, so your " +
            "next bid is sharper.",
        tier = Tier.ELITE,
        onUpgrade = onOpenSubscription,
    )
}

@Composable
private fun CrewEditorDialog(
    draft: CrewDraft,
    onChange: ((CrewDraft) -> CrewDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == null) "New crew member" else "Edit crew member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField(draft.name, { v -> onChange { it.copy(name = v) } }, "Name")
                FormField(draft.role, { v -> onChange { it.copy(role = v) } }, "Role (e.g. Journeyman)")
                FormField(
                    draft.hourlyCostRate,
                    { v -> onChange { it.copy(hourlyCostRate = v) } },
                    "Hourly cost $ (what you pay)",
                    keyboardType = KeyboardType.Decimal,
                    supportingText = "Your cost per hour — never shown to the client.",
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Active", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Inactive crew stay in history but hide from pickers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = draft.active, onCheckedChange = { v -> onChange { it.copy(active = v) } })
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = draft.name.isNotBlank()) { Text("Save") } },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
