package com.doomly.app.ui.components

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.doomly.app.ResultCallback
import com.doomly.app.data.AuthRepository

@Composable
fun SignInButton(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) busy = false }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Button(enabled = !busy, onClick = {
        var host = context
        while (host is ContextWrapper && host !is Activity) host = host.baseContext
        val activity = host as? Activity
        if (activity == null) { error = "Couldn't open sign-in. Try reopening Doomly." }
        else {
            busy = true; error = null
            AuthRepository.getInstance().signIn(activity, object : ResultCallback<Void> {
                override fun onSuccess(value: Void?) { busy = false; onSuccess() }
                override fun onError(message: String) { busy = false; error = "Sign-in didn't finish. Please try again." }
            })
        }
    }) { Text(if (busy) "Opening sign-in…" else "Sign in with Google") }
    error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
}
