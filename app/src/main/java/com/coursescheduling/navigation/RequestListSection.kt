package com.coursescheduling.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coursescheduling.domain.model.ScheduleRequestEntity

@Composable
fun RequestListSection(
    requests: List<ScheduleRequestEntity>,
    onAction: (ScheduleRequestEntity, Boolean) -> Unit
) {
    if (requests.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Pending Requests (${requests.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(requests) { request ->
                RequestCard(request, onAction)
            }
        }
    }
}

@Composable
fun RequestCard(
    request: ScheduleRequestEntity,
    onAction: (ScheduleRequestEntity, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    request.lecturerName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    request.requestType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Slot: ${listOf("Mon", "Tue", "Wed", "Thu", "Fri")[request.weekday]}, Slot ${request.timeSlot + 1}",
                style = MaterialTheme.typography.bodySmall
            )
            if (request.note.isNotBlank()) {
                Text(
                    "Note: ${request.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onAction(request, false) },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject")
                }
                IconButton(
                    onClick = { onAction(request, true) },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF43A047))
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approve")
                }
            }
        }
    }
}
