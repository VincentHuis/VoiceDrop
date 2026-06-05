package com.vincent.voicedrop.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vincent.voicedrop.R
import com.vincent.voicedrop.data.ShoppingGroup
import com.vincent.voicedrop.ui.viewmodel.ShoppingViewModel
import kotlinx.coroutines.delay

/** @StringRes-label voor een winkelschap; `null` = de "Afgerond"-sectie. */
@StringRes
fun shoppingGroupTitle(group: ShoppingGroup?): Int = when (group) {
    ShoppingGroup.GROENTE_FRUIT -> R.string.group_groente_fruit
    ShoppingGroup.ZUIVEL_KOELING -> R.string.group_zuivel_koeling
    ShoppingGroup.BROOD_BAKKERIJ -> R.string.group_brood_bakkerij
    ShoppingGroup.VLEES_VIS -> R.string.group_vlees_vis
    ShoppingGroup.DIEPVRIES -> R.string.group_diepvries
    ShoppingGroup.DRINKEN -> R.string.group_drinken
    ShoppingGroup.HOUDBAAR -> R.string.group_houdbaar
    ShoppingGroup.SLIJTERIJ -> R.string.group_slijterij
    ShoppingGroup.HUISHOUD_DROGIST -> R.string.group_huishoud_drogist
    ShoppingGroup.OVERIG -> R.string.group_overig
    null -> R.string.group_done
}

/** Boodschappenlijst-inhoud, gegroepeerd per winkelschap met compacte sticky koppen. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingContent(viewModel: ShoppingViewModel = viewModel()) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.cleanupAndRefresh()
            delay(30_000)
        }
    }

    if (sections.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_groceries),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            sections.forEach { section ->
                stickyHeader(key = "h_${section.group?.name ?: "done"}") {
                    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(shoppingGroupTitle(section.group)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                }
                items(section.items, key = { it.id }) { memo ->
                    val checked = memo.checkedAt != null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setChecked(memo, !checked) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { viewModel.setChecked(memo, it) })
                        Text(
                            text = memo.text,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (checked) TextDecoration.LineThrough else null,
                            color = if (checked) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
