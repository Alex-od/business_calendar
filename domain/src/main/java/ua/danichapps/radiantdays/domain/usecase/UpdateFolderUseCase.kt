package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.Folder
import ua.danichapps.radiantdays.domain.model.Folder.Companion.GENERAL_NAME
import ua.danichapps.radiantdays.domain.repository.FolderRepository

class UpdateFolderUseCase(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(folder: Folder): DomainResult<Unit> {
        val trimmedName = folder.name.trim()
        if (folder.guid.isBlank() || Folder.isGeneral(folder.guid)) {
            return DomainResult.Error(
                IllegalArgumentException("Folder guid is required"),
                "Папка не найдена",
            )
        }
        if (trimmedName.equals(GENERAL_NAME, ignoreCase = true)) {
            return DomainResult.Error(
                IllegalArgumentException("Reserved folder name"),
                "Имя «$GENERAL_NAME» зарезервировано",
            )
        }
        if (trimmedName.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Folder name is required"),
                "Введите имя папки",
            )
        }
        if (repository.isFolderNameTaken(trimmedName, excludeGuid = folder.guid)) {
            return DomainResult.Error(
                IllegalArgumentException("Folder name already exists"),
                "Папка с таким именем уже существует",
            )
        }

        return repository.updateFolder(folder.copy(name = trimmedName))
    }
}
