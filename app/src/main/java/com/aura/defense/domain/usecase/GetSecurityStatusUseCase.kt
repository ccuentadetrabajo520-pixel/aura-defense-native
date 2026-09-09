package com.aura.defense.domain.usecase

import com.aura.defense.data.model.SecurityStatus
import com.aura.defense.data.repository.SecurityRepository
import kotlinx.coroutines.flow.StateFlow

class GetSecurityStatusUseCase(
    private val repository: SecurityRepository
) {
    operator fun invoke(): StateFlow<SecurityStatus> {
        return repository.securityStatus
    }
}