package com.jiqu.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.SurfaceTexture
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.UUID
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.abs
import com.jiqu.app.ui.theme.ThemeMode
import com.jiqu.app.ui.theme.即取Theme

private val PageShape = RoundedCornerShape(12.dp)
private val SmallShape = RoundedCornerShape(8.dp)
private val DownloadQualitySheetHeight = 420.dp
private val DescriptionPanelHeight = 240.dp
private const val GALLERY_SWIPE_DISTANCE_PX = 72f

private object CoverBitmapCache {
    private const val MAXIMUM_ENTRY_COUNT = 64
    private val bitmaps = object : LinkedHashMap<String, android.graphics.Bitmap>(MAXIMUM_ENTRY_COUNT, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, android.graphics.Bitmap>?): Boolean =
            size > MAXIMUM_ENTRY_COUNT
    }

    @Synchronized
    fun get(key: String): android.graphics.Bitmap? = bitmaps[key]

    @Synchronized
    fun put(key: String, bitmap: android.graphics.Bitmap) {
        bitmaps[key] = bitmap
    }
}

private enum class MainPage(val label: String, val icon: ImageVector) {
    Parse("解析", Icons.Outlined.Analytics),
    History("历史", Icons.Outlined.History),
    Settings("设置", Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            cleanupStaleDownloadTemporaryFiles(applicationContext)
        }
        enableEdgeToEdge()
        setContent {
            val parserViewModel: ParserViewModel = viewModel()
            val updateViewModel: UpdateViewModel = viewModel()
            即取Theme(themeMode = parserViewModel.themeMode) {
                JinanMediaApp(parserViewModel, updateViewModel)
            }
        }
    }
}

@Composable
private fun JinanMediaApp(
    parserViewModel: ParserViewModel,
    updateViewModel: UpdateViewModel
) {
    var selectedPage by rememberSaveable { mutableStateOf(MainPage.Parse) }
    var previewSessionKey by rememberSaveable { mutableIntStateOf(0) }
    val navigateToPage: (MainPage) -> Unit = { page ->
        if (page == MainPage.Parse && selectedPage != MainPage.Parse) previewSessionKey++
        selectedPage = page
    }
    BackHandler(enabled = selectedPage != MainPage.Parse) {
        navigateToPage(MainPage.Parse)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(selectedPage) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                MainPage.entries.forEach { page ->
                    NavigationBarItem(
                        selected = selectedPage == page,
                        onClick = { navigateToPage(page) },
                        icon = { Icon(page.icon, contentDescription = page.label) },
                        label = { Text(page.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedPage) {
            MainPage.Parse -> ParsePage(paddingValues, parserViewModel, previewSessionKey)
            MainPage.History -> HistoryPage(
                paddingValues = paddingValues,
                history = parserViewModel.history,
                onReparse = { entry ->
                    if (parserViewModel.reparse(entry)) navigateToPage(MainPage.Parse)
                },
                onDelete = parserViewModel::deleteHistory
            )
            MainPage.Settings -> SettingsPage(
                paddingValues = paddingValues,
                parserViewModel = parserViewModel,
                updateViewModel = updateViewModel,
                onNavigateUp = { navigateToPage(MainPage.Parse) }
            )
        }
    }

    parserViewModel.downloadTask?.let { task ->
        DownloadProgressDialog(
            task = task,
            onCancel = parserViewModel::cancelDownload,
            onClose = parserViewModel::dismissDownloadTask
        )
    }

    updateViewModel.availableUpdate?.let { update ->
        val context = LocalContext.current
        AppUpdateDialog(
            update = update,
            onDismiss = updateViewModel::dismissUpdate,
            onGitHubUpdate = {
                context.openExternalUrl(update.downloadUrl)
                updateViewModel.dismissUpdate()
            },
            onLanzouUpdate = {
                context.copyLanzouExtractCode()
                context.openExternalUrl(LANZOU_UPDATE_URL)
                updateViewModel.dismissUpdate()
            }
        )
    }
}

@Composable
private fun AppUpdateDialog(
    update: AppUpdate,
    onDismiss: () -> Unit,
    onGitHubUpdate: () -> Unit,
    onLanzouUpdate: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp),
            shape = PageShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("发现新版本", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "v${update.versionName}" + update.assetSizeBytes.toDisplayFileSize(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "点击蓝奏云后，提取码会自动复制到剪贴板：$LANZOU_EXTRACT_CODE",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (update.releaseNotes.isNotBlank()) {
                    Text(
                        text = update.releaseNotes,
                        modifier = Modifier
                            .heightIn(max = 236.dp)
                            .verticalScroll(rememberScrollState()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onGitHubUpdate,
                        modifier = Modifier.weight(1f),
                        shape = SmallShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("GitHub")
                    }
                    Button(
                        onClick = onLanzouUpdate,
                        modifier = Modifier.weight(1f),
                        shape = SmallShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("蓝奏云")
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("稍后再说")
                }
            }
        }
    }
}

private fun Long.toDisplayFileSize(): String =
    if (this > 0) "  ${String.format(Locale.US, "%.2f MB", this / (1024f * 1024f))}" else ""

@Composable
private fun AppTopBar(selectedPage: MainPage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = selectedPage.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ParsePage(
    paddingValues: PaddingValues,
    parserViewModel: ParserViewModel,
    previewSessionKey: Int
) {
    var linkError by rememberSaveable { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val lifecycleOwner = context.findActivity() as? LifecycleOwner
    val pasteFirstClipboardItem = {
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        val clipboardText = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        parserViewModel.pasteClipboardLinkIfEligible(clipboardText)
    }
    val onDownloads = { title: String, downloads: List<ParsedDownload>, mediaSequenceNumbers: Map<String, Int> ->
        if (!parserViewModel.startDownloads(title, downloads, mediaSequenceNumbers)) {
            Toast.makeText(context, "已有下载任务正在进行", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(previewSessionKey, parserViewModel.autoPasteLinks) {
        pasteFirstClipboardItem()
    }
    DisposableEffect(lifecycleOwner, parserViewModel.autoPasteLinks) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) pasteFirstClipboardItem()
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = PageShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.11f)
                            )
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("粘贴链接，开始解析", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = parserViewModel.linkText,
                    onValueChange = {
                        parserViewModel.updateLinkText(it)
                        linkError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = linkError,
                    shape = SmallShape,
                    leadingIcon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null) },
                    placeholder = { Text("请在此处粘贴分享链接") },
                    supportingText = if (linkError) {{ Text("请先粘贴分享链接") }} else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                )
                Button(
                    onClick = {
                        linkError = !parserViewModel.parseCurrent()
                    },
                    enabled = !parserViewModel.isParsing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = SmallShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (parserViewModel.isParsing) "正在解析..." else "立即解析", fontWeight = FontWeight.Medium)
                }
                when (val result = parserViewModel.result) {
                    is ParseResult.Failure -> Text(
                        result.message,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    is ParseResult.Success -> ParsedMediaResult(
                        media = result.media,
                        music = result.media.music,
                        previewSessionKey = previewSessionKey,
                        onDownload = { downloads, mediaSequenceNumbers ->
                            onDownloads(result.media.title, downloads, mediaSequenceNumbers)
                        }
                    )
                    null -> Unit
                }
            }
        }
        (parserViewModel.result as? ParseResult.Success)?.let { result ->
            DescriptionPanel(description = result.media.description)
        }

    }
}

@Composable
private fun ParsedMediaResult(
    media: ParsedMedia,
    music: ParsedDownload?,
    previewSessionKey: Int,
    onDownload: (List<ParsedDownload>, Map<String, Int>) -> Unit
) {
    var isQualitySheetVisible by remember { mutableStateOf(false) }
    val galleryItems = media.galleryItems
    val selectableDownloads = galleryItems.map(ParsedGalleryItem::download).ifEmpty { media.videoDownloads }
    val requiresSelection = galleryItems.size >= 2
    var selectedDownloadUrls by remember(media.sourceUrl) { mutableStateOf(emptySet<String>()) }
    val selectedDownloads = selectableDownloads.filter { it.url in selectedDownloadUrls }
    val downloadsToStart = if (requiresSelection) selectedDownloads else selectableDownloads
    val mediaSequenceNumbers = if (requiresSelection) {
        selectableDownloads.mapIndexed { index, download -> download.url to index }.toMap()
    } else {
        emptyMap()
    }
    val canDownload = downloadsToStart.isNotEmpty()
    val allDownloadsSelected = selectableDownloads.isNotEmpty() && selectedDownloadUrls.size == selectableDownloads.size
    val toggleAllDownloads = {
        selectedDownloadUrls = if (allDownloadsSelected) emptySet() else selectableDownloads.mapTo(linkedSetOf()) { it.url }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = media.platformName + " · " + media.mediaType,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium
        )
        MediaPreview(
            media = media,
            previewSessionKey = previewSessionKey,
            galleryItems = galleryItems,
            selectedDownloadUrls = selectedDownloadUrls,
            onSelectionChange = { url ->
                selectedDownloadUrls = if (url in selectedDownloadUrls) {
                    selectedDownloadUrls - url
                } else {
                    selectedDownloadUrls + url
                }
            }
        )
        if (requiresSelection) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = toggleAllDownloads,
                    modifier = Modifier.weight(1f),
                    shape = SmallShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.Done, contentDescription = if (allDownloadsSelected) "取消全选" else "全选")
                    Spacer(Modifier.width(6.dp))
                    Text(if (allDownloadsSelected) "取消全选" else "全选")
                }
                DownloadMediaButton(
                    mediaType = media.mediaType,
                    enabled = canDownload,
                    modifier = Modifier.weight(1f),
                    onClick = { onDownload(downloadsToStart, mediaSequenceNumbers) }
                )
            }
        } else {
            DownloadMediaButton(
                mediaType = media.mediaType,
                enabled = canDownload,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (media.mediaType == "视频") isQualitySheetVisible = true else onDownload(downloadsToStart, emptyMap())
                }
            )
        }
        if (media.mediaType == "实况" && galleryItems.isEmpty() && selectableDownloads.isEmpty()) {
            Text(
                "当前链接未提供可下载的实况动态内容",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
        music?.let { backgroundMusic ->
            MusicPreviewBar(
                music = backgroundMusic,
                onDownload = { onDownload(listOf(it), emptyMap()) }
            )
        }
    }

    if (isQualitySheetVisible && media.mediaType != "图集") {
        DownloadQualitySheet(
            downloads = selectableDownloads,
            title = downloadSheetTitleFor(media.mediaType),
            onDismiss = { isQualitySheetVisible = false },
            onDownload = { selectedDownload ->
                onDownload(listOf(selectedDownload), emptyMap())
                isQualitySheetVisible = false
            }
        )
    }
}

