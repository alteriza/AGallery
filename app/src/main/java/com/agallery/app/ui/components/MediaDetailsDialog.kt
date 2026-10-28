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
        title = { Text("Detail Media") },
        text = {
            Column {
                DetailRow("Nama", photo.name)
                DetailRow("Tipe", if (photo.isVideo) "Video" else "Gambar")
                DetailRow("Resolusi", "${photo.width} × ${photo.height}")
                DetailRow("Ukuran", formatBytes(photo.size))
                DetailRow("Tanggal", formatDate(photo.dateTaken))
                DetailRow("Album", photo.bucket)
                if (photo.isVideo) DetailRow("Durasi", formatDuration(photo.duration))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
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
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
