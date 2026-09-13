package com.aura.defense.ui.components.aura

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private const val TERMS = """
TERMINOS Y CONDICIONES — AURA CIBER DEFENSA

1. ACEPTACIÓN. Al marcar la casilla y usar AURA aceptas estos términos.

2. QUÉ ES AURA. AURA es el primer asistente virtual de ciberdefensa local para Android sin root: (a) firewall DNS que filtra dominios peligrosos mediante una VPN local; (b) escáner de aplicaciones instaladas y sus permisos; (c) análisis de enlaces recibidos por notificaciones o compartidos; (d) análisis básico de archivos; (e) evaluación de la postura de seguridad del dispositivo; (f) asistente local conversacional de ayuda y educación en seguridad. AURA NO es un antivirus tradicional y no elimina malware ya instalado.

3. PRIVACIDAD. Todo el procesamiento ocurre EN TU DISPOSITIVO. AURA no recoge, no sube y no vende: tus aplicaciones, tu ubicación, tus URL, tu identidad ni ningún otro dato personal. No requerimos cuenta. Las únicas conexiones de red son la descarga de listas públicas de dominios maliciosos y las consultas DNS que el propio filtrado necesita.

4. FUENTES DE INTELIGENCIA. AURA utiliza listas públicas y gratuitas con fines de bloqueo, con agradecimiento a sus mantenedores: StevenBlack hosts (licencia MIT), URLhaus de abuse.ch y otras fuentes comunitarias. Estas listas pueden contener errores ajenos a AURA; puedes permitir o bloquear dominios manualmente desde la app.

5. LÍMITES REALES SIN ROOT. Android impone límites que ninguna app puede superar sin root. AURA NO puede: inspeccionar memoria de otras apps, detectar rootkits, leer el contenido cifrado del tráfico de otras apps, ni garantizar detección de todas las amenazas. AURA informa con evidencia verificable y declara cuando algo no puede saberse.

6. PERMISOS. VPN: crea el túnel local de filtrado (Android lo notifica y puede revocarlo). Notificaciones: para analizar enlaces recibidos (opcional, tú lo activas). Cámara: escáner QR defensivo (opcional). Ninguno se usa para vigilarte.

7. RESPONSABILIDAD DEL USUARIO. AURA es una capa de defensa, no un sustituto del criterio: mantén tu sistema actualizado, no instales APKs de orígenes desconocidos y verifica siempre los enlaces sensibles.

8. SIN GARANTÍA ABSOLUTA. AURA se ofrece "tal cual". Ninguna tecnología puede garantizar seguridad total. No seremos responsables por daños derivados de amenazas no detectadas, falsos positivos de listas públicas o mal uso de la aplicación.

9. CAMBIOS. Podemos actualizar estos términos; la app te lo notificará y solicitará nueva aceptación si el cambio es material.

10. PROPIEDAD. El software y su marca pertenecen a su autor. Las listas de terceros pertenecen a sus autores según sus licencias.

11. LEY APLICABLE. Estos términos se rigen por las leyes de la República Bolivariana de Venezuela. Cualquier controversia se somete a los tribunales competentes de dicha jurisdicción.

12. CONTACTO. Dudas o reportes: aura.ciberdefensa@proton.me
"""

@Composable
fun AuraTermsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Términos y Condiciones", fontFamily = FontFamily.Monospace) },
        text = {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.82f).verticalScroll(rememberScrollState())) {
                Text(TERMS, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 16.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