@Composable
private fun DownloadMediaButton(
    mediaType: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = SmallShape,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(downloadActionIconFor(mediaType), contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(downloadActionTextFor(mediaType))
    }
}

@Composable
private fun MediaPreview(
    media: ParsedMedia,
    previewSessionKey: Int,
    galleryItems: List<ParsedGalleryItem>,
    selectedDownloadUrls: Set<String>,
    onSelectionChange: (String) -> Unit
) {
    if (galleryItems.isNotEmpty()) {
        GalleryPreview(
            media = media,
            galleryItems = galleryItems,
            selectedDownloadUrls = selectedDownloadUrls,
            onSelectionChange = onSelectionChange
        )
    } else {
        VideoPreview(media = media, previewSessionKey = previewSessionKey)
    }
}

@Composable
private fun GalleryPreview(
    media: ParsedMedia,
    galleryItems: List<ParsedGalleryItem>,
    selectedDownloadUrls: Set<String>,
    onSelectionChange: (String) -> Unit
) {
    SelectableMediaGallery(
        galleryKey = media.sourceUrl,
        galleryItems = galleryItems,
        fallbackPreviewUrl = media.coverUrl,
        selectedDownloadUrls = selectedDownloadUrls,
        onSelectionChange = onSelectionChange,
        contentDescription = if (media.mediaType == "实况") "实况封面" else "图集图片"
    )
}

