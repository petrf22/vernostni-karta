package io.github.petrf22.vernostnikarta.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.petrf22.vernostnikarta.R
import io.github.petrf22.vernostnikarta.backup.BackupCodec
import io.github.petrf22.vernostnikarta.backup.BackupFile
import io.github.petrf22.vernostnikarta.location.LocationProvider
import io.github.petrf22.vernostnikarta.ui.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val locationSetting by container.settings.locationEnabled.collectAsStateWithLifecycle(initialValue = false)
    val maxBrightness by container.settings.maxBrightness.collectAsStateWithLifecycle(initialValue = true)
    val usages by container.repository.usages.collectAsStateWithLifecycle(initialValue = emptyList())

    var hasLocationPermission by remember { mutableStateOf(container.location.hasPermission()) }
    val permissionDenied = stringResource(R.string.location_permission_denied)
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            hasLocationPermission = result.values.any { it }
            scope.launch {
                if (hasLocationPermission) {
                    container.settings.setLocationEnabled(true)
                } else {
                    snackbar.showSnackbar(permissionDenied)
                }
            }
        }

    val exportDone = stringResource(R.string.export_done)
    val exportFailed = stringResource(R.string.export_failed)
    val importFailed = stringResource(R.string.import_failed)
    val locationsDeleted = stringResource(R.string.locations_deleted)

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                scope.launch {
                    val ok = try {
                        val json = container.backup.exportJson()
                        withContext(Dispatchers.IO) { writeText(context, uri, json) }
                    } catch (e: Exception) {
                        false
                    }
                    snackbar.showSnackbar(if (ok) exportDone else exportFailed)
                }
            }
        }

    var pendingImport by remember { mutableStateOf<BackupFile?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val backup = try {
                    withContext(Dispatchers.IO) { readText(context, uri)?.let(BackupCodec::decode) }
                } catch (e: Exception) {
                    null
                }
                if (backup == null) snackbar.showSnackbar(importFailed) else pendingImport = backup
            }
        }
    }

    var confirmClearLocations by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.section_location))
            val locationOn = locationSetting && hasLocationPermission
            val toggleLocation: (Boolean) -> Unit = { on ->
                when {
                    !on -> scope.launch { container.settings.setLocationEnabled(false) }
                    hasLocationPermission -> scope.launch { container.settings.setLocationEnabled(true) }
                    else -> locationPermissionLauncher.launch(LocationProvider.PERMISSIONS)
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_location_title)) },
                supportingContent = { Text(stringResource(R.string.settings_location_desc)) },
                trailingContent = { Switch(checked = locationOn, onCheckedChange = toggleLocation) },
                modifier = Modifier.clickable { toggleLocation(!locationOn) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_clear_locations)) },
                supportingContent = {
                    Text(pluralStringResource(R.plurals.locations_count, usages.size, usages.size))
                },
                modifier = Modifier.clickable(enabled = usages.isNotEmpty()) { confirmClearLocations = true },
            )

            HorizontalDivider()
            SectionTitle(stringResource(R.string.section_display))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_brightness_title)) },
                supportingContent = { Text(stringResource(R.string.settings_brightness_desc)) },
                trailingContent = {
                    Switch(
                        checked = maxBrightness,
                        onCheckedChange = { scope.launch { container.settings.setMaxBrightness(it) } },
                    )
                },
                modifier = Modifier.clickable { scope.launch { container.settings.setMaxBrightness(!maxBrightness) } },
            )

            HorizontalDivider()
            SectionTitle(stringResource(R.string.section_backup))
            ListItem(
                headlineContent = { Text(stringResource(R.string.export_title)) },
                supportingContent = { Text(stringResource(R.string.export_desc)) },
                modifier = Modifier.clickable { exportLauncher.launch("vernostni-karta-${LocalDate.now()}.json") },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.import_title)) },
                supportingContent = { Text(stringResource(R.string.import_desc)) },
                modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
            )

            HorizontalDivider()
            SectionTitle(stringResource(R.string.section_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = {
                    Column {
                        Text(stringResource(R.string.about_version, appVersion(context)))
                        Text(stringResource(R.string.about_license))
                        Text(stringResource(R.string.about_source))
                    }
                },
            )
            ListItem(headlineContent = { Text(stringResource(R.string.about_privacy)) })
        }
    }

    if (confirmClearLocations) {
        AlertDialog(
            onDismissRequest = { confirmClearLocations = false },
            text = { Text(stringResource(R.string.clear_locations_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearLocations = false
                        scope.launch {
                            container.repository.clearUsages()
                            snackbar.showSnackbar(locationsDeleted)
                        }
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearLocations = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    pendingImport?.let { backup ->
        val count = backup.cards.size
        val resources = LocalResources.current
        fun runImport(replace: Boolean) {
            pendingImport = null
            scope.launch {
                val message = try {
                    val imported = container.backup.import(backup, replace)
                    hasLocationPermission = container.location.hasPermission()
                    resources.getQuantityString(R.plurals.import_done, imported, imported)
                } catch (e: Exception) {
                    importFailed
                }
                snackbar.showSnackbar(message)
            }
        }
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.import_dialog_title)) },
            text = {
                Column {
                    Text(pluralStringResource(R.plurals.import_file_cards, count, count))
                    Text(stringResource(R.string.import_dialog_hint), modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { runImport(replace = false) }) { Text(stringResource(R.string.import_merge)) }
            },
            dismissButton = {
                TextButton(onClick = { runImport(replace = true) }) { Text(stringResource(R.string.import_replace)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

private fun writeText(context: Context, uri: Uri, text: String): Boolean {
    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return false
    stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    return true
}

private fun readText(context: Context, uri: Uri): String? =
    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }

private fun appVersion(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
} catch (e: Exception) {
    ""
}
