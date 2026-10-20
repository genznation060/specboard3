package com.example

import com.example.model.SmartphoneSpecDef
import com.example.parser.TsvParser
import com.example.queue.PasteQueueManager
import com.example.queue.QueueSeparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

    @Before
    fun setup() {
        PasteQueueManager.clear()
        PasteQueueManager.setSeparator(QueueSeparator.NEWLINE)
    }

    @Test
    fun testTsvDetection() {
        val nonTsv = "Samsung Galaxy S26 Ultra is a phone with 120Hz display"
        assertFalse(TsvParser.isLikelyTsv(nonTsv))

        val validTsv = "Samsung\tGalaxy S26 Ultra\tSamsung Galaxy S26 Ultra\t120Hz"
        assertTrue(TsvParser.isLikelyTsv(validTsv))
    }

    @Test
    fun test39ColumnTsvParsing() {
        val sampleRow = "Samsung\tGalaxy S26 Ultra\tSamsung Galaxy S26 Ultra\t2026, Feb\t$1299\t162.8 x 77.6 x 8.2 mm\t218 g\tTitanium\tIP68\tDual SIM\tAMOLED\t6.9 inch\t1440x3120\t120 hertz\t3000 nits\tGorilla Armor\tAndroid 16\tSnapdragon 8 Elite\tOcta-core\tAdreno 830\t16GB\t512GB\tNo\t200 MP\t50 MP\t50 MP\t8K@30fps\t12 MP\t5000 milliamp hours\t65 watt\t25 watt\t4.5 watt\t5G\tWi-Fi 7\tBluetooth 5.4\tYes\tUltrasonic\tStereo\tTitanium Black"

        val result = TsvParser.parseTsv(sampleRow)
        assertTrue(result.isTsv)
        assertEquals(1, result.parsedPhones.size)

        val phone = result.parsedPhones.first()
        assertEquals("Samsung", phone.brand)
        assertEquals("Galaxy S26 Ultra", phone.model)
        assertEquals("Samsung Galaxy S26 Ultra", phone.fullName)
        assertEquals(39, phone.fields.size)

        // Verify categories
        val refreshRateField = phone.fields.firstOrNull { it.fieldName.contains("Refresh Rate", ignoreCase = true) }
        assertNotNull(refreshRateField)
        assertEquals("120 hertz", refreshRateField?.fieldValue)
        assertEquals("DISPLAY", refreshRateField?.category)

        val batteryField = phone.fields.firstOrNull { it.fieldName.contains("Battery", ignoreCase = true) }
        assertNotNull(batteryField)
        assertEquals("5000 milliamp hours", batteryField?.fieldValue)
        assertEquals("BATTERY", batteryField?.category)
    }

    @Test
    fun testEmptyFieldPreservation() {
        val rowWithEmptyFields = "Vivo\tX300\tVivo X300\t\t\t\t\t\t\t\tAMOLED\t\t\t120Hz"
        val result = TsvParser.parseTsv(rowWithEmptyFields)
        val phone = result.parsedPhones.first()

        assertEquals("Vivo", phone.brand)
        assertEquals("X300", phone.model)
        // Ensure empty fields were not dropped
        assertTrue(phone.fields.size >= 14)
        val emptyField = phone.fields[3]
        assertEquals("", emptyField.fieldValue)
    }

    @Test
    fun testHeaderRecognition() {
        val tsvWithHeader = "Brand\tModel\tFull Name\tDisplay Type\tChipset\nGoogle\tPixel 10 Pro\tGoogle Pixel 10 Pro\tLTPO OLED\tTensor G5"
        val result = TsvParser.parseTsv(tsvWithHeader)
        assertTrue(result.isTsv)
        assertTrue(result.hasHeaders)
        assertEquals(1, result.parsedPhones.size)
        assertEquals("Google", result.parsedPhones[0].brand)
        assertEquals("Pixel 10 Pro", result.parsedPhones[0].model)
        assertEquals("Display Type", result.parsedPhones[0].fields[3].fieldName)
        assertEquals("LTPO OLED", result.parsedPhones[0].fields[3].fieldValue)
        assertEquals("DISPLAY", result.parsedPhones[0].fields[3].category)
    }

    @Test
    fun testCustomColumnMappingConfig() {
        // Model in col 0, Brand in col 1, Full Name in col 2
        val customOrderRow = "Galaxy S26\tSamsung\tSamsung Galaxy S26\t2026\t$899"
        val config = SmartphoneSpecDef.ColumnMappingConfig(
            brandColumnIndex = 1,
            modelColumnIndex = 0,
            fullNameColumnIndex = 2
        )
        val result = TsvParser.parseTsv(customOrderRow, config)
        assertTrue(result.isTsv)
        val phone = result.parsedPhones.first()
        assertEquals("Samsung", phone.brand)
        assertEquals("Galaxy S26", phone.model)
        assertEquals("Samsung Galaxy S26", phone.fullName)
    }

    @Test
    fun testIdentityResolutionInference() {
        // Brand is blank, Model is blank, but Full Name is "OnePlus 13 Pro"
        val rowWithInferredBrand = "\t\tOnePlus 13 Pro\t2026\t$999"
        val result = TsvParser.parseTsv(rowWithInferredBrand)
        assertTrue(result.isTsv)
        val phone = result.parsedPhones.first()
        assertEquals("OnePlus", phone.brand)
        assertEquals("13 Pro", phone.model)
        assertEquals("OnePlus 13 Pro", phone.fullName)
    }

    @Test
    fun testSpreadsheetQuotesUnescaping() {
        val quotedRow = "\"Apple\"\t\"iPhone 18 Pro\"\t\"Apple iPhone 18 Pro\"\t\"48 MP, f/1.78, \"\"Sensor-Shift\"\" OIS\""
        val result = TsvParser.parseTsv(quotedRow)
        val phone = result.parsedPhones.first()
        assertEquals("Apple", phone.brand)
        assertEquals("iPhone 18 Pro", phone.model)
        val cameraField = phone.fields[3]
        assertEquals("48 MP, f/1.78, \"Sensor-Shift\" OIS", cameraField.fieldValue)
    }

    @Test
    fun testAdditionalColumnsHandling() {
        // 41 columns (2 additional beyond the 39 standard)
        val rowWith41Cols = (1..41).joinToString("\t") { "Val$it" }
        val result = TsvParser.parseTsv(rowWith41Cols)
        assertTrue(result.isTsv)
        val phone = result.parsedPhones.first()
        assertEquals(41, phone.fields.size)
        assertEquals("Val40", phone.fields[39].fieldValue)
        assertEquals("Val41", phone.fields[40].fieldValue)
        assertTrue(result.warnings.any { it.contains("additional specifications preserved") })
    }

    @Test
    fun testCategoryAssignmentHeuristics() {
        assertEquals("DISPLAY", TsvParser.guessCategory("Panel Refresh Rate"))
        assertEquals("PERFORMANCE", TsvParser.guessCategory("SoC Chipset"))
        assertEquals("PERFORMANCE", TsvParser.guessCategory("System RAM"))
        assertEquals("CAMERA", TsvParser.guessCategory("Periscope Telephoto Lens"))
        assertEquals("BATTERY", TsvParser.guessCategory("Fast Charging Wattage"))
        assertEquals("SOFTWARE", TsvParser.guessCategory("Android UI Skin"))
        assertEquals("CONNECTIVITY", TsvParser.guessCategory("5G Bands Support"))
        assertEquals("SECURITY", TsvParser.guessCategory("Ultrasonic Fingerprint Scanner"))
        assertEquals("DESIGN", TsvParser.guessCategory("Body Weight in grams"))
    }

    @Test
    fun testPasteQueueOrderingAndRemoval() {
        PasteQueueManager.addItem("Item 1", "Label 1")
        PasteQueueManager.addItem("Item 2", "Label 2")
        PasteQueueManager.addItem("Item 3", "Label 3")
        assertEquals(3, PasteQueueManager.queue.value.size)

        // Remove item at index 1 ("Item 2")
        PasteQueueManager.removeAt(1)
        assertEquals(2, PasteQueueManager.queue.value.size)
        assertEquals("Item 1", PasteQueueManager.queue.value[0].value)
        assertEquals("Item 3", PasteQueueManager.queue.value[1].value)

        // Format all without draining
        PasteQueueManager.setSeparator(QueueSeparator.COMMA)
        val formatted = PasteQueueManager.formatAll()
        assertEquals("Item 1, Item 3", formatted)
        assertEquals(2, PasteQueueManager.queue.value.size)

        // Pop next consumes only 1 item
        val next = PasteQueueManager.popNext()
        assertEquals("Item 1", next?.value)
        assertEquals(1, PasteQueueManager.queue.value.size)

        PasteQueueManager.clear()
        assertEquals(0, PasteQueueManager.queue.value.size)
    }

    @Test
    fun testCustomSeparatorFormatting() {
        PasteQueueManager.addItem("Spec A", "Label A")
        PasteQueueManager.addItem("Spec B", "Label B")
        PasteQueueManager.addItem("Spec C", "Label C")

        PasteQueueManager.setCustomSeparator(" | ")
        val formatted = PasteQueueManager.drainAll()
        assertEquals("Spec A | Spec B | Spec C", formatted)
        assertEquals(0, PasteQueueManager.queue.value.size)
    }
}
