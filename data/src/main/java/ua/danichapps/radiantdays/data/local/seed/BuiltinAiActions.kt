package ua.danichapps.radiantdays.data.local.seed

import ua.danichapps.radiantdays.data.local.entity.AiActionEntity
import ua.danichapps.radiantdays.domain.model.AiAction

/** Default AI actions seeded on first launch and after a fresh DB install at v12. */
object BuiltinAiActions {
    val all: List<AiActionEntity> = listOf(
        AiActionEntity(
            guid = AiAction.BUILTIN_IMPROVE_GUID,
            name = "Улучшить текст",
            description = "Исправляет орфографию и стиль",
            prompt = "Исправь орфографию и стиль, сохрани смысл:\n{{text}}",
            isVisible = true,
            sortOrder = 0,
            isBuiltIn = true,
        ),
        AiActionEntity(
            guid = AiAction.BUILTIN_SHORTEN_GUID,
            name = "Сократить",
            description = "Краткое резюме заметки",
            prompt = "Сократи следующий текст до 1–2 предложений, сохрани смысл:\n{{text}}",
            isVisible = true,
            sortOrder = 1,
            isBuiltIn = true,
        ),
        AiActionEntity(
            guid = AiAction.BUILTIN_CHECKLIST_GUID,
            name = "Список задач",
            description = "Преобразует текст в чеклист",
            prompt = "Преобразуй следующий текст в маркированный чеклист:\n{{text}}",
            isVisible = true,
            sortOrder = 2,
            isBuiltIn = true,
        ),
    )
}
