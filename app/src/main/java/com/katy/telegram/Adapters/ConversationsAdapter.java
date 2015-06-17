package com.katy.telegram.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.katy.telegram.Managers.ChatsManager;
import com.katy.telegram.Models.Conversations.Conversation;
import com.katy.telegram.Models.Conversations.ConversationGroup;
import com.katy.telegram.Models.Conversations.ConversationPrivate;
import com.katy.telegram.R;
import com.katy.telegram.Utils.RoundImage;
import com.katy.telegram.Utils.AvatarHelper;

import org.drinkless.td.libcore.telegram.TdApi;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ConversationsAdapter extends BaseAdapter {

    private LayoutInflater layoutInflater;
    private List<Conversation> messages;
    private Context context;
    private HashMap<Long, Integer> lastReadOutboxMap = new HashMap<>();

    public class ViewHolder {
        public Conversation conversation;
        public Context context;
        public ImageView imageAvatar;
        public ImageView imageGroupChat;
        public ImageView imageOnlineStatus;
        public ImageView imageUnreadMessage;
        public TextView textShortName;
        public TextView textUserName;
        public TextView textMessage;
        public TextView textTime;
        public TextView textNumberOfUnreadMessages;

        public void assignConversation(Conversation conversation) {
            if (conversation != null) {
                conversation.onViewDetached(this);
            }
            this.conversation = conversation;
            conversation.onViewAttached(this);
        }
    }

    public ConversationsAdapter(Context context, List<Conversation> messages,
                          HashMap<Long, Integer> lastReadOutboxMap) {
        this.context = context;
        this.messages = messages;
        this.lastReadOutboxMap = lastReadOutboxMap;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_conversations, parent, false);

            viewHolder = new ViewHolder();

            viewHolder.imageAvatar = (ImageView) convertView.findViewById(R.id.avatar);
            viewHolder.imageGroupChat = (ImageView) convertView.findViewById(R.id.group_image);
            viewHolder.imageOnlineStatus = (ImageView) convertView.findViewById(R.id.online_status);
            viewHolder.imageUnreadMessage = (ImageView) convertView.findViewById(R.id.unread_message);
            viewHolder.textShortName = (TextView) convertView.findViewById(R.id.short_name);
            viewHolder.textUserName = (TextView) convertView.findViewById(R.id.user_name);
            viewHolder.textMessage = (TextView) convertView.findViewById(R.id.text_message);
            viewHolder.textTime = (TextView) convertView.findViewById(R.id.time);
            viewHolder.textNumberOfUnreadMessages = (TextView) convertView.findViewById(R.id.number_of_unread_messages);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.context = context;

        Conversation chat = (Conversation) getItem(position);

        TdApi.User user = null;
        TdApi.GroupChat groupChat = null;

        if (chat instanceof ConversationPrivate) {
            user = ((TdApi.PrivateChatInfo) chat.getChat().type).user;
            setChatAvatar(null, user.firstName + " " + user.lastName, viewHolder);
            viewHolder.textUserName.setText(user.firstName + " " + user.lastName);

        } else if (chat instanceof ConversationGroup) {
            groupChat = ((TdApi.GroupChatInfo) chat.getChat().type).groupChat;
            setChatAvatar(null, groupChat.title, viewHolder);
            viewHolder.textUserName.setText(groupChat.title);
        }

        viewHolder.imageGroupChat.setVisibility(groupChat == null ? View.GONE : View.VISIBLE);

        if (chat.getChat().unreadCount > 0) {
            setUnreadMessages(chat.getChat().unreadCount, viewHolder);
        }

        viewHolder.imageUnreadMessage.setVisibility(chat.getChat().unreadCount > 0 ? View.VISIBLE : View.GONE);
        viewHolder.textNumberOfUnreadMessages.setVisibility(chat.getChat().unreadCount > 0 ? View.VISIBLE : View.GONE);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"),
                Locale.getDefault());
        calendar.setTimeInMillis(((long) chat.getChat().topMessage.date) * 1000L);
        Date d = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        dateFormat.setTimeZone(TimeZone.getDefault());

        if (chat.getChat().topMessage.message.getConstructor() == TdApi.MessageText.CONSTRUCTOR) {
            viewHolder.textMessage.setText(((TdApi.MessageText) chat.getChat().topMessage.message).text);
        }
        viewHolder.textTime.setText(dateFormat.format(d));
        if (chat.getChat().topMessage.date == 0) {
            viewHolder.imageUnreadMessage.setImageResource(R.drawable.ic_error);
            viewHolder.textNumberOfUnreadMessages.setText("");
        }

        //visible = the message is not delivered to the server yet, changes on UpdateMessageId
        if (chat.getChat().topMessage.id >= 1000000000) {
            viewHolder.imageOnlineStatus.setVisibility(View.VISIBLE);
        } else {
            viewHolder.imageOnlineStatus.setVisibility(View.GONE);
            HashMap<Long, Integer> lastReadOutboxMap = ChatsManager.getInstance().getLastReadOutboxMap();
            if (lastReadOutboxMap.containsKey(chat.getChatId())) {
                int lastReadMsg = lastReadOutboxMap.get(chat.getChatId());
                if (lastReadMsg > 0 && chat.getChat().topMessage.id > lastReadMsg) {
                    viewHolder.imageOnlineStatus.setVisibility(View.VISIBLE);
                    viewHolder.imageOnlineStatus.setImageResource(R.drawable.ic_unread);
                } else {
                    viewHolder.imageOnlineStatus.setVisibility(View.GONE);
                }
            }
        }
        viewHolder.assignConversation(chat);

        return convertView;
    }

    private void setUnreadMessages(int numberOfunreadMessages, ViewHolder viewHolder) {
        viewHolder.imageUnreadMessage.setImageResource(R.drawable.ic_badge);
        viewHolder.textNumberOfUnreadMessages.setText(Integer.toString(numberOfunreadMessages));
    }

    private void setChatAvatar(String imagePath, String chatName, ViewHolder viewHolder) {
        RoundImage roundedImage = AvatarHelper.createRoundImage(context, imagePath, chatName);
        AvatarHelper.createImageShortName(imagePath == null, chatName, viewHolder.textShortName);
        viewHolder.imageAvatar.setImageDrawable(roundedImage);
        viewHolder.imageAvatar.setBackgroundColor(Color.TRANSPARENT);
    }
}
