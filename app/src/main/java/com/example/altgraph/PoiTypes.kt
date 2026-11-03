package com.example.altgraph

// Tipos de punto de interés de Hammerhead (Symbol.POI.Types) con su texto localizado
object PoiTypes {
    val all: List<Pair<String, Int>> = listOf(
        "aid_station" to R.string.poi_type_aid_station,
        "atm" to R.string.poi_type_atm,
        "bar" to R.string.poi_type_bar,
        "bike_parking" to R.string.poi_type_bike_parking,
        "bike_share" to R.string.poi_type_bike_share,
        "bike_shop" to R.string.poi_type_bike_shop,
        "camping" to R.string.poi_type_camping,
        "caution" to R.string.poi_type_caution,
        "coffee" to R.string.poi_type_coffee,
        "control" to R.string.poi_type_control,
        "convenience_store" to R.string.poi_type_convenience_store,
        "ferry" to R.string.poi_type_ferry,
        "first_aid" to R.string.poi_type_first_aid,
        "food" to R.string.poi_type_food,
        "gas_station" to R.string.poi_type_gas_station,
        "generic" to R.string.poi_type_generic,
        "geocache" to R.string.poi_type_geocache,
        "hospital" to R.string.poi_type_hospital,
        "library" to R.string.poi_type_library,
        "lodging" to R.string.poi_type_lodging,
        "monument" to R.string.poi_type_monument,
        "park" to R.string.poi_type_park,
        "parking" to R.string.poi_type_parking,
        "rest_stop" to R.string.poi_type_rest_stop,
        "restroom" to R.string.poi_type_restroom,
        "shopping" to R.string.poi_type_shopping,
        "shower" to R.string.poi_type_shower,
        "summit" to R.string.poi_type_summit,
        "swimming" to R.string.poi_type_swimming,
        "trailhead" to R.string.poi_type_trailhead,
        "transit_center" to R.string.poi_type_transit_center,
        "viewpoint" to R.string.poi_type_viewpoint,
        "water" to R.string.poi_type_water,
        "winery" to R.string.poi_type_winery,
    )
    val keys: List<String> = all.map { it.first }
}
