package io.routelab.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.routelab.app.domain.analyzer.AiRouteAssistantEngine
import io.routelab.app.presentation.theme.*

@Composable
fun AiAssistantSection(
    messages: List<AiRouteAssistantEngine.AssistantMessage>,
    onAskQuestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var customQuery by remember { mutableStateOf("") }

    val presetQuestions = listOf(
        "Tanjakan paling berat di mana?",
        "Bagian mana yang harus hemat tenaga?",
        "Bagaimana strategi target waktu COT?",
        "Di mana sebaiknya saya fueling?",
        "Apakah paruh kedua lebih berat dari paruh pertama?",
        "Kalau berhenti 15 menit, bagaimana pengaruhnya?"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Asisten Rute Cerdas (AI Route Assistant)",
                    color = TextPrimaryDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Menganalisis data spesifik rute yang aktif",
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .background(ColorClimb.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "KONTEKSTUAL",
                    color = ColorClimb,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Chips Row
        Text(
            text = "Pertanyaan Rekomendasi:",
            color = TextSecondaryDark,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetQuestions.forEach { question ->
                Box(
                    modifier = Modifier
                        .background(SurfaceElevated, RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                        .clickable { onAskQuestion(question) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = question,
                        color = TextPrimaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Message dialogue list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF090E17), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                Text(
                    text = "Silakan ketuk salah satu pertanyaan di atas atau ketik pertanyaan seputar tanjakan, strategi, dan nutrisi rute ini.",
                    color = TextMutedDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            } else {
                messages.forEach { msg ->
                    if (msg.isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(AccentBlue, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .background(SurfaceElevated, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customQuery,
                onValueChange = { customQuery = it },
                placeholder = {
                    Text("Tanyakan sesuatu tentang rute...", color = TextMutedDark, fontSize = 12.sp)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlueBright,
                    unfocusedBorderColor = SurfaceElevated,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (customQuery.isNotBlank()) {
                        onAskQuestion(customQuery.trim())
                        customQuery = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text("Tanya", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
