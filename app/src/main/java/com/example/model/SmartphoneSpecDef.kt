package com.example.model

/**
 * 39 Canonical Smartphone Specification Columns
 */
data class SpecColumnDef(
    val index: Int,
    val name: String,
    val category: String,
    val description: String,
    val sampleValue: String
)

object SmartphoneSpecDef {

    val CATEGORIES = listOf(
        "ALL",
        "DISPLAY",
        "PERFORMANCE",
        "CAMERA",
        "BATTERY",
        "SOFTWARE",
        "CONNECTIVITY",
        "SECURITY",
        "DESIGN",
        "OTHER"
    )

    val DEFAULT_39_COLUMNS: List<SpecColumnDef> = listOf(
        SpecColumnDef(0, "Brand", "OTHER", "Smartphone Brand / Manufacturer", "Samsung"),
        SpecColumnDef(1, "Model", "OTHER", "Phone Model Identifier", "Galaxy S26 Ultra"),
        SpecColumnDef(2, "Full Name", "OTHER", "Commercial Product Name", "Samsung Galaxy S26 Ultra"),
        SpecColumnDef(3, "Release Date", "OTHER", "Launch / Announcement Date", "2026, February"),
        SpecColumnDef(4, "Price", "OTHER", "Official MSRP or Launch Price", "$1,299"),
        SpecColumnDef(5, "Dimensions", "DESIGN", "Body Dimensions (H x W x D)", "162.8 x 77.6 x 8.2 mm"),
        SpecColumnDef(6, "Weight", "DESIGN", "Device Weight", "218 g"),
        SpecColumnDef(7, "Build Material", "DESIGN", "Frame and Back Construction", "Titanium frame, Gorilla Glass back"),
        SpecColumnDef(8, "IP Rating", "DESIGN", "Water and Dust Ingress Protection", "IP68 dust/water resistant"),
        SpecColumnDef(9, "SIM Type", "DESIGN", "SIM slots & eSIM support", "Dual SIM (Nano-SIM and eSIM)"),
        SpecColumnDef(10, "Display Type", "DISPLAY", "Screen Panel Technology", "Dynamic LTPO AMOLED 2X, 120Hz, HDR10+"),
        SpecColumnDef(11, "Display Size", "DISPLAY", "Screen Diagonal Size", "6.9 inches"),
        SpecColumnDef(12, "Resolution", "DISPLAY", "Display Pixel Dimensions & Density", "1440 x 3120 pixels (~500 ppi)"),
        SpecColumnDef(13, "Refresh Rate", "DISPLAY", "Panel Refresh Frequency", "120 hertz"),
        SpecColumnDef(14, "Peak Brightness", "DISPLAY", "Maximum Panel Brightness", "3000 nits peak"),
        SpecColumnDef(15, "Screen Protection", "DISPLAY", "Front Glass Hardness", "Corning Gorilla Armor 2"),
        SpecColumnDef(16, "Operating System", "SOFTWARE", "OS and Custom Skin Version", "Android 16, One UI 8.0"),
        SpecColumnDef(17, "Chipset", "PERFORMANCE", "SoC / Processor Family", "Qualcomm Snapdragon 8 Elite (3 nm)"),
        SpecColumnDef(18, "CPU", "PERFORMANCE", "Core Architecture and Clocks", "Octa-core (2x4.32 GHz & 6x3.53 GHz Oryon)"),
        SpecColumnDef(19, "GPU", "PERFORMANCE", "Graphics Processing Unit", "Adreno 830"),
        SpecColumnDef(20, "RAM", "PERFORMANCE", "System Memory", "12 GB / 16 GB LPDDR5X"),
        SpecColumnDef(21, "Storage", "PERFORMANCE", "Internal Flash Storage", "256 GB / 512 GB / 1 TB UFS 4.1"),
        SpecColumnDef(22, "SD Card Slot", "PERFORMANCE", "Expandable Memory Support", "No"),
        SpecColumnDef(23, "Main Camera", "CAMERA", "Primary Rear Camera Sensor", "200 MP, f/1.7, 24mm (wide), multi-directional PDAF, OIS"),
        SpecColumnDef(24, "Ultra-Wide Camera", "CAMERA", "Secondary Ultra-Wide Lens", "50 MP, f/1.9, 120˚ (ultrawide), dual pixel PDAF"),
        SpecColumnDef(25, "Telephoto Camera", "CAMERA", "Optical Zoom / Periscope Lens", "50 MP, f/3.4, 115mm (5x periscope telephoto), OIS"),
        SpecColumnDef(26, "Video Recording", "CAMERA", "Rear Camera Video Modes", "8K@30fps, 4K@120fps, 1080p@240fps, HDR10+"),
        SpecColumnDef(27, "Front Camera", "CAMERA", "Selfie Camera Sensor & Specs", "12 MP, f/2.2, 26mm (wide), dual pixel PDAF, 4K@60fps"),
        SpecColumnDef(28, "Battery Capacity", "BATTERY", "Typical Battery Energy Storage", "5000 milliamp hours"),
        SpecColumnDef(29, "Wired Charging", "BATTERY", "Maximum Cable Fast-Charge Speed", "65 watt wired, PD3.0"),
        SpecColumnDef(30, "Wireless Charging", "BATTERY", "Inductive Wireless Charging", "25 watt wireless (Qi2)"),
        SpecColumnDef(31, "Reverse Wireless", "BATTERY", "Device-to-device Power Sharing", "4.5 watt reverse wireless"),
        SpecColumnDef(32, "5G Bands", "CONNECTIVITY", "Cellular Network Support", "5G SA/NSA/Sub6/mmWave"),
        SpecColumnDef(33, "Wi-Fi Standard", "CONNECTIVITY", "Wireless Local Area Networking", "Wi-Fi 7 (802.11be), tri-band"),
        SpecColumnDef(34, "Bluetooth", "CONNECTIVITY", "Bluetooth Version & Codecs", "Bluetooth 5.4, A2DP, LE, aptX HD"),
        SpecColumnDef(35, "NFC", "CONNECTIVITY", "Near Field Communication", "Yes"),
        SpecColumnDef(36, "Biometrics", "SECURITY", "Fingerprint and Face Unlock", "Ultrasonic In-display Fingerprint, Face unlock"),
        SpecColumnDef(37, "Speakers & Audio", "OTHER", "Sound Output & Headphone Jack", "Stereo speakers, Tuned by AKG, No 3.5mm jack"),
        SpecColumnDef(38, "Colors", "DESIGN", "Available Exterior Colorways", "Titanium Black, Titanium Gray, Titanium Violet, Titanium Yellow")
    )

    /**
     * User configurable column indexes
     */
    data class ColumnMappingConfig(
        val brandColumnIndex: Int = 0,
        val modelColumnIndex: Int = 1,
        val fullNameColumnIndex: Int = 2,
        val totalExpectedColumns: Int = 39
    )

    /**
     * Map category names to icons/colors
     */
    fun getCategoryColor(category: String): Long {
        return when (category.uppercase()) {
            "DISPLAY" -> 0xFF3B82F6 // Blue
            "PERFORMANCE" -> 0xFFEF4444 // Red
            "CAMERA" -> 0xFF10B981 // Emerald
            "BATTERY" -> 0xFFF59E0B // Amber
            "SOFTWARE" -> 0xFF8B5CF6 // Purple
            "CONNECTIVITY" -> 0xFF06B6D4 // Cyan
            "SECURITY" -> 0xFF6366F1 // Indigo
            "DESIGN" -> 0xFFEC4899 // Pink
            else -> 0xFF64748B // Slate
        }
    }
}
