package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Support
import com.example.data.SupportType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupportDialog(
    beamLength: Double,
    onDismiss: () -> Unit,
    onAddSupport: (Support) -> Unit
) {
    var name by remember { mutableStateOf("C") }
    var selectedType by remember { mutableStateOf(SupportType.ROLLER) }
    var positionStr by remember { mutableStateOf("3.0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ajouter un appui", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de l'appui") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Support Type Selector
                Text("Type de liaison :", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                SupportType.entries.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedType == type),
                            onClick = { selectedType = type }
                        )
                        Text(
                            text = "${type.labelFr} (${when(type){
                                SupportType.ROLLER -> "Ry"
                                SupportType.PINNED -> "Rx, Ry"
                                SupportType.FIXED -> "Rx, Ry, M"
                            }})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Position x Input
                OutlinedTextField(
                    value = positionStr,
                    onValueChange = { positionStr = it },
                    label = { Text("Abscisse x (m) [0 - $beamLength]") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pos = positionStr.toDoubleOrNull()
                    if (pos == null || pos < 0 || pos > beamLength) {
                        errorMessage = "Veuillez entrer une position valide entre 0 et $beamLength m"
                    } else {
                        onAddSupport(
                            Support(
                                id = "sup_" + UUID.randomUUID().toString().take(6),
                                name = name.ifBlank { "Appui" },
                                type = selectedType,
                                position = pos
                            )
                        )
                    }
                }
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
