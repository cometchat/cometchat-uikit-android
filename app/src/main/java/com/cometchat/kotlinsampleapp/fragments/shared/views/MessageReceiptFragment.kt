package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.views.CometChatReceipt.CometChatReceipt
import com.cometchat.chatuikit.shared.views.CometChatReceipt.Receipt
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R

class MessageReceiptFragment : Fragment() {
    private var messageReceiptRead: CometChatReceipt? = null
    private var messageReceiptDeliver: CometChatReceipt? = null
    private var messageReceiptSent: CometChatReceipt? = null
    private var messageReceiptProgress: CometChatReceipt? = null
    private var messageReceiptError: CometChatReceipt? = null
    private var parentView: LinearLayout? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_message_receipt, container, false)
        messageReceiptRead = view.findViewById(R.id.receiptRead)
        parentView = view.findViewById(R.id.parent_view)
        messageReceiptDeliver = view.findViewById(R.id.receiptDeliver)
        messageReceiptSent = view.findViewById(R.id.receiptSent)
        messageReceiptProgress = view.findViewById(R.id.receiptProgress)
        messageReceiptError = view.findViewById(R.id.receiptError)
        setReceipts()
        setUpUI(view)
        return view
    }

    private fun setReceipts() {
        //Initializing BaseMessage
        val baseMessage = BaseMessage()
        baseMessage.sender = CometChatUIKit.getLoggedInUser()
        baseMessage.receiverType = CometChatConstants.RECEIVER_TYPE_USER

        //setting ReadReceipt
        baseMessage.readAt = System.currentTimeMillis()
        messageReceiptRead!!.setReceipt(Receipt.READ)

        //setting DeliverReceipt
        baseMessage.readAt = 0
        baseMessage.deliveredAt = System.currentTimeMillis()
        messageReceiptDeliver!!.setReceipt(Receipt.DELIVERED)

        //setting SentReceipt
        baseMessage.readAt = 0
        baseMessage.deliveredAt = 0
        baseMessage.sentAt = System.currentTimeMillis()
        messageReceiptSent!!.setReceipt(Receipt.SENT)

        //setting progressReceipt
        baseMessage.readAt = 0
        baseMessage.deliveredAt = 0
        baseMessage.sentAt = 0
        messageReceiptProgress!!.setReceipt(Receipt.IN_PROGRESS)

        //setting errorReceipt
        baseMessage.readAt = 0
        baseMessage.deliveredAt = 0
        baseMessage.sentAt = -1
        messageReceiptError!!.setReceipt(Receipt.ERROR)
    }

    private fun setUpUI(view: View) {
        if (AppUtils.isNightMode(requireContext())) {
            AppUtils.changeTextColorToWhite(
                requireContext(),
                view.findViewById(R.id.message_receipt_text)
            )
            AppUtils.changeTextColorToWhite(
                requireContext(),
                view.findViewById(R.id.message_receipt_text_desc)
            )
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.progress_text))
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.sent_text))
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.deliver_text))
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.read_text))
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.error_text))
            parentView!!.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.app_background_dark
                    )
            )
        } else {
            AppUtils.changeTextColorToBlack(
                requireContext(),
                view.findViewById(R.id.message_receipt_text)
            )
            AppUtils.changeTextColorToBlack(
                requireContext(),
                view.findViewById(R.id.message_receipt_text_desc)
            )
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.progress_text))
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.sent_text))
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.deliver_text))
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.read_text))
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.error_text))
            parentView!!.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.app_background
                    )
            )
        }
    }
}