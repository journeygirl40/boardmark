package com.boardmark.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardmark.app.R
import com.boardmark.app.ui.components.AddBookmarkDialog
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
private fun HelpStep(number: Int, text: String) {
    Text(
        text = "$number. $text",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun HelpStepImage(drawableRes: Int, description: String) {
    Image(
        painter = painterResource(drawableRes),
        contentDescription = description,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp)),
    )
}

@Composable
private fun TocEntry(number: Int, title: String, onClick: () -> Unit) {
    Text(
        text = "$number. $title",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit, viewModel: HelpViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var shareSectionY by remember { mutableIntStateOf(0) }
    var pasteSectionY by remember { mutableIntStateOf(0) }
    var addBookmarkDialogVisible by remember { mutableStateOf(false) }

    fun scrollToSection(y: Int) {
        coroutineScope.launch { scrollState.animateScrollTo(y) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // 最初に目次を見せ、タップでそのセクションへスクロールする。
            Text(stringResource(R.string.help_toc_title), style = MaterialTheme.typography.titleMedium)
            TocEntry(1, stringResource(R.string.help_share_section_title)) { scrollToSection(shareSectionY) }
            TocEntry(2, stringResource(R.string.help_paste_section_title)) { scrollToSection(pasteSectionY) }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Column(modifier = Modifier.onGloballyPositioned { shareSectionY = it.positionInParent().y.roundToInt() }) {
                Text(
                    "1. " + stringResource(R.string.help_share_section_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                HelpStep(1, stringResource(R.string.help_share_step1))
                HelpStepImage(R.drawable.help_share_step1, stringResource(R.string.help_share_step1))
                HelpStep(2, stringResource(R.string.help_share_step2))
                HelpStepImage(R.drawable.help_share_step2, stringResource(R.string.help_share_step2))
                HelpStep(3, stringResource(R.string.help_share_step3))
                HelpStepImage(R.drawable.help_share_step3, stringResource(R.string.help_share_step3))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Column(modifier = Modifier.onGloballyPositioned { pasteSectionY = it.positionInParent().y.roundToInt() }) {
                Text(
                    "2. " + stringResource(R.string.help_paste_section_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.help_paste_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                Button(onClick = { addBookmarkDialogVisible = true }) {
                    Text(stringResource(R.string.help_paste_open_dialog_action))
                }
            }
        }
    }

    if (addBookmarkDialogVisible) {
        AddBookmarkDialog(
            onConfirm = { url ->
                addBookmarkDialogVisible = false
                coroutineScope.launch {
                    val added = viewModel.addBookmarkFromUrl(url)
                    val messageRes = if (added) {
                        R.string.toast_bookmark_added
                    } else {
                        R.string.toast_bookmark_already_exists
                    }
                    Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { addBookmarkDialogVisible = false },
        )
    }
}
