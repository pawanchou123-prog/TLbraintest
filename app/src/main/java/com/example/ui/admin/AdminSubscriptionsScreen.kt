package com.example.ui.admin

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CurriculumCatalog
import com.example.data.model.AllowedStudent
import com.example.data.model.SubscriptionRecord
import java.text.SimpleDateFormat
import java.util.Locale

private fun parseIsoDateOrFallback(dateStr: String, fallbackMs: Long): Long {
    val clean = dateStr.trim()
    if (clean.isEmpty()) return fallbackMs
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(clean)?.time ?: fallbackMs
    } catch (_: Exception) {
        fallbackMs
    }
}

@Composable
fun AdminSubscriptionsScreen(
    students: List<AllowedStudent>,
    records: List<SubscriptionRecord>,
    onReviewRequest: (
        record: SubscriptionRecord,
        approve: Boolean,
        paymentMethod: String,
        startMs: Long,
        endMs: Long,
        notes: String
    ) -> Unit,
    onRecordDirectPayment: (
        email: String,
        name: String,
        studentClass: Int,
        method: String,
        startMs: Long,
        endMs: Long,
        refCode: String,
        notes: String
    ) -> Unit,
    onExtendOrExpire: (
        student: AllowedStudent,
        newStartMs: Long,
        newEndMs: Long,
        markActive: Boolean,
        paymentMethod: String,
        notes: String
    ) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var activeSection by remember { mutableStateOf("pending") } // pending | manual | passes | history
    var customDateStudent by remember { mutableStateOf<AllowedStudent?>(null) }

    val pendingList = remember(records) { records.filter { it.status == "pending" } }
    val nowMs = System.currentTimeMillis()
    val ninetyDaysMs = 90L * 24L * 60L * 60L * 1000L

    // Direct manual payment state
    var directEmail by remember { mutableStateOf("") }
    var directName by remember { mutableStateOf("") }
    var directClassText by remember { mutableStateOf("10") }
    var directMethod by remember { mutableStateOf("upi") }
    var directStartIso by remember { mutableStateOf(CurriculumCatalog.currentDayKey(nowMs)) }
    var directEndIso by remember { mutableStateOf(CurriculumCatalog.currentDayKey(nowMs + ninetyDaysMs)) }
    var directRef by remember { mutableStateOf("") }
    var directNotes by remember { mutableStateOf("") }

    if (customDateStudent != null) {
        val target = customDateStudent!!
        CustomSubscriptionDatesModal(
            student = target,
            onDismiss = { customDateStudent = null },
            onSaveDates = { startMs, endMs, method, notes ->
                onExtendOrExpire(target, startMs, endMs, true, method, notes)
                customDateStudent = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_subscriptions_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subscription Policy Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.5.dp, scheme.primary, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardMembership, contentDescription = null, tint = scheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subscription Management  •  Rs. 50 for 3 Months",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Manual UPI and Cash verification only. Payments are never auto-approved. Expiring a pass restricts paid access while preserving student profiles, points, and quiz history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section Filter Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeSection == "pending",
                    onClick = { activeSection = "pending" },
                    label = { Text("Pending Requests (${pendingList.size})") },
                    modifier = Modifier.testTag("sub_tab_pending")
                )
                FilterChip(
                    selected = activeSection == "manual",
                    onClick = { activeSection = "manual" },
                    label = { Text("Record UPI / Cash") },
                    modifier = Modifier.testTag("sub_tab_manual")
                )
                FilterChip(
                    selected = activeSection == "passes",
                    onClick = { activeSection = "passes" },
                    label = { Text("Extend / Expire (${students.size})") },
                    modifier = Modifier.testTag("sub_tab_passes")
                )
                FilterChip(
                    selected = activeSection == "history",
                    onClick = { activeSection = "history" },
                    label = { Text("Payment History (${records.size})") },
                    modifier = Modifier.testTag("sub_tab_history")
                )
            }
        }

        when (activeSection) {
            "pending" -> {
                if (pendingList.isEmpty()) {
                    item {
                        Text(
                            text = "No pending payment or access requests waiting for manual verification.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    items(pendingList, key = { it.id }) { req ->
                        PendingRequestReviewCard(
                            record = req,
                            onApprove = { method, startMs, endMs, notes ->
                                onReviewRequest(req, true, method, startMs, endMs, notes)
                            },
                            onReject = { notes ->
                                onReviewRequest(req, false, req.paymentMethod, 0L, 0L, notes)
                            }
                        )
                    }
                }
            }

            "manual" -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 860.dp)
                            .border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Record Verified Manual Payment (Rs. 50 / 3 Months)",
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onSurface
                            )
                            OutlinedTextField(
                                value = directEmail,
                                onValueChange = { directEmail = it },
                                label = { Text("Student Gmail Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("manual_sub_email_input")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = directName,
                                    onValueChange = { directName = it },
                                    label = { Text("Student Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(2f)
                                )
                                OutlinedTextField(
                                    value = directClassText,
                                    onValueChange = { directClassText = it },
                                    label = { Text("Class (1-12)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = directMethod == "upi",
                                    onClick = { directMethod = "upi" },
                                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                                    label = { Text("Manual UPI") }
                                )
                                FilterChip(
                                    selected = directMethod == "cash",
                                    onClick = { directMethod = "cash" },
                                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                                    label = { Text("Direct Cash") }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = directStartIso,
                                    onValueChange = { directStartIso = it },
                                    label = { Text("Start Date (YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = directEndIso,
                                    onValueChange = { directEndIso = it },
                                    label = { Text("Expiry Date (YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = directRef,
                                onValueChange = { directRef = it },
                                label = { Text("UPI Transaction ID / Receipt Ref (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = directNotes,
                                onValueChange = { directNotes = it },
                                label = { Text("Internal Verification Notes") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val sMs = parseIsoDateOrFallback(directStartIso, nowMs)
                                    val eMs = parseIsoDateOrFallback(directEndIso, nowMs + ninetyDaysMs)
                                    val cls = directClassText.toIntOrNull()?.coerceIn(1, 12) ?: 10
                                    onRecordDirectPayment(
                                        directEmail,
                                        directName,
                                        cls,
                                        directMethod,
                                        sMs,
                                        eMs,
                                        directRef,
                                        directNotes
                                    )
                                    if (directEmail.contains("@")) {
                                        directEmail = ""
                                        directName = ""
                                        directRef = ""
                                        directNotes = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("manual_sub_submit_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Approve & Activate Student Pass")
                            }
                        }
                    }
                }
            }

            "passes" -> {
                if (students.isEmpty()) {
                    item {
                        Text(
                            text = "No student accounts found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(students, key = { it.id.ifBlank { it.email } }) { s ->
                        val isValid = s.isSubscriptionCurrentlyValid(nowMs)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 860.dp)
                                .border(1.dp, scheme.outline, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = scheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${s.studentName} (${s.email})  •  Class ${s.studentClass}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = scheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Status: ${if (isValid) "ACTIVE" else "EXPIRED"}  •  Start: ${CurriculumCatalog.formatReadableDate(s.subscriptionStartMs)}  •  Expiry: ${CurriculumCatalog.formatReadableDate(s.subscriptionEndMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isValid) scheme.tertiary else scheme.error
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val baseEnd = maxOf(s.subscriptionEndMs, nowMs)
                                            onExtendOrExpire(
                                                s,
                                                s.subscriptionStartMs.takeIf { it > 0L } ?: nowMs,
                                                baseEnd + ninetyDaysMs,
                                                true,
                                                s.paymentMethod,
                                                "Extended subscription by +90 days (Rs. 50 verified)"
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Extend +90d")
                                    }
                                    OutlinedButton(
                                        onClick = { customDateStudent = s },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Custom Dates")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onExtendOrExpire(
                                                s,
                                                s.subscriptionStartMs,
                                                nowMs - 1000L,
                                                false,
                                                s.paymentMethod,
                                                "Expired subscription manually; historical student data preserved"
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.EventBusy, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Expire")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                if (records.isEmpty()) {
                    item {
                        Text(
                            text = "No payment history records in /subscription_records yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(records, key = { it.id.ifBlank { it.hashCode().toString() } }) { rec ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 860.dp)
                                .border(1.dp, scheme.outline, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = scheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${rec.studentName} (${rec.studentEmail})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = scheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = rec.status.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when (rec.status) {
                                            "approved" -> scheme.tertiary
                                            "rejected", "expired" -> scheme.error
                                            else -> scheme.secondary
                                        }
                                    )
                                }
                                Text(
                                    text = "Amount: Rs. ${rec.amountInr}  •  Method: ${rec.paymentMethod.uppercase()}  •  Ref: ${rec.referenceCode.ifBlank { "N/A" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                                if (rec.endMs > 0L) {
                                    Text(
                                        text = "Validity: ${CurriculumCatalog.formatReadableDate(rec.startMs)} to ${CurriculumCatalog.formatReadableDate(rec.endMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.secondary
                                    )
                                }
                                if (rec.verificationNotes.isNotBlank()) {
                                    Text(
                                        text = "Internal Notes: ${rec.verificationNotes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                                if (rec.verifiedByAdminEmail.isNotBlank()) {
                                    Text(
                                        text = "Verified by: ${rec.verifiedByAdminEmail}  •  ${formatAdminTimestamp(rec.updatedAt?.seconds)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.secondary
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

@Composable
private fun PendingRequestReviewCard(
    record: SubscriptionRecord,
    onApprove: (method: String, startMs: Long, endMs: Long, notes: String) -> Unit,
    onReject: (notes: String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val nowMs = System.currentTimeMillis()
    val ninetyDaysMs = 90L * 24L * 60L * 60L * 1000L

    var verifiedMethod by remember {
        mutableStateOf(if (record.paymentMethod == "cash") "cash" else "upi")
    }
    var notes by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 860.dp)
            .border(1.5.dp, scheme.tertiary, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${record.studentName}  •  Class ${record.studentClass}",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Gmail: ${record.studentEmail}  •  Submitted Mode: ${record.paymentMethod.uppercase()}  •  Ref: ${record.referenceCode.ifBlank { "None" }}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.secondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = verifiedMethod == "upi", onClick = { verifiedMethod = "upi" }, label = { Text("Verified UPI") })
                FilterChip(selected = verifiedMethod == "cash", onClick = { verifiedMethod = "cash" }, label = { Text("Verified Cash") })
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Internal Verification Notes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onApprove(verifiedMethod, nowMs, nowMs + ninetyDaysMs, notes) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Approve Verified Rs. 50")
                }
                OutlinedButton(
                    onClick = { onReject(notes) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
private fun CustomSubscriptionDatesModal(
    student: AllowedStudent,
    onDismiss: () -> Unit,
    onSaveDates: (startMs: Long, endMs: Long, method: String, notes: String) -> Unit
) {
    val nowMs = System.currentTimeMillis()
    val ninetyDaysMs = 90L * 24L * 60L * 60L * 1000L
    var startIso by remember {
        mutableStateOf(CurriculumCatalog.currentDayKey(student.subscriptionStartMs.takeIf { it > 0L } ?: nowMs))
    }
    var endIso by remember {
        mutableStateOf(CurriculumCatalog.currentDayKey(student.subscriptionEndMs.takeIf { it > nowMs } ?: (nowMs + ninetyDaysMs)))
    }
    var method by remember { mutableStateOf(if (student.paymentMethod == "cash") "cash" else "upi") }
    var notes by remember { mutableStateOf("Custom subscription validity set by Main Admin") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Custom Subscription Dates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${student.studentName} (${student.email})", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = startIso,
                    onValueChange = { startIso = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = endIso,
                    onValueChange = { endIso = it },
                    label = { Text("Expiry Date (YYYY-MM-DD)") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = method == "upi", onClick = { method = "upi" }, label = { Text("UPI") })
                    FilterChip(selected = method == "cash", onClick = { method = "cash" }, label = { Text("Cash") })
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Internal Verification Notes") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sMs = parseIsoDateOrFallback(startIso, nowMs)
                    val eMs = parseIsoDateOrFallback(endIso, nowMs + ninetyDaysMs)
                    onSaveDates(sMs, eMs, method, notes)
                }
            ) {
                Text("Save Dates")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
