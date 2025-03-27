package com.example.money.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.*

@Composable
fun EditSavingsGoalDialog(
    visible: Boolean,
    currentGoal: Double,
    currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("INR")
    },
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