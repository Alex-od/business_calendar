package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.FolderRepository

class DeleteFolderUseCase(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(guid: String): DomainResult<Unit> {
        if (guid.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Folder guid is required"),
                "Папка не найдена",
            )
        }

        return repository.deleteFolder(guid)
    }
}
