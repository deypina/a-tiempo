package ec.atiempo.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import ec.atiempo.app.R

/** Public repository. Opened in the browser only when the button is tapped; the app has no internet access. */
private const val REPO_URL = "https://github.com/deypina/a-tiempo"

/** "Acerca de A Tiempo" (docs/PRD.md F9): version, what the app is, medical disclaimer, repository. */
@Composable
fun AboutScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val version = remember { versionName(context) }

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
        Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(
            onClick = { openUrl(context, REPO_URL) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.about_repo))
        }
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
