package com.cometchat.chatuikit.shared.spans;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

public class MentionMovementMethod extends LinkMovementMethod {
    private static final String TAG = MentionMovementMethod.class.getSimpleName();
    private static MentionMovementMethod sInstance;
    private static Object touchedSpan;

    public static MentionMovementMethod getInstance() {
        if (sInstance == null) sInstance = new MentionMovementMethod();
        return sInstance;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            // Get touch coordinates
            int x = (int) event.getX();
            int y = (int) event.getY();

            // Adjust for padding
            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            // Adjust for scrolling
            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            if (layout == null) return false;

            // Get line and offset
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            // Find the spans at the touch position
            TagSpan[] spans = buffer.getSpans(off, off, TagSpan.class);

            if (spans.length > 0) {
                TagSpan clickedSpan = spans[0];
                float spanStart = layout.getPrimaryHorizontal(buffer.getSpanStart(clickedSpan));
                float spanEnd = layout.getPrimaryHorizontal(buffer.getSpanEnd(clickedSpan));

                // Ensure the click is within the span's bounds
                if (x >= spanStart && x <= spanEnd) {
                    if (action == MotionEvent.ACTION_UP) {
                        if (touchedSpan == clickedSpan) {
                            clickedSpan.onClick(widget);
                        }
                        touchedSpan = null;
                    } else {
                        touchedSpan = clickedSpan;
                    }
                    return true;
                }
            }

            // Clear touchedSpan if not within bounds
            touchedSpan = null;
        }

        return super.onTouchEvent(widget, buffer, event);
    }
}
