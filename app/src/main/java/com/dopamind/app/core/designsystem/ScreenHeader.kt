package com.dopamind.app.core.designsystem

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.dopamind.app.R
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary

/** The standard "back arrow + module title" header used at the top of every module screen. */
@Composable
fun ScreenHeader(titleRes: Int, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = TextSecondary)
        }
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
    }
}
