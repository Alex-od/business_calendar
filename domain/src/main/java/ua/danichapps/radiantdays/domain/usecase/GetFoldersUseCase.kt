package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.repository.FolderRepository

class GetFoldersUseCase(
    private val repository: FolderRepository,
) {
    operator fun invoke() = repository.getFolders()
}
