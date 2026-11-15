package io.github.petrf22.vernostnikarta.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.petrf22.vernostnikarta.R
import io.github.petrf22.vernostnikarta.data.CardEntity
import io.github.petrf22.vernostnikarta.location.GeoPoint
import io.github.petrf22.vernostnikarta.location.NearbyRanker
import io.github.petrf22.vernostnikarta.ui.appContainer
import io.github.petrf22.vernostnikarta.ui.contentColorOn
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val MAX_NEARBY = 3

@Composable
fun CardListScreen(onAdd: () -> Unit, onOpen: (Long) -> Unit, onSettings: () -> Unit) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val cards by container.repository.cards.collectAsStateWithLifecycle(initialValue = null)
    val usages by container.repository.usages.collectAsStateWithLifecycle(initialValue = emptyList())
    val locationEnabled by container.settings.locationEnabled.collectAsStateWithLifecycle(initialValue = false)

    var query by rememberSaveable { mutableStateOf("") }

    // Local copy of the list so that dragging feels instant; the database follows when the drag ends.
    var ordered by remember { mutableStateOf(emptyList<CardEntity>()) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(cards) { if (!dragging) ordered = cards.orEmpty() }

    var here by remember { mutableStateOf<GeoPoint?>(null) }
    LifecycleResumeEffect(locationEnabled) {
        val job = scope.launch { here = if (locationEnabled) container.location.current() else null }
        onPauseOrDispose { job.cancel() }
    }
    val nearby = remember(here, usages, ordered) {
        val point = here ?: return@remember emptyList()
        NearbyRanker.rank(usages, point, System.currentTimeMillis())
            .mapNotNull { id -> ordered.firstOrNull { it.id == id } }
            .take(MAX_NEARBY)
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = ordered.indexOfFirst { it.id == from.key }
        val toIndex = ordered.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            ordered = ordered.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_card))
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (cards?.isEmpty() == true) {
                EmptyState(stringResource(R.string.no_cards))
                return@Column
            }
            SearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            val searching = query.isNotBlank()
            val filtered = remember(ordered, query) { filterCards(ordered, query) }
            if (searching && filtered.isEmpty()) {
                EmptyState(stringResource(R.string.no_results))
                return@Column
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (searching) {
                    items(filtered, key = { it.id }) { card ->
                        CardTile(card, onClick = { onOpen(card.id) })
                    }
                } else {
                    if (nearby.isNotEmpty()) {
                        item(key = "header-nearby") { SectionHeader(stringResource(R.string.nearby)) }
                        items(nearby, key = { "nearby-${it.id}" }) { card ->
                            CardTile(card, onClick = { onOpen(card.id) })
                        }
                        item(key = "header-all") { SectionHeader(stringResource(R.string.all_cards)) }
                    }
                    items(ordered, key = { it.id }) { card ->
                        ReorderableItem(reorderState, key = card.id) { isDragging ->
                            CardTile(
                                card = card,
                                onClick = { onOpen(card.id) },
                                lifted = isDragging,
                                modifier = Modifier.longPressDraggableHandle(
                                    onDragStarted = { dragging = true },
                                    onDragStopped = {
                                        val ids = ordered.map { it.id }
                                        scope.launch {
                                            container.repository.reorder(ids)
                                            dragging = false
                                        }
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

@Composable
private fun EmptyState(text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CardTile(
    card: CardEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lifted: Boolean = false,
) {
    val color = Color(card.color)
    Surface(
        onClick = onClick,
        color = color,
        contentColor = contentColorOn(color),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (lifted) 10.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (card.note.isNotBlank()) {
                Text(
                    text = card.note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
