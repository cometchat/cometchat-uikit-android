package com.cometchat.sampleapp.java.fcm.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.calls.exceptions.CometChatException;
import com.cometchat.calls.model.CallLog;
import com.cometchat.calls.model.CallUser;
import com.cometchat.chatuikit.shared.interfaces.OnCallError;
import com.cometchat.sampleapp.java.fcm.databinding.FragmentCallDetailsTabBinding;
import com.cometchat.sampleapp.java.fcm.ui.adapters.CallDetailsTabHistoryAdapter;
import com.cometchat.sampleapp.java.fcm.viewmodels.CallDetailsTabHistoryViewModel;
import com.google.gson.Gson;

import java.util.List;

public class CallDetailsTabHistoryFragment extends Fragment {
    private final String TAG = CallDetailsTabHistoryFragment.class.getSimpleName();
    /**
     * Observer for handling exceptions. If an error occurs, the OnCallError
     * listener is triggered to handle the exception.
     */
    Observer<CometChatException> exceptionObserver = exception -> {
        Log.e(TAG, "Error: " + exception);
    };
    private FragmentCallDetailsTabBinding binding;
    private CallLog callLog;
    private CallDetailsTabHistoryViewModel viewModel;
    private CallDetailsTabHistoryAdapter adapter;
    /**
     * Observer that listens for updates in the list of call logs and sets the data
     * in the adapter.
     */
    Observer<List<CallLog>> listObserver = callList -> adapter.setCallLogs(callList);
    /**
     * Observer that updates a specific item in the recycler view when its position
     * is provided.
     */
    Observer<Integer> update = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            adapter.notifyItemChanged(integer);
        }
    };
    /**
     * Observer that removes an item from the recycler view based on its position.
     */
    Observer<Integer> remove = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            adapter.notifyItemRemoved(integer);
        }
    };
    private LinearLayoutManager layoutManager;
    /**
     * Observer that listens for new data and inserts the new item at the top of the
     * recycler view.
     */
    Observer<Integer> insertAtTop = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            adapter.notifyItemInserted(integer);
            scrollToTop();
        }
    };
    /**
     * Observer that moves a data item to the top of the recycler view and notifies
     * the adapter to update the view.
     */
    Observer<Integer> moveToTop = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            adapter.notifyItemMoved(integer, 0);
            adapter.notifyItemChanged(0);
            scrollToTop();
        }
    };
    private OnCallError onError;

    public CallDetailsTabHistoryFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            callLog = new Gson().fromJson(getArguments().getString("callLog"), CallLog.class);
            callLog.setInitiator(new Gson().fromJson(getArguments().getString("initiator"), CallUser.class));
            callLog.setReceiver(new Gson().fromJson(getArguments().getString("receiver"), CallUser.class));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCallDetailsTabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initRecyclerView();

        initViewModel();
    }

    private void initRecyclerView() {
        layoutManager = new LinearLayoutManager(requireContext());
        binding.rvCallDetails.setLayoutManager(layoutManager);
        adapter = new CallDetailsTabHistoryAdapter(requireContext());
        binding.rvCallDetails.setAdapter(adapter);
        binding.rvCallDetails.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.fetchCalls();
                }
            }
        });
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider.NewInstanceFactory().create(CallDetailsTabHistoryViewModel.class);
        viewModel.setCallLog(callLog);
        viewModel.fetchCalls();
        viewModel.getMutableCallsList().observe((LifecycleOwner) requireContext(), listObserver);
        viewModel.insertAtTop().observe((LifecycleOwner) requireContext(), insertAtTop);
        viewModel.moveToTop().observe((LifecycleOwner) requireContext(), moveToTop);
        viewModel.updateCall().observe((LifecycleOwner) requireContext(), update);
        viewModel.removeCall().observe((LifecycleOwner) requireContext(), remove);
        viewModel.getInitiatedCall().observe((LifecycleOwner) requireContext(), (call) -> {
        });
        viewModel.getCometChatException().observe((LifecycleOwner) requireContext(), exceptionObserver);
    }

    private void scrollToTop() {
        if (layoutManager.findFirstVisibleItemPosition() < 5) {
            layoutManager.scrollToPosition(0);
        }
    }
}
