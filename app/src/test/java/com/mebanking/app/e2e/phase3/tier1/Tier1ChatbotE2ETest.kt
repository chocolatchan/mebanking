package com.mebanking.app.e2e.phase3.tier1

import com.mebanking.app.e2e.phase3.harness.BasePhase3E2ETest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 1 Equivalence Class 6: Offline Rule-Based Chatbot
 * Total: 5 test cases
 */
class Tier1ChatbotE2ETest : BasePhase3E2ETest() {

    @Test
    fun testChatbotBalanceIntentMatching() = runTest {
        val response = container.chatbotService.processQuery("kiểm tra số dư tài khoản của tôi")
        assertEquals("check_balance", response.intentCode)
        assertEquals("home", response.targetScreen)
        assertTrue(response.responseText.isNotBlank())
    }

    @Test
    fun testChatbotTransferIntentMatching() = runTest {
        val response = container.chatbotService.processQuery("tôi muốn chuyển tiền cho bạn bè")
        assertEquals("transfer_money", response.intentCode)
        assertEquals("transfer", response.targetScreen)
        assertTrue(response.responseText.isNotBlank())
    }

    @Test
    fun testChatbotBillPaymentIntentMatching() = runTest {
        val response = container.chatbotService.processQuery("thanh toán hóa đơn tiền điện tháng này")
        assertEquals("pay_bills", response.intentCode)
        assertEquals("bills", response.targetScreen)
        assertTrue(response.responseText.isNotBlank())
    }

    @Test
    fun testChatbotNearestAtmIntentMatching() = runTest {
        val response = container.chatbotService.processQuery("tìm cây atm gần nhất ở đâu")
        assertEquals("nearest_atm", response.intentCode)
        assertEquals("branches", response.targetScreen)
        assertTrue(response.responseText.isNotBlank())
    }

    @Test
    fun testChatbotUnknownQueryFallback() = runTest {
        val response = container.chatbotService.processQuery("thời tiết hôm nay thế nào hả bot")
        assertEquals("unknown_intent", response.intentCode)
        assertEquals("home", response.targetScreen)
        assertTrue("Fallback response must be non-empty", response.responseText.isNotBlank())
    }
}
