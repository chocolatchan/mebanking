package com.mebanking.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.ekyc.FaceCaptureDialog
import com.mebanking.app.ui.theme.BankGoldPrimary
import com.mebanking.app.ui.theme.VcbGreenDark
import com.mebanking.app.ui.theme.VcbGreenLight
import com.mebanking.app.ui.theme.VcbGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    // If Face Capture dialog is open
    if (uiState.isFaceCaptureOpen) {
        FaceCaptureDialog(
            title = "Thu thập sinh trắc học khuôn mặt eKYC",
            subtitle = "Căn giữa khuôn mặt trong khung oval. Dữ liệu khuôn mặt sẽ được mã hóa lưu trữ làm mẫu đối soát theo QĐ 2345/QĐ-NHNN.",
            isProcessing = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onFrameCaptured = { frame ->
                viewModel.submitRegistrationWithBiometrics(frame)
            },
            onDismiss = {
                viewModel.closeFaceCapture()
            },
        )
    }

    // Success Dialog upon completed registration
    uiState.registrationResult?.let { result ->
        AlertDialog(
            onDismissRequest = onNavigateBackToLogin,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🎉", fontSize = 32.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Mở tài khoản eKYC thành công!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Chúc mừng Quý khách ${result.user.fullName} đã mở thành công tài khoản ngân hàng số MEbanking.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Số tài khoản thanh toán:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(result.accountNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = VcbGreenPrimary)

                            Spacer(Modifier.height(6.dp))

                            Text("Quà tặng mở tài khoản mới:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(CurrencyFormatter.formatVnd(result.initialBalance), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))

                            Spacer(Modifier.height(6.dp))

                            Text("Định danh sinh trắc học:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("Đã thu thập & kích hoạt eKYC ✔️", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = VcbGreenPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onNavigateBackToLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = VcbGreenPrimary),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("ĐĂNG NHẬP NGAY", fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mở tài khoản eKYC",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Chuẩn định danh sinh trắc học quốc gia",
                            color = Color(0xFFC7ECD3),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBackToLogin) {
                        Text("‹ Đăng nhập", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VcbGreenPrimary,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Steps indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StepChip(number = "1", label = "Thông tin cá nhân", isActive = true)
                Text("───", color = Color.Gray)
                StepChip(number = "2", label = "Khuôn mặt eKYC", isActive = false)
            }

            Spacer(Modifier.height(12.dp))

            // Form Fields
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Thông tin đăng ký trực tuyến",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    OutlinedTextField(
                        value = uiState.fullName,
                        onValueChange = viewModel::onFullNameChanged,
                        label = { Text("Họ và tên (như trên CCCD)") },
                        leadingIcon = { Text("🪪", fontSize = 16.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.nationalId,
                        onValueChange = viewModel::onNationalIdChanged,
                        label = { Text("Số CCCD (12 chữ số)") },
                        leadingIcon = { Text("🔢", fontSize = 16.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChanged,
                        label = { Text("Tên đăng nhập (4-32 ký tự)") },
                        leadingIcon = { Text("👤", fontSize = 16.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = { Text("Mật khẩu (tối thiểu 8 ký tự)") },
                        leadingIcon = { Text("🔒", fontSize = 16.sp) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Ẩn" else "Hiện")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.phone,
                        onValueChange = viewModel::onPhoneChanged,
                        label = { Text("Số điện thoại (10 số, bắt đầu bằng 0)") },
                        leadingIcon = { Text("📱", fontSize = 16.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChanged,
                        label = { Text("Email liên hệ") },
                        leadingIcon = { Text("✉️", fontSize = 16.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "⚠️ ${uiState.errorMessage}",
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Notice badge on eKYC biometric requirement
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🛡️", fontSize = 20.sp)
                    Text(
                        text = "Theo Quyết định 2345/QĐ-NHNN, bước tiếp theo hệ thống sẽ quét khuôn mặt trực tiếp để lưu mẫu sinh trắc học bảo mật.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VcbGreenDark,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // CTA Button to Trigger Face Capture
            Button(
                onClick = viewModel::openFaceCapture,
                enabled = uiState.isFormValid && !uiState.isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VcbGreenPrimary,
                    disabledContainerColor = Color(0xFFA5D6A7),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TIẾP TỤC: CHỤP KHUÔN MẶT eKYC", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("📷")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepChip(number: String, label: String, isActive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isActive) VcbGreenPrimary else Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = if (isActive) Color.White else Color.DarkGray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) VcbGreenPrimary else Color.Gray,
        )
    }
}
