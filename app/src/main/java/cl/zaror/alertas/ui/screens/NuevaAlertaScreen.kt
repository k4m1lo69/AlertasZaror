
package cl.zaror.alertas.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cl.zaror.alertas.domain.Validaciones
import cl.zaror.alertas.ui.components.CampoTexto
import cl.zaror.alertas.ui.components.SelectorChips
import cl.zaror.alertas.viewmodel.EstadoUbicacion
import cl.zaror.alertas.viewmodel.EstadoClima

@Composable
fun NuevaAlertaScreen(
    nombreUsuario: String,
    estadoUbicacion: EstadoUbicacion = EstadoUbicacion.Inactivo,
    estadoClima: EstadoClima = EstadoClima.Inactivo,
    onBuscarUbicacion: () -> Unit = {},
    onUbicacionAplicada: () -> Unit = {},
    onConsultarClima: () -> Unit = {},
    onRegistrar: (String, String, String, String?) -> Unit
) {
    val context = LocalContext.current

    var tipo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<String?>(null) }

    var errorTipo by remember { mutableStateOf<String?>(null) }
    var errorDescripcion by remember { mutableStateOf<String?>(null) }
    var errorUbicacion by remember { mutableStateOf<String?>(null) }

    // Permisos independientes para buscar la dirección.
    val permisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) {
            onBuscarUbicacion()
        } else {
            Toast.makeText(
                context,
                "Sin permiso de ubicación no se puede detectar tu dirección",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Permisos independientes para consultar el clima.
    val permisoClima = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) {
            onConsultarClima()
        } else {
            Toast.makeText(
                context,
                "Se necesita permiso de ubicación para consultar el clima",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Aplica el resultado del GPS al campo Ubicación.
    LaunchedEffect(estadoUbicacion) {
        when (estadoUbicacion) {
            is EstadoUbicacion.Lista -> {
                ubicacion = estadoUbicacion.direccion
                errorUbicacion = null
                onUbicacionAplicada()
            }

            is EstadoUbicacion.Error -> {
                Toast.makeText(
                    context,
                    estadoUbicacion.mensaje,
                    Toast.LENGTH_LONG
                ).show()

                onUbicacionAplicada()
            }

            else -> Unit
        }
    }

    val selectorFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        fotoUri = uri?.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Registrar nueva alerta",
            style = MaterialTheme.typography.headlineSmall
        )

        SelectorChips(
            titulo = "Tipo de alerta",
            opciones = listOf(
                "Emergencia",
                "Falla de alarma",
                "Solicitud de apoyo"
            ),
            seleccionado = tipo,
            error = errorTipo
        ) { opcion ->
            tipo = opcion
            errorTipo = Validaciones.validarTipo(opcion)
        }

        CampoTexto(
            valor = descripcion,
            onCambio = {
                descripcion = it
                errorDescripcion = null
            },
            etiqueta = "Descripción",
            icono = Icons.Default.Edit,
            error = errorDescripcion,
            minLineas = 3
        )

        CampoTexto(
            valor = ubicacion,
            onCambio = {
                ubicacion = it
                errorUbicacion = null
            },
            etiqueta = "Ubicación de prueba",
            icono = Icons.Default.Place,
            error = errorUbicacion
        )

        OutlinedButton(
            enabled = estadoUbicacion !is EstadoUbicacion.Buscando,
            onClick = {
                val concedido = listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ).any {
                    ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }

                if (concedido) {
                    onBuscarUbicacion()
                } else {
                    permisoUbicacion.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.MyLocation, null)
            Spacer(Modifier.width(8.dp))

            Text(
                if (estadoUbicacion is EstadoUbicacion.Buscando) {
                    "Buscando tu ubicación…"
                } else {
                    "Usar mi ubicación (GPS)"
                }
            )
        }

        // Consulta meteorológica independiente.
        OutlinedButton(
            enabled = estadoClima !is EstadoClima.Consultando,
            onClick = {
                val concedido = listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ).any {
                    ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }

                if (concedido) {
                    onConsultarClima()
                } else {
                    permisoClima.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (estadoClima is EstadoClima.Consultando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Cloud, null)
            }

            Spacer(Modifier.width(8.dp))

            Text(
                if (estadoClima is EstadoClima.Consultando) {
                    "Consultando clima…"
                } else {
                    "Consultar clima"
                }
            )
        }

        // Resultado de Open-Meteo.
        when (val resultado = estadoClima) {
            EstadoClima.Inactivo -> Unit

            EstadoClima.Consultando -> {
                Text(
                    "Obteniendo las condiciones meteorológicas de tu ubicación.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            is EstadoClima.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null
                        )

                        Text(
                            resultado.mensaje,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            is EstadoClima.Disponible -> {
                val clima = resultado.clima

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = null
                            )

                            Text(
                                "Condiciones meteorológicas",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            descripcionClima(clima.codigoClima),
                            style = MaterialTheme.typography.titleLarge
                        )

                        HorizontalDivider()

                        Text(
                            "Temperatura: ${
                                clima.temperatura?.let {
                                    String.format("%.1f °C", it)
                                } ?: "No disponible"
                            }"
                        )

                        Text(
                            "Humedad: ${
                                clima.humedad?.let {
                                    String.format("%.0f %%", it)
                                } ?: "No disponible"
                            }"
                        )

                        Text(
                            "Precipitación: ${
                                clima.precipitacion?.let {
                                    String.format("%.1f mm", it)
                                } ?: "No disponible"
                            }"
                        )

                        Text(
                            "Viento: ${
                                clima.velocidadViento?.let {
                                    String.format("%.1f km/h", it)
                                } ?: "No disponible"
                            }"
                        )

                        HorizontalDivider()

                        Text(
                            "Hora local: ${clima.hora ?: "No disponible"}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            "Zona horaria: ${clima.zonaHoraria ?: "No disponible"}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            "Datos meteorológicos: Open-Meteo.com",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Text(
                    "Para consultar el clima, tus coordenadas se envían a Open-Meteo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedButton(
            onClick = {
                ubicacion = "Ubicación ficticia: Block 3, casa 15"
                errorUbicacion = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Place, null)
            Spacer(Modifier.width(8.dp))
            Text("Usar ubicación ficticia")
        }

        OutlinedButton(
            onClick = { probarVibracion(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.NotificationsActive, null)
            Spacer(Modifier.width(8.dp))
            Text("Probar vibración del dispositivo")
        }

        Text(
            text = "Reportada por: $nombreUsuario",
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedButton(
            onClick = { selectorFoto.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddAPhoto, null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (fotoUri == null) {
                    "Adjuntar foto opcional"
                } else {
                    "Foto seleccionada"
                }
            )
        }

        Button(
            onClick = {
                errorTipo = Validaciones.validarTipo(tipo)
                errorDescripcion = Validaciones.validarDescripcion(descripcion)
                errorUbicacion = Validaciones.validarUbicacion(ubicacion)

                val formularioValido =
                    errorTipo == null &&
                            errorDescripcion == null &&
                            errorUbicacion == null

                if (formularioValido) {
                    vibrarAlRegistrar(context)

                    onRegistrar(
                        tipo.trim(),
                        descripcion.trim(),
                        ubicacion.trim(),
                        fotoUri
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Registrar alerta")
        }
    }
}

/** Traduce los códigos meteorológicos WMO a descripciones en español. */
private fun descripcionClima(codigo: Int?): String {
    return when (codigo) {
        0 -> "Despejado"
        1 -> "Principalmente despejado"
        2 -> "Parcialmente nublado"
        3 -> "Cubierto"
        45, 48 -> "Niebla"
        51 -> "Llovizna ligera"
        53 -> "Llovizna moderada"
        55 -> "Llovizna intensa"
        56, 57 -> "Llovizna helada"
        61 -> "Lluvia ligera"
        63 -> "Lluvia moderada"
        65 -> "Lluvia intensa"
        66, 67 -> "Lluvia helada"
        71 -> "Nevada ligera"
        73 -> "Nevada moderada"
        75 -> "Nevada intensa"
        77 -> "Granos de nieve"
        80 -> "Chubascos ligeros"
        81 -> "Chubascos moderados"
        82 -> "Chubascos violentos"
        85, 86 -> "Chubascos de nieve"
        95 -> "Tormenta"
        96, 99 -> "Tormenta con granizo"
        else -> "Condición meteorológica no disponible"
    }
}

fun vibrarAlRegistrar(context: Context) {
    val vibrador = context.getSystemService(Vibrator::class.java)

    if (vibrador?.hasVibrator() == true) {
        vibrador.vibrate(
            VibrationEffect.createOneShot(
                200,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }
}

private fun probarVibracion(context: Context) {
    val vibrador = context.getSystemService(Vibrator::class.java)

    if (vibrador?.hasVibrator() == true) {
        vibrador.vibrate(
            VibrationEffect.createOneShot(
                250,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )

        Toast.makeText(
            context,
            "Prueba de vibración ejecutada",
            Toast.LENGTH_SHORT
        ).show()
    } else {
        Toast.makeText(
            context,
            "Este dispositivo no dispone de vibración",
            Toast.LENGTH_SHORT
        ).show()
    }
}
