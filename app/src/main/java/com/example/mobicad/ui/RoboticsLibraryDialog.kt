package com.example.mobicad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobicad.geometry.RoboticsLibrary
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.ui.theme.*

@Composable
fun RoboticsLibraryDialog(
    lang: CadLanguage,
    onSelectComponent: (String, Map<String, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDef by remember { mutableStateOf(RoboticsLibrary.CATALOG.first()) }
    val currentParams = remember(selectedDef) { mutableStateMapOf<String, Double>().apply { putAll(selectedDef.defaultParams) } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
                .testTag("robotics_library_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = CadOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = CadStrings.get("robotics", lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "Standard mechanical robotics components with exact engineering dimensions",
                    fontSize = 11.sp,
                    color = CadTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Divider(color = CadBorder)

                // List of Components
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RoboticsLibrary.CATALOG) { item ->
                        val isSelected = (item.id == selectedDef.id)
                        Card(
                            onClick = { selectedDef = item },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CadSurfaceVariant else Color(0xFF131924)
                            ),
                            border = if (isSelected) BorderStroke(1.5.dp, CadCyan) else null,
                            modifier = Modifier.fillMaxWidth().testTag("comp_item_${item.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (lang == CadLanguage.AR) item.nameAr else item.nameEn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) CadCyan else Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (lang == CadLanguage.AR) item.descriptionAr else item.descriptionEn,
                                    fontSize = 11.sp,
                                    color = CadTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Insert Button
                Button(
                    onClick = {
                        onSelectComponent(selectedDef.id, currentParams.toMap())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("insert_robotics_component_btn")
                ) {
                    Text(
                        text = "Insert ${if (lang == CadLanguage.AR) selectedDef.nameAr else selectedDef.nameEn}",
                        color = CadBackgroundDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
