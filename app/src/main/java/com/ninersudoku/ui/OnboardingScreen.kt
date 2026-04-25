package com.ninersudoku.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninersudoku.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val drawableRes: Int,
    val title: String,
    val body: String,
    val imageAlt: String
)

private val pages = listOf(
    OnboardingPage(
        drawableRes = R.drawable.onboarding_1,
        title = "Tap a cell, then a number",
        body = "Pick any empty cell on the grid, then choose 1–9 from the pad to fill it in.",
        imageAlt = "Illustration: a hand tapping a glowing cell on a sudoku grid"
    ),
    OnboardingPage(
        drawableRes = R.drawable.onboarding_2,
        title = "Long-press for notes",
        body = "Hold a number to drop it as a small pencil note in the selected cell. Or tap Notes to switch modes.",
        imageAlt = "Illustration: a cell showing small pencil-mark note digits"
    ),
    OnboardingPage(
        drawableRes = R.drawable.onboarding_3,
        title = "Build your streak",
        body = "There's a new daily puzzle every day. Solve it daily to keep your streak alive.",
        imageAlt = "Illustration: a flame icon beside a calendar showing a streak"
    )
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        // Skip button top-right
        TextButton(
            onClick = onDone,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Text("Skip", color = cs.onBackground.copy(alpha = 0.6f))
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                val data = pages[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = data.drawableRes),
                        contentDescription = data.imageAlt,
                        modifier = Modifier.size(220.dp)
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = cs.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = data.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Page dot indicators
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) cs.primary else cs.outline.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            // Action button
            Button(
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        onDone()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) "Get started" else "Next",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
