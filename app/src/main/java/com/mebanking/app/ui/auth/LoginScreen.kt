package com.mebanking.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.ui.theme.BankGoldPrimary
import com.mebanking.app.ui.theme.MEbankingTheme
import com.mebanking.app.ui.theme.VcbGreenDark
import com.mebanking.app.ui.theme.VcbGreenLight
import com.mebanking.app.ui.theme.VcbGreenPrimary

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onNavigateToRegister: () -> Unit = {},
    viewModel: LoginViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginContent(
        uiState = uiState,
        onUsernameChanged = viewModel::onUsernameChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onLogin = viewModel::login,
        onNavigateToRegister = onNavigateToRegister,
        modifier = modifier,
    )
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00381E),
                            Color(0xFF005A36),
                            Color(0xFFF4F6F9),
                            Color(0xFFF4F6F9),
                        ),
                        startY = 0f,
                        endY = 1200f,
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))

                // Clean Vector Brand Emblem Header
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF002413))
                        .border(2.dp, BankGoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = "MEbanking Brand",
                        tint = BankGoldPrimary,
                        modifier = Modifier.size(34.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "MEBANKING",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp,
                )

                Text(
                    text = "Ngân hàng số thế hệ mới • Tiêu chuẩn bảo mật VCB/TCB",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC7ECD3),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                // Modern Floating Login Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "ĐĂNG NHẬP",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Vui lòng nhập tài khoản ngân hàng điện tử",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(Modifier.height(22.dp))

                        // Username Field with Vector Icon
                        OutlinedTextField(
                            value = uiState.username,
                            onValueChange = onUsernameChanged,
                            label = { Text("Tên đăng nhập") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = VcbGreenPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            singleLine = true,
                            enabled = !uiState.isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VcbGreenPrimary,
                                focusedLabelColor = VcbGreenPrimary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(14.dp))

                        // Password Field with Vector Icons
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = onPasswordChanged,
                            label = { Text("Mật khẩu") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = VcbGreenPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            singleLine = true,
                            enabled = !uiState.isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VcbGreenPrimary,
                                focusedLabelColor = VcbGreenPrimary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (uiState.errorMessage != null) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = uiState.errorMessage,
                                        color = Color(0xFFC62828),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Primary Login Button (VCB Emerald Green)
                        Button(
                            onClick = onLogin,
                            enabled = uiState.canSubmit,
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
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Text(
                                    text = "ĐĂNG NHẬP AN TOÀN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Clean Vector Biometric Button
                        OutlinedButton(
                            onClick = {
                                if (!uiState.canSubmit) {
                                    onUsernameChanged("customer1")
                                    onPasswordChanged("Password@123")
                                }
                                onLogin()
                            },
                            enabled = !uiState.isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = null,
                                    tint = VcbGreenPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Đăng nhập bằng Sinh trắc học (Face ID)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Open New eKYC Account Button
                        Button(
                            onClick = onNavigateToRegister,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE8F5E9),
                                contentColor = VcbGreenPrimary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    tint = VcbGreenPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Mở tài khoản trực tuyến (eKYC)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Fast Testing Demo Account Pills
                DemoQuickCredentialsCard(
                    onSelect = { u, p ->
                        onUsernameChanged(u)
                        onPasswordChanged(p)
                    },
                )

                Spacer(Modifier.height(24.dp))

                // Bottom Banking Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Quên mật khẩu?",
                        style = MaterialTheme.typography.bodySmall,
                        color = VcbGreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("•", color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "Kích hoạt Smart OTP",
                        style = MaterialTheme.typography.bodySmall,
                        color = VcbGreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("•", color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "Hotline 1900 8888",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoQuickCredentialsCard(
    onSelect: (String, String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tài khoản kiểm thử nhanh",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Chạm để chọn",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect("customer1", "Password@123") },
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = VcbGreenPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Khách hàng", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = VcbGreenPrimary)
                        }
                        Text("customer1", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect("officer1", "Password@123") },
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Badge,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Cán bộ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                        }
                        Text("officer1", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModernLoginPreview() {
    MEbankingTheme {
        LoginContent(
            uiState = LoginUiState(username = "customer1", password = "Password@123"),
            onUsernameChanged = {},
            onPasswordChanged = {},
            onLogin = {},
        )
    }
}
