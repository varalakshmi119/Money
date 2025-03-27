package com.example.money.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.money.models.Transaction
import com.example.money.utils.FinancialUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Dialog for adding a new transaction
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit,
    categories: List<String>,
    existingTransaction: Transaction? = null
) {
    // Initialize with existing transaction data if provided, otherwise use defaults
    var details by remember { mutableStateOf(existingTransaction?.details ?: "") }
    var amount by remember { mutableStateOf(existingTransaction?.amount ?: "") }
    var selectedType by remember { mutableStateOf(existingTransaction?.type ?: "Debit") }
    var selectedCategory by remember { mutableStateOf(existingTransaction?.category ?: (categories.firstOrNull() ?: "Food and Dining")) }
    var dateString by remember { mutableStateOf(existingTransaction?.date ?: SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())) }
    var timeString by remember { mutableStateOf(existingTransaction?.time ?: SimpleDateFormat("hh:mm a", Locale.US).format(Date())) }
    var utrNo by remember { mutableStateOf(existingTransaction?.utrNo ?: "") }
    var accountReference by remember { mutableStateOf(existingTransaction?.accountReference ?: "") }
    
    // State for dropdowns
    var typeExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    
    // Date picker state
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Time picker state
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    
    // Auto-categorization
    var shouldAutoCategorize by remember { mutableStateOf(true) }
    
    // Validation
    val isValid = details.isNotBlank() && amount.isNotBlank() && 
                 amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
                 
    // Auto-categorize based on details if enabled
    LaunchedEffect(details, shouldAutoCategorize) {
        if (details.isNotBlank() && shouldAutoCategorize && existingTransaction == null) {
            val suggestedCategory = FinancialUtils.suggestCategoryFromDescription(details)
            if (suggestedCategory.isNotEmpty()) {
                selectedCategory = suggestedCategory
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = existingTransaction?.let { "Edit Transaction" } ?: "Add New Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Transaction details
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Transaction Details") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₹") },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Transaction type dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transaction Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        listOf("Debit", "Credit").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Category, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Date field with date picker
                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date") },
                    readOnly = true,
                    leadingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Time field with time picker
                OutlinedTextField(
                    value = timeString,
                    onValueChange = { timeString = it },
                    label = { Text("Time") },
                    readOnly = true,
                    leadingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Select Time")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val date = Date(millis)
                                    dateString = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
                                }
                                showDatePicker = false
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                
                // Time picker dialog
                if (showTimePicker) {
                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        title = { Text("Select Time") },
                        text = {
                            Column {
                                // Time picker content
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Hour picker
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                                            Icon(Icons.Default.KeyboardArrowUp, "Increase hour")
                                        }
                                        Text(
                                            text = String.format("%02d", selectedHour),
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                        IconButton(onClick = { selectedHour = if (selectedHour > 0) selectedHour - 1 else 23 }) {
                                            Icon(Icons.Default.KeyboardArrowDown, "Decrease hour")
                                        }
                                    }
                                    
                                    Text(":", style = MaterialTheme.typography.headlineMedium)
                                    
                                    // Minute picker
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(onClick = { selectedMinute = (selectedMinute + 1) % 60 }) {
                                            Icon(Icons.Default.KeyboardArrowUp, "Increase minute")
                                        }
                                        Text(
                                            text = String.format("%02d", selectedMinute),
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                        IconButton(onClick = { selectedMinute = if (selectedMinute > 0) selectedMinute - 1 else 59 }) {
                                            Icon(Icons.Default.KeyboardArrowDown, "Decrease minute")
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val calendar = Calendar.getInstance()
                                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                                calendar.set(Calendar.MINUTE, selectedMinute)
                                timeString = SimpleDateFormat("hh:mm a", Locale.US).format(calendar.time)
                                showTimePicker = false
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTimePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Additional fields
                OutlinedTextField(
                    value = utrNo,
                    onValueChange = { utrNo = it },
                    label = { Text("UTR Number (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = accountReference,
                    onValueChange = { accountReference = it },
                    label = { Text("Account Reference (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Auto-categorize switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-categorize",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = shouldAutoCategorize,
                        onCheckedChange = { shouldAutoCategorize = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // Create transaction
                            val transaction = createTransaction(
                                details = details,
                                amount = amount,
                                type = selectedType,
                                category = selectedCategory,
                                dateString = dateString,
                                timeString = timeString,
                                utrNo = utrNo,
                                accountReference = accountReference,
                                existingId = existingTransaction?.transactionId
                            )
                            onConfirm(transaction)
                            onDismiss()
                        },
                        enabled = isValid
                    ) {
                        Text(existingTransaction?.let { "Update" } ?: "Add Transaction")
                    }
                }
            }
        }
    }
}

/**
 * Helper function to create a transaction object
 */
private fun createTransaction(
    details: String,
    amount: String,
    type: String,
    category: String,
    dateString: String,
    timeString: String,
    utrNo: String = "",
    accountReference: String = "",
    existingId: String? = null
): Transaction {
    val transactionId = existingId ?: UUID.randomUUID().toString()
    val timestamp = try {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US)
        dateFormat.parse("$dateString $timeString") ?: Date()
    } catch (e: Exception) {
        Date()
    }
    
    return Transaction(
        transactionId = transactionId,
        date = dateString,
        time = timeString,
        details = details,
        utrNo = utrNo,
        accountReference = accountReference,
        type = type,
        amount = amount,
        category = category,
        timestamp = timestamp
    )
}
