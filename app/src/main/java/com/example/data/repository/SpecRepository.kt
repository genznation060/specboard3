package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.entity.ClipboardItem
import com.example.data.entity.PhoneField
import com.example.data.entity.PhoneRecord
import com.example.parser.FieldData
import com.example.parser.ParsedPhoneData
import com.example.parser.TsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class DuplicateAction {
    UPDATE,
    REPLACE,
    ADD_AS_NEW,
    CANCEL
}

class SpecRepository(private val database: AppDatabase) {

    private val phoneDao = database.phoneDao()
    private val fieldDao = database.phoneFieldDao()
    private val clipboardDao = database.clipboardDao()

    val allPhones: Flow<List<PhoneRecord>> = phoneDao.getAllPhones()
    val allBrands: Flow<List<String>> = phoneDao.getAllBrands()
    val favoritePhones: Flow<List<PhoneRecord>> = phoneDao.getFavoritePhones()
    val recentPhones: Flow<List<PhoneRecord>> = phoneDao.getRecentPhones()
    val allClipboard: Flow<List<ClipboardItem>> = clipboardDao.getAllItems()

    fun getPhonesByBrand(brand: String): Flow<List<PhoneRecord>> = phoneDao.getPhonesByBrand(brand)

    fun getPhoneById(id: Long): Flow<PhoneRecord?> = phoneDao.getPhoneById(id)

    fun getFieldsForPhone(phoneId: Long): Flow<List<PhoneField>> = fieldDao.getFieldsForPhone(phoneId)

    fun getFieldsByCategory(phoneId: Long, category: String): Flow<List<PhoneField>> =
        fieldDao.getFieldsByCategory(phoneId, category)

    fun searchPhones(query: String): Flow<List<PhoneRecord>> = phoneDao.searchPhones(query)

    fun searchPhonesWithSpecs(query: String): Flow<List<PhoneRecord>> {
        return if (query.isBlank()) {
            phoneDao.getAllPhones()
        } else {
            phoneDao.searchPhonesWithSpecs(query.trim())
        }
    }

    fun searchFields(query: String): Flow<List<PhoneField>> = fieldDao.searchFields(query)

    suspend fun checkDuplicate(brand: String, model: String, fullName: String): PhoneRecord? {
        return withContext(Dispatchers.IO) {
            phoneDao.findDuplicate(brand, model, fullName)
        }
    }

