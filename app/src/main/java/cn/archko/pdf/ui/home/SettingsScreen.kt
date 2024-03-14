package cn.archko.pdf.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cn.archko.pdf.R
import cn.archko.pdf.ui.settings.ListPreferenceType
import cn.archko.pdf.ui.settings.ProvidePreferenceLocals
import cn.archko.pdf.ui.settings.checkboxPreference
import cn.archko.pdf.ui.settings.footerPreference
import cn.archko.pdf.ui.settings.listPreference
import cn.archko.pdf.ui.settings.multiSelectListPreference
import cn.archko.pdf.ui.settings.preference
import cn.archko.pdf.ui.settings.radioButtonPreference
import cn.archko.pdf.ui.settings.sliderPreference
import cn.archko.pdf.ui.settings.switchPreference
import cn.archko.pdf.ui.settings.textFieldPreference
import cn.archko.pdf.ui.settings.twoTargetIconButtonPreference
import cn.archko.pdf.ui.settings.twoTargetSwitchPreference

/**
 * @author: archko 2024/3/14 :15:53
 */

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen() {
    val windowInsets = WindowInsets.safeDrawing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var context = LocalContext.current
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val context = LocalContext.current
            val appLabel =
                if (LocalView.current.isInEditMode) {
                    "Sample"
                } else {
                    context.applicationInfo.loadLabel(context.packageManager).toString()
                }
            TopAppBar(
                title = { Text(text = appLabel) },
                modifier = Modifier.fillMaxWidth(),
                windowInsets =
                windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.Transparent,
        contentColor = contentColorFor(MaterialTheme.colorScheme.background),
        contentWindowInsets = windowInsets
    ) { contentPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            preference(
                key = "preference",
                title = { Text(text = context.resources.getString(R.string.options)) },
                summary = { Text(text = "Summary") }
            )
            listPreference(
                key = "orientation",
                defaultValue = context.resources.getStringArray(R.array.opts_orientations)[7],
                values = context.resources.getStringArray(R.array.opts_orientations)
                    .asList(),
                title = { Text(text = stringResource(id = R.string.opts_orientation)) },
                summary = { Text(text = context.resources.getStringArray(R.array.opts_orientation_labels)[it.toInt()]) },
            )
            checkboxPreference(
                key = "image_ocr",
                defaultValue = true,
                title = { Text(text = stringResource(id = R.string.opts_ocr_view)) },
                summary = {
                    Text(
                        text = if (it) {
                            context.resources.getString(R.string.opts_on)
                        } else {
                            context.resources.getString(R.string.opts_off)
                        }
                    )
                }
            )
            checkboxPreference(
                key = "fullscreen",
                defaultValue = true,
                title = { Text(text = stringResource(id = R.string.opts_fullscreen)) },
                summary = {
                    Text(
                        text = if (it) {
                            context.resources.getString(R.string.opts_on)
                        } else {
                            context.resources.getString(R.string.opts_off)
                        }
                    )
                }
            )
            checkboxPreference(
                key = "autocrop",
                defaultValue = true,
                title = { Text(text = stringResource(id = R.string.opts_autocrop)) },
                summary = {
                    Text(
                        text = if (it) {
                            context.resources.getString(R.string.opts_on)
                        } else {
                            context.resources.getString(R.string.opts_off)
                        }
                    )
                }
            )
            checkboxPreference(
                key = "keepOn",
                defaultValue = true,
                title = { Text(text = stringResource(id = R.string.opts_keep_on)) },
                summary = {
                    Text(
                        text = if (it) {
                            context.resources.getString(R.string.opts_on)
                        } else {
                            context.resources.getString(R.string.opts_off)
                        }
                    )
                }
            )
            checkboxPreference(
                key = "dirsFirst",
                defaultValue = true,
                title = { Text(text = stringResource(id = R.string.opts_dirs_first)) },
                summary = {
                    Text(
                        text = if (it) {
                            context.resources.getString(R.string.opts_on)
                        } else {
                            context.resources.getString(R.string.opts_off)
                        }
                    )
                }
            )
            checkboxPreference(
                key = "showExtension",
                defaultValue = true,
                title = { Text(text = stringResource(id = R.string.opts_show_extension)) },
                summary = {
                    Text(
                        text = if (it) {
                            context.resources.getString(R.string.opts_on)
                        } else {
                            context.resources.getString(R.string.opts_off)
                        }
                    )
                }
            )

            //footerPreference(
            //    key = "footer_preference",
            //    summary = { Text(text = "Footer preference summary") }
            //)
        }
    }
}


