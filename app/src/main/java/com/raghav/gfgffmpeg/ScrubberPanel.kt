package com.raghav.gfgffmpeg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.RangeSlider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

fun Int.getTime(): String {
    val hr = this / 3600
    val rem = this % 3600
    val mn = rem / 60
    val sec = rem % 60
    return String.format("%02d", hr) + ":" + String.format(
        "%02d",
        mn
    ) + ":" + String.format("%02d", sec)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ScrubberPanel(
    modifier: Modifier = Modifier,
    lowerValue: Float,
    upperValue: Float,
    from: Float = 0f,
    to: Float = 100f,
    onValueChange: (lower: Float, upper: Float) -> Unit
) {
    var sliderPosition by remember(
        lowerValue,
        upperValue
    ) { mutableStateOf(lowerValue..upperValue) }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = lowerValue.toInt().getTime(), style = TextStyle(color = Color.White))
            Text(text = upperValue.toInt().getTime(), style = TextStyle(color = Color.White))
        }
        RangeSlider(
            value = sliderPosition,
            onValueChange = { range ->
                onValueChange(range.start, range.endInclusive)
            },
            valueRange = from..to,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScrubberPanelPreview() {
    var l by remember { mutableStateOf(0f) }
    var u by remember { mutableStateOf(10f) }
    ScrubberPanel(lowerValue = l, upperValue =  u, to = 10f) { lo, up ->
        l = lo
        u = up
    }
}

@Preview(showBackground = true)
@Composable
fun ScrubberPanelLotsPreview() {
    var l by remember { mutableStateOf(0f) }
    var u by remember { mutableStateOf(1000f) }
    ScrubberPanel(lowerValue = l, upperValue = u, to = 1000f) { lo, up ->
        l = lo
        u = up
    }
}