    suspend fun insertPhoneWithFields(
        parsed: ParsedPhoneData,
        duplicateAction: DuplicateAction = DuplicateAction.ADD_AS_NEW,
        targetPhoneId: Long? = null
    ): Long {
        return withContext(Dispatchers.IO) {
            database.withTransaction {
                val existing = targetPhoneId?.let { phoneDao.getPhoneByIdSync(it) }
                    ?: phoneDao.findDuplicate(parsed.brand, parsed.model, parsed.fullName)

                if (existing != null) {
                    when (duplicateAction) {
                        DuplicateAction.CANCEL -> return@withTransaction existing.id
                        DuplicateAction.UPDATE -> {
                            // Update existing phone record
                            val updated = existing.copy(
                                updatedAt = System.currentTimeMillis()
                            )
                            phoneDao.updatePhone(updated)

                            // Update or append fields (do not overwrite with empty values)
                            val currentFields = fieldDao.getFieldsForPhoneSync(existing.id).toMutableList()
                            for (newF in parsed.fields) {
                                val existingIndex = currentFields.indexOfFirst {
                                    it.columnIndex == newF.columnIndex || it.fieldName.equals(newF.fieldName, ignoreCase = true)
                                }
                                if (existingIndex >= 0) {
                                    if (newF.fieldValue.isNotBlank()) {
                                        val fToUpdate = currentFields[existingIndex].copy(
                                            fieldValue = newF.fieldValue,
                                            category = newF.category
                                        )
                                        fieldDao.updateField(fToUpdate)
                                    }
                                } else {
                                    fieldDao.insertFields(
                                        listOf(
                                            PhoneField(
                                                phoneId = existing.id,
                                                columnIndex = newF.columnIndex,
                                                fieldName = newF.fieldName,
                                                category = newF.category,
                                                fieldValue = newF.fieldValue
                                            )
                                        )
                                    )
                                }
                            }
                            return@withTransaction existing.id
                        }
                        DuplicateAction.REPLACE -> {
                            // Replace all fields
                            fieldDao.deleteFieldsForPhone(existing.id)
                            val updated = existing.copy(
                                brand = parsed.brand,
                                model = parsed.model,
                                fullName = parsed.fullName,
                                originalTsv = parsed.originalRowTsv,
                                updatedAt = System.currentTimeMillis()
                            )
                            phoneDao.updatePhone(updated)
                            val fieldsToInsert = parsed.fields.map {
                                PhoneField(
                                    phoneId = existing.id,
                                    columnIndex = it.columnIndex,
                                    fieldName = it.fieldName,
                                    category = it.category,
                                    fieldValue = it.fieldValue
                                )
                            }
                            fieldDao.insertFields(fieldsToInsert)
                            return@withTransaction existing.id
                        }
                        DuplicateAction.ADD_AS_NEW -> {
                            // Proceed to create a distinct new record
                        }
                    }
                }

                val newPhone = PhoneRecord(
                    brand = parsed.brand,
                    model = parsed.model,
                    fullName = parsed.fullName,
                    originalTsv = parsed.originalRowTsv
                )
                val newId = phoneDao.insertPhone(newPhone)
                val fieldsToInsert = parsed.fields.map {
                    PhoneField(
                        phoneId = newId,
                        columnIndex = it.columnIndex,
                        fieldName = it.fieldName,
                        category = it.category,
                        fieldValue = it.fieldValue
                    )
                }
                fieldDao.insertFields(fieldsToInsert)
                newId
            }
        }
    }

    suspend fun incrementPhoneUsage(phoneId: Long) {
        withContext(Dispatchers.IO) {
            phoneDao.incrementPhoneUsage(phoneId)
        }
    }

    suspend fun incrementFieldUsage(fieldId: Long) {
        withContext(Dispatchers.IO) {
            fieldDao.incrementFieldUsage(fieldId)
        }
    }

