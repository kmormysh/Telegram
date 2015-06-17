package com.katy.telegram.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.katy.telegram.Adapters.ConversationsAdapter;
import com.katy.telegram.Fragments.NavigationDrawerFragment;
import com.katy.telegram.Managers.ChatsManager;
import com.katy.telegram.Managers.TgClient;
import com.katy.telegram.Models.Conversations.Conversation;
import com.katy.telegram.Models.Conversations.ConversationFactory;
import com.katy.telegram.Models.Conversations.ConversationGroup;
import com.katy.telegram.R;
import com.katy.telegram.Utils.TdFileHelper;

import org.drinkless.td.libcore.telegram.TdApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

public class ConversationsActivity extends BaseActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, TgClient.TLResponseCallback, ChatsManager.Callback<List<TdApi.Chat>> {

    @InjectView(R.id.container)
    FrameLayout container;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.massages_list)
    ListView messagesListView;
    @InjectView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private ActionBarDrawerToggle mDrawerToggle;
    private List<Conversation> chats;
    private ConversationsAdapter conversationsAdapter;
    private HashMap<Long, Integer> lastReadOutboxMap = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);
        EventBus.getDefault().registerSticky(this);
        ButterKnife.inject(this);

        toolbar.setTitle(getResources().getString(R.string.label_messages));
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);

        NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        chats = new ArrayList<>();

        ChatsManager.getInstance().getChats(this);
//        TdApi.GetChats getChats = new TdApi.GetChats(0, 10);
//        TgClient.send(getChats, this);

        conversationsAdapter = new ConversationsAdapter(this, chats, lastReadOutboxMap);

        messagesListView.setAdapter(conversationsAdapter);
        messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                Conversation conversation = (Conversation) conversationsAdapter.getItem(position);
                intent.putExtra("chatId", conversation.getChatId());
                intent.putExtra("isGroup", conversation.getChat().type instanceof TdApi.GroupChatInfo);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    @Override
    public void onResult(final TdApi.TLObject object) {
        if (object.getConstructor() == TdApi.Chats.CONSTRUCTOR) {
            for (TdApi.Chat chat : ((TdApi.Chats) object).chats) {
                chats.add(ConversationFactory.create(chat));
            }
            conversationsAdapter.notifyDataSetChanged();
        }
        if (object.getConstructor() == TdApi.Error.CONSTRUCTOR) {
            TdApi.Error error = (TdApi.Error) object;
        }
    }

    public void onEventMainThread(TdApi.UpdateNewMessage newMessage) {
        for (int i = 0; i < chats.size(); i++) {
            Conversation conversation = chats.get(i);
            if (conversation.getChatId() == newMessage.message.chatId) {
                chats.remove(i);
                chats.add(0, conversation);

                if (newMessage.message.message instanceof TdApi.MessageChatChangePhoto) {
                    TdApi.MessageChatChangePhoto chatChangePhoto = (TdApi.MessageChatChangePhoto) newMessage.message.message;
                    TdApi.PhotoSize newPhoto = chatChangePhoto.photo.photos[0];
                    ((TdApi.GroupChatInfo) conversation.getChat().type).groupChat.photoSmall = newPhoto.photo;
                    TdFileHelper.getInstance().getFile(newPhoto.photo, true);
                }
                break;
            }
        }
        conversationsAdapter.notifyDataSetChanged();
    }

    public void onEventMainThread(TdApi.UpdateFile file) {
        for (int i = 0; i < chats.size(); i++) {
            Conversation conversation = chats.get(i);
            if (conversation instanceof ConversationGroup) {
                TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) conversation.getChat().type).groupChat;
                if (groupChat != null && file.fileId == (int) TdFileHelper.getFileId(groupChat.photoSmall)) {
                    groupChat.photoSmall = new TdApi.FileLocal(file.fileId, file.size, file.path);
                    conversationsAdapter.notifyDataSetChanged();
                }
            }
            break;
        }
    }

    public void onEventMainThread(TdApi.UpdateChatReadOutbox chatReadOutbox) {
        lastReadOutboxMap.put(chatReadOutbox.chatId, chatReadOutbox.lastRead);
        ChatsManager.getInstance().setLastReadOutboxMap(lastReadOutboxMap);
    }

    public void onEventMainThread(TdApi.UpdateChatParticipantsCount chatParticipantsCount) {
        for (int i = 0; i < chats.size(); i++) {
            Conversation conversation = chats.get(i);
            if (conversation.getChatId() == chatParticipantsCount.chatId) {
                TdApi.ChatInfo chatInfo = conversation.getChat().type;
                if (chatInfo instanceof TdApi.GroupChatInfo) {
                    if (chatParticipantsCount.participantsCount == 0) {
                        chats.remove(conversation);
                    } else {
                        ((TdApi.GroupChatInfo) chatInfo).groupChat.participantsCount = chatParticipantsCount.participantsCount;
                    }
                    conversationsAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    public void onEventMainThread(TdApi.UpdateChatTitle chatTitle) {
        long chatId = chatTitle.chatId;
        ChatsManager.getInstance().getChat(chatId, new ChatsManager.Callback<TdApi.Chat>() {
            @Override
            public void onResult(TdApi.Chat result) {
                boolean isChatExist = false;
                for (Conversation chat : chats) {
                    if (chat.getChatId() == result.id) {
                        isChatExist = true;
                        break;
                    }
                }
                if (!isChatExist) {

                    chats.add(0, ConversationFactory.create(result));
                    conversationsAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        if (conversationsAdapter != null)
            conversationsAdapter.notifyDataSetChanged();
        super.onResume();
    }


    @Override
    public void onResult(List<TdApi.Chat> result) {
        for (TdApi.Chat chat : result) {
            chats.add(ConversationFactory.create(chat));
        }
        conversationsAdapter.notifyDataSetChanged();

    }

    //    public void handleFileDownloaded(TdApi.UpdateFile file) {
//        TdApi.User user = AccountManager.getInstance().getCurrentUser();
//        if (user.photoSmall instanceof TdApi.FileEmpty) {
//            if (file.fileId == ((TdApi.FileEmpty) user.photoSmall).id) {
//                user.photoSmall = new TdApi.FileLocal(file.fileId, file.size, file.path);
//            }
//        } else if (user.photoBig instanceof TdApi.FileEmpty) {
//            if (file.fileId == ((TdApi.FileEmpty) user.photoBig).id) {
//                user.photoBig = new TdApi.FileLocal(file.fileId, file.size, file.path);
//            }
//        }
//
//        TdApi.Chat chat = downloadingFilesPositions.get((long) file.fileId);
//        if (chat != null) {
//            if (chat.type instanceof TdApi.PrivateChatInfo) {
//                TdApi.File photoSmall = ((TdApi.PrivateChatInfo) chat.type).user.photoSmall;
//                TdApi.File photoBig = ((TdApi.PrivateChatInfo) chat.type).user.photoBig;
//                if (photoSmall instanceof TdApi.FileEmpty)
//                    if (file.fileId == ((TdApi.FileEmpty) photoSmall).id) {
//                        ((TdApi.PrivateChatInfo) chat.type).user.photoSmall = new TdApi.FileLocal(file.fileId, file.size, file.path);
//                    }
//                if (photoBig instanceof TdApi.FileEmpty)
//                    if (file.fileId == ((TdApi.FileEmpty) photoBig).id) {
//                        ((TdApi.PrivateChatInfo) chat.type).user.photoBig = new TdApi.FileLocal(file.fileId, file.size, file.path);
//                    }
//            } else if (chat.type instanceof TdApi.GroupChatInfo) {
//                //TODO
//            }
//        conversationsAdapter.notifyDataSetChanged();
//    }

    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
        }
    }
}
