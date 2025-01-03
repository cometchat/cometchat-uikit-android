package com.cometchat.sampleapp.kotlin.ui.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cometchat.calls.model.CallLog
import com.google.gson.Gson

class CallDetailsTabFragmentAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val fragmentList: MutableList<Fragment> = ArrayList()
    private val callLogList: MutableList<CallLog> = ArrayList()
    private val tabTitleList: MutableList<String> = ArrayList()

    fun addFragment(
        fragment: Fragment,
        tabTitle: String,
        callLog: CallLog
    ) {
        fragmentList.add(fragment)
        tabTitleList.add(tabTitle)
        callLogList.add(callLog)
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = fragmentList[position]
        val callLog = callLogList[position]

        // Convert callLog to JSON and set as an argument
        val args = Bundle()
        args.putString("callLog", Gson().toJson(callLog))
        args.putString("initiator", Gson().toJson(callLog.initiator))
        args.putString("receiver", Gson().toJson(callLog.receiver))

        fragment.arguments = args

        return fragment
    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    fun getCallLog(position: Int): CallLog {
        return callLogList[position]
    }

    fun getTabTitle(position: Int): String {
        return tabTitleList[position]
    }
}
