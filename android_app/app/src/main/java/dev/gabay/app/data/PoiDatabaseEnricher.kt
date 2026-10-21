package dev.gabay.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

object PoiDatabaseEnricher {
    private const val TAG = "PoiEnricher"
    private const val PREFS_NAME = "gabay_poi_enricher_prefs"
    private const val KEY_ENRICHED_VERSION = "enriched_version_key"
    private const val CURRENT_VERSION = 2

    private val RICH_POIS = listOf(
        // --- UNIVERSITIES, COLLEGES & INSTITUTES ---
        Triple("NEUST - San Isidro Campus", "SCHOOL", Pair(15.3101, 120.9038)),
        Triple("NEUST Main Campus Cabanatuan", "UNIVERSITY", Pair(15.4855, 120.9712)),
        Triple("NEUST Atate Campus", "COLLEGE", Pair(15.4612, 121.0345)),
        Triple("NEUST Sumacab Campus", "UNIVERSITY", Pair(15.4418, 120.9482)),
        Triple("General de Jesus College", "COLLEGE", Pair(15.3116, 120.9042)),
        Triple("Central Luzon State University (CLSU)", "UNIVERSITY", Pair(15.7428, 120.9388)),
        Triple("Wesleyan University-Philippines", "UNIVERSITY", Pair(15.4802, 120.9634)),
        Triple("Araullo University - PHINMA", "UNIVERSITY", Pair(15.4682, 120.9571)),
        Triple("Manuel V. Gallego Foundation Colleges", "COLLEGE", Pair(15.4883, 120.9678)),
        Triple("College of the Immaculate Conception", "COLLEGE", Pair(15.4891, 120.9734)),
        Triple("Midway Maritime Foundation", "COLLEGE", Pair(15.4925, 120.9680)),
        Triple("Good Samaritan Colleges", "COLLEGE", Pair(15.4812, 120.9611)),
        Triple("Holy Rosary Colleges Foundation", "COLLEGE", Pair(15.5843, 120.9234)),
        Triple("Baliuag University", "UNIVERSITY", Pair(14.9542, 120.9015)),
        Triple("Bulacan State University - Bustos", "UNIVERSITY", Pair(14.9567, 120.9234)),
        Triple("Bulacan Agricultural State College (BASC)", "COLLEGE", Pair(15.0789, 120.9412)),
        Triple("Dr. Gloria D. Lacson Foundation Colleges", "COLLEGE", Pair(15.3638, 120.9625)),
        Triple("La Fortuna College", "COLLEGE", Pair(15.4842, 120.9685)),
        Triple("STI College Cabanatuan", "COLLEGE", Pair(15.4820, 120.9650)),
        Triple("AMA Computer College Cabanatuan", "COLLEGE", Pair(15.4760, 120.9590)),

        // --- HIGH SCHOOLS & ELEMENTARY SCHOOLS ---
        Triple("San Isidro Central School", "SCHOOL", Pair(15.3108, 120.9029)),
        Triple("Juan R. Liwag Memorial High School", "SCHOOL", Pair(15.2974, 120.9556)),
        Triple("Gapan South Central School", "SCHOOL", Pair(15.2982, 120.9483)),
        Triple("San Roque National High School", "SCHOOL", Pair(15.2891, 120.9123)),
        Triple("San Roque Elementary School", "SCHOOL", Pair(15.2898, 120.9130)),
        Triple("Alua National High School", "SCHOOL", Pair(15.3245, 120.8934)),
        Triple("Alua Elementary School", "SCHOOL", Pair(15.3252, 120.8940)),
        Triple("Pambuan Elementary School", "SCHOOL", Pair(15.3312, 120.9189)),
        Triple("Pambuan National High School", "SCHOOL", Pair(15.3320, 120.9195)),
        Triple("Malapit National High School", "SCHOOL", Pair(15.3189, 120.9082)),
        Triple("Malapit Elementary School", "SCHOOL", Pair(15.3195, 120.9090)),
        Triple("Sto. Cristo Elementary School", "SCHOOL", Pair(15.3056, 120.9145)),
        Triple("Calaba Elementary School", "SCHOOL", Pair(15.2985, 120.8975)),
        Triple("Manggang Marikit Elementary School", "SCHOOL", Pair(15.3170, 120.9250)),
        Triple("Cabiao National High School", "SCHOOL", Pair(15.2489, 120.8543)),
        Triple("Cabiao Central School", "SCHOOL", Pair(15.2475, 120.8525)),
        Triple("San Leonardo National High School", "SCHOOL", Pair(15.3621, 120.9612)),
        Triple("San Leonardo Central School", "SCHOOL", Pair(15.3610, 120.9595)),
        Triple("Jaen National High School", "SCHOOL", Pair(15.3392, 120.9087)),
        Triple("Jaen Central School", "SCHOOL", Pair(15.3380, 120.9075)),
        Triple("Peñaranda National High School", "SCHOOL", Pair(15.3541, 121.0023)),
        Triple("Peñaranda Central School", "SCHOOL", Pair(15.3530, 121.0010)),
        Triple("Santa Rosa National High School", "SCHOOL", Pair(15.4215, 120.9389)),
        Triple("Divina Pastora College", "SCHOOL", Pair(15.2988, 120.9490)),
        Triple("San Miguel National High School", "SCHOOL", Pair(15.1432, 120.9745)),

        // --- HOSPITALS & HEALTHCARE ---
        Triple("Dr. Gloria D. Lacson General Hospital", "HOSPITAL", Pair(15.3645, 120.9632)),
        Triple("Gapan District Hospital", "HOSPITAL", Pair(15.2954, 120.9512)),
        Triple("San Isidro Rural Health Unit (RHU)", "HOSPITAL", Pair(15.3112, 120.9031)),
        Triple("San Isidro Medicare Hospital", "HOSPITAL", Pair(15.3120, 120.9050)),
        Triple("Nueva Ecija Doctors Hospital", "HOSPITAL", Pair(15.4789, 120.9645)),
        Triple("Premiere General Hospital of Nueva Ecija", "HOSPITAL", Pair(15.4823, 120.9689)),
        Triple("Paulino J. Garcia Memorial Research Hospital (PJGMRMC)", "HOSPITAL", Pair(15.4912, 120.9789)),
        Triple("Cabanatuan City Hospital", "HOSPITAL", Pair(15.4856, 120.9723)),
        Triple("Good Samaritan Hospital Cabanatuan", "HOSPITAL", Pair(15.4815, 120.9615)),
        Triple("Gonzaga-Hernandez Memorial Hospital", "HOSPITAL", Pair(15.3612, 120.9589)),
        Triple("Cabiao Rural Health Unit", "HOSPITAL", Pair(15.2478, 120.8532)),
        Triple("Jaen Rural Health Unit", "HOSPITAL", Pair(15.3385, 120.9079)),
        Triple("San Leonardo Rural Health Unit", "HOSPITAL", Pair(15.3605, 120.9602)),
        Triple("Peñaranda Rural Health Unit", "HOSPITAL", Pair(15.3535, 121.0015)),
        Triple("Baliwag District Hospital", "HOSPITAL", Pair(14.9523, 120.9045)),
        Triple("Castro Maternity and General Hospital", "HOSPITAL", Pair(14.9512, 120.9021)),
        Triple("Mercury Drug San Isidro", "PHARMACY", Pair(15.3118, 120.9036)),
        Triple("Mercury Drug Gapan Bayan", "PHARMACY", Pair(15.2972, 120.9498)),
        Triple("Southstar Drug Gapan", "PHARMACY", Pair(15.2965, 120.9505)),

        // --- GOVERNMENT & CIVIC OFFICES ---
        Triple("San Isidro Municipal Hall", "GOVERNMENT", Pair(15.3110, 120.9035)),
        Triple("Gapan City Hall", "GOVERNMENT", Pair(15.2991, 120.9498)),
        Triple("San Leonardo Municipal Hall", "GOVERNMENT", Pair(15.3618, 120.9608)),
        Triple("Cabiao Municipal Hall", "GOVERNMENT", Pair(15.2482, 120.8539)),
        Triple("Jaen Municipal Hall", "GOVERNMENT", Pair(15.3388, 120.9082)),
        Triple("Peñaranda Municipal Hall", "GOVERNMENT", Pair(15.3538, 121.0019)),
        Triple("Santa Rosa Municipal Hall", "GOVERNMENT", Pair(15.4221, 120.9395)),
        Triple("Cabanatuan City Hall", "GOVERNMENT", Pair(15.4876, 120.9742)),
        Triple("Nueva Ecija Provincial Capitol", "GOVERNMENT", Pair(15.5398, 121.0821)),
        Triple("San Isidro Police Station", "GOVERNMENT", Pair(15.3114, 120.9033)),
        Triple("Gapan City Police Station", "GOVERNMENT", Pair(15.2985, 120.9502)),
        Triple("San Isidro Fire Station", "GOVERNMENT", Pair(15.3109, 120.9039)),
        Triple("Gapan Fire Station", "GOVERNMENT", Pair(15.2980, 120.9508)),
        Triple("San Isidro Post Office", "GOVERNMENT", Pair(15.3115, 120.9030)),
        Triple("LTO Gapan District Office", "GOVERNMENT", Pair(15.2930, 120.9560)),

        // --- BARANGAY HALLS ---
        Triple("Barangay Hall Poblacion San Isidro", "GOVERNMENT", Pair(15.3105, 120.9028)),
        Triple("Barangay Hall Sto. Cristo San Isidro", "GOVERNMENT", Pair(15.3060, 120.9138)),
        Triple("Barangay Hall Malapit San Isidro", "GOVERNMENT", Pair(15.3195, 120.9075)),
        Triple("Barangay Hall Alua San Isidro", "GOVERNMENT", Pair(15.3250, 120.8928)),
        Triple("Barangay Hall San Roque San Isidro", "GOVERNMENT", Pair(15.2895, 120.9118)),
        Triple("Barangay Hall Calaba San Isidro", "GOVERNMENT", Pair(15.2980, 120.8970)),
        Triple("Barangay Hall Manggang Marikit San Isidro", "GOVERNMENT", Pair(15.3165, 120.9245)),
        Triple("Barangay Hall Tabon San Isidro", "GOVERNMENT", Pair(15.2940, 120.9200)),
        Triple("Barangay Hall Pambuan Gapan", "GOVERNMENT", Pair(15.3318, 120.9182)),
        Triple("Barangay Hall Mangino Gapan", "GOVERNMENT", Pair(15.3021, 120.9385)),
        Triple("Barangay Hall Bayanihan Gapan", "GOVERNMENT", Pair(15.2965, 120.9542)),
        Triple("Barangay Hall San Vicente Gapan", "GOVERNMENT", Pair(15.2995, 120.9475)),
        Triple("Barangay Hall Sto. Niño Gapan", "GOVERNMENT", Pair(15.2950, 120.9460)),
        Triple("Barangay Hall San Nicolas Gapan", "GOVERNMENT", Pair(15.2980, 120.9520)),
        Triple("Barangay Hall Malimba Gapan", "GOVERNMENT", Pair(15.2850, 120.9350)),
        Triple("Barangay Hall Castellano San Leonardo", "GOVERNMENT", Pair(15.3640, 120.9620)),
        Triple("Barangay Hall Mallorca San Leonardo", "GOVERNMENT", Pair(15.3580, 120.9550)),

        // --- HOUSING, SUBDIVISIONS & RESIDENCES ---
        Triple("Lumina Homes San Isidro", "RESIDENTIAL", Pair(15.3156, 120.9125)),
        Triple("Camella Gapan", "RESIDENTIAL", Pair(15.2912, 120.9645)),
        Triple("Bria Homes Gapan", "RESIDENTIAL", Pair(15.2889, 120.9589)),
        Triple("Lessandra Gapan", "RESIDENTIAL", Pair(15.2905, 120.9621)),
        Triple("Villa San Isidro Subdivision", "RESIDENTIAL", Pair(15.3092, 120.9085)),
        Triple("St. Jude Village San Isidro", "RESIDENTIAL", Pair(15.3142, 120.9056)),
        Triple("Grand Victoria Heights Gapan", "RESIDENTIAL", Pair(15.2934, 120.9512)),
        Triple("Monte Cielo Subdivision San Leonardo", "RESIDENTIAL", Pair(15.3589, 120.9645)),
        Triple("Ciudad Vitas Subdivision Cabanatuan", "RESIDENTIAL", Pair(15.4745, 120.9612)),
        Triple("Lakewood City Cabanatuan", "RESIDENTIAL", Pair(15.4589, 120.9423)),
        Triple("Camella Nueva Ecija Cabanatuan", "RESIDENTIAL", Pair(15.4712, 120.9856)),
        Triple("Lumina Cabanatuan", "RESIDENTIAL", Pair(15.5012, 120.9812)),
        Triple("St. Francis Subdivision Peñaranda", "RESIDENTIAL", Pair(15.3520, 121.0035)),
        Triple("Green Meadows Subdivision San Leonardo", "RESIDENTIAL", Pair(15.3650, 120.9660)),
        Triple("Villa Cristina Subdivision Gapan", "RESIDENTIAL", Pair(15.2960, 120.9600)),

        // --- PARKS & CULTURAL LANDMARKS ---
        Triple("San Isidro Town Plaza & Park", "PARK", Pair(15.3113, 120.9032)),
        Triple("San Isidro Labrador Parish Church", "RELIGIOUS", Pair(15.3118, 120.9026)),
        Triple("National Shrine of La Virgen Divina Pastora", "RELIGIOUS", Pair(15.2978, 120.9492)),
        Triple("Gapan City Plaza & Night Market", "PARK", Pair(15.2988, 120.9488)),
        Triple("Lumang Gapan Heritage District", "TOURISM", Pair(15.2982, 120.9495)),
        Triple("Cabanatuan Freedom Park", "PARK", Pair(15.4862, 120.9705)),
        Triple("St. Nicholas of Tolentine Cathedral", "RELIGIOUS", Pair(15.4895, 120.9721)),
        Triple("San Leonardo Town Plaza", "PARK", Pair(15.3620, 120.9605)),
        Triple("Cabiao Town Plaza", "PARK", Pair(15.2485, 120.8535)),
        Triple("St. John Nepomucene Parish Church Cabiao", "RELIGIOUS", Pair(15.2490, 120.8530)),
        Triple("Peñaranda Town Plaza", "PARK", Pair(15.3540, 121.0020)),
        Triple("St. Francis of Assisi Parish Peñaranda", "RELIGIOUS", Pair(15.3545, 121.0025)),
        Triple("General Luna Statue Plaza Cabanatuan", "PARK", Pair(15.4890, 120.9710)),
        Triple("Camp Pangatian Heroes Memorial Shrine", "TOURISM", Pair(15.4980, 121.0420)),

        // --- MARKETS & COMMERCIAL MALLS ---
        Triple("San Isidro Public Market", "MARKET", Pair(15.3125, 120.9045)),
        Triple("Gapan City Public Market", "MARKET", Pair(15.2968, 120.9525)),
        Triple("WalterMart Gapan", "MALL", Pair(15.2945, 120.9578)),
        Triple("Robinsons Gapan", "MALL", Pair(15.2915, 120.9612)),
        Triple("SM City Cabanatuan", "MALL", Pair(15.4645, 120.9534)),
        Triple("SM Megacenter Cabanatuan", "MALL", Pair(15.4882, 120.9715)),
        Triple("Puregold Jr. San Isidro", "COMMERCIAL", Pair(15.3120, 120.9048)),
        Triple("Puregold Gapan", "COMMERCIAL", Pair(15.2970, 120.9515)),
        Triple("Savemore Market Gapan", "COMMERCIAL", Pair(15.2955, 120.9530)),
        Triple("San Leonardo Public Market", "MARKET", Pair(15.3615, 120.9610)),
        Triple("Cabiao Public Market", "MARKET", Pair(15.2470, 120.8540)),
        Triple("Jaen Public Market", "MARKET", Pair(15.3375, 120.9070)),
        Triple("Peñaranda Public Market", "MARKET", Pair(15.3530, 121.0012)),

        // --- FUEL STATIONS ---
        Triple("Petron San Isidro Poblacion", "GAS", Pair(15.3102, 120.9051)),
        Triple("Shell San Isidro", "GAS", Pair(15.3135, 120.9068)),
        Triple("Cleanfuel Gapan Maharlika", "GAS", Pair(15.2960, 120.9565)),
        Triple("Caltex San Leonardo", "GAS", Pair(15.3630, 120.9620)),
        Triple("Seaoil Gapan Maharlika", "GAS", Pair(15.2930, 120.9590)),
        Triple("Petron Gapan Maharlika", "GAS", Pair(15.2980, 120.9540)),
        Triple("Shell Gapan Highway", "GAS", Pair(15.2910, 120.9620)),
        Triple("Total Gas Station San Leonardo", "GAS", Pair(15.3590, 120.9580)),
        Triple("Phoenix Gas Station San Isidro", "GAS", Pair(15.3080, 120.9080)),
        Triple("Flying V San Isidro", "GAS", Pair(15.3150, 120.9020)),

        // --- FOOD & RESTAURANTS ---
        Triple("Jollibee Gapan Bayan", "FOOD", Pair(15.2975, 120.9495)),
        Triple("Jollibee WalterMart Gapan", "FOOD", Pair(15.2942, 120.9575)),
        Triple("McDonald's Gapan Maharlika", "FOOD", Pair(15.2950, 120.9570)),
        Triple("Mang Inasal Gapan", "FOOD", Pair(15.2968, 120.9510)),
        Triple("Chowking Gapan", "FOOD", Pair(15.2972, 120.9502)),
        Triple("7-Eleven San Isidro Poblacion", "FOOD", Pair(15.3115, 120.9038)),
        Triple("7-Eleven Gapan Bayan", "FOOD", Pair(15.2978, 120.9490)),
        Triple("7-Eleven San Leonardo", "FOOD", Pair(15.3625, 120.9615)),

        // --- BRIDGES & CROSSINGS ---
        Triple("Peñaranda River Bridge", "BRIDGE", Pair(15.3480, 120.9980)),
        Triple("Pampanga River Bridge San Isidro", "BRIDGE", Pair(15.3075, 120.8990)),
        Triple("Tabuating Bridge San Leonardo", "BRIDGE", Pair(15.3460, 120.9420)),
        Triple("Gapan Rotunda Junction", "JUNCTION", Pair(15.2935, 120.9585)),
        Triple("San Isidro - Cabiao Junction", "JUNCTION", Pair(15.3050, 120.9010)),
        Triple("Jaen - San Isidro Crossing", "JUNCTION", Pair(15.3220, 120.9040)),
        Triple("San Leonardo - Peñaranda Crossing", "JUNCTION", Pair(15.3628, 120.9635))
    )

