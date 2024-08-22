/*
 * Copyright (c) 2024 New Vector Ltd
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

package im.vector.app.features.crypto.verification

import android.annotation.SuppressLint
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation


import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import im.vector.app.R

class StaticEmojiVerificationDialog(
        context: Context,
        private val staticEmojis: List<EmojiRepresentation>,
        private val onMarkAsVerified: () -> Unit
) : Dialog(context) {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_static_emoji_verification, null)
        setContentView(view)

        val emojiContainer = view.findViewById<LinearLayout>(R.id.emoji_container)
        staticEmojis.forEach { emoji ->
            val emojiView = LayoutInflater.from(context).inflate(R.layout.item_emoji, emojiContainer, false)
            emojiView.findViewById<TextView>(R.id.emoji_text).text = emoji.emoji
            emojiView.findViewById<TextView>(R.id.emoji_description).text = emoji.description
            emojiContainer.addView(emojiView)
        }

        view.findViewById<View>(R.id.mark_as_verified_button).setOnClickListener {
            onMarkAsVerified()
            dismiss()
        }
    }
}
