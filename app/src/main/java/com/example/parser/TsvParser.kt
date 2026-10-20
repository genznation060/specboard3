package com.example.parser

import com.example.data.entity.PhoneField
import com.example.data.entity.PhoneRecord
import com.example.model.SmartphoneSpecDef

data class ParsedPhoneData(
    val brand: String,
    val model: String,
    val fullName: String,
    val originalRowTsv: String,
    val fields: List<FieldData>,
    val existingDuplicatePhone: PhoneRecord? = null
)

data class FieldData(
    val columnIndex: Int,
    val fieldName: String,
    val category: String,
    val fieldValue: String
)

data class TsvDetectionResult(
    val isTsv: Boolean,
    val rowCount: Int,
    val maxColumns: Int,
    val hasHeaders: Boolean,
    val detectedHeaders: List<String>,
    val parsedPhones: List<ParsedPhoneData>,
    val warnings: List<String> = emptyList()
)

object TsvParser {

    private val COMMON_BRANDS = listOf(
        "Samsung", "Apple", "Vivo", "Oppo", "Xiaomi", "OnePlus", "Google", "Realme",
        "Motorola", "Sony", "Asus", "Huawei", "Honor", "Nothing", "Infinix", "Tecno",
        "Poco", "iQOO", "ZTE", "Nubia", "Meizu", "Lenovo", "Sharp", "Nokia", "HMD"
    )

    private val HEADER_KEYWORDS = setOf(
        "brand", "model", "phone", "name", "display", "screen", "chipset", "processor",
        "cpu", "gpu", "ram", "storage", "camera", "battery", "os", "charging", "price",
        "specs", "specifications", "dimensions", "weight", "refresh rate", "resolution"
    )

    /**
     * Determines whether the given text looks like smartphone TSV data.
     */
    fun isLikelyTsv(text: String): Boolean {
        if (!text.contains('\t')) return false
        val normalized = text.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalized.trim().lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return false
        val tabCounts = lines.map { line -> line.count { it == '\t' } }
        return tabCounts.any { it >= 1 }
    }

