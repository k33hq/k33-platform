package no.arcane.platform.cms

interface CmsService {
    fun getHtml(
        entryKey: String
    ): String?

    fun check(
        entryKey: String,
        spaceId: String,
        entryId: String,
        fieldId: String,
        version: String,
    ): Boolean
}