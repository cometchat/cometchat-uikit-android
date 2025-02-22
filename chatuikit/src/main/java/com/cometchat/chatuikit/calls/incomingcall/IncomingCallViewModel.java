package com.cometchat.chatuikit.calls.incomingcall;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.Call;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chatuikit.calls.CallingExtension;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKitHelper;

import java.util.Objects;

public class IncomingCallViewModel extends ViewModel {
    private static final String TAG = IncomingCallViewModel.class.getSimpleName();
    private final MutableLiveData<Call> rejectCall;
    private final MutableLiveData<Call> acceptedCall;
    private final MutableLiveData<CometChatException> exception;
    private String LISTENER_ID;

    public IncomingCallViewModel() {
        rejectCall = new MutableLiveData<>();
        acceptedCall = new MutableLiveData<>();
        exception = new MutableLiveData<>();
    }

    public MutableLiveData<Call> getRejectCall() {
        return rejectCall;
    }

    public MutableLiveData<Call> getAcceptedCall() {
        return acceptedCall;
    }

    public MutableLiveData<CometChatException> getException() {
        return exception;
    }

    public void rejectCall(Call call) {
        CometChat.rejectCall(call.getSessionId(), CometChatConstants.CALL_STATUS_REJECTED, new CometChat.CallbackListener<Call>() {
            @Override
            public void onSuccess(Call call) {
                CometChatUIKitHelper.onCallRejected(call);
                rejectCall.setValue(call);
            }

            @Override
            public void onError(CometChatException e) {
                CometChatUIKitHelper.onCallRejected(call);
                exception.setValue(e);
            }
        });
    }

    public void addListeners() {
        LISTENER_ID = System.currentTimeMillis() + "";
        CometChat.addCallListener(LISTENER_ID, new CometChat.CallListener() {
            @Override
            public void onIncomingCallReceived(Call call) {
            }

            @Override
            public void onOutgoingCallAccepted(Call call) {
            }

            @Override
            public void onOutgoingCallRejected(Call call) {
            }

            @Override
            public void onIncomingCallCancelled(Call call) {
                String sessionId = "";
                if (CallingExtension.getActiveCall() != null) {
                    sessionId = CallingExtension.getActiveCall().getSessionId();
                }
                if (CometChat.getActiveCall() == null && (Objects.equals(call.getSessionId(), sessionId) || sessionId.isEmpty()))
                    rejectCall.setValue(call);
            }
        });
    }

    public void removeListeners() {
        CometChat.removeCallListener(LISTENER_ID);
    }

    public void acceptCall(Call call) {
        CometChat.acceptCall(call.getSessionId(), new CometChat.CallbackListener<Call>() {
            @Override
            public void onSuccess(Call call) {
                CometChatUIKitHelper.onCallAccepted(call);
                acceptedCall.setValue(call);
            }

            @Override
            public void onError(CometChatException e) {
                exception.setValue(e);
            }
        });
    }
}