    /**
     * Unescape spreadsheet / Excel quotes around TSV cell text.
     */
    fun unescapeCell(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length - 1).replace("\"\"", "\"").trim()
        }
        return trimmed
    }

    /**
     * Analyzes and parses the TSV input text.
     */
    fun parseTsv(
        rawText: String,
        config: SmartphoneSpecDef.ColumnMappingConfig = SmartphoneSpecDef.ColumnMappingConfig()
    ): TsvDetectionResult {
        if (rawText.isBlank() || !rawText.contains('\t')) {
            return TsvDetectionResult(
                isTsv = false,
                rowCount = 0,
                maxColumns = 0,
                hasHeaders = false,
                detectedHeaders = emptyList(),
                parsedPhones = emptyList(),
                warnings = listOf("Input does not contain tab-separated columns.")
            )
        }

        val warnings = mutableListOf<String>()

        // Validate column config mapping
        val effectiveBrandCol = if (config.brandColumnIndex in 0..100) config.brandColumnIndex else {
            warnings.add("Brand column index ${config.brandColumnIndex} out of range; defaulting to 0.")
            0
        }
        val effectiveModelCol = if (config.modelColumnIndex in 0..100) config.modelColumnIndex else {
            warnings.add("Model column index ${config.modelColumnIndex} out of range; defaulting to 1.")
            1
        }
        val effectiveNameCol = if (config.fullNameColumnIndex in 0..100) config.fullNameColumnIndex else {
            warnings.add("Full Name column index ${config.fullNameColumnIndex} out of range; defaulting to 2.")
            2
        }

        // Normalize newlines and preserve non-empty lines
        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalized.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return TsvDetectionResult(
                isTsv = false,
                rowCount = 0,
                maxColumns = 0,
                hasHeaders = false,
                detectedHeaders = emptyList(),
                parsedPhones = emptyList(),
                warnings = listOf("Input text is empty.")
            )
        }

        // Check first line for headers
        val firstLineCells = lines.first().split('\t').map { unescapeCell(it) }
        val hasHeaders = isHeaderRow(firstLineCells)

        val headerNames = if (hasHeaders) {
            firstLineCells.mapIndexed { idx, cell ->
                cell.ifEmpty {
                    SmartphoneSpecDef.DEFAULT_39_COLUMNS.getOrNull(idx)?.name ?: "Column ${idx + 1}"
                }
            }
        } else {
            SmartphoneSpecDef.DEFAULT_39_COLUMNS.map { it.name }
        }

        if (hasHeaders) {
            warnings.add("Header row detected with ${headerNames.size} columns.")
        } else {
            warnings.add("No header row detected. Applied standard 39-column smartphone schema.")
        }

        val dataLines = if (hasHeaders) lines.drop(1) else lines
        val parsedPhones = mutableListOf<ParsedPhoneData>()
        var maxCols = firstLineCells.size

        var rowIndex = 0
        for (line in dataLines) {
            if (line.isBlank()) continue
            rowIndex++
            // Preserve empty fields exactly with split('\t')
            val rawCells = line.split('\t')
            val cells = rawCells.map { unescapeCell(it) }
            if (cells.size > maxCols) maxCols = cells.size

            if (cells.size < SmartphoneSpecDef.DEFAULT_39_COLUMNS.size) {
                warnings.add("Row $rowIndex has ${cells.size} columns (expected 39). Missing specifications preserved as blank.")
            } else if (cells.size > SmartphoneSpecDef.DEFAULT_39_COLUMNS.size) {
                warnings.add("Row $rowIndex contains ${cells.size} columns (${cells.size - SmartphoneSpecDef.DEFAULT_39_COLUMNS.size} additional specifications preserved).")
            }

            val brandFromCol = cells.getOrNull(effectiveBrandCol)?.trim().orEmpty()
            val modelFromCol = cells.getOrNull(effectiveModelCol)?.trim().orEmpty()
            val nameFromCol = cells.getOrNull(effectiveNameCol)?.trim().orEmpty()

            // Resolve Brand, Model, and Full Name
            val (resolvedBrand, resolvedModel, resolvedFullName) = resolvePhoneIdentity(
                brand = brandFromCol,
                model = modelFromCol,
                fullName = nameFromCol,
                cells = cells
            )

            if (brandFromCol.isBlank() || modelFromCol.isBlank()) {
                warnings.add("Row $rowIndex: Inferred brand/model as '$resolvedBrand $resolvedModel'.")
            }

            // Extract all fields
            val fieldList = mutableListOf<FieldData>()
            val totalColumnsToMap = maxOf(cells.size, SmartphoneSpecDef.DEFAULT_39_COLUMNS.size)

            for (colIdx in 0 until totalColumnsToMap) {
                val value = cells.getOrNull(colIdx) ?: ""
                val defaultDef = SmartphoneSpecDef.DEFAULT_39_COLUMNS.getOrNull(colIdx)
                val fieldName = headerNames.getOrNull(colIdx)
                    ?: defaultDef?.name
                    ?: "Column ${colIdx + 1}"
                val category = if (hasHeaders && (defaultDef == null || !defaultDef.name.equals(fieldName, ignoreCase = true))) {
                    guessCategory(fieldName)
                } else {
                    defaultDef?.category ?: guessCategory(fieldName)
                }

                fieldList.add(
                    FieldData(
                        columnIndex = colIdx,
                        fieldName = fieldName,
                        category = category,
                        fieldValue = value
                    )
                )
            }

            parsedPhones.add(
                ParsedPhoneData(
                    brand = resolvedBrand,
                    model = resolvedModel,
                    fullName = resolvedFullName,
                    originalRowTsv = line,
                    fields = fieldList
                )
            )
        }

        return TsvDetectionResult(
            isTsv = parsedPhones.isNotEmpty(),
            rowCount = dataLines.size,
            maxColumns = maxCols,
            hasHeaders = hasHeaders,
            detectedHeaders = headerNames,
            parsedPhones = parsedPhones,
            warnings = warnings
        )
    }

    private fun isHeaderRow(cells: List<String>): Boolean {
        if (cells.isEmpty()) return false
        val firstCell = cells.first().trim().lowercase()
        if (firstCell == "brand" || firstCell == "phone" || firstCell == "manufacturer" || firstCell == "device") {
            return true
        }
        var matchCount = 0
        for (cell in cells) {
            val lower = cell.trim().lowercase()
            if (HEADER_KEYWORDS.any { lower == it || lower == "${it}s" || lower.startsWith("$it ") || lower.endsWith(" $it") }) {
                matchCount++
            }
        }
        return matchCount >= 3
    }

    private fun resolvePhoneIdentity(
        brand: String,
        model: String,
        fullName: String,
        cells: List<String>
    ): Triple<String, String, String> {
        var finalBrand = brand
        var finalModel = model
        var finalName = fullName

        if (finalBrand.isBlank()) {
            // Try to extract known brand from full name or model
            val textToCheck = if (finalName.isNotBlank()) finalName else finalModel
            for (known in COMMON_BRANDS) {
                if (textToCheck.startsWith(known, ignoreCase = true) ||
                    textToCheck.contains(" $known ", ignoreCase = true) ||
                    textToCheck.contains(known, ignoreCase = true)
                ) {
                    finalBrand = known
                    break
                }
            }
            if (finalBrand.isBlank()) {
                finalBrand = "Unknown Brand"
            }
        }

        if (finalModel.isBlank()) {
            if (finalName.isNotBlank()) {
                finalModel = finalName.removePrefix(finalBrand).trim().ifBlank { finalName }
            } else {
                finalModel = "Unknown Model"
            }
        }

        if (finalName.isBlank()) {
            finalName = if (finalModel.startsWith(finalBrand, ignoreCase = true)) {
                finalModel
            } else {
                "$finalBrand $finalModel".trim()
            }
        }

        return Triple(finalBrand, finalModel, finalName)
    }

    fun guessCategory(fieldName: String): String {
        val lower = fieldName.lowercase()
        return when {
            lower.contains("display") || lower.contains("screen") || lower.contains("resolution") ||
                    lower.contains("refresh") || lower.contains("brightness") || lower.contains("oled") ||
                    lower.contains("amoled") || lower.contains("glass") || lower.contains("ppi") -> "DISPLAY"

            lower.contains("chipset") || lower.contains("cpu") || lower.contains("gpu") ||
                    lower.contains("processor") ||
                    Regex("\\bram\\b").containsMatchIn(lower) || lower.contains("lpddr") ||
                    lower.contains("storage") || lower.contains("sd card") || lower.contains("memory") || lower.contains("ufs") -> "PERFORMANCE"

            lower.contains("camera") || lower.contains("lens") || lower.contains("sensor") ||
                    lower.contains("video") || lower.contains("selfie") || lower.contains("telephoto") ||
                    lower.contains("ultrawide") || lower.contains("zoom") || lower.contains("ois") -> "CAMERA"

            lower.contains("battery") || lower.contains("charging") || lower.contains("mah") ||
                    lower.contains("watt") || lower.contains("wireless charge") -> "BATTERY"

            lower.contains("os") || lower.contains("android") || lower.contains("software") ||
                    lower.contains("ui") || lower.contains("firmware") -> "SOFTWARE"

            lower.contains("5g") || lower.contains("wifi") || lower.contains("wi-fi") ||
                    lower.contains("bluetooth") || lower.contains("nfc") || lower.contains("lte") ||
                    lower.contains("network") || lower.contains("sim") -> "CONNECTIVITY"

            lower.contains("fingerprint") || lower.contains("face") || lower.contains("biometric") ||
                    lower.contains("security") -> "SECURITY"

            lower.contains("dimension") || lower.contains("weight") || lower.contains("color") ||
                    lower.contains("material") || lower.contains("build") || lower.contains("ip rating") ||
                    lower.contains("water") -> "DESIGN"

            else -> "OTHER"
        }
    }
}
