package com.dopamind.app.feature.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.DMSecondaryButton
import com.dopamind.app.core.designsystem.DMTextField
import com.dopamind.app.core.designsystem.ProgressRing
import com.dopamind.app.core.designsystem.SectionHeader
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.Danger
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.feature.finance.data.BudgetSettingsEntity
import com.dopamind.app.feature.finance.data.FinanceRepository
import com.dopamind.app.feature.finance.data.SpendCategory
import com.dopamind.app.feature.finance.data.SpendLogEntity
import com.dopamind.app.feature.finance.domain.ConvenienceCalculator
import com.dopamind.app.feature.finance.domain.PricePoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {
    val spends: StateFlow<List<SpendLogEntity>> =
        repository.observeSpends().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budget: StateFlow<BudgetSettingsEntity?> =
        repository.observeBudget().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logSpend(category: SpendCategory, amount: Float, quantity: Float?, unit: String?) {
        viewModelScope.launch { repository.logSpend(category, amount, quantity, unit, note = null) }
    }

    fun setBudget(limit: Float) = viewModelScope.launch { repository.setMonthlyBudget(limit) }
}

@Composable
fun FinanceScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> FinanceViewModel(container.financeRepository) }
    val spends by viewModel.spends.collectAsStateWithLifecycle()
    val budget by viewModel.budget.collectAsStateWithLifecycle()

    val monthStartEpochDay = remember { LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1).toEpochDay() }
    val monthStartMillis = monthStartEpochDay * 86_400_000L
    val spentThisMonth = spends.filter { it.timestampEpochMillis >= monthStartMillis }.sumOf { it.amountEuros.toDouble() }.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.module_finance, onBack = onBack) }
        item { BudgetCard(spentThisMonth, budget, onSetBudget = viewModel::setBudget) }
        item { SpendLogCard(onLog = viewModel::logSpend) }
        item { ConvenienceCalculatorCard(spends) }
        item { SectionHeader(stringResource(R.string.finance_recent_spends)) }
        items(spends.take(10)) { spend -> SpendRow(spend) }
    }
}

@Composable
private fun BudgetCard(spentThisMonth: Float, budget: BudgetSettingsEntity?, onSetBudget: (Float) -> Unit) {
    var limitInput by remember { mutableStateOf("") }
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.finance_budget_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (budget == null) {
            DMTextField(value = limitInput, onValueChange = { limitInput = it }, placeholder = stringResource(R.string.finance_budget_placeholder), singleLine = true)
            Spacer(Modifier.height(8.dp))
            DMPrimaryButton(text = stringResource(R.string.finance_budget_set), onClick = { limitInput.toFloatOrNull()?.let(onSetBudget) })
        } else {
            val progress = (spentThisMonth / budget.monthlyLimitEuros).coerceIn(0f, 1f)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressRing(progress = progress, progressColor = if (progress >= 1f) Danger else Accent) {
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                }
                Text(
                    text = stringResource(R.string.finance_budget_progress, spentThisMonth, budget.monthlyLimitEuros),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun SpendLogCard(onLog: (SpendCategory, Float, Float?, String?) -> Unit) {
    var category by remember { mutableStateOf(SpendCategory.ALCOHOL) }
    var amount by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.finance_spend_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpendCategory.entries.forEach { c -> DMChip(label = categoryLabel(c), selected = category == c, onClick = { category = c }) }
        }
        Spacer(Modifier.height(12.dp))
        DMTextField(value = amount, onValueChange = { amount = it }, placeholder = stringResource(R.string.finance_spend_amount_placeholder), singleLine = true)
        Spacer(Modifier.height(8.dp))
        DMTextField(value = quantity, onValueChange = { quantity = it }, placeholder = stringResource(R.string.finance_spend_quantity_placeholder), singleLine = true)
        Spacer(Modifier.height(12.dp))
        DMPrimaryButton(
            text = stringResource(R.string.finance_spend_save),
            onClick = {
                val amountValue = amount.toFloatOrNull()
                if (amountValue != null) {
                    onLog(category, amountValue, quantity.toFloatOrNull(), null)
                    amount = ""
                    quantity = ""
                }
            },
        )
    }
}

@Composable
private fun ConvenienceCalculatorCard(history: List<SpendLogEntity>) {
    var amount by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf<String?>(null) }

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.finance_convenience_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        DMTextField(value = amount, onValueChange = { amount = it }, placeholder = stringResource(R.string.finance_spend_amount_placeholder), singleLine = true)
        Spacer(Modifier.height(8.dp))
        DMTextField(value = quantity, onValueChange = { quantity = it }, placeholder = stringResource(R.string.finance_convenience_quantity_placeholder), singleLine = true)
        Spacer(Modifier.height(12.dp))
        DMSecondaryButton(
            text = stringResource(R.string.finance_convenience_calculate),
            onClick = {
                val a = amount.toFloatOrNull()
                val q = quantity.toFloatOrNull()
                if (a != null && q != null && q > 0f) {
                    val historyPoints = history.mapNotNull { entry ->
                        entry.quantity?.let { qty -> PricePoint(entry.amountEuros, qty, entry.timestampEpochMillis) }
                    }
                    val result = ConvenienceCalculator.analyze(PricePoint(a, q, System.currentTimeMillis()), historyPoints)
                    resultText = "€%.2f/unit".format(result.pricePerUnit)
                }
            },
        )
        resultText?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, style = MaterialTheme.typography.bodyLarge, color = Accent)
        }
    }
}

@Composable
private fun categoryLabel(category: SpendCategory): String = stringResource(
    when (category) {
        SpendCategory.CANNABIS -> R.string.module_cannabis
        SpendCategory.TOBACCO -> R.string.module_tobacco
        SpendCategory.ALCOHOL -> R.string.module_alcohol
        SpendCategory.OTHER -> R.string.finance_category_other
    }
)

@Composable
private fun SpendRow(spend: SpendLogEntity) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = categoryLabel(SpendCategory.valueOf(spend.category)), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(text = "€%.2f".format(spend.amountEuros), style = MaterialTheme.typography.bodyLarge, color = Accent)
        }
    }
}
