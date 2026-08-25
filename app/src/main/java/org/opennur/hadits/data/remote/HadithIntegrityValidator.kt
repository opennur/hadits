package org.opennur.hadits.data.remote

object HadithIntegrityValidator {
    fun validPageItems(
        page: HadithPageDto,
        firstNumber: Int,
        pageSize: Int,
    ): List<HadithItemDto> {
        val lastNumber = firstNumber + pageSize - 1
        val seenNumbers = mutableSetOf<Int>()

        return page.items.orEmpty().filter { item ->
            val number = item.number ?: return@filter false
            val hasContent = !item.arab.orEmpty().isBlank() || !item.id.orEmpty().isBlank()
            val inRange = number in firstNumber..lastNumber &&
                (page.total <= 0 || number <= page.total)
            hasContent && inRange && seenNumbers.add(number)
        }
    }

    fun isValidDetail(
        detail: HadithDetailDto,
        expectedSlug: String,
        expectedNumber: Int,
    ): Boolean {
        val hasContent = !detail.arab.orEmpty().isBlank() || !detail.id.orEmpty().isBlank()
        return detail.slug == expectedSlug &&
            detail.number == expectedNumber &&
            hasContent
    }
}
