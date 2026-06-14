package com.example.a211198_hasif_drnelson_Project2.view_model

import com.example.a211198_hasif_drnelson_Project2.model.RunRoute

// State of the Home "Plan Your Weekend Run" section.
sealed interface WeekendRunUiState {
    data object Loading : WeekendRunUiState
    data class Success(val routes: List<RunRoute>) : WeekendRunUiState   // live Foursquare data
    data class Fallback(val routes: List<RunRoute>) : WeekendRunUiState  // offline/unconfigured samples
}
