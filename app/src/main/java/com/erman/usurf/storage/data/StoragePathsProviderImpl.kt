package com.erman.usurf.storage.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.storage.domain.StorageDirectoriesSource
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.ROOT_DIRECTORY
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The storage directories from one look for the volumes, kept for every later read: a read on a tap costs no file
 * system walk. The file system root joins them while root access is on.
 */
class StoragePathsProviderImpl(
    private val probeVolumes: () -> Set<String>,
    private val preferencesRepository: PreferencesRepository,
    rootAccessPreference: LiveData<Boolean>,
    ioDispatcher: CoroutineDispatcher
) : StoragePathsProvider,
    StorageDirectoriesSource {
    private val lock = Any()

    @Volatile
    private var volumes: Set<String>? = null

    private val volumesFound = MutableLiveData<Unit>()

    override val storageDirectories: LiveData<Set<String>> =
        MediatorLiveData<Unit>()
            .apply {
                addSource(rootAccessPreference) { value = Unit }
                addSource(volumesFound) { value = Unit }
            }.switchMap { liveData(ioDispatcher) { emit(getStorageDirectories()) } }
            .distinctUntilChanged()

    override fun getStorageDirectories(): Set<String> = withRootIfEnabled(knownVolumes())

    override fun refreshStorageDirectories(): Set<String> {
        val found: Set<String> = probeVolumes()
        val changed: Boolean = synchronized(lock) { (found != volumes).also { volumes = found } }
        if (changed) volumesFound.postValue(Unit)
        return withRootIfEnabled(found)
    }

    private fun knownVolumes(): Set<String> = volumes ?: synchronized(lock) { volumes ?: probeVolumes().also { volumes = it } }

    private fun withRootIfEnabled(volumes: Set<String>): Set<String> =
        if (preferencesRepository.getRootAccessPreference()) volumes + ROOT_DIRECTORY else volumes
}
