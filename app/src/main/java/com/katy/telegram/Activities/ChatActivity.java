package com.katy.telegram.Activities;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.katy.telegram.Adapters.ChatAdapter;
import com.katy.telegram.Fragments.AttachPhotoFragment;
import com.katy.telegram.Managers.AccountManager;
import com.katy.telegram.Managers.ChatsManager;
import com.katy.telegram.Managers.LastMessageRegistry;
import com.katy.telegram.Managers.TgClient;
import com.katy.telegram.Models.Messages.ChatEvent;
import com.katy.telegram.Models.Messages.ChatEventFactory;
import com.katy.telegram.Models.Messages.GroupCreatedNotification;
import com.katy.telegram.Models.Messages.NewDaySeparatorNotification;
import com.katy.telegram.Models.Messages.UnreadMessagesBadge;
import com.katy.telegram.R;
import com.katy.telegram.Utils.RoundImage;
import com.katy.telegram.Utils.AvatarHelper;
import com.katy.telegram.Utils.RecyclerItemClickListener;
import com.katy.telegram.Utils.TdFileHelper;

import org.drinkless.td.libcore.telegram.TdApi;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import de.greenrobot.event.EventBus;

public class ChatActivity extends BaseActivity implements TgClient.TLResponseCallback, AttachPhotoFragment.PhotoGalleryImagesHandler, ChatsManager.Callback<TdApi.Chat> {

