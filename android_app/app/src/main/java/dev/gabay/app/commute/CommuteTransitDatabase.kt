package dev.gabay.app.commute

import uniffi.gabay_core.LatLng

/**
 * Built-in Registry of Philippine Transit Lines & Multimodal Stations
 * Covers Metro Manila Rail (LRT-1, LRT-2, MRT-3), EDSA Busway Carousel,
 * Pasig River Ferry, and Central Luzon / Provincial Bus & Modern PUV Corridors.
 */
object CommuteTransitDatabase {

    data class TransitLineDef(
        val lineId: String,
        val lineName: String,
        val shortName: String,
        val hexColor: String,
        val vehicleType: TransitVehicleType,
        val headwayMinutes: Int,
        val baseFarePhp: Int,
        val perKmFarePhp: Double,
        val stations: List<TransitStation>
    )

    val lines: List<TransitLineDef> by lazy {
        listOf(
            // 1. MRT-3 (Metro Rail Transit Line 3 - EDSA Line)
            TransitLineDef(
                lineId = "mrt_3",
                lineName = "MRT-3 EDSA Line",
                shortName = "MRT-3",
                hexColor = "#0284C7", // Sky Blue
                vehicleType = TransitVehicleType.TRAIN,
                headwayMinutes = 4,
                baseFarePhp = 13,
                perKmFarePhp = 1.0,
                stations = listOf(
                    TransitStation("mrt3_north_ave", "North Avenue", 14.6533, 121.0331, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_q_ave", "Quezon Avenue", 14.6427, 121.0384, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_kamuning", "GMA-Kamuning", 14.6353, 121.0433, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_cubao", "Araneta Center-Cubao", 14.6195, 121.0526, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_santolan", "Santolan-Annapolis", 14.6079, 121.0564, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_ortigas", "Ortigas", 14.5878, 121.0567, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_shaw", "Shaw Boulevard", 14.5812, 121.0536, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_boni", "Boni", 14.5739, 121.0482, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_guadalupe", "Guadalupe", 14.5673, 121.0456, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_buendia", "Buendia", 14.5540, 121.0345, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_ayala", "Ayala", 14.5491, 121.0280, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_magallanes", "Magallanes", 14.5420, 121.0195, "mrt_3", "MRT-3", TransitVehicleType.TRAIN),
                    TransitStation("mrt3_taft", "Taft Avenue", 14.5376, 121.0014, "mrt_3", "MRT-3", TransitVehicleType.TRAIN)
                )
            ),

            // 2. LRT-1 (Light Rail Transit Line 1 - Green/Yellow Line)
            TransitLineDef(
                lineId = "lrt_1",
                lineName = "LRT-1 Cavite-Monumento",
                shortName = "LRT-1",
                hexColor = "#EAB308", // Solar Amber
                vehicleType = TransitVehicleType.TRAIN,
                headwayMinutes = 4,
                baseFarePhp = 15,
                perKmFarePhp = 1.1,
                stations = listOf(
                    TransitStation("lrt1_fpj", "Fernando Poe Jr. (Roosevelt)", 14.6576, 121.0211, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_balintawak", "Balintawak", 14.6574, 121.0044, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_monumento", "Yamaha Monumento", 14.6543, 120.9839, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_5th_ave", "5th Avenue", 14.6444, 120.9836, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_r_papa", "R. Papa", 14.6361, 120.9825, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_abad_santos", "Abad Santos", 14.6306, 120.9814, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_blumentritt", "Blumentritt", 14.6227, 120.9829, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_tayuman", "Tayuman", 14.6167, 120.9831, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_bambang", "Bambang", 14.6106, 120.9824, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_djose", "Doroteo Jose", 14.6053, 120.9819, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_carriedo", "Carriedo", 14.5997, 120.9814, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_central", "Central Terminal", 14.5928, 120.9817, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_un", "United Nations", 14.5825, 120.9844, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_pedro_gil", "Pedro Gil", 14.5769, 120.9881, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_quirino", "Quirino", 14.5703, 120.9917, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_vito_cruz", "Vito Cruz", 14.5636, 120.9947, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_gil_puyat", "Gil Puyat", 14.5542, 120.9972, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_libertad", "Libertad", 14.5478, 120.9989, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_edsa", "EDSA", 14.5386, 121.0006, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_baclaran", "Baclaran", 14.5342, 120.9983, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_redemptorist", "Redemptorist-Aseana", 14.5245, 120.9942, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_mia", "MIA Road", 14.5165, 120.9928, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_pitx", "Asia World (PITX)", 14.5100, 120.9912, "lrt_1", "LRT-1", TransitVehicleType.TRAIN),
                    TransitStation("lrt1_dr_santos", "Dr. Santos", 14.4950, 120.9860, "lrt_1", "LRT-1", TransitVehicleType.TRAIN)
                )
            ),

            // 3. LRT-2 (Light Rail Transit Line 2 - Purple Line)
            TransitLineDef(
                lineId = "lrt_2",
                lineName = "LRT-2 East-West Line",
                shortName = "LRT-2",
                hexColor = "#A855F7", // Purple
                vehicleType = TransitVehicleType.TRAIN,
                headwayMinutes = 6,
                baseFarePhp = 15,
                perKmFarePhp = 1.1,
                stations = listOf(
                    TransitStation("lrt2_recto", "Recto", 14.6036, 120.9817, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_legarda", "Legarda", 14.6008, 120.9925, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_pureza", "Pureza", 14.6017, 121.0050, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_vmapa", "V. Mapa", 14.6042, 121.0172, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_jruiz", "J. Ruiz", 14.6106, 121.0261, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_gilmore", "Gilmore", 14.6136, 121.0342, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_betty_go", "Betty Go-Belmonte", 14.6186, 121.0428, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_cubao", "Araneta Center-Cubao", 14.6225, 121.0531, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_anonas", "Anonas", 14.6281, 121.0647, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_katipunan", "Katipunan", 14.6322, 121.0725, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_santolan", "Santolan", 14.6220, 121.0858, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_marikina", "Marikina-Pasig", 14.6206, 121.1000, "lrt_2", "LRT-2", TransitVehicleType.TRAIN),
                    TransitStation("lrt2_antipolo", "Antipolo", 14.6247, 121.1214, "lrt_2", "LRT-2", TransitVehicleType.TRAIN)
                )
            ),

            // 4. EDSA Busway (EDSA Carousel)
            TransitLineDef(
                lineId = "edsa_carousel",
                lineName = "EDSA Busway Carousel",
                shortName = "EDSA Busway",
                hexColor = "#10B981", // Emerald Green
                vehicleType = TransitVehicleType.BUS,
                headwayMinutes = 3,
                baseFarePhp = 15,
                perKmFarePhp = 1.8,
                stations = listOf(
                    TransitStation("busway_monumento", "Monumento", 14.6545, 120.9840, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_bagong_barrio", "Bagong Barrio", 14.6572, 120.9961, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_balintawak", "Balintawak", 14.6575, 121.0040, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_kaingin", "Kaingin Road", 14.6560, 121.0142, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_roosevelt", "Roosevelt", 14.6550, 121.0210, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_north_ave", "North Avenue", 14.6530, 121.0328, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_q_ave", "Quezon Avenue", 14.6425, 121.0380, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_q_mart", "Mega Q-Mart", 14.6289, 121.0486, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_main_ave", "Main Avenue Cubao", 14.6150, 121.0544, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_santolan", "Santolan", 14.6075, 121.0560, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_ortigas", "Ortigas", 14.5875, 121.0565, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_guadalupe", "Guadalupe", 14.5670, 121.0450, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_buendia", "Buendia", 14.5538, 121.0340, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_one_ayala", "One Ayala", 14.5488, 121.0285, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_tramo", "Tramo", 14.5425, 121.0120, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_taft", "Taft Avenue", 14.5372, 121.0010, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_roxas", "Roxas Boulevard", 14.5320, 120.9920, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_moa", "Macapagal / MOA", 14.5345, 120.9830, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS),
                    TransitStation("busway_pitx", "PITX Terminal", 14.5090, 120.9910, "edsa_carousel", "EDSA Busway", TransitVehicleType.BUS)
                )
            ),

            // 5. Pasig River Ferry Service
            TransitLineDef(
                lineId = "pasig_ferry",
                lineName = "Pasig River Ferry",
                shortName = "PRFS Ferry",
                hexColor = "#06B6D4", // River Cyan
                vehicleType = TransitVehicleType.FERRY,
                headwayMinutes = 20,
                baseFarePhp = 0, // Currently free / subsidized MMDA
                perKmFarePhp = 0.0,
                stations = listOf(
                    TransitStation("ferry_escolta", "Escolta Terminal", 14.5969, 120.9781, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_lawton", "Lawton Terminal", 14.5947, 120.9806, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_quinta", "Quinta Market", 14.5981, 120.9850, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_pup", "PUP Sta. Mesa", 14.5978, 121.0111, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_sta_ana", "Sta. Ana", 14.5806, 121.0150, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_lambingan", "Lambingan", 14.5828, 121.0264, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_valenzuela", "Valenzuela", 14.5764, 121.0319, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_hulo", "Hulo", 14.5711, 121.0381, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_guadalupe", "Guadalupe Ferry Station", 14.5683, 121.0461, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_san_joaquin", "San Joaquin", 14.5528, 121.0742, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_maybunga", "Maybunga", 14.5694, 121.0858, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY),
                    TransitStation("ferry_pinagbuhatan", "Pinagbuhatan Terminal", 14.5489, 121.0964, "pasig_ferry", "PRFS Ferry", TransitVehicleType.FERRY)
                )
            ),

            // 6. Central Luzon Provincial Bus Corridor (Cabanatuan - Gapan - Manila)
            TransitLineDef(
                lineId = "cabanatuan_gapan_bus",
                lineName = "Maharlika Provincial Bus (Baliwag / Five Star)",
                shortName = "Provincial Bus",
                hexColor = "#F97316", // Orange
                vehicleType = TransitVehicleType.BUS,
                headwayMinutes = 10,
                baseFarePhp = 20,
                perKmFarePhp = 2.0,
                stations = listOf(
                    TransitStation("bus_cabanatuan", "Cabanatuan Central Terminal", 15.4880, 120.9730, "cabanatuan_gapan_bus", "Provincial Bus", TransitVehicleType.BUS),
                    TransitStation("bus_santa_rosa", "Santa Rosa Town Center", 15.4240, 120.9400, "cabanatuan_gapan_bus", "Provincial Bus", TransitVehicleType.BUS),
                    TransitStation("bus_san_leonardo", "San Leonardo Crossing", 15.3620, 120.9570, "cabanatuan_gapan_bus", "Provincial Bus", TransitVehicleType.BUS),
                    TransitStation("bus_gapan", "Gapan City Central Terminal", 15.3075, 120.9472, "cabanatuan_gapan_bus", "Provincial Bus", TransitVehicleType.BUS),
                    TransitStation("bus_san_miguel", "San Miguel Bulacan Terminal", 15.1450, 120.9750, "cabanatuan_gapan_bus", "Provincial Bus", TransitVehicleType.BUS),
                    TransitStation("bus_baliwag", "Baliwag Transit Central", 14.9540, 120.9020, "cabanatuan_gapan_bus", "Provincial Bus", TransitVehicleType.BUS)
                )
            ),

            // 7. Gapan - San Isidro - Cabiao Modern PUV / Jeepney Corridor
            TransitLineDef(
                lineId = "gapan_san_isidro_puv",
                lineName = "Gapan-San Isidro-Cabiao Modern PUV",
                shortName = "Jeepney / PUV",
                hexColor = "#F59E0B", // Amber
                vehicleType = TransitVehicleType.JEEPNEY,
                headwayMinutes = 5,
                baseFarePhp = 15,
                perKmFarePhp = 1.8,
                stations = listOf(
                    TransitStation("puv_gapan", "Gapan Public Market Terminal", 15.3075, 120.9472, "gapan_san_isidro_puv", "Modern PUV", TransitVehicleType.JEEPNEY),
                    TransitStation("puv_san_isidro_hall", "San Isidro Municipal Hall", 15.3090, 120.9025, "gapan_san_isidro_puv", "Modern PUV", TransitVehicleType.JEEPNEY),
                    TransitStation("puv_san_isidro_pob", "San Isidro Poblacion Crossing", 15.3080, 120.9010, "gapan_san_isidro_puv", "Modern PUV", TransitVehicleType.JEEPNEY),
                    TransitStation("puv_neust", "NEUST San Isidro Campus", 15.3120, 120.8985, "gapan_san_isidro_puv", "Modern PUV", TransitVehicleType.JEEPNEY),
                    TransitStation("puv_cabiao", "Cabiao Town Plaza / Public Market", 15.2520, 120.8560, "gapan_san_isidro_puv", "Modern PUV", TransitVehicleType.JEEPNEY),
                    TransitStation("puv_arayat", "Arayat Town Center", 15.1500, 120.7680, "gapan_san_isidro_puv", "Modern PUV", TransitVehicleType.JEEPNEY)
                )
            )
        )
    }

    /**
     * Finds nearest transit stations within maxRadiusMeters
     */
    fun findNearbyStations(lat: Double, lon: Double, maxRadiusMeters: Double = 3500.0): List<Pair<TransitStation, Double>> {
        val matches = mutableListOf<Pair<TransitStation, Double>>()
        for (line in lines) {
            for (st in line.stations) {
                val d = haversineDistanceMeters(lat, lon, st.latitude, st.longitude)
                if (d <= maxRadiusMeters) {
                    matches.add(Pair(st, d))
                }
            }
        }
        return matches.sortedBy { it.second }
    }

    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
