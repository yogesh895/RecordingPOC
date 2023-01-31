package com.example.msgshareapp

sealed class RecordingResult<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T?) : RecordingResult<T>(data)
    class Error<T>(message: String?, data: T? = null) : RecordingResult<T>(data, message)
    class Loading<T>(data: T?) : RecordingResult<T>(data)
}
