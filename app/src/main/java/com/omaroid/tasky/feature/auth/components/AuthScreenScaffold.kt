package com.omaroid.tasky.feature.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.omaroid.tasky.R
import com.omaroid.tasky.core.presentation.adaptive.DeviceConfiguration
import com.omaroid.tasky.core.presentation.adaptive.currentDeviceConfiguration
import com.omaroid.tasky.core.presentation.designsystem.theme.AuthHeadlineTextStyle
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyDarkBackground
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyDarkOnSurface
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import com.omaroid.tasky.core.presentation.util.UiText

@Composable
fun AuthScreenScaffold(
    title: UiText,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (currentDeviceConfiguration()) {
        DeviceConfiguration.MOBILE_PORTRAIT ->
            MobilePortraitLayout(title.asString(), modifier, content)
        DeviceConfiguration.MOBILE_LANDSCAPE ->
            MobileLandscapeLayout(title.asString(), modifier, content)
        DeviceConfiguration.TABLET_PORTRAIT ->
            TabletPortraitLayout(title.asString(), modifier, content)
        DeviceConfiguration.TABLET_LANDSCAPE,
        DeviceConfiguration.DESKTOP ->
            TabletLandscapeLayout(title.asString(), modifier, content)
    }
}

/**
 * Mobile portrait: full-screen dark background, title at top, white rounded card
 * slides up from ~144dp from the top (top corners rounded 32dp, bottom open).
 * Card scrolls vertically if content overflows.
 */
@Composable
private fun MobilePortraitLayout(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TaskyDarkBackground),
    ) {
        Text(
            text = title,
            style = AuthHeadlineTextStyle,
            color = TaskyDarkOnSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 24.dp, end = 24.dp),
        )
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 144.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 40.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

/**
 * Mobile landscape: left pane (35%) = title centered, right pane (65%) = card that fills
 * the pane height. Auth content fits without scrolling; SpaceBetween pins fields top /
 * actions bottom to match the Figma landscape layout.
 */
@Composable
private fun MobileLandscapeLayout(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(TaskyDarkBackground),
    ) {
        Box(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = AuthHeadlineTextStyle,
                color = TaskyDarkOnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
                .padding(vertical = 12.dp, horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                content = content,
            )
        }
    }
}

/**
 * Tablet portrait: dark background, centered floating card (max 580dp wide).
 * Title sits at 20% of screen height via BoxWithConstraints.
 * Card is content-sized and scrollable.
 */
@Composable
private fun TabletPortraitLayout(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TaskyDarkBackground),
        contentAlignment = Alignment.TopCenter,
    ) {
        val topOffset = maxHeight * 0.20f
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(topOffset))
            Text(
                text = title,
                style = AuthHeadlineTextStyle,
                color = TaskyDarkOnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(40.dp))
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(32.dp),
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
        }
    }
}

/**
 * Tablet landscape / desktop: left pane (34%) = title centered, right pane (66%) =
 * centered floating card (max 580dp wide). Card is content-sized and scrollable.
 */
@Composable
private fun TabletLandscapeLayout(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(TaskyDarkBackground),
    ) {
        Box(
            modifier = Modifier
                .weight(0.34f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = AuthHeadlineTextStyle,
                color = TaskyDarkOnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight(0.66f)
                .fillMaxHeight()
                .padding(vertical = 24.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(32.dp),
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
        }
    }
}


@Preview(
    name = "AuthScreen Scaffold - Light Portrait",
    device = "spec:width=360dp,height=800dp,dpi=420",
    showBackground = true
)
@Composable
private fun AuthScreenScaffoldLightPortraitPreview() {
    AuthScreenScaffold(
        title = UiText.StringResource(R.string.login_title),
        content = {},
    )
}

@Preview(
    name = "AuthScreen Scaffold - Dark Portrait",
    device = "spec:width=360dp,height=800dp,dpi=420",
    showBackground = true
)
@Composable
private fun AuthScreenScaffoldDarkPortraitPreview() {
    TaskyTheme(darkTheme = true) {
        AuthScreenScaffold(
            title = UiText.StringResource(R.string.login_title),
            content = {},
        )
    }
}

@Preview(
    showBackground = true,
    device = "spec:parent=pixel_5, orientation=landscape"
)
@Composable
private fun AuthScreenLandscapeScaffoldPreview() {
    TaskyTheme {
        AuthScreenScaffold(
            title = UiText.DynamicString("Welcome Back!"),
            content = {
                Text("Hello")
            }
        )
    }
}

@Preview(
    name = "AuthScreen Scaffold - Mobile Landscape",
    device = "spec:width=800dp,height=360dp,dpi=320",
    showBackground = true
)
@Composable
private fun AuthScreenScaffoldLandscapePreview() {
    AuthScreenScaffold(
        title = UiText.StringResource(R.string.login_title),
        content = {},
    )
}

@Preview(
    name = "AuthScreen Scaffold - Tablet Landscape",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    showBackground = true
)
@Composable
private fun AuthScreenScaffoldTabletLandscapePreview() {
    AuthScreenScaffold(
        title = UiText.StringResource(R.string.login_title),
        content = {},
    )
}

@Preview(
    name = "AuthScreen Scaffold - Tablet Portrait",
    device = "spec:width=800dp,height=1280dp,dpi=240",
    showBackground = true
)
@Composable
private fun AuthScreenScaffoldTabletPortraitPreview() {
    AuthScreenScaffold(
        title = UiText.StringResource(R.string.login_title),
        content = {},
    )
}

@Preview(
    name = "AuthScreen Scaffold - Tablet Portrait Dark",
    device = "spec:width=800dp,height=1280dp,dpi=240",
    showBackground = true
)
@Composable
private fun AuthScreenScaffoldTabletPortraitDarkPreview() {
    TaskyTheme(darkTheme = true) {

        AuthScreenScaffold(
            title = UiText.StringResource(R.string.login_title),
            content = {},
        )
    }
}
