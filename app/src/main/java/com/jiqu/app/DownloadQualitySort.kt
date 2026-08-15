package com.jiqu.app

private val QUALITY_RESOLUTION_PATTERN = Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE)

internal fun sortDownloadQualities(downloads: List<ParsedDownload>): List<ParsedDownload> =
    downloads.sortedWith(
        compareBy<ParsedDownload> { if (it.isOriginal) 0 else 1 }
            .thenByDescending { download ->
                listOfNotNull(download.width, download.height).minOrNull()
                    ?: QUALITY_RESOLUTION_PATTERN.find(download.label)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.toIntOrNull()
                    ?: 0
            }
            .thenBy { it.label }
    )
