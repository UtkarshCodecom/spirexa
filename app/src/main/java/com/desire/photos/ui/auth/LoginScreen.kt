package com.desire.photos.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desire.photos.ui.findActivity
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoButton
import com.desire.photos.ui.theme.NeoSurface

@Composable
fun LoginScreen(viewModel: AuthViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neo.bg)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NeoSurface(cornerRadius = 40.dp, contentPadding = PaddingValues(22.dp)) {
            Icon(Icons.Filled.CloudUpload, null, tint = Neo.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("Photos", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = Neo.text)
        Text(
            if (ui.isSignUp) "Create your backup account" else "Sign in to back up your photos",
            style = MaterialTheme.typography.bodyMedium, color = Neo.muted,
        )

        Spacer(Modifier.height(30.dp))

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Neo.primary,
            unfocusedBorderColor = Neo.dark,
            focusedLabelColor = Neo.primary,
            unfocusedLabelColor = Neo.muted,
            focusedTextColor = Neo.text,
            unfocusedTextColor = Neo.text,
            cursorColor = Neo.primary,
        )

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true, colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true, colors = fieldColors,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        if (ui.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(ui.error!!, color = Neo.danger, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(22.dp))

        if (ui.isLoading) {
            CircularProgressIndicator(color = Neo.primary, modifier = Modifier.size(28.dp))
        } else {
            NeoButton(
                text = if (ui.isSignUp) "Create account" else "Sign in",
                onClick = { viewModel.submitEmail(email, password) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            if (ui.isSignUp) "Already have an account? Sign in" else "New here? Create an account",
            color = Neo.primary, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable(enabled = !ui.isLoading) { viewModel.toggleMode() }
                .padding(8.dp),
        )

        if (viewModel.isGoogleConfigured) {
            Spacer(Modifier.height(14.dp))
            NeoButton(
                text = "Continue with Google",
                onClick = { context.findActivity()?.let { viewModel.signInWithGoogle(it) } },
                modifier = Modifier.fillMaxWidth(),
                contentColor = Neo.text,
            )
        }
    }
}
