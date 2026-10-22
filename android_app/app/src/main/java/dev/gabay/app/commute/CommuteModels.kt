package dev.gabay.app.commute

import uniffi.gabay_core.LatLng

/**
 * Philippine Public Transit Modes
 */
enum class TransitVehicleType {
    TRAIN,    // MRT-3, LRT-1, LRT-2, PNR
    BUS,      // EDSA Carousel, Provincial & City Buses
    FERRY,    // Pasig River Ferry, Inter-island RoRo / Fastcraft
    JEEPNEY,  // Traditional Jeepneys & Modern PUVs / E-Jeeps
    WALK      // First-mile, transfer, and last-mile walking
}

/**
 * Transit Station / Stop Definition
 */
data class TransitStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val lineId: String,
    val lineName: String,
    val vehicleType: TransitVehicleType
)

/**
 * Step in a Multimodal Commute Journey
 */
sealed class CommuteLeg {
    data class Walk(
        val instruction: String,
        val distanceMeters: Double,
        val durationMinutes: Int,
        val polyline: List<LatLng>,
        val targetStationName: String?
    ) : CommuteLeg()

    data class TransitRide(
        val lineId: String,
        val lineName: String,
        val lineHexColor: String,
        val vehicleType: TransitVehicleType,
        val departureStation: String,
        val arrivalStation: String,
        val headsign: String,
        val stopsCount: Int,
        val intermediateStops: List<String>,
        val durationMinutes: Int,
        val distanceMeters: Double,
        val headwayMinutes: Int,
        val fareEstimatePhp: Int,
        val polyline: List<LatLng>
    ) : CommuteLeg()

    data class Transfer(
        val fromLineName: String,
        val toLineName: String,
        val stationName: String,
        val instruction: String,
        val walkDistanceMeters: Double,
        val durationMinutes: Int
    ) : CommuteLeg()
}

/**
 * Visual Pill for Itinerary Timeline Header
 */
data class CommuteSummaryPill(
    val vehicleType: TransitVehicleType,
    val label: String,
    val hexColor: String,
    val durationMinutes: Int
)

/**
 * Complete Multimodal Commute Itinerary
 */
data class CommuteItinerary(
    val id: String,
    val title: String,
    val totalDurationMinutes: Int,
    val totalWalkMeters: Double,
    val totalFarePhp: Int,
    val summaryPills: List<CommuteSummaryPill>,
    val legs: List<CommuteLeg>,
    val fullPolyline: List<LatLng>
)
