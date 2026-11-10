package com.instachat.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instachat.app.R
import com.instachat.app.ui.theme.LocalInstaColors

@Composable
fun LogoWordmark(
    modifier: Modifier = Modifier,
    markSize: Int = 34,
    textSize: Int = 22
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_mark),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(markSize.dp)
        )
        Row {
            Text(
                text = "Insta",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Chat",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.linearGradient(LocalInstaColors.current.brandGradient)
                )
            )
        }
    }
}
