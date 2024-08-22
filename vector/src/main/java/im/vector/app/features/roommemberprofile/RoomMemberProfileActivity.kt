/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package im.vector.app.features.roommemberprofile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.crypto.verification.StaticEmojiVerificationDialog
import im.vector.app.features.room.RequireActiveMembershipViewEvents
import im.vector.app.features.room.RequireActiveMembershipViewModel
import im.vector.lib.core.utils.compat.getParcelableCompat
import org.matrix.android.sdk.api.crypto.getAllVerificationEmojis
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import javax.inject.Inject

@AndroidEntryPoint
class RoomMemberProfileActivity : VectorBaseActivity<ActivitySimpleBinding>(), RoomMemberProfileController.Callback {

    companion object {
        fun newIntent(context: Context, args: RoomMemberProfileArgs): Intent {
            return Intent(context, RoomMemberProfileActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, args)
            }
        }
    }

    @Inject lateinit var session: Session
    @Inject lateinit var stringProvider: StringProvider
    private val requireActiveMembershipViewModel: RequireActiveMembershipViewModel by viewModel()
    private lateinit var controller: RoomMemberProfileController

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize controller and other setup code...
        controller = RoomMemberProfileController(stringProvider, session)
        controller.callback = this
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val fragmentArgs: RoomMemberProfileArgs = intent?.extras?.getParcelableCompat(Mavericks.KEY_ARG) ?: return
            addFragment(views.simpleFragmentContainer, RoomMemberProfileFragment::class.java, fragmentArgs)
        }

        requireActiveMembershipViewModel.observeViewEvents {
            when (it) {
                is RequireActiveMembershipViewEvents.RoomLeft -> handleRoomLeft(it)
            }
        }
    }

    private fun handleRoomLeft(roomLeft: RequireActiveMembershipViewEvents.RoomLeft) {
        if (roomLeft.leftMessage != null) {
            Toast.makeText(this, roomLeft.leftMessage, Toast.LENGTH_LONG).show()
        }
        finish()
    }

    override fun onIgnoreClicked() {
        TODO("Not yet implemented")
    }

    override fun onReportClicked() {
        TODO("Not yet implemented")
    }

    override fun onTapVerify() {
        TODO("Not yet implemented")
    }

    override fun onShowDeviceList() {
        TODO("Not yet implemented")
    }

    override fun onShowDeviceListNoCrossSigning() {
        TODO("Not yet implemented")
    }

    override fun onOpenDmClicked() {
        TODO("Not yet implemented")
    }

    override fun onOverrideColorClicked() {
        TODO("Not yet implemented")
    }

    override fun onJumpToReadReceiptClicked() {
        TODO("Not yet implemented")
    }

    override fun onMentionClicked() {
        TODO("Not yet implemented")
    }

    override fun onEditPowerLevel(currentRole: Role) {
        TODO("Not yet implemented")
    }

    override fun onKickClicked(isSpace: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onBanClicked(isSpace: Boolean, isUserBanned: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onCancelInviteClicked() {
        TODO("Not yet implemented")
    }

    override fun onInviteClicked() {
        TODO("Not yet implemented")
    }

    override fun onShowStaticEmojiVerification() {
        val staticEmojis = generateStaticEmojis() // Implement this method to generate the static emojis
        StaticEmojiVerificationDialog(this, staticEmojis) {
            // Handle mark as verified action
        }.show()
    }

    // Other callback methods...

    private fun generateStaticEmojis(): List<EmojiRepresentation> {
        // Get the pool of all possible emojis
        val emojiPool = getAllVerificationEmojis()

        // Shuffle the pool and take the first 7 emojis
        return emojiPool.shuffled().take(7)
    }

}
