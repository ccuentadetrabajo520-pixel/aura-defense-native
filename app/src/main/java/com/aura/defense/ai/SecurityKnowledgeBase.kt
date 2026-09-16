package com.aura.defense.ai

object SecurityKnowledgeBase {
    data class KnowledgeEntry(
        val id: String,
        val title: String,
        val shortAnswer: String,
        val detail: String,
        val protectionTip: String,
        val synonyms: List<String>
    )

    val entries: List<KnowledgeEntry> by lazy {
        listOf(
        entry("phishing", "Phishing", "Suplantacion para robar credenciales o datos.", "El atacante imita una entidad confiable mediante mensajes, paginas o dominios parecidos y crea urgencia.", "Escribe manualmente la direccion oficial y nunca entregues credenciales desde un enlace recibido.", listOf("suplantacion", "enlace falso", "pagina falsa")),
        entry("smishing", "Smishing", "Phishing enviado por SMS.", "El mensaje suele fingir ser un banco, courier o premio y dirige a un enlace o telefono controlado por el atacante.", "No abras el enlace; verifica el aviso desde la aplicacion o web oficial.", listOf("sms sospechoso", "mensaje falso")),
        entry("troyano-bancario", "Troyano bancario", "Malware que intenta robar operaciones y credenciales bancarias.", "Puede llegar como APK externa y abusar de accesibilidad, SMS o superposicion para leer pantallas y simular toques.", "No concedas accesibilidad ni instales APK fuera de una fuente confiable.", listOf("banca", "mekotio", "grandoreiro")),
        entry("spyware", "Spyware", "Software que recopila informacion sin consentimiento claro.", "Puede observar actividad, ubicacion, mensajes o archivos mediante permisos y servicios del sistema.", "Revisa permisos, accesibilidad, administradores y desinstala aplicaciones que no reconozcas.", listOf("espia", "software espia")),
        entry("stalkerware", "Stalkerware", "Software espia orientado a vigilar a una persona.", "Suele abusar de Device Admin, accesibilidad, notificaciones o ubicacion para dificultar su deteccion.", "Revisa administradores y listeners; busca ayuda segura si sospechas vigilancia personal.", listOf("acoso digital", "monitorizacion")),
        entry("adware", "Adware", "Software que muestra publicidad intrusiva.", "Algunas variantes rastrean actividad o redirigen el navegador; otras solo son molestas, por lo que el contexto importa.", "Desinstala apps recientes que generen ventanas superpuestas y revisa su origen.", listOf("publicidad maliciosa")),
        entry("ransomware", "Ransomware", "Malware que bloquea o cifra datos para exigir un rescate.", "Suele entrar por archivos, enlaces o aplicaciones vulnerables y puede afectar documentos accesibles por el usuario.", "Mantén copias de seguridad separadas y no pagues ni abras archivos inesperados.", listOf("secuestro de archivos")),
        entry("botnet", "Botnet", "Red de dispositivos controlados por un operador.", "El dispositivo recibe ordenes remotas para fraude, spam, escaneo o ataques distribuidos.", "Actualiza Android, elimina apps sospechosas y vigila consumo anormal de red.", listOf("red zombi")),
        entry("c2", "C2", "Servidor de mando y control de una amenaza.", "El malware consulta dominios o IP para recibir ordenes y enviar datos; el bloqueo DNS puede cortar esa comunicacion.", "No ignores bloqueos repetidos y revisa la app instalada recientemente asociada.", listOf("command and control", "mando y control")),
        entry("malware", "Malware", "Nombre general para software diseñado para causar dano o abuso.", "Incluye troyanos, spyware, ransomware, gusanos y otras familias con objetivos distintos.", "Instala desde fuentes confiables y revisa permisos y hallazgos de Aura.", listOf("virus", "software malicioso")),
        entry("rootkit", "Rootkit", "Conjunto de herramientas para ocultar actividad con privilegios elevados.", "En Android sin root, Aura puede observar señales visibles pero no prometer detectar componentes fuera del alcance del sistema.", "Mantén el dispositivo actualizado y usa el restablecimiento oficial si hay compromiso grave.", listOf("root", "ocultacion")),
        entry("mitm", "Man in the middle", "Intercepcion de una comunicacion entre dos partes.", "El atacante se coloca entre dispositivo y servicio para observar o modificar trafico, especialmente en redes inseguras.", "Usa HTTPS, evita Wi-Fi abierto para operaciones sensibles y valida certificados.", listOf("intercepcion", "mitm attack")),
        entry("arp-spoofing", "ARP spoofing", "Engano de la tabla ARP para hacerse pasar por otro equipo de la red.", "Permite desviar trafico local hacia el atacante, aunque HTTPS puede limitar lo que puede leer.", "Evita redes desconocidas y no ignores avisos de certificados.", listOf("arp poisoning")),
        entry("dns-hijacking", "DNS hijacking", "Manipulacion de respuestas DNS para redirigir dominios.", "Una red, router, app o malware puede responder con destinos falsos aunque el dominio parezca correcto.", "Usa DNS confiable, HTTPS y verifica la configuracion del router.", listOf("secuestro dns")),
        entry("doh", "DNS over HTTPS", "Transporte de consultas DNS dentro de HTTPS.", "Protege la consulta frente a observadores de la red, pero no convierte en segura una pagina maliciosa.", "Usa un proveedor confiable y recuerda que el dominio sigue requiriendo criterio.", listOf("dns seguro", "dns over https")),
        entry("vpn", "VPN", "Tunel que transporta trafico hacia un servidor intermediario.", "Cifra el tramo entre el dispositivo y el proveedor, pero el proveedor puede observar metadatos y no sustituye HTTPS.", "Usa un proveedor confiable y confirma que el tunel de Aura figura activo.", listOf("tunel", "red privada")),
        entry("dns", "DNS", "Sistema que traduce nombres de dominio a direcciones.", "Las consultas DNS revelan destinos y pueden ser bloqueadas cuando coinciden con inteligencia de amenazas.", "Mantén activo el firewall DNS y verifica sus eventos reales.", listOf("resolucion de nombres")),
        entry("ip", "Direccion IP", "Identificador de red de un dispositivo o servicio.", "Una IP puede cambiar, compartirse o pertenecer a infraestructura legitima y maliciosa en distintos momentos.", "No juzgues una IP aislada; correlaciona fuente, fecha y comportamiento.", listOf("direccion de internet")),
        entry("puerto", "Puerto de red", "Numero que identifica un servicio dentro de una IP.", "Un puerto abierto no demuestra por si solo una intrusión, pero expone una superficie que debe estar justificada.", "Desactiva servicios innecesarios y evita exponer interfaces de administracion.", listOf("port", "puertos")),
        entry("cifrado", "Cifrado", "Transformacion que hace ilegible la informacion sin una clave.", "Protege datos en transito o reposo, pero no evita que una app autorizada lea datos antes o despues del cifrado.", "Usa bloqueo de pantalla, almacenamiento cifrado y HTTPS.", listOf("encriptacion")),
        entry("https", "HTTPS frente a HTTP", "HTTPS cifra y autentica la conexion web mediante TLS.", "HTTP transmite sin esa proteccion; HTTPS tampoco garantiza que el sitio sea honesto o libre de malware.", "No introduzcas datos si el navegador muestra advertencias de certificado.", listOf("http", "tls")),
        entry("punycode", "Punycode", "Codificacion usada para representar caracteres internacionales en dominios.", "Puede facilitar dominios visualmente parecidos a marcas mediante homografos.", "Revisa el dominio caracter por caracter y usa marcadores oficiales.", listOf("homografo", "idn")),
        entry("acortadores", "Acortadores", "Servicios que ocultan el destino final de un enlace.", "Dificultan revisar el dominio y pueden encadenar redirecciones hacia phishing o malware.", "No abras acortadores inesperados; analiza el enlace antes.", listOf("url corta", "enlace acortado")),
        entry("sideloading", "Sideloading", "Instalacion de una app fuera de una tienda administrada.", "Reduce las garantias de revision y facilita APK modificadas o troyanizadas.", "Desactiva la instalacion desde fuentes desconocidas cuando no la necesites.", listOf("instalacion externa", "fuera de tienda")),
        entry("apk", "APK", "Paquete instalable de una aplicacion Android.", "Una APK puede ser legitima, pero una copia externa puede incluir codigo modificado.", "Comprueba origen, firma y reputacion antes de instalar.", listOf("paquete android")),
        entry("firma-digital", "Firma digital", "Mecanismo que vincula una app con una clave de firma.", "Permite comprobar continuidad de actualizaciones, aunque una firma valida no prueba que la app sea buena.", "Instala desde fuentes que puedas verificar y no aceptes copias alteradas.", listOf("firma apk", "certificado de aplicacion")),
        entry("permisos-sms", "Permisos SMS", "Permisos para leer, recibir o enviar mensajes.", "Pueden exponer codigos de verificacion y facilitar fraude si una app no los necesita.", "Concedelos solo a apps cuya funcion los requiera claramente.", listOf("sms permissions")),
        entry("permisos-ubicacion", "Permisos de ubicacion", "Acceso a la posicion aproximada o precisa.", "Una app puede construir historiales sensibles o inferir rutinas.", "Usa ubicacion solo durante el uso cuando sea suficiente.", listOf("gps", "ubicacion")),
        entry("permisos-microfono", "Permisos de microfono", "Acceso a la captura de audio.", "Una app abusiva puede intentar registrar conversaciones o activar el microfono sin una razon clara.", "Revoca el permiso a apps que no graben audio como funcion principal.", listOf("audio", "microfono")),
        entry("permisos-camara", "Permisos de camara", "Acceso a la camara del dispositivo.", "Permite capturar imagenes o video y puede revelar espacios privados.", "Concedelo solo cuando la funcion lo necesite y revisa el indicador del sistema.", listOf("camara", "video")),
        entry("permisos-contactos", "Permisos de contactos", "Acceso a la agenda del usuario.", "Puede usarse para spam, suplantacion o exposicion de relaciones.", "No lo concedas a utilidades que no gestionen contactos.", listOf("agenda", "contactos")),
        entry("permisos-overlay", "Superposicion", "Permiso para dibujar sobre otras aplicaciones.", "Puede ocultar botones, robar toques o mostrar pantallas falsas sobre una app bancaria.", "No concedas overlay a apps desconocidas.", listOf("ventanas flotantes", "draw over other apps")),
        entry("permisos-accesibilidad", "Accesibilidad", "Servicio que puede observar y actuar sobre la interfaz.", "Es muy potente y puede leer pantalla, pulsar botones y automatizar operaciones.", "Activalo solo para servicios confiables y necesarios.", listOf("accessibility", "servicio de accesibilidad")),
        entry("request-install-packages", "Instalar aplicaciones desconocidas", "Permiso para iniciar instalaciones de APK.", "Un navegador o app comprometida puede usarlo para colocar software adicional.", "Manténlo desactivado salvo durante una instalación consciente.", listOf("instalar apk", "fuentes desconocidas")),
        entry("ingenieria-social", "Ingenieria social", "Manipulacion de personas para obtener acceso o informacion.", "Explota confianza, urgencia, miedo o autoridad en vez de una vulnerabilidad tecnica.", "Confirma solicitudes sensibles por un segundo canal.", listOf("engaño", "manipulacion")),
        entry("2fa", "Autenticacion de dos factores", "Segundo requisito ademas de la contrasena.", "Reduce el impacto de una contrasena robada, aunque el phishing puede intentar capturar ambos factores.", "Prefiere una app autenticadora o llave de seguridad frente a SMS cuando sea posible.", listOf("mfa", "doble factor")),
        entry("gestor-contrasenas", "Gestor de contrasenas", "Aplicacion que genera y almacena credenciales unicas.", "Reduce reutilizacion y permite contrasenas largas, pero el gestor y su cuenta maestra deben protegerse.", "Usa una contrasena maestra unica y 2FA.", listOf("password manager", "contrasenas")),
        entry("hibp", "Have I Been Pwned", "Servicio que informa de filtraciones conocidas asociadas a una cuenta.", "Una coincidencia indica exposicion historica, no que la cuenta este siendo atacada ahora.", "Cambia la contrasena reutilizada y activa 2FA desde el sitio oficial.", listOf("filtracion de correo", "breach")),
        entry("hash-sha256", "Hash SHA-256", "Huella de longitud fija de unos datos.", "Sirve para comparar integridad, pero no es cifrado ni permite recuperar de forma practica el original.", "Compara hashes desde una fuente confiable y el archivo exacto.", listOf("sha256", "hash")),
        entry("wifi-publica", "Wi-Fi publica", "Red inalambrica compartida con desconocidos.", "Un atacante puede observar metadatos, crear un punto falso o intentar interceptar trafico.", "Evita banca en redes abiertas y usa HTTPS y VPN.", listOf("wifi abierto", "red publica")),
        entry("evil-twin", "Evil twin", "Punto Wi-Fi falso que imita una red legitima.", "Busca que el usuario se conecte para observar trafico o presentar portales fraudulentos.", "Confirma el nombre exacto con el responsable y desactiva conexion automatica.", listOf("wifi gemelo", "punto de acceso falso")),
        entry("imsi-catcher", "IMSI catcher", "Equipo que suplanta una antena movil para identificar o atraer dispositivos.", "Puede obtener identificadores y degradar conexiones; sus capacidades dependen de la red y del equipo.", "Mantén Android actualizado y evita confiar en una alerta sin evidencia adicional.", listOf("antena falsa", "intercepcion movil")),
        entry("pegasus", "Pegasus", "Familia de spyware avanzado dirigida a objetivos concretos.", "Algunas versiones explotaron vulnerabilidades sofisticadas fuera del alcance normal de una app sin root.", "Actualiza pronto y busca asistencia especializada ante una amenaza dirigida.", listOf("spyware avanzado")),
        entry("parche-seguridad", "Parche de seguridad", "Actualizacion que corrige vulnerabilidades conocidas.", "Retrasar parches deja expuestos fallos que atacantes ya pueden conocer.", "Instala actualizaciones oficiales y revisa la fecha del parche.", listOf("actualizacion android", "security patch")),
        entry("play-protect", "Google Play Protect", "Servicio que analiza aplicaciones y comportamientos en Android.", "Ayuda a detectar amenazas, pero no conoce todo y no sustituye permisos, actualizaciones ni criterio.", "Mantenlo activo y no interpretes su silencio como garantia absoluta.", listOf("play protect", "proteccion play")),
        entry("modo-seguro", "Modo seguro", "Arranque que limita aplicaciones de terceros.", "Ayuda a comprobar si un problema desaparece al impedir que apps instaladas se ejecuten normalmente.", "Usalo para diagnostico y desinstala solo lo que identifiques.", listOf("safe mode", "arranque seguro")),
        entry("safe-browsing", "Safe Browsing", "Proteccion que compara sitios y descargas con listas de riesgo.", "Puede advertir sobre amenazas conocidas, pero los dominios nuevos o ataques dirigidos pueden no aparecer.", "Respeta los avisos y verifica el dominio aunque no haya alerta.", listOf("navegacion segura", "navegacion protegida"))
        ) + additionalEntries
    }

