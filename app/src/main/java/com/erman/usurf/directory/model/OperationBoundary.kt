package com.erman.usurf.directory.model

import com.erman.usurf.utils.UNKNOWN_ERROR
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The one boundary of every public operation of the directory's model classes: [block] runs on [dispatcher], and the
 * platform refusing a call, with an IOException or a SecurityException, leaves it as a FileOperationException that
 * names no error of its own and keeps the refusal as its cause.
 */
internal suspend fun <T> operationOn(dispatcher: CoroutineDispatcher, block: suspend () -> T): T = withContext(dispatcher) {
    try {
        block()
    } catch (err: IOException) {
        throw FileOperationException(err.localizedMessage ?: UNKNOWN_ERROR, cause = err)
    } catch (err: SecurityException) {
        throw FileOperationException(err.localizedMessage ?: UNKNOWN_ERROR, cause = err)
    }
}
