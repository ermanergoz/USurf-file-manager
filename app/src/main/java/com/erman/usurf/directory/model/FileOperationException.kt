package com.erman.usurf.directory.model

class FileOperationException(message: String? = null, val error: OperationError? = null) : Exception(message)
