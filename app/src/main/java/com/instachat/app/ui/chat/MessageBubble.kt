package com.instachat.app.ui.chat

import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.instachat.app.R
import com.instachat.app.data.local.entity.MessageEntity
import com.instachat.app.data.local.entity.MessageStatus
import com.instachat.app.data.local.entity.MessageType
import com.instachat.app.data.local.entity.Sender
import com.instachat.app.ui.components.Avatar
import com.instachat.app.ui.components.TypingDots
import com.instachat.app.util.TimeText
import java.io.File
import java.util.Random
import kotlinx.coroutines.delay

@Composable
fun DateChip(
    timestamp: Long,
    palette: ChatPalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = TimeText.chatSeparator(LocalContext.current, timestamp),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.2.sp
            ),
            color = Color(0xFF8E8E93)
        )
    }
}

enum class MessagePosition { SINGLE, FIRST, MIDDLE, LAST }

@Composable
fun MessageBubble(
    message: MessageEntity,
    position: MessagePosition,
    showAvatar: Boolean,
    contactName: String,
    contactAvatar: String?,
    showReceipt: Boolean,
    palette: ChatPalette,
    animateArrival: Boolean,
    onLongPress: (MessageEntity) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val mine = message.sender == Sender.ME
    val density = LocalDensity.current
    val alpha = remember { Animatable(if (animateArrival) 0f else 1f) }
    val offset = remember { Animatable(if (animateArrival) with(density) { 12.dp.toPx() } else 0f) }

    LaunchedEffect(Unit) {
        if (animateArrival) {
            alpha.animateTo(1f, tween(200))
            offset.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha.value
                translationY = offset.value
            }
            .padding(horizontal = 12.dp)
    ) {
        val bubbleMax = (maxWidth * 0.74f).coerceAtMost(420.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            if (!mine) {
                if (showAvatar) {
                    Avatar(
                        name = contactName,
                        path = contactAvatar,
                        size = 28.dp
                    )
                } else {
                    Spacer(Modifier.size(28.dp))
                }
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }

            Column(
                horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = bubbleMax)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BubbleBody(
                        message = message,
                        position = position,
                        mine = mine,
                        palette = palette,
                        onLongPress = { onLongPress(message) },
                        onImageClick = onImageClick
                    )
                    if (!mine && message.type == MessageType.IMAGE) {
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF262626)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_send),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
                if (showReceipt) {
                    ReceiptRow(
                        status = message.status,
                        timestamp = message.timestamp
                    )
                }
            }

            if (!mine) Spacer(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BubbleBody(
    message: MessageEntity,
    position: MessagePosition,
    mine: Boolean,
    palette: ChatPalette,
    onLongPress: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val outer = 22.dp
    val innerJoined = 4.dp
    val startsGroup = position == MessagePosition.FIRST || position == MessagePosition.SINGLE
    val endsGroup = position == MessagePosition.LAST || position == MessagePosition.SINGLE
    val bubbleShape = if (!mine) {
        RoundedCornerShape(
            topStart = if (startsGroup) outer else innerJoined,
            bottomStart = if (endsGroup) outer else innerJoined,
            topEnd = outer,
            bottomEnd = outer
        )
    } else {
        RoundedCornerShape(
            topStart = outer,
            bottomStart = outer,
            topEnd = if (startsGroup) outer else innerJoined,
            bottomEnd = if (endsGroup) outer else innerJoined
        )
    }

    val reaction = message.reaction
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenHeightPx = remember(config.screenHeightDp, density) {
        with(density) { config.screenHeightDp.dp.toPx() }.coerceAtLeast(1f)
    }
    var bubbleYInWindow by remember { mutableFloatStateOf(screenHeightPx * 0.55f) }

    val spokenSummary = when (message.type) {
        MessageType.IMAGE -> "a photo"
        MessageType.VOICE -> "a voice message, ${TimeText.formatDuration(message.durationMs ?: 0L)}"
        MessageType.TEXT -> message.text.orEmpty()
    }
    val spokenWho = if (mine) "You" else "Friend"
    val spokenReaction = reaction?.let { ", reacted $it" }.orEmpty()
    val spokenTime = TimeText.messageTime(LocalContext.current, message.timestamp)

    Box(
        modifier = Modifier
            .padding(bottom = if (reaction != null) 12.dp else 0.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$spokenWho said $spokenSummary at $spokenTime$spokenReaction"
            }
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    bubbleYInWindow = coords.positionInWindow().y
                }
                .clip(bubbleShape)
                .background(
                    brush = if (mine) {
                        Brush.verticalGradient(
                            colors = listOf(palette.sentStart, palette.sentEnd),
                            startY = -bubbleYInWindow,
                            endY = (screenHeightPx - bubbleYInWindow).coerceAtLeast(-bubbleYInWindow + 200f)
                        )
                    } else {
                        Brush.verticalGradient(listOf(palette.received, palette.received))
                    }
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (message.type == MessageType.IMAGE && message.mediaPath != null) {
                            onImageClick(message.mediaPath)
                        }
                    },
                    onLongClick = onLongPress
                )
        ) {
            when (message.type) {
                MessageType.IMAGE -> ImageContent(
                    message = message,
                    shape = bubbleShape
                )
                MessageType.VOICE -> VoiceContent(message = message, mine = mine, palette = palette)
                MessageType.TEXT -> TextContent(
                    message = message,
                    mine = mine,
                    palette = palette
                )
            }
        }

        if (reaction != null) {
            val reactionScale = remember(reaction) { Animatable(0.8f) }
            LaunchedEffect(reaction) {
                reactionScale.animateTo(
                    targetValue = 1.15f,
                    animationSpec = tween(90)
                )
                reactionScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(90)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(
                        x = if (mine) (-20).dp else 20.dp,
                        y = 14.dp
                    )
                    .graphicsLayer {
                        scaleX = reactionScale.value
                        scaleY = reactionScale.value
                    }
                    .background(
                        color = Color(0xFF262626),
                        shape = RoundedCornerShape(50)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = reaction, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TextContent(
    message: MessageEntity,
    mine: Boolean,
    palette: ChatPalette
) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        if (!message.text.isNullOrBlank()) {
            Text(
                text = message.text,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = if (mine) palette.onSent else palette.onReceived
            )
        }
    }
}

@Composable
private fun ImageContent(
    message: MessageEntity,
    shape: RoundedCornerShape
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .height(210.dp)
            .clip(shape)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(message.mediaPath?.let { File(it) })
                .crossfade(true)
                .build(),
            contentDescription = "Photo message",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun VoiceContent(
    message: MessageEntity,
    mine: Boolean,
    palette: ChatPalette
) {
    val content = if (mine) palette.onSent else palette.onReceived
    val bars = remember(message.id) { voiceBars(message.id, 32) }
    val player = remember(message.mediaPath) { MediaPlayer() }
    var prepared by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(message.durationMs ?: 0L) }

    DisposableEffect(player) {
        onDispose {
            runCatching { player.release() }
        }
    }

    LaunchedEffect(playing) {
        while (playing) {
            val total = runCatching { player.duration }.getOrDefault(0)
            if (total > 0) {
                duration = total.toLong()
                progress = (player.currentPosition.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            }
            delay(80)
        }
    }

    fun togglePlayback() {
        val path = message.mediaPath ?: run { failed = true; return }
        runCatching {
            if (playing) {
                player.pause()
                playing = false
            } else {
                if (!prepared) {
                    player.setDataSource(path)
                    player.prepare()
                    player.setOnCompletionListener {
                        playing = false
                        progress = 0f
                        runCatching { player.seekTo(0) }
                    }
                    prepared = true
                }
                player.start()
                playing = true
            }
        }.onFailure {
            failed = true
            playing = false
        }
    }

    Column(
        modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(content.copy(alpha = 0.18f))
                    .clickable { togglePlayback() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (playing) R.drawable.ic_insta_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (playing) "Pause voice message" else "Play voice message",
                    tint = content,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
            Canvas(
                modifier = Modifier
                    .width(146.dp)
                    .height(28.dp)
            ) {
                val gap = 2.2.dp.toPx()
                val barWidth = ((size.width - gap * (bars.size - 1)) / bars.size)
                    .coerceAtLeast(1.6.dp.toPx())
                val playedBars = (progress * bars.size).toInt()
                bars.forEachIndexed { index, amplitude ->
                    val height = (size.height * (0.22f + amplitude * 0.78f))
                    val left = index * (barWidth + gap)
                    val top = (size.height - height) / 2f
                    val played = index <= playedBars && (playing || progress > 0f)
                    drawRoundRect(
                        color = content.copy(alpha = if (played) 0.96f else 0.4f),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (failed) "—:—" else TimeText.formatDuration(duration),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = content.copy(alpha = if (failed) 0.4f else 0.78f)
            )
        }
    }
}

private fun voiceBars(seed: Long, count: Int): List<Float> {
    val random = Random(seed * 31L + 7L)
    return List(count) {
        0.18f + random.nextFloat() * 0.82f
    }
}

@Composable
private fun ReceiptRow(status: MessageStatus, timestamp: Long) {
    val label = when (status) {
        MessageStatus.SENT -> "Sent"
        MessageStatus.DELIVERED -> "Delivered"
        MessageStatus.SEEN -> "Seen ${TimeText.seenDayLabel(timestamp)}"
    }
    Text(
        text = label,
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Normal
        ),
        color = Color(0xFF8E8E93),
        modifier = Modifier.padding(top = 4.dp, end = 2.dp)
    )
}

@Composable
fun TypingMessageRow(contactName: String, contactAvatar: String?, palette: ChatPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Avatar(
            name = contactName,
            path = contactAvatar,
            size = 28.dp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(palette.received)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TypingDots(color = palette.onReceived.copy(alpha = 0.6f))
        }
        Spacer(Modifier.weight(1f))
    }
}
