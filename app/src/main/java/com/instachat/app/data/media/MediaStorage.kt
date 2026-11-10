package com.instachat.app.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PendingCapture(val file: File, val uri: Uri)

enum class AvatarStyle { BOB, CURLS }

/**
 * All user media lives in app-private storage. The database only ever holds
 * absolute paths to files inside filesDir/media.
 */
@Singleton
class MediaStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mediaRoot = File(context.filesDir, "media")
    private val imageDir = File(mediaRoot, "images")
    private val voiceDir = File(mediaRoot, "voice")
    private val avatarDir = File(mediaRoot, "avatars")
    private val cameraDir = File(context.cacheDir, "camera")

    init {
        imageDir.mkdirs()
        voiceDir.mkdirs()
        avatarDir.mkdirs()
        cameraDir.mkdirs()
    }

    fun createCameraCapture(): PendingCapture {
        cameraDir.mkdirs()
        val file = File(cameraDir, "capture_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return PendingCapture(file, uri)
    }

    fun createVoiceFile(): File {
        voiceDir.mkdirs()
        return File(voiceDir, "voice_${UUID.randomUUID()}.m4a")
    }


    suspend fun importImage(uri: Uri, maxDimension: Int = 1920): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@runCatching null
                val bitmap = decodeNormalized(bytes, maxDimension) ?: return@runCatching null
                imageDir.mkdirs()
                val file = File(imageDir, "img_${UUID.randomUUID()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                bitmap.recycle()
                file.absolutePath
            }.getOrNull()
        }

    suspend fun importAvatar(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@runCatching null
            val bitmap = decodeNormalized(bytes, 1024) ?: return@runCatching null
            avatarDir.mkdirs()
            val file = File(avatarDir, "avatar_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
            }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    suspend fun importAudio(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val extension = resolveAudioExtension(uri)
            voiceDir.mkdirs()
            val file = File(voiceDir, "audio_${UUID.randomUUID()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            if (file.length() <= 0L) {
                file.delete()
                null
            } else {
                file.absolutePath
            }
        }.getOrNull()
    }

    suspend fun durationMs(path: String): Long? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun deletePath(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            val allowed = file.absolutePath.startsWith(mediaRoot.absolutePath) ||
                file.absolutePath.startsWith(cameraDir.absolutePath)
            if (allowed && file.exists()) file.delete()
        }
    }

    fun deleteCapture(file: File) {
        if (file.absolutePath.startsWith(cameraDir.absolutePath)) file.delete()
    }

    fun clearAllMedia() {
        mediaRoot.listFiles()?.forEach { child ->
            if (child.isDirectory) child.listFiles()?.forEach { it.delete() }
        }
        cameraDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Draws an original, stylized illustrated portrait for the seed contacts.
     * No external or trademarked artwork is used.
     */
    suspend fun generateSeedAvatar(style: AvatarStyle): String? = withContext(Dispatchers.IO) {
        runCatching {
            val size = 512
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            val bgStart: Int
            val bgEnd: Int
            val skin: Int
            val hair: Int
            val shirt: Int
            when (style) {
                AvatarStyle.BOB -> {
                    bgStart = Color.parseColor("#FF9E9E")
                    bgEnd = Color.parseColor("#FF6A88")
                    skin = Color.parseColor("#F0C3A4")
                    hair = Color.parseColor("#3B2624")
                    shirt = Color.parseColor("#5B3E70")
                }
                AvatarStyle.CURLS -> {
                    bgStart = Color.parseColor("#74D4FF")
                    bgEnd = Color.parseColor("#5061FF")
                    skin = Color.parseColor("#D9A47C")
                    hair = Color.parseColor("#211B26")
                    shirt = Color.parseColor("#F0E6D8")
                }
            }

            paint.shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(), bgStart, bgEnd, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            paint.shader = null

            paint.color = Color.argb(38, 255, 255, 255)
            canvas.drawCircle(90f, 96f, 120f, paint)
            canvas.drawCircle(445f, 385f, 150f, paint)
            paint.color = Color.argb(24, 255, 255, 255)
            canvas.drawCircle(420f, 90f, 60f, paint)

            // Shoulders / shirt.
            paint.color = shirt
            canvas.drawOval(RectF(72f, 360f, 440f, 660f), paint)
            paint.color = Color.argb(28, 255, 255, 255)
            canvas.drawOval(RectF(180f, 400f, 332f, 470f), paint)

            // Neck.
            paint.color = skin
            canvas.drawRoundRect(RectF(216f, 300f, 296f, 390f), 34f, 34f, paint)

            // Long hair silhouette (behind the face) for the bob style.
            if (style == AvatarStyle.BOB) {
                paint.color = hair
                canvas.drawRoundRect(RectF(140f, 136f, 372f, 392f), 116f, 116f, paint)
            } else {
                // Curly halo.
                paint.color = hair
                val curls = listOf(
                    Triple(180f, 170f, 54f),
                    Triple(235f, 138f, 62f),
                    Triple(300f, 145f, 58f),
                    Triple(348f, 195f, 50f),
                    Triple(160f, 235f, 46f),
                    Triple(360f, 250f, 42f)
                )
                curls.forEach { (x, y, r) -> canvas.drawCircle(x, y, r, paint) }
            }

            // Face.
            paint.color = skin
            canvas.drawOval(RectF(164f, 146f, 348f, 360f), paint)

            // Ears.
            canvas.drawCircle(164f, 252f, 22f, paint)
            canvas.drawCircle(348f, 252f, 22f, paint)

            // Hairline / fringe.
            paint.color = hair
            val fringe = Path().apply {
                moveTo(164f, 226f)
                cubicTo(176f, 142f, 330f, 130f, 352f, 232f)
                cubicTo(318f, 206f, 290f, 198f, 256f, 200f)
                cubicTo(220f, 202f, 190f, 208f, 164f, 226f)
                close()
            }
            canvas.drawPath(fringe, paint)
            if (style == AvatarStyle.CURLS) {
                canvas.drawCircle(170f, 210f, 28f, paint)
                canvas.drawCircle(342f, 212f, 27f, paint)
            }

            // Eyes, brows, nose, smile.
            paint.color = Color.parseColor("#2B2024")
            canvas.drawCircle(222f, 254f, 8.5f, paint)
            canvas.drawCircle(290f, 254f, 8.5f, paint)
            paint.color = Color.argb(80, 255, 255, 255)
            canvas.drawCircle(225f, 251f, 2.3f, paint)
            canvas.drawCircle(293f, 251f, 2.3f, paint)

            paint.color = Color.parseColor("#5A3B33")
            paint.strokeWidth = 6f
            paint.style = Paint.Style.STROKE
            canvas.drawArc(RectF(199f, 218f, 246f, 240f), 200f, 140f, false, paint)
            canvas.drawArc(RectF(266f, 218f, 313f, 240f), 200f, 140f, false, paint)

            paint.color = Color.argb(70, 150, 70, 70)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(198f, 291f, 19f, paint)
            canvas.drawCircle(314f, 291f, 19f, paint)

            paint.color = Color.parseColor("#B87364")
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            canvas.drawArc(RectF(249f, 280f, 263f, 300f), 70f, 90f, false, paint)

            paint.color = Color.parseColor("#8C4A45")
            paint.strokeWidth = 7f
            canvas.drawArc(RectF(225f, 293f, 287f, 335f), 20f, 140f, false, paint)
            paint.style = Paint.Style.FILL

            avatarDir.mkdirs()
            val file = File(avatarDir, "seed_${style.name.lowercase()}_${UUID.randomUUID()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    /** An original synthetic "golden hour rooftop" image used in the demo chat. */
    suspend fun generateSeedImage(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val width = 1080
            val height = 720
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            paint.shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#241447"),
                    Color.parseColor("#7A3B7A"),
                    Color.parseColor("#FF6A88"),
                    Color.parseColor("#FFB86B")
                ),
                floatArrayOf(0f, 0.42f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null

            // Sun glow.
            paint.shader = RadialGradient(
                770f, 360f, 280f,
                intArrayOf(Color.argb(130, 255, 237, 178), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(770f, 360f, 280f, paint)
            paint.shader = null
            paint.color = Color.parseColor("#FFEDB2")
            canvas.drawCircle(770f, 360f, 92f, paint)

            // Distant skyline.
            paint.color = Color.parseColor("#3B2B55")
            val buildings = listOf(
                floatArrayOf(0f, 505f, 120f, 720f),
                floatArrayOf(104f, 460f, 190f, 720f),
                floatArrayOf(180f, 530f, 260f, 720f),
                floatArrayOf(250f, 430f, 352f, 720f),
                floatArrayOf(340f, 490f, 420f, 720f),
                floatArrayOf(410f, 380f, 505f, 720f),
                floatArrayOf(495f, 470f, 590f, 720f),
                floatArrayOf(580f, 420f, 690f, 720f),
                floatArrayOf(680f, 500f, 760f, 720f),
                floatArrayOf(750f, 445f, 840f, 720f),
                floatArrayOf(830f, 510f, 930f, 720f),
                floatArrayOf(920f, 400f, 1030f, 720f),
                floatArrayOf(1020f, 480f, 1080f, 720f)
            )
            buildings.forEach { b ->
                canvas.drawRect(b[0], b[1], b[2], b[3], paint)
            }

            // Warm windows.
            paint.color = Color.argb(170, 255, 205, 130)
            val random = java.util.Random(42)
            buildings.forEach { b ->
                var y = b[1] + 28f
                while (y < height - 30f) {
                    var x = b[0] + 18f
                    while (x < b[2] - 12f) {
                        if (random.nextFloat() > 0.55f) {
                            canvas.drawRect(x, y, x + 9f, y + 13f, paint)
                        }
                        x += 28f
                    }
                    y += 34f
                }
            }

            // Waterfront band and reflections.
            paint.color = Color.parseColor("#241B3D")
            canvas.drawRect(0f, 650f, width.toFloat(), height.toFloat(), paint)
            paint.color = Color.argb(90, 255, 190, 130)
            for (i in 0 until 16) {
                val w = 120f + random.nextFloat() * 280f
                val x = random.nextFloat() * (width - w)
                val y = 662f + random.nextFloat() * 45f
                canvas.drawRoundRect(RectF(x, y, x + w, y + 4f), 2f, 2f, paint)
            }

            // Soft film grain dots.
            paint.color = Color.argb(22, 255, 255, 255)
            repeat(900) {
                val x = random.nextFloat() * width
                val y = random.nextFloat() * height
                canvas.drawCircle(x, y, 1.2f, paint)
            }

            imageDir.mkdirs()
            val file = File(imageDir, "seed_rooftop_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    private fun resolveAudioExtension(uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        mime?.let {
            when {
                it.contains("mpeg") -> return "mp3"
                it.contains("mp4") || it.contains("m4a") -> return "m4a"
                it.contains("wav") -> return "wav"
                it.contains("ogg") -> return "ogg"
                it.contains("aac") -> return "aac"
                it.contains("3gpp") -> return "3gp"
            }
        }
        val name = queryDisplayName(uri)
        val ext = name?.substringAfterLast('.', "")?.lowercase()
        if (!ext.isNullOrBlank() && ext.length in 1..5) return ext
        return "m4a"
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else null
                }
        }.getOrNull()
    }

    private fun decodeNormalized(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null

        val rotation = runCatching {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)

        val rotated = if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }

        val longest = max(rotated.width, rotated.height)
        val scaled = if (longest > maxDimension) {
            val factor = maxDimension.toFloat() / longest.toFloat()
            val targetW = max(1, (rotated.width * factor).toInt())
            val targetH = max(1, (rotated.height * factor).toInt())
            Bitmap.createScaledBitmap(rotated, targetW, targetH, true)
                .also { if (it !== rotated) rotated.recycle() }
        } else {
            rotated
        }
        return scaled
    }
}
