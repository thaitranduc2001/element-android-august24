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

package im.vector.app.features.crypto.verification.user

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.databinding.BottomSheetVerificationBinding
import im.vector.app.features.crypto.verification.VerificationAction
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewEvents
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * Specific to other users verification (not self verification).
 */
@AndroidEntryPoint
class UserVerificationBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetVerificationBinding>() {
    @Parcelize
    data class Args(
            val otherUserId: String,
            val verificationId: String? = null,
            // user verifications happen in DMs
            val roomId: String? = null,
    ) : Parcelable

    override val showExpanded = true

    @Inject
    lateinit var avatarRenderer: AvatarRenderer

    private val viewModel by fragmentViewModel(UserVerificationViewModel::class)
    private lateinit var staticEmojis: List<EmojiRepresentation>

    init {
        // we manage dismiss/back manually to confirm cancel on verification
        isCancelable = false
    }

    override fun getBinding(
            inflater: LayoutInflater,
            container: ViewGroup?
    ) = BottomSheetVerificationBinding.inflate(inflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showFragment(UserVerificationFragment::class)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_verification_bottom_sheet, container, false)

        withState(viewModel) { state ->
            staticEmojis = state.staticEmojis ?: emptyList()
        }

        displayStaticEmojis(view, staticEmojis)

        view.findViewById<Button>(R.id.mark_as_verified_button).setOnClickListener {
            viewModel.handle(VerificationAction.MarkSessionAsVerified)
        }

        return view
    }

    private fun displayStaticEmojis(view: View, emojis: List<EmojiRepresentation>) {
        val emojiContainer = view.findViewById<LinearLayout>(R.id.emoji_container)
        emojiContainer.removeAllViews()

        emojis.forEach { emoji ->
            val emojiView = LayoutInflater.from(context).inflate(R.layout.item_emoji, emojiContainer, false)
            emojiView.findViewById<TextView>(R.id.emoji_text).text = emoji.emoji
            emojiView.findViewById<TextView>(R.id.emoji_description).text = emoji.description
            emojiContainer.addView(emojiView)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.observeViewEvents { event ->
            when (event) {
                VerificationBottomSheetViewEvents.AccessSecretStore -> {
                    // nop for user verification?
                }
                VerificationBottomSheetViewEvents.Dismiss -> {
                    dismiss()
                }
                VerificationBottomSheetViewEvents.GoToSettings -> {
                    // nop for user verificaiton
                }
                is VerificationBottomSheetViewEvents.ModalError -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(CommonStrings.dialog_title_error))
                            .setMessage(event.errorMessage)
                            .setCancelable(false)
                            .setPositiveButton(CommonStrings.ok, null)
                            .show()
                }
                VerificationBottomSheetViewEvents.ResetAll,
                VerificationBottomSheetViewEvents.DismissAndOpenDeviceSettings -> {
                    // no-op for user verification
                }
                is VerificationBottomSheetViewEvents.RequestNotFound -> {
                    // no-op for user verification
                }
                is VerificationBottomSheetViewEvents.ConfirmCancel -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(CommonStrings.dialog_title_confirmation))
                            .setMessage(
                                    getString(CommonStrings.verify_cancel_other, event.otherUserId, event.deviceId ?: "*")
                                            .toSpannable()
                                            .colorizeMatchingText(
                                                    event.otherUserId,
                                                    ThemeUtils.getColor(requireContext(), im.vector.lib.ui.styles.R.attr.vctr_notice_text_color)
                                            )
                            )
                            .setCancelable(false)
                            .setPositiveButton(CommonStrings._resume, null)
                            .setNegativeButton(CommonStrings.action_cancel) { _, _ ->
                                viewModel.handle(VerificationAction.CancelPendingVerification)
                            }
                            .show()
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnKeyListener { _, keyCode, keyEvent ->
                if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
                    viewModel.queryCancel()
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        avatarRenderer.render(state.otherUserMxItem, views.otherUserAvatarImageView)
        views.otherUserNameText.text = getString(CommonStrings.verification_verify_user, state.otherUserMxItem.getBestName())
        views.otherUserShield.render(
                if (state.otherUserIsTrusted) RoomEncryptionTrustLevel.Trusted
                else RoomEncryptionTrustLevel.Default
        )
        super.invalidate()
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>, argsParcelable: Parcelable? = null) {
        if (childFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            childFragmentManager.commitTransaction {
                replace(
                        R.id.bottomSheetFragmentContainer,
                        fragmentClass.java,
                        argsParcelable?.toMvRxBundle(),
                        fragmentClass.simpleName
                )
            }
        }
    }

    companion object {
        fun verifyUser(otherUserId: String, transactionId: String? = null): UserVerificationBottomSheet {
            return UserVerificationBottomSheet().apply {
                setArguments(
                        Args(
                                otherUserId = otherUserId,
                                verificationId = transactionId
                        )
                )
            }
        }
    }
}
