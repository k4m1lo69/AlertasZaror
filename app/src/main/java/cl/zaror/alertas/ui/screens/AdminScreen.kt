package cl.zaror.alertas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.zaror.alertas.data.remote.CuadraResponse
import cl.zaror.alertas.data.remote.UsuarioResponse
import cl.zaror.alertas.viewmodel.AdminViewModel

private val ROLES = listOf("VECINO", "COORDINADOR", "DIRECTIVA", "ADMINISTRADOR")

/** Panel del administrador: aprobar vecinos, asignar roles y restablecer contraseñas. */
@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar("PENDIENTE") }

    estado.claveTemporal?.let { info ->
        AlertDialog(
            onDismissRequest = viewModel::descartarClave,
            title = { Text("Clave temporal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Entrégasela a ${info.nombre} en persona. Solo se muestra una vez; " +
                            "debe cambiarla desde «Mi cuenta»."
                    )
                    Text(info.clave, style = MaterialTheme.typography.headlineSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::descartarClave) { Text("Listo") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onVolver) { Text("← Volver") }
                Text("Administrar vecinos", style = MaterialTheme.typography.headlineSmall)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "PENDIENTE" to "Pendientes",
                        "APROBADO" to "Aprobados",
                        "RECHAZADO" to "Rechazados"
                    ).forEach { (valor, etiqueta) ->
                        if (estado.filtro == valor) {
                            Button(onClick = { viewModel.cargar(valor) }, modifier = Modifier.weight(1f)) {
                                Text(etiqueta, maxLines = 1)
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.cargar(valor) }, modifier = Modifier.weight(1f)) {
                                Text(etiqueta, maxLines = 1)
                            }
                        }
                    }
                }

                if (estado.cargando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                estado.aviso?.let { aviso ->
                    Text(
                        aviso.texto,
                        color = if (aviso.esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                if (!estado.cargando && estado.usuarios.isEmpty()) {
                    Text("No hay usuarios en este estado.")
                }
            }
        }

        items(estado.usuarios, key = { it.id }) { usuario ->
            TarjetaUsuario(
                usuario = usuario,
                cuadras = estado.cuadras,
                ocupado = estado.cargando,
                onAprobar = { cuadraId -> viewModel.aprobar(usuario.id, cuadraId) },
                onRechazar = { viewModel.rechazar(usuario.id) },
                onCambiarRol = { rol -> viewModel.cambiarRol(usuario.id, rol) },
                onRestablecer = { viewModel.restablecerClave(usuario.id, usuario.nombre) }
            )
        }
    }
}

@Composable
private fun TarjetaUsuario(
    usuario: UsuarioResponse,
    cuadras: List<CuadraResponse>,
    ocupado: Boolean,
    onAprobar: (Long) -> Unit,
    onRechazar: () -> Unit,
    onCambiarRol: (String) -> Unit,
    onRestablecer: () -> Unit
) {
    var cuadra by remember(usuario.id) { mutableStateOf<CuadraResponse?>(null) }
    var menuCuadra by remember { mutableStateOf(false) }
    var menuRol by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                "Tel. ${usuario.telefono} · ${usuario.estado} · ${usuario.rol}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (usuario.estado != "APROBADO") {
                Box {
                    OutlinedButton(onClick = { menuCuadra = true }) {
                        Text(cuadra?.let { "${it.calle} ${it.tramo}" } ?: "Elegir cuadra")
                    }
                    DropdownMenu(expanded = menuCuadra, onDismissRequest = { menuCuadra = false }) {
                        cuadras.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.calle} ${c.tramo} (${c.sector})") },
                                onClick = {
                                    cuadra = c
                                    menuCuadra = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { cuadra?.let { onAprobar(it.id) } },
                        enabled = cuadra != null && !ocupado
                    ) {
                        Text("Aprobar")
                    }
                    if (usuario.estado == "PENDIENTE") {
                        OutlinedButton(onClick = onRechazar, enabled = !ocupado) {
                            Text("Rechazar")
                        }
                    }
                }
            } else {
                Box {
                    OutlinedButton(onClick = { menuRol = true }) {
                        Text("Rol: ${usuario.rol}")
                    }
                    DropdownMenu(expanded = menuRol, onDismissRequest = { menuRol = false }) {
                        ROLES.forEach { rol ->
                            DropdownMenuItem(
                                text = { Text(rol) },
                                onClick = {
                                    menuRol = false
                                    if (rol != usuario.rol) onCambiarRol(rol)
                                }
                            )
                        }
                    }
                }

                OutlinedButton(onClick = onRestablecer, enabled = !ocupado) {
                    Text("Restablecer contraseña")
                }
            }
        }
    }
}