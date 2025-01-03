package com.cometchat.chatuikit.extensions.thumbnailgeneration;

import android.content.Context;
import android.view.View;

import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.MediaMessage;
import com.cometchat.chatuikit.extensions.Extensions;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.framework.DataSource;
import com.cometchat.chatuikit.shared.framework.DataSourceDecorator;
import com.cometchat.chatuikit.shared.models.AdditionParameter;

import java.util.List;

public class ThumbnailGenerationExtensionDecorator extends DataSourceDecorator {
    private static final String TAG = ThumbnailGenerationExtensionDecorator.class.getSimpleName();

    public ThumbnailGenerationExtensionDecorator(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void bindImageBubbleContentView(Context context, View createdView, String thumbnailUrl, MediaMessage message, @StyleRes int imageBubbleContentView, UIKitConstants.MessageBubbleAlignment alignment, RecyclerView.ViewHolder holder, List<BaseMessage> messageList, int position, AdditionParameter additionParameter) {
        super.bindImageBubbleContentView(context, createdView, Extensions.getThumbnailUrl(message), message, imageBubbleContentView, alignment, holder, messageList, position, additionParameter);
    }

    @Override
    public void bindVideoBubbleContentView(Context context, View createdView, String thumbnailUrl, MediaMessage message, @StyleRes int videoBubbleContentView, UIKitConstants.MessageBubbleAlignment alignment, RecyclerView.ViewHolder holder, List<BaseMessage> messageList, int position, AdditionParameter additionParameter) {
        super.bindVideoBubbleContentView(context, createdView, Extensions.getThumbnailUrl(message), message, videoBubbleContentView, alignment, holder, messageList, position, additionParameter);
    }

    @Override
    public String getId() {
        return ThumbnailGenerationExtension.class.getSimpleName();
    }
}
