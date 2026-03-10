package io.androidpoet.superwall.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.androidpoet.superwall.Superwall
import io.androidpoet.superwall.compose.SuperwallGate
import io.androidpoet.superwall.models.SubscriptionStatus

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        SampleScreen()
      }
    }
  }
}

@Composable
private fun SampleScreen() {
  val subscriptionStatus by Superwall.instance.subscriptionStatus.collectAsState()

  Scaffold { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = "Superwall KMP Sample",
        style = MaterialTheme.typography.headlineMedium,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = when (subscriptionStatus) {
          is SubscriptionStatus.Active -> "Status: Active"
          is SubscriptionStatus.Inactive -> "Status: Inactive"
          is SubscriptionStatus.Unknown -> "Status: Loading..."
        },
        style = MaterialTheme.typography.bodyLarge,
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Direct placement registration
      Button(onClick = {
        Superwall.instance.register("campaign_trigger") {
          println("Feature unlocked!")
        }
      }) {
        Text("Trigger Paywall")
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Compose-native gated content
      SuperwallGate(placement = "premium_feature") {
        Text(
          text = "This is premium content!",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
