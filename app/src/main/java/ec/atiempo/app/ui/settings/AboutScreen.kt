package ec.atiempo.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import ec.atiempo.app.R

/** Opened only when a button is tapped; the app itself has no internet access (spec §9). */
private const val FEEDBACK_EMAIL = "deykpina.proyectos@gmail.com"
private const val REPO_URL = "https://github.com/deypina/a-tiempo"
private const val INSTAGRAM_URL = "https://instagram.com/deykpina"
private const val LINKEDIN_URL = "https://www.linkedin.com/in/deykpina"
private const val X_URL = "https://x.com/deykpina"
private const val FACEBOOK_URL = "https://facebook.com/deykpina"

private val links = listOf(
    R.string.about_instagram to INSTAGRAM_URL,
    R.string.about_linkedin to LINKEDIN_URL,
    R.string.about_x to X_URL,
    R.string.about_facebook to FACEBOOK_URL,
    R.string.about_repo to REPO_URL,
)

/** "Acerca de A Tiempo" (docs/PRD.md F9): version, author, feedback by email, links, medical disclaimer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val version = remember { versionName(context) }
    var noEmailApp by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
            Icon(painter = painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.common_back))
        }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.about_credit), style = MaterialTheme.typography.bodyLarge)

        Button(
            onClick = { noEmailApp = !openFeedbackEmail(context, version) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.about_feedback))
        }
        if (noEmailApp) {
            Text(stringResource(R.string.about_no_email), style = MaterialTheme.typography.bodyLarge)
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            links.forEach { (label, url) ->
                OutlinedButton(onClick = { openUrl(context, url) }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text(stringResource(label))
                }
            }
        }

        Text(stringResource(R.string.about_disclaimer), style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Opens a draft in the phone's email app. Nothing is sent by the app: the person reads the
 * draft (with app version, Android version and phone model) and decides. False without an email app.
 */
private fun openFeedbackEmail(context: Context, version: String): Boolean {
    val subject = context.getString(R.string.about_feedback_subject)
    val phone = listOf(Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, Build.MODEL).joinToString(" ")
    val body = context.getString(R.string.about_feedback_body, version, Build.VERSION.RELEASE, phone)
    // Some email apps read the mailto query, others the extras: both carry the same text.
    val uri = "mailto:$FEEDBACK_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}".toUri()
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private fun versionName(context: Context): String {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    return info.versionName.orEmpty()
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // No browser on the phone: nothing else to do.
    }
}
