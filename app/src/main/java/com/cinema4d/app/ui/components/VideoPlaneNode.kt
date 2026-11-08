package com.cinema4d.app.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Stream
import com.google.android.filament.Texture
import io.github.sceneview.geometries.Plane
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.PlaneNode

/**
 * A plane that plays a [MediaPlayer] video in the 3D scene.
 *
 * The player's output is routed to an internal [SurfaceTexture], exposed to Filament as an
 * external [Texture] through a [Stream], and shaded with SceneView's video material. The
 * scene renders continuously, so frames land on the plane as the player produces them.
 */
class VideoPlaneNode private constructor(
    private val materialLoader: MaterialLoader,
    val player: MediaPlayer,
    private val surfaceTexture: SurfaceTexture,
    private val surface: Surface,
    private val stream: Stream,
    private val texture: Texture,
    geometry: Plane,
    materialInstance: MaterialInstance,
) : PlaneNode(
    engine = materialLoader.engine,
    geometry = geometry,
    materialInstance = materialInstance,
) {
    override fun destroy() {
        runCatching { player.setSurface(null) }
        runCatching { player.stop() }
        runCatching { player.release() }
        runCatching { surface.release() }
        runCatching { surfaceTexture.release() }
        runCatching { materialLoader.engine.destroyStream(stream) }
        runCatching { materialLoader.engine.destroyTexture(texture) }
        super.destroy()
    }

    companion object {
        fun create(
            materialLoader: MaterialLoader,
            player: MediaPlayer,
            size: Size,
            center: Position,
            normal: Direction,
        ): VideoPlaneNode {
            val engine = materialLoader.engine
            val surfaceTexture = SurfaceTexture(0).also { it.detachFromGLContext() }
            val stream = Stream.Builder().stream(surfaceTexture).build(engine)
            val texture = Texture.Builder()
                .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
                .format(Texture.InternalFormat.RGB8)
                .build(engine)
                .apply { setExternalStream(engine, stream) }
            val materialInstance = materialLoader.createVideoInstance(texture, null)
            val geometry = Plane.Builder()
                .size(size)
                .center(center)
                .normal(normal)
                .build(engine)
            val surface = Surface(surfaceTexture)
            player.setSurface(surface)
            return VideoPlaneNode(
                materialLoader = materialLoader,
                player = player,
                surfaceTexture = surfaceTexture,
                surface = surface,
                stream = stream,
                texture = texture,
                geometry = geometry,
                materialInstance = materialInstance,
            )
        }
    }
}
