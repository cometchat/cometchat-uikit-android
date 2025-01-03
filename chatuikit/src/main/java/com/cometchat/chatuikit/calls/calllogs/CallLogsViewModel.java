package com.cometchat.chatuikit.calls.calllogs;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.calls.constants.CometChatCallsConstants;
import com.cometchat.calls.core.CallLogRequest;
import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.calls.exceptions.CometChatException;
import com.cometchat.calls.model.CallLog;
import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.Call;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.events.CometChatCallEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for managing call logs in the CometChat application. This class
 * holds the data related to call logs and handles the communication with the
 * CometChat API for fetching, updating, and initiating calls.
 */
public class CallLogsViewModel extends ViewModel {
    private static final String TAG = CallLogsViewModel.class.getSimpleName();
    private final List<CallLog> callsArrayList;
    private final MutableLiveData<List<CallLog>> mutableCallsList;
    private final MutableLiveData<Integer> insertAtTop;
    private final MutableLiveData<Integer> moveToTop;
    private final MutableLiveData<Integer> updateCall;
    private final MutableLiveData<Call> initiatedCall;
    private final MutableLiveData<Integer> removeCall;
    private final MutableLiveData<UIKitConstants.States> states;
    private final MutableLiveData<CometChatException> cometchatException;
    private int limit = 30;
    private boolean hasMore = true;
    private CallLogRequest callLogRequest;
    private CallLogRequest.CallLogRequestBuilder callLogRequestBuilder;

    /**
     * Constructor for CallLogsViewModel. Initializes the mutable live data objects
     * and builds the call log request with the user authentication token.
     */
    public CallLogsViewModel() {
        mutableCallsList = new MutableLiveData<>();
        insertAtTop = new MutableLiveData<>();
        moveToTop = new MutableLiveData<>();
        callsArrayList = new ArrayList<>();
        updateCall = new MutableLiveData<>();
        removeCall = new MutableLiveData<>();
        cometchatException = new MutableLiveData<>();
        initiatedCall = new MutableLiveData<>();
        states = new MutableLiveData<>();
        callLogRequestBuilder = new CallLogRequest.CallLogRequestBuilder().setAuthToken(CometChat.getUserAuthToken()).setCallCategory(
            CometChatCallsConstants.CALL_CATEGORY_CALL);
        callLogRequest = callLogRequestBuilder.build();
    }

    /**
     * Gets the mutable live data list of call logs.
     *
     * @return A MutableLiveData object containing the list of call logs.
     */
    public MutableLiveData<List<CallLog>> getMutableCallsList() {
        return mutableCallsList;
    }

    /**
     * Gets the mutable live data for the position to insert a call log at the top.
     *
     * @return A MutableLiveData object containing the position to insert.
     */
    public MutableLiveData<Integer> insertAtTop() {
        return insertAtTop;
    }

    /**
     * Gets the mutable live data for the position to move a call log to the top.
     *
     * @return A MutableLiveData object containing the position to move.
     */
    public MutableLiveData<Integer> moveToTop() {
        return moveToTop;
    }

    /**
     * Gets the list of call logs.
     *
     * @return A List of CallLog objects.
     */
    public List<CallLog> getCallsArrayList() {
        return callsArrayList;
    }

    /**
     * Gets the mutable live data for updating a call.
     *
     * @return A MutableLiveData object containing the index of the updated call.
     */
    public MutableLiveData<Integer> updateCall() {
        return updateCall;
    }

    /**
     * Gets the mutable live data for removing a call.
     *
     * @return A MutableLiveData object containing the index of the removed call.
     */
    public MutableLiveData<Integer> removeCall() {
        return removeCall;
    }

    /**
     * Gets the mutable live data for CometChat exceptions.
     *
     * @return A MutableLiveData object containing a CometChatException.
     */
    public MutableLiveData<CometChatException> getCometChatException() {
        return cometchatException;
    }

    /**
     * Gets the mutable live data for initiated calls.
     *
     * @return A MutableLiveData object containing the initiated Call.
     */
    public MutableLiveData<Call> getInitiatedCall() {
        return initiatedCall;
    }

    /**
     * Gets the mutable live data for the current state of call logs.
     *
     * @return A MutableLiveData object containing the current state.
     */
    public MutableLiveData<UIKitConstants.States> getStates() {
        return states;
    }

    /**
     * Updates the call log in the list.
     *
     * @param call The CallLog object to be updated.
     */
    public void updateCall(CallLog call) {
        if (callsArrayList.contains(call)) {
            callsArrayList.set(callsArrayList.indexOf(call), call);
            updateCall.setValue(callsArrayList.indexOf(call));
        }
    }

    /**
     * Moves a call log to the top of the list.
     *
     * @param call The CallLog object to be moved.
     */
    public void moveToTop(CallLog call) {
        if (callsArrayList.contains(call)) {
            int oldIndex = callsArrayList.indexOf(call);
            callsArrayList.remove(call);
            callsArrayList.add(0, call);
            moveToTop.setValue(oldIndex);
        }
    }

