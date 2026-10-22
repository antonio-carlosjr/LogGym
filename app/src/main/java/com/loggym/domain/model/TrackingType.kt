package com.loggym.domain.model

/** Persisted keys are stable across application versions. */
enum class TrackingType(val key: String) {
    WEIGHT_REPS("WEIGHT_REPS"), TIME("TIME");
}
