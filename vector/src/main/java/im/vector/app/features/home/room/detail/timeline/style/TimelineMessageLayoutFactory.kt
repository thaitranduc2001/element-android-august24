/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.detail.timeline.style

import im.vector.app.core.extensions.localDateTime
import im.vector.app.features.home.room.detail.timeline.factory.TimelineItemFactoryParams
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.isEdition
import javax.inject.Inject

class TimelineMessageLayoutFactory @Inject constructor(private val session: Session,
                                                       private val layoutSettingsProvider: TimelineLayoutSettingsProvider,
                                                       private val vectorPreferences: VectorPreferences) {

    companion object {
        // Can't be rendered in bubbles, so get back to default layout
        private val EVENT_TYPES_WITH_BUBBLE_LAYOUT = setOf(
                EventType.MESSAGE,
                EventType.POLL_START,
                EventType.ENCRYPTED,
                EventType.STICKER
        )

        // Can't be rendered in bubbles, so get back to default layout
        private val MSG_TYPES_WITHOUT_BUBBLE_LAYOUT = setOf(
                MessageType.MSGTYPE_VERIFICATION_REQUEST
        )

        // Use the bubble layout but without borders
        private val MSG_TYPES_WITH_PSEUDO_BUBBLE_LAYOUT = setOf(
                MessageType.MSGTYPE_IMAGE, MessageType.MSGTYPE_VIDEO, MessageType.MSGTYPE_STICKER_LOCAL
        )
        private val MSG_TYPES_WITH_TIMESTAMP_AS_OVERLAY = setOf(
                MessageType.MSGTYPE_IMAGE, MessageType.MSGTYPE_VIDEO
        )
    }

    fun create(params: TimelineItemFactoryParams): TimelineMessageLayout {
        val event = params.event
        val nextDisplayableEvent = params.nextDisplayableEvent
        val prevDisplayableEvent = params.prevDisplayableEvent
        val isSentByMe = event.root.senderId == session.myUserId

        val date = event.root.localDateTime()
        val nextDate = nextDisplayableEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()

        val isNextMessageReceivedMoreThanOneHourAgo = nextDate?.isBefore(date.minusMinutes(60))
                ?: false

        val showInformation = addDaySeparator ||
                event.senderInfo.avatarUrl != nextDisplayableEvent?.senderInfo?.avatarUrl ||
                event.senderInfo.disambiguatedDisplayName != nextDisplayableEvent?.senderInfo?.disambiguatedDisplayName ||
                nextDisplayableEvent.root.getClearType() !in listOf(EventType.MESSAGE, EventType.STICKER, EventType.ENCRYPTED) ||
                isNextMessageReceivedMoreThanOneHourAgo ||
                isTileTypeMessage(nextDisplayableEvent) ||
                nextDisplayableEvent.isEdition()

        val messageLayout = when (layoutSettingsProvider.getLayoutSettings()) {
            TimelineLayoutSettings.MODERN -> {
                buildModernLayout(showInformation)
            }
            TimelineLayoutSettings.BUBBLE -> {
                val shouldBuildBubbleLayout = event.shouldBuildBubbleLayout()
                if (shouldBuildBubbleLayout) {
                    val isFirstFromThisSender = nextDisplayableEvent == null || !nextDisplayableEvent.shouldBuildBubbleLayout() ||
                            nextDisplayableEvent.root.senderId != event.root.senderId || addDaySeparator

                    val isLastFromThisSender = prevDisplayableEvent == null || !prevDisplayableEvent.shouldBuildBubbleLayout() ||
                            prevDisplayableEvent.root.senderId != event.root.senderId ||
                            prevDisplayableEvent.root.localDateTime().toLocalDate() != date.toLocalDate()

                    val messageContent = event.getLastMessageContent()
                    TimelineMessageLayout.Bubble(
                            showAvatar = showInformation && !isSentByMe,
                            showDisplayName = showInformation && !isSentByMe,
                            isIncoming = !isSentByMe,
                            isFirstFromThisSender = isFirstFromThisSender,
                            isLastFromThisSender = isLastFromThisSender,
                            isPseudoBubble = messageContent?.msgType in MSG_TYPES_WITH_PSEUDO_BUBBLE_LAYOUT,
                            timestampAsOverlay = messageContent?.msgType in MSG_TYPES_WITH_TIMESTAMP_AS_OVERLAY
                    )
                } else {
                    buildModernLayout(showInformation)
                }
            }
        }
        return messageLayout
    }

    private fun TimelineEvent.shouldBuildBubbleLayout(): Boolean {
        val type = root.getClearType()
        if (type in EVENT_TYPES_WITH_BUBBLE_LAYOUT) {
            val messageContent = getLastMessageContent()
            if (messageContent?.msgType in MSG_TYPES_WITHOUT_BUBBLE_LAYOUT) {
                return false
            }
            return true
        }
        return false
    }

    private fun buildModernLayout(showInformation: Boolean): TimelineMessageLayout.Default {
        return TimelineMessageLayout.Default(
                showAvatar = showInformation,
                showDisplayName = showInformation,
                showTimestamp = showInformation || vectorPreferences.alwaysShowTimeStamps()
        )
    }

    /**
     * Tiles type message never show the sender information (like verification request), so we should repeat it for next message
     * even if same sender
     */
    private fun isTileTypeMessage(event: TimelineEvent?): Boolean {
        return when (event?.root?.getClearType()) {
            EventType.KEY_VERIFICATION_DONE,
            EventType.KEY_VERIFICATION_CANCEL -> true
            EventType.MESSAGE                 -> {
                event.getLastMessageContent() is MessageVerificationRequestContent
            }
            else                              -> false
        }
    }
}
