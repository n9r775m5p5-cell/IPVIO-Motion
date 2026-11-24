package com.ipvio.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import io.github.ivorisnoob.smoothmotion.media3.SmoothMotion
import io.github.ivorisnoob.smoothmotion.media3.describe
import android.os.SystemClock
import kotlinx.coroutines.delay

/** "Technik-Info" (technical info; round 15, addendum 17): codec, bitrate, frame rate, dropped frames, buffer. Top left of the picture. */
@Composable
fun BoxScope.PTechOverlay(player: ExoPlayer, motion: SmoothMotion? = null) {
    var lines by remember { mutableStateOf(listOf("…")) }
    LaunchedEffect(player, motion) {
        var lastTime = SystemClock.elapsedRealtime()
        var lastInput = motion?.diagnostics?.inputFrames ?: 0L
        var lastOut = motion?.diagnostics?.emitted ?: 0L
        var lastDrawn = motion?.diagnostics?.synthesized ?: 0L
        var lastDecoded = player.videoDecoderCounters?.renderedOutputBufferCount ?: 0
        while (true) {
            val out = ArrayList<String>()
            val vf = player.videoFormat
            if (vf != null) {
                out.add("Video: " + codecName(vf.sampleMimeType).ifBlank { vf.sampleMimeType ?: "?" } + "  " + vf.width + "×" + vf.height + "  " + TechText.fps(vf.frameRate) + " fps")
                val br = if (vf.averageBitrate > 0) vf.averageBitrate else vf.bitrate
                if (br > 0) out.add("Video-Rate (laut Datei): " + TechText.kbps(br))
            } else out.add("Video: –")
            val af = player.audioFormat
            if (af != null) out.add("Ton: " + codecName(af.sampleMimeType).ifBlank { af.sampleMimeType ?: "?" } + "  " + channelLabel(af.channelCount) + "  " + (af.sampleRate / 1000.0) + " kHz")
            val dc = player.videoDecoderCounters
            if (dc != null) {
                dc.ensureUpdated()
                out.add("Bilder: ${dc.renderedOutputBufferCount} gezeigt · ${dc.droppedBufferCount} verworfen")
            }
            out.add("Puffer: " + TechText.secs(player.totalBufferedDuration))
            if (motion != null) {
                val d = motion.diagnostics
                val now = SystemClock.elapsedRealtime()
                val sec = ((now - lastTime) / 1000.0).coerceAtLeast(0.001)
                val decoded = dc?.renderedOutputBufferCount ?: 0
                out.add("MEMC V0.2: ${motion.status.value.describe()}")
                out.add("GPU supported: ${motion.isSupported} | active: ${d.active}")
                out.add(d.glInfo)
                out.add("GL input: ${d.inputFrames} | engine: ${d.engine.ifEmpty { "-" }}")
                if (now - lastTime >= 500) {
                    out.add("FPS decoder / GL in: %.1f / %.1f".format((decoded-lastDecoded)/sec,(d.inputFrames-lastInput)/sec))
                    out.add("FPS GL out / new: %.1f / %.1f".format((d.emitted-lastOut)/sec,(d.synthesized-lastDrawn)/sec))
                }
                out.add("PTS FPS: %.1f | cap: %d | target: %d".format(d.sourceFps, motion.maxFps,d.targetFps))
                out.add("Pairs: ${d.pairs} | error: ${d.lastError}")
                lastTime=now; lastInput=d.inputFrames; lastOut=d.emitted; lastDrawn=d.synthesized; lastDecoded=decoded
            }
            lines = out
            delay(1000)
        }
    }
    Column(
        Modifier.align(Alignment.TopStart).padding(16.dp).clip(RoundedCornerShape(10.dp))
            .background(Color(0xB3000000)).padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        lines.forEach { androidx.compose.foundation.text.BasicText(Lang.tr(it), style = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)) }
    }
}
