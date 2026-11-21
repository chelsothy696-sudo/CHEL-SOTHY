package com.example

import com.example.model.AspectRatioPreset
import com.example.model.VideoClip
import com.example.model.VideoProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EditorModelsTest {

    @Test
    fun `clip effective duration scales by playback speed`() {
        val clip = VideoClip(
            uriString = "file:///sample.mp4",
            originalDurationMs = 10_000L,
            trimStartMs = 2_000L,
            trimEndMs = 8_000L,
            speed = 2.0f
        )
        assertEquals(6_000L, clip.trimmedSourceDurationMs)
        assertEquals(3_000L, clip.effectiveDurationMs)
    }

    @Test
    fun `project resolves clip index and local offset at playhead`() {
        val clip1 = VideoClip(uriString = "file:///a.mp4", originalDurationMs = 4_000L, trimEndMs = 4_000L)
        val clip2 = VideoClip(uriString = "file:///b.mp4", originalDurationMs = 6_000L, trimEndMs = 6_000L)
        val project = VideoProject(
            title = "Test",
            aspectRatio = AspectRatioPreset.RATIO_9_16,
            clips = listOf(clip1, clip2)
        )

        assertEquals(10_000L, project.totalDurationMs)
        val hit = project.clipAtTimelinePosition(5_500L)
        assertNotNull(hit)
        assertEquals(1, hit?.first)
        assertEquals(1_500L, hit?.second)
    }
}
