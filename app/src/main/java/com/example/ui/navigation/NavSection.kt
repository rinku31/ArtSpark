package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavSection(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    DISCOVER("Discover", Icons.Rounded.AutoAwesome, "tab_discover"),
    FAVORITES("Favorites", Icons.Rounded.Favorite, "tab_favorites"),
    HISTORY("History", Icons.Rounded.History, "tab_history")
}
