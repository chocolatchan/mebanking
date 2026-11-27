package com.mebanking.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.common.label
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Returns Material 3 Segmented List corner geometry for an item at [index] inside a group of [count].
 */
fun segmentedListItemShape(index: Int, count: Int): RoundedCornerShape {
    val outer = 20.dp
    val inner = 6.dp
    return when {
        count <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        index == count - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}

@Composable
fun tonalTextFieldColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    disabledContainerColor = disabledContainerColor,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledLabelColor = MaterialTheme.colorScheme.primary,
)

/**
 * Shared borderless Material 3 Expressive Centered Hero Carousel for selecting a source / target bank account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionCarousel(
    accounts: List<Account>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (accounts.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Không có tài khoản khả dụng",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
        return
    }

    if (accounts.size == 1) {
        AccountHeroCard(
            account = accounts.first(),
            selected = true,
            detailAlpha = 1f,
            onClick = { onAccountSelected(accounts.first().id) },
            modifier = modifier.fillMaxWidth(),
        )
        return
    }

    val initialItem = accounts.indexOfFirst { it.id == selectedAccountId }.coerceAtLeast(0)
    val carouselState = rememberCarouselState(initialItem = initialItem) { accounts.size }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedAccountId, accounts) {
        val targetIdx = accounts.indexOfFirst { it.id == selectedAccountId }
        if (targetIdx >= 0 && targetIdx != carouselState.currentItem && !carouselState.isScrollInProgress) {
            carouselState.animateScrollToItem(targetIdx, animationSpec = M3EMotion.spatialSpring())
        }
    }

    LaunchedEffect(carouselState.currentItem, carouselState.isScrollInProgress, accounts) {
        if (!carouselState.isScrollInProgress) {
            accounts.getOrNull(carouselState.currentItem)?.let { centered ->
                if (centered.id != selectedAccountId) {
                    onAccountSelected(centered.id)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalCenteredHeroCarousel(
            state = carouselState,
            maxItemWidth = 316.dp,
            itemSpacing = 8.dp,
            minSmallItemWidth = 24.dp,
            maxSmallItemWidth = 44.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp),
        ) { page ->
            val account = accounts[page]
            val selected = account.id == selectedAccountId || page == carouselState.currentItem
            val visibleRatio = (carouselItemDrawInfo.size / carouselItemDrawInfo.maxSize.coerceAtLeast(1f))
                .coerceIn(0f, 1f)
            val detailAlpha = ((visibleRatio - 0.45f) / 0.55f).coerceIn(0f, 1f)

            AccountHeroCard(
                account = account,
                selected = selected,
                detailAlpha = detailAlpha,
                onClick = {
                    onAccountSelected(account.id)
                    coroutineScope.launch {
                        carouselState.animateScrollToItem(page, animationSpec = M3EMotion.spatialSpring())
                    }
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .maskClip(RoundedCornerShape(26.dp)),
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(accounts.size) { idx ->
                val isCurrent = idx == carouselState.currentItem
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (isCurrent) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                        .clickable {
                            onAccountSelected(accounts[idx].id)
                            coroutineScope.launch {
                                carouselState.animateScrollToItem(idx, animationSpec = M3EMotion.spatialSpring())
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun AccountHeroCard(
    account: Account,
    selected: Boolean,
    detailAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        if (detailAlpha > 0.1f) {
                            Text(
                                text = account.accountType.label(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = detailAlpha),
                                maxLines = 1,
                            )
                        }
                    }
                }

                if (selected && detailAlpha > 0.4f) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Đang chọn",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = detailAlpha),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Text(
                text = account.accountNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f * detailAlpha)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = detailAlpha)
                },
            )

            Column(modifier = Modifier.graphicsLayer { alpha = detailAlpha }) {
                Text(
                    text = "Số dư khả dụng",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
                Text(
                    text = CurrencyFormatter.formatVnd(account.balance),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Enlarged tonal hero amount input block shared across Transfer, Deposit, and Withdraw screens.
 */
