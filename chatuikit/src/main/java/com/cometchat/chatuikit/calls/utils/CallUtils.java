package com.cometchat.chatuikit.calls.utils;

import android.content.Context;

import com.cometchat.calls.constants.CometChatCallsConstants;
import com.cometchat.calls.model.CallGroup;
import com.cometchat.calls.model.CallLog;
import com.cometchat.calls.model.CallUser;
import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.Call;
import com.cometchat.chat.models.CustomMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.resources.utils.Utils;

public class CallUtils {
    private static final String TAG = CallUtils.class.getSimpleName();

    public static String getCallerName(Context context, Call call, boolean showSeparator) {
        String name = "";
        String separator = showSeparator ? ": " : "";
        if (call.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_GROUP)) {
            if (((User) call.getCallInitiator()).getUid().equals(CometChatUIKit.getLoggedInUser().getUid())) {
                name = context.getString(R.string.cometchat_you) + separator;
            } else {
                name = ((User) call.getCallInitiator()).getName() + separator;
            }
        }
        return name;
    }

    public static String getCallerName(Context context, CallLog call, boolean showSeparator) {
        String name = "";
        String separator = showSeparator ? ": " : "";
        if (call.getReceiverType().equals(CometChatCallsConstants.RECEIVER_TYPE_USER)) {
            if (call.getInitiator() != null) {
                if (((CallUser) call.getInitiator()).getUid().equals(CometChatUIKit.getLoggedInUser().getUid())) {
                    name = context.getString(R.string.cometchat_you) + separator;
                } else {
                    name = ((CallUser) call.getInitiator()).getName() + separator;
                }
            }
        } else if (call.getReceiverType().equals(CometChatCallsConstants.RECEIVER_TYPE_GROUP)) {
            if (call.getReceiver() != null) {
                name = ((CallGroup) call.getReceiver()).getName() + separator;
            }
        }
        return name;
    }

    public static boolean isCallInitiatedByMe(Call call) {
        return ((User) call.getCallInitiator()).getUid().equals(CometChatUIKit.getLoggedInUser().getUid());
    }

    public static User getCallingUser(Call call) {
        User user = null;
        if (call.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
            if (((User) call.getCallInitiator()).getUid().equals(CometChatUIKit.getLoggedInUser().getUid())) {
                user = ((User) call.getCallReceiver());
            } else {
                user = ((User) call.getCallInitiator());
            }
        }
        return user;
    }

    public static Group getCallingGroup(Call call) {
        Group group = null;
        if (call.getReceiverType().equals(UIKitConstants.ReceiverType.GROUP)) {
            group = ((Group) call.getCallReceiver());
        }
        return group;
    }

    public static Group getCallingGroup(CustomMessage customMessage) {
        Group group = null;
        if (customMessage != null && UIKitConstants.MessageType.MEETING.equalsIgnoreCase(customMessage.getType())) {
            group = (Group) customMessage.getReceiver();
        }
        return group;
    }

    public static String getCallerImage(Call call) {
        String avatar = null;
        if (call.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
            if (((User) call.getCallInitiator()).getUid().equals(CometChatUIKit.getLoggedInUser().getUid())) {
                avatar = ((User) call.getCallReceiver()).getAvatar();
            } else {
                avatar = ((User) call.getCallInitiator()).getAvatar();
            }
        } else {
            if (call.getReceiver() != null) avatar = ((Group) call.getCallReceiver()).getIcon();
        }
        return avatar;
    }

    public static String getCallLogUserName(CallLog callLog) {
        String name = null;
        if (callLog.getReceiverType().equals(CometChatCallsConstants.RECEIVER_TYPE_USER)) {
            if (callLog.getInitiator() != null) {
                if (((CallUser) callLog.getInitiator()).getUid().equals(CometChatUIKit.getLoggedInUser().getUid())) {
                    name = ((CallUser) callLog.getReceiver()).getName();
                } else {
                    name = ((CallUser) callLog.getInitiator()).getName();
                }
            }
        } else if (callLog.getReceiverType().equals(CometChatCallsConstants.RECEIVER_TYPE_GROUP)) {
            if (callLog.getReceiver() != null) {
                name = ((CallGroup) callLog.getReceiver()).getName();
            }
        }
        return name;
    }

    public static String getCallLogUserAvatar(CallLog callLog) {
        String avatar = null;
        if (callLog.getReceiverType().equals(CometChatCallsConstants.RECEIVER_TYPE_USER)) {
            if (callLog.getInitiator() != null) {
                if (((CallUser) callLog.getInitiator()).getUid().equals(CometChatUIKit.getLoggedInUser().getUid())) {
                    avatar = ((CallUser) callLog.getReceiver()).getAvatar();
                } else {
                    avatar = ((CallUser) callLog.getInitiator()).getAvatar();
                }
            }
        } else if (callLog.getReceiverType().equals(CometChatCallsConstants.RECEIVER_TYPE_GROUP)) {
            if (callLog.getReceiver() != null)
                avatar = ((CallGroup) callLog.getReceiver()).getIcon();
        }
        return avatar;
    }

    public static String getCallStatus(Context context, Call call) {
        String callMessageText = "";
        if (call.getReceiverType().equals(UIKitConstants.ReceiverType.USER)) {
            User initiator = (User) call.getCallInitiator();
            if (call.getCallStatus().equals(UIKitConstants.CallStatusConstants.INITIATED)) {
                if (!Utils.isLoggedInUser(initiator))
                    callMessageText = context.getString(R.string.cometchat_incoming) + " " + context.getString(R.string.cometchat_call);
                else
                    callMessageText = context.getString(R.string.cometchat_outgoing) + " " + context.getString(R.string.cometchat_call);
            } else if (call.getCallStatus().equals(UIKitConstants.CallStatusConstants.ONGOING)) {
                callMessageText = context.getString(R.string.cometchat_call) + " " + context.getString(R.string.cometchat_accepted);
            } else if (call.getCallStatus().equals(UIKitConstants.CallStatusConstants.ENDED)) {
                callMessageText = context.getString(R.string.cometchat_call) + " " + context.getString(R.string.cometchat_ended);
            } else if (call.getCallStatus().equals(UIKitConstants.CallStatusConstants.UNANSWERED)) {
                if (Utils.isLoggedInUser(initiator))
                    callMessageText = context.getString(R.string.cometchat_call) + " " + context.getString(R.string.cometchat_unanswered);
                else
                    callMessageText = context.getString(R.string.cometchat_missed_call) + " " + context.getString(R.string.cometchat_call);
            } else if (call.getCallStatus().equals(UIKitConstants.CallStatusConstants.CANCELLED)) {
                if (Utils.isLoggedInUser(initiator))
                    callMessageText = context.getString(R.string.cometchat_call) + " " + context.getString(R.string.cometchat_cancel_call);
                else
                    callMessageText = context.getString(R.string.cometchat_missed_call) + " " + context.getString(R.string.cometchat_call);
            } else if (call.getCallStatus().equals(UIKitConstants.CallStatusConstants.REJECTED)) {
                callMessageText = context.getString(R.string.cometchat_call) + " " + context.getString(R.string.cometchat_rejected_call);
            } else if (call.getCallStatus().equals(UIKitConstants.CallStatusConstants.BUSY)) {
                if (Utils.isLoggedInUser(initiator))
                    callMessageText = context.getString(R.string.cometchat_call) + " " + context.getString(R.string.cometchat_busy_call);
                else
                    callMessageText = context.getString(R.string.cometchat_missed_call) + " " + context.getString(R.string.cometchat_call);
            }
        }
        return " " + callMessageText;
    }

    public static String getCallStatus(Context context, CallLog call) {
        String callMessageText = "";
        if (call.getInitiator() instanceof CallUser) {
            CallUser user = (CallUser) call.getInitiator();
            if (call.getStatus().equals(CometChatCallsConstants.CALL_STATUS_INITIATED)) {
                if (!isLoggedInUser(user))
                    callMessageText = context.getString(R.string.cometchat_incoming) + " " + context.getString(R.string.cometchat_call);
                else
                    callMessageText = context.getString(R.string.cometchat_outgoing) + " " + context.getString(R.string.cometchat_call);
            } else if (call.getStatus().equals(CometChatCallsConstants.CALL_STATUS_ONGOING)) {
                callMessageText = context.getString(R.string.cometchat_call) + " " + context.getString(R.string.cometchat_accepted);
            } else if (call.getStatus().equals(CometChatCallsConstants.CALL_STATUS_ENDED)) {
                if (!isLoggedInUser(user))
                    callMessageText = context.getString(R.string.cometchat_incoming) + " " + context.getString(R.string.cometchat_call);
                else
                    callMessageText = context.getString(R.string.cometchat_outgoing) + " " + context.getString(R.string.cometchat_call);
            } else if (call.getStatus().equals(CometChatCallsConstants.CALL_STATUS_UNANSWERED)) {
                if (isLoggedInUser(user))
                    callMessageText = context.getString(R.string.cometchat_unanswered) + " " + context.getString(R.string.cometchat_call);
                else
                    callMessageText = context.getString(R.string.cometchat_missed_call) + " " + context.getString(R.string.cometchat_call);
            } else if (call.getStatus().equals(CometChatCallsConstants.CALL_STATUS_CANCELLED)) {
                if (isLoggedInUser(user))
                    callMessageText = context.getString(R.string.cometchat_cancel_call) + " " + context.getString(R.string.cometchat_call);
                else
                    callMessageText = context.getString(R.string.cometchat_missed_call) + " " + context.getString(R.string.cometchat_call);
            } else if (call.getStatus().equals(CometChatCallsConstants.CALL_STATUS_REJECTED)) {
                if (isLoggedInUser(user))
                    callMessageText = context.getString(R.string.cometchat_rejected_call) + " " + context.getString(R.string.cometchat_call);
                else
                    callMessageText = context.getString(R.string.cometchat_missed_call) + " " + context.getString(R.string.cometchat_call);
            } else if (call.getStatus().equals(CometChatCallsConstants.CALL_STATUS_BUSY)) {
                if (isLoggedInUser(user))
                    callMessageText = context.getString(R.string.cometchat_call) + " " + context.getString(R.string.cometchat_busy_call);
                else
                    callMessageText = context.getString(R.string.cometchat_missed_call) + " " + context.getString(R.string.cometchat_call);
            }
        }
        return callMessageText;
    }

    public static boolean isVideoCall(Call call) {
        return call.getType().equals(CometChatConstants.CALL_TYPE_VIDEO);
    }

    public static boolean isVideoCall(CallLog call) {
        return call.getType().equals(CometChatCallsConstants.CALL_TYPE_VIDEO);
    }

    public static boolean isLoggedInUser(CallUser user) {
        return CometChatUIKit.getLoggedInUser().getUid().equals(user != null ? user.getUid() : "");
    }

    public static boolean isMissedCall(Call call) {
        if (call.getReceiverType().equals(UIKitConstants.ReceiverType.USER)) {
            User initiator = (User) call.getCallInitiator();
            if (call.getCallStatus().equals(UIKitConstants.CallStatusConstants.UNANSWERED)) {
                return !Utils.isLoggedInUser(initiator);
            } else if (call.getCallStatus().equals(UIKitConstants.CallStatusConstants.CANCELLED)) {
                return !Utils.isLoggedInUser(initiator);
            } else if (call.getCallStatus().equals(UIKitConstants.CallStatusConstants.BUSY)) {
                return !Utils.isLoggedInUser(initiator);
            }
        }
        return false;
    }

    public static boolean isVideoCall(CustomMessage customMessage) {
        if (customMessage.getCustomData() != null) {
            if (customMessage.getCustomData().has("callType")) {
                try {
                    if (customMessage.getCustomData().getString("callType").equalsIgnoreCase("video"))
                        return true;
                } catch (Exception e) {
                    CometChatLogger.e("CallUtils", e.toString());
                }
            }
        }
        return false;
    }

    public static String getCallerName(CustomMessage customMessage) {
        String name = null;
        if (customMessage != null) {
            Group group = (Group) customMessage.getReceiver();
            name = group.getName();
        }
        return name;
    }

    public static String getCallerImage(CustomMessage customMessage) {
        String image = null;
        if (customMessage != null) {
            Group group = (Group) customMessage.getReceiver();
            image = group.getIcon();
        }
        return image;
    }
}
