package com.music.vivi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.blend.SupabaseBlendClient
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.viewmodels.BlendResult
import com.music.vivi.viewmodels.BlendViewModel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendScreen(
    navController: NavController,
    viewModel: BlendViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val blendResult by viewModel.blendResult.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isJoining by viewModel.isJoining.collectAsState()
    val joinedBlend by viewModel.joinedBlend.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    var user1 by remember { mutableStateOf("") }
    var user2 by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Blend", fontWeight = FontWeight.Bold)
                        Text(
                            "Compare your taste with a friend",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BlendInputCard(
                    user1 = user1,
                    user2 = user2,
                    onUser1Change = { user1 = it },
                    onUser2Change = { user2 = it },
                    isLoading = isLoading,
                    onCreateBlend = { viewModel.createBlend(user1, user2) },
                    colorScheme = colorScheme
                )
            }

            if (error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error!!,
                            modifier = Modifier.padding(16.dp),
                            color = colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (blendResult != null) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        BlendResultCard(
                            result = blendResult!!,
                            isSaving = isSaving,
                            onSave = { viewModel.saveToSupabase() },
                            colorScheme = colorScheme
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                JoinBlendCard(
                    code = joinCode,
                    onCodeChange = { joinCode = it },
                    isJoining = isJoining,
                    onJoin = { viewModel.joinBlend(joinCode) },
                    joinedBlend = joinedBlend,
                    colorScheme = colorScheme
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BlendInputCard(
    user1: String,
    user2: String,
    onUser1Change: (String) -> Unit,
    onUser2Change: (String) -> Unit,
    isLoading: Boolean,
    onCreateBlend: () -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "CREATE A BLEND",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 1.5.sp
            )
            OutlinedTextField(
                value = user1,
                onValueChange = onUser1Change,
                label = { Text("Your Last.fm username") },
                leadingIcon = {
                    Icon(painterResource(R.drawable.person), contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = user2,
                onValueChange = onUser2Change,
                label = { Text("Friend's Last.fm username") },
                leadingIcon = {
                    Icon(painterResource(R.drawable.group_outlined), contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onCreateBlend() }),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = onCreateBlend,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && user1.isNotBlank() && user2.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Blending…")
                } else {
                    Text("Create Blend", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun BlendResultCard(
    result: BlendResult,
    isSaving: Boolean,
    onSave: () -> Unit,
    colorScheme: ColorScheme
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${result.user1}  ×  ${result.user2}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            CompatibilityRing(score = result.compatibilityScore, accentColor = colorScheme.primary)

            if (result.sharedArtists.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SHARED ARTISTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(result.sharedArtists) { _, artist ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(artist, style = MaterialTheme.typography.bodySmall) },
                                icon = {
                                    Icon(
                                        painterResource(R.drawable.music_note),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TopListColumn(
                    title = result.user1.uppercase(),
                    items = result.user1Artists.take(5).map { it.name },
                    modifier = Modifier.weight(1f),
                    colorScheme = colorScheme
                )
                VerticalDivider(modifier = Modifier.height(160.dp))
                TopListColumn(
                    title = result.user2.uppercase(),
                    items = result.user2Artists.take(5).map { it.name },
                    modifier = Modifier.weight(1f),
                    colorScheme = colorScheme
                )
            }

            if (result.blendCode != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "BLEND CODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                text = result.blendCode,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimaryContainer,
                                letterSpacing = 4.sp
                            )
                        }
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(result.blendCode)) }
                        ) {
                            Icon(
                                painterResource(R.drawable.content_copy),
                                contentDescription = "Copy",
                                tint = colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            } else if (SupabaseBlendClient.isConfigured) {
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Saving…")
                    } else {
                        Icon(painterResource(R.drawable.ios_share), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share Blend")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompatibilityRing(score: Int, accentColor: Color) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "score"
    )
    val sweepAngle = (animatedScore / 100f) * 270f

    val ringColor = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFF9800)
        score >= 40 -> accentColor
        else -> Color(0xFFE57373)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 18.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val arcOffset = Offset(inset, inset)
            drawArc(
                color = ringColor.copy(alpha = 0.12f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${score}%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = ringColor
            )
            Text(
                text = when {
                    score >= 80 -> "Twin flames 🔥"
                    score >= 60 -> "Great match ✨"
                    score >= 40 -> "Decent vibes 🎵"
                    else -> "Different worlds 🌍"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TopListColumn(
    title: String,
    items: List<String>,
    modifier: Modifier,
    colorScheme: ColorScheme
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            letterSpacing = 0.5.sp
        )
        items.forEachIndexed { i, name ->
            Text(
                text = "${i + 1}. $name",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = if (i == 0) 1f else 0.7f),
                fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun JoinBlendCard(
    code: String,
    onCodeChange: (String) -> Unit,
    isJoining: Boolean,
    onJoin: () -> Unit,
    joinedBlend: com.music.vivi.blend.BlendRecord?,
    colorScheme: ColorScheme
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "JOIN A BLEND",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 1.5.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { onCodeChange(it.uppercase().take(6)) },
                    label = { Text("Blend code") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onJoin() }),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = onJoin,
                    enabled = code.length >= 4 && !isJoining,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colorScheme.onPrimary)
                    } else {
                        Text("Join")
                    }
                }
            }

            if (joinedBlend != null) {
                HorizontalDivider()
                Text(
                    text = "${joinedBlend.user1_username}  ×  ${joinedBlend.user2_username}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Match:", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        "${joinedBlend.compatibility_score.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }
                if (joinedBlend.shared_artists.isNotBlank()) {
                    Text(
                        "Shared: ${joinedBlend.shared_artists}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
