package com.mebanking.app.di

import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.ui.home.HomeViewModel
import com.mebanking.app.ui.navigation.BranchLocatorViewModel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewModelFactoryTest {

    private lateinit var container: AppContainer
    private lateinit var factory: ViewModelFactory

    private class UnknownViewModel : ViewModel()

    @Before
    fun setUp() {
        container = DefaultAppContainer(ApplicationProvider.getApplicationContext())
        factory = ViewModelFactory(container)
    }

    @Test
    fun create_homeViewModel_succeeds() {
        val vm = factory.create(HomeViewModel::class.java)
        assertNotNull(vm)
    }

    @Test
    fun create_branchLocatorViewModel_succeeds() {
        val vm = factory.create(BranchLocatorViewModel::class.java)
        assertNotNull(vm)
    }

    @Test
    fun create_unknownViewModel_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }

    @Test
    fun create_returnsNewInstanceForEachCall() {
        val vm1 = factory.create(HomeViewModel::class.java)
        val vm2 = factory.create(HomeViewModel::class.java)
        assertNotSame(vm1, vm2)
    }
}
