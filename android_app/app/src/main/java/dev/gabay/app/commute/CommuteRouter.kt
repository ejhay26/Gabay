package dev.gabay.app.commute

import uniffi.gabay_core.LatLng
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Multimodal Commute Routing Engine
 * Replicates Google Maps public transit directions (Walk -> Bus/Train/Ferry -> Transfer -> Walk)
 * tailored for Philippine transportation networks.
 */
object CommuteRouter {

    /**
     * Calculates public transit commute options between origin and destination
     */
    fun planCommute(origin: LatLng, destination: LatLng): List<CommuteItinerary> {
        val totalDirectDist = CommuteTransitDatabase.haversineDistanceMeters(
            origin.latitude, origin.longitude, destination.latitude, destination.longitude
        )

        val itineraries = mutableListOf<CommuteItinerary>()

        // 1. Direct Walk option (always available if < 2.5 km)
        if (totalDirectDist <= 2500.0) {
            val walkMins = (totalDirectDist / (1.33 * 60.0)).roundToInt().coerceAtLeast(1)
            val walkPoly = listOf(origin, destination)
            itineraries.add(
                CommuteItinerary(
                    id = "commute_walk_direct",
                    title = "Walk directly",
                    totalDurationMinutes = walkMins,
                    totalWalkMeters = totalDirectDist,
                    totalFarePhp = 0,
                    summaryPills = listOf(
                        CommuteSummaryPill(TransitVehicleType.WALK, "Walk", "#94A3B8", walkMins)
                    ),
                    legs = listOf(
                        CommuteLeg.Walk(
                            instruction = "Walk to destination (${formatDist(totalDirectDist)})",
                            distanceMeters = totalDirectDist,
                            durationMinutes = walkMins,
                            polyline = walkPoly,
                            targetStationName = null
                        )
                    ),
                    fullPolyline = walkPoly
                )
            )
        }

        // 2. Scan nearby transit stations to origin and destination
        val originStations = CommuteTransitDatabase.findNearbyStations(origin.latitude, origin.longitude, 3500.0)
        val destStations = CommuteTransitDatabase.findNearbyStations(destination.latitude, destination.longitude, 3500.0)

        // 3. Find Direct Transit Lines (same line)
        for ((origSt, origDist) in originStations.take(6)) {
            for ((destSt, destDist) in destStations.take(6)) {
                if (origSt.lineId == destSt.lineId && origSt.id != destSt.id) {
                    val line = CommuteTransitDatabase.lines.firstOrNull { it.lineId == origSt.lineId } ?: continue
                    val fromIdx = line.stations.indexOfFirst { it.id == origSt.id }
                    val toIdx = line.stations.indexOfFirst { it.id == destSt.id }

                    if (fromIdx != -1 && toIdx != -1) {
                        val slice = if (fromIdx < toIdx) {
                            line.stations.subList(fromIdx, toIdx + 1)
                        } else {
                            line.stations.subList(toIdx, fromIdx + 1).reversed()
                        }

                        val stopsCount = (fromIdx - toIdx).let { if (it < 0) -it else it }
                        var rideDistM = 0.0
                        for (i in 0 until slice.size - 1) {
                            rideDistM += CommuteTransitDatabase.haversineDistanceMeters(
                                slice[i].latitude, slice[i].longitude,
                                slice[i + 1].latitude, slice[i + 1].longitude
                            )
                        }

                        // Speeds: Train ~35 km/h (9.7 m/s), Bus/PUV ~25 km/h (6.9 m/s), Ferry ~20 km/h (5.5 m/s)
                        val speedMps = when (line.vehicleType) {
                            TransitVehicleType.TRAIN -> 9.7
                            TransitVehicleType.BUS -> 6.9
                            TransitVehicleType.FERRY -> 5.5
                            TransitVehicleType.JEEPNEY -> 6.0
                            TransitVehicleType.WALK -> 1.33
                        }

                        val rideMins = (rideDistM / (speedMps * 60.0)).roundToInt().coerceAtLeast(2)
                        val walk1Mins = (origDist / (1.33 * 60.0)).roundToInt().coerceAtLeast(1)
                        val walk2Mins = (destDist / (1.33 * 60.0)).roundToInt().coerceAtLeast(1)
                        val totalMins = walk1Mins + rideMins + walk2Mins
                        val fare = (line.baseFarePhp + (rideDistM / 1000.0) * line.perKmFarePhp).roundToInt()

                        val polyline = mutableListOf<LatLng>()
                        polyline.add(origin)
                        for (st in slice) {
                            polyline.add(LatLng(latitude = st.latitude, longitude = st.longitude))
                        }
                        polyline.add(destination)

                        val intermediateNames = slice.drop(1).dropLast(1).map { it.name }
                        val headsign = line.stations.last().name

                        val itin = CommuteItinerary(
                            id = "direct_${line.lineId}_${origSt.id}_${destSt.id}",
                            title = "Via ${line.shortName}",
                            totalDurationMinutes = totalMins,
                            totalWalkMeters = origDist + destDist,
                            totalFarePhp = fare,
                            summaryPills = listOf(
                                CommuteSummaryPill(TransitVehicleType.WALK, "$walk1Mins min", "#94A3B8", walk1Mins),
                                CommuteSummaryPill(line.vehicleType, line.shortName, line.hexColor, rideMins),
                                CommuteSummaryPill(TransitVehicleType.WALK, "$walk2Mins min", "#94A3B8", walk2Mins)
                            ),
                            legs = listOf(
                                CommuteLeg.Walk(
                                    instruction = "Walk to ${origSt.name} (${formatDist(origDist)})",
                                    distanceMeters = origDist,
                                    durationMinutes = walk1Mins,
                                    polyline = listOf(origin, LatLng(origSt.latitude, origSt.longitude)),
                                    targetStationName = origSt.name
                                ),
                                CommuteLeg.TransitRide(
                                    lineId = line.lineId,
                                    lineName = line.lineName,
                                    lineHexColor = line.hexColor,
                                    vehicleType = line.vehicleType,
                                    departureStation = origSt.name,
                                    arrivalStation = destSt.name,
                                    headsign = headsign,
                                    stopsCount = stopsCount,
                                    intermediateStops = intermediateNames,
                                    durationMinutes = rideMins,
                                    distanceMeters = rideDistM,
                                    headwayMinutes = line.headwayMinutes,
                                    fareEstimatePhp = fare,
                                    polyline = slice.map { LatLng(it.latitude, it.longitude) }
                                ),
                                CommuteLeg.Walk(
                                    instruction = "Walk to destination (${formatDist(destDist)})",
                                    distanceMeters = destDist,
                                    durationMinutes = walk2Mins,
                                    polyline = listOf(LatLng(destSt.latitude, destSt.longitude), destination),
                                    targetStationName = null
                                )
                            ),
                            fullPolyline = polyline
                        )
                        itineraries.add(itin)
                    }
                }
            }
        }

        // 4. Find 1-Transfer Multimodal Connections (e.g. MRT-3 <-> LRT-2 at Cubao, or MRT-3 <-> LRT-1 at EDSA/Taft)
        if (itineraries.isEmpty() || itineraries.size < 3) {
            val transferHubs = listOf(
                Pair("mrt3_cubao", "lrt2_cubao"),         // Araneta Center-Cubao Link
                Pair("mrt3_taft", "lrt1_edsa"),           // EDSA / Pasay Taft Link
                Pair("lrt1_monumento", "busway_monumento"),// Monumento Hub
                Pair("mrt3_guadalupe", "ferry_guadalupe"), // Guadalupe River Ferry Hub
                Pair("mrt3_north_ave", "lrt1_fpj"),       // North Ave Grand Central
                Pair("bus_gapan", "puv_gapan")            // Gapan Central Hub
            )

            for ((hub1Id, hub2Id) in transferHubs) {
                val hub1 = findStationById(hub1Id) ?: continue
                val hub2 = findStationById(hub2Id) ?: continue

                for ((origSt, origDist) in originStations.take(3)) {
                    if (origSt.lineId == hub1.lineId && origSt.id != hub1.id) {
                        for ((destSt, destDist) in destStations.take(3)) {
                            if (destSt.lineId == hub2.lineId && destSt.id != hub2.id) {
                                val line1 = CommuteTransitDatabase.lines.first { it.lineId == origSt.lineId }
                                val line2 = CommuteTransitDatabase.lines.first { it.lineId == destSt.lineId }

                                val slice1 = getStationSlice(line1, origSt.id, hub1.id)
                                val slice2 = getStationSlice(line2, hub2.id, destSt.id)

                                if (slice1.isNotEmpty() && slice2.isNotEmpty()) {
                                    val dist1 = calculateSliceDist(slice1)
                                    val dist2 = calculateSliceDist(slice2)
                                    val transferWalkDist = CommuteTransitDatabase.haversineDistanceMeters(
                                        hub1.latitude, hub1.longitude, hub2.latitude, hub2.longitude
                                    ).coerceAtLeast(60.0)

                                    val mins1 = (dist1 / (8.5 * 60.0)).roundToInt().coerceAtLeast(3)
                                    val mins2 = (dist2 / (8.5 * 60.0)).roundToInt().coerceAtLeast(3)
                                    val walk1 = (origDist / (1.33 * 60.0)).roundToInt().coerceAtLeast(1)
                                    val walk2 = (destDist / (1.33 * 60.0)).roundToInt().coerceAtLeast(1)
                                    val transferWalkMins = (transferWalkDist / (1.33 * 60.0)).roundToInt().coerceAtLeast(2)

                                    val totalMins = walk1 + mins1 + transferWalkMins + mins2 + walk2
                                    val totalFare = (line1.baseFarePhp + 10) + (line2.baseFarePhp + 10)

                                    val polyline = mutableListOf<LatLng>()
                                    polyline.add(origin)
                                    slice1.forEach { polyline.add(LatLng(it.latitude, it.longitude)) }
                                    slice2.forEach { polyline.add(LatLng(it.latitude, it.longitude)) }
                                    polyline.add(destination)

                                    val itin = CommuteItinerary(
                                        id = "transfer_${origSt.id}_${hub1.id}_${destSt.id}",
                                        title = "${line1.shortName} ➔ ${line2.shortName}",
                                        totalDurationMinutes = totalMins,
                                        totalWalkMeters = origDist + transferWalkDist + destDist,
                                        totalFarePhp = totalFare,
                                        summaryPills = listOf(
                                            CommuteSummaryPill(TransitVehicleType.WALK, "$walk1 min", "#94A3B8", walk1),
                                            CommuteSummaryPill(line1.vehicleType, line1.shortName, line1.hexColor, mins1),
                                            CommuteSummaryPill(TransitVehicleType.WALK, "$transferWalkMins min", "#94A3B8", transferWalkMins),
                                            CommuteSummaryPill(line2.vehicleType, line2.shortName, line2.hexColor, mins2),
                                            CommuteSummaryPill(TransitVehicleType.WALK, "$walk2 min", "#94A3B8", walk2)
                                        ),
                                        legs = listOf(
                                            CommuteLeg.Walk(
                                                instruction = "Walk to ${origSt.name} (${formatDist(origDist)})",
                                                distanceMeters = origDist,
                                                durationMinutes = walk1,
                                                polyline = listOf(origin, LatLng(origSt.latitude, origSt.longitude)),
                                                targetStationName = origSt.name
                                            ),
                                            CommuteLeg.TransitRide(
                                                lineId = line1.lineId,
                                                lineName = line1.lineName,
                                                lineHexColor = line1.hexColor,
                                                vehicleType = line1.vehicleType,
                                                departureStation = origSt.name,
                                                arrivalStation = hub1.name,
                                                headsign = line1.stations.last().name,
                                                stopsCount = slice1.size - 1,
                                                intermediateStops = slice1.drop(1).dropLast(1).map { it.name },
                                                durationMinutes = mins1,
                                                distanceMeters = dist1,
                                                headwayMinutes = line1.headwayMinutes,
                                                fareEstimatePhp = line1.baseFarePhp + 10,
                                                polyline = slice1.map { LatLng(it.latitude, it.longitude) }
                                            ),
                                            CommuteLeg.Transfer(
                                                fromLineName = line1.shortName,
                                                toLineName = line2.shortName,
                                                stationName = hub1.name,
                                                instruction = "Transfer from ${line1.shortName} to ${line2.shortName} at ${hub1.name} (${formatDist(transferWalkDist)})",
                                                walkDistanceMeters = transferWalkDist,
                                                durationMinutes = transferWalkMins
                                            ),
                                            CommuteLeg.TransitRide(
                                                lineId = line2.lineId,
                                                lineName = line2.lineName,
                                                lineHexColor = line2.hexColor,
                                                vehicleType = line2.vehicleType,
                                                departureStation = hub2.name,
                                                arrivalStation = destSt.name,
                                                headsign = line2.stations.last().name,
                                                stopsCount = slice2.size - 1,
                                                intermediateStops = slice2.drop(1).dropLast(1).map { it.name },
                                                durationMinutes = mins2,
                                                distanceMeters = dist2,
                                                headwayMinutes = line2.headwayMinutes,
                                                fareEstimatePhp = line2.baseFarePhp + 10,
                                                polyline = slice2.map { LatLng(it.latitude, it.longitude) }
                                            ),
                                            CommuteLeg.Walk(
                                                instruction = "Walk to destination (${formatDist(destDist)})",
                                                distanceMeters = destDist,
                                                durationMinutes = walk2,
                                                polyline = listOf(LatLng(destSt.latitude, destSt.longitude), destination),
                                                targetStationName = null
                                            )
                                        ),
                                        fullPolyline = polyline
                                    )
                                    itineraries.add(itin)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sort itineraries by total duration
        return itineraries.sortedBy { it.totalDurationMinutes }
    }

    private fun findStationById(id: String): TransitStation? {
        for (line in CommuteTransitDatabase.lines) {
            for (st in line.stations) {
                if (st.id == id) return st
            }
        }
        return null
    }

    private fun getStationSlice(line: CommuteTransitDatabase.TransitLineDef, fromId: String, toId: String): List<TransitStation> {
        val fromIdx = line.stations.indexOfFirst { it.id == fromId }
        val toIdx = line.stations.indexOfFirst { it.id == toId }
        if (fromIdx == -1 || toIdx == -1) return emptyList()
        return if (fromIdx <= toIdx) {
            line.stations.subList(fromIdx, toIdx + 1)
        } else {
            line.stations.subList(toIdx, fromIdx + 1).reversed()
        }
    }

    private fun calculateSliceDist(slice: List<TransitStation>): Double {
        var d = 0.0
        for (i in 0 until slice.size - 1) {
            d += CommuteTransitDatabase.haversineDistanceMeters(
                slice[i].latitude, slice[i].longitude,
                slice[i + 1].latitude, slice[i + 1].longitude
            )
        }
        return d
    }

    private fun formatDist(meters: Double): String {
        return if (meters >= 1000.0) {
            String.format(Locale.US, "%.1f km", meters / 1000.0)
        } else {
            "${meters.roundToInt()}m"
        }
    }
}
