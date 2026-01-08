package com.kroslabs.lifecoach.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private const val MAX_LOGS = 500

    fun d(tag: String, message: String) {
        addLog(LogLevel.DEBUG, tag, message)
    }

    fun i(tag: String, message: String) {
        addLog(LogLevel.INFO, tag, message)
    }

    fun w(tag: String, message: String) {
        addLog(LogLevel.WARN, tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        addLog(LogLevel.ERROR, tag, fullMessage)
    }

    private fun addLog(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            formattedTime = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )

        // Also log to Android logcat
        when (level) {
            LogLevel.DEBUG -> android.util.Log.d(tag, message)
            LogLevel.INFO -> android.util.Log.i(tag, message)
            LogLevel.WARN -> android.util.Log.w(tag, message)
            LogLevel.ERROR -> android.util.Log.e(tag, message)
        }

        _logs.value = (_logs.value + entry).takeLast(MAX_LOGS)
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun getAllLogsAsText(): String {
        return _logs.value.joinToString("\n") { entry ->
            "${entry.formattedTime} [${entry.level.name}] ${entry.tag}: ${entry.message}"
        }
    }
}

data class LogEntry(
    val timestamp: Long,
    val formattedTime: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}
