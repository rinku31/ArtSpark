package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.NavSection
import com.example.ui.screens.BrainstormScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanInkBlack
import com.example.ui.theme.CleanMutedText
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.SparkYellow
import com.example.ui.viewmodel.ArtSparkViewModel

@Composable
fun MainScreen(
    viewModel: ArtSparkViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(NavSection.DISCOVER) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        bottomBar = {
            val borderColor = MaterialTheme.colorScheme.outlineVariant
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .drawBehind {
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            ) {
                NavSection.values().forEach { section ->
                    val isSelected = selectedSection == section
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedSection = section },
                        icon = {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.title
                            )
                        },
                        label = {
                            Text(
                                text = section.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.2.sp
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF141413),
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = SparkYellow,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(section.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = selectedSection,
                label = "screen_transition"
            ) { section ->
                when (section) {
                    NavSection.DISCOVER -> {
                        DiscoverScreen(
                            viewModel = viewModel,
                            onOpenSettings = { isSettingsOpen = true },
                            onNavigateToBrainstorm = { selectedSection = NavSection.BRAINSTORM }
                        )
                    }
                    NavSection.BRAINSTORM -> {
                        BrainstormScreen(
                            viewModel = viewModel,
                            onNavigateToDiscover = { selectedSection = NavSection.DISCOVER }
                        )
                    }
                    NavSection.FAVORITES -> {
                        FavoritesScreen(
                            viewModel = viewModel,
                            onNavigateToDiscover = { selectedSection = NavSection.DISCOVER }
                        )
                    }
                    NavSection.HISTORY -> {
                        HistoryScreen(
                            viewModel = viewModel,
                            onNavigateToDiscover = { selectedSection = NavSection.DISCOVER }
                        )
                    }
                }
            }
        }
    }

    if (isSettingsOpen) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { isSettingsOpen = false }
        )
    }
}
