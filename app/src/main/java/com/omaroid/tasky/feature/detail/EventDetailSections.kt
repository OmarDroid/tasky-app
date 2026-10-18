package com.omaroid.tasky.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaroid.tasky.core.presentation.adaptive.DeviceConfiguration
import com.omaroid.tasky.core.presentation.adaptive.currentDeviceConfiguration
import com.omaroid.tasky.core.presentation.designsystem.theme.extendedColors
import com.omaroid.tasky.feature.detail.model.AttendeeFilter
import com.omaroid.tasky.feature.detail.model.AttendeeUi
import com.omaroid.tasky.feature.detail.model.PhotoUi
import kotlinx.coroutines.launch

@Composable
fun EventVisitorsSection(
    // Groups are pre-computed in the ViewModel — never partition the attendee list in a composable.
    goingAttendees: List<AttendeeUi>,
    notGoingAttendees: List<AttendeeUi>,
    currentUserId: String,
    isUserCreator: Boolean,
    isEditMode: Boolean,
    isOnline: Boolean,
    attendeeFilter: AttendeeFilter,
    onFilterChanged: (AttendeeFilter) -> Unit,
    onAddAttendee: () -> Unit,
    onRemoveAttendee: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showGoing = attendeeFilter != AttendeeFilter.NOT_GOING
    val showNotGoing = attendeeFilter != AttendeeFilter.GOING

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Visitors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!isOnline) {
                    // Design shows a neutral icon matching the header text tone — default tint
                    // (LocalContentColor), not error red.
                    Icon(Icons.Default.CloudOff, "Offline")
                }
            }
            if (isEditMode && isUserCreator && isOnline) {
                IconButton(onClick = onAddAttendee) {
                    Icon(Icons.Default.PersonAdd, "Add visitor", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        if (!isOnline) {
            Text(
                "Attendee management requires an internet connection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // attendeeFilter comes from ViewModel state — never local remember, so it survives recomposition.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AttendeeFilter.entries.forEach { f ->
                FilterChip(selected = attendeeFilter == f, onClick = { onFilterChanged(f) }, label = { Text(f.label) })
            }
        }
        if (showGoing && goingAttendees.isNotEmpty()) {
            Text("Going", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            goingAttendees.forEach { attendee ->
                AttendeeRow(attendee, currentUserId, isUserCreator, isEditMode, isOnline, onRemoveAttendee)
            }
        }
        if (showNotGoing && notGoingAttendees.isNotEmpty()) {
            Text("Not going", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            notGoingAttendees.forEach { attendee ->
                AttendeeRow(attendee, currentUserId, isUserCreator, isEditMode, isOnline, onRemoveAttendee)
            }
        }
    }
}

@Composable
private fun AttendeeRow(
    attendee: AttendeeUi,
    currentUserId: String,
    isUserCreator: Boolean,
    isEditMode: Boolean,
    isOnline: Boolean,
    onRemoveAttendee: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only the creator can remove added (non-creator) attendees, and only online while editing.
    val canRemove = isEditMode && isUserCreator && isOnline && !attendee.isCreator
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            // initials is pre-computed in AttendeeUi.toUi() — never compute inline.
            Text(
                attendee.initials,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(
            text = if (attendee.userId == currentUserId) "${attendee.fullName} (you)" else attendee.fullName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (attendee.isCreator) {
            Text(
                "CREATOR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (canRemove) {
            IconButton(onClick = { onRemoveAttendee(attendee.userId) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Remove visitor", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EventPhotosSection(
    photos: List<PhotoUi>,
    isEditMode: Boolean,
    isOnline: Boolean,
    onAddPhoto: () -> Unit,
    onPhotoClick: (PhotoUi) -> Unit,
    onDeletePhoto: (PhotoUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = currentDeviceConfiguration()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Photos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!isOnline) {
                    // Design shows a neutral icon matching the header text tone — default tint
                    // (LocalContentColor), not error red.
                    Icon(Icons.Default.CloudOff, "Offline")
                }
            }
            if (isEditMode && isOnline && photos.size < 10) {
                IconButton(onClick = onAddPhoto) {
                    Icon(Icons.Default.AddPhotoAlternate, "Add photo", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        if (!isOnline) {
            Text(
                "Photo changes require an internet connection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (photos.isEmpty()) {
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = isOnline, onClick = onAddPhoto),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Add photos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else if (config == DeviceConfiguration.MOBILE_PORTRAIT) {
            // Non-lazy 2-column grid: this section lives inside the scaffold's verticalScroll column,
            // where a LazyVerticalGrid does not measure/render reliably. Photos are capped at 10.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                photos.chunked(2).forEach { rowPhotos ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowPhotos.forEach { photo ->
                            PhotoThumbnail(
                                photo, isEditMode && isOnline, onPhotoClick, onDeletePhoto,
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                            )
                        }
                        if (rowPhotos.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                photos.forEach { photo ->
                    PhotoThumbnail(photo, isEditMode && isOnline, onPhotoClick, onDeletePhoto, modifier = Modifier.size(80.dp))
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(
    photo: PhotoUi,
    canDelete: Boolean,
    onPhotoClick: (PhotoUi) -> Unit,
    onDeletePhoto: (PhotoUi) -> Unit,
    modifier: Modifier = Modifier.aspectRatio(1f),
) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        AsyncImage(
            model = photo.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clickable { onPhotoClick(photo) },
        )
        if (canDelete) {
            IconButton(
                onClick = { onDeletePhoto(photo) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(Icons.Default.Close, "Delete photo", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAttendeeSheet(
    email: String,
    emailError: String?,
    isEmailValid: Boolean,
    isChecking: Boolean,
    isAdded: Boolean,
    onEmailChanged: (String) -> Unit,
    onAddClicked: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Cancel dismisses synchronously; the Add button starts an async check (spinner) and the sheet
    // stays open. On success the ViewModel flips isAdded → animate the sheet out here, then dismiss.
    fun hideThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }
    LaunchedEffect(isAdded) {
        if (isAdded) {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, // swipe/scrim already animates before this fires
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Add visitor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChanged,
                label = { Text("Email address") },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                // Green check only for a valid email format (per requirements).
                trailingIcon = if (emailError == null && isEmailValid) {
                    { Icon(Icons.Default.CheckCircle, null, tint = extendedColors.taskyGreen) }
                } else null,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { hideThen(onDismiss) }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onAddClicked,
                    // The Add button is clickable only for a valid email format.
                    enabled = isEmailValid && !isChecking,
                    colors = ButtonDefaults.buttonColors(containerColor = extendedColors.taskyGreen),
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Add")
                    }
                }
            }
        }
    }
}
