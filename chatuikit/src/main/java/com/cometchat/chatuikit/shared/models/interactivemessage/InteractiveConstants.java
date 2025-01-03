package com.cometchat.chatuikit.shared.models.interactivemessage;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class InteractiveConstants {
    private static final String TAG = InteractiveConstants.class.getSimpleName();


    public static final String INTERACTIVE_MESSAGE_FORM_FIELD = "formFields";
    public static final String CARD_ACTIONS = "cardActions";
    public static final String TITLE = "title";
    public static final String SUBTITLE = "subtitle";
    public static final String LOGGED_IN_USER = "cometchatSenderUid";
    public static final String GOAL_COMPLETION_TEXT = "goalCompletionText";
    public static final String ELEMENT_TYPE = "elementType";
    public static final String ELEMENT_ID = "elementId";
    public static final String INTERACTIVE_MESSAGE_SUBMIT_ELEMENT = "submitElement";
    public static final String INTERACTIVE_MESSAGE_SCHEDULE_ELEMENT = "scheduleElement";
    public static final String IMAGE_URL = "imageUrl";
    public static final String TEXT = "text";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String TIME_ZONE_CODE = "timezoneCode";
    public static final String BUFFER_TIME = "bufferTime";
    public static final String DURATION = "duration";
    public static final String AVAILABILITY = "availability";
    public static final String DATE_RANGE_START = "dateRangeStart";
    public static final String DATE_RANGE_END = "dateRangeEnd";
    public static final String ICS_FILE_URL = "icsFileUrl";
    public static final String AVATAR_URL = "avatarUrl";
    public static final String FROM_TIME = "from";
    public static final String TO_TIME = "to";

    public static final class OptionElementConstants {
        public static final String LABEL = "label";
        public static final String VALUE = "value";
    }

    public static final class DateTimeMode {
        public static final String DATE = "date";
        public static final String TIME = "time";
        public static final String DATE_TIME = "dateTime";
    }

    public static final class ElementsType {

        public static final String TEXT_INPUT = "textInput";
        public static final String RADIO_BUTTON = "radio";
        public static final String LABEL = "label";
        public static final String DROP_DOWN = "dropdown";
        public static final String CHECK_BOX = "checkbox";
        public static final String BUTTON = "button";
        public static final String SINGLE_SELECT = "singleSelect";
        public static final String DATE_TIME = "dateTime";
    }

    public static final class TextInputUIConstants {

        public static final String ENABLED = "enabled";
        public static final String OPTIONAL = "optional";
        public static final String LABEL = "label";
        public static final String MAX_LINES = "maxLines";
        public static final String PLACEHOLDER = "placeholder";
        public static final String PLACEHOLDER_TEXT = "text";
        public static final String TEXT = "text";
    }

    public static final class RadioButtonUIConstants {
        public static final String ENABLED = "enabled";
        public static final String OPTIONAL = "optional";
        public static final String LABEL = "label";
        public static final String OPTIONS = "options";
        public static final String OPTION_ID = "id";
        public static final String OPTION_VALUE = "value";
        public static final String OPTION_SELECTED = "selected";
    }

    public static final class LabelUIConstants {
        public static final String TEXT = "text";
    }

    public static final class DropDownUIConstants {
        public static final String ENABLED = "enabled";
        public static final String OPTIONAL = "optional";
        public static final String LABEL = "label";
        public static final String OPTIONS = "options";
        public static final String OPTION_LABEL = "label";
        public static final String OPTION_VALUE = "value";
    }

    public static final class DateTimeUIConstants {

        public static final String MODE = "mode";
        public static final String LABEL = "label";
        public static final String OPTIONAL = "optional";
        public static final String PLACEHOLDER = "placeholder";

        public static final String DATE_TIME_FORMAT = "dateTimeFormat";
        public static final String FROM = "from";
        public static final String TO = "to";
        public static final String TIME_ZONE_CODE = "timezoneCode";
    }

    public static final class CheckBoxUIConstants {
        public static final String ENABLED = "enabled";
        public static final String OPTIONAL = "optional";
        public static final String LABEL = "label";
        public static final String OPTIONS = "options";
        public static final String OPTION_LABEL = "label";
        public static final String OPTION_VALUE = "value";
        public static final String OPTION_SELECTED = "selected";
    }

    public static final class ButtonUIConstants {
        public static final String DISABLE_AFTER_INTERACTED = "disableAfterInteracted";
        public static final String TEXT = "buttonText";
        public static final String ACTION = "action";
        public static final String ACTION_TYPE = "actionType";
        public static final String ACTION_URL = "url";
        public static final String ACTION_PAYLOAD = "payload";
        public static final String ACTION_HEADERS = "headers";
        public static final String METHOD = "method";
        public static final String DATA_KEY = "dataKey";
    }

    public static final class SchedulerConstant {
        public static final String DURATION = "duration";
        public static final String MEET_START_AT = "meetStartAt";
        public static final String MESSAGE_ID = "messageId";
        public static final String CONVERSATION_ID = "conversationId";
        public static final String SENDER_ID = "senderId";
    }

    @StringDef({ACTION_TYPE_URL_NAVIGATION, ACTION_TYPE_API_ACTION, ACTION_TYPE_CUSTOM, ACTION_TYPE_DEEP_LINKING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {
    }

    public static final String ACTION_TYPE_URL_NAVIGATION = "urlNavigation";
    public static final String ACTION_TYPE_API_ACTION = "apiAction";
    public static final String ACTION_TYPE_CUSTOM = "custom";
    public static final String ACTION_TYPE_DEEP_LINKING = "deepLinking";

    public static final class InteractiveRequestPayload {
        public static final String APP_ID = "appID";
        public static final String REGION = "region";
        public static final String TRIGGER = "trigger";
        public static final String PAYLOAD = "payload";
        public static final String DATA = "data";
        public static final String CONVERSATION_ID = "conversationId";
        public static final String SENDER = "sender";
        public static final String RECEIVER = "receiver";
        public static final String RECEIVER_TYPE = "receiverType";
        public static final String MESSAGE_CATEGORY = "messageCategory";
        public static final String MESSAGE_ID = "messageId";
        public static final String MESSAGE_TYPE = "messageType";
        public static final String INTERACTION_TIMEZONE_CODE = "interactionTimezoneCode";
        public static final String INTERACTED_BY = "interactedBy";
        public static final String INTERACTED_ELEMENT_ID = "interactedElementId";
        public static final String FORM_DATA = "formData";
        public static final String SCHEDULER_DATA = "schedulerData";
    }
}
