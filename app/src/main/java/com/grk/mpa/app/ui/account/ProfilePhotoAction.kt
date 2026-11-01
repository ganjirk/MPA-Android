package com.grk.mpa.app.ui.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
internal fun ProfilePhotoAction(viewModel: AccountViewModel, photoUrl: String?, displayName: String) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null) {
                val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                viewModel.uploadProfilePicture(bytes, type, "profile-photo")
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile picture",
                modifier = Modifier.size(72.dp).clip(CircleShape)
            )
        } else {
            Text(displayName.take(1).uppercase())
        }
        OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !viewModel.profileSaving) {
            Text("Change profile picture")
        }
        if (photoUrl != null) {
            OutlinedButton(onClick = viewModel::removeProfilePicture, enabled = !viewModel.profileSaving) {
                Text("Remove profile picture")
            }
        }
        Text("JPG, PNG or WebP · maximum 1 MB")
    }
}
