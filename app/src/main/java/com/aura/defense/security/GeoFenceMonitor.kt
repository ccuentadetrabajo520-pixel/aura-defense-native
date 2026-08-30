package com.aura.defense.security

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Build
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class SafeZone(
        val id: String,
            val name: String,
                val latitude: Double,
                    val longitude: Double,
                        val radiusMeters: Int,
                            val createdAt: String
)

data class GeoFenceResult(
        val currentLatitude: Double,
            val currentLongitude: Double,
                val safeZones: List<SafeZone>,
                    val insideAny: Boolean,
                        val nearestZone: String?,
                            val distanceToNearest: Int,
                                val warnings: List<String>,
                                    val timestamp: String
)

class GeoFenceMonitor(private val context: Context) {

        companion object {
                    private const val PREFS_NAME ="aura_geofence"
                            private const val EARTH_RADIUS_METERS = 6371000.0
        }

            private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                fun saveSafeZone(name: String, latitude: Double, longitude: Double, radiusMeters: Int = 200): String {
                            val id = "zone_${System.currentTimeMillis()}"
                                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                            val zone = SafeZone(id, name, latitude, longitude, radiusMeters, ts)
                                                    val json = "{\"id\":\"$id\",\"name\":\"$name\",\"lat\":$latitude,\"lng\":$longitude,\"radius\":$radiusMeters,\"created\":\"$ts\"}"
                                                            prefs.edit().putString(id, json).apply()
                                                                    val ids = getZoneIds().toMutableList()
                                                                            ids.add(id)
                                                                                    prefs.edit().putString("zone_ids", ids.joinToString(",")).apply()
                                                                                            return id
                }

                    fun getZoneIds(): List<String> {
                                val stored = prefs.getString("zone_ids", "") ?: ""
                                        return if (stored.isBlank()) emptyList() else stored.split(",")
                    }

                        fun getSafeZones(): List<SafeZone> {
                                    return getZoneIds().mapNotNull { id ->
                                                val json = prefs.getString(id, null) ?: return@mapNotNull null
                                                            runCatching {
                                                                                val name = json.substringAfter("\"name\":\"").substringBefore("\"")
                                                                                                val lat = json.substringAfter("\"lat\":").substringBefore(",").toDouble()
                                                                                                                val lng = json.substringAfter("\"lng\":").substringBefore(",").toDouble()
                                                                                                                                val radius = json.substringAfter("\"radius\":").substringBefore(",").toInt()
                                                                                                                                                val created = json.substringAfter("\"created\":\"").substringBefore("\"")
                                                                                                                                                                SafeZone(id, name, lat, lng, radius, created)
                                                            }.getOrNull()
                                    }
                        }

                            fun deleteZone(id: String) {
                                        prefs.edit().remove(id).apply()
                                                val ids = getZoneIds().toMutableList()
                                                        ids.remove(id)
                                                                prefs.edit().putString("zone_ids", ids.joinToString(",")).apply()
                            }

                                fun checkGeofence(): GeoFenceResult {
                                            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                                    val warnings = mutableListOf<String>()
                                                            val location = getCurrentLocation()
                                                                    val zones = getSafeZones()

                                                                            if (location == null) {
                                                                                            warnings.add("No se pudo obtener la ubicacion actual. Verifica que el GPS este activado.")
                                                                                                        return GeoFenceResult(0.0, 0.0, zones, false, null, 0, warnings, ts)
                                                                            }

                                                                                    var insideAny = false 
                                                                                            var nearestName: String? = null
                                                                                                    var nearestDistance = Int.MAX_VALUE

                                                                                                            zones.forEach { zone ->
                                                                                                                        val dist = haversine(location.latitude, location.longitude, zone.latitude, zone.longitude)
                                                                                                                                    val distMeters = dist.roundToInt()
                                                                                                                                                if (distMeters <= zone.radiusMeters) {
                                                                                                                                                                    insideAny = true
                                                                                                                                                }
                                                                                                                                                            if (distMeters < nearestDistance) {
                                                                                                                                                                                nearestDistance = distMeters
                                                                                                                                                                                                nearestName = zone.name
                                                                                                                                                            }
                                                                                                            }

                                                                                                                    if (zones.isNotEmpty() && !insideAny) {
                                                                                                                                    warnings.add("Tu dispositivo esta FUERA de todas las zonas seguras definidas.")
                                                                                                                                                if (nearestName != null) {
                                                                                                                                                                    warnings.add("Zona mas cercana: $nearestName ($nearestDistance m)")
                                                                                                                                                }
                                                                                                                    }

                                                                                                                            return GeoFenceResult(
                                                                                                                                            currentLatitude = location.latitude,
                                                                                                                                                        currentLongitude = location.longitude,
                                                                                                                                                                    safeZones = zones,
                                                                                                                                                                                insideAny = insideAny,
                                                                                                                                                                                            nearestZone = nearestName,
                                                                                                                                                                                                        distanceToNearest = nearestDistance,
                                                                                                                                                                                                                    warnings = warnings,
                                                                                                                                                                                                                                timestamp = ts
                                                                                                                            )
                                }

                                    private fun getCurrentLocation(): Location? {
                                                return runCatching {
                                                                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                                                            val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER
                                                                                            else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER
                                                                                                            else return null
                                                                                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                                                                                                            lm.getCurrentLocation(provider, null, context.mainLooper) { loc ->
                                                                                                                                                            }
                                                                                                                                                                            lm.getLastKnownLocation(provider)
                                                                                                                        } else {
                                                                                                                                            @Suppress("DEPRECATION")
                                                                                                                                                            lm.getLastKnownLocation(provider)
                                                                                                                        }
                                                }.onFailure { Timber.e(it, "Error getting location") }.getOrNull()
                                    }

                                        private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
                                                    val dLat = Math.toRadians(lat2 - lat1)
                                                            val dLon = Math.toRadians(lon2 - lon1)
                                                                    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
                                                                            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                                                                                    return EARTH_RADIUS_METERS * c
                                        }
}
