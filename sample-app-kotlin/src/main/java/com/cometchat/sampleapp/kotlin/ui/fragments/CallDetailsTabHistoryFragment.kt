package com.cometchat.sampleapp.kotlin.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.sampleapp.kotlin.databinding.FragmentCallDetailsTabBinding
import com.cometchat.sampleapp.kotlin.ui.adapters.CallDetailsTabHistoryAdapter
import com.cometchat.sampleapp.kotlin.viewmodels.CallDetailsTabHistoryViewModel
import com.google.gson.Gson

class CallDetailsTabHistoryFragment : Fragment() {

    private val TAG: String = CallDetailsTabHistoryFragment::class.java.simpleName
    private lateinit var binding: FragmentCallDetailsTabBinding
    private lateinit var viewModel: CallDetailsTabHistoryViewModel
    private lateinit var callLog: CallLog
    private lateinit var adapter: CallDetailsTabHistoryAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            callLog = Gson().fromJson(
                requireArguments().getString("callLog"), CallLog::class.java
            )
            callLog.initiator = Gson().fromJson(
                requireArguments().getString("initiator"), CallUser::class.java
            )
            callLog.receiver = Gson().fromJson(
                requireArguments().getString("receiver"), CallUser::class.java
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCallDetailsTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()

        initViewModel()
    }

    private fun initRecyclerView() {
        layoutManager = LinearLayoutManager(requireContext())
        binding.rvCallDetails.layoutManager = layoutManager
        adapter = CallDetailsTabHistoryAdapter(requireContext())
        binding.rvCallDetails.adapter = adapter
        binding.rvCallDetails.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.fetchCalls()
                }
            }
        })
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider.NewInstanceFactory().create(
            CallDetailsTabHistoryViewModel::class.java
        )
        viewModel.setCallLog(callLog)
        viewModel.fetchCalls()
        viewModel.mutableCallsList.observe(requireContext() as LifecycleOwner, listObserver)
        viewModel.insertAtTop().observe(requireContext() as LifecycleOwner, insertAtTop)
        viewModel.moveToTop().observe(requireContext() as LifecycleOwner, moveToTop)
        viewModel.updateCall().observe(requireContext() as LifecycleOwner, update)
        viewModel.removeCall().observe(requireContext() as LifecycleOwner, remove)
        viewModel.initiatedCall.observe(
            requireContext() as LifecycleOwner
        ) {}
        viewModel.cometChatException.observe(
            requireContext() as LifecycleOwner, exceptionObserver
        )
    }

    /**
     * Observer that listens for updates in the list of call logs and sets the data
     * in the adapter.
     */
    private var listObserver: Observer<List<CallLog>> = Observer { callList: List<CallLog> -> adapter.setCallLogs(callList) }

    /**
     * Observer that listens for new data and inserts the new item at the top of the
     * recycler view.
     */
    private var insertAtTop: Observer<Int> = Observer { integer ->
        adapter.notifyItemInserted(integer)
        scrollToTop()
    }

    /**
     * Observer that moves a data item to the top of the recycler view and notifies
     * the adapter to update the view.
     */
    private var moveToTop: Observer<Int> = Observer { integer ->
        adapter.notifyItemMoved(integer, 0)
        adapter.notifyItemChanged(0)
        scrollToTop()
    }

    private fun scrollToTop() {
        if (layoutManager.findFirstVisibleItemPosition() < 5) {
            layoutManager.scrollToPosition(0)
        }
    }

    /**
     * Observer that updates a specific item in the recycler view when its position
     * is provided.
     */
    private var update: Observer<Int> = Observer { value -> adapter.notifyItemChanged(value) }

    /**
     * Observer that removes an item from the recycler view based on its position.
     */
    private var remove: Observer<Int> = Observer { integer -> adapter.notifyItemRemoved(integer) }

    /**
     * Observer for handling exceptions. If an error occurs, the OnCallError
     * listener is triggered to handle the exception.
     */
    private var exceptionObserver: Observer<CometChatException> = Observer { exception: CometChatException ->
        Log.e(
            TAG, "Error: $exception"
        )
    }
}
