package io.github.petrf22.vernostnikarta.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.petrf22.vernostnikarta.R
import io.github.petrf22.vernostnikarta.barcode.CodeFormat
import io.github.petrf22.vernostnikarta.ui.contentColorOn

/** Width : height of a real plastic card (ISO/IEC 7810 ID-1, 85.6 × 54 mm). */
const val CARD_ASPECT_RATIO = 85.6f / 54f

/** The card as the cashier sees it: store color, rounded corners, the code on a white field. */
@Composable
fun LoyaltyCardView(
    name: String,
    note: String,
    code: String,
    format: CodeFormat,
    color: Color,
    modifier: Modifier = Modifier,
    onCodeClick: (() -> Unit)? = null,
) {
    val onColor = contentColorOn(color)
    Surface(
        color = color,
        contentColor = onColor,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT_RATIO),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.isNotBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            val enlarge = stringResource(R.string.enlarge_code)
            Surface(
                color = Color.White,
                contentColor = Color.Black,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (onCodeClick != null) {
                            Modifier
                                .clickable(onClick = onCodeClick)
                                .semantics { contentDescription = enlarge }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BarcodeView(
                        code = code,
                        format = format,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    if (code.isNotEmpty()) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
