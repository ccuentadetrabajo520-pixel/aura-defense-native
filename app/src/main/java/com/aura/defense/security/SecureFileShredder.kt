package com.aura.defense.security

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.Random

data class ShredResult(
        val file: String,
            val success: Boolean,
                val passes: Int,
                    val error: String? = null
)

class SecureFileShredder(private val context: Context) {

        companion object {
                    private const val DEFAULT_PASSES = 3
        }

            fun shred(file: File, passes: Int = DEFAULT_PASSES): ShredResult {
                        val fileName = file.name
                                if (!file.exists()) return ShredResult(fileName, false, 0, "El archivo no existe")
                                        if (!file.canWrite()) return ShredResult(fileName, false, 0, "Sin permisos de escritura")

                                                return runCatching {
                                                                val length = file.length()
                                                                            repeat(passes) {
                                                                                                val data = ByteArray(minOf(length.toInt(), 1024 * 1024))
                                                                                                                Random().nextBytes(data)
                                                                                                                                FileOutputStream(file, false).use { fos ->
                                                                                                                                                    var written = 0L
                                                                                                                                                                        while (written < length) {
                                                                                                                                                                                                    val toWrite = minOf(data.size.toLong(), length - written)
                                                                                                                                                                                                                            fos.write(data, 0, toWrite.toInt())
                                                                                                                                                                                                                                                    written += toWrite
                                                                                                                                                                        }
                                                                                                                                }
                                                                            }
                                                                                        val zeros = ByteArray(minOf(length.toInt(), 1024 * 1024))
                                                                                                    FileOutputStream(file, false).use { fos ->
                                                                                                                    var written = 0L
                                                                                                                                    while (written < length) {
                                                                                                                                                            val toWrite = minOf(zeros.size.toLong(), length - written)
                                                                                                                                                                                fos.write(zeros, 0, toWrite.toInt())
                                                                                                                                                                                                    written += toWrite
                                                                                                                                    }
                                                                                                    }
                                                                                                                val deleted = file.delete()
                                                                                                                            if (!deleted) file.deleteOnExit()
                                                                                                                                        ShredResult(fileName, true, passes)
                                                }.onFailure { e ->
                                                            Timber.e(e, "Shred failed for $fileName")
                                                                        ShredResult(fileName, false, passes, e.message)
                                                                                }.getOrDefault(ShredResult(fileName, false, passes, "Error desconocido"))
            }

                fun listShreddableFiles(): List<File> {
                            return runCatching {
                                            context.filesDir.listFiles()?.filter { it.isFile && it.canWrite() }?.sortedByDescending { it.lastModified() } ?: emptyList()
                            }.getOrDefault(emptyList())
                }
}