////////================
@Composable
fun SampleApp() {
    ProvidePreferenceLocals { SampleScreen() }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SampleScreen() {
    val windowInsets = WindowInsets.safeDrawing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val context = LocalContext.current
            val appLabel =
                if (LocalView.current.isInEditMode) {
                    "Sample"
                } else {
                    context.applicationInfo.loadLabel(context.packageManager).toString()
                }
            TopAppBar(
                title = { Text(text = appLabel) },
                modifier = Modifier.fillMaxWidth(),
                windowInsets =
                windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.Transparent,
        contentColor = contentColorFor(MaterialTheme.colorScheme.background),
        contentWindowInsets = windowInsets
    ) { contentPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            preference(
                key = "preference",
                title = { Text(text = "Preference") },
                summary = { Text(text = "Summary") }
            ) {}
            checkboxPreference(
                key = "checkbox_preference",
                defaultValue = false,
                title = { Text(text = "Checkbox preference") },
                summary = { Text(text = if (it) "On" else "Off") }
            )
            switchPreference(
                key = "switch_preference",
                defaultValue = false,
                title = { Text(text = "Switch preference") },
                summary = { Text(text = if (it) "On" else "Off") }
            )
            twoTargetSwitchPreference(
                key = "two_target_switch_preference",
                defaultValue = false,
                title = { Text(text = "Two target switch preference") },
                summary = { Text(text = if (it) "On" else "Off") }
            ) {}
            twoTargetIconButtonPreference(
                key = "two_target_icon_button_preference",
                title = { Text(text = "Two target icon button preference") },
                summary = { Text(text = "Summary") },
                onClick = {},
                iconButtonIcon = {
                    Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings")
                }
            ) {}
            sliderPreference(
                key = "slider_preference",
                defaultValue = 0f,
                title = { Text(text = "Slider preference") },
                valueRange = 0f..5f,
                valueSteps = 9,
                summary = { Text(text = "Summary") },
                valueText = { Text(text = "%.1f".format(it)) }
            )
            listPreference(
                key = "list_alert_dialog_preference",
                defaultValue = "Alpha",
                values = listOf("Alpha", "Beta", "Canary"),
                title = { Text(text = "List preference (alert dialog)") },
                summary = { Text(text = it) }
            )
            listPreference(
                key = "list_dropdown_menu_preference",
                defaultValue = "Alpha",
                values = listOf("Alpha", "Beta", "Canary"),
                title = { Text(text = "List preference (dropdown menu)") },
                summary = { Text(text = it) },
                type = ListPreferenceType.DROPDOWN_MENU
            )
            multiSelectListPreference(
                key = "multi_select_list_preference",
                defaultValue = setOf("Alpha", "Beta"),
                values = listOf("Alpha", "Beta", "Canary"),
                title = { Text(text = "Multi-select list preference") },
                summary = { Text(text = it.sorted().joinToString(", ")) }
            )
            textFieldPreference(
                key = "text_field_preference",
                defaultValue = "Value",
                title = { Text(text = "Text field preference") },
                textToValue = { it },
                summary = { Text(text = it) }
            )
            radioButtonPreference(
                key = "radio_button_preference",
                selected = true,
                title = { Text(text = "Radio button preference") },
                summary = { Text(text = "Summary") }
            ) {}
            footerPreference(
                key = "footer_preference",
                summary = { Text(text = "Footer preference summary") }
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SampleAppPreview() {
    SampleApp()
}

/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel = SettingsViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = cn.archko.pdf.R.string.menu_settings),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
            )
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(it)
                .padding(16.dp)
        ) {
            SettingsGroup(name = R.string.settings_first_category) {
                SettingsSwitchComp(
                    name = R.string.settings_switch,
                    icon = R.drawable.icon,
                    iconDesc = R.string.desc,
                    state = vm.isSwitchOn.collectAsState()
                ) {
                    vm.toggleSwitch()
                }
                SettingsTextComp(
                    name = R.string.settings_title,
                    icon = R.drawable.icon,
                    iconDesc = R.string.desc,
                    state = vm.textPreference.collectAsState(),
                    onSave = { finalText -> vm.saveText(finalText) },
                    onCheck = { text -> vm.checkTextInput(text) },
                )
            }

            SettingsGroup(name = R.string.settings_second_category) {
                SettingsNumberComp(
                    name = R.string.settings_title,
                    icon = R.drawable.icon,
                    iconDesc = R.string.desc,
                    state = vm.textPreference.collectAsState(),
                    //inputFiler = { text -> filterNumbers(text) },
                    onSave = { finalText -> vm.saveNumber(finalText) },
                    onCheck = { text -> vm.checkNumber(text) },
                )
            }
        }
    }
}

