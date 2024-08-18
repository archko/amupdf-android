package cn.archko.pdf.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupMenu(
    modifier: Modifier,
    showMenu: MutableState<Boolean>,
    content: @Composable ColumnScope.() -> Unit
) {
    if (showMenu.value) {
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    onClick = { showMenu.value = false },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
        ) {
            Card(
                modifier = modifier
                    .width(140.dp)
                    .padding(8.dp)
                    .animateContentSize(),
                shape = RoundedCornerShape(4.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun MenuItem(name: String, onPalletChange: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onPalletChange)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Icon(imageVector = Icons.Filled.FiberManualRecord, tint = color, contentDescription = null)
        Text(
            text = name,
            modifier = Modifier
                .padding(2.dp)
                .fillMaxWidth(),
        )
    }
}
