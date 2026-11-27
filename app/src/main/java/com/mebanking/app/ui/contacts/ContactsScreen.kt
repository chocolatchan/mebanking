package com.mebanking.app.ui.contacts

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.service.SavedContact
import com.mebanking.app.domain.util.FuzzySearchHelper
import com.mebanking.app.ui.components.M3EMotion
import com.mebanking.app.ui.components.TopSearchAppBar
import com.mebanking.app.ui.components.m3eBouncyPress
import com.mebanking.app.ui.components.tonalTextFieldColors
import com.mebanking.app.ui.theme.SynchronizedStatusBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    isBottomBarVisible: Boolean = true,
    onSelectContactForTransfer: (accountNumber: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())
    val context = LocalContext.current
    val store = remember(context) {
        (context.applicationContext as MEbankingApplication).container.savedContactStore
    }
    val contacts by store.contacts.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter { contact ->
            FuzzySearchHelper.matches(
                query = searchQuery,
                fields = listOf(contact.name, contact.accountNumber, contact.bank, contact.phone),
            )
        }
    }

    val fabBottomPadding by animateDpAsState(
        targetValue = if (isBottomBarVisible) 88.dp else 16.dp,
        animationSpec = M3EMotion.spatialSpring(),
        label = "contacts_fab_padding",
    )

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopSearchAppBar(
                title = "Danh bạ liên hệ",
                subtitle = "${filteredContacts.size} người nhận đã lưu",
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Tìm tên, số tài khoản, ngân hàng...",
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            FloatingActionButton(
                onClick = { showAddDialog = true },
                interactionSource = interactionSource,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = fabBottomPadding)
                    .m3eBouncyPress(interactionSource, pressedScale = 0.92f),
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Thêm liên hệ mới",
                )
            }
        },
    ) { innerPadding ->
        if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Chưa có liên hệ nào" else "Không tìm thấy liên hệ phù hợp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Nhấn nút + bên dưới để thêm người nhận thường xuyên.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 152.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = filteredContacts,
                    key = { it.id },
                ) { contact ->
                    ContactGridCard(
                        contact = contact,
                        onTransferClick = {
                            onSelectContactForTransfer(contact.accountNumber, contact.name)
                        },
                        onDeleteClick = { store.deleteContact(contact.id) },
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = M3EMotion.effectsSpring(),
                                placementSpec = M3EMotion.offsetSpring(),
                                fadeOutSpec = M3EMotion.effectsSpring(),
                            )
                            .animateContentSize(M3EMotion.sizeSpring()),
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, accountNumber, bank, phone ->
                store.addContact(
                    name = name,
                    accountNumber = accountNumber,
                    bank = bank,
                    phone = phone,
                )
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun ContactGridCard(
    contact: SavedContact,
    onTransferClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onTransferClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .m3eBouncyPress(interactionSource, pressedScale = 0.96f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = contact.initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Xóa liên hệ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = contact.accountNumber,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (contact.phone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = contact.phone,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = contact.bank,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Chuyển tiền",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, accountNumber: String, bank: String, phone: String) -> Unit,
) {
    val popularBanks = listOf("MEbanking", "VCB", "TCB", "MB", "ACB", "BIDV")
    var name by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("MEbanking") }
    var phone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                text = "Thêm liên hệ mới",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Tên gợi nhớ / Họ và tên") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = accountNumber,
                    onValueChange = {
                        accountNumber = it.filter { ch -> ch.isLetterOrDigit() }
                        errorMessage = null
                    },
                    label = { Text("Số tài khoản") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Số điện thoại (tùy chọn)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Ngân hàng",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(popularBanks) { item ->
                        FilterChip(
                            selected = bank == item,
                            onClick = { bank = item },
                            label = { Text(item) },
                        )
                    }
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || accountNumber.isBlank()) {
                        errorMessage = "Vui lòng nhập đầy đủ tên và số tài khoản"
                    } else {
                        onConfirm(name, accountNumber, bank, phone)
                    }
                },
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Lưu liên hệ", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}
