package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStoragePathTest {
    @Test
    fun savesImagesInThePictureDirectory() {
        assertEquals(
            DOWNLOADS_PICTURE_RELATIVE_PATH,
            downloadRelativePathFor(ParsedDownload("图片", "https://example.com/image.jpg"))
        )
    }

    @Test
    fun savesMusicInTheMusicDirectory() {
        assertEquals(
            DOWNLOADS_MUSIC_RELATIVE_PATH,
            downloadRelativePathFor(ParsedDownload("背景音乐", "https://example.com/music.mp3"))
        )
    }

    @Test
    fun savesVideoAndLivePhotoMediaInTheVideoDirectory() {
        assertEquals(
            DOWNLOADS_VIDEO_RELATIVE_PATH,
            downloadRelativePathFor(ParsedDownload("1080p", "https://example.com/video.mp4"))
        )
        assertEquals(
            DOWNLOADS_VIDEO_RELATIVE_PATH,
            downloadRelativePathFor(ParsedDownload("实况视频", "https://example.com/live.mp4"))
        )
        assertEquals(
            DOWNLOADS_VIDEO_RELATIVE_PATH,
            downloadRelativePathFor(ParsedDownload("图片", "https://example.com/animated.gif"))
        )
    }
}
