package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Load
import com.example.data.LoadType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoadDialog(
    beamLength: Double,
    onDismiss: () -> Unit,
    onAddLoad: (Load) -> Unit
) {
    var selectedType by remember { mutableStateOf(LoadType.POINT_FORCE) }
    var x1Str by remember { mutableStateOf("3.0") }
    var x2Str by remember { mutableStateOf("6.0") }
    var magnitudeStr by remember { mutableStateOf("20.0") }
    var magnitudeEndStr by remember { mutableStateOf("20.0") }
    var angleStr by remember { mutableStateOf("90") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ajouter une charge", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Type selector
                Text("Type de charge :", style = MaterialTheme.typography.labelMedium)
                LoadType.entries.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedType == type),
                            onClick = { selectedType = type }
                        )
                        Text(type.labelFr, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Magnitude
                val unitLabel = when (selectedType) {
                    LoadType.POINT_FORCE -> "Valeur F (kN)"
                    LoadType.DISTRIBUTED_UNIFORM -> "Intensité q (kN/m)"
                    LoadType.DISTRIBUTED_TRIANGULAR -> "Intensité q1 (kN/m)"
                    LoadType.MOMENT -> "Moment C (kN·m)"
                }
                OutlinedTextField(
                    value = magnitudeStr,
                    onValueChange = { magnitudeStr = it },
                    label = { Text(unitLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // For triangular load: second magnitude
                if (selectedType == LoadType.DISTRIBUTED_TRIANGULAR) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = magnitudeEndStr,
                        onValueChange = { magnitudeEndStr = it },
                        label = { Text("Intensité q2 (kN/m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Position Start x1
                OutlinedTextField(
                    value = x1Str,
                    onValueChange = { x1Str = it },
                    label = {
                        Text(
                            if (selectedType == LoadType.DISTRIBUTED_UNIFORM || selectedType == LoadType.DISTRIBUTED_TRIANGULAR)
                                "Début x1 (m) [0 - $beamLength]"
                            else
                                "Position x (m) [0 - $beamLength]"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Position End x2 for distributed loads
                if (selectedType == LoadType.DISTRIBUTED_UNIFORM || selectedType == LoadType.DISTRIBUTED_TRIANGULAR) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = x2Str,
                        onValueChange = { x2Str = it },
                        label = { Text("Fin x2 (m) [x1 - $beamLength]") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Angle for point force
                if (selectedType == LoadType.POINT_FORCE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = angleStr,
                        onValueChange = { angleStr = it },
                        label = { Text("Angle (degrés, 90° = vertical)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                    val mag = magnitudeStr.toDoubleOrNull()
                    val x1 = x1Str.toDoubleOrNull()
                    val x2 = if (selectedType == LoadType.DISTRIBUTED_UNIFORM || selectedType == LoadType.DISTRIBUTED_TRIANGULAR) {
                        x2Str.toDoubleOrNull() ?: x1
                    } else {
                        x1
                    }
                    val magEnd = magnitudeEndStr.toDoubleOrNull() ?: mag ?: 0.0
                    val angle = angleStr.toDoubleOrNull() ?: 90.0

                    if (mag == null || x1 == null || x2 == null) {
                        errorMessage = "Veuillez renseigner des valeurs numériques valides."
                    } else if (x1 < 0 || x1 > beamLength || x2 < 0 || x2 > beamLength) {
                        errorMessage = "Positions hors de la poutre [0 - $beamLength m]."
                    } else if ((selectedType == LoadType.DISTRIBUTED_UNIFORM || selectedType == LoadType.DISTRIBUTED_TRIANGULAR) && x2 <= x1) {
                        errorMessage = "La position finale x2 doit être supérieure à x1."
                    } else {
                        onAddLoad(
                            Load(
                                id = "load_" + UUID.randomUUID().toString().take(6),
                                type = selectedType,
                                positionStart = x1,
                                positionEnd = x2,
                                magnitude = mag,
                                magnitudeEnd = magEnd,
                                angleDegrees = angle
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
