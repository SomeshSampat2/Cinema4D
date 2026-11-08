package com.cinema4d.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import com.google.android.filament.LightManager
import io.github.sceneview.Scene
import io.github.sceneview.environment.Environment
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ImageNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import com.cinema4d.app.data.model.SeatInfo
import com.cinema4d.app.data.model.SeatMapDocument
import com.cinema4d.app.data.repository.SeatMapRepository
import com.cinema4d.app.ui.components.GlassIconButton
import com.cinema4d.app.ui.components.SpeakerOffIcon
import com.cinema4d.app.ui.components.SpeakerOnIcon
import com.cinema4d.app.ui.components.GlassSurface
import com.cinema4d.app.ui.components.VideoPlaneNode
import com.cinema4d.app.ui.components.GlassTextPrimary
import com.cinema4d.app.ui.components.GlassTextSecondary

/**
 * First-person look state for the seat camera: the seat's eye position plus the
 * view direction decomposed into yaw/pitch. Dragging the 3D view adds offsets on
 * top of the base orientation (which always faces the screen); pinch dollies
 * slightly forward/back along the view axis. Position never leaves the seat.
 */
private class SeatLookState {
    var eye = Position(0f, 0f, 0f)
    var baseYaw = 0f
    var basePitch = 0f
    var yawOff = 0f
    var pitchOff = 0f
    var dolly = 0f

    // live camera pose (updated every frame while flying between seats)
    var camPos = Position(0f, 2f, 0f)
    private var travelId = 0
    var traveling = false
        private set
    private var pendingEye = Position(0f, 0f, 0f)
    private var pendingYaw = 0f
    private var pendingPitch = 0f

    fun apply(cameraNode: CameraNode) {
        val yaw = baseYaw + yawOff
        val pitch = (basePitch + pitchOff).coerceIn(-1.2f, 1.2f)
        val cp = cos(pitch)
        val fx = sin(yaw) * cp
        val fy = sin(pitch)
        val fz = cos(yaw) * cp
        val px = eye.x + fx * dolly
        val py = eye.y + fy * dolly
        val pz = eye.z + fz * dolly
        camPos = Position(px, py, pz)
        cameraNode.position = camPos
        cameraNode.lookAt(Position(px + fx, py + fy, pz + fz), smooth = false)
    }

    private fun setPose(cameraNode: CameraNode, x: Float, y: Float, z: Float, yaw: Float, pitch: Float) {
        val cp = cos(pitch)
        val fx = sin(yaw) * cp
        val fy = sin(pitch)
        val fz = cos(yaw) * cp
        camPos = Position(x, y, z)
        cameraNode.position = camPos
        cameraNode.lookAt(Position(x + fx, y + fy, z + fz), smooth = false)
    }

    /**
     * Glides the camera from wherever it is to [targetEye], easing position and
     * orientation together with a small arc lift so the move reads as a travel
     * through the hall rather than a cut. The first placement snaps instantly.
     */
    suspend fun travelTo(cameraNode: CameraNode, targetEye: Position, lookTarget: Position, instant: Boolean) {
        val dx = lookTarget.x - targetEye.x
        val dy = lookTarget.y - targetEye.y
        val dz = lookTarget.z - targetEye.z
        val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001f)
        val endYaw = atan2(dx, dz)
        val endPitch = asin((dy / len).coerceIn(-1f, 1f))

        if (instant) {
            eye = targetEye
            baseYaw = endYaw
            basePitch = endPitch
            yawOff = 0f
            pitchOff = 0f
            dolly = 0f
            apply(cameraNode)
            return
        }

        travelId++
        val myId = travelId
        traveling = true
        pendingEye = targetEye
        pendingYaw = endYaw
        pendingPitch = endPitch

        val start = camPos
        val startYaw = baseYaw + yawOff
        val startPitch = (basePitch + pitchOff).coerceIn(-1.2f, 1.2f)
        var dYaw = endYaw - startYaw
        while (dYaw > Math.PI.toFloat()) dYaw -= (Math.PI * 2).toFloat()
        while (dYaw < -Math.PI.toFloat()) dYaw += (Math.PI * 2).toFloat()

