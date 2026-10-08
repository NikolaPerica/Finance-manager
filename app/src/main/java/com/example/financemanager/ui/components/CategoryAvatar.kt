package com.example.financemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.financemanager.data.Category
import com.example.financemanager.ui.Croatian
import com.example.financemanager.ui.theme.FinanceTheme

/** The colour a category is drawn with. */
@Composable
fun categoryColor(category: Category): Color = FinanceTheme.colors.categories[category.colorIndex]

/** Round badge in the category's colour with its first letter (decorative; the name is always shown next to it). */
@Composable
fun CategoryAvatar(category: Category, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).background(categoryColor(category), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            category.name.take(1).uppercase(Croatian),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}

/** Small dot in the category's colour, for use next to its name. */
@Composable
fun CategoryDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).background(color, CircleShape))
}