    /**
     * Adds a call log to the top of the list.
     *
     * @param call The CallLog object to be added.
     */
    public void addToTop(CallLog call) {
        if (call != null && !callsArrayList.contains(call)) {
            callsArrayList.add(0, call);
            insertAtTop.setValue(0);
        }
    }

    /**
     * Removes a call log from the list.
     *
     * @param call The CallLog object to be removed.
     */
    public void removeCall(CallLog call) {
        if (callsArrayList.contains(call)) {
            int index = callsArrayList.indexOf(call);
            this.callsArrayList.remove(call);
            removeCall.setValue(index);
            states.setValue(checkIsEmpty(callsArrayList));
        }
    }

    /**
     * Checks if the list of calls is empty and returns the corresponding state.
     *
     * @param calls The list of CallLog objects to check.
     * @return The state indicating whether the list is empty or not.
     */
    private UIKitConstants.States checkIsEmpty(List<CallLog> calls) {
        if (calls.isEmpty()) return UIKitConstants.States.EMPTY;
        return UIKitConstants.States.NON_EMPTY;
    }

    /**
     * Initiates a call to a specified user.
     *
     * @param receiverId The ID of the user to call.
     * @param callType   The type of call (audio/video).
     */
    public void startCall(String receiverId, String callType) {
        Call call = new Call(receiverId, CometChatConstants.RECEIVER_TYPE_USER, callType);
        CometChat.initiateCall(call, new CometChat.CallbackListener<Call>() {
            @Override
            public void onSuccess(Call call) {
                initiatedCall.setValue(call);
                for (CometChatCallEvents events : CometChatCallEvents.callingEvents.values()) {
                    events.ccOutgoingCall(call);
                }
            }

            @Override
            public void onError(com.cometchat.chat.exceptions.CometChatException e) {
                CometChatException exception = new CometChatException(e.getMessage(), e.getCode());
                exception.setDetails(e.getDetails());
                cometchatException.setValue(exception);
            }
        });
    }

    /**
     * Sets the CallLogRequestBuilder and builds the CallLogRequest.
     *
     * @param callLogRequestBuilder The CallLogRequestBuilder to be set.
     */
    public void setCallLogRequestBuilder(CallLogRequest.CallLogRequestBuilder callLogRequestBuilder) {
        if (callLogRequestBuilder != null) {
            this.callLogRequestBuilder = callLogRequestBuilder.setCallCategory(CometChatCallsConstants.CALL_CATEGORY_CALL);
            this.callLogRequest = callLogRequestBuilder.setAuthToken(CometChat.getUserAuthToken()).build();
        }
    }

    /**
     * Forces the refresh of the call log by resetting the pagination state and reinitializing
     * the call log request with the current user's authentication token. Clears the existing
     * call list and fetches fresh data from the server.
     */
    public void forceToRefresh() {
        hasMore = true;
        callLogRequestBuilder = new CallLogRequest.CallLogRequestBuilder().setAuthToken(CometChat.getUserAuthToken()).setCallCategory(
            CometChatCallsConstants.CALL_CATEGORY_CALL);
        callLogRequest = callLogRequestBuilder.build();
        clear();
        fetchCalls();
    }

    /**
     * Clears the calls array list and notifies the observers.
     */
    public void clear() {
        callsArrayList.clear();
        mutableCallsList.setValue(callsArrayList);
    }

    /**
     * Fetches the call logs from the server. Updates the states based on the
     * fetching status.
     */
    public void fetchCalls() {
        if (callsArrayList.isEmpty()) states.setValue(UIKitConstants.States.LOADING);
        if (hasMore) {
            callLogRequest.fetchNext(new CometChatCalls.CallbackListener<List<CallLog>>() {
                @Override
                public void onSuccess(@NonNull List<CallLog> callLogs) {
                    hasMore = !callLogs.isEmpty();
                    if (hasMore) addList(callLogs);
                    states.setValue(UIKitConstants.States.LOADED);
                    states.setValue(checkIsEmpty(callsArrayList));
                }

                @Override
                public void onError(com.cometchat.calls.exceptions.CometChatException e) {
                    cometchatException.setValue(e);
                    states.setValue(UIKitConstants.States.ERROR);
                }
            });
        }
    }

    /**
     * Adds a list of call logs to the existing calls array list.
     *
     * @param callList The list of CallLog objects to be added.
     */
    public void addList(List<CallLog> callList) {
        callsArrayList.addAll(callList);
        mutableCallsList.setValue(callsArrayList);
    }

    /**
     * Gets the current limit for the number of call logs.
     *
     * @return An integer representing the current limit.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Sets the limit for the number of call logs to be fetched.
     *
     * @param limit The maximum number of call logs to fetch.
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }
}
