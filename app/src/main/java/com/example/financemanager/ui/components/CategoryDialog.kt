package com.example.financemanager.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.financemanager.R
import com.example.financemanager.data.Category

enum class CategoryNameError { EMPTY, EXISTS }

/** Returns why [name] can't be used as a new category among [existing], or null if it can. */
fun categoryNameError(name: String, existing: List<Category>): CategoryNameError? = when {
    name.isBlank() -> CategoryNameError.EMPTY
    existing.any { it.name.equals(name.trim(), ignoreCase = true) } -> CategoryNameError.EXISTS
    else -> null
}

@Composable
fun AddCategoryDialog(
    validate: (String) -> CategoryNameError?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<CategoryNameError?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun submit() {
        error = validate(name)
        if (error == null) onConfirm(name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nova_kategorija)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text(stringResource(R.string.naziv_kategorije)) },
                isError = error != null,
                supportingText = error?.let {
                    {
                        Text(
                            stringResource(
                                when (it) {
                                    CategoryNameError.EMPTY -> R.string.error_category_name
                                    CategoryNameError.EXISTS -> R.string.error_category_exists
                                },
                            ),
                        )
                    }
                },
                singleLine = true,
                shape = FieldShape,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = ::submit) { Text(stringResource(R.string.dodaj)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) } },
    )
}
