package com.mebanking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mebanking.app.domain.model.ChatIntent

/**
 * Room entity representing an offline chatbot intent pattern and response.
 */
@Entity(
    tableName = "chat_intents",
    indices = [
        Index(value = ["intent_code"], unique = true),
    ],
)
data class ChatIntentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "intent_code")
    val intentCode: String,

    @ColumnInfo(name = "patterns")
    val patterns: List<String>,

    @ColumnInfo(name = "response_template")
    val responseTemplate: String,

    @ColumnInfo(name = "target_screen")
    val targetScreen: String,

    @ColumnInfo(name = "priority")
    val priority: Int = 0,
) {
    fun toDomainModel(): ChatIntent {
        return ChatIntent(
            id = id,
            intentCode = intentCode,
            patterns = patterns,
            responseTemplate = responseTemplate,
            targetScreen = targetScreen,
            priority = priority,
        )
    }

    companion object {
        fun fromDomainModel(intent: ChatIntent): ChatIntentEntity {
            return ChatIntentEntity(
                id = intent.id,
                intentCode = intent.intentCode,
                patterns = intent.patterns,
                responseTemplate = intent.responseTemplate,
                targetScreen = intent.targetScreen,
                priority = intent.priority,
            )
        }
    }
}
