package io.github.cotrin1208.service

import com.google.cloud.datastore.Key
import io.github.cotrin1208.model.message.PushMessage
import io.github.cotrin1208.model.webhook.Source
import io.github.cotrin1208.model.webhook.WebhookEvent
import io.github.cotrin1208.repository.IDatastoreRepository
import io.github.cotrin1208.repository.ILineApiRepository
import io.github.cotrin1208.util.*

class WebhookService(
    private val datastoreRepository: IDatastoreRepository,
    private val lineApiRepository: ILineApiRepository,
) : IWebhookService {
    override suspend fun onFollowEvent(event: WebhookEvent.Follow) {
        val source = event.source
        if (source is Source.User) {
            val userProfile = lineApiRepository.getUserProfile(source.userId)
            println(userProfile.displayName)
            val key = datastoreRepository.createKey(KindName.USER_KIND, userProfile.displayName)
            datastoreRepository.createEntity(key) {
                set(PropertyName.FRIDAY_MORNING_RESPONDED, false)
                set(PropertyName.FRIDAY_EVENING_RESPONDED, false)
                set(PropertyName.SUNDAY_MORNING_RESPONDED, false)
                set(PropertyName.USER_ID, source.userId)
            }
        }
    }

    override suspend fun onPostbackEvent(event: WebhookEvent.Postback) {
        val source = event.source
        if (source !is Source.User) return

        val user =
            datastoreRepository.queryEntitiesWithPropertyName(KindName.USER_KIND, PropertyName.USER_ID, source.userId)
                .first()
        when (event.postback.data) {
            ActionData.FRIDAY_MORNING_RESPONDED -> {
                if (user.getBoolean(PropertyName.FRIDAY_MORNING_RESPONDED)) return
                updateRespondedFlag(user.key, PropertyName.FRIDAY_MORNING_RESPONDED)
                sendRandomStickerMessage(source.userId)
            }

            ActionData.FRIDAY_EVENING_RESPONDED -> {
                if (user.getBoolean(PropertyName.FRIDAY_EVENING_RESPONDED)) return
                updateRespondedFlag(user.key, PropertyName.FRIDAY_EVENING_RESPONDED)
                sendRandomStickerMessage(source.userId)
            }

            ActionData.SUNDAY_MORNING_RESPONDED -> {
                if (user.getBoolean(PropertyName.SUNDAY_MORNING_RESPONDED)) return
                updateRespondedFlag(user.key, PropertyName.SUNDAY_MORNING_RESPONDED)
                sendRandomStickerMessage(source.userId)
            }
        }
    }

    private fun updateRespondedFlag(key: Key, flagName: String) {
        datastoreRepository.updateParameters(key) {
            set(flagName, true)
        }
    }

    private suspend fun sendRandomStickerMessage(to: String) {
        lineApiRepository.sendPushMessage(
            PushMessage(
                to = to,
                messages = Stickers.random().asList()
            )
        )
    }
}
