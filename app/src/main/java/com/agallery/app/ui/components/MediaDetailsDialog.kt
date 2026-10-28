package com.agallery.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agallery.app.data.model.Photo
import com.agallery.app.util.formatBytes
import com.agallery.app.util.formatDate
import com.agallery.app.util.formatDuration

@Composable
fun MediaDetailsDialog(photo: Photo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Media Details", color = Color.White) },
        text = {
            Column {
                DetailRow("Name", photo.name)
                DetailRow("Type", if (photo.isVideo) "Video" else "Image")
                DetailRow("Resolution", "${photo.width} × ${photo.height}")
                DetailRow("Size", formatBytes(photo.size))
                DetailRow("Date", formatDate(photo.dateTaken))
                DetailRow("Album", photo.bucket)
                if (photo.isVideo) DetailRow("Duration", formatDuration(photo.duration))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            modifier = Modifier.width(88.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}
