package com.kroslabs.lifecoach.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(
    isSetupMode: Boolean,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    onPinSubmit: (String) -> Unit,
    onPinSetup: (String, Boolean) -> Unit,
    onBiometricClick: () -> Unit,
    errorMessage: String?
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var enableBiometric by remember { mutableStateOf(biometricAvailable) }
    var isConfirmStep by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isSetupMode) "Set Up Your PIN" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSetupMode) {
                if (isConfirmStep) "Confirm your PIN" else "Create a PIN to secure your data"
            } else {
                "Enter your PIN to unlock"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = if (isConfirmStep) confirmPin else pin,
            onValueChange = { value ->
                if (value.length <= 6 && value.all { it.isDigit() }) {
                    if (isConfirmStep) confirmPin = value else pin = value
                }
            },
            label = { Text(if (isConfirmStep) "Confirm PIN" else "PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPin = !showPin }) {
                    Icon(
                        imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPin) "Hide PIN" else "Show PIN"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isSetupMode && biometricAvailable && !isConfirmStep) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enableBiometric,
                    onCheckedChange = { enableBiometric = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enable biometric unlock")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isSetupMode) {
                    if (isConfirmStep) {
                        if (pin == confirmPin && pin.length >= 4) {
                            onPinSetup(pin, enableBiometric)
                        }
                    } else if (pin.length >= 4) {
                        isConfirmStep = true
                    }
                } else {
                    onPinSubmit(pin)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = if (isConfirmStep) {
                confirmPin.length >= 4 && pin == confirmPin
            } else {
                pin.length >= 4
            }
        ) {
            Text(
                text = when {
                    isSetupMode && isConfirmStep -> "Set Up PIN"
                    isSetupMode -> "Continue"
                    else -> "Unlock"
                }
            )
        }

        if (!isSetupMode && biometricEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onBiometricClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Biometric")
            }
        }
    }
}
