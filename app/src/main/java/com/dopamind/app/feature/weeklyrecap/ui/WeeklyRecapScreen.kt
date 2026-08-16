package com.dopamind.app.feature.weeklyrecap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.analytics.model.Trend
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.feature.alcohol.data.DrinkType
import com.dopamind.app.feature.weeklyrecap.domain.RecapCard
import com.dopamind.app.feature.weeklyrecap.domain.RecapCardType
import kotlin.math.roundToInt

@Composable
fun WeeklyRecapScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> WeeklyRecapViewModel(container.weeklyRecapGenerator) }
    val cards by viewModel.cards.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        val cardList = cards
        if (cardList == null) {
            Text(
                text = stringResource(R.string.recap_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            val pagerState = rememberPagerState(initialPage = 0) { cardList.size }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                RecapCardContent(cardList[page])
            }
        }

        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = TextSecondary)
        }
    }
}

@Composable
private fun RecapCardContent(card: RecapCard) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        DMCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                Text(text = emojiFor(card.type), fontSize = 40.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = valueLine(card),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Accent,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = captionLine(card),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun emojiFor(type: RecapCardType): String = when (type) {
    RecapCardType.TOTAL_SPEND -> "💸"
    RecapCardType.ALCOHOL_SPEND -> "🍹"
    RecapCardType.SMOKE_FREE_DAYS -> "🫁"
    RecapCardType.CANNABIS_SESSIONS -> "🌿"
    RecapCardType.SLEEP_AVERAGE -> "😴"
    RecapCardType.DOPAMINE_DEBT_TREND -> "⚡"
    RecapCardType.MOOD_AVERAGE -> "🙂"
    RecapCardType.NO_DATA -> "🌱"
}

@Composable
private fun valueLine(card: RecapCard): String = when (card.type) {
    RecapCardType.TOTAL_SPEND, RecapCardType.ALCOHOL_SPEND -> stringResource(R.string.recap_value_euros, card.primaryValue)
    RecapCardType.SMOKE_FREE_DAYS -> stringResource(R.string.recap_value_days, card.primaryValue.roundToInt())
    RecapCardType.CANNABIS_SESSIONS -> card.primaryValue.roundToInt().toString()
    RecapCardType.SLEEP_AVERAGE -> stringResource(R.string.recap_value_hours, card.primaryValue)
    RecapCardType.DOPAMINE_DEBT_TREND -> stringResource(R.string.recap_value_score, card.primaryValue.roundToInt())
    RecapCardType.MOOD_AVERAGE -> stringResource(R.string.recap_value_score_5, card.primaryValue)
    RecapCardType.NO_DATA -> ""
}

@Composable
private fun captionLine(card: RecapCard): String = when (card.type) {
    RecapCardType.TOTAL_SPEND -> stringResource(R.string.recap_caption_total_spend)
    RecapCardType.ALCOHOL_SPEND -> {
        val drinkType = card.secondaryLabel?.let { name -> runCatching { DrinkType.valueOf(name) }.getOrNull() }
        stringResource(R.string.recap_caption_alcohol_spend, stringResource(drinkTypeLabelRes(drinkType)))
    }
    RecapCardType.SMOKE_FREE_DAYS -> stringResource(R.string.recap_caption_smoke_free)
    RecapCardType.CANNABIS_SESSIONS -> stringResource(R.string.recap_caption_cannabis_sessions)
    RecapCardType.SLEEP_AVERAGE -> stringResource(R.string.recap_caption_sleep)
    RecapCardType.DOPAMINE_DEBT_TREND -> stringResource(
        when (card.trend) {
            Trend.RISING -> R.string.recap_caption_dopamine_debt_rising
            Trend.FALLING -> R.string.recap_caption_dopamine_debt_falling
            else -> R.string.recap_caption_dopamine_debt_stable
        }
    )
    RecapCardType.MOOD_AVERAGE -> stringResource(R.string.recap_caption_mood)
    RecapCardType.NO_DATA -> stringResource(R.string.recap_caption_no_data)
}

private fun drinkTypeLabelRes(drinkType: DrinkType?): Int = when (drinkType) {
    DrinkType.BEER -> R.string.drink_type_beer
    DrinkType.WINE -> R.string.drink_type_wine
    DrinkType.SPIRIT_SHOT -> R.string.drink_type_spirit
    DrinkType.SPRITZ -> R.string.drink_type_spritz
    DrinkType.NEGRONI -> R.string.drink_type_negroni
    DrinkType.MOJITO -> R.string.drink_type_mojito
    DrinkType.MARGARITA -> R.string.drink_type_margarita
    DrinkType.GIN_TONIC -> R.string.drink_type_gin_tonic
    DrinkType.COCKTAIL_OTHER -> R.string.drink_type_cocktail_other
    else -> R.string.drink_type_other
}
