package cl.zaror.alertas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.zaror.alertas.domain.ValidacionesCuenta
import cl.zaror.alertas.ui.components.CampoTexto
import cl.zaror.alertas.viewmodel.Aviso
import cl.zaror.alertas.viewmodel.UsuarioSesion

/** Editar perfil (nombre y dirección) y cambiar la contraseña. */
@Composable
fun CuentaScreen(
    usuario: UsuarioSesion,
    mensaje: Aviso?,
    procesando: Boolean,
    onGuardarPerfil: (nombre: String, direccion: String) -> Unit,
    onCambiarClave: (claveActual: String, claveNueva: String) -> Unit,
    onLimpiarMensaje: () -> Unit,
    onVolver: () -> Unit
) {
    var nombre by remember(usuario.id) { mutableStateOf(usuario.nombre) }
    var direccion by remember(usuario.id) { mutableStateOf(usuario.direccion) }
    var errorNombre by remember { mutableStateOf<String?>(null) }
    var errorDireccion by remember { mutableStateOf<String?>(null) }

    var claveActual by remember { mutableStateOf("") }
    var claveNueva by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var errorActual by remember { mutableStateOf<String?>(null) }
    var errorNueva by remember { mutableStateOf<String?>(null) }
    var errorConfirmacion by remember { mutableStateOf<String?>(null) }

    // Tras un cambio de clave exitoso se vacían los campos.
    LaunchedEffect(mensaje) {
        if (mensaje != null && !mensaje.esError && mensaje.texto.startsWith("Contraseña")) {
            claveActual = ""
            claveNueva = ""
            confirmacion = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onVolver) { Text("← Volver") }

        Text("Mi cuenta", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Teléfono: ${usuario.telefono} · Rol: ${usuario.rol.etiqueta}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))
        Text("Datos personales", style = MaterialTheme.typography.titleMedium)

        CampoTexto(
            valor = nombre,
            onCambio = {
                nombre = it
                errorNombre = null
                onLimpiarMensaje()
            },
            etiqueta = "Nombre completo",
            icono = Icons.Default.Person,
            error = errorNombre
        )

        CampoTexto(
            valor = direccion,
            onCambio = {
                direccion = it
                errorDireccion = null
                onLimpiarMensaje()
            },
            etiqueta = "Dirección",
            icono = Icons.Default.Home,
            error = errorDireccion
        )

        Button(
            onClick = {
                errorNombre = ValidacionesCuenta.validarNombre(nombre)
                errorDireccion = ValidacionesCuenta.validarDireccion(direccion)
                if (errorNombre == null && errorDireccion == null) {
                    onGuardarPerfil(nombre.trim(), direccion.trim())
                }
            },
            enabled = !procesando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar datos")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Cambiar contraseña", style = MaterialTheme.typography.titleMedium)

        CampoTexto(
            valor = claveActual,
            onCambio = {
                claveActual = it
                errorActual = null
                onLimpiarMensaje()
            },
            etiqueta = "Contraseña actual",
            icono = Icons.Default.Lock,
            error = errorActual,
            esClave = true
        )

        CampoTexto(
            valor = claveNueva,
            onCambio = {
                claveNueva = it
                errorNueva = null
                onLimpiarMensaje()
            },
            etiqueta = "Contraseña nueva",
            icono = Icons.Default.Lock,
            error = errorNueva,
            esClave = true
        )

        CampoTexto(
            valor = confirmacion,
            onCambio = {
                confirmacion = it
                errorConfirmacion = null
                onLimpiarMensaje()
            },
            etiqueta = "Repetir contraseña nueva",
            icono = Icons.Default.Lock,
            error = errorConfirmacion,
            esClave = true
        )

        Button(
            onClick = {
                errorActual = ValidacionesCuenta.validarClaveActual(claveActual)
                errorNueva = ValidacionesCuenta.validarClaveNueva(claveNueva)
                errorConfirmacion = ValidacionesCuenta.validarConfirmacion(claveNueva, confirmacion)
                if (errorActual == null && errorNueva == null && errorConfirmacion == null) {
                    onCambiarClave(claveActual, claveNueva)
                }
            },
            enabled = !procesando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (procesando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Cambiar contraseña")
            }
        }

        if (mensaje != null) {
            Text(
                text = mensaje.texto,
                color = if (mensaje.esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}