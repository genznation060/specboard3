package com.example.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PhoneField
import com.example.data.entity.PhoneRecord
import com.example.data.repository.SpecRepository
import com.example.model.SmartphoneSpecDef
import com.example.queue.PasteQueueManager
import com.example.queue.QueueSeparator
import kotlinx.coroutines.launch

enum class ImeNavMode {
    BRANDS,
    PHONES,
    SPEC_CATEGORIES,
    QUEUE,
    FAVORITES,
    RECENT,
    SEARCH,
    KEYPAD
}

@Composable
fun ImeKeyboardScreen(
    repository: SpecRepository,
    onInsertText: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onSwitchIme: () -> Unit,
    onCloseIme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var navMode by remember { mutableStateOf(ImeNavMode.BRANDS) }
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var selectedPhone by remember { mutableStateOf<PhoneRecord?>(null) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val allPhones by repository.allPhones.collectAsState(initial = emptyList())
    val allBrands by repository.allBrands.collectAsState(initial = emptyList())
    val favoritePhones by repository.favoritePhones.collectAsState(initial = emptyList())
    val recentPhones by repository.recentPhones.collectAsState(initial = emptyList())

    val queueItems by PasteQueueManager.queue.collectAsState()
    val selectedSeparator by PasteQueueManager.selectedSeparator.collectAsState()

    // Helper for inserting text with haptic and usage increment
    fun triggerInsert(text: String, phoneId: Long? = null, fieldId: Long? = null) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onInsertText(text)
        scope.launch {
            phoneId?.let { repository.incrementPhoneUsage(it) }
            fieldId?.let { repository.incrementFieldUsage(it) }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp),
        color = Color(0xFF0F172A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- TOP BREADCRUMB & MODE BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (if drilled down)
                if (navMode == ImeNavMode.SPEC_CATEGORIES || (navMode == ImeNavMode.PHONES && selectedBrand != null)) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (navMode == ImeNavMode.SPEC_CATEGORIES) {
                                navMode = ImeNavMode.PHONES
                            } else {
                                selectedBrand = null
                                navMode = ImeNavMode.BRANDS
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Breadcrumb Text or Title
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BRANDS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (navMode == ImeNavMode.BRANDS) FontWeight.Bold else FontWeight.Normal,
                        color = if (navMode == ImeNavMode.BRANDS) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                selectedBrand = null
                                selectedPhone = null
                                navMode = ImeNavMode.BRANDS
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    if (selectedBrand != null) {
                        Text(" › ", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text(
                            text = selectedBrand.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (navMode == ImeNavMode.PHONES) FontWeight.Bold else FontWeight.Normal,
                            color = if (navMode == ImeNavMode.PHONES) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    selectedPhone = null
                                    navMode = ImeNavMode.PHONES
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (selectedPhone != null && navMode == ImeNavMode.SPEC_CATEGORIES) {
                        Text(" › ", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text(
                            text = selectedPhone?.model.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick Navigation Mode Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { navMode = ImeNavMode.SEARCH },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (navMode == ImeNavMode.SEARCH) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { navMode = ImeNavMode.FAVORITES },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorites",
                            tint = if (navMode == ImeNavMode.FAVORITES) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { navMode = ImeNavMode.QUEUE },
                        modifier = Modifier.size(32.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (queueItems.isNotEmpty()) {
                                    Badge(
                                        containerColor = Color(0xFF38BDF8),
                                        contentColor = Color.Black
                                    ) {
                                        Text("${queueItems.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Queue,
                                contentDescription = "Queue",
                                tint = if (navMode == ImeNavMode.QUEUE) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { navMode = ImeNavMode.KEYPAD },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Keypad",
                            tint = if (navMode == ImeNavMode.KEYPAD) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)

            // --- MAIN CONTENT BODY ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (navMode) {
                    ImeNavMode.BRANDS -> {
                        BrandPickerView(
                            brands = allBrands,
                            onBrandSelected = { brand ->
                                selectedBrand = brand
                                navMode = ImeNavMode.PHONES
                            }
                        )
                    }
                    ImeNavMode.PHONES -> {
                        val filteredPhones = if (selectedBrand != null) {
                            allPhones.filter { it.brand.equals(selectedBrand, ignoreCase = true) }
                        } else {
                            allPhones
                        }
                        PhonePickerView(
                            brandName = selectedBrand ?: "All Phones",
                            phones = filteredPhones,
                            onPhoneSelected = { phone ->
                                selectedPhone = phone
                                navMode = ImeNavMode.SPEC_CATEGORIES
                            },
                            onInsertFullName = { phone ->
                                triggerInsert(phone.fullName, phoneId = phone.id)
                            }
                        )
                    }
                    ImeNavMode.SPEC_CATEGORIES -> {
                        val currentPhone = selectedPhone
                        if (currentPhone != null) {
                            PhoneSpecsExplorerView(
                                repository = repository,
                                phone = currentPhone,
                                selectedCategory = selectedCategory,
                                onCategoryChange = { selectedCategory = it },
                                onInsertField = { field ->
                                    triggerInsert(field.fieldValue, phoneId = currentPhone.id, fieldId = field.id)
                                },
                                onAddToQueue = { field ->
                                    PasteQueueManager.addItem(
                                        value = field.fieldValue,
                                        label = field.fieldName,
                                        sourcePhone = currentPhone.model
                                    )
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                        } else {
                            Text("No phone selected", modifier = Modifier.padding(16.dp))
                        }
                    }
                    ImeNavMode.QUEUE -> {
                        PasteQueueView(
                            queue = queueItems,
                            selectedSeparator = selectedSeparator,
                            onSelectSeparator = { PasteQueueManager.setSeparator(it) },
                            onInsertNext = {
                                val next = PasteQueueManager.peekNext()
                                if (next != null) {
                                    triggerInsert(next.value)
                                    PasteQueueManager.popNext()
                                }
                            },
                            onInsertAll = {
                                val allText = PasteQueueManager.formatAll()
                                if (allText.isNotEmpty()) {
                                    triggerInsert(allText)
                                    PasteQueueManager.clear()
                                }
                            },
                            onRemoveAt = { idx -> PasteQueueManager.removeAt(idx) },
                            onClear = { PasteQueueManager.clear() }
                        )
                    }
                    ImeNavMode.FAVORITES -> {
                        FavoritesPickerView(
                            phones = favoritePhones,
                            onPhoneSelected = { phone ->
                                selectedPhone = phone
                                navMode = ImeNavMode.SPEC_CATEGORIES
                            },
                            onInsertFullName = { phone ->
                                triggerInsert(phone.fullName, phoneId = phone.id)
                            }
                        )
                    }
                    ImeNavMode.RECENT -> {
                        FavoritesPickerView(
                            phones = recentPhones,
                            onPhoneSelected = { phone ->
                                selectedPhone = phone
                                navMode = ImeNavMode.SPEC_CATEGORIES
                            },
                            onInsertFullName = { phone ->
                                triggerInsert(phone.fullName, phoneId = phone.id)
                            }
                        )
                    }
                    ImeNavMode.SEARCH -> {
                        SearchSpecsView(
                            repository = repository,
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onInsertField = { field ->
                                triggerInsert(field.fieldValue, phoneId = field.phoneId, fieldId = field.id)
                            },
                            onAddToQueue = { field ->
                                PasteQueueManager.addItem(field.fieldValue, label = field.fieldName)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                    ImeNavMode.KEYPAD -> {
                        ImeCompactKeypadView(
                            onChar = { char ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onInsertText(char)
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)

            // --- COMPACT BOTTOM TOOLBAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Switch IME Globe
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwitchIme()
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Switch Input Method",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Quick Mode Switch: Brands
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navMode = ImeNavMode.BRANDS
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = "Brands",
                        tint = if (navMode == ImeNavMode.BRANDS) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Space Key
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpace()
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF334155),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SpaceBar,
                            contentDescription = "Space",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Space", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    }
                }

                // Backspace Key
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBackspace()
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF334155),
                    modifier = Modifier
                        .size(width = 46.dp, height = 36.dp)
                        .padding(horizontal = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Enter Key
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnter()
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0284C7),
                    modifier = Modifier
                        .size(width = 48.dp, height = 36.dp)
                        .padding(horizontal = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                            contentDescription = "Enter",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Hide Keyboard
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCloseIme()
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardHide,
                        contentDescription = "Hide Keyboard",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BrandPickerView(
    brands: List<String>,
    onBrandSelected: (String) -> Unit
) {
    if (brands.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No smartphone records yet.\nImport TSV in main app or load sample flagships.",
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(brands) { brand ->
            Card(
                onClick = { onBrandSelected(brand) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = brand.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Text("›", color = Color(0xFF64748B), fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun PhonePickerView(
    brandName: String,
    phones: List<PhoneRecord>,
    onPhoneSelected: (PhoneRecord) -> Unit,
    onInsertFullName: (PhoneRecord) -> Unit
) {
    if (phones.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No phones found for $brandName", color = Color(0xFF94A3B8), fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(phones) { phone ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPhoneSelected(phone) }
                    ) {
                        Text(
                            text = phone.model,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = phone.fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onInsertFullName(phone) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF334155),
                            contentColor = Color(0xFF38BDF8)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Paste Name", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { onPhoneSelected(phone) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("›", color = Color(0xFF64748B), fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneSpecsExplorerView(
    repository: SpecRepository,
    phone: PhoneRecord,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onInsertField: (PhoneField) -> Unit,
    onAddToQueue: (PhoneField) -> Unit
) {
    val fields by if (selectedCategory == "ALL") {
        repository.getFieldsForPhone(phone.id).collectAsState(initial = emptyList())
    } else {
        repository.getFieldsByCategory(phone.id, selectedCategory).collectAsState(initial = emptyList())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = SmartphoneSpecDef.CATEGORIES.indexOf(selectedCategory).coerceAtLeast(0),
            containerColor = Color(0xFF1E293B),
            contentColor = Color(0xFF38BDF8),
            edgePadding = 8.dp,
            divider = {}
        ) {
            SmartphoneSpecDef.CATEGORIES.forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(cat) },
                    text = {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedCategory == cat) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                )
            }
        }

        if (fields.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No specifications in this category", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(fields) { field ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onInsertField(field) }
                            ) {
                                Text(
                                    text = field.fieldName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(SmartphoneSpecDef.getCategoryColor(field.category))
                                )
                                Text(
                                    text = field.fieldValue.ifBlank { "—" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { onAddToQueue(field) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add to Queue",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Button(
                                onClick = { onInsertField(field) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Insert", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PasteQueueView(
    queue: List<com.example.queue.QueuedItem>,
    selectedSeparator: QueueSeparator,
    onSelectSeparator: (QueueSeparator) -> Unit,
    onInsertNext: () -> Unit,
    onInsertAll: () -> Unit,
    onRemoveAt: (Int) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Paste Queue (${queue.size} items)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onInsertNext,
                    enabled = queue.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Insert Next", fontSize = 10.sp)
                }

                Button(
                    onClick = onInsertAll,
                    enabled = queue.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Insert All", fontSize = 10.sp)
                }

                IconButton(
                    onClick = onClear,
                    enabled = queue.isNotEmpty(),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear Queue", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            items(QueueSeparator.values()) { sep ->
                FilterChip(
                    selected = selectedSeparator == sep,
                    onClick = { onSelectSeparator(sep) },
                    label = { Text(sep.displayName, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        val customSep by PasteQueueManager.customSeparator.collectAsState()
        if (selectedSeparator == QueueSeparator.CUSTOM) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Custom Sep:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = customSep,
                    onValueChange = { PasteQueueManager.setCustomSeparator(it) },
                    placeholder = { Text("e.g. \" | \" or \" // \"", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                )
            }
        }

        if (queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Queue is empty.\nTap '+' on any phone spec to queue multiple fields!",
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(queue.size) { idx ->
                    val item = queue[idx]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text("${idx + 1}.", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                if (item.label.isNotBlank()) {
                                    Text(item.label, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                Text(item.value, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        IconButton(
                            onClick = { onRemoveAt(idx) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesPickerView(
    phones: List<PhoneRecord>,
    onPhoneSelected: (PhoneRecord) -> Unit,
    onInsertFullName: (PhoneRecord) -> Unit
) {
    if (phones.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No favorites or recents yet", color = Color(0xFF94A3B8), fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(phones) { phone ->
            Card(
                onClick = { onPhoneSelected(phone) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(phone.fullName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(phone.brand, color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onInsertFullName(phone) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Paste", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSpecsView(
    repository: SpecRepository,
    query: String,
    onQueryChange: (String) -> Unit,
    onInsertField: (PhoneField) -> Unit,
    onAddToQueue: (PhoneField) -> Unit
) {
    val searchResults by repository.searchFields(query).collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search specs (e.g. 120Hz, Snapdragon, 5000mAh...)", fontSize = 12.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF334155)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (query.isBlank()) "Type above to search across all smartphone specifications" else "No matching specifications found",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults) { field ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onInsertField(field) }
                            ) {
                                Text(
                                    text = field.fieldName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(SmartphoneSpecDef.getCategoryColor(field.category))
                                )
                                Text(
                                    text = field.fieldValue,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { onAddToQueue(field) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            }

                            Button(
                                onClick = { onInsertField(field) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Insert", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImeCompactKeypadView(
    onChar: (String) -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "-"),
        listOf("Z", "X", "C", "V", "B", "N", "M", ".", ",", "/")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEach { char ->
                    Surface(
                        onClick = { onChar(char) },
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = char,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
