package com.raghav.gfgffmpeg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TopBar(
    selectClick: () -> Unit,
    saveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.Transparent),
        horizontalArrangement = Arrangement.SpaceEvenly

    ) {
        Button(onClick = {
            selectClick()
        }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Video")
        }
        Button(onClick = {
            saveClick()
        }) {
            Icon(Icons.Default.ThumbUp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    TopBar({}) {}
}