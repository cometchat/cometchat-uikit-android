package com.cometchat.chatuikit.ai;

import com.cometchat.chatuikit.ai.aiconversationstarter.AIConversationStarterExtension;
import com.cometchat.chatuikit.ai.aiconversationsummary.AIConversationSummaryExtension;
import com.cometchat.chatuikit.ai.aismartreplies.AISmartRepliesExtension;

import java.util.ArrayList;
import java.util.List;

public class DefaultAIFeature {
    private static final String TAG = DefaultAIFeature.class.getSimpleName();

    public static List<AIExtensionDataSource> get() {
        List<AIExtensionDataSource> list = new ArrayList<>();
        list.add(new AIConversationStarterExtension());
        list.add(new AISmartRepliesExtension());
        list.add(new AIConversationSummaryExtension());
        return list;
    }
}
