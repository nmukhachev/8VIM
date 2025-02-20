/*
 * Copyright (C) 2021 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package inc.flide.vim8.lib.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val stepHeaderPaddingVertical = 8.dp
private val stepHeaderNumberBoxSize = 40.dp
private val stepHeaderNumberBoxPaddingEnd = 16.dp
private val stepHeaderTextBoxHeight = 32.dp
private val stepHeaderTextInnerPaddingHorizontal = 16.dp
private val stepStrokeWidth = 2.dp
private val stepStrokeX =
    -(stepHeaderNumberBoxPaddingEnd + (stepHeaderNumberBoxSize / 2 - stepStrokeWidth / 2))

data class Step(
    val id: Int,
    val title: String,
    val content: @Composable StepLayoutScope.() -> Unit
)

class StepLayoutScope(
    columnScope: ColumnScope
) : ColumnScope by columnScope {
    @Composable
    fun StepText(
        text: String,
        modifier: Modifier = Modifier,
        fontStyle: FontStyle = FontStyle.Normal
    ) {
        Text(
            modifier = modifier,
            text = text,
            textAlign = TextAlign.Justify,
            fontStyle = fontStyle
        )
    }

    @Composable
    fun StepButton(
        label: String,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Button(
            modifier = modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp),
            onClick = onClick
        ) {
            Text(text = label, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

class StepState private constructor(
    private val currentAuto: MutableState<Int>,
    private val currentManual: MutableState<Int> = mutableStateOf(-1)
) {
    companion object {
        fun new(init: Int) = StepState(mutableStateOf(init))

        val Saver = Saver<StepState, ArrayList<Int>>(
            save = {
                arrayListOf(it.currentAuto.value, it.currentManual.value)
            },
            restore = {
                StepState(mutableStateOf(it[0]), mutableStateOf(it[1]))
            }
        )
    }

    fun getCurrent(): State<Int> {
        return if (currentManual.value >= 0 && currentAuto.value >= currentManual.value) {
            currentManual
        } else {
            currentAuto
        }
    }

    fun getCurrentAuto(): State<Int> = currentAuto

    fun getCurrentManual(): State<Int> = currentManual

    fun setCurrentAuto(value: Int) {
        currentAuto.value = value
    }

    fun setCurrentManual(value: Int) {
        if (currentAuto.value == value) {
            currentManual.value = -1
        } else {
            currentManual.value = value
        }
    }
}

@Composable
fun StepLayout(
    stepState: StepState,
    steps: List<Step>,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    header: @Composable StepLayoutScope.() -> Unit = { },
    footer: @Composable StepLayoutScope.() -> Unit = { }
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        val scope = StepLayoutScope(this)
        header(scope)
        for (step in steps) {
            key(step.id) {
                Step(
                    ownStepId = step.id,
                    totalSteps = steps.size,
                    stepState = stepState,
                    title = step.title,
                    primaryColor = primaryColor
                ) {
                    step.content(StepLayoutScope(this))
                }
            }
        }
        footer(scope)
    }
}

@Composable
private fun ColumnScope.Step(
    ownStepId: Int,
    totalSteps: Int,
    stepState: StepState,
    title: String,
    primaryColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val currentStepId by stepState.getCurrent()
    val autoStepId by stepState.getCurrentAuto()
    val backgroundColor = when (ownStepId) {
        currentStepId -> primaryColor
        else -> MaterialTheme.colorScheme.outline
    }
    val contentVisible = ownStepId == currentStepId
    StepHeader(
        modifier = when {
            ownStepId <= autoStepId ->
                Modifier
                    .clickable(enabled = !contentVisible) { stepState.setCurrentManual(ownStepId) }

            else -> Modifier.alpha(0.38f)
        },
        backgroundColor = backgroundColor,
        step = ownStepId,
        title = title
    )
    val animSpec = spring<Float>(stiffness = Spring.StiffnessMedium)
    val weight by animateFloatAsState(
        targetValue = if (contentVisible) 1.0f else 0.00001f,
        animationSpec = animSpec,
        label = ""
    )
    AnimatedVisibility(
        modifier = Modifier
            .fillMaxWidth()
            .weight(weight),
        visible = contentVisible,
        enter = fadeIn(animationSpec = animSpec),
        exit = fadeOut(animationSpec = animSpec)
    ) {
        val onBackground = MaterialTheme.colorScheme.onSurface
        val modifier = Modifier
            .padding(start = 56.dp)
        Box(
            modifier = if (currentStepId < totalSteps) {
                modifier.drawBehind {
                    drawLine(
                        color = onBackground,
                        start = Offset(stepStrokeX.toPx(), 0f),
                        end = Offset(stepStrokeX.toPx(), size.height),
                        strokeWidth = stepStrokeWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(
                                2.dp.toPx(),
                                10.dp.toPx()
                            )
                        ),
                        alpha = 0.38f
                    )
                }
            } else {
                modifier
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll()
                    .padding(end = 8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun StepHeader(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color = contentColorFor(backgroundColor),
    step: Int,
    title: String
) {
    Row(
        modifier = modifier
            .padding(vertical = stepHeaderPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = stepHeaderNumberBoxPaddingEnd)
                .size(stepHeaderNumberBoxSize)
                .clip(CircleShape)
                .background(backgroundColor)
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = step.toString(),
                color = contentColor
            )
        }

        Box(
            modifier = Modifier
                .height(stepHeaderTextBoxHeight)
                .weight(1.0f)
                .clip(CircleShape)
                .background(backgroundColor)
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = stepHeaderTextInnerPaddingHorizontal),
                text = title,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = contentColor
            )
        }
    }
}