    suspend fun togglePhoneFavorite(phoneId: Long, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            phoneDao.setFavorite(phoneId, isFavorite)
        }
    }

    suspend fun togglePhonePinned(phoneId: Long, isPinned: Boolean) {
        withContext(Dispatchers.IO) {
            phoneDao.setPinned(phoneId, isPinned)
        }
    }

    suspend fun toggleFieldFavorite(fieldId: Long, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            fieldDao.setFieldFavorite(fieldId, isFavorite)
        }
    }

    suspend fun deletePhone(phoneId: Long) {
        withContext(Dispatchers.IO) {
            phoneDao.deletePhoneById(phoneId)
        }
    }

    suspend fun updateField(field: PhoneField) {
        withContext(Dispatchers.IO) {
            fieldDao.updateField(field)
        }
    }

    suspend fun addClipboardItem(text: String, isTsv: Boolean, detectedCount: Int): Long {
        return withContext(Dispatchers.IO) {
            val item = ClipboardItem(
                content = text,
                isTsv = isTsv,
                detectedPhonesCount = detectedCount
            )
            clipboardDao.insertItem(item)
        }
    }

    suspend fun deleteClipboardItem(id: Long) {
        withContext(Dispatchers.IO) {
            clipboardDao.deleteItemById(id)
        }
    }

    suspend fun toggleClipboardFavorite(id: Long, isFav: Boolean) {
        withContext(Dispatchers.IO) {
            clipboardDao.setFavorite(id, isFav)
        }
    }

    suspend fun toggleClipboardPinned(id: Long, isPinned: Boolean) {
        withContext(Dispatchers.IO) {
            clipboardDao.setPinned(id, isPinned)
        }
    }

    suspend fun clearClipboard() {
        withContext(Dispatchers.IO) {
            clipboardDao.clearAll()
        }
    }

    suspend fun clearDatabase() {
        withContext(Dispatchers.IO) {
            phoneDao.clearAllPhones()
        }
    }

    // Export to JSON
    suspend fun exportToJson(): String {
        return withContext(Dispatchers.IO) {
            val phones = phoneDao.getAllPhonesSync()
            val root = JSONObject()
            val phoneArray = JSONArray()

            for (phone in phones) {
                val pObj = JSONObject()
                pObj.put("brand", phone.brand)
                pObj.put("model", phone.model)
                pObj.put("fullName", phone.fullName)
                pObj.put("originalTsv", phone.originalTsv)
                pObj.put("createdAt", phone.createdAt)
                pObj.put("updatedAt", phone.updatedAt)
                pObj.put("usageCount", phone.usageCount)
                pObj.put("isFavorite", phone.isFavorite)

                val fields = fieldDao.getFieldsForPhoneSync(phone.id)
                val fieldArray = JSONArray()
                for (f in fields) {
                    val fObj = JSONObject()
                    fObj.put("columnIndex", f.columnIndex)
                    fObj.put("fieldName", f.fieldName)
                    fObj.put("category", f.category)
                    fObj.put("fieldValue", f.fieldValue)
                    fObj.put("usageCount", f.usageCount)
                    fObj.put("isFavorite", f.isFavorite)
                    fieldArray.put(fObj)
                }
                pObj.put("fields", fieldArray)
                phoneArray.put(pObj)
            }
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())
            root.put("phones", phoneArray)
            root.toString(2)
        }
    }

    // Export to TSV
    suspend fun exportToTsv(): String {
        return withContext(Dispatchers.IO) {
            val phones = phoneDao.getAllPhonesSync()
            val sb = StringBuilder()
            val headers = com.example.model.SmartphoneSpecDef.DEFAULT_39_COLUMNS.map { it.name }
            sb.append(headers.joinToString("\t")).append("\n")

            for (p in phones) {
                if (p.originalTsv.isNotBlank()) {
                    sb.append(p.originalTsv).append("\n")
                } else {
                    val fields = fieldDao.getFieldsForPhoneSync(p.id)
                    val row = (0..38).map { idx ->
                        fields.firstOrNull { it.columnIndex == idx }?.fieldValue.orEmpty()
                    }
                    sb.append(row.joinToString("\t")).append("\n")
                }
            }
            sb.toString()
        }
    }

    // Export to CSV
    suspend fun exportToCsv(): String {
        return withContext(Dispatchers.IO) {
            val phones = phoneDao.getAllPhonesSync()
            val sb = StringBuilder()
            val headers = com.example.model.SmartphoneSpecDef.DEFAULT_39_COLUMNS.map { "\"${it.name.replace("\"", "\"\"")}\"" }
            sb.append(headers.joinToString(",")).append("\n")

            for (p in phones) {
                val fields = fieldDao.getFieldsForPhoneSync(p.id)
                val row = (0..38).map { idx ->
                    val v = fields.firstOrNull { it.columnIndex == idx }?.fieldValue.orEmpty()
                    "\"${v.replace("\"", "\"\"")}\""
                }
                sb.append(row.joinToString(",")).append("\n")
            }
            sb.toString()
        }
    }

    // Import from JSON
    suspend fun importFromJson(jsonString: String): Int {
        return withContext(Dispatchers.IO) {
            database.withTransaction {
                val root = JSONObject(jsonString)
                val phoneArray = root.getJSONArray("phones")
                var count = 0
                for (i in 0 until phoneArray.length()) {
                    val pObj = phoneArray.getJSONObject(i)
                    val brand = pObj.getString("brand")
                    val model = pObj.getString("model")
                    val fullName = pObj.getString("fullName")
                    val originalTsv = pObj.optString("originalTsv", "")
                    val isFav = pObj.optBoolean("isFavorite", false)

                    val phone = PhoneRecord(
                        brand = brand,
                        model = model,
                        fullName = fullName,
                        originalTsv = originalTsv,
                        isFavorite = isFav
                    )
                    val id = phoneDao.insertPhone(phone)

                    val fieldsArray = pObj.optJSONArray("fields")
                    if (fieldsArray != null) {
                        val fields = mutableListOf<PhoneField>()
                        for (j in 0 until fieldsArray.length()) {
                            val fObj = fieldsArray.getJSONObject(j)
                            fields.add(
                                PhoneField(
                                    phoneId = id,
                                    columnIndex = fObj.getInt("columnIndex"),
                                    fieldName = fObj.getString("fieldName"),
                                    category = fObj.getString("category"),
                                    fieldValue = fObj.getString("fieldValue"),
                                    isFavorite = fObj.optBoolean("isFavorite", false)
                                )
                            )
                        }
                        fieldDao.insertFields(fields)
                    }
                    count++
                }
                count
            }
        }
    }

    // Seed Flagship Sample Phones
    suspend fun seedSampleFlagships() {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val count = phoneDao.getAllPhonesSync().size
                if (count > 0) return@withTransaction

                val sampleTsv = """
Samsung	Galaxy S26 Ultra	Samsung Galaxy S26 Ultra	2026, February	$1,299	162.8 x 77.6 x 8.2 mm	218 g	Titanium frame, Gorilla Armor 2 back	IP68 dust/water resistant	Dual SIM (Nano-SIM and eSIM)	Dynamic LTPO AMOLED 2X, 120Hz	6.9 inches	1440 x 3120 pixels (~500 ppi)	120 hertz	3000 nits peak	Corning Gorilla Armor 2	Android 16, One UI 8.0	Qualcomm Snapdragon 8 Elite (3 nm)	Octa-core (2x4.32 GHz Oryon)	Adreno 830	16 GB LPDDR5X	512 GB UFS 4.1	No	200 MP, f/1.7, OIS	50 MP, f/1.9, 120˚ ultrawide	50 MP, f/3.4, 5x periscope OIS	8K@30fps, 4K@120fps	12 MP, f/2.2, 4K@60fps	5000 milliamp hours	65 watt wired	25 watt wireless Qi2	4.5 watt reverse wireless	5G SA/NSA/Sub6/mmWave	Wi-Fi 7 (802.11be)	Bluetooth 5.4	Yes	Ultrasonic In-display	Stereo speakers, AKG	Titanium Black, Titanium Gray
Vivo	Vivo X300 Pro	Vivo X300 Pro	2026, January	$1,199	164.1 x 75.3 x 8.4 mm	224 g	Aluminum frame, AG frosted glass	IP68/IP69 dust/water resistant	Dual Nano-SIM	AMOLED, 1B colors, 120Hz, Zeiss	6.78 inches	1260 x 2800 pixels (~453 ppi)	120 hertz	3200 nits peak	Armor Glass Glass	Android 16, Funtouch 16	MediaTek Dimensity 9400+ (3 nm)	Octa-core (1x3.63 GHz Cortex-X925)	Immortalis-G925	16 GB LPDDR5X	512 GB UFS 4.1	No	50 MP, 1-inch Sony LYT-900, OIS	50 MP, f/2.0, 119˚ ultrawide	200 MP Zeiss APO telephoto 3.7x OIS	4K@120fps, 10-bit Log	50 MP, f/2.0, AF	5400 milliamp hours	100 watt wired	50 watt wireless	Reverse wired	5G Dual Standby	Wi-Fi 7	Bluetooth 5.4, aptX Lossless	Yes	Optical In-display	Stereo Hi-Fi speakers	Titanium, Carbon Black
Apple	iPhone 18 Pro Max	Apple iPhone 18 Pro Max	2026, September	$1,199	163.0 x 77.6 x 8.25 mm	227 g	Grade 5 Titanium frame, Ceramic Shield	IP68 water resistant (6m for 30m)	eSIM only (US) / Nano+eSIM	Super Retina XDR OLED, ProMotion	6.9 inches	1320 x 2868 pixels (~460 ppi)	120 hertz	3000 nits peak	Ceramic Shield 2	iOS 20	Apple A20 Pro (2 nm)	6-core CPU (2 performance + 4 efficiency)	6-core Apple GPU	12 GB LPDDR5X	256 GB / 512 GB / 1 TB	No	48 MP, f/1.78, sensor-shift OIS	48 MP, f/2.2, 120˚ ultrawide	48 MP, f/2.8, 5x tetraprism OIS	4K ProRes@120fps, Dolby Vision	12 MP TrueDepth with autofocus	4685 milliamp hours	35 watt wired	25 watt MagSafe Qi2	4.5 watt reverse wired	5G NR Sub-6 and mmWave	Wi-Fi 7	Bluetooth 5.3	Yes (Apple Pay)	Face ID 3D Facial Recognition	Stereo speakers with Spatial Audio	Natural Titanium, Desert Titanium
OnePlus	OnePlus 13	OnePlus 13	2025, November	$899	162.9 x 76.5 x 8.5 mm	213 g	Ceramic / Eco Leather back, aluminum	IP68/IP69 water and dust proof	Dual SIM (Nano-SIM)	LTPO 4.0 AMOLED, Dolby Vision	6.82 inches	1440 x 3168 pixels (~510 ppi)	120 hertz	4500 nits peak	Crystal Shield Glass	OxygenOS 15, Android 15	Snapdragon 8 Elite	Octa-core Oryon	Adreno 830	16 GB / 24 GB	512 GB / 1 TB	No	50 MP Sony LYT-808, OIS, Hasselblad	50 MP, f/2.0, 120˚ ultrawide	50 MP periscope 3x optical zoom	8K@30fps, 4K@60fps Dolby Vision	32 MP, f/2.4	6000 milliamp hours	100 watt SuperVOOC	50 watt AirVOOC	10 watt reverse wireless	5G Full Netcom	Wi-Fi 7	Bluetooth 5.4	Yes	Ultrasonic Fingerprint	Stereo speakers, Dolby Atmos	Midnight Ocean, Arctic Dawn
Google	Pixel 10 Pro XL	Google Pixel 10 Pro XL	2026, August	$1,099	162.8 x 76.6 x 8.5 mm	221 g	Polished aluminum frame, Matte glass	IP68 water and dust resistant	Single Nano-SIM and eSIM	Super Actua LTPO OLED, 120Hz	6.8 inches	1344 x 2992 pixels (~486 ppi)	120 hertz	3000 nits peak	Gorilla Glass Victus 2	Android 16	Google Tensor G5 (TSMC 3nm)	Octa-core custom CPU	Immortalis-G720	16 GB LPDDR5X	256 GB / 512 GB / 1 TB	No	50 MP Octa PD wide, OIS	48 MP Quad PD ultrawide, Macro	48 MP Quad PD 5x telephoto, OIS	4K@60fps, Video Boost, Night Sight	42 MP Dual PD ultrawide, 103˚	5060 milliamp hours	45 watt wired	23 watt wireless Pixel Stand	Reverse wireless share	5G Sub-6 / mmWave	Wi-Fi 7	Bluetooth 5.4	Yes	Ultrasonic In-display & Face Unlock	Stereo speakers, Spatial Audio	Obsidian, Porcelain, Hazel
""".trimIndent()

                val result = TsvParser.parseTsv(sampleTsv)
                for (phone in result.parsedPhones) {
                    val p = PhoneRecord(
                        brand = phone.brand,
                        model = phone.model,
                        fullName = phone.fullName,
                        originalTsv = phone.originalRowTsv
                    )
                    val id = phoneDao.insertPhone(p)
                    val fields = phone.fields.map {
                        PhoneField(
                            phoneId = id,
                            columnIndex = it.columnIndex,
                            fieldName = it.fieldName,
                            category = it.category,
                            fieldValue = it.fieldValue
                        )
                    }
                    fieldDao.insertFields(fields)
                }
            }
        }
    }
}
