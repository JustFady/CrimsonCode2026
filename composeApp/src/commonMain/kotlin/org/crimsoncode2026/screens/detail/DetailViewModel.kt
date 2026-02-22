package org.crimsoncode2026.screens.detail

import androidx.lifecycle.ViewModel
import org.crimsoncode2026.data.MuseumObject
import org.crimsoncode2026.data.MuseumRepository
import kotlinx.coroutines.flow.Flow

class DetailViewModel(private val museumRepository: MuseumRepository) : ViewModel() {
    fun getObject(objectId: Int): Flow<MuseumObject?> =
        museumRepository.getObjectById(objectId)
}