        val travelDistance = sqrt(
            (targetEye.x - start.x) * (targetEye.x - start.x) +
                (targetEye.y - start.y) * (targetEye.y - start.y) +
                (targetEye.z - start.z) * (targetEye.z - start.z)
        )
        val durationNs = (620f + travelDistance * 55f).coerceIn(620f, 1100f) * 1_000_000f
        // rows rise toward the back, so a straight line between two seats cuts
        // through seat backs — arc high enough to clear the tallest row between
        val heightDelta = abs(targetEye.y - start.y)
        val lift = (heightDelta * 0.5f + 0.24f).coerceAtLeast(0.12f)

        val t0 = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            if (myId != travelId) return
            val t = ((now - t0) / durationNs).coerceIn(0f, 1f)
            val e = if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f) * (-2f * t + 2f) * (-2f * t + 2f) / 2f
            val arc = sin(Math.PI * t).toFloat() * lift
            setPose(
                cameraNode,
                start.x + (targetEye.x - start.x) * e,
                start.y + (targetEye.y - start.y) * e + arc,
                start.z + (targetEye.z - start.z) * e,
                startYaw + dYaw * e,
                startPitch + (endPitch - startPitch) * e,
            )
            if (t >= 1f) break
        }

        eye = targetEye
        baseYaw = endYaw
        basePitch = endPitch
        yawOff = 0f
        pitchOff = 0f
        dolly = 0f
        traveling = false
        apply(cameraNode)
    }

    /** A gesture mid-flight takes over: finish the move instantly, then let the drag apply. */
    fun interruptTravel() {
        if (!traveling) return
        travelId++
        traveling = false
        eye = pendingEye
        baseYaw = pendingYaw
        basePitch = pendingPitch
        yawOff = 0f
        pitchOff = 0f
        dolly = 0f
    }
}

/**
 * Renders the Blender-modeled auditorium (assets/models/hall.glb) and places the
 * camera at the selected seat's eye position, looking at the screen. The movie's
 * widescreen artwork is projected onto an image plane at the screen location.
 * Swipe to look around the auditorium; pinch to lean in/out.
 */
