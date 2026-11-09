package com.cinema4d.app.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cinema4d.app.data.model.SeatInfo
import com.cinema4d.app.data.model.SeatMapDocument
import com.cinema4d.app.data.repository.TrainRepository
import com.cinema4d.app.ui.components.GlassSurface
import com.cinema4d.app.ui.components.GlassTextPrimary
import com.cinema4d.app.ui.components.GlassTextSecondary
import com.cinema4d.app.ui.components.VideoPlaneNode
import com.google.android.filament.LightManager
import io.github.sceneview.Scene
import io.github.sceneview.environment.Environment
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * First-person look state for a seat: eye position plus yaw/pitch, with the
 * offsets that dragging adds on top of the base orientation.
 */
private class CoachLookState {
    var eye = Position(0f, 0f, 0f)
    var baseYaw = 0f
    var basePitch = 0f
    var yawOff = 0f
    var pitchOff = 0f
    var camPos = Position(0f, 1.3f, 0f)
    private var travelId = 0
    private var traveling = false
    private var pendingEye = Position(0f, 0f, 0f)
    private var pendingYaw = 0f
    private var pendingPitch = 0f

    fun apply(cameraNode: CameraNode) {
        val yaw = baseYaw + yawOff
        val pitch = (basePitch + pitchOff).coerceIn(-1.1f, 1.1f)
        val cp = cos(pitch)
        val fx = sin(yaw) * cp
        val fy = sin(pitch)
        val fz = cos(yaw) * cp
        camPos = Position(eye.x, eye.y, eye.z)
        cameraNode.position = camPos
        cameraNode.lookAt(Position(eye.x + fx, eye.y + fy, eye.z + fz), smooth = false)
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
        val startPitch = (basePitch + pitchOff).coerceIn(-1.1f, 1.1f)
        var dYaw = endYaw - startYaw
        while (dYaw > Math.PI.toFloat()) dYaw -= (Math.PI * 2).toFloat()
        while (dYaw < -Math.PI.toFloat()) dYaw += (Math.PI * 2).toFloat()

        val dist = sqrt(
            (targetEye.x - start.x) * (targetEye.x - start.x) +
                (targetEye.y - start.y) * (targetEye.y - start.y) +
                (targetEye.z - start.z) * (targetEye.z - start.z)
        )
        val durationNs = (520f + dist * 45f).coerceIn(520f, 900f) * 1_000_000f
        // the coach floor is flat, so a small lift is all that's needed to clear armrests
        val lift = 0.10f + dist * 0.02f

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
        traveling = false
        apply(cameraNode)
    }

    fun interruptTravel() {
        if (!traveling) return
        travelId++
        traveling = false
        eye = pendingEye
        baseYaw = pendingYaw
        basePitch = pendingPitch
        yawOff = 0f
        pitchOff = 0f
    }
}

/**
 * Renders the chair-car interior (assets/models/coach.glb) and places the camera
 * at the selected seat's eye height, looking forward-left so the window and the
 * rows ahead are both in frame. The window strip plays a looping scenery clip.
 */
@Composable
fun Coach3DView(
    seatMap: SeatMapDocument,
    previewSeat: SeatInfo?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val environment = remember { Environment(indirectLight = null, skybox = null) }

    // look down the coach from the berth, so the aisle and the opposite
    // bank of berths are both in frame
    val lookState = remember { CoachLookState() }
    fun lookTargetFor(eye: Position) = Position(0.15f, eye.y - 0.62f, eye.z + 4.2f)

    val cameraNode = rememberCameraNode(engine) {
        position = Position(0f, 1.3f, 0f)
        lookAt(Position(0.15f, 1.18f, 5f))
        focalLength = 10.5
        setExposure(16.0f, 1.0f / 125.0f, 120.0f)
    }

    var hasPositioned by remember { mutableStateOf(false) }
    LaunchedEffect(previewSeat?.id) {
        val seat = previewSeat ?: return@LaunchedEffect
        val eye = seat.eye
        val target = if (eye.size >= 3) {
            Position(eye[0], eye[1], eye[2])
        } else {
            Position(seat.x, 1.3f, -seat.mapY)
        }
        lookState.travelTo(cameraNode, target, lookTargetFor(target), instant = !hasPositioned)
        hasPositioned = true
    }

    // window scenery, looping on the left wall behind the window openings
    val windowPlayer = remember {
        runCatching {
            MediaPlayer().apply {
                context.assets.openFd(TrainRepository.WINDOW_VIDEO_ASSET).use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                isLooping = true
                setVolume(0f, 0f)
                prepare()
                start()
            }
        }.getOrNull()
    }
    DisposableEffect(windowPlayer) {
        onDispose { windowPlayer?.release() }
    }

    val coachCreator = remember(modelLoader) {
        {
            ModelNode(
                modelInstance = modelLoader.createModelInstance(
                    assetFileLocation = TrainRepository.COACH_MODEL_ASSET,
                ),
            )
        }
    }
    val coachNode = rememberNode(coachCreator)

    val windowCreator = remember(materialLoader, windowPlayer) {
        if (windowPlayer != null) {
            {
                VideoPlaneNode.create(
                    materialLoader = materialLoader,
                    player = windowPlayer,
                    // roughly 16:9 so the scenery is not stretched
                    size = Size(1.38f, 0.78f, 0f),
                    center = Position(0f, 0f, 0f),
                    normal = Direction(1f, 0f, 0f),
                )
            }
        } else null
    }
    val windowNode = windowCreator?.let { rememberNode(it) }

    // park the window video in the opening beside the selected berth
    LaunchedEffect(previewSeat?.id) {
        val seat = previewSeat ?: return@LaunchedEffect
        windowNode?.position = Position(-1.575f, 1.41f, -seat.mapY)
    }

    // ceiling LEDs plus daylight flooding in from the window side
    val lightCreators = remember(engine) {
        val ceiling = listOf(-8f, -4.5f, -1f, 2.5f, 6f, 8.5f).map { z ->
            {
                LightNode(engine, LightManager.Type.POINT) {
                    position(0f, 2.68f, z)
                    intensity(200f)
                    color(1f, 0.95f, 0.86f)
                }
            }
        }
        val daylight = listOf(-8f, -4f, 0f, 4f, 8f).flatMap { z ->
            listOf(-1.45f, 1.45f).map { x ->
                {
                    LightNode(engine, LightManager.Type.POINT) {
                        position(x, 1.45f, z)
                        intensity(if (x < 0) 430f else 320f)
                        color(0.80f, 0.89f, 1f)
                    }
                }
            }
        }
        ceiling + daylight
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
            childNodes = listOfNotNull(coachNode, windowNode) + lightNodes,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, _, _ ->
                        lookState.interruptTravel()
                        lookState.yawOff -= pan.x * 0.005f
                        lookState.pitchOff = (lookState.pitchOff - pan.y * 0.005f)
                            .coerceIn(-1.0f, 1.0f)
                        lookState.apply(cameraNode)
                    }
                },
        )

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
                        text = "Berth ${seat.seatLabel} · ${seat.tier.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassTextPrimary,
                    )
                    Text(
                        text = "Coach S4 · Sleeper · ${if (seat.x < 0) "window side" else "aisle side"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTextSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
