package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.Folder
import ua.danichapps.radiantdays.domain.repository.FolderRepository

class DeleteFolderUseCase(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(guid: String): DomainResult<Unit> {
        if (guid.isBlank() || Folder.isGeneral(guid)) {
            return DomainResult.Error(
                IllegalArgumentException("Folder guid is required"),
                "Папку «${Folder.GENERAL_NAME}» нельзя удалить",
            )
        }

        return repository.deleteFolder(guid)
    }
}