@Composable
private fun SelectableMediaGallery(
    galleryKey: String,
    galleryItems: List<ParsedGalleryItem>,
    fallbackPreviewUrl: String?,
    selectedDownloadUrls: Set<String>,
    onSelectionChange: (String) -> Unit,
    contentDescription: String
) {
    var currentImageIndex by rememberSaveable(galleryKey) { mutableIntStateOf(0) }
    val currentGalleryItem = galleryItems.getOrNull(currentImageIndex)
    val currentDownload = currentGalleryItem?.download
    val currentPreviewUrl = currentGalleryItem?.previewUrl ?: fallbackPreviewUrl
    val allowsSelection = galleryItems.size >= 2
    val isCurrentImageSelected = currentDownload?.url in selectedDownloadUrls

    LaunchedEffect(galleryKey, currentImageIndex, galleryItems) {
        if (galleryItems.size > 1) {
            val adjacentIndexes = listOf(
                (currentImageIndex + 1) % galleryItems.size,
                (currentImageIndex - 1 + galleryItems.size) % galleryItems.size
            ).distinct()
            withContext(Dispatchers.IO) {
                adjacentIndexes.forEach { index ->
                    galleryItems[index].previewUrl?.let(::preloadCoverBitmap)
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SmallShape,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clickable(enabled = allowsSelection && currentDownload != null) {
                    currentDownload?.url?.let(onSelectionChange)
                }
                .then(
                    if (galleryItems.size > 1) {
                        Modifier.pointerInput(galleryKey, galleryItems.size) {
                            var horizontalDragDistance = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { horizontalDragDistance = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    horizontalDragDistance += dragAmount
                                },
                                onDragEnd = {
                                    currentImageIndex = galleryIndexForSwipe(
                                        currentIndex = currentImageIndex,
                                        itemCount = galleryItems.size,
                                        horizontalDragDistance = horizontalDragDistance
                                    )
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(
                url = currentPreviewUrl,
                retainPreviousBitmapWhileLoading = true,
                contentDescription = contentDescription
            )
            if (isCurrentImageSelected) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = "已选择图片",
                        modifier = Modifier.size(56.dp),
                        tint = Color.White
                    )
                }
            }
            if (galleryItems.size > 1) {
                IconButton(
                    onClick = {
                        currentImageIndex = (currentImageIndex - 1 + galleryItems.size) % galleryItems.size
                    },
                    modifier = Modifier.align(Alignment.CenterStart).padding(8.dp)
                ) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "上一张图片", tint = Color.White)
                }
                IconButton(
                    onClick = { currentImageIndex = (currentImageIndex + 1) % galleryItems.size },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp)
                ) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "下一张图片", tint = Color.White)
                }
            }
            if (galleryItems.isNotEmpty()) {
                Text(
                    text = "${currentImageIndex + 1} / ${galleryItems.size}",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.62f), SmallShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

internal fun galleryIndexForSwipe(
    currentIndex: Int,
    itemCount: Int,
    horizontalDragDistance: Float
): Int {
    if (itemCount < 2 || abs(horizontalDragDistance) < GALLERY_SWIPE_DISTANCE_PX) return currentIndex
    return if (horizontalDragDistance < 0f) {
        (currentIndex + 1) % itemCount
    } else {
        (currentIndex - 1 + itemCount) % itemCount
    }
}

private fun downloadActionTextFor(mediaType: String): String = when (mediaType) {
    "图集" -> "下载图片"
    "实况" -> "下载实况"
    else -> "下载视频"
}

private fun downloadActionIconFor(mediaType: String): ImageVector = when (mediaType) {
    "图集" -> Icons.Outlined.Image
    "实况" -> Icons.Outlined.VideoLibrary
    else -> Icons.Outlined.Download
}

private fun downloadSheetTitleFor(mediaType: String): String = when (mediaType) {
    "图集" -> "选择图片"
    "实况" -> "选择实况内容"
    else -> "选择清晰度"
}

@Composable
private fun VideoPreview(media: ParsedMedia, previewSessionKey: Int) {
    val context = LocalContext.current
    var player by remember(media.previewUrl, previewSessionKey) { mutableStateOf<MediaPlayer?>(null) }
    var videoSurface by remember(media.previewUrl, previewSessionKey) { mutableStateOf<Surface?>(null) }
    var durationMillis by remember(media.previewUrl, previewSessionKey) { mutableIntStateOf(0) }
    var positionMillis by remember(media.previewUrl, previewSessionKey) { mutableIntStateOf(0) }
    var isPlaying by remember(media.previewUrl, previewSessionKey) { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(media.previewUrl, previewSessionKey) { mutableStateOf(false) }
    var previewWidthPx by remember(media.previewUrl, previewSessionKey) { mutableIntStateOf(1) }
    var previewError by remember(media.previewUrl, previewSessionKey) { mutableStateOf<String?>(null) }

    DisposableEffect(player, isPlaying) {
        val handler = Handler(Looper.getMainLooper())
        val updateProgress = object : Runnable {
            override fun run() {
                player?.let { mediaPlayer ->
                    if (isMediaPlayerPlaying(mediaPlayer)) {
                        positionMillis = mediaPlayer.currentPosition.coerceAtLeast(0)
                        handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MILLIS)
                    }
                }
            }
        }
        if (isPlaying) handler.post(updateProgress)
        onDispose { handler.removeCallbacksAndMessages(null) }
    }
    DisposableEffect(media.previewUrl, previewSessionKey) {
        onDispose {
            player?.release()
            player = null
            videoSurface?.release()
            videoSurface = null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SmallShape,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .onSizeChanged { previewWidthPx = it.width.coerceAtLeast(1) }
                    .pointerInput(player, durationMillis) {
                        detectHorizontalDragGestures(
                            onDragStart = { positionMillis = player?.currentPosition ?: positionMillis },
                            onHorizontalDrag = { _, dragAmount ->
                                val updatedPosition = (positionMillis + dragAmount / previewWidthPx * durationMillis)
                                    .toInt()
                                    .coerceIn(0, durationMillis)
                                positionMillis = updatedPosition
                                player?.seekTo(updatedPosition)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                key(media.previewUrl, previewSessionKey) {
                    media.previewUrl?.let { previewUrl ->
                        AndroidView(
                            factory = {
                            TextureView(context).also { textureView ->
                                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        MediaPlayer().also { mediaPlayer ->
                                            player?.release()
                                            player = mediaPlayer
                                            videoSurface?.release()
                                            videoSurface = Surface(surfaceTexture)
                                            mediaPlayer.setSurface(videoSurface)
                                            mediaPlayer.setDataSource(context, previewUrl.toUri())
                                            mediaPlayer.setOnPreparedListener { preparedPlayer ->
                                                durationMillis = preparedPlayer.duration.coerceAtLeast(0)
                                                textureView.post {
                                                    fitTextureToVideo(
                                                        textureView = textureView,
                                                        videoWidth = preparedPlayer.videoWidth,
                                                        videoHeight = preparedPlayer.videoHeight
                                                    )
                                                }
                                                preparedPlayer.start()
                                                isPlaying = true
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    if (player === preparedPlayer && isMediaPlayerPlaying(preparedPlayer)) {
                                                        hasRenderedFirstFrame = true
                                                    }
                                                }, FIRST_FRAME_FALLBACK_MILLIS)
                                            }
                                            mediaPlayer.setOnInfoListener { _, what, _ ->
                                                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) hasRenderedFirstFrame = true
                                                true
                                            }
                                            mediaPlayer.setOnCompletionListener {
                                                isPlaying = false
                                                positionMillis = durationMillis
                                            }
                                            mediaPlayer.setOnErrorListener { _, _, _ ->
                                                isPlaying = false
                                                previewError = "视频预览加载失败，仍可使用下载功能"
                                                true
                                            }
                                            runCatching { mediaPlayer.prepareAsync() }.onFailure {
                                                previewError = "视频预览地址不可用"
                                            }
                                        }
                                    }

                                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

                                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                        player?.release()
                                        player = null
                                        videoSurface?.release()
                                        videoSurface = null
                                        isPlaying = false
                                        return true
                                    }

                                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                                }
                            }
                        },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (!hasRenderedFirstFrame) {
                    CoverImage(url = media.coverUrl, fallbackMediaUrl = media.previewUrl)
                }
                previewError?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            LinearProgressIndicator(
                progress = { if (durationMillis > 0) positionMillis.toFloat() / durationMillis else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        player?.let { mediaPlayer ->
                            if (isMediaPlayerPlaying(mediaPlayer)) {
                                mediaPlayer.pause()
                                isPlaying = false
                            } else {
                                mediaPlayer.start()
                                isPlaying = true
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (isPlaying) "暂停预览" else "播放预览",
                        tint = Color.White
                    )
                }
                Text(
                    text = formatPlaybackTime(positionMillis) + " / " + formatPlaybackTime(durationMillis),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun CoverImage(
    url: String?,
    modifier: Modifier = Modifier,
    fallbackMediaUrl: String? = null,
    retainPreviousBitmapWhileLoading: Boolean = false,
    contentDescription: String = "视频封面"
) {
    val cacheKey = url ?: fallbackMediaUrl
    var coverBitmap by remember(if (retainPreviousBitmapWhileLoading) null else cacheKey) {
        mutableStateOf(cacheKey?.let(CoverBitmapCache::get))
    }
    LaunchedEffect(cacheKey, url, fallbackMediaUrl) {
        val cachedBitmap = cacheKey?.let(CoverBitmapCache::get)
        if (cachedBitmap != null) {
            coverBitmap = cachedBitmap
        } else {
            if (!retainPreviousBitmapWhileLoading) coverBitmap = null
            val loadedBitmap = withContext(Dispatchers.IO) {
                url?.takeIf(::isHttpDownloadUrl)?.let(::loadCoverBitmap)
                    ?: fallbackMediaUrl?.takeIf(::isHttpDownloadUrl)?.let(::loadVideoPreviewFrame)
            }
            if (loadedBitmap != null && cacheKey != null) {
                CoverBitmapCache.put(cacheKey, loadedBitmap)
                coverBitmap = loadedBitmap
            }
        }
    }
    if (coverBitmap != null) {
        Image(
            bitmap = coverBitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)))
    }
}

private fun loadCoverBitmap(url: String): android.graphics.Bitmap? =
    coverReferersFor(url).firstNotNullOfOrNull { referer -> loadCoverBitmap(url, referer) }

private fun preloadCoverBitmap(url: String) {
    if (CoverBitmapCache.get(url) == null) {
        loadCoverBitmap(url)?.let { bitmap -> CoverBitmapCache.put(url, bitmap) }
    }
}

private fun loadCoverBitmap(url: String, referer: String?): android.graphics.Bitmap? = runCatching {
    val connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = COVER_CONNECT_TIMEOUT_MILLIS
        readTimeout = COVER_READ_TIMEOUT_MILLIS
        setRequestProperty("User-Agent", COVER_USER_AGENT)
        setRequestProperty("Accept", "image/webp,image/apng,image/jpeg,image/png,image/*,*/*;q=0.8")
        referer?.let { setRequestProperty("Referer", it) }
    }
    try {
        if (connection.responseCode !in 200..299) return@runCatching null
        connection.inputStream.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            })
        }
    } finally {
        connection.disconnect()
    }
}.getOrNull()

private fun loadVideoPreviewFrame(url: String): android.graphics.Bitmap? = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(url, mapOf("User-Agent" to COVER_USER_AGENT))
        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } finally {
        retriever.release()
    }
}.getOrNull()

