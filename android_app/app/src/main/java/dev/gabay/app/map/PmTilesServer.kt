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

    @Synchronized
    fun start(vaultDir: File) {
        if (isRunning) return

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

            serverSocket = ServerSocket(PORT)
            isRunning = true
            Log.i(TAG, "Local tile server started on port $PORT")

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

                    if (segments.size == 4) {
                        val layer = segments[0]
                        val z = segments[1].toIntOrNull()
                        val x = segments[2].toIntOrNull()
                        val yWithExt = segments[3].split(".")[0]
                        val y = yWithExt.toIntOrNull()

                        if (z != null && x != null && y != null) {
                            val (tileBytes, contentType, isGzip) = when (layer) {
                                "vector" -> Triple(vectorReader?.getTile(z, x, y), "application/x-protobuf", true)
                                "topo" -> Triple(topoReader?.getTile(z, x, y), "image/webp", false)
                                "satellite" -> Triple(satReader?.getTile(z, x, y), "image/jpeg", false)
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