@Composable
fun SettingsClickableComp(
    @DrawableRes icon: Int,
    @StringRes iconDesc: Int,
    @StringRes name: Int,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        onClick = onClick,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(id = icon),
                        contentDescription = stringResource(id = iconDesc),
                        modifier = Modifier
                            .size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = name),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.surfaceTint
                        ),
                        modifier = Modifier
                            .padding(16.dp),
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.weight(1.0f))
                //Icon(
                //    Icons.Rounded.KeyboardArrowRight,
                //    tint = MaterialTheme.colorScheme.surfaceTint,
                //    contentDescription = stringResource(id = R.string.ic_arrow_forward)
                //)
            }
            Divider()
        }

    }
}

@Composable
fun SettingsSwitchComp(
    @DrawableRes icon: Int,
    @StringRes iconDesc: Int,
    @StringRes name: Int,
    state: State<Boolean>,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        onClick = onClick,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(id = icon),
                        contentDescription = stringResource(id = iconDesc),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = name),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = state.value,
                    onCheckedChange = { onClick() }
                )
            }
            Divider()
        }
    }
}

@Composable
fun SettingsTextComp(
    @DrawableRes icon: Int,
    @StringRes iconDesc: Int,
    @StringRes name: Int,
    state: State<String>, // current value
    onSave: (String) -> Unit, // method to save the new value
    onCheck: (String) -> Boolean // check if new value is valid to save
) {

    // if the dialog is visible
    var isDialogShown by remember {
        mutableStateOf(false)
    }

    // conditional visibility in dependence to state
    if (isDialogShown) {
        Dialog(onDismissRequest = {
            // dismiss the dialog on touch outside
            isDialogShown = false
        }) {
            TextEditDialog(name, state, onSave, onCheck) {
                // to dismiss dialog from within
                isDialogShown = false
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        onClick = {
            // clicking on the preference, will show the dialog
            isDialogShown = true
        },
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    painterResource(id = icon),
                    contentDescription = stringResource(id = iconDesc),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.padding(8.dp)) {
                    // setting text title
                    Text(
                        text = stringResource(id = name),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // current value shown
                    Text(
                        text = state.value,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start,
                    )
                }
            }
            Divider()
        }
    }
}

@Composable
fun SettingsNumberComp(
    @DrawableRes icon: Int,
    @StringRes iconDesc: Int,
    @StringRes name: Int,
    state: State<String>,
    onSave: (String) -> Unit,
    //inputFilter: (String) -> String, // input filter for the preference
    onCheck: (String) -> Boolean
) {

    var isDialogShown by remember {
        mutableStateOf(false)
    }

    if (isDialogShown) {
        Dialog(onDismissRequest = { isDialogShown = isDialogShown.not() }) {
            TextEditDialog(name, state, onSave, onCheck) {
                isDialogShown = isDialogShown.not()
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        onClick = {
            isDialogShown = isDialogShown.not()
        },
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    painterResource(id = icon),
                    contentDescription = stringResource(id = iconDesc),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = stringResource(id = name),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.value,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start,
                    )
                }
            }
            Divider()
        }
    }
}

@Composable
private fun TextEditDialog(
    @StringRes name: Int,
    storedValue: State<String>,
    onSave: (String) -> Unit,
    onCheck: (String) -> Boolean,
    onDismiss: () -> Unit // internal method to dismiss dialog from within
) {

    // storage for new input
    var currentInput by remember {
        mutableStateOf(TextFieldValue(storedValue.value))
    }

    // if the input is valid - run the method for current value
    var isValid by remember {
        mutableStateOf(onCheck(storedValue.value))
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceTint
    ) {

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(id = name))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(currentInput, onValueChange = {
                // check on change, if the value is valid
                isValid = onCheck(it.text)
                currentInput = it
            })
            Row {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    // save and dismiss the dialog
                    onSave(currentInput.text)
                    onDismiss()
                    // disable / enable the button
                }, enabled = isValid) {
                    //Text(stringResource(id = R.string.next))
                    Text("next")
                }
            }
        }
    }
}

@Composable
private fun TextEditNumberDialog(
    @StringRes name: Int,
    storedValue: State<String>,
    inputFilter: (String) -> String, // filters out not needed letters
    onSave: (String) -> Unit,
    onCheck: (String) -> Boolean,
    onDismiss: () -> Unit
) {

    var currentInput by remember {
        mutableStateOf(TextFieldValue(storedValue.value))
    }

    var isValid by remember {
        mutableStateOf(onCheck(storedValue.value))
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceTint
    ) {

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(id = name))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(currentInput,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onValueChange = {
                    // filters the input and removes redundant numbers
                    val filteredText = inputFilter(it.text)
                    isValid = onCheck(filteredText)
                    currentInput = TextFieldValue(filteredText)
                })
            Row {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    onSave(currentInput.text)
                    onDismiss()
                }, enabled = isValid) {
                    //Text(stringResource(id = R.string.next))
                    Text("next")
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    @StringRes name: Int,
    // to accept only composables compatible with column
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(stringResource(id = name))
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4),
        ) {
            Column {
                content()
            }
        }
    }
}*/
