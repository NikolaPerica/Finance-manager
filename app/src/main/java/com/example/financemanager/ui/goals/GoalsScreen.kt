package com.example.financemanager.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financemanager.R
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.budgets.BudgetBar
import com.example.financemanager.ui.theme.FinanceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: GoalsViewModel, onBack: () -> Unit, onOpen: (Long) -> Unit, onAdd: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.goals_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.natrag))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.new_goal)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "summary") {
                val summary = state.summary
                if (summary != null) {
                    SummaryCard(summary, Modifier.contentWidth())
                } else if (!state.isLoading) {
                    EmptyGoals(Modifier.contentWidth())
                }
            }
            items(state.goals, key = { it.goal.id }) { progress ->
                GoalCard(progress, onClick = { onOpen(progress.goal.id) }, modifier = Modifier.contentWidth().animateItem())
            }
        }
    }
}

private fun Modifier.contentWidth() = widthIn(max = 640.dp).fillMaxWidth()

@Composable
private fun SummaryCard(summary: GoalsSummary, modifier: Modifier) {
    val white = Color.White
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(FinanceTheme.colors.gradient))
            .padding(20.dp),
    ) {
        Text(stringResource(R.string.saved_in_goals), style = MaterialTheme.typography.bodyLarge, color = white.copy(alpha = 0.75f))
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(MoneyFormat.format(summary.saved), style = MaterialTheme.typography.headlineSmall, color = white)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.budget_of, MoneyFormat.format(summary.target)),
                style = MaterialTheme.typography.bodyLarge,
                color = white.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        BudgetBar(if (summary.target > 0) summary.saved / summary.target else 0.0, color = white, track = white.copy(alpha = 0.25f))
    }
}

@Composable
private fun EmptyGoals(modifier: Modifier) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_savings),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(R.string.goals_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
