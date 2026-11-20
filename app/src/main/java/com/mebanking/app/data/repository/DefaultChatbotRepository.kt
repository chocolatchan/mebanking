package com.mebanking.app.data.repository

import com.mebanking.app.data.local.dao.ChatIntentDao
import com.mebanking.app.data.local.entity.ChatIntentEntity
import com.mebanking.app.domain.model.ChatIntent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Default Room-backed implementation of [ChatbotRepository].
 */
class DefaultChatbotRepository(
    private val chatIntentDao: ChatIntentDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatbotRepository {

    override fun getAllIntentsOrderedByPriority(): Flow<List<ChatIntent>> {
        return chatIntentDao.getAllOrderedByPriority()
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getAllIntentsSync(): List<ChatIntent> = withContext(ioDispatcher) {
        chatIntentDao.getIntentsOrderedByPriority().map { it.toDomainModel() }
    }

    override suspend fun getIntentByCode(intentCode: String): ChatIntent? = withContext(ioDispatcher) {
        chatIntentDao.getByIntentCode(intentCode)?.toDomainModel()
    }

    override suspend fun insertIntent(intent: ChatIntent): Long = withContext(ioDispatcher) {
        chatIntentDao.insert(ChatIntentEntity.fromDomainModel(intent))
    }

    override suspend fun count(): Int = withContext(ioDispatcher) {
        chatIntentDao.count()
    }
}
