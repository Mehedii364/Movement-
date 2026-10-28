package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MovementInGreen
import com.example.ui.theme.MovementInGreenContainer
import com.example.ui.theme.MovementOutAmber
import com.example.ui.theme.MovementOutAmberContainer
import java.io.File

@Composable
fun PersonPhotoAvatar(
    photoPath: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val bitmap = remember(photoPath) {
        photoPath?.let {
            val file = File(it)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(it)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            } else null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Photo of $name",
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initials = name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "P" }

        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val isInside = status.equals("INSIDE", ignoreCase = true)
    val bgColor = if (isInside) MovementInGreenContainer else MovementOutAmberContainer
    val textColor = if (isInside) MovementInGreen else MovementOutAmber

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier.testTag("status_badge_$status")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isInside) "INSIDE" else "OUTSIDE",
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MovementTypeBadge(
    type: String,
    modifier: Modifier = Modifier
) {
    val isOut = type.equals("OUT", ignoreCase = true)
    val bgColor = if (isOut) MovementOutAmberContainer else MovementInGreenContainer
    val textColor = if (isOut) MovementOutAmber else MovementInGreen

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier.testTag("movement_badge_$type")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOut) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isOut) "OUT" else "IN",
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
