package com.omaroid.tasky.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme

@Composable
fun PhotoDetailScreen(
    photoUrl: String,
    isOnline: Boolean,
    isUserCreator: Boolean,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape).padding(4.dp),
            )
        }

        Box(modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
            if (isOnline && isUserCreator) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape).size(48.dp),
                ) {
                    Icon(Icons.Default.Delete, "Delete photo", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            } else if (!isOnline) {
                Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(20.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.CloudOff, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Unavailable offline", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }
            }
        }
    }
}

@Preview(name = "Photo Detail – Online", device = "spec:width=360dp,height=800dp,dpi=420")
@Composable
private fun PhotoDetailOnlinePreview() {
    TaskyTheme { PhotoDetailScreen("", isOnline = true, isUserCreator = true, {}, {}) }
}

@Preview(name = "Photo Detail – Offline", device = "spec:width=360dp,height=800dp,dpi=420")
@Composable
private fun PhotoDetailOfflinePreview() {
    TaskyTheme(darkTheme = true) { PhotoDetailScreen("", isOnline = false, isUserCreator = true, {}, {}) }
}
