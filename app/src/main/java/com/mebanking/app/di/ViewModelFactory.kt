package com.mebanking.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mebanking.app.ui.home.HomeViewModel
import com.mebanking.app.ui.navigation.BranchLocatorViewModel

class ViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    repository = container.accountRepository,
                ) as T
            }
            modelClass.isAssignableFrom(BranchLocatorViewModel::class.java) -> {
                BranchLocatorViewModel(
                    repository = container.branchNavigationRepository,
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
