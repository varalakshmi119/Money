package com.example.money.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.components.CategoryManagementDialog
import com.example.money.database.BudgetEntity
import com.example.money.utils.FileUtils
import com.example.money.utils.PDFUtils
import com.example.money.viewmodel.FinancialViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Settings screen for app configuration and data management
 */
@Composable
fun SettingsScreen(viewModel: FinancialViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showPdfImportDialog by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var currentPdfUri by remember { mutableStateOf<Uri?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var isJsonImporting by remember { mutableStateOf(false) }

    // File picker launcher for JSON
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isJsonImporting = true
            scope.launch {
                try {
                    val transactions = FileUtils.parseTransactionsFromJson(context, it)
                    if (transactions != null && transactions.isNotEmpty()) {
                        viewModel.importTransactions(transactions)
                        snackbarHostState.showSnackbar("Successfully imported ${transactions.size} transactions")
                    } else {
                        snackbarHostState.showSnackbar("No transactions found in the file")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error importing JSON: ${e.message}")
                } finally {
                    isJsonImporting = false
                }
            }
        }
    }
    
    // File picker launcher for PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            currentPdfUri = it
            showPdfImportDialog = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Enhanced Theme Settings Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Theme Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dark Mode Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dark Mode")
                            Switch(
                                checked = viewModel.isDarkTheme.value,
                                onCheckedChange = { isDark ->
                                    viewModel.updateThemePreferences(
                                        isDark = isDark,
                                        isDynamic = viewModel.isDynamicTheme.value
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dynamic Color Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dynamic Colors")
                            Switch(
                                checked = viewModel.isDynamicTheme.value,
                                onCheckedChange = { isDynamic ->
                                    viewModel.updateThemePreferences(
                                        isDark = viewModel.isDarkTheme.value,
                                        isDynamic = isDynamic
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enhanced Data Management Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Data Management",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Import JSON Data Button
                        Button(
                            onClick = { filePickerLauncher.launch("application/json") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isJsonImporting && !isImporting
                        ) {
                            if (isJsonImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text("Import JSON Transactions")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Import PDF Statement Button
                        Button(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isJsonImporting && !isImporting
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Import PhonePe PDF Statement")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Delete All Data Button
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Delete All Data")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Manage Categories
                        ListItem(
                            headlineContent = { Text("Manage Categories") },
                            leadingContent = { Icon(Icons.Default.Category, null) },
                            modifier = Modifier.clickable { showCategoryDialog = true }
                        )

                        CategoryManagementDialog(
                            visible = showCategoryDialog,
                            onDismiss = { showCategoryDialog = false },
                            categories = viewModel.categories.collectAsState(initial = emptyList()).value,
                            onAddCategory = { category ->
                                viewModel.insertBudget(BudgetEntity(category, 0.0))
                            },
                            onRenameCategory = { oldCat, newCat ->
                                viewModel.deleteBudgetByCategory(oldCat)
                                viewModel.insertBudget(BudgetEntity(newCat, 0.0))
                            },
                            onDeleteCategory = { category ->
                                viewModel.deleteBudgetByCategory(category)
                            }
                        )
                    }
                }
            }
        }
        
        // Delete Confirmation Dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete All Data") },
                text = { Text("Are you sure you want to delete all transactions? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAllTransactions()
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // PDF Import Dialog with phone number input (password)
        if (showPdfImportDialog) {
            AlertDialog(
                onDismissRequest = { 
                    if (!isImporting) showPdfImportDialog = false 
                },
                title = { Text("Import PhonePe PDF Statement") },
                text = { 
                    Column {
                        Text("Enter your phone number associated with PhonePe (used as PDF password)")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("10-digit phone number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            enabled = !isImporting
                        )
                        
                        if (isImporting) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Processing PDF... This may take a moment")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            currentPdfUri?.let { uri ->
                                isImporting = true
                                scope.launch {
                                    try {
                                        val transactions = PDFUtils.parsePhonePePDF(context, uri, phoneNumber)
                                        if (transactions != null && transactions.isNotEmpty()) {
                                            viewModel.importTransactions(transactions)
                                            snackbarHostState.showSnackbar("Successfully imported ${transactions.size} transactions from PDF")
                                        } else {
                                            snackbarHostState.showSnackbar("No transactions found in the PDF or incorrect password")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error parsing PDF: ${e.message}")
                                    } finally {
                                        isImporting = false
                                        showPdfImportDialog = false
                                    }
                                }
                            }
                        },
                        enabled = !isImporting && phoneNumber.isNotEmpty() && phoneNumber.length >= 10
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPdfImportDialog = false },
                        enabled = !isImporting
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