@Composable
fun LargeAmountInputSection(
    amountInput: String,
    onAmountChanged: (String) -> Unit,
    helperText: String,
    enabled: Boolean = true,
    quickAmounts: List<Pair<String, String>> = listOf(
        "100000" to "100.000đ",
        "200000" to "200.000đ",
        "500000" to "500.000đ",
        "1000000" to "1.000.000đ",
        "2000000" to "2.000.000đ",
        "5000000" to "5.000.000đ",
    ),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "Số tiền giao dịch",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                TextField(
                    value = amountInput,
                    onValueChange = onAmountChanged,
                    placeholder = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                                fontSize = 30.sp,
                                fontFeatureSettings = "tnum",
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                    },
                    suffix = {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "VND",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    },
                    singleLine = true,
                    enabled = enabled,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    colors = tonalTextFieldColors(containerColor = Color.Transparent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickAmounts.forEach { (value, label) ->
                val isSelected = amountInput == value
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    onClick = { if (enabled) onAmountChanged(value) },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                            fontFeatureSettings = "tnum",
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

/**
 * Uniform enlarged primary CTA button (`60.dp` tall, `18.dp` corner radius) with M3E press spring.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.97f else 1f,
        animationSpec = M3EMotion.fastSpatialSpring(),
        label = "cta_press_scale",
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Shared borderless tonal feedback banner for error / success messages.
 */
@Composable
fun TonalFeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text("Đóng", color = contentColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Material 3 Expressive motion tokens:
 * - Spatial springs have subtle overshoot for physical transforms/bounds.
 * - Effects springs are critically damped (no overshoot) for opacity/color.
 */
object M3EMotion {
    fun <T> spatialSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.75f,
        stiffness = 380f,
    )

    fun <T> fastSpatialSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = 650f,
    )

    fun <T> effectsSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 800f,
    )

    fun offsetSpring(): FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.78f,
        stiffness = 420f,
    )

    fun sizeSpring(): FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 0.8f,
        stiffness = 420f,
    )
}

/**
 * Adds subtle M3E spring press feedback to interactive tonal surfaces.
 */
fun Modifier.m3eBouncyPress(
    interactionSource: MutableInteractionSource? = null,
    pressedScale: Float = 0.96f,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = M3EMotion.fastSpatialSpring(),
        label = "m3e_press",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Staggered M3E reveal entrance for list/grid sections.
 */
@Composable
fun M3EAnimatedEntrance(
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { shown = visible }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(M3EMotion.effectsSpring()) +
            scaleIn(initialScale = 0.95f, animationSpec = M3EMotion.fastSpatialSpring()) +
            expandVertically(animationSpec = M3EMotion.sizeSpring()),
        exit = fadeOut(M3EMotion.effectsSpring()) +
            shrinkVertically(animationSpec = M3EMotion.sizeSpring()),
        modifier = modifier,
    ) {
        content()
    }
}

/**
 * Shared borderless tonal pill search field used inside TopSearchAppBar across tabs.
 */
@Composable
fun TonalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotBlank(),
                enter = fadeIn(M3EMotion.effectsSpring()) + scaleIn(animationSpec = M3EMotion.fastSpatialSpring()),
                exit = fadeOut(M3EMotion.effectsSpring()),
            ) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Xóa tìm kiếm",
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = tonalTextFieldColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Transparent collapsible LargeTopAppBar with spacious expanded top breathing room
 * by default that compresses on scroll, paired with a docked tonal M3 Search Bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSearchAppBar(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    subtitle: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LargeTopAppBar(
            title = {
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!subtitle.isNullOrBlank() && (scrollBehavior == null || scrollBehavior.state.collapsedFraction < 0.5f)) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            actions = { actions() },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            scrollBehavior = scrollBehavior,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 10.dp),
        ) {
            TonalSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = placeholder,
            )
        }
    }
}

/**
 * Shared Material 3 ModalBottomSheet for Smart OTP verification.
 * Displays the incoming OTP code via a live [Snackbar] and dismisses it as soon
 * as OTP verification succeeds or the sheet is closed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationBottomSheet(
    title: String,
    subtitle: String? = null,
    otpCode: String,
    otpInput: String,
    onOtpInputChanged: (String) -> Unit,
    errorMessage: String? = null,
    confirmText: String = "Xác thực",
    onVerify: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(otpCode) {
        snackbarHostState.currentSnackbarData?.dismiss()
        if (otpCode.isNotBlank()) {
            snackbarHostState.showSnackbar(
                message = "Mã Smart OTP gửi đến: $otpCode",
                actionLabel = "Đóng",
                duration = SnackbarDuration.Indefinite,
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            snackbarHostState.currentSnackbarData?.dismiss()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SnackbarHost(hostState = snackbarHostState)

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextField(
                value = otpInput,
                onValueChange = { onOtpInputChanged(it.filter(Char::isDigit).take(6)) },
                label = { Text("Nhập mã OTP 6 chữ số") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                    fontFeatureSettings = "tnum",
                ),
                shape = RoundedCornerShape(16.dp),
                colors = tonalTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!errorMessage.isNullOrBlank()) {
                TonalFeedbackBanner(message = errorMessage, isError = true)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("Hủy", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onVerify,
                    enabled = otpInput.length == 6,
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                    modifier = Modifier
                        .weight(1.45f)
                        .height(52.dp),
                ) {
                    Text(confirmText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Shared docked bottom bar for Flight, Cinema, Hotel, and ME Shop screens.
 * Displays the active selection summary and rounded total, and opens a checkout
 * [ModalBottomSheet] containing the payment form when tapped.
 */
@Composable
fun LifestyleCheckoutBottomBar(
    summaryLabel: String,
    amountVnd: Long,
    buttonText: String = "Thanh toán",
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = summaryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = CurrencyFormatter.formatVnd(amountVnd),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                modifier = Modifier.height(50.dp),
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
