package io.github.petrf22.vernostnikarta.ui.detail

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.petrf22.vernostnikarta.R
import io.github.petrf22.vernostnikarta.barcode.CodeFormat
import io.github.petrf22.vernostnikarta.data.CardEntity
import io.github.petrf22.vernostnikarta.ui.appContainer
import io.github.petrf22.vernostnikarta.ui.components.BarcodeView
import io.github.petrf22.vernostnikarta.ui.components.LoyaltyCardView
import kotlinx.coroutines.launch

/** The card, ready to be shown at the checkout. */
@Composable
fun DetailScreen(cardId: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val card by remember(cardId) { container.repository.observeCard(cardId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val maxBrightness by container.settings.maxBrightness.collectAsStateWithLifecycle(initialValue = false)

    var usageRecorded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(cardId) {
        if (!usageRecorded) {
            usageRecorded = true
            container.recordUsage(cardId)
        }
    }

    BrightScreen(maxBrightness)

    var confirmDelete by remember { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                    }
                },
            )
        },
    ) { padding ->
        val current = card ?: return@Scaffold
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            LoyaltyCardView(
                name = current.name,
                note = current.note,
                code = current.code,
                format = current.codeFormat(),
                color = Color(current.color),
                onCodeClick = { fullscreen = true },
            )
        }
        if (fullscreen) {
            FullscreenCode(current, onDismiss = { fullscreen = false })
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_card_title)) },
            text = { Text(stringResource(R.string.delete_card_text, card?.name.orEmpty())) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            container.repository.delete(cardId)
                            onBack()
                        }
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private fun CardEntity.codeFormat(): CodeFormat = CodeFormat.fromName(format) ?: CodeFormat.QR_CODE

/** Just the code on white, as large as the screen allows — for scanners that struggle with a small one. */
@Composable
private fun FullscreenCode(card: CardEntity, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = Color.White,
            contentColor = Color.Black,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                val format = card.codeFormat()
                BarcodeView(
                    code = card.code,
                    format = format,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(if (format.is2D) 0.6f else 0.3f),
                )
                Text(
                    text = card.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/** Keeps the screen on and, when [maxBrightness] is set, at full brightness while the card is visible. */
@Composable
private fun BrightScreen(maxBrightness: Boolean) {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window, maxBrightness) {
        val previous = window.attributes.screenBrightness
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (maxBrightness) {
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
        }
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply { screenBrightness = previous }
        }
    }
}
