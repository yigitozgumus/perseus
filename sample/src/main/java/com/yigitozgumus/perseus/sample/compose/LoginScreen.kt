package com.yigitozgumus.perseus.sample.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.sample.keys.LoginKey
import org.koin.core.annotation.Single

@Single
class LoginScreenProvider : ComposeScreenProvider<LoginKey> {
    override fun canProvide(key: NavigationKey) = key is LoginKey

    @Composable
    override fun Content(key: LoginKey) {
        LoginScreen()
    }
}

@Composable
fun LoginScreen(onLoginClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Perseus",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Navigation Library",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
    }
}
