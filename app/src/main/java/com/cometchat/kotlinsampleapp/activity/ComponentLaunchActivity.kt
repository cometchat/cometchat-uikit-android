package com.cometchat.kotlinsampleapp.activity

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R
import com.cometchat.kotlinsampleapp.databinding.ActivityComponentLaunchBinding
import com.cometchat.kotlinsampleapp.fragments.calls.CallButtonFragment
import com.cometchat.kotlinsampleapp.fragments.calls.CallLogDetailsFragment
import com.cometchat.kotlinsampleapp.fragments.calls.CallLogHistoryFragment
import com.cometchat.kotlinsampleapp.fragments.calls.CallLogParticipantsFragment
import com.cometchat.kotlinsampleapp.fragments.calls.CallLogRecordingFragment
import com.cometchat.kotlinsampleapp.fragments.calls.CallLogWithDetailsFragment
import com.cometchat.kotlinsampleapp.fragments.calls.CallLogsFragment
import com.cometchat.kotlinsampleapp.fragments.conversations.ContactsFragment
import com.cometchat.kotlinsampleapp.fragments.conversations.ConversationsFragment
import com.cometchat.kotlinsampleapp.fragments.conversations.ConversationsWithMessagesFragment
import com.cometchat.kotlinsampleapp.fragments.groups.AddMemberFragment
import com.cometchat.kotlinsampleapp.fragments.groups.BannedMembersFragment
import com.cometchat.kotlinsampleapp.fragments.groups.CreateGroupFragment
import com.cometchat.kotlinsampleapp.fragments.groups.GroupDetailsFragment
import com.cometchat.kotlinsampleapp.fragments.groups.GroupMembersFragment
import com.cometchat.kotlinsampleapp.fragments.groups.GroupsFragment
import com.cometchat.kotlinsampleapp.fragments.groups.GroupsWithMessagesFragment
import com.cometchat.kotlinsampleapp.fragments.groups.JoinProtectedGroupFragment
import com.cometchat.kotlinsampleapp.fragments.groups.TransferOwnershipFragment
import com.cometchat.kotlinsampleapp.fragments.messages.MessageComposerFragment
import com.cometchat.kotlinsampleapp.fragments.messages.MessageHeaderFragment
import com.cometchat.kotlinsampleapp.fragments.messages.MessageInformationFragment
import com.cometchat.kotlinsampleapp.fragments.messages.MessageListFragment
import com.cometchat.kotlinsampleapp.fragments.messages.MessagesFragment
import com.cometchat.kotlinsampleapp.fragments.shared.resources.LocalizeFragment
import com.cometchat.kotlinsampleapp.fragments.shared.resources.SoundManagerFragment
import com.cometchat.kotlinsampleapp.fragments.shared.resources.ThemeFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.AudioBubbleFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.AvatarFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.BadgeCountFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.CardBubbleFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.FileBubbleFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.FormBubbleFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.ImageBubbleFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.ListItemFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.MediaRecorderFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.MessageReceiptFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.SchedulerBubbleFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.StatusIndicatorFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.TextBubbleFragment
import com.cometchat.kotlinsampleapp.fragments.shared.views.VideoBubbleFragment
import com.cometchat.kotlinsampleapp.fragments.users.UserDetailsFragment
import com.cometchat.kotlinsampleapp.fragments.users.UsersFragment
import com.cometchat.kotlinsampleapp.fragments.users.UsersWithMessagesFragment

class ComponentLaunchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityComponentLaunchBinding
    private lateinit var parentView: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComponentLaunchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val id = intent.getIntExtra("component", 0)
        parentView = binding.container
        setUpUI()
        when (id) {
            R.id.conversationWithMessages -> {
                loadFragment(ConversationsWithMessagesFragment())
            }

            R.id.conversations -> {
                loadFragment(ConversationsFragment())
            }

            R.id.userWithMessages -> {
                loadFragment(UsersWithMessagesFragment())
            }

            R.id.users -> {
                loadFragment(UsersFragment())
            }

            R.id.user_details -> {
                loadFragment(UserDetailsFragment())
            }

            R.id.groupWithMessages -> {
                loadFragment(GroupsWithMessagesFragment())
            }

            R.id.call_button -> {
                loadFragment(CallButtonFragment())
            }

            R.id.groups -> {
                loadFragment(GroupsFragment())
            }

            R.id.create_group -> {
                loadFragment(CreateGroupFragment())
            }

            R.id.join_protected_group -> {
                loadFragment(JoinProtectedGroupFragment())
            }

            R.id.group_member -> {
                loadFragment(GroupMembersFragment())
            }

            R.id.add_member -> {
                loadFragment(AddMemberFragment())
            }

            R.id.transfer_ownership -> {
                loadFragment(TransferOwnershipFragment())
            }

            R.id.banned_members -> {
                loadFragment(BannedMembersFragment())
            }

            R.id.group_details -> {
                loadFragment(GroupDetailsFragment())
            }

            R.id.messages -> {
                loadFragment(MessagesFragment())
            }

            R.id.messageList -> {
                loadFragment(MessageListFragment())
            }

            R.id.messageHeader -> {
                loadFragment(MessageHeaderFragment())
            }

            R.id.messageComposer -> {
                loadFragment(MessageComposerFragment())
            }

            R.id.avatar -> {
                loadFragment(AvatarFragment())
            }

            R.id.badgeCount -> {
                loadFragment(BadgeCountFragment())
            }

            R.id.messageReceipt -> {
                loadFragment(MessageReceiptFragment())
            }

            R.id.statusIndicator -> {
                loadFragment(StatusIndicatorFragment())
            }

            R.id.soundManager -> {
                loadFragment(SoundManagerFragment())
            }

            R.id.theme -> {
                loadFragment(ThemeFragment())
            }

            R.id.localize -> {
                loadFragment(LocalizeFragment())
            }

            R.id.list_item -> {
                loadFragment(ListItemFragment())
            }

            R.id.text_bubble -> {
                loadFragment(TextBubbleFragment())
            }

            R.id.image_bubble -> {
                loadFragment(ImageBubbleFragment())
            }

            R.id.video_bubble -> {
                loadFragment(VideoBubbleFragment())
            }

            R.id.audio_bubble -> {
                loadFragment(AudioBubbleFragment())
            }

            R.id.files_bubble -> {
                loadFragment(FileBubbleFragment())
            }

            R.id.form_bubble -> {
                loadFragment(FormBubbleFragment())
            }

            R.id.card_bubble -> {
                loadFragment(CardBubbleFragment())
            }

            R.id.scheduler_bubble -> {
                loadFragment(SchedulerBubbleFragment())
            }

            R.id.media_recorder -> {
                loadFragment(MediaRecorderFragment())
            }

            R.id.contacts -> {
                loadFragment(ContactsFragment())
            }

            R.id.messageInformation -> {
                loadFragment(MessageInformationFragment())
            }

            R.id.call_logs -> {
                loadFragment(CallLogsFragment())
            }

            R.id.call_logs_details -> {
                loadFragment(CallLogDetailsFragment())
            }

            R.id.call_logs_with_details -> {
                loadFragment(CallLogWithDetailsFragment())
            }

            R.id.call_log_participants -> {
                loadFragment(CallLogParticipantsFragment())
            }

            R.id.call_log_recording -> {
                loadFragment(CallLogRecordingFragment())
            }

            R.id.call_log_history -> {
                loadFragment(CallLogHistoryFragment())
            }
        }
    }

    private fun setUpUI() {
        if (AppUtils.isNightMode(this)) {
            Utils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.app_background_dark))
            parentView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.app_background_dark
                )
            )
        } else {
            Utils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.app_background))
            parentView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.app_background
                )
            )
        }
    }

    private fun loadFragment(fragment: Fragment?) {
        if (fragment != null) {
            supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
        }
    }
}