package com.cometchat.kotlinsampleapp.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.cometchat.kotlinsampleapp.AppUtils.Companion.changeIconTintToBlack
import com.cometchat.kotlinsampleapp.AppUtils.Companion.changeIconTintToWhite
import com.cometchat.kotlinsampleapp.AppUtils.Companion.isNightMode
import com.cometchat.kotlinsampleapp.R
import com.cometchat.kotlinsampleapp.constants.StringConstants
import com.cometchat.kotlinsampleapp.databinding.ActivityComponentListBinding

class ComponentListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityComponentListBinding
    private lateinit var parentView: LinearLayout
    private lateinit var title: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComponentListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        parentView = binding.parentView
        title = binding.title
        setUpUI()
        if (intent != null) {
            title.text = intent.getStringExtra(StringConstants.MODULE)
            if (intent.getStringExtra(StringConstants.MODULE)
                    .equals(StringConstants.CONVERSATIONS, ignoreCase = true)
            ) {
                findViewById<View>(R.id.moduleChats).visibility = View.VISIBLE
            } else if (intent.getStringExtra(StringConstants.MODULE)
                    .equals(StringConstants.USERS, ignoreCase = true)
            ) {
                findViewById<View>(R.id.moduleUsers).visibility = View.VISIBLE
            } else if (intent.getStringExtra(StringConstants.MODULE)
                    .equals(StringConstants.GROUPS, ignoreCase = true)
            ) {
                findViewById<View>(R.id.noduleGroups).visibility = View.VISIBLE
            } else if (intent.getStringExtra(StringConstants.MODULE)
                    .equals(StringConstants.MESSAGES, ignoreCase = true)
            ) {
                findViewById<View>(R.id.moduleMessages).visibility = View.VISIBLE
            } else if (intent.getStringExtra(StringConstants.MODULE)
                    .equals(StringConstants.SHARED, ignoreCase = true)
            ) {
                findViewById<View>(R.id.shared).visibility = View.VISIBLE
            } else if (intent.getStringExtra(StringConstants.MODULE)
                    .equals(StringConstants.CALLS, ignoreCase = true)
            ) {
                findViewById<View>(R.id.module_calls).visibility = View.VISIBLE
            }
        }

        //back
        binding.backIcon.setOnClickListener { onBackPressed() }

        //chats
        binding.conversationWithMessages.setOnClickListener {
            handleIntent(
                R.id.conversationWithMessages
            )
        }
        binding.conversations.setOnClickListener {
            handleIntent(
                R.id.conversations
            )
        }
        binding.contacts.setOnClickListener {
            handleIntent(
                R.id.contacts
            )
        }

        //users
        binding.userWithMessages.setOnClickListener {
            handleIntent(
                R.id.userWithMessages
            )
        }
        binding.users.setOnClickListener {
            handleIntent(
                R.id.users
            )
        }
        binding.userDetails.setOnClickListener {
            handleIntent(
                R.id.user_details
            )
        }

        //groups
        binding.groupWithMessages.setOnClickListener {
            handleIntent(R.id.groupWithMessages)
        }
        binding.groups.setOnClickListener {
            handleIntent(R.id.groups)
        }
        binding.createGroup.setOnClickListener {
            handleIntent(R.id.create_group)
        }
        binding.joinProtectedGroup.setOnClickListener {
            handleIntent(R.id.join_protected_group)
        }
        binding.groupMember.setOnClickListener {
            handleIntent(R.id.group_member)
        }
        binding.addMember.setOnClickListener {
            handleIntent(R.id.add_member)
        }
        binding.transferOwnership.setOnClickListener {
            handleIntent(R.id.transfer_ownership)
        }
        binding.bannedMembers.setOnClickListener {
            handleIntent(R.id.banned_members)
        }
        binding.groupDetails.setOnClickListener {
            handleIntent(R.id.group_details)
        }

        //messages
        binding.messages.setOnClickListener {
            handleIntent(R.id.messages)
        }
        binding.messageList.setOnClickListener {
            handleIntent(R.id.messageList)
        }
        binding.messageHeader.setOnClickListener {
            handleIntent(R.id.messageHeader)
        }
        binding.messageComposer.setOnClickListener {
            handleIntent(R.id.messageComposer)
        }
        binding.messageInformation.setOnClickListener {
            handleIntent(R.id.messageInformation)
        }

        //calls
        binding.callButton.setOnClickListener {
            handleIntent(R.id.call_button)
        }
        binding.callLogs.setOnClickListener {
            handleIntent(R.id.call_logs)
        }
        binding.callLogsDetails.setOnClickListener {
            handleIntent(R.id.call_logs_details)
        }
        binding.callLogsWithDetails.setOnClickListener {
            handleIntent(R.id.call_logs_with_details)
        }
        binding.callLogParticipants.setOnClickListener {
            handleIntent(R.id.call_log_participants)
        }
        binding.callLogRecording.setOnClickListener {
            handleIntent(R.id.call_log_recording)
        }
        binding.callLogHistory.setOnClickListener {
            handleIntent(R.id.call_log_history)
        }

        //shared
        //views
        binding.avatar.setOnClickListener {
            handleIntent(R.id.avatar)
        }
        binding.badgeCount.setOnClickListener {
            handleIntent(R.id.badgeCount)
        }
        binding.messageReceipt.setOnClickListener {
            handleIntent(R.id.messageReceipt)
        }
        binding.statusIndicator.setOnClickListener {
            handleIntent(R.id.statusIndicator)
        }
        binding.listItem.setOnClickListener {
            handleIntent(R.id.list_item)
        }
        binding.textBubble.setOnClickListener {
            handleIntent(R.id.text_bubble)
        }
        binding.imageBubble.setOnClickListener {
            handleIntent(R.id.image_bubble)
        }
        binding.videoBubble.setOnClickListener {
            handleIntent(R.id.video_bubble)
        }
        binding.audioBubble.setOnClickListener {
            handleIntent(R.id.audio_bubble)
        }
        binding.filesBubble.setOnClickListener {
            handleIntent(R.id.files_bubble)
        }
        binding.formBubble.setOnClickListener {
            handleIntent(R.id.form_bubble)
        }
        binding.cardBubble.setOnClickListener {
            handleIntent(R.id.card_bubble)
        }
        binding.schedulerBubble.setOnClickListener {
            handleIntent(R.id.scheduler_bubble)
        }
        binding.mediaRecorder.setOnClickListener {
            handleIntent(R.id.media_recorder)
        }

        //resources
        binding.soundManager.setOnClickListener {
            handleIntent(R.id.soundManager)
        }
        binding.theme.setOnClickListener {
            handleIntent(R.id.theme)
        }
        binding.localize.setOnClickListener {
            handleIntent(R.id.localize)
        }

    }

    private fun setUpUI() {
        if (isNightMode(this)) {
            changeIconTintToWhite(this, binding.backIcon)
            changeIconTintToWhite(this, binding.imageCwm)
            changeIconTintToWhite(this, binding.imageC)
            changeIconTintToWhite(this, binding.imageContacts)
            changeIconTintToWhite(this, binding.imageUwm)
            changeIconTintToWhite(this, binding.imageU)
            changeIconTintToWhite(this, binding.imageUd)
            changeIconTintToWhite(this, binding.imageGwm)
            changeIconTintToWhite(this, binding.imageG)
            changeIconTintToWhite(this, binding.imageCg)
            changeIconTintToWhite(this, binding.imageJp)
            changeIconTintToWhite(this, binding.imageGm)
            changeIconTintToWhite(this, binding.imageAd)
            changeIconTintToWhite(this, binding.imageTo)
            changeIconTintToWhite(this, binding.imageBm)
            changeIconTintToWhite(this, binding.imageGd)
            changeIconTintToWhite(this, binding.imageMessage)
            changeIconTintToWhite(this, binding.imageMessageHeader)
            changeIconTintToWhite(this, binding.imageMessageList)
            changeIconTintToWhite(this, binding.imageMessageComposer)
            changeIconTintToWhite(this, binding.imageMessageInformation)
            changeIconTintToWhite(this, binding.imageCallButton)
            changeIconTintToWhite(this, binding.imageAudio)
            changeIconTintToWhite(this, binding.imageTranslate)
            changeIconTintToWhite(this, binding.imageAvatar)
            changeIconTintToWhite(this, binding.imageBadgeCount)
            changeIconTintToWhite(this, binding.imageMessageReceipt)
            changeIconTintToWhite(this, binding.imageStatusIndicator)
            changeIconTintToWhite(this, binding.imageTextBubble)
            changeIconTintToWhite(this, binding.imageImageBubble)
            changeIconTintToWhite(this, binding.imageVideoBubble)
            changeIconTintToWhite(this, binding.imageAudioBubble)
            changeIconTintToWhite(this, binding.imageFileBubble)
            changeIconTintToWhite(this, binding.imageFormBubble)
            changeIconTintToWhite(this, binding.imageCardBubble)
            changeIconTintToWhite(this, binding.imageSchedulerBubble)
            changeIconTintToWhite(this, binding.imageMic)
            changeIconTintToWhite(this, binding.imageListItem)
            Utils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.app_background_dark))
            parentView.setBackgroundColor(
                ContextCompat.getColor(
                    this, R.color.app_background_dark
                )
            )
        } else {
            changeIconTintToBlack(this, binding.backIcon)
            changeIconTintToBlack(this, binding.imageCwm)
            changeIconTintToBlack(this, binding.imageC)
            changeIconTintToBlack(this, binding.imageContacts)
            changeIconTintToBlack(this, binding.imageUwm)
            changeIconTintToBlack(this, binding.imageU)
            changeIconTintToBlack(this, binding.imageUd)
            changeIconTintToBlack(this, binding.imageGwm)
            changeIconTintToBlack(this, binding.imageG)
            changeIconTintToBlack(this, binding.imageCg)
            changeIconTintToBlack(this, binding.imageJp)
            changeIconTintToBlack(this, binding.imageGm)
            changeIconTintToBlack(this, binding.imageAd)
            changeIconTintToBlack(this, binding.imageTo)
            changeIconTintToBlack(this, binding.imageBm)
            changeIconTintToBlack(this, binding.imageGd)
            changeIconTintToBlack(this, binding.imageMessage)
            changeIconTintToBlack(this, binding.imageMessageHeader)
            changeIconTintToBlack(this, binding.imageMessageList)
            changeIconTintToBlack(this, binding.imageMessageComposer)
            changeIconTintToBlack(this, binding.imageMessageInformation)
            changeIconTintToBlack(this, binding.imageCallButton)
            changeIconTintToBlack(this, binding.imageAudio)
            changeIconTintToBlack(this, binding.imageTranslate)
            changeIconTintToBlack(this, binding.imageAvatar)
            changeIconTintToBlack(this, binding.imageBadgeCount)
            changeIconTintToBlack(this, binding.imageMessageReceipt)
            changeIconTintToBlack(this, binding.imageStatusIndicator)
            changeIconTintToBlack(this, binding.imageTextBubble)
            changeIconTintToBlack(this, binding.imageImageBubble)
            changeIconTintToBlack(this, binding.imageVideoBubble)
            changeIconTintToBlack(this, binding.imageAudioBubble)
            changeIconTintToBlack(this, binding.imageFileBubble)
            changeIconTintToBlack(this, binding.imageFormBubble)
            changeIconTintToBlack(this, binding.imageCardBubble)
            changeIconTintToBlack(this, binding.imageSchedulerBubble)
            changeIconTintToBlack(this, binding.imageMic)
            changeIconTintToBlack(this, binding.imageListItem)
            Utils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.app_background))
            parentView.setBackgroundColor(
                ContextCompat.getColor(
                    this, R.color.app_background
                )
            )
        }
    }

    private fun handleIntent(id: Int) {
        val intent = Intent(this, ComponentLaunchActivity::class.java)
        intent.putExtra("component", id)
        startActivity(intent)
    }
}