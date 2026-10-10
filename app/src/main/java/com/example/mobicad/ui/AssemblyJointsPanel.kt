package com.example.mobicad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun AssemblyJointsPanel(
    project: CadProject,
    lang: CadLanguage,
    onJointValueChanged: (AssemblyJoint, Double) -> Unit,
    onAddPartInstance: () -> Unit,
    onAddJoint: (JointType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .testTag("assembly_joints_panel"),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = CadSurfaceDark,
        border = BorderStroke(1.dp, CadBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = CadCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = CadStrings.get("assembly", lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Text(
                text = "Hierarchical components, spatial positioning, and kinematic joint limits",
                fontSize = 12.sp,
                color = CadTextSecondary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Add Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddPartInstance,
                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("add_assembly_part_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CadCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Part Instance", fontSize = 11.sp, color = Color.White)
                }
                Button(
                    onClick = { onAddJoint(JointType.REVOLUTE) },
                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("add_revolute_joint_btn")
                ) {
                    Text("+ Revolute Joint", fontSize = 11.sp, color = CadOrange)
                }
                Button(
                    onClick = { onAddJoint(JointType.SLIDER) },
                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("add_slider_joint_btn")
                ) {
                    Text("+ Slider Joint", fontSize = 11.sp, color = CadGreen)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Joint List with Motion Sliders
            Text(
                text = "Active Kinematic Joints (${project.assemblyJoints.size}):",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CadCyan
            )

            if (project.assemblyJoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No joints configured yet. Add a joint above to test mechanism motion.", fontSize = 12.sp, color = CadTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(project.assemblyJoints) { joint ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CadSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("joint_card_${joint.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(joint.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text(
                                        when (joint.type) {
                                            JointType.REVOLUTE -> String.format(Locale.US, "%.1f°", joint.currentValue)
                                            JointType.SLIDER -> String.format(Locale.US, "%.1f mm", joint.currentValue)
                                            else -> "Fixed"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        color = CadCyan,
                                        fontSize = 13.sp
                                    )
                                }

                                if (joint.type != JointType.FIXED) {
                                    Slider(
                                        value = joint.currentValue.toFloat(),
                                        onValueChange = { newVal ->
                                            joint.currentValue = newVal.toDouble()
                                            onJointValueChanged(joint, newVal.toDouble())
                                        },
                                        valueRange = joint.minLimit.toFloat()..joint.maxLimit.toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = CadCyan,
                                            activeTrackColor = CadCyan
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
