package com.example.ui.questionmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.core.audio.SoundEffect
import com.example.core.audio.SoundManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CsvImportPreview
import com.example.data.QuestionCsvHelper
import com.example.data.repository.QuestionManagerOperatorSession

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionImportExportScreen(
    session: QuestionManagerOperatorSession,
    csvPreview: CsvImportPreview?,
    totalQuestionCount: Int,
    onAnalyzeCsv: (String) -> Unit,
    onClearPreview: () -> Unit,
    onConfirmImport: (includeDuplicates: Boolean, publishIfPermitted: Boolean) -> Unit,
    onGenerateExportCsv: () -> String
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val canDirectPublish = session.isMainAdmin || session.canPublishDirectly

    var csvInputText by remember { mutableStateOf("") }
    var includeDuplicateRows by remember { mutableStateOf(false) }
    var publishValidRowsIfPermitted by remember { mutableStateOf(false) }
    var localInfoMessage by remember { mutableStateOf<String?>(null) }

    val openCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
                csvInputText = content
                onAnalyzeCsv(content)
                localInfoMessage = "Loaded CSV file for validation preview."
            } catch (e: Exception) {
                localInfoMessage = "Could not read selected CSV file: ${e.localizedMessage}"
            }
        }
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val exported = onGenerateExportCsv()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write(exported)
                }
                localInfoMessage = "Exported $totalQuestionCount questions to CSV file."
            } catch (e: Exception) {
                localInfoMessage = "CSV export failed: ${e.localizedMessage}"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qm_import_export_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Export Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Export Question Bank to CSV ($totalQuestionCount Questions)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "Download a standard UTF-8 CSV backup (supports both English and Hindi Devanagari text) or copy the CSV data to your clipboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { saveCsvLauncher.launch("tl_brain_test_questions.csv") },
                            enabled = totalQuestionCount > 0,
                            modifier = Modifier.testTag("qm_export_csv_file_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save CSV File")
                        }
                        OutlinedButton(
                            onClick = {
                                val csv = onGenerateExportCsv()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("TL Brain Test CSV", csv))
                                localInfoMessage = "Copied $totalQuestionCount questions as CSV to clipboard."
                            },
                            enabled = totalQuestionCount > 0,
                            modifier = Modifier.testTag("qm_copy_csv_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy CSV to Clipboard")
                        }
                    }
                }
            }
        }

        // Import & Validation Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.2.dp, scheme.primary, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Bulk CSV Question Import (Safe Preview & Duplicate Protection)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "Columns: class, subject, topic, language, question_text, option_a, option_b, option_c, option_d, correct_option (A-D), explanation, difficulty, points, time_limit_seconds, image_url, status. Existing questions in Firestore are never overwritten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { openCsvLauncher.launch(arrayOf("text/*", "application/vnd.ms-excel")) },
                            modifier = Modifier.testTag("qm_pick_csv_file_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select CSV File")
                        }
                        OutlinedButton(
                            onClick = {
                                val sample = QuestionCsvHelper.sampleCsvTemplate()
                                csvInputText = sample
                                onAnalyzeCsv(sample)
                                localInfoMessage = "Loaded sample bilingual CSV format for preview."
                            },
                            modifier = Modifier.testTag("qm_load_csv_template_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Load Sample CSV Template")
                        }
                        OutlinedButton(
                            onClick = { onAnalyzeCsv(csvInputText) },
                            enabled = csvInputText.isNotBlank(),
                            modifier = Modifier.testTag("qm_validate_csv_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Validate & Preview Rows")
                        }
                    }

                    OutlinedTextField(
                        value = csvInputText,
                        onValueChange = { csvInputText = it },
                        label = { Text("Paste or Edit CSV Rows Here") },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qm_csv_text_input")
                    )

                    if (localInfoMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = scheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = localInfoMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Validation & Duplicate Preview Section
        if (csvPreview != null && csvPreview.rows.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .border(1.dp, scheme.secondary, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "CSV Validation Summary (Preview Before Saving)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Valid New Rows: ${csvPreview.validNonDuplicateRows.size}  •  Duplicate Warnings: ${csvPreview.validDuplicateRows.size}  •  Invalid Rows Blocked: ${csvPreview.invalidRows.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.secondary
                        )

                        if (csvPreview.validDuplicateRows.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = scheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = scheme.onErrorContainer)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Duplicate Warning: ${csvPreview.validDuplicateRows.size} row(s) match an existing question in Firestore or another row in this CSV. By default they are skipped so duplicates are avoided and existing questions are never overwritten.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onErrorContainer
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = includeDuplicateRows,
                                    onCheckedChange = { includeDuplicateRows = it }
                                )
                                Text(
                                    text = "Also import duplicate rows as separate new draft copies",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurface
                                )
                            }
                        }

                        if (canDirectPublish) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = publishValidRowsIfPermitted,
                                    onCheckedChange = { publishValidRowsIfPermitted = it }
                                )
                                Text(
                                    text = "Honor 'published' status column (leave unchecked to import all as safe Drafts)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurface
                                )
                            }
                        }

                        val readyCount = csvPreview.validNonDuplicateRows.size +
                            if (includeDuplicateRows) csvPreview.validDuplicateRows.size else 0

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    SoundManager.play(SoundEffect.SUCCESS)
                                    onConfirmImport(includeDuplicateRows, publishValidRowsIfPermitted)
                                },
                                enabled = readyCount > 0,
                                modifier = Modifier.testTag("qm_confirm_csv_import_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirm & Save $readyCount Valid Question(s)")
                            }
                            OutlinedButton(
                                onClick = onClearPreview,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel Preview")
                            }
                        }
                    }
                }
            }

            items(csvPreview.rows, key = { it.rowNumber }) { row ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .border(
                            1.dp,
                            when {
                                !row.isValid -> scheme.error
                                row.isDuplicateInDatabase || row.isDuplicateInFile -> scheme.secondary
                                else -> scheme.outlineVariant
                            },
                            RoundedCornerShape(14.dp)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Row #${row.rowNumber}  •  Class ${row.studentClass}  •  ${row.subject}  •  ${row.language.uppercase()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.secondary
                        )
                        Text(
                            text = row.questionText.ifBlank { "(Empty question text)" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface
                        )
                        if (!row.isValid) {
                            Text(
                                text = "Validation Errors: ${row.errors.joinToString(" ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.error
                            )
                        } else if (row.isDuplicateInDatabase || row.isDuplicateInFile) {
                            Text(
                                text = if (row.isDuplicateInDatabase) {
                                    "Duplicate Warning: Question already exists in Firestore database."
                                } else {
                                    "Duplicate Warning: Repeated row inside this CSV file."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.secondary
                            )
                        } else {
                            Text(
                                text = "Ready to Import (${row.difficulty.uppercase()} • ${row.points} pts • ${row.timerSeconds}s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