internal fun coverReferersFor(url: String): List<String?> {
    val host = runCatching { java.net.URI(url).host.orEmpty().lowercase() }.getOrDefault("")
    val fallbackReferers = when {
        host.endsWith("hdslb.com") || host.endsWith("bilivideo.com") -> listOf("https://www.bilibili.com/")
        host.endsWith("yximgs.com") || host.endsWith("kwimgs.com") -> listOf("https://www.kuaishou.com/")
        host.endsWith("xhscdn.com") || host.endsWith("xiaohongshu.com") -> listOf("https://www.xiaohongshu.com/")
        host.endsWith("sinaimg.cn") || host.endsWith("weibo.com") -> listOf("https://weibo.com/")
        host.endsWith("qpic.cn") || host.endsWith("weixin.qq.com") -> listOf("https://channels.weixin.qq.com/")
        host.endsWith("izuiyou.com") || host.endsWith("xiaochuankeji.cn") -> listOf("https://share.izuiyou.com/")
        host.endsWith("pstatp.com") || host.endsWith("byteimg.com") -> listOf(
            "https://h5.pipix.com/",
            "https://www.toutiao.com/"
        )
        else -> emptyList()
    }
    return listOf(null) + fallbackReferers
}

@Composable
private fun DescriptionPanel(description: String) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val copyDescription = {
        context.getSystemService(android.content.ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("视频文案", description))
        Toast.makeText(context, "文案已复制", Toast.LENGTH_SHORT).show()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = PageShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DescriptionPanelHeight)
                .padding(12.dp)
        ) {
            Text(
                "点击文案可复制",
                modifier = Modifier.align(Alignment.TopStart),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp, end = 8.dp)
                    .verticalScroll(scrollState)
                    .clickable(onClick = copyDescription),
                style = MaterialTheme.typography.bodyMedium
            )
            ScrollIndicator(scrollState, Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun MusicPreviewBar(music: ParsedDownload, onDownload: (ParsedDownload) -> Unit) {
    val context = LocalContext.current
    var player by remember(music.url) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember(music.url) { mutableStateOf(false) }
    var isPreparing by remember(music.url) { mutableStateOf(false) }
    var durationMillis by remember(music.url) { mutableIntStateOf(0) }
    var positionMillis by remember(music.url) { mutableIntStateOf(0) }

    DisposableEffect(player, isPlaying) {
        val handler = Handler(Looper.getMainLooper())
        val updateProgress = object : Runnable {
            override fun run() {
                player?.takeIf(::isMediaPlayerPlaying)?.let { mediaPlayer ->
                    positionMillis = mediaPlayer.currentPosition.coerceAtLeast(0)
                    handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MILLIS)
                }
            }
        }
        if (isPlaying) handler.post(updateProgress)
        onDispose { handler.removeCallbacksAndMessages(null) }
    }

    DisposableEffect(music.url) {
        isPreparing = true
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(context, music.url.toUri())
            setOnPreparedListener { preparedPlayer ->
                durationMillis = preparedPlayer.duration.coerceAtLeast(0)
                isPreparing = false
            }
            setOnErrorListener { _, _, _ ->
                isPreparing = false
                isPlaying = false
                true
            }
            setOnCompletionListener {
                isPlaying = false
                positionMillis = durationMillis
            }
        }
        player = mediaPlayer
        runCatching { mediaPlayer.prepareAsync() }.onFailure {
            isPreparing = false
            mediaPlayer.release()
            player = null
        }
        onDispose {
            mediaPlayer.release()
            if (player === mediaPlayer) player = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmallShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                val currentPlayer = player
                if (!isPreparing && currentPlayer?.let(::isMediaPlayerPlaying) == true) {
                    currentPlayer.pause()
                    isPlaying = false
                } else if (!isPreparing && currentPlayer != null) {
                    currentPlayer.start()
                    isPlaying = true
                }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (isPreparing) "正在加载背景音乐" else if (isPlaying) "暂停背景音乐" else "播放背景音乐",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "背景音乐",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    formatPlaybackTime(positionMillis) + " / " + formatPlaybackTime(durationMillis),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
                IconButton(onClick = { onDownload(music) }) {
                    Icon(Icons.Outlined.Download, contentDescription = "下载背景音乐", tint = MaterialTheme.colorScheme.primary)
                }
            }
            LinearProgressIndicator(
                progress = { if (durationMillis > 0) positionMillis.toFloat() / durationMillis else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DownloadQualitySheet(
    downloads: List<ParsedDownload>,
    title: String,
    onDismiss: () -> Unit,
    onDownload: (ParsedDownload) -> Unit
) {
    val scrollState = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        sheetGesturesEnabled = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(DownloadQualitySheetHeight)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp).verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortDownloadQualities(downloads).forEach { download ->
                        Button(
                            onClick = { onDownload(download) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = SmallShape
                        ) {
                            Text(download.label)
                        }
                    }
                }
                ScrollIndicator(scrollState, Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}

@Composable
private fun DownloadProgressDialog(
    task: DownloadTaskUiState,
    onCancel: () -> Unit,
    onClose: () -> Unit
) {
    val isActive = task.status == DownloadTaskStatus.DOWNLOADING || task.status == DownloadTaskStatus.CANCELLING
    val isIndeterminate = task.totalBytes <= 0
    val progress = if (isIndeterminate) 0f else {
        task.downloadedBytes.toFloat() / task.totalBytes.toFloat()
    }.coerceIn(0f, 1f)

    Dialog(onDismissRequest = { if (!isActive) onClose() }) {
        Card(
            modifier = Modifier.fillMaxWidth().height(288.dp),
            shape = SmallShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("下载任务", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    task.qualityLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isIndeterminate) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
                Text(
                    if (isIndeterminate) {
                        "${formatDownloadBytes(task.downloadedBytes)}  ${formatDownloadSpeed(task.bytesPerSecond)}"
                    } else {
                        "${formatDownloadBytes(task.downloadedBytes)} / ${formatDownloadBytes(task.totalBytes)}  ${formatDownloadSpeed(task.bytesPerSecond)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = task.status.toDisplayText(),
                    style = MaterialTheme.typography.labelMedium,
                    color = when (task.status) {
                        DownloadTaskStatus.FAILED -> MaterialTheme.colorScheme.error
                        DownloadTaskStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(Modifier.weight(1f))
                if (isActive) {
                    Button(
                        onClick = onCancel,
                        enabled = task.status == DownloadTaskStatus.DOWNLOADING,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmallShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (task.status == DownloadTaskStatus.CANCELLING) "正在取消" else "取消下载")
                    }
                } else {
                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmallShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

private fun DownloadTaskStatus.toDisplayText(): String = when (this) {
    DownloadTaskStatus.DOWNLOADING -> "正在下载"
    DownloadTaskStatus.CANCELLING -> "正在取消"
    DownloadTaskStatus.COMPLETED -> "下载完成"
    DownloadTaskStatus.FAILED -> "下载失败"
    DownloadTaskStatus.CANCELLED -> "已取消"
}

private fun formatDownloadBytes(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_024 * 1_024 -> "%.1f KB".format(bytes / 1_024f)
    bytes < 1_024L * 1_024L * 1_024L -> "%.1f MB".format(bytes / (1_024f * 1_024f))
    else -> "%.2f GB".format(bytes / (1_024f * 1_024f * 1_024f))
}

private fun formatDownloadSpeed(bytesPerSecond: Long): String =
    "${formatDownloadBytes(bytesPerSecond)}/s"

@Composable
private fun ScrollIndicator(scrollState: androidx.compose.foundation.ScrollState, modifier: Modifier = Modifier) {
    val maxScrollValue = scrollState.maxValue
    if (maxScrollValue > 0) {
        val thumbFraction = (1f / (maxScrollValue / 82f + 1f)).coerceIn(0.16f, 1f)
        BoxWithConstraints(
            modifier = modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(thumbFraction)
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(
                            0,
                            calculateScrollIndicatorOffset(
                                containerHeight = maxHeight.roundToPx(),
                                thumbFraction = thumbFraction,
                                scrollValue = scrollState.value,
                                maxScrollValue = maxScrollValue
                            )
                        )
                    }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            )
        }
    }
}

private fun formatPlaybackTime(milliseconds: Int): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun isMediaPlayerPlaying(mediaPlayer: MediaPlayer): Boolean =
    runCatching { mediaPlayer.isPlaying }.getOrDefault(false)

private fun fitTextureToVideo(textureView: TextureView, videoWidth: Int, videoHeight: Int) {
    val scale = calculateVideoScale(textureView.width, textureView.height, videoWidth, videoHeight)
    textureView.scaleX = scale.scaleX
    textureView.scaleY = scale.scaleY
}

private const val PROGRESS_UPDATE_INTERVAL_MILLIS = 250L
private const val FIRST_FRAME_FALLBACK_MILLIS = 750L
private const val COVER_CONNECT_TIMEOUT_MILLIS = 10_000
private const val COVER_READ_TIMEOUT_MILLIS = 15_000
private const val COVER_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
private const val ZUIYOU_PLATFORM_NAME = "最右"

internal suspend fun downloadWithMediaStore(
    context: Context,
    title: String,
    fileName: String,
    relativePath: String,
    sourceUrl: String,
    cancellationController: DownloadCancellationController,
    onProgress: (Long, Long) -> Unit
) {
    val resolver = context.contentResolver
    var destinationUri: Uri? = null
    try {
        destinationUri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, guessMimeType(fileName))
                put(MediaStore.Downloads.RELATIVE_PATH, "$relativePath/")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        ) ?: error("Unable to create a download destination")

        val progressUpdater = DownloadProgressUpdater(onProgress)
        val rangeDownloadSize = probeRangeDownloadSize(sourceUrl, cancellationController)
        if (rangeDownloadSize != null) {
            downloadWithParallelRanges(
                sourceUrl = sourceUrl,
                totalBytes = rangeDownloadSize,
                resolver = resolver,
                destinationUri = destinationUri,
                relativePath = relativePath,
                cancellationController = cancellationController,
                onProgress = progressUpdater::update
            )
        } else {
            downloadSequentially(
                sourceUrl = sourceUrl,
                resolver = resolver,
                destinationUri = destinationUri,
                cancellationController = cancellationController,
                onProgress = progressUpdater::update
            )
        }
        progressUpdater.complete()

        resolver.update(
            destinationUri,
            ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
            null,
            null
        )
    } catch (error: kotlinx.coroutines.CancellationException) {
        destinationUri?.let { resolver.delete(it, null, null) }
        throw error
    } catch (error: Exception) {
        destinationUri?.let { resolver.delete(it, null, null) }
        if (cancellationController.isCancelled) {
            throw kotlinx.coroutines.CancellationException("Download cancelled").apply { initCause(error) }
        }
        throw error
    }
}

private suspend fun downloadWithParallelRanges(
    sourceUrl: String,
    totalBytes: Long,
    resolver: android.content.ContentResolver,
    destinationUri: Uri,
    relativePath: String,
    cancellationController: DownloadCancellationController,
    onProgress: (Long, Long) -> Unit
) {
    val temporaryUri = resolver.insert(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$DOWNLOAD_TEMP_FILE_PREFIX${UUID.randomUUID()}$DOWNLOAD_TEMP_FILE_SUFFIX")
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, "$relativePath/")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
    ) ?: error("Unable to create a temporary download destination")
    try {
        try {
            val downloadedBytes = AtomicLong(0)
            val ranges = createDownloadByteRanges(
                totalBytes = totalBytes,
                maximumWorkers = DOWNLOAD_MAXIMUM_PARALLEL_WORKERS,
                minimumSegmentBytes = DOWNLOAD_MINIMUM_SEGMENT_BYTES
            )

            requireNotNull(resolver.openFileDescriptor(temporaryUri, "rw")).use { descriptor ->
                FileOutputStream(descriptor.fileDescriptor).channel.use { outputChannel ->
                    coroutineScope {
                        ranges.map { range ->
                            async(Dispatchers.IO) {
                                downloadByteRange(
                                    sourceUrl = sourceUrl,
                                    range = range,
                                    outputChannel = outputChannel,
                                    cancellationController = cancellationController
                                ) { bytesRead ->
                                    onProgress(downloadedBytes.addAndGet(bytesRead.toLong()), totalBytes)
                                }
                            }
                        }.awaitAll()
                    }
                }
            }

            requireNotNull(resolver.openInputStream(temporaryUri)).use { input ->
                requireNotNull(resolver.openOutputStream(destinationUri)).use { output -> input.copyTo(output, DOWNLOAD_BUFFER_SIZE) }
            }
        } catch (_: RangeDownloadUnsupportedException) {
            downloadSequentially(sourceUrl, resolver, destinationUri, cancellationController, onProgress)
        }
    } finally {
        resolver.delete(temporaryUri, null, null)
    }
}

private suspend fun downloadSequentially(
    sourceUrl: String,
    resolver: android.content.ContentResolver,
    destinationUri: Uri,
    cancellationController: DownloadCancellationController,
    onProgress: (Long, Long) -> Unit
) {
    val connection = openDownloadConnection(sourceUrl, cancellationController)
    try {
        connection.inputStream.use { input ->
            requireNotNull(resolver.openOutputStream(destinationUri)).use { output ->
                val totalBytes = connection.contentLengthLong
                val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                var downloadedBytes = 0L
                while (true) {
                    cancellationController.throwIfCancelled()
                    val bytesRead = input.read(buffer)
                    if (bytesRead < 0) break
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    onProgress(downloadedBytes, totalBytes)
                }
            }
        }
    } finally {
        cancellationController.unregister(connection)
        connection.disconnect()
    }
}

private fun probeRangeDownloadSize(sourceUrl: String, cancellationController: DownloadCancellationController): Long? = try {
    val connection = openDownloadConnection(sourceUrl, cancellationController).apply { setRequestProperty("Range", "bytes=0-0") }
    try {
        if (connection.responseCode != HttpURLConnection.HTTP_PARTIAL) {
            null
        } else {
            connection.inputStream.use { it.read() }
            connection.getHeaderField("Content-Range")
                ?.let(CONTENT_RANGE_TOTAL_BYTES::find)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
        }
    } finally {
        cancellationController.unregister(connection)
        connection.disconnect()
    }
} catch (error: kotlinx.coroutines.CancellationException) {
    throw error
} catch (_: Exception) {
    null
}

private suspend fun downloadByteRange(
    sourceUrl: String,
    range: DownloadByteRange,
    outputChannel: FileChannel,
    cancellationController: DownloadCancellationController,
    onBytesRead: (Int) -> Unit
) {
    val connection = openDownloadConnection(sourceUrl, cancellationController).apply {
        setRequestProperty("Range", "bytes=${range.startInclusive}-${range.endInclusive}")
    }
    try {
        if (connection.responseCode != HttpURLConnection.HTTP_PARTIAL) {
            throw RangeDownloadUnsupportedException()
        }
        var receivedBytes = 0L
        connection.inputStream.use { input ->
            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
            while (true) {
                cancellationController.throwIfCancelled()
                val bytesRead = input.read(buffer)
                if (bytesRead < 0) break
                writeByteRange(outputChannel, buffer, bytesRead, range.startInclusive + receivedBytes)
                receivedBytes += bytesRead
                onBytesRead(bytesRead)
            }
        }
        check(receivedBytes == range.endInclusive - range.startInclusive + 1) {
            "Incomplete byte range response"
        }
    } finally {
        cancellationController.unregister(connection)
        connection.disconnect()
    }
}

private fun writeByteRange(
    outputChannel: FileChannel,
    buffer: ByteArray,
    bytesRead: Int,
    startPosition: Long
) {
    val byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead)
    var writePosition = startPosition
    while (byteBuffer.hasRemaining()) {
        val writtenBytes = outputChannel.write(byteBuffer, writePosition)
        check(writtenBytes > 0) { "Unable to write downloaded byte range" }
        writePosition += writtenBytes
    }
}

private class DownloadProgressUpdater(
    private val onProgress: (Long, Long) -> Unit
) {
    private val lastProgressUpdate = AtomicLong(0)
    private var latestDownloadedBytes = 0L
    private var latestTotalBytes = -1L

    @Synchronized
    fun update(downloadedBytes: Long, totalBytes: Long) {
        latestDownloadedBytes = maxOf(latestDownloadedBytes, downloadedBytes)
        latestTotalBytes = totalBytes
        val now = System.currentTimeMillis()
        val previousUpdate = lastProgressUpdate.get()
        if (now - previousUpdate >= DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MILLIS &&
            lastProgressUpdate.compareAndSet(previousUpdate, now)
        ) {
            onProgress(downloadedBytes, totalBytes)
        }
    }

    @Synchronized
    fun complete() {
        onProgress(latestDownloadedBytes, latestTotalBytes)
    }
}

private fun openDownloadConnection(
    sourceUrl: String,
    cancellationController: DownloadCancellationController
): HttpURLConnection =
    (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = DOWNLOAD_CONNECT_TIMEOUT_MILLIS
        readTimeout = DOWNLOAD_READ_TIMEOUT_MILLIS
        setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
        setRequestProperty("Accept", "*/*")
        setRequestProperty("Accept-Encoding", "identity")
    }.also(cancellationController::register)

private class RangeDownloadUnsupportedException : IllegalStateException("The server did not return a partial response")

internal class DownloadCancellationController {
    private val cancelled = AtomicBoolean(false)
    private val activeConnections = ConcurrentHashMap.newKeySet<HttpURLConnection>()

    fun register(connection: HttpURLConnection) {
        activeConnections += connection
        if (cancelled.get()) connection.disconnect()
    }

    fun unregister(connection: HttpURLConnection) {
        activeConnections -= connection
    }

    fun cancel() {
        if (cancelled.compareAndSet(false, true)) {
            activeConnections.forEach(HttpURLConnection::disconnect)
        }
    }

    val isCancelled: Boolean
        get() = cancelled.get()

    fun throwIfCancelled() {
        if (cancelled.get()) throw kotlinx.coroutines.CancellationException("Download cancelled")
    }
}

private fun cleanupStaleDownloadTemporaryFiles(context: Context) {
    listOfNotNull(context.externalCacheDir, context.cacheDir)
        .distinct()
        .forEach { cacheDirectory ->
            cacheDirectory.listFiles()
                ?.asSequence()
                ?.filter { file ->
                    file.isFile &&
                        file.name.startsWith(DOWNLOAD_TEMP_FILE_PREFIX) &&
                        file.name.endsWith(DOWNLOAD_TEMP_FILE_SUFFIX)
                }
                ?.forEach { it.delete() }
        }
    val resolver = context.contentResolver
    resolver.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.MediaColumns._ID),
        "${MediaStore.MediaColumns.IS_PENDING}=1 AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
        arrayOf("$DOWNLOAD_TEMP_FILE_PREFIX%$DOWNLOAD_TEMP_FILE_SUFFIX"),
        null
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        while (cursor.moveToNext()) {
            resolver.delete(
                ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn)),
                null,
                null
            )
        }
    }
}

