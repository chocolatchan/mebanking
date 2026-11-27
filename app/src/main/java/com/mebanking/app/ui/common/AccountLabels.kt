package com.mebanking.app.ui.common

import com.mebanking.app.domain.model.AccountType

/**
 * Shared Vietnamese display labels for account types.
 * Kept in one place so every screen presents the same wording.
 */
internal fun AccountType.label(): String = vietnameseName()
