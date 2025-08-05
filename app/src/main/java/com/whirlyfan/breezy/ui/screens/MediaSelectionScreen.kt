package com.whirlyfan.breezy.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.whirlyfan.breezy.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.plusAssign
import kotlin.compareTo
import kotlin.div
import kotlin.text.compareTo
import kotlin.text.toFloat
import kotlin.times
import kotlin.unaryMinus

data class MediaAlbum(val name: String, val count: Int)
data class MediaItem(val id: Long, val uri: Uri, val albumName: String)

@Composable
fun MediaSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var albums by remember { mutableStateOf<List<MediaAlbum>>(emptyList()) }
    var selectedAlbum by remember { mutableStateOf<MediaAlbum?>(null) }
    var selectedMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Image position state
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Dropdown state
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Request permissions based on Android version
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch {
                loadMediaData(context) { items, albumList ->
                    mediaItems = items
                    albums = albumList
                    selectedAlbum = albumList.firstOrNull()
                    selectedMediaItem = items.firstOrNull()
                    isLoading = false
                }
            }
        } else {
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permission)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = "New post",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )

                TextButton(
                    onClick = {
                        selectedMediaItem?.let { media ->
                            val encodedUri =
                                java.net.URLEncoder.encode(media.uri.toString(), "UTF-8")
                            navController.navigate("createPost/$encodedUri")
                        }
                    },
                    enabled = selectedMediaItem != null
                ) {
                    Text("Next")
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color.Black)
                        .clipToBounds()
                ) {
                    selectedMediaItem?.let { media ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(media.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Selected image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Album selector and camera button row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Album dropdown
                    Box {
                        TextButton(onClick = { isDropdownExpanded = true }) {
                            Text(selectedAlbum?.name ?: "All photos")
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select album"
                            )
                        }

                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            albums.forEach { album ->
                                DropdownMenuItem(
                                    text = { Text("${album.name} (${album.count})") },
                                    onClick = {
                                        selectedAlbum = album
                                        isDropdownExpanded = false

                                        // Filter by album
                                        val filteredItems = if (album.name == "All Photos") {
                                            mediaItems
                                        } else {
                                            mediaItems.filter { it.albumName == album.name }
                                        }
                                        selectedMediaItem = filteredItems.firstOrNull()
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                )
                            }
                        }
                    }

                    // Camera button
                    IconButton(onClick = { navController.navigate("camera") }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.outline_camera_alt_24),
                            contentDescription = "Take photo"
                        )
                    }
                }

                // Photos grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val filteredItems =
                        if (selectedAlbum?.name == "All Photos" || selectedAlbum == null) {
                            mediaItems
                        } else {
                            mediaItems.filter { it.albumName == selectedAlbum?.name }
                        }

                    items(filteredItems) { item ->
                        val isSelected = selectedMediaItem?.id == item.id

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clickable {
                                    selectedMediaItem = item
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Photo thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(4.dp))
                            )

                            if (isSelected) {
                                // Gray overlay for selected image
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Load media from device storage function (unchanged)
private suspend fun loadMediaData(
    context: Context,
    onResult: (List<MediaItem>, List<MediaAlbum>) -> Unit
) = withContext(Dispatchers.IO) {
    // Same implementation as before
    val mediaItems = mutableListOf<MediaItem>()
    val albumsMap = mutableMapOf<String, Int>()

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )

    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val albumNameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val albumName = cursor.getString(albumNameColumn) ?: "Unknown Album"

            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )

            mediaItems.add(MediaItem(id, contentUri, albumName))
            albumsMap[albumName] = albumsMap.getOrDefault(albumName, 0) + 1
        }
    }

    val albums = albumsMap.map { MediaAlbum(it.key, it.value) }
        .sortedByDescending { it.count }

    // Add "All Photos" option
    val allPhotosAlbum = MediaAlbum("All Photos", mediaItems.size)
    val finalAlbums = listOf(allPhotosAlbum) + albums

    withContext(Dispatchers.Main) {
        onResult(mediaItems, finalAlbums)
    }
}