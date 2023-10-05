package com.raghav.gfgffmpeg

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ControlPanelButtons(
    firstClick: () -> Unit,
    secondClick: () -> Unit,
    thirdClick: () -> Unit
) {
    Column {
        Text("Tap to add effects", style = TextStyle(color = Color.White, fontSize = 14.sp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SingleToolIcon(R.drawable.icon_effect_slow, "Slow Motion") { firstClick() }
            SingleToolIcon(R.drawable.icon_effect_time, "Reverse") { secondClick() }
            SingleToolIcon(R.drawable.icon_effect_repeatedly, "Flash") { thirdClick() }
        }
    }
}


@Composable
fun SingleToolIcon(@DrawableRes icon: Int, text: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(50.dp),
            painter = painterResource(icon),
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, style = TextStyle(color = Color.White, fontSize = 14.sp))
    }
}


@Preview(showBackground = true)
@Composable
fun ControlPanelButtonsPreview() {
    ControlPanelButtons({}, {}, {})
}