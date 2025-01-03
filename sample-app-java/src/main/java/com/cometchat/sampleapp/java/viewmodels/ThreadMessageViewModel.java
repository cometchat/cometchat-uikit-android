package com.cometchat.sampleapp.java.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.sampleapp.java.data.repository.Repository;

public class ThreadMessageViewModel extends ViewModel {

    private final MutableLiveData<BaseMessage> parentMessage;
    private int id;

    public ThreadMessageViewModel() {
        parentMessage = new MutableLiveData<>();
    }

    public int getId() {
        return id;
    }

    public MutableLiveData<BaseMessage> getParentMessage() {
        return parentMessage;
    }

    public void fetchMessageDetails(int id) {
        this.id = id;
        Repository.fetchMessageInformation(id, new CometChat.CallbackListener<BaseMessage>() {
            @Override
            public void onSuccess(BaseMessage message) {
                parentMessage.setValue(message);
            }

            @Override
            public void onError(CometChatException e) {
            }
        });
    }
}
