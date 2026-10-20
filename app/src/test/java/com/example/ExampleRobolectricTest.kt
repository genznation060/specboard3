package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.entity.ClipboardItem
import com.example.data.entity.PhoneField
import com.example.data.entity.PhoneRecord
import com.example.data.repository.DuplicateAction
import com.example.data.repository.SpecRepository
import com.example.parser.FieldData
import com.example.parser.ParsedPhoneData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SpecRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SpecRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testStringResourcesAndIdentity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("SpecBoard", context.getString(R.string.app_name))
        assertEquals("SpecBoard Keyboard", context.getString(R.string.ime_name))
    }

    @Test
    fun testInsertAndSearchPhoneWithSpecifications() = runBlocking {
        val parsedPhone = ParsedPhoneData(
            brand = "Google",
            model = "Pixel 10 Pro",
            fullName = "Google Pixel 10 Pro",
            originalRowTsv = "Google\tPixel 10 Pro\tGoogle Pixel 10 Pro",
            fields = listOf(
                FieldData(0, "Brand", "OTHER", "Google"),
                FieldData(1, "Model", "OTHER", "Pixel 10 Pro"),
                FieldData(2, "Full Name", "OTHER", "Google Pixel 10 Pro"),
                FieldData(17, "Chipset", "PERFORMANCE", "Google Tensor G5 (3nm)"),
                FieldData(13, "Refresh Rate", "DISPLAY", "120 hertz")
            )
        )

        val id = repository.insertPhoneWithFields(parsedPhone)
        assertTrue(id > 0)

        // Query by phone model
        val phonesByModel = repository.searchPhonesWithSpecs("Pixel 10").first()
        assertEquals(1, phonesByModel.size)
        assertEquals("Google Pixel 10 Pro", phonesByModel.first().fullName)

        // Query by specification value ("Tensor G5")
        val phonesByChipset = repository.searchPhonesWithSpecs("Tensor G5").first()
        assertEquals(1, phonesByChipset.size)
        assertEquals("Pixel 10 Pro", phonesByChipset.first().model)

        // Query by specification field name ("Refresh Rate")
        val phonesByField = repository.searchPhonesWithSpecs("Refresh Rate").first()
        assertEquals(1, phonesByField.size)
    }

    @Test
    fun testDuplicateResolutionUpdate() = runBlocking {
        val initialPhone = ParsedPhoneData(
            brand = "Vivo",
            model = "X300",
            fullName = "Vivo X300",
            originalRowTsv = "row1",
            fields = listOf(
                FieldData(0, "Brand", "OTHER", "Vivo"),
                FieldData(1, "Model", "OTHER", "X300"),
                FieldData(10, "Display Type", "DISPLAY", "AMOLED 90Hz"),
                FieldData(28, "Battery Capacity", "BATTERY", "5000 mAh")
            )
        )
        val originalId = repository.insertPhoneWithFields(initialPhone)

        // Incoming duplicate with updated display (120Hz) and a blank battery (should not erase battery)
        val duplicateUpdate = ParsedPhoneData(
            brand = "Vivo",
            model = "X300",
            fullName = "Vivo X300",
            originalRowTsv = "row2",
            fields = listOf(
                FieldData(0, "Brand", "OTHER", "Vivo"),
                FieldData(1, "Model", "OTHER", "X300"),
                FieldData(10, "Display Type", "DISPLAY", "AMOLED 120Hz"),
                FieldData(28, "Battery Capacity", "BATTERY", ""), // Blank: should retain 5000 mAh
                FieldData(33, "Wi-Fi Standard", "CONNECTIVITY", "Wi-Fi 7") // Newly appended
            )
        )

        val resultId = repository.insertPhoneWithFields(
            parsed = duplicateUpdate,
            duplicateAction = DuplicateAction.UPDATE,
            targetPhoneId = originalId
        )
        assertEquals(originalId, resultId)

        val fields = repository.getFieldsForPhone(originalId).first()
        val displayField = fields.first { it.columnIndex == 10 }
        assertEquals("AMOLED 120Hz", displayField.fieldValue)

        val batteryField = fields.first { it.columnIndex == 28 }
        assertEquals("5000 mAh", batteryField.fieldValue)

        val wifiField = fields.firstOrNull { it.columnIndex == 33 }
        assertNotNull(wifiField)
        assertEquals("Wi-Fi 7", wifiField?.fieldValue)
    }

    @Test
    fun testDuplicateResolutionReplace() = runBlocking {
        val initialPhone = ParsedPhoneData(
            brand = "Xiaomi",
            model = "15",
            fullName = "Xiaomi 15",
            originalRowTsv = "old_tsv",
            fields = listOf(
                FieldData(0, "Brand", "OTHER", "Xiaomi"),
                FieldData(1, "Model", "OTHER", "15"),
                FieldData(10, "Display", "DISPLAY", "Old Display")
            )
        )
        val originalId = repository.insertPhoneWithFields(initialPhone)

        val replaceData = ParsedPhoneData(
            brand = "Xiaomi",
            model = "15 Pro",
            fullName = "Xiaomi 15 Pro",
            originalRowTsv = "new_tsv",
            fields = listOf(
                FieldData(0, "Brand", "OTHER", "Xiaomi"),
                FieldData(1, "Model", "OTHER", "15 Pro"),
                FieldData(10, "Display", "DISPLAY", "New LTPO 2K")
            )
        )

        val resultId = repository.insertPhoneWithFields(
            parsed = replaceData,
            duplicateAction = DuplicateAction.REPLACE,
            targetPhoneId = originalId
        )
        assertEquals(originalId, resultId)

        val phone = repository.getPhoneById(originalId).first()
        assertEquals("Xiaomi 15 Pro", phone?.fullName)
        assertEquals("new_tsv", phone?.originalTsv)

        val fields = repository.getFieldsForPhone(originalId).first()
        assertEquals(3, fields.size)
        assertEquals("New LTPO 2K", fields.first { it.columnIndex == 10 }.fieldValue)
    }

    @Test
    fun testCascadingDeletion() = runBlocking {
        val phoneData = ParsedPhoneData(
            brand = "Oppo",
            model = "Find X8",
            fullName = "Oppo Find X8",
            originalRowTsv = "tsv",
            fields = listOf(
                FieldData(0, "Brand", "OTHER", "Oppo"),
                FieldData(1, "Model", "OTHER", "Find X8")
            )
        )
        val id = repository.insertPhoneWithFields(phoneData)
        val fieldsBefore = repository.getFieldsForPhone(id).first()
        assertEquals(2, fieldsBefore.size)

        repository.deletePhone(id)

        val phoneAfter = repository.getPhoneById(id).first()
        assertNull(phoneAfter)

        val fieldsAfter = repository.getFieldsForPhone(id).first()
        assertEquals(0, fieldsAfter.size)
    }

    @Test
    fun testJsonBackupAndRestore() = runBlocking {
        val phoneData = ParsedPhoneData(
            brand = "Sony",
            model = "Xperia 1 VII",
            fullName = "Sony Xperia 1 VII",
            originalRowTsv = "Sony\tXperia 1 VII",
            fields = listOf(
                FieldData(0, "Brand", "OTHER", "Sony"),
                FieldData(1, "Model", "OTHER", "Xperia 1 VII"),
                FieldData(10, "Display Type", "DISPLAY", "4K OLED 120Hz")
            )
        )
        repository.insertPhoneWithFields(phoneData)

        val exportedJson = repository.exportToJson()
        assertTrue(exportedJson.contains("Sony Xperia 1 VII"))
        assertTrue(exportedJson.contains("4K OLED 120Hz"))

        repository.clearDatabase()
        val phonesEmpty = repository.allPhones.first()
        assertEquals(0, phonesEmpty.size)

        val restoredCount = repository.importFromJson(exportedJson)
        assertEquals(1, restoredCount)

        val restoredPhones = repository.allPhones.first()
        assertEquals(1, restoredPhones.size)
        assertEquals("Sony Xperia 1 VII", restoredPhones.first().fullName)
    }

    @Test
    fun testTsvAndCsvExport() = runBlocking {
        val phoneData = ParsedPhoneData(
            brand = "Nothing",
            model = "Phone (3)",
            fullName = "Nothing Phone (3)",
            originalRowTsv = "Nothing\tPhone (3)\tNothing Phone (3)",
            fields = listOf(
                FieldData(0, "Brand", "OTHER", "Nothing"),
                FieldData(1, "Model", "OTHER", "Phone (3)"),
                FieldData(2, "Full Name", "OTHER", "Nothing Phone (3)")
            )
        )
        repository.insertPhoneWithFields(phoneData)

        val tsv = repository.exportToTsv()
        assertTrue(tsv.contains("Brand\tModel\tFull Name"))
        assertTrue(tsv.contains("Nothing Phone (3)"))

        val csv = repository.exportToCsv()
        assertTrue(csv.contains("\"Brand\",\"Model\",\"Full Name\""))
        assertTrue(csv.contains("\"Nothing Phone (3)\""))
    }

    @Test
    fun testClipboardDaoOperations() = runBlocking {
        val id1 = repository.addClipboardItem("Non-tsv text line", isTsv = false, detectedCount = 0)
        val id2 = repository.addClipboardItem("Brand\tModel\nSamsung\tS26", isTsv = true, detectedCount = 1)

        val allItems = repository.allClipboard.first()
        assertEquals(2, allItems.size)

        repository.toggleClipboardFavorite(id2, true)
        repository.toggleClipboardPinned(id2, true)

        val updatedItems = repository.allClipboard.first()
        val pinnedItem = updatedItems.first()
        assertEquals(id2, pinnedItem.id)
        assertTrue(pinnedItem.isPinned)
        assertTrue(pinnedItem.isFavorite)

        repository.deleteClipboardItem(id1)
        val remainingItems = repository.allClipboard.first()
        assertEquals(1, remainingItems.size)
        assertEquals(id2, remainingItems.first().id)
    }
}
