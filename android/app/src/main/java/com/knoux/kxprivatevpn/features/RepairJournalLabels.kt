package com.knoux.kxprivatevpn.features

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.knoux.kxprivatevpn.R

/**
 * Customer-facing labels for the app-private repair journal. The journal stores stable keys;
 * the UI must translate them and never print the keys themselves.
 */
@Composable
internal fun repairActionLabel(action: String?): String = stringResource(
    when (action) {
        ACTION_CACHE_REMOVE -> R.string.repair_action_cache_remove
        ACTION_GHOST_CLEANUP -> R.string.ghost_action
        ACTION_DUPLICATE_CLEANUP -> R.string.dna_action
        ACTION_AUTOPILOT_RUN -> R.string.repair_action_autopilot_run
        else -> R.string.repair_action_other
    },
)

@Composable
internal fun repairOutcomeLabel(outcome: String?): String = stringResource(
    when (outcome) {
        "COMPLETE" -> R.string.repair_journal_complete
        "PARTIAL" -> R.string.repair_journal_partial
        "FAILED" -> R.string.repair_journal_failed
        else -> R.string.repair_journal_unknown
    },
)

private const val ACTION_CACHE_REMOVE = "cache_remove"
private const val ACTION_GHOST_CLEANUP = "ghost_cleanup"
private const val ACTION_DUPLICATE_CLEANUP = "duplicate_cleanup"
private const val ACTION_AUTOPILOT_RUN = "autopilot_run"
