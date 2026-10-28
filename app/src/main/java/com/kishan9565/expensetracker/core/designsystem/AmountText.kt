package com.kishan9565.expensetracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.kishan9565.expensetracker.core.money.Money

@Composable
fun AmountText(
    paise: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = Color.Unspecified
) {
    Text(
        text = Money.format(paise),
        modifier = modifier,
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color
    )
}