    private val additionalEntries: List<KnowledgeEntry> = buildList {
        addAll(listOf(
        compact("sim-swapping", "SIM swapping", "sim swap"), compact("credential-stuffing", "Credential stuffing", "password reuse"),
        compact("brute-force", "Fuerza bruta", "brute force"), compact("password-spraying", "Password spraying", "spraying"),
        compact("credential-phishing", "Robo de credenciales", "credential theft"), compact("business-email-compromise", "Fraude de correo corporativo", "bec"),
        compact("malvertising", "Malvertising", "publicidad maliciosa"), compact("drive-by-download", "Descarga automatica", "drive by"),
        compact("watering-hole", "Watering hole", "sitio comprometido"), compact("typosquatting", "Typosquatting", "dominio parecido"),
        compact("domain-fronting", "Domain fronting", "ocultacion de dominio"), compact("url-spoofing", "Suplantacion de URL", "url falsa"),
        compact("qr-phishing", "QR phishing", "quishing"), compact("vishing", "Vishing", "llamada fraudulenta"),
        compact("smishing", "Smishing avanzado", "sms fraudulento"), compact("pretexting", "Pretexting", "pretexto"),
        compact("baiting", "Baiting", "cebo digital"), compact("tailgating", "Acceso por seguimiento", "tailgating"),
        compact("shoulder-surfing", "Shoulder surfing", "observacion directa"), compact("insider-threat", "Amenaza interna", "insider"),
        compact("data-exfiltration", "Exfiltracion de datos", "robo de datos"), compact("data-loss", "Perdida de datos", "dlp"),
        compact("data-breach", "Filtracion de datos", "brecha de datos"), compact("supply-chain", "Ataque a la cadena de suministro", "supply chain"),
        compact("dependency-confusion", "Dependency confusion", "dependencia maliciosa"), compact("typo-package", "Paquete con nombre parecido", "typosquatting de paquetes"),
        compact("zero-day", "Zero day", "vulnerabilidad de dia cero"), compact("n-day", "Vulnerabilidad conocida", "n day"),
        compact("cve", "CVE", "vulnerabilidad catalogada"), compact("cvss", "CVSS", "puntuacion de vulnerabilidad"),
        compact("exploit", "Exploit", "explotacion"), compact("patching", "Gestion de parches", "parcheo"),
        compact("secure-boot", "Arranque seguro", "secure boot"), compact("verified-boot", "Verified Boot", "arranque verificado"),
        compact("keystore", "Android Keystore", "almacen de claves"), compact("hardware-backed-key", "Clave respaldada por hardware", "hardware backed"),
        compact("biometric-security", "Seguridad biometrica", "biometria"), compact("screen-lock", "Bloqueo de pantalla", "pin seguro"),
        compact("encryption-at-rest", "Cifrado en reposo", "datos cifrados"), compact("transport-encryption", "Cifrado en transito", "datos en transito"),
        compact("certificate-pinning", "Certificate pinning", "fijacion de certificados"), compact("tls", "TLS", "seguridad tls"),
        compact("certificate-transparency", "Certificate Transparency", "transparencia de certificados"), compact("hsts", "HSTS", "strict transport security"),
        compact("content-security-policy", "Content Security Policy", "csp"), compact("secure-cookie", "Cookie segura", "secure cookies"),
        compact("csrf", "CSRF", "cross site request forgery"), compact("xss", "XSS", "cross site scripting"),
        compact("sql-injection", "Inyeccion SQL", "sql injection"), compact("command-injection", "Inyeccion de comandos", "command injection"),
        compact("path-traversal", "Path traversal", "traversal de rutas"), compact("ssrf", "SSRF", "server side request forgery"),
        compact("open-redirect", "Redireccion abierta", "open redirect"), compact("insecure-deserialization", "Deserializacion insegura", "deserializacion"),
        compact("buffer-overflow", "Desbordamiento de buffer", "buffer overflow"), compact("race-condition", "Condicion de carrera", "race condition"),
        compact("privilege-escalation", "Escalada de privilegios", "privilege escalation"), compact("lateral-movement", "Movimiento lateral", "lateral movement"),
        compact("persistence", "Persistencia", "persistencia de malware"), compact("defense-evasion", "Evasion de defensas", "defense evasion"),
        compact("command-control", "Mando y control", "c2 server"), compact("beaconing", "Beaconing", "baliza de red"),
        compact("domain-generation", "Domain generation algorithm", "dga"), compact("fast-flux", "Fast flux", "rotacion dns"),
        compact("tor", "Red Tor", "onion routing"), compact("proxy", "Proxy", "servidor proxy"),
        compact("firewall", "Firewall", "cortafuegos"), compact("allowlist", "Lista de permitidos", "allow list"),
        compact("blocklist", "Lista de bloqueados", "deny list"), compact("network-segmentation", "Segmentacion de red", "segmentacion"),
        compact("zero-trust", "Zero trust", "confianza cero"), compact("least-privilege", "Minimo privilegio", "least privilege"),
        compact("security-monitoring", "Monitorizacion de seguridad", "observabilidad"), compact("audit-log", "Registro de auditoria", "audit trail"),
        compact("incident-response", "Respuesta a incidentes", "incident response"), compact("digital-forensics", "Forense digital", "forensics"),
        compact("threat-model", "Modelo de amenazas", "threat modeling"), compact("risk-assessment", "Evaluacion de riesgo", "risk analysis"),
        compact("security-baseline", "Linea base de seguridad", "baseline"), compact("backup", "Copia de seguridad", "backup seguro"),
        compact("recovery", "Recuperacion", "disaster recovery"), compact("account-recovery", "Recuperacion de cuenta", "account recovery"),
        compact("passkey", "Passkey", "clave de acceso"), compact("webauthn", "WebAuthn", "llave de seguridad"),
        compact("oauth", "OAuth", "autorizacion delegada"), compact("session-hijacking", "Secuestro de sesion", "session hijacking"),
        compact("token-theft", "Robo de token", "token theft"), compact("api-security", "Seguridad de API", "api security"),
        compact("mobile-security", "Seguridad movil", "mobile security"), compact("app-signing", "Firma de aplicaciones", "app signing"),
        compact("supply-chain-mobile", "Cadena de suministro movil", "app supply chain"), compact("permissions", "Permisos de aplicacion", "app permissions"),
        compact("notification-listener", "Listener de notificaciones", "notification access"), compact("device-admin", "Administrador del dispositivo", "device administrator"),
        compact("accessibility-abuse", "Abuso de accesibilidad", "accessibility abuse"), compact("overlay-attack", "Ataque de superposicion", "overlay attack"),
        compact("unknown-sources", "Fuentes desconocidas", "unknown sources"), compact("root-detection", "Deteccion de root", "rooted device"),
        compact("emulator-detection", "Deteccion de emulador", "emulator"), compact("adb", "Android Debug Bridge", "adb enabled"),
        compact("private-dns", "DNS privado", "private dns"), compact("vpn-security", "Seguridad VPN", "vpn security"),
        compact("wifi-security", "Seguridad Wi-Fi", "wifi security"), compact("bluetooth-security", "Seguridad Bluetooth", "bluetooth"),
        compact("nfc-security", "Seguridad NFC", "nfc"), compact("location-privacy", "Privacidad de ubicacion", "location privacy")
        ))
    }

    private fun compact(id: String, title: String, vararg synonyms: String) = entry(
        id,
        title,
        "Concepto de ciberseguridad que requiere contexto para evaluarse.",
        "La senal puede aparecer en ataques reales, pero su presencia aislada no demuestra compromiso.",
        "Correlaciona esta senal con fuente, fecha, permisos y comportamiento observable.",
        synonyms.toList()
    )

    private fun entry(id: String, title: String, shortAnswer: String, detail: String, protectionTip: String, synonyms: List<String>) =
        KnowledgeEntry(id, title, shortAnswer, detail, protectionTip, synonyms)

    fun search(query: String): KnowledgeEntry? {
        val q = normalize(query)
        entries.firstOrNull { q.contains(it.id) }?.let { return it }
        entries.firstOrNull { entry -> entry.synonyms.any { q.contains(normalize(it)) } }?.let { return it }
        return entries.firstOrNull { q.contains(normalize(it.title)) }
    }

    fun normalize(value: String): String = value.lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
}
