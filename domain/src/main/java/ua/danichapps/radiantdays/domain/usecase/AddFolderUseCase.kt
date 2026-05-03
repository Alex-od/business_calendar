package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.FolderRepository

class AddFolderUseCase(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(name: String): DomainResult<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Folder name is required"),
                "Введите имя папки",
            )
        }
        if (repository.isFolderNameTaken(trimmedName)) {
            return DomainResult.Error(
                IllegalArgumentException("Folder name already exists"),
                "Папка с таким именем уже существует",
            )
        }

        return repository.addFolder(trimmedName)
    }
}
