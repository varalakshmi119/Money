package com.example.money.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun EditSavingsGoalDialog(
    visible: Boolean,
    currentGoal: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    if (visible) {
        var goalAmount by remember(currentGoal) { mutableStateOf(currentGoal.toString()) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Edit Savings Goal",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = goalAmount,
                        onValueChange = { value ->
                            goalAmount = value
                            isError = false
                        },
                        label = { Text("Savings Goal Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = isError,
                        supportingText = if (isError) {
                            { Text("Please enter a valid amount") }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val amount = goalAmount.toDouble()
                            if (amount > 0) {
                                onSave(amount)
                                onDismiss()
                            } else {
                                isError = true
                            }
                        } catch (e: NumberFormatException) {
                            isError = true
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}