package io.androidpoet.superwall.sample

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.androidpoet.superwall.Superwall
import io.androidpoet.superwall.di.superwallAndroidModule
import io.androidpoet.superwall.models.SuperwallOptions
import java.lang.ref.WeakReference

class SampleApp : Application() {

  private var currentActivity: WeakReference<Activity>? = null

  override fun onCreate() {
    super.onCreate()

    // Track the current foreground activity for billing flow
    registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
      }
      override fun onActivityPaused(activity: Activity) {
        if (currentActivity?.get() == activity) {
          currentActivity = null
        }
      }
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
      override fun onActivityStarted(activity: Activity) {}
      override fun onActivityStopped(activity: Activity) {}
      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
      override fun onActivityDestroyed(activity: Activity) {}
    })

    Superwall.configure(
      apiKey = "pk_your_api_key_here",
      options = SuperwallOptions(),
      platformModule = superwallAndroidModule(
        context = applicationContext,
        activityProvider = { currentActivity?.get() },
      ),
    ) { result ->
      result.fold(
        onSuccess = { println("Superwall configured successfully") },
        onFailure = { println("Superwall configuration failed: ${it.message}") },
      )
    }
  }
}
