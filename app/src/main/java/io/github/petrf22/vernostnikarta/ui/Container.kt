package io.github.petrf22.vernostnikarta.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.petrf22.vernostnikarta.App
import io.github.petrf22.vernostnikarta.AppContainer

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as App).container
