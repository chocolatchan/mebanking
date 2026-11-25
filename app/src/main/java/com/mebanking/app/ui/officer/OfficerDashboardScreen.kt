package com.mebanking.app.ui.officer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.ui.ekyc.FaceCaptureDialog

@Composable
fun OfficerDashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val factory = (context.applicationContext as MEbankingApplication).container.viewModelFactory
    val viewModel: OfficerDashboardViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OfficerDashboardContent(
        state = state,
        onBack = onBack,
        onQueryChanged = viewModel::onQueryChanged,
        onSelect = viewModel::selectCustomer,
        onNew = viewModel::newCustomer,
        onUsername = viewModel::onUsernameChanged,
        onPassword = viewModel::onPasswordChanged,
        onName = viewModel::onFullNameChanged,
        onEmail = viewModel::onEmailChanged,
        onPhone = viewModel::onPhoneChanged,
        onNationalId = viewModel::onNationalIdChanged,
        onKyc = viewModel::onKycChanged,
        onSave = viewModel::save,
        onEnrollFace = { customerId, frame -> viewModel.enrollFaceBiometrics(customerId, frame) },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun OfficerDashboardContent(
    state: OfficerDashboardUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSelect: (com.mebanking.app.domain.model.User) -> Unit,
    onNew: () -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPhone: (String) -> Unit,
    onNationalId: (String) -> Unit,
    onKyc: (Boolean) -> Unit,
    onSave: () -> Unit,
    onEnrollFace: (Long, ByteArray) -> Unit = { _, _ -> },
) {
    var enrollingCustomerId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    com.mebanking.app.ui.theme.SynchronizedStatusBar(darkIcons = !isDark)

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            enrollingCustomerId = null
        }
    }

    val scrollToForm = {
        val formIndex = 2 +
            (if (state.isLoading) 1 else 0) +
            (if (state.errorMessage != null) 1 else 0) +
            state.customers.size
        coroutineScope.launch {
            listState.animateScrollToItem(formIndex)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Quản lý hồ sơ khách hàng") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm theo CCCD hoặc họ tên") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                )
            }
            item {
                Button(
                    onClick = {
                        onNew()
                        scrollToForm()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Tạo hồ sơ khách hàng mới")
                }
            }
            if (state.isLoading) item { CircularProgressIndicator(Modifier.padding(12.dp)) }
            state.errorMessage?.let { message ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
            state.customers.forEach { customer ->
                item(key = "customer_${customer.id}") {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            onSelect(customer)
                            scrollToForm()
                        },
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(customer.fullName, style = MaterialTheme.typography.titleMedium)
                            Text("CCCD: ${customer.nationalId ?: "Chưa cập nhật"} · ${customer.phone}")
                            Text(
                                if (customer.kycVerified) "KYC đã xác minh" else "KYC chưa xác minh",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item { Text(if (state.isCreating) "Tạo hồ sơ" else "Cập nhật hồ sơ", style = MaterialTheme.typography.titleLarge) }
            if (state.isCreating) {
                item { OutlinedTextField(state.username, onUsername, Modifier.fillMaxWidth(), label = { Text("Tên đăng nhập") }, singleLine = true) }
                item { OutlinedTextField(state.password, onPassword, Modifier.fillMaxWidth(), label = { Text("Mật khẩu tạm (tối thiểu 8 ký tự)") }, singleLine = true) }
            }
            item { OutlinedTextField(state.fullName, onName, Modifier.fillMaxWidth(), label = { Text("Họ và tên") }, singleLine = true) }
            item { OutlinedTextField(state.nationalId, onNationalId, Modifier.fillMaxWidth(), label = { Text("CCCD (12 số)") }, singleLine = true) }
            item { OutlinedTextField(state.email, onEmail, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true) }
            item { OutlinedTextField(state.phone, onPhone, Modifier.fillMaxWidth(), label = { Text("Số điện thoại") }, singleLine = true) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.kycVerified, onCheckedChange = onKyc)
                    Text("Đã xác minh KYC")
                }
            }
            if (!state.isCreating && state.selectedCustomer != null) {
                item {
                    OutlinedButton(
                        onClick = { enrollingCustomerId = state.selectedCustomer.id },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Face,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Thu thập sinh trắc học khuôn mặt eKYC")
                        }
                    }
                }
            }
            state.successMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
            item { Button(onClick = onSave, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) { Text("Lưu hồ sơ") } }
        }
    }

    enrollingCustomerId?.let { customerId ->
        FaceCaptureDialog(
            title = "Thu thập sinh trắc học khuôn mặt eKYC",
            subtitle = "Căn giữa khuôn mặt của khách hàng vào khung oval để hoàn tất đăng ký sinh trắc học.",
            isProcessing = state.isLoading,
            errorMessage = state.errorMessage,
            onFrameCaptured = { frame ->
                onEnrollFace(customerId, frame)
            },
            onDismiss = {
                enrollingCustomerId = null
            },
        )
    }
}
