package com.example.ime

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.text.InputType
import com.example.SpecBoardApplication
import com.example.data.AppDatabase
import com.example.data.repository.SpecRepository
import com.example.ime.ui.ImeKeyboardScreen
import com.example.ui.theme.SpecBoardTheme

class SpecBoardImeService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val repository: SpecRepository by lazy {
        (application as? SpecBoardApplication)?.repository
            ?: SpecRepository(AppDatabase.getInstance(this))
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@SpecBoardImeService)
            setViewTreeViewModelStoreOwner(this@SpecBoardImeService)
            setViewTreeSavedStateRegistryOwner(this@SpecBoardImeService)
            setContent {
                SpecBoardTheme(darkTheme = true) {
                    ImeKeyboardScreen(
                        repository = repository,
                        onInsertText = { text -> insertSpec(text) },
                        onBackspace = { handleBackspace() },
                        onEnter = { handleEnter() },
                        onSpace = { handleSpace() },
                        onSwitchIme = { switchKeyboard() },
                        onCloseIme = { requestHideSelf(0) }
                    )
                }
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        store.clear()
    }

    /**
     * Determines whether the active input field is a password or secure field.
     */
    fun isSecureField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val inputType = info.inputType
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val isPassword = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        val isTextClass = (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
        val isNumberClass = (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER
        return (isTextClass || isNumberClass) && isPassword
    }

    /**
     * Inserts text using InputConnection, returning true if committed successfully.
     */
    fun insertText(text: String): Boolean {
        val ic = currentInputConnection ?: return false
        return ic.commitText(text, 1)
    }

    /**
     * Inserts specification values into the active editor, refusing to insert into password fields.
     */
    fun insertSpec(text: String): Boolean {
        if (isSecureField(currentInputEditorInfo)) {
            return false
        }
        return insertText(text)
    }

    fun handleBackspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    fun handleEnter() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        if (info != null && (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
            if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                if (ic.performEditorAction(action)) {
                    return
                }
            }
        }
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    fun handleSpace() {
        val ic = currentInputConnection ?: return
        ic.commitText(" ", 1)
    }

    fun switchKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToPreviousInputMethod()
            } else {
                imm?.showInputMethodPicker()
            }
        } catch (_: Exception) {
            imm?.showInputMethodPicker()
        }
    }
}
