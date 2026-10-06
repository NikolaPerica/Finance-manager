package com.example.financemanager.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financemanager.R
import com.example.financemanager.ui.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Corner shape shared by the text fields on the form screens. */
val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun DateField(
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes label: Int = R.string.datum,
) {
    // A read-only text field that opens the picker; the overlay catches the tap.
    Box(modifier) {
        OutlinedTextField(
            value = DateFormat.long(date),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(label)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_calendar), contentDescription = null) },
            shape = FieldShape,
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            shape = FieldShape,
            modifier = Modifier.matchParentSize().padding(top = 8.dp),
        ) {}
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    // The Material date picker works in UTC milliseconds.
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    ?.let(onConfirm) ?: onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) } },
    ) {
        DatePicker(state = pickerState, title = {
            Text(stringResource(R.string.odaberite_datum), modifier = Modifier.padding(start = 24.dp, top = 16.dp))
        })
    }
}

