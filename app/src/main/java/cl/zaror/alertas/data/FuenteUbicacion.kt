package cl.zaror.alertas.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class Coordenadas(val latitud: Double, val longitud: Double)

/** De dónde salen las coordenadas. Es una interfaz para poder probar sin GPS. */
interface FuenteUbicacion {
    /** Posición actual, o null si el teléfono no pudo obtenerla (GPS apagado, sin señal). */
    suspend fun actual(): Coordenadas?
}

/** Implementación real con Google Play Services (requiere permiso de ubicación concedido). */
class UbicacionGps(context: Context) : FuenteUbicacion {

    private val cliente = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    override suspend fun actual(): Coordenadas? = suspendCancellableCoroutine { continuacion ->
        val cancelacion = CancellationTokenSource()
        continuacion.invokeOnCancellation { cancelacion.cancel() }

        cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacion.token)
            .addOnSuccessListener { ubicacion ->
                continuacion.resume(ubicacion?.let { Coordenadas(it.latitude, it.longitude) })
            }
            .addOnFailureListener { error ->
                continuacion.resumeWithException(error)
            }
    }
}
