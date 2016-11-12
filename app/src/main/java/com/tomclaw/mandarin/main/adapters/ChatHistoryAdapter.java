package com.tomclaw.mandarin.main.adapters;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.mandarin.main.views.history.BaseHistoryView;
import com.tomclaw.mandarin.main.views.history.incoming.IncomingFileView;
import com.tomclaw.mandarin.main.views.history.incoming.IncomingImageView;
import com.tomclaw.mandarin.main.views.history.incoming.IncomingTextView;
import com.tomclaw.mandarin.main.views.history.incoming.IncomingVideoView;
import com.tomclaw.mandarin.main.views.history.outgoing.OutgoingFileView;
import com.tomclaw.mandarin.main.views.history.outgoing.OutgoingImageView;
import com.tomclaw.mandarin.main.views.history.outgoing.OutgoingTextView;
import com.tomclaw.mandarin.main.views.history.outgoing.OutgoingVideoView;
import com.tomclaw.mandarin.util.QueryBuilder;
import com.tomclaw.mandarin.util.SelectionHelper;
import com.tomclaw.mandarin.util.SmileyParser;
import com.tomclaw.mandarin.util.TimeHelper;

import java.lang.reflect.Constructor;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/7/13
 * Time: 11:43 PM
 */
public class ChatHistoryAdapter extends CursorRecyclerAdapter<BaseHistoryView> implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int[] ITEM_LAYOUTS = new int[]{
            0,
            R.layout.chat_item_inc_text,
            R.layout.chat_item_inc_image,
            R.layout.chat_item_inc_video,
            R.layout.chat_item_inc_file,
            R.layout.chat_item_out_text,
            R.layout.chat_item_out_image,
            R.layout.chat_item_out_video,
            R.layout.chat_item_out_file
    };

    private static final Class[] ITEM_HOLDERS = new Class[]{
            null,
            IncomingTextView.class,
            IncomingImageView.class,
            IncomingVideoView.class,
            IncomingFileView.class,
            OutgoingTextView.class,
            OutgoingImageView.class,
            OutgoingVideoView.class,
            OutgoingFileView.class
    };

    private TimeHelper timeHelper;

    /**
     * Adapter ID
     */
    private final int LOADER_ID = 10;

    private Buddy buddy = null;

    private static int COLUMN_MESSAGE_TEXT;
    private static int COLUMN_MESSAGE_TIME;
    private static int COLUMN_MESSAGE_TYPE;
    private static int COLUMN_MESSAGE_COOKIE;
    private static int COLUMN_MESSAGE_ACCOUNT_DB_ID;
    private static int COLUMN_MESSAGE_BUDDY_ID;
    private static int COLUMN_ROW_AUTO_ID;
    private static int COLUMN_CONTENT_TYPE;
    private static int COLUMN_CONTENT_SIZE;
    private static int COLUMN_CONTENT_STATE;
    private static int COLUMN_CONTENT_PROGRESS;
    private static int COLUMN_CONTENT_URI;
    private static int COLUMN_CONTENT_NAME;
    private static int COLUMN_PREVIEW_HASH;
    private static int COLUMN_CONTENT_TAG;

    private Context context;
    private LayoutInflater inflater;
    private LoaderManager loaderManager;
    private ContentMessageClickListener contentMessageClickListener;
    private SelectionModeListener selectionModeListener;

    private final SelectionHelper<Long> selectionHelper = new SelectionHelper<>();

    public ChatHistoryAdapter(Context context, LoaderManager loaderManager,
                              Buddy buddy, TimeHelper timeHelper) {
        super(null);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.loaderManager = loaderManager;
        this.timeHelper = timeHelper;
        setBuddy(buddy);
        // Initialize smileys.
        SmileyParser.init(context);
        setHasStableIds(true);
    }

    private void setBuddy(Buddy buddy) {
        if (!TextUtils.isEmpty(buddy.getBuddyId())) {
            // Destroy current loader.
            loaderManager.destroyLoader(LOADER_ID);
        }
        this.buddy = buddy;
        // Initialize loader for adapter Id.
        loaderManager.initLoader(LOADER_ID, null, this);
    }

    public Buddy getBuddy() {
        return buddy;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return getDefaultQueryBuilder().createCursorLoader(context, Settings.HISTORY_RESOLVER_URI);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            // Detecting columns.
            COLUMN_ROW_AUTO_ID = cursor.getColumnIndex(GlobalProvider.ROW_AUTO_ID);
            COLUMN_MESSAGE_TEXT = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TEXT);
            COLUMN_MESSAGE_TIME = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TIME);
            COLUMN_MESSAGE_TYPE = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_TYPE);
            COLUMN_MESSAGE_COOKIE = cursor.getColumnIndex(GlobalProvider.HISTORY_MESSAGE_COOKIE);
            COLUMN_MESSAGE_ACCOUNT_DB_ID = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID);
            COLUMN_MESSAGE_BUDDY_ID = cursor.getColumnIndex(GlobalProvider.HISTORY_BUDDY_ID);
            COLUMN_CONTENT_TYPE = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_TYPE);
            COLUMN_CONTENT_SIZE = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_SIZE);
            COLUMN_CONTENT_STATE = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_STATE);
            COLUMN_CONTENT_PROGRESS = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_PROGRESS);
            COLUMN_CONTENT_URI = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_URI);
            COLUMN_CONTENT_NAME = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_NAME);
            COLUMN_PREVIEW_HASH = cursor.getColumnIndex(GlobalProvider.HISTORY_PREVIEW_HASH);
            COLUMN_CONTENT_TAG = cursor.getColumnIndex(GlobalProvider.HISTORY_CONTENT_TAG);
            // Changing current cursor.
            swapCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        close();
    }

    @Override
    @SuppressWarnings("unchecked")
    public BaseHistoryView onCreateViewHolder(ViewGroup viewGroup, int messageType) {
        try {
            // Inflate view by type.
            View view = inflater.inflate(ITEM_LAYOUTS[messageType], viewGroup, false);
            Class clazz = ITEM_HOLDERS[messageType];
            // Instantiate holder for this view.
            Constructor<BaseHistoryView> constructor = clazz.getConstructor(View.class);
            return constructor.newInstance(view);
        } catch (Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = getCursor();
        int type;
        try {
            if (cursor == null || !cursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            int messageType = cursor.getInt(COLUMN_MESSAGE_TYPE);
            int contentType = cursor.getInt(COLUMN_CONTENT_TYPE);
            type = getItemType(messageType, contentType);
        } catch (Throwable ex) {
            type = 0;
        }
        return type;
    }

    private int getItemType(int messageType, int contentType) {
        int type;
        switch (messageType) {
            case GlobalProvider.HISTORY_MESSAGE_TYPE_ERROR:
                type = 0;
                break;
            case GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING:
                switch (contentType) {
                    case GlobalProvider.HISTORY_CONTENT_TYPE_TEXT:
                        type = 1;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_PICTURE:
                        type = 2;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_VIDEO:
                        type = 3;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_FILE:
                        type = 4;
                        break;
                    default:
                        return 0;
                }
                break;
            case GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING:
                switch (contentType) {
                    case GlobalProvider.HISTORY_CONTENT_TYPE_TEXT:
                        type = 5;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_PICTURE:
                        type = 6;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_VIDEO:
                        type = 7;
                        break;
                    case GlobalProvider.HISTORY_CONTENT_TYPE_FILE:
                        type = 8;
                        break;
                    default:
                        return 0;
                }
                break;
            default:
                return 0;
        }
        return type;
    }

    public long getMessageDbId(int position) throws MessageNotFoundException {
        Cursor cursor = getCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            throw new MessageNotFoundException();
        }
        return cursor.getLong(COLUMN_ROW_AUTO_ID);
    }

    private QueryBuilder getDefaultQueryBuilder() {
        return new QueryBuilder()
                .columnEquals(GlobalProvider.HISTORY_BUDDY_ACCOUNT_DB_ID, buddy.getAccountDbId()).and()
                .columnEquals(GlobalProvider.HISTORY_BUDDY_ID, buddy.getBuddyId())
                .descending(GlobalProvider.ROW_AUTO_ID);
    }

    public void setContentMessageClickListener(
            ContentMessageClickListener contentMessageClickListener) {
        this.contentMessageClickListener = contentMessageClickListener;
    }

    public void setSelectionModeListener(SelectionModeListener selectionModeListener) {
        this.selectionModeListener = selectionModeListener;
    }

    public void close() {
        Cursor cursor = swapCursor(null);
        // Maybe, previous non-closed cursor present?
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    public void onBindViewHolderCursor(BaseHistoryView holder, Cursor cursor) {
        // Message data.
        long messageDbId = cursor.getLong(COLUMN_ROW_AUTO_ID);
        int messageType = cursor.getInt(COLUMN_MESSAGE_TYPE);
        CharSequence messageText = SmileyParser.getInstance().addSmileySpans(
                cursor.getString(COLUMN_MESSAGE_TEXT));
        long messageTime = cursor.getLong(COLUMN_MESSAGE_TIME);
        final String messageCookie = cursor.getString(COLUMN_MESSAGE_COOKIE);
        // Content message data
        int contentType = cursor.getInt(COLUMN_CONTENT_TYPE);
        long contentSize = cursor.getLong(COLUMN_CONTENT_SIZE);
        final int contentState = cursor.getInt(COLUMN_CONTENT_STATE);
        int contentProgress = cursor.getInt(COLUMN_CONTENT_PROGRESS);
        final String contentName = cursor.getString(COLUMN_CONTENT_NAME);
        final String contentUri = cursor.getString(COLUMN_CONTENT_URI);
        String previewHash = cursor.getString(COLUMN_PREVIEW_HASH);
        final String contentTag = cursor.getString(COLUMN_CONTENT_TAG);
        String messageTimeText = timeHelper.getFormattedTime(messageTime);
        String messageDateText = timeHelper.getFormattedDate(messageTime);
        // Showing or hiding date.
        // Go to previous message and comparing dates.
        boolean dateVisible = !(cursor.moveToNext() && messageDateText
                .equals(timeHelper.getFormattedDate(cursor.getLong(COLUMN_MESSAGE_TIME))));
        // Creating chat history item to bind the view.
        ChatHistoryItem historyItem = new ChatHistoryItem(messageDbId, messageType, messageText, messageTime,
                messageCookie, contentType, contentSize, contentState, contentProgress, contentName,
                contentUri, previewHash, contentTag, messageTimeText, messageDateText, dateVisible);
        holder.setSelectionHelper(selectionHelper);
        holder.setContentClickListener(contentMessageClickListener);
        holder.setSelectionModeListener(selectionModeListener);
        holder.bind(historyItem);
    }

    public TimeHelper getTimeHelper() {
        return timeHelper;
    }

    public interface ContentMessageClickListener {

        void onClicked(ChatHistoryItem historyItem);
    }

    public interface SelectionModeListener {
        void onItemStateChanged(ChatHistoryItem historyItem);

        void onNothingSelected();

        void onLongClicked(ChatHistoryItem historyItem, SelectionHelper<Long> selectionHelper);
    }

    public interface AdapterListener {
        void onPreviewClicked(String filePath, String previewHash);
    }
}