private fun guessMimeType(fileName: String): String =
    URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"

private const val DOWNLOAD_CONNECT_TIMEOUT_MILLIS = 15_000
private const val DOWNLOAD_READ_TIMEOUT_MILLIS = 60_000
private const val DOWNLOAD_BUFFER_SIZE = 32 * 1024
private const val DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MILLIS = 150L
private const val DOWNLOAD_MAXIMUM_PARALLEL_WORKERS = 20
private const val DOWNLOAD_MINIMUM_SEGMENT_BYTES = 1_048_576L
private const val DOWNLOAD_TEMP_FILE_PREFIX = "jiqu-download-"
private const val DOWNLOAD_TEMP_FILE_SUFFIX = ".part"
private const val DOWNLOAD_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
private val CONTENT_RANGE_TOTAL_BYTES = Regex("bytes\\s+\\d+-\\d+/(\\d+)", RegexOption.IGNORE_CASE)

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openExternalUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}

private fun Context.copyLanzouExtractCode() {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
        ClipData.newPlainText("蓝奏云提取码", LANZOU_EXTRACT_CODE)
    )
    Toast.makeText(this, "蓝奏云提取码已复制：$LANZOU_EXTRACT_CODE", Toast.LENGTH_SHORT).show()
}

private const val OPEN_SOURCE_URL = "https://github.com/dhvbjvvb/jiqu"
private const val LANZOU_UPDATE_URL = "https://wwbjl.lanzout.com/b01d74gqdc"
private const val LANZOU_EXTRACT_CODE = "3nwk"

@Composable
private fun HistoryPage(
    paddingValues: PaddingValues,
    history: List<ParseHistoryEntry>,
    onReparse: (ParseHistoryEntry) -> Unit,
    onDelete: (Set<String>) -> Unit
) {
    var isSelecting by rememberSaveable { mutableStateOf(false) }
    var selectedUrls by remember { mutableStateOf(emptySet<String>()) }
    val scrollState = rememberScrollState()
    val allHistoryUrlsSelected = areAllHistoryUrlsSelected(history, selectedUrls)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("历史记录", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(
                enabled = history.isNotEmpty(),
                onClick = {
                    isSelecting = true
                    selectedUrls = if (allHistoryUrlsSelected) {
                        emptySet()
                    } else {
                        historySourceUrls(history)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.SelectAll,
                    contentDescription = if (allHistoryUrlsSelected) "取消全选历史记录" else "全选历史记录",
                    tint = if (history.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
            IconButton(onClick = {
                isSelecting = !isSelecting
                if (!isSelecting) selectedUrls = emptySet()
            }) {
                Icon(
                    imageVector = if (isSelecting) Icons.Outlined.Done else Icons.Outlined.CheckCircle,
                    contentDescription = if (isSelecting) "完成多选" else "多选记录",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                enabled = selectedUrls.isNotEmpty(),
                onClick = {
                    onDelete(selectedUrls)
                    selectedUrls = emptySet()
                    isSelecting = false
                }
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除选中记录",
                    tint = if (selectedUrls.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        }
        if (history.isEmpty()) {
            Text(
                "暂无解析记录",
                modifier = Modifier.padding(top = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        history.forEach { entry ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isSelecting) {
                            selectedUrls = if (entry.sourceUrl in selectedUrls) {
                                selectedUrls - entry.sourceUrl
                            } else {
                                selectedUrls + entry.sourceUrl
                            }
                        } else {
                            onReparse(entry)
                        }
                    },
                shape = PageShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                HistoryEntryRow(
                    entry = entry,
                    isSelecting = isSelecting,
                    isSelected = entry.sourceUrl in selectedUrls,
                    onSelectionChange = { selected ->
                        selectedUrls = if (selected) selectedUrls + entry.sourceUrl else selectedUrls - entry.sourceUrl
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryEntryRow(
    entry: ParseHistoryEntry,
    isSelecting: Boolean,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(SmallShape)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(
                url = entry.coverUrl.takeUnless { entry.platformName == ZUIYOU_PLATFORM_NAME },
                fallbackMediaUrl = entry.previewUrl
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.platformName,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(entry.parsedAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(entry.mediaType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isSelecting) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )
        }
    }
}

@Composable
private fun SettingsPage(
    paddingValues: PaddingValues,
    parserViewModel: ParserViewModel,
    updateViewModel: UpdateViewModel,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var isParserPreferencesPageVisible by rememberSaveable { mutableStateOf(false) }
    var isThemeSettingsPageVisible by rememberSaveable { mutableStateOf(false) }
    var isStoragePageVisible by rememberSaveable { mutableStateOf(false) }
    var isFeedbackPageVisible by rememberSaveable { mutableStateOf(false) }
    val updateCheckMessage = updateViewModel.checkMessage
    LaunchedEffect(updateCheckMessage) {
        updateCheckMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            updateViewModel.consumeCheckMessage()
        }
    }
    BackHandler {
        when {
            isParserPreferencesPageVisible -> isParserPreferencesPageVisible = false
            isThemeSettingsPageVisible -> isThemeSettingsPageVisible = false
            isStoragePageVisible -> isStoragePageVisible = false
            isFeedbackPageVisible -> isFeedbackPageVisible = false
            else -> onNavigateUp()
        }
    }
    if (isParserPreferencesPageVisible) {
        ParserPreferencesSettingsPage(
            paddingValues = paddingValues,
            parserViewModel = parserViewModel,
            onBack = { isParserPreferencesPageVisible = false }
        )
        return
    }
    if (isThemeSettingsPageVisible) {
        ThemeSettingsPage(
            paddingValues = paddingValues,
            selectedThemeMode = parserViewModel.themeMode,
            onThemeModeSelected = parserViewModel::updateThemeMode,
            onBack = { isThemeSettingsPageVisible = false }
        )
        return
    }
    if (isStoragePageVisible) {
        StorageSettingsPage(paddingValues = paddingValues, onBack = { isStoragePageVisible = false })
        return
    }
    if (isFeedbackPageVisible) {
        FeedbackSettingsPage(paddingValues = paddingValues, onBack = { isFeedbackPageVisible = false })
        return
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsGroup("解析偏好") {
            SettingsValueRow("链接识别", Icons.Outlined.Link, "", onClick = { isParserPreferencesPageVisible = true })
        }
        SettingsGroup("主题设置") {
            SettingsValueRow("显示模式", Icons.Outlined.Palette, parserViewModel.themeMode.displayName, onClick = { isThemeSettingsPageVisible = true })
        }
        SettingsGroup("下载与存储") {
            SettingsValueRow("保存位置", Icons.Outlined.Folder, "", onClick = { isStoragePageVisible = true })
        }
        SupportedPlatformsCard()
        SettingsGroup("关于") {
            SettingsValueRow("当前版本", Icons.Outlined.Info, BuildConfig.VERSION_NAME, showChevron = false)
            SettingsValueRow(
                label = "检查更新",
                icon = Icons.Outlined.SystemUpdate,
                value = if (updateViewModel.isChecking) "正在检查" else "",
                showChevron = !updateViewModel.isChecking,
                onClick = { updateViewModel.checkForUpdate(force = true) }
            )
            SettingsValueRow(
                label = "开源地址",
                icon = Icons.Outlined.Language,
                value = "dhvbjvvb/jiqu",
                onClick = { context.openExternalUrl(OPEN_SOURCE_URL) }
            )
            SettingsValueRow("意见反馈", Icons.Outlined.Forum, "", onClick = { isFeedbackPageVisible = true })
        }
    }
}

@Composable
private fun ParserPreferencesSettingsPage(
    paddingValues: PaddingValues,
    parserViewModel: ParserViewModel,
    onBack: () -> Unit
) {
    SettingsSubpageLayout(title = "解析偏好", paddingValues = paddingValues, onBack = onBack) {
        SettingsGroup("链接识别") {
            SettingsSwitchRow("自动识别链接", Icons.Outlined.Link, parserViewModel.autoDetect, parserViewModel::updateAutoDetection)
            SettingsSwitchRow("自动粘贴链接", Icons.Outlined.ContentPaste, parserViewModel.autoPasteLinks, parserViewModel::updateAutoPasteLinks)
        }
    }
}

@Composable
private fun ThemeSettingsPage(
    paddingValues: PaddingValues,
    selectedThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onBack: () -> Unit
) {
    SettingsSubpageLayout(title = "主题设置", paddingValues = paddingValues, onBack = onBack) {
        SettingsGroup("显示模式") {
            ThemeMode.entries.forEach { mode ->
                SettingsSelectionRow(
                    label = mode.displayName,
                    selected = selectedThemeMode == mode,
                    onClick = { onThemeModeSelected(mode) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSubpageLayout(
    title: String,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回设置",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun StorageSettingsPage(paddingValues: PaddingValues, onBack: () -> Unit) {
    SettingsSubpageLayout(title = "下载与存储", paddingValues = paddingValues, onBack = onBack) {
        SettingsGroup("保存位置") {
            SettingsValueRow("图片保存位置", Icons.Outlined.Image, "Download\\Jiqu\\Picture", showChevron = false)
            SettingsValueRow("音频保存位置", Icons.Outlined.PlayArrow, "Download\\Jiqu\\Music", showChevron = false)
            SettingsValueRow("视频，动图保存位置", Icons.Outlined.VideoLibrary, "Download\\Jiqu\\video", showChevron = false)
        }
    }
}

@Composable
private fun FeedbackSettingsPage(paddingValues: PaddingValues, onBack: () -> Unit) {
    SettingsSubpageLayout(title = "意见反馈", paddingValues = paddingValues, onBack = onBack) {
        SettingsGroup("联系我们") {
            SettingsValueRow(
                label = "QQ邮箱",
                icon = Icons.Outlined.Forum,
                value = "15150689599@qq.com",
                valueMaxWidth = 176.dp,
                showChevron = false
            )
            SettingsValueRow("制作人", Icons.Outlined.Info, "春日大阪", showChevron = false)
        }
    }
}

@Composable
private fun SupportedPlatformsCard() {
    val platforms = remember { PlatformDetector.supportedPlatformList() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = PageShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "可解析平台",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            platforms.chunked(5).forEach { platformRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    platformRow.forEach { platform ->
                        PlatformDisplayItem(platform = platform, modifier = Modifier.weight(1f))
                    }
                    repeat(5 - platformRow.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun PlatformDisplayItem(platform: SupportedPlatform, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(platformLogoResource(platform.id)),
            contentDescription = "${platform.displayName}图标",
            modifier = Modifier
                .size(38.dp)
                .clip(SmallShape),
            contentScale = ContentScale.Fit
        )
        Text(
            text = platform.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            letterSpacing = 0.sp
        )
    }
}

private fun platformLogoResource(platformId: String): Int = when (platformId) {
    "bilibili" -> R.drawable.platform_bilibili
    "douyin" -> R.drawable.platform_douyin
    "kuaishou" -> R.drawable.platform_kuaishou
    "pipixia" -> R.drawable.platform_pipixia
    "pipigx" -> R.drawable.platform_pipigx
    "toutiao" -> R.drawable.platform_toutiao
    "weibo" -> R.drawable.platform_weibo
    "wxsph" -> R.drawable.platform_wxsph
    "xiaohongshu" -> R.drawable.platform_xiaohongshu
    else -> R.drawable.platform_zuiyou
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = PageShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@Composable
private fun SettingsValueRow(
    label: String,
    icon: ImageVector,
    value: String,
    valueMaxWidth: Dp = 136.dp,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .let { modifier -> if (onClick != null) modifier.clickable(onClick = onClick) else modifier }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (value.isNotEmpty()) {
            Text(
                value,
                modifier = Modifier.widthIn(max = valueMaxWidth),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (showChevron) Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSelectionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (selected) {
            Icon(
                Icons.Outlined.Done,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JinanMediaAppPreview() {
    即取Theme { JinanMediaApp(viewModel(), viewModel()) }
}
