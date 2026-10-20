package com.example.queue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QueuedItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val value: String,
    val sourcePhone: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class QueueSeparator(val displayName: String, val separator: String) {
    NEWLINE("New Line (\\n)", "\n"),
    COMMA("Comma (, )", ", "),
    TAB("Tab (\\t)", "\t"),
    SPACE("Space ( )", " "),
    BULLET("Bullet list (• )", "\n• "),
    DASH("Dash ( - )", " - "),
    CUSTOM("Custom", "")
}

object PasteQueueManager {

    private val _queue = MutableStateFlow<List<QueuedItem>>(emptyList())
    val queue: StateFlow<List<QueuedItem>> = _queue.asStateFlow()

    private val _selectedSeparator = MutableStateFlow(QueueSeparator.NEWLINE)
    val selectedSeparator: StateFlow<QueueSeparator> = _selectedSeparator.asStateFlow()

    private val _customSeparator = MutableStateFlow("")
    val customSeparator: StateFlow<String> = _customSeparator.asStateFlow()

    fun addItem(value: String, label: String = "", sourcePhone: String = "") {
        if (value.isBlank()) return
        val effectiveLabel = label.ifBlank {
            if (value.length > 25) value.take(25) + "..." else value
        }
        _queue.update { current ->
            current + QueuedItem(
                label = effectiveLabel,
                value = value,
                sourcePhone = sourcePhone
            )
        }
    }

    fun addMultiple(items: List<Pair<String, String>>, sourcePhone: String = "") {
        val newEntries = items.filter { it.second.isNotBlank() }.map { (label, value) ->
            QueuedItem(
                label = label.ifBlank { value.take(25) },
                value = value,
                sourcePhone = sourcePhone
            )
        }
        _queue.update { it + newEntries }
    }

    /**
     * Pops and returns the first item from the queue, or null if empty.
     */
    fun popNext(): QueuedItem? {
        val current = _queue.value
        if (current.isEmpty()) return null
        val next = current.first()
        _queue.update { it.drop(1) }
        return next
    }

    /**
     * Pushes an item back to the front of the queue if an insertion attempt failed.
     */
    fun pushFront(item: QueuedItem) {
        _queue.update { current -> listOf(item) + current }
    }

    /**
     * Formats all items using the active separator without clearing the queue.
     */
    fun formatAll(): String {
        val current = _queue.value
        if (current.isEmpty()) return ""
        val sep = if (_selectedSeparator.value == QueueSeparator.CUSTOM || _customSeparator.value.isNotEmpty()) {
            _customSeparator.value
        } else {
            _selectedSeparator.value.separator
        }
        return current.joinToString(separator = sep) { it.value }
    }

    /**
     * Formats all items using the active separator and clears the queue.
     */
    fun drainAll(): String {
        val text = formatAll()
        if (text.isNotEmpty()) {
            _queue.value = emptyList()
        }
        return text
    }

    fun peekNext(): QueuedItem? {
        return _queue.value.firstOrNull()
    }

    fun removeItem(id: String) {
        _queue.update { current -> current.filterNot { it.id == id } }
    }

    fun removeAt(index: Int) {
        _queue.update { current ->
            if (index in current.indices) {
                current.toMutableList().apply { removeAt(index) }
            } else current
        }
    }

    fun clear() {
        _queue.value = emptyList()
    }

    fun setSeparator(separator: QueueSeparator) {
        _selectedSeparator.value = separator
        if (separator != QueueSeparator.CUSTOM) {
            _customSeparator.value = ""
        }
    }

    fun setCustomSeparator(custom: String) {
        _selectedSeparator.value = QueueSeparator.CUSTOM
        _customSeparator.value = custom
    }
}
