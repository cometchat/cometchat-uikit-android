package com.cometchat.sampleapp.java.fcm.ui.activity;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.resources.utils.keyboard_utils.KeyBoardUtils;
import com.cometchat.sampleapp.java.fcm.R;
import com.cometchat.sampleapp.java.fcm.databinding.ActivityThreadMessageBinding;
import com.cometchat.sampleapp.java.fcm.viewmodels.ThreadMessageViewModel;

public class ThreadMessageActivity extends AppCompatActivity {
    private ActivityThreadMessageBinding binding;
    private User user;
    private Group group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        binding = ActivityThreadMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Create an instance of the MessagesViewModel
        ThreadMessageViewModel viewModel = new ViewModelProvider.NewInstanceFactory().create(ThreadMessageViewModel.class);
        viewModel.getParentMessage().observe(this, this::setParentMessage);
        int messageId = getIntent().getIntExtra(getString(R.string.app_message_id), -1);
        viewModel.fetchMessageDetails(messageId);

        // Set up back button behavior
        binding.backIcon.setOnClickListener((v) -> {
            Utils.hideKeyBoard(this, binding.getRoot());
            finish();
        });

        // Get the screen height
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;

        // Calculate 25% of the screen height
        int requiredHeight = (int) (screenHeight * 0.35);
        binding.threadHeader.setMaxHeight(requiredHeight);
    }

    private void setParentMessage(BaseMessage parentMessage) {
        if (UIKitConstants.ReceiverType.USER.equalsIgnoreCase(parentMessage.getReceiverType())) {
            user = parentMessage
                .getSender()
                .getUid()
                .equalsIgnoreCase(CometChatUIKit.getLoggedInUser().getUid()) ? (User) parentMessage.getReceiver() : parentMessage.getSender();
        } else if (UIKitConstants.ReceiverType.GROUP.equalsIgnoreCase(parentMessage.getReceiverType())) {
            group = (Group) parentMessage.getReceiver();
        }

        KeyBoardUtils.setKeyboardVisibilityListener(this, binding.getRoot(), keyboardVisible -> {
            if (binding.messageComposer.getMessageInput().getComposeBox().isFocused() && keyboardVisible) {
                if (binding.messageList.atBottom()) {
                    binding.messageList.scrollToBottom();
                }
            }
        });

        binding.messageList.setParentMessage(parentMessage.getId());
        binding.messageComposer.setParentMessageId(parentMessage.getId());
        binding.threadHeader.setParentMessage(parentMessage);

        // Set user or group data to the message header and composer
        if (user != null) {
            binding.messageList.setUser(user);
            binding.messageComposer.setUser(user);
        } else if (group != null) {
            binding.messageList.setGroup(group);
            binding.messageComposer.setGroup(group);
        }
    }
}
