package com.jiqu.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {
    @Test
    fun detectsNewerReleaseVersions() {
        assertTrue(isRemoteVersionNewer("v1.5.3", "1.5.2"))
        assertTrue(isRemoteVersionNewer("1.6", "1.5.9"))
        assertTrue(isRemoteVersionNewer("2.0.0", "1.99.99"))
    }

    @Test
    fun rejectsSameOlderAndMalformedReleaseVersions() {
        assertFalse(isRemoteVersionNewer("v1.5.2", "1.5.2"))
        assertFalse(isRemoteVersionNewer("1.5.1", "1.5.2"))
        assertFalse(isRemoteVersionNewer("release-1.5.3", "1.5.2"))
        assertFalse(isRemoteVersionNewer("1.beta.3", "1.5.2"))
    }

    @Test
    fun acceptsOnlyTheOfficialGitHubAssetUrl() {
        assertTrue(isTrustedGitHubAssetUrl("https://github.com/dhvbjvvb/jiqu/releases/download/v1.5.3/app-release.apk"))
        assertFalse(isTrustedGitHubAssetUrl("http://github.com/dhvbjvvb/jiqu/releases/download/v1.5.3/app-release.apk"))
        assertFalse(isTrustedGitHubAssetUrl("https://github.com.example.com/app-release.apk"))
        assertFalse(isTrustedGitHubAssetUrl("https://github.com/another/repository/releases/download/v1.5.3/app-release.apk"))
        assertFalse(isTrustedGitHubAssetUrl("https://github.com/dhvbjvvb/jiqu/releases/download/v1.5.3/other.apk"))
    }
}
