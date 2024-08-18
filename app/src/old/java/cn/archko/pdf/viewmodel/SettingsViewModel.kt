//package cn.archko.pdf.viewmodel
//
//import androidx.lifecycle.ViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//
///**
// * @author: archko 2024/3/14 :16:04
// */
//class SettingsViewModel constructor() : ViewModel() {
//
//    private val _isSwitchOn: MutableStateFlow<Boolean> = MutableStateFlow(false)
//    var isSwitchOn = _isSwitchOn.asStateFlow()
//
//    private val _textPreference: MutableStateFlow<String> = MutableStateFlow("")
//    var textPreference = _textPreference.asStateFlow()
//
//    private val _intPreference: MutableStateFlow<Int> = MutableStateFlow(0)
//    var intPreference = _intPreference.asStateFlow()
//
//
//    fun toggleSwitch() {
//        _isSwitchOn.value = _isSwitchOn.value.not()
//        // here is place for permanent storage handling - switch
//    }
//
//    fun saveText(finalText: String) {
//        _textPreference.value = finalText
//        // place to store text
//    }
//
//    // just checking, if it is not empty - but you can check anything
//    fun checkTextInput(text: String) = text.isNotEmpty()
//    fun saveNumber(finalText: String) {
//
//    }
//
//    fun checkNumber(text: String): Boolean {
//        return true
//    }
//
//    companion object {
//        const val TAG = "SettingsViewModel"
//    }
//
//}