    fun ensureEnriched(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentVersion = prefs.getInt(KEY_ENRICHED_VERSION, 0)
        val dbFile = File("/sdcard/Documents/Gabay/pois_fts.sqlite")
        if (!dbFile.exists() || !dbFile.canWrite()) {
            Log.d(TAG, "POI database not ready or writable on /sdcard/Documents/Gabay")
            return
        }

        try {
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            
            // Check if already enriched
            val checkCursor = db.rawQuery(
                "SELECT 1 FROM pois WHERE name = 'NEUST - San Isidro Campus' LIMIT 1",
                emptyArray()
            )
            val alreadyPresent = checkCursor.moveToFirst()
            checkCursor.close()

            if (alreadyPresent && currentVersion >= CURRENT_VERSION) {
                db.close()
                return
            }

            Log.i(TAG, "Seeding ${RICH_POIS.size} authentic local POIs into database...")
            db.beginTransaction()
            try {
                val insertStmt = db.compileStatement(
                    "INSERT OR IGNORE INTO pois (name, category, latitude, longitude) VALUES (?, ?, ?, ?)"
                )

                for (poi in RICH_POIS) {
                    val name = poi.first
                    val category = poi.second
                    val lat = poi.third.first
                    val lon = poi.third.second

                    insertStmt.bindString(1, name)
                    insertStmt.bindString(2, category)
                    insertStmt.bindDouble(3, lat)
                    insertStmt.bindDouble(4, lon)
                    val rowId = insertStmt.executeInsert()

                    if (rowId > 0) {
                        try {
                            db.execSQL(
                                "INSERT OR IGNORE INTO pois_fts (rowid, name, category) VALUES (?, ?, ?)",
                                arrayOf(rowId, name, category)
                            )
                        } catch (e: Exception) {
                            // Virtual FTS5 table may be content-backed or managed by rust_core
                        }
                    }
                }
                db.setTransactionSuccessful()
                prefs.edit().putInt(KEY_ENRICHED_VERSION, CURRENT_VERSION).apply()
                Log.i(TAG, "Successfully enriched local POI database with schools, hospitals, governments, and housing!")
            } finally {
                db.endTransaction()
                db.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enrich POI database: ${e.message}", e)
        }
    }
}