    private static final int LOAD_MESSAGES_COUNT = 15;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.chat_history)
    RecyclerView chatHistory;
    @InjectView(R.id.text_no_messages)
    TextView textNoMessages;
    @InjectView(R.id.text_message)
    EditText textMessage;
    @InjectView(R.id.attach)
    ImageButton attach;
    @InjectView(R.id.choose_photo)
    RecyclerView msgRecyclerView;

    private TdApi.User user;
    private ChatAdapter chatAdapter;
    private List<ChatEvent> messageList = new ArrayList<>();
    private TdApi.Chat chat;
    private long chatId;
    private TdApi.PrivateChatInfo privateChatInfo;
    private TdApi.GroupChatFull groupChatFull;
    private HashMap<Long, TdApi.User> usersMap;
    private LinearLayoutManager linearLayoutManager;
    private TextView interlocutorStatus;
    private Fragment attachMenuFragment;
    private boolean isGroup;
    private boolean loadingMore;
    private int messagesLoadedCount = 0;
    private boolean allMessagesLoaded;
    private boolean unreadCounterAdded;
    private ImageView chatAvatar;
    private TextView chatAvatarShortName;
    private boolean notEmptyHistory;

    @OnClick(R.id.attach)
    void attachImageLayout() {
        if (attach.getTag().equals(R.drawable.ic_attach)) {
            addAttachImageLayout();
        } else {
            sendMessage(textMessage.getText().toString());
        }
    }

    @OnClick(R.id.emoji)
    void showEmoji() {
        Toast.makeText(getApplicationContext(), "Not implemented yet :(", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.shadow)
    void onShadowClick() {
        hideAttachLayout();
    }

    @OnTextChanged(value = R.id.text_message)
    void onInputTextChanged(CharSequence text) {
        if (text.length() == 0) {
            attach.setTag(R.drawable.ic_attach);
            attach.setImageResource(R.drawable.ic_attach);
        } else {
            if (attach.getTag() == null || !attach.getTag().equals(R.drawable.ic_send)) {
                attach.setTag(R.drawable.ic_send);
                attach.setImageResource(R.drawable.ic_send);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (attachMenuFragment != null) {
            attachMenuFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chat != null) {
            chat.unreadCount = 0;
            HashMap<Long, Integer> lastReadOutboxMap = ChatsManager.getInstance().getLastReadOutboxMap();
            if (lastReadOutboxMap.containsKey(chatId)) {
                chat.lastReadOutboxMessageId = lastReadOutboxMap.get(chatId);
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_chat);
        ButterKnife.inject(this);

        attach.setTag(R.drawable.ic_attach);

        chatHistory.addOnItemTouchListener(new RecyclerItemClickListener(this, chatHistory, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // ...
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        }));

        Intent intent = getIntent();
        isGroup = intent.getBooleanExtra("isGroup", false);
        chatId = intent.getLongExtra("chatId", 0);

        usersMap = new HashMap<>();
        TdApi.User currentUser = AccountManager.getInstance().getCurrentUser();
        usersMap.put((long) currentUser.id, currentUser);

        if (!isGroup) {
            TdApi.GetUser getUser = new TdApi.GetUser((int) chatId);
            TgClient.send(getUser, this);
        }

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_back));
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ChatsManager.getInstance().getChat(chatId, this);

        chatAdapter = new ChatAdapter(this, messageList);
        chatHistory.setAdapter(chatAdapter);

        notEmptyHistory = messageList.size() > 0;
        textNoMessages.setVisibility(notEmptyHistory ? View.GONE : View.VISIBLE);
        chatHistory.setVisibility(notEmptyHistory ? View.VISIBLE : View.GONE);

        attachMenuFragment = getFragmentManager().findFragmentById(R.id.attach_menu);

        msgRecyclerView = (RecyclerView) findViewById(R.id.chat_history);
        msgRecyclerView.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        linearLayoutManager.setStackFromEnd(true);
        msgRecyclerView.setLayoutManager(linearLayoutManager);
        msgRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (recyclerView.computeVerticalScrollOffset() <= 50 && dy < 0) {

                    if (loadingMore || allMessagesLoaded) {
                        return;
                    }
                    loadMoreMessages();
                }
            }
        });
    }

    private void loadMoreMessages() {
        loadingMore = true;
        int fromId = LastMessageRegistry.getInstance().getLastMessageId(chat.topMessage.chatId);
        if (isGroup) {
            TdApi.GetChatHistory getChatHistory = new TdApi.GetChatHistory(groupChatFull.groupChat.id, fromId, messagesLoadedCount, LOAD_MESSAGES_COUNT);
            TgClient.send(getChatHistory, this);
        } else {
            TdApi.GetChatHistory getChatHistory = new TdApi.GetChatHistory(chatId, fromId, messagesLoadedCount, LOAD_MESSAGES_COUNT);
            TgClient.send(getChatHistory, this);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void sendMessage(String text) {
        TdApi.InputMessageText inputMessageText = new TdApi.InputMessageText(text);
        TdApi.SendMessage sendMessage = new TdApi.SendMessage(chat.id, inputMessageText);
        TgClient.send(sendMessage, this);
    }

    private void addAttachImageLayout() {
        Animation bottomUp = AnimationUtils.loadAnimation(this,
                R.anim.slide_in_up);

        View shadow = findViewById(R.id.shadow);

        ColorDrawable[] color = {new ColorDrawable(Color.argb(0, 0, 0, 0)), new ColorDrawable(Color.argb(80, 0, 0, 0))};
        TransitionDrawable trans = new TransitionDrawable(color);
        shadow.setBackgroundDrawable(trans);
        trans.startTransition(400);

        findViewById(R.id.dummy).setVisibility(View.VISIBLE);
        findViewById(R.id.attach_menu).startAnimation(bottomUp);
    }

    private void hideAttachLayout() {
        (findViewById(R.id.dummy)).setVisibility(View.GONE);
    }

    public void onEventMainThread(TdApi.UpdateNewMessage newMessage) {
        if (newMessage.message.chatId != chatId || newMessage instanceof FakeUpdateNewMessage) {
            return;
        }
        if (newMessage.message.message instanceof TdApi.MessageChatChangePhoto) {
            TdApi.MessageChatChangePhoto chatChangePhoto = (TdApi.MessageChatChangePhoto) newMessage.message.message;
            TdApi.PhotoSize newPhoto = chatChangePhoto.photo.photos[0];
            groupChatFull.groupChat.photoSmall = newPhoto.photo;
            TdFileHelper.getInstance().getFile(newPhoto.photo, true);
        }
        TdApi.User messageAuthor = usersMap.get((long) newMessage.message.fromId);
        messageList.add(ChatEventFactory.create(chat, newMessage.message, messageAuthor));

        chatAdapter.notifyDataSetChanged();

        linearLayoutManager.scrollToPositionWithOffset(messageList.size() - 1, 0);

        textNoMessages.setVisibility(View.VISIBLE);
        chatHistory.setVisibility(View.VISIBLE);

        notEmptyHistory = true;
    }

    public void onEventMainThread(TdApi.UpdateUserStatus status) {
        if (!isGroup && usersMap.containsKey((long) status.userId)) {
            if (status.status instanceof TdApi.UserStatusOffline) {
                TdApi.User interlocutor = usersMap.get((long) status.userId);
                Date d = new Date(((TdApi.UserStatusOffline) interlocutor.status).wasOnline * 1000L);
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                interlocutorStatus.setText(String.format("last seen %s ", dateFormat.format(d)));
            } else if (status.status instanceof TdApi.UserStatusEmpty) {
                interlocutorStatus.setText("");
            } else if (status.status instanceof TdApi.UserStatusOnline) {
                interlocutorStatus.setText("online");
            } else if (status.status instanceof TdApi.UserStatusRecently) {
                interlocutorStatus.setText("was recenlty online");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat, menu);
        if (privateChatInfo != null) {
            //hide 'leave group'
            menu.getItem(1).setVisible(false);
        }
        if (groupChatFull != null){
            menu.getItem(1).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear_history) {
            TdApi.DeleteChatHistory deleteChatHistory = new TdApi.DeleteChatHistory(chatId);
            TgClient.send(deleteChatHistory, new TgClient.TLResponseCallback() {
                @Override
                public void onResult(TdApi.TLObject object) {
                    messageList.clear();
                    chatAdapter.notifyDataSetChanged();
                }
            });
            Toast.makeText(getApplicationContext(), "Clear history", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.leave_group) {
            Toast.makeText(getApplicationContext(), "Leave group", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.mute) {
            Toast.makeText(getApplicationContext(), "Mute", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResult(TdApi.TLObject object) {
        if (object instanceof TdApi.User) {
            user = (TdApi.User) object;
            setChatToolbar(user.firstName + " " + user.lastName, user.photoSmall);

        } else if (object instanceof TdApi.Messages) {
            onMessagesLoaded((TdApi.Messages) object);

        } else if (object instanceof TdApi.Message) {
            TdApi.User author = AccountManager.getInstance().getCurrentUser();
            messageList.add(ChatEventFactory.create(chat, (TdApi.Message) object, author));
            chatAdapter.notifyDataSetChanged();

            EventBus.getDefault().post(new FakeUpdateNewMessage((TdApi.Message) object));

            linearLayoutManager.scrollToPositionWithOffset(messageList.size() - 1, 0);

            textMessage.setText(null);
            textMessage.setTag(null);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        } else if (object instanceof TdApi.GroupChatFull) {
            groupChatFull = (TdApi.GroupChatFull) object;

            for (TdApi.ChatParticipant participant : groupChatFull.participants) {
                usersMap.put(((long) participant.user.id), participant.user);
            }

            setChatToolbar(groupChatFull.groupChat.title, groupChatFull.groupChat.photoSmall);

            loadMoreMessages();

        } else if (object instanceof TdApi.Error) {
            TdApi.Error error = (TdApi.Error) object;
        }
    }

    private void onMessagesLoaded(TdApi.Messages object) {
        TdApi.Message[] tdMessagesList = object.messages;
        if (tdMessagesList.length == 0) {
            allMessagesLoaded = true;
        }

        if (messagesLoadedCount + tdMessagesList.length > 0) {
            textNoMessages.setVisibility(View.GONE);
            chatHistory.setVisibility(View.VISIBLE);
        } else {
            textNoMessages.setVisibility(View.VISIBLE);
            chatHistory.setVisibility(View.GONE);
        }

        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeZone(TimeZone.getDefault());
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeZone(TimeZone.getDefault());
        cal1.setTime(new Date(0));
        boolean needsAdjustScroll = false;
        for (int i = tdMessagesList.length - 1; i >= 0; i--) {
            TdApi.Message message = tdMessagesList[i];
            TdApi.User messageAuthor = usersMap.get((long) message.fromId);
            cal2.setTime(new Date(((long) message.date) * 1000L));

//            //insert "new day" separator
//            if (cal1.get(Calendar.DAY_OF_YEAR) < cal2.get(Calendar.DAY_OF_YEAR)
//                    || cal2.get(Calendar.DAY_OF_YEAR) == 1) {
//                messageList.add(new NewDaySeparatorNotification(new Date(((long) message.date) * 1000L)));
//                cal1.setTime(new Date(((long) message.date) * 1000L));
//            }

            if (messagesLoadedCount == 0) {
                if (message.message instanceof TdApi.MessageGroupChatCreate) {
                    messageList.add(new NewDaySeparatorNotification(new Date(((long) message.date) * 1000L)));
                    messageList.add(new GroupCreatedNotification(message.fromId, ((TdApi.MessageGroupChatCreate) message.message).title));
                } else {
                    messageList.add(ChatEventFactory.create(chat, message, messageAuthor));
                }
            } else {
                int indexToInsert = (tdMessagesList.length - 1) - i;
                needsAdjustScroll = true;
                messageList.add(indexToInsert, ChatEventFactory.create(chat, message, messageAuthor));
            }
        }
        if (chat.unreadCount > 0 && !unreadCounterAdded) {
            unreadCounterAdded = true;
            messageList.add(messageList.size() - chat.unreadCount, new UnreadMessagesBadge(chat.unreadCount));
        }
        chatAdapter.notifyDataSetChanged();
        if (needsAdjustScroll) {
            linearLayoutManager.scrollToPositionWithOffset(tdMessagesList.length, 0);
        }
        messagesLoadedCount += tdMessagesList.length;
        loadingMore = false;
    }

    private void setChatToolbar(String name, TdApi.File avatarFile) {

        View view = getLayoutInflater().inflate(R.layout.chat_toolbar, null);

        chatAvatar = (ImageView) view.findViewById(R.id.avatar);
        chatAvatarShortName = (TextView) view.findViewById(R.id.short_name);
        TextView userName = (TextView) view.findViewById(R.id.user_name);
        interlocutorStatus = (TextView) view.findViewById(R.id.status);

        updateConversationStatus();

        RoundImage roundedImage = AvatarHelper.createRoundImage(getApplicationContext(),
                avatarFile instanceof TdApi.FileLocal ? ((TdApi.FileLocal) avatarFile).path : null,
                name);
        AvatarHelper.createImageShortName(avatarFile instanceof TdApi.FileEmpty, name, chatAvatarShortName);
        chatAvatar.setImageDrawable(roundedImage);
        userName.setText(name);
        toolbar.addView(view);
    }


    private void updateConversationStatus() {
        if (isGroup) {
            int onlineUsers = 0;
            for (TdApi.ChatParticipant participant : groupChatFull.participants) {
                if (participant.user.status instanceof TdApi.UserStatusOnline) {
                    onlineUsers++;
                }
            }
            interlocutorStatus.setText(String.format("%s members, %s online", groupChatFull.participants.length, onlineUsers));
        } else {
            if (user.status instanceof TdApi.UserStatusOffline) {
                Date d = new Date(((long) ((TdApi.UserStatusOffline) user.status).wasOnline) * 1000L);
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                interlocutorStatus.setText(String.format("last seen %s ", dateFormat.format(d)));
            } else if (user.status instanceof TdApi.UserStatusEmpty) {
                interlocutorStatus.setText("");
            } else if (user.status instanceof TdApi.UserStatusOnline) {
                interlocutorStatus.setText("online");
            } else if (user.status instanceof TdApi.UserStatusRecently) {
                interlocutorStatus.setText("was recenlty online");
            } else if (user.status instanceof TdApi.UserStatusLastWeek) {
                interlocutorStatus.setText("was online a week ago");
            } else if (user.status instanceof TdApi.UserStatusLastMonth) {
                interlocutorStatus.setText("was online a month ago");
            }
        }
    }

    @Override
    public void onImagesSelected(List<Uri> uris) {
        for (Uri imageUri : uris) {
            String path = null;
            try {
                path = URLDecoder.decode(imageUri.getPath(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            TdApi.SendMessage sendMessage = new TdApi.SendMessage(chat.id, new TdApi.InputMessagePhoto(path));
            TgClient.send(sendMessage, this);
        }
        (findViewById(R.id.dummy)).setVisibility(View.GONE);
    }


    @Override
    public void onResult(TdApi.Chat result) {
        chat = result;

        if (chat.type instanceof TdApi.GroupChatInfo) {
            int groupId = ((TdApi.GroupChatInfo) chat.type).groupChat.id;
            TdApi.GetGroupChatFull getGroupChatFull = new TdApi.GetGroupChatFull(groupId);
            TgClient.send(getGroupChatFull, this);
        } else if (chat.type instanceof TdApi.PrivateChatInfo) {
            privateChatInfo = (TdApi.PrivateChatInfo) chat.type;
            usersMap.put((long) privateChatInfo.user.id, privateChatInfo.user);
            loadMoreMessages();
        }
    }

    public class FakeUpdateNewMessage extends TdApi.UpdateNewMessage {
        public FakeUpdateNewMessage(TdApi.Message msg) {
            super(msg);
        }
    }

    public void onEventMainThread(TdApi.UpdateFile file) {
        if (groupChatFull != null && file.fileId == (int) TdFileHelper.getFileId(groupChatFull.groupChat.photoSmall)) {
            if (file.path != null && !file.path.equals("")) {
                groupChatFull.groupChat.photoSmall = new TdApi.FileLocal(file.fileId, file.size, file.path);
            }

            RoundImage roundedImage = AvatarHelper.createRoundImage(getApplicationContext(),
                    file.path != null ? file.path : null, groupChatFull.groupChat.title);
            AvatarHelper.createImageShortName(file.path == null, groupChatFull.groupChat.title, chatAvatarShortName);
            chatAvatar.setImageDrawable(roundedImage);
        }
    }
}

