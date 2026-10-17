package dev.gabay.app.map

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

data class PmTileEntry(
    var tileId: Long,
    var offset: Long,
    var length: Long,
    var runLength: Long
)

class PmTilesReader(private val file: File) {
    private val raf: RandomAccessFile = RandomAccessFile(file, "r")
    
    val rootOffset: Long
    val rootLength: Long
    val leafOffset: Long
    val leafLength: Long
    val tileDataOffset: Long
    val tileDataLength: Long
    val tileCompression: Int
    val tileType: Int
    val minZoom: Int
    val maxZoom: Int

    private val rootEntries: List<PmTileEntry>

    init {
        val header = ByteArray(127)
        raf.seek(0)
        raf.readFully(header)

        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val magic = String(header, 0, 7)
        require(magic == "PMTiles") { "Not a PMTiles file: ${file.name}" }
        require(header[7] == 3.toByte()) { "Unsupported PMTiles version: ${header[7]}" }

        rootOffset = bb.getLong(8)
        rootLength = bb.getLong(16)
        leafOffset = bb.getLong(40)
        leafLength = bb.getLong(48)
        tileDataOffset = bb.getLong(56)
        tileDataLength = bb.getLong(64)
        tileCompression = header[98].toInt() and 0xFF
        tileType = header[99].toInt() and 0xFF
        minZoom = header[100].toInt() and 0xFF
        maxZoom = header[101].toInt() and 0xFF

        val rootBuf = ByteArray(rootLength.toInt())
        synchronized(raf) {
            raf.seek(rootOffset)
            raf.readFully(rootBuf)
        }
        rootEntries = deserializeDirectory(rootBuf)
    }

    fun getTile(z: Int, x: Int, y: Int): ByteArray? {
        if (z < minZoom || z > maxZoom) return null
        val maxCoord = (1 shl z) - 1
        if (x < 0 || x > maxCoord || y < 0 || y > maxCoord) return null

        val tileId = zxyToTileId(z, x, y)
        var entry = findTile(rootEntries, tileId)

        if (entry != null && entry.runLength == 0L) {
            val leafBuf = ByteArray(entry.length.toInt())
            synchronized(raf) {
                raf.seek(leafOffset + entry.offset)
                raf.readFully(leafBuf)
            }
            val leafEntries = deserializeDirectory(leafBuf)
            entry = findTile(leafEntries, tileId)
        }

        if (entry != null && entry.length > 0) {
            val tileBuf = ByteArray(entry.length.toInt())
            synchronized(raf) {
                raf.seek(tileDataOffset + entry.offset)
                raf.readFully(tileBuf)
            }
            return tileBuf
        }
        return null
    }

    fun close() {
        try {
            raf.close()
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        fun zxyToTileId(z: Int, x: Int, y: Int): Long {
            var acc = ((1L shl (z * 2)) - 1L) / 3L
            var a = z - 1
            var curX = x.toLong()
            var curY = y.toLong()
            while (a >= 0) {
                val s = 1L shl a
                val rx = s and curX
                val ry = s and curY
                acc += ((3L * rx) xor ry) shl a
                val rotated = rotate(s, curX, curY, rx, ry)
                curX = rotated[0]
                curY = rotated[1]
                a--
            }
            return acc
        }

        private fun rotate(n: Long, x: Long, y: Long, rx: Long, ry: Long): LongArray {
            var resX = x
            var resY = y
            if (ry == 0L) {
                if (rx != 0L) {
                    resX = n - 1L - resX
                    resY = n - 1L - resY
                }
                val tmp = resX
                resX = resY
                resY = tmp
            }
            return longArrayOf(resX, resY)
        }

        private fun readVarint(stream: InputStream): Long {
            var res = 0L
            var shift = 0
            while (true) {
                val b = stream.read()
                if (b == -1) break
                res = res or ((b and 0x7F).toLong() shl shift)
                if ((b and 0x80) == 0) break
                shift += 7
            }
            return res
        }

        fun deserializeDirectory(buf: ByteArray): List<PmTileEntry> {
            val gzip = GZIPInputStream(ByteArrayInputStream(buf))
            val numEntries = readVarint(gzip).toInt()
            val entries = ArrayList<PmTileEntry>(numEntries)

            var lastId = 0L
            for (i in 0 until numEntries) {
                val tmp = readVarint(gzip)
                entries.add(PmTileEntry(lastId + tmp, 0L, 0L, 0L))
                lastId += tmp
            }
            for (i in 0 until numEntries) {
                entries[i].runLength = readVarint(gzip)
            }
            for (i in 0 until numEntries) {
                entries[i].length = readVarint(gzip)
            }
            for (i in 0 until numEntries) {
                val tmp = readVarint(gzip)
                if (i > 0 && tmp == 0L) {
                    entries[i].offset = entries[i - 1].offset + entries[i - 1].length
                } else {
                    entries[i].offset = tmp - 1L
                }
            }
            return entries
        }

        fun findTile(entries: List<PmTileEntry>, tileId: Long): PmTileEntry? {
            var m = 0
            var n = entries.size - 1
            while (m <= n) {
                val k = (n + m) ushr 1
                val c = tileId - entries[k].tileId
                if (c > 0) {
                    m = k + 1
                } else if (c < 0) {
                    n = k - 1
                } else {
                    return entries[k]
                }
            }
            if (n >= 0) {
                if (entries[n].runLength == 0L) {
                    return entries[n]
                }
                if (tileId - entries[n].tileId < entries[n].runLength) {
                    return entries[n]
                }
            }
            return null
        }
    }
}
