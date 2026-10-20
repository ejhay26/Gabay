package dev.gabay.app.map

import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object PmTilesServer {
    private const val TAG = "PmTilesServer"
    const val PORT = 8765

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    private var vectorReader: PmTilesReader? = null
    private var topoReader: PmTilesReader? = null
    private var satReader: PmTilesReader? = null

    private var appContext: android.content.Context? = null

    @Synchronized
    fun start(context: android.content.Context, vaultDir: File) {
        if (isRunning) return
        appContext = context.applicationContext

        try {
            val vecFile = File(vaultDir, "philippines_vector.pmtiles")
            if (vecFile.exists() && vecFile.length() > 1024) {
                vectorReader = PmTilesReader(vecFile)
                Log.i(TAG, "Initialized vector PMTiles reader from ${vecFile.absolutePath}")
            }

            val topoFile = File(vaultDir, "topography_contours.pmtiles")
            if (topoFile.exists() && topoFile.length() > 1024) {
                topoReader = PmTilesReader(topoFile)
                Log.i(TAG, "Initialized topo PMTiles reader from ${topoFile.absolutePath}")
            }

            val satFile = File(vaultDir, "satellite_base.pmtiles")
            if (satFile.exists() && satFile.length() > 1024) {
                satReader = PmTilesReader(satFile)
                Log.i(TAG, "Initialized satellite PMTiles reader from ${satFile.absolutePath}")
            }

            serverSocket = ServerSocket(PORT, 50, java.net.InetAddress.getByName("127.0.0.1"))
            isRunning = true
            Log.i(TAG, "Local tile server started on loopback port $PORT")

            thread(name = "PmTilesServerThread", isDaemon = true) {
                while (isRunning) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        handleClient(client)
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local tile server", e)
        }
    }

    @Synchronized
    fun reloadReaders(vaultDir: File) {
        try {
            if (vectorReader == null) {
                val vecFile = File(vaultDir, "philippines_vector.pmtiles")
                if (vecFile.exists() && vecFile.length() > 1024) {
                    vectorReader = PmTilesReader(vecFile)
                    Log.i(TAG, "Reloaded vector PMTiles reader from ${vecFile.absolutePath}")
                }
            }
            if (topoReader == null) {
                val topoFile = File(vaultDir, "topography_contours.pmtiles")
                if (topoFile.exists() && topoFile.length() > 1024) {
                    topoReader = PmTilesReader(topoFile)
                    Log.i(TAG, "Reloaded topo PMTiles reader from ${topoFile.absolutePath}")
                }
            }
            if (satReader == null) {
                val satFile = File(vaultDir, "satellite_base.pmtiles")
                if (satFile.exists() && satFile.length() > 1024) {
                    satReader = PmTilesReader(satFile)
                    Log.i(TAG, "Reloaded satellite PMTiles reader from ${satFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading PMTiles readers", e)
        }
    }

    private fun handleClient(socket: Socket) {
        thread(isDaemon = true) {
            try {
                socket.use { s ->
                    val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                    val line = reader.readLine() ?: return@use
                    val parts = line.split(" ")
                    if (parts.size < 2 || parts[0] != "GET") return@use

                    val path = parts[1].split("?")[0]
                    val segments = path.trim('/').split('/')

                    // Font Glyph Request: /fonts/{fontstack}/{range}.pbf
                    if (segments.size >= 3 && segments[0] == "fonts") {
                        val fontStack = java.net.URLDecoder.decode(segments[1], "UTF-8")
                        val rangeWithExt = segments[2]
                        val glyphBytes = loadFontGlyphs(fontStack, rangeWithExt)
                        if (glyphBytes != null && glyphBytes.isNotEmpty()) {
                            val out = BufferedOutputStream(s.getOutputStream())
                            val headerBuilder = StringBuilder()
                            headerBuilder.append("HTTP/1.1 200 OK\r\n")
                            headerBuilder.append("Content-Type: application/x-protobuf\r\n")
                            headerBuilder.append("Content-Length: ${glyphBytes.size}\r\n")
                            headerBuilder.append("Access-Control-Allow-Origin: *\r\n")
                            headerBuilder.append("Cache-Control: max-age=604800\r\n\r\n")
                            out.write(headerBuilder.toString().toByteArray())
                            out.write(glyphBytes)
                            out.flush()
                            return@use
                        }
                    }

                    // Map Tile Request: /{layer}/{z}/{x}/{y}.{ext}
                    if (segments.size == 4) {
                        val layer = segments[0].lowercase()
                        val z = segments[1].toIntOrNull()
                        val x = segments[2].toIntOrNull()
                        val yWithExt = segments[3].split(".")[0]
                        val y = yWithExt.toIntOrNull()

                        if (z != null && x != null && y != null) {
                            val (tileBytes, contentType, isGzip) = when (layer) {
                                "vector" -> Triple(vectorReader?.getTile(z, x, y), "application/x-protobuf", true)
                                "topo", "topography" -> Triple(topoReader?.getTile(z, x, y), "image/webp", false)
                                "satellite", "sat" -> Triple(satReader?.getTile(z, x, y), "image/jpeg", false)
                                else -> Triple(null, "application/octet-stream", false)
                            }

                            val out = BufferedOutputStream(s.getOutputStream())
                            if (tileBytes != null && tileBytes.isNotEmpty()) {
                                val headerBuilder = StringBuilder()
                                headerBuilder.append("HTTP/1.1 200 OK\r\n")
                                headerBuilder.append("Content-Type: $contentType\r\n")
                                if (isGzip) {
                                    headerBuilder.append("Content-Encoding: gzip\r\n")
                                }
                                headerBuilder.append("Content-Length: ${tileBytes.size}\r\n")
                                headerBuilder.append("Access-Control-Allow-Origin: *\r\n")
                                headerBuilder.append("Cache-Control: max-age=86400\r\n\r\n")

                                out.write(headerBuilder.toString().toByteArray())
                                out.write(tileBytes)
                                out.flush()
                                return@use
                            }
                        }
                    }

                    // 204 No Content for missing tile
                    val out = BufferedOutputStream(s.getOutputStream())
                    out.write("HTTP/1.1 204 No Content\r\nContent-Length: 0\r\nAccess-Control-Allow-Origin: *\r\n\r\n".toByteArray())
                    out.flush()
                }
            } catch (e: Exception) {
                // Ignore transient socket close
            }
        }
    }

    private fun loadFontGlyphs(fontStack: String, rangeWithExt: String): ByteArray? {
        val ctx = appContext ?: return null
        val candidates = listOf(
            "fonts/$fontStack/$rangeWithExt",
            "fonts/Noto Sans Medium/$rangeWithExt",
            "fonts/Noto Sans Regular/$rangeWithExt",
            "fonts/Noto Sans Medium/0-255.pbf",
            "fonts/Noto Sans Regular/0-255.pbf"
        )
        for (candidate in candidates) {
            try {
                ctx.assets.open(candidate).use { input ->
                    return input.readBytes()
                }
            } catch (e: Exception) {
                // Try next candidate
            }
        }
        return null
    }

    @Synchronized
    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
        vectorReader?.close()
        vectorReader = null
        topoReader?.close()
        topoReader = null
        satReader?.close()
        satReader = null
    }
}
