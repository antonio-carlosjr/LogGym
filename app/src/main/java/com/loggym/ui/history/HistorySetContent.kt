package com.loggym.ui.history

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.loggym.R
import com.loggym.domain.logic.WeightFormat
import com.loggym.domain.model.SessionSet
import com.loggym.domain.model.TrackingType

@Composable
internal fun HistorySetContent(number: Int, set: SessionSet, type: TrackingType) {
    Text(stringResource(R.string.set_number, number), style = MaterialTheme.typography.titleSmall)
    if (type == TrackingType.TIME) {
        Text(if (set.durationSeconds == null) stringResource(R.string.history_set_duration_missing)
            else stringResource(R.string.history_set_duration, set.durationSeconds.toString()))
    } else {
        Text(if (set.weightKg == null) stringResource(R.string.history_set_weight_missing)
            else stringResource(R.string.history_set_weight, WeightFormat.format(set.weightKg)))
        Text(if (set.reps == null) stringResource(R.string.history_set_reps_missing)
            else stringResource(R.string.history_set_reps, set.reps.toString()))
    }
}