@Composable
fun Seat3DView(
    seatMap: SeatMapDocument,
    previewSeat: SeatInfo?,
    screenArtAsset: String?,
    trailerAsset: String? = null,
    showAudioToggle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val environment = remember { Environment(indirectLight = null, skybox = null) }

    val screenCenter = seatMap.hall.screen.center
    val lookTarget = Position(
        screenCenter.getOrElse(0) { 0f },
        screenCenter.getOrElse(1) { 3f } - 1.5f,
        screenCenter.getOrElse(2) { 9.9f },
    )

    val cameraNode = rememberCameraNode(engine) {
        position = Position(0f, 2.0f, 0f)
        lookAt(lookTarget)
        // ~9.4 mm on the default 24 mm sensor: a wide portrait field so the
        // 10.6 m screen sits inside the frame with the rows below it. Set as a
        // lens (not an FOV) because SceneView re-derives the projection from
        // focal length whenever the surface resizes.
        focalLength = 9.4
        // The default exposure is tuned for sunlight (~100k lux); the hall is
        // lit by interior fixtures (~1-2k lux), so open up ~6 stops via ISO.
        setExposure(16.0f, 1.0f / 125.0f, 100.0f)
    }

    val lookState = remember { SeatLookState() }

    // Fly the camera to the newly picked seat; the first placement snaps.
    var hasPositioned by remember { mutableStateOf(false) }
    LaunchedEffect(previewSeat?.id) {
        val seat = previewSeat ?: return@LaunchedEffect
        val eye = seat.eye
        val target = if (eye.size >= 3) {
            Position(eye[0], eye[1], eye[2])
        } else {
            Position(seat.x, 2.0f, -seat.mapY)
        }
        lookState.travelTo(cameraNode, target, lookTarget, instant = !hasPositioned)
        hasPositioned = true
    }

    val screenW = seatMap.hall.screen.w.takeIf { it > 0f } ?: 10.6f
    val screenH = seatMap.hall.screen.h.takeIf { it > 0f } ?: 5.0f

    val screenBitmap = remember(screenArtAsset, screenW, screenH) {
        screenArtAsset?.let {
            runCatching {
                context.assets.open(it).use { stream -> BitmapFactory.decodeStream(stream) }
            }.getOrNull()?.let { src ->
                // Center-crop poster art to the screen's aspect, then lift
                // exposure so the screen glows like a real auditorium surface.
                val targetAspect = screenW / screenH
                val srcAspect = src.width.toFloat() / src.height
                val (cw, ch, cx, cy) = if (srcAspect > targetAspect) {
                    val w = (src.height * targetAspect).toInt()
                    arrayOf(w, src.height, (src.width - w) / 2, 0)
                } else {
                    val h = (src.width / targetAspect).toInt()
                    arrayOf(src.width, h, 0, (src.height - h) / 2)
                }
                val cropped = Bitmap.createBitmap(src, cx, cy, cw, ch)
                val boosted = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(boosted)
                val paint = android.graphics.Paint().apply {
                    colorFilter = android.graphics.ColorMatrixColorFilter(
                        android.graphics.ColorMatrix(
                            floatArrayOf(
                                1.3f, 0f, 0f, 0f, 0f,
                                0f, 1.3f, 0f, 0f, 0f,
                                0f, 0f, 1.3f, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f,
                            )
                        )
                    )
                }
                canvas.drawBitmap(cropped, 0f, 0f, paint)
                boosted
            }
        }
    }

    val screenZ = screenCenter.getOrElse(2) { 9.92f } - 0.14f
    val screenY = screenCenter.getOrElse(1) { 3.45f }

    // Trailer loop on the cinema screen: muted, looping, and sized to fit the
    // screen height so the picture keeps its aspect ratio (flat-ratio masking).
    val mediaPlayer = remember(trailerAsset) {
        trailerAsset?.let { path ->
            runCatching {
                MediaPlayer().apply {
                    context.assets.openFd(path).use { afd ->
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    }
                    isLooping = true
                    setVolume(0f, 0f)
                    prepare()
                    start()
                }
            }.getOrNull()
        }
    }
    DisposableEffect(mediaPlayer) {
        onDispose { mediaPlayer?.release() }
    }

    // Trailers start muted; the corner toggle unmutes the hall.
    var soundOn by remember { mutableStateOf(false) }
    LaunchedEffect(mediaPlayer, soundOn) {
        val player = mediaPlayer ?: return@LaunchedEffect
        runCatching {
            if (soundOn) player.setVolume(1f, 1f) else player.setVolume(0f, 0f)
        }
    }

    val videoSize = mediaPlayer?.let { player ->
        val vw = player.videoWidth.takeIf { it > 0 } ?: 16
        val vh = player.videoHeight.takeIf { it > 0 } ?: 9
        val h = screenH
        Size(h * vw.toFloat() / vh.toFloat(), h, 0f)
    }
    val videoCreator = remember(materialLoader, mediaPlayer, videoSize, screenY, screenZ) {
        if (mediaPlayer != null && videoSize != null) {
            {
                VideoPlaneNode.create(
                    materialLoader = materialLoader,
                    player = mediaPlayer,
                    size = videoSize,
                    center = Position(0f, screenY, screenZ),
                    normal = Direction(0f, 0f, -1f),
                )
            }
        } else null
    }
    val videoNode = videoCreator?.let { rememberNode(it) }

    // Stable creators — rememberNode keys on the lambda instance, so the lambdas
    // themselves must be remembered or nodes would rebuild on every recomposition.
    val hallCreator = remember(modelLoader) {
        {
            ModelNode(
                modelInstance = modelLoader.createModelInstance(
                    assetFileLocation = SeatMapRepository.HALL_MODEL_ASSET,
                ),
            )
        }
    }
    val hallNode = rememberNode(hallCreator)

    val screenCreator = remember(materialLoader, screenBitmap, screenW, screenH, screenY, screenZ) {
        screenBitmap?.let { bmp ->
            {
                ImageNode(
                    materialLoader = materialLoader,
                    bitmap = bmp,
                    size = Size(screenW, screenH, 0f),
                    center = Position(0f, screenY, screenZ),
                    normal = Direction(0f, 0f, -1f),
                )
            }
        }
    }
    val screenNode = screenCreator?.let { rememberNode(it) }

    // House lighting: dim warm ceiling pools plus a cool spill from the screen
    // so seat backs near the front catch its glow, the way they do on set.
    val lightCreators = remember(engine) {
        val housePositions = listOf(-3.6f, 3.6f).flatMap { x ->
            listOf(-6.5f, -2.2f, 2.2f, 6.5f).map { z -> Position(x, 6.4f, z) }
        }
        val house = housePositions.map { pos ->
            {
                LightNode(engine, LightManager.Type.POINT) {
                    position(pos.x, pos.y, pos.z)
                    intensity(700f)
                    color(1f, 0.78f, 0.52f)
                }
            }
        }
        val screenSpill = {
            LightNode(engine, LightManager.Type.POINT) {
                position(0f, screenY, screenZ - 2.4f)
                intensity(300f)
                color(0.85f, 0.88f, 1f)
            }
        }
        // dim rear fill so seat backs turned away from the screen still read
        val rearFill = listOf(-3.2f, 3.2f).map { x ->
            {
                LightNode(engine, LightManager.Type.POINT) {
                    position(x, 5.2f, -8.4f)
                    intensity(380f)
                    color(1f, 0.72f, 0.48f)
                }
            }
        }
        house + screenSpill + rearFill
    }
    val lightNodes = lightCreators.map { rememberNode(it) }

    Box(modifier = modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            environmentLoader = environmentLoader,
            environment = environment,
            cameraNode = cameraNode,
            cameraManipulator = null,
            childNodes = listOfNotNull(hallNode, videoNode ?: screenNode) + lightNodes,
        )

        // Look-around layer: one-finger drag rotates the view in place, pinch
        // dollies a little along the view direction. Sits under the info chip.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        lookState.interruptTravel()
                        lookState.yawOff -= pan.x * 0.005f
                        lookState.pitchOff = (lookState.pitchOff - pan.y * 0.005f)
                            .coerceIn(-1.1f, 1.1f)
                        lookState.dolly = (lookState.dolly + (zoom - 1f) * 0.6f)
                            .coerceIn(-0.8f, 1.0f)
                        lookState.apply(cameraNode)
                    }
                },
        )

        // audio toggle for the trailer, bottom-right of the hall view
        if (mediaPlayer != null && showAudioToggle) {
            GlassIconButton(
                icon = if (soundOn) SpeakerOnIcon else SpeakerOffIcon,
                contentDescription = if (soundOn) "Mute trailer" else "Unmute trailer",
                onClick = { soundOn = !soundOn },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 150.dp),
            )
        }

        // seat info chip
        previewSeat?.let { seat ->
            GlassSurface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Seat ${seat.seatLabel} · ${seat.tier.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    val eye = seat.eye
                    if (eye.size >= 3) {
                        val dx = screenCenter.getOrElse(0) { 0f } - eye[0]
                        val dy = screenCenter.getOrElse(1) { 3f } - eye[1]
                        val dz = screenCenter.getOrElse(2) { 9.9f } - eye[2]
                        val dist = sqrt(dx * dx + dy * dy + dz * dz)
                        val fov = (2.0 * kotlin.math.atan((screenW / 2f) / dist) * 180.0 / kotlin.math.PI).toInt()
                        Text(
                            text = "%.1f m from screen · fills %d° of your view".format(dist, fov),
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

