package com.katy.telegram.Adapters;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.katy.telegram.Activities.BaseActivity;
import com.katy.telegram.Managers.AccountManager;
import com.katy.telegram.Managers.ChatsManager;
import com.katy.telegram.Managers.TgClient;
import com.katy.telegram.Models.Messages.AddParticipantNotification;
import com.katy.telegram.Models.Messages.ChatEvent;
import com.katy.telegram.Models.Messages.ChatMessage;
import com.katy.telegram.Models.Messages.GroupCreatedNotification;
import com.katy.telegram.Models.Messages.GroupPhotoChangedNotification;
import com.katy.telegram.Models.Messages.NewDaySeparatorNotification;
import com.katy.telegram.Models.Messages.UnreadMessagesBadge;
import com.katy.telegram.R;
import com.katy.telegram.Utils.AvatarHelper;
import com.katy.telegram.Utils.CenteredImageSpan;
import com.katy.telegram.Utils.FileSaveOrCopyHelper;
import com.katy.telegram.Utils.FileSizeUnitsHelper;
import com.katy.telegram.Utils.RoundImage;
import com.katy.telegram.Utils.TdFileHelper;
import com.katy.telegram.Utils.URLSpanNoUnderline;

import org.drinkless.td.libcore.telegram.TdApi;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context activity;
    private List<ChatEvent> events;
    private LayoutInflater layoutInflater;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    Handler seekHandler = new Handler();
    private ProgressBar progBar;

    public class ChatEventViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout layoutEvent;

        public ChatEventViewHolder(View itemView) {
            super(itemView);
            layoutEvent = (LinearLayout) itemView;
            LinearLayout.LayoutParams paramsLinearLayout = new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            layoutEvent.setLayoutParams(paramsLinearLayout);
        }
    }

    public class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        public ChatMessage assignedChatMessage;
        public ImageView avatar;
        public TextView short_name;
        public TextView time;
        public ImageView message_status;
        public TextView user_name;
        public FrameLayout message_content;
        public View rootView;
        public Context activity;

        public FrameLayout getMessageFrameLayout() {
            return message_content;
        }

        public ChatMessageViewHolder(View v) {
            super(v);
            rootView = v;
            user_name = (TextView) v.findViewById(R.id.user_name);
            time = (TextView) v.findViewById(R.id.time);
            short_name = (TextView) v.findViewById(R.id.short_name);
            avatar = (ImageView) v.findViewById(R.id.avatar);
            message_status = (ImageView) v.findViewById(R.id.message_status);
            message_content = (FrameLayout) v.findViewById(R.id.message_content);
        }

        public void assignChatMessage(ChatMessage message) {
            if (message != null) {
                message.onViewDetached(this);
            }
            assignedChatMessage = message;
            assignedChatMessage.onViewAttached(this);
        }

        public View getView() {
            return this.rootView;
        }

        public Context getActivity() {
            return activity;
        }

    }

    public ChatAdapter(Activity activity, List<ChatEvent> events) {
        this.activity = activity;
        this.events = events;
        this.layoutInflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (viewType == CHAT_MESSAGE_TYPE) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
            return new ChatMessageViewHolder(v);
        } else {
            v = new LinearLayout(parent.getContext());
            return new ChatEventViewHolder(v);
        }
    }

    public static final int CHAT_MESSAGE_TYPE = 100;
    public static final int CHAT_EVENT_TYPE = 200;

    @Override
    public int getItemViewType(int position) {
        if (events.get(position) instanceof ChatMessage) {
            return CHAT_MESSAGE_TYPE;
        } else {
            return CHAT_EVENT_TYPE;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder baseHolder, int position) {

        if (baseHolder instanceof ChatMessageViewHolder) {
            bindChatViewHolder((ChatMessageViewHolder) baseHolder, position);
        } else if (baseHolder instanceof ChatEventViewHolder) {
            bindEventViewHolder((ChatEventViewHolder) baseHolder, position);
        }
    }

    private void bindChatViewHolder(ChatMessageViewHolder holder, int position) {
        ChatMessage chatMsg = (ChatMessage) events.get(position);
        TdApi.Message message = chatMsg.getMessage();
        holder.message_content.removeAllViews();
        holder.activity = activity;
        TdApi.MessageContent messageContent = message.message;
        TdApi.User user = chatMsg.getFrom();

        //new message is not delivered to the server = before UpdateMessageId
        if (message.id >= 1000000000) {
            holder.message_status.setVisibility(View.VISIBLE);
        } else {
            holder.message_status.setVisibility(View.GONE);
            HashMap<Long, Integer> lastReadOutboxMap = ChatsManager.getInstance().getLastReadOutboxMap();
            if (lastReadOutboxMap.containsKey(chatMsg.getMessage().chatId)) {
                int lastReadMsg = lastReadOutboxMap.get(chatMsg.getMessage().chatId);
                if (lastReadMsg > 0 && message.id > lastReadMsg
                        && message.fromId == AccountManager.getInstance().getCurrentUser().id) {
                    holder.message_status.setVisibility(View.VISIBLE);
                    holder.message_status.setImageResource(R.drawable.ic_unread);
                } else {
                    holder.message_status.setVisibility(View.GONE);
                }
            }
        }

        drawUserData(holder, user, message.date);

        if (chatMsg.getMessage().forwardFromId > 0) {
            //message is forwarded
            forwardedMessages(holder, chatMsg.getMessage());
        } else {
            setMessageContentLayout(messageContent, holder.message_content);
        }
        holder.assignChatMessage(chatMsg);
    }

    private void setMessageContentLayout(TdApi.MessageContent messageContent, FrameLayout container) {
        if (messageContent instanceof TdApi.MessageText) {
            addTextToSend(container, ((TdApi.MessageText) messageContent).text);
        } else if (messageContent instanceof TdApi.MessagePhoto) {
            TdApi.PhotoSize[] photoSize = ((TdApi.MessagePhoto) messageContent).photo.photos;
            addImageToSend(container, photoSize[0], photoSize[photoSize.length - 1]);
        } else if (messageContent instanceof TdApi.MessageAudio) {
            addAudioToDownload(container, ((TdApi.MessageAudio) messageContent).audio);
        } else if (messageContent instanceof TdApi.MessageVideo) {
            addVideoToSend(container, ((TdApi.MessageVideo) messageContent).video);
        } else if (messageContent instanceof TdApi.MessageSticker) {
            addStickerToSend(container, ((TdApi.MessageSticker) messageContent).sticker);
        } else if (messageContent instanceof TdApi.MessageDocument) {
            addDocumentToSend(container, ((TdApi.MessageDocument) messageContent).document);
        } else if (messageContent instanceof TdApi.MessageGeoPoint) {
            addGeoPoint(container, ((TdApi.MessageGeoPoint) messageContent).geoPoint);
        } else {
            addTextToSend(container, "[this type of messages is not implemented yet]");
        }
    }

    private void addGeoPoint(FrameLayout container, TdApi.GeoPoint geoPoint) {
        RelativeLayout relativeLayout = (RelativeLayout) layoutInflater.inflate(R.layout.msg_geo_point, (ViewGroup) container.getRootView(), false);
        container.addView(relativeLayout);
    }

    private void addDocumentToSend(FrameLayout container, final TdApi.Document document) {
        RelativeLayout relativeLayout = (RelativeLayout) layoutInflater.inflate(R.layout.msg_document, (ViewGroup) container.getRootView(), false);
        ProgressBar progressBar = (ProgressBar) relativeLayout.findViewById(R.id.progressBar);
        ImageButton control_button = (ImageButton) relativeLayout.findViewById(R.id.control_button);
        ImageView file_thumb = (ImageView) relativeLayout.findViewById(R.id.file_thumb);
        TextView file_name = (TextView) relativeLayout.findViewById(R.id.file_name);
        TextView file_size = (TextView) relativeLayout.findViewById(R.id.file_size);

        String thumbPath = TdFileHelper.getInstance().getFile(document.thumb.photo, false);
        final String documentPath = TdFileHelper.getInstance().getFile(document.document, false);

        file_name.setText(document.fileName);

        int fileSize = 0;
        if (document.document instanceof TdApi.FileLocal) {
            fileSize = ((TdApi.FileLocal) document.document).size;
        } else {
            fileSize = ((TdApi.FileEmpty) document.document).size;
        }
        file_size.setText(FileSizeUnitsHelper.readableFileSize((long) fileSize));


        if (thumbPath != null) {
            file_thumb.setImageBitmap(BitmapFactory.decodeFile(thumbPath));
        } else {
            file_thumb.setImageBitmap(null);
        }

        if (documentPath != null) {
            progressBar.setVisibility(View.GONE);
            control_button.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_file));
            control_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    newIntent.setDataAndType(Uri.fromFile(new File(documentPath)), document.mimeType);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        activity.startActivity(newIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(activity, "No handler for this type of file.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            progressBar.setVisibility(View.VISIBLE);
            control_button.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_download_blue));
        }

        container.addView(relativeLayout);
    }

    private void addStickerToSend(FrameLayout container, TdApi.Sticker sticker) {
        RelativeLayout relativeLayout = (RelativeLayout) layoutInflater.inflate(R.layout.msg_sticker,
                (ViewGroup) container.getRootView(), false);

        ImageView stickerImage = (ImageView) relativeLayout.findViewById(R.id.sticker);
        String stickerPath = TdFileHelper.getInstance().getFile(sticker.sticker, false);
        if (stickerPath != null) {
            stickerImage.setImageBitmap(BitmapFactory.decodeFile(stickerPath));
        }

        container.addView(relativeLayout);
    }

    private void addVideoToSend(FrameLayout container, TdApi.Video video) {
        RelativeLayout relativeLayout = (RelativeLayout) layoutInflater.inflate(R.layout.msg_video,
                (ViewGroup) container.getRootView(), false);
        ImageButton controlButton = (ImageButton) relativeLayout.findViewById(R.id.control_button);
        ImageView videoThubm = (ImageView) relativeLayout.findViewById(R.id.video_thubm);
        ProgressBar progressBar = (ProgressBar) relativeLayout.findViewById(R.id.progressBar);

        final String filePath = TdFileHelper.getInstance().getFile(video.thumb.photo, false);
        videoThubm.setImageBitmap(BitmapFactory.decodeFile(filePath));

        if (filePath != null) {
            controlButton.setImageResource(R.drawable.ic_play);
            progressBar.setVisibility(View.GONE);
            controlButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newFilePath = null;
                    try {
                        newFilePath = FileSaveOrCopyHelper.copyFile(new File(filePath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newFilePath));
                    intent.setDataAndType(Uri.parse(filePath), "video/mp4");
                    BaseActivity.getCurrentActivity().startActivity(intent);
                }
            });
        } else {
            controlButton.setImageResource(R.drawable.ic_download);
            progressBar.setVisibility(View.VISIBLE);
        }

        container.addView(relativeLayout);
    }

    private void bindEventViewHolder(ChatEventViewHolder holder, int position) {
        AddParticipantNotification addParticipantNotification = null;
        NewDaySeparatorNotification newDaySeparatorNotification = null;
        UnreadMessagesBadge unreadMessagesBadge = null;
        GroupPhotoChangedNotification groupPhotoChangedNotification = null;
        GroupCreatedNotification groupCreatedNotification = null;
        holder.layoutEvent.removeAllViews();

        if (events.get(position) instanceof AddParticipantNotification) {
            addParticipantNotification = (AddParticipantNotification) events.get(position);
            holder.layoutEvent.addView(printInvitation(holder, addParticipantNotification.getWho(), addParticipantNotification.getWhom()));
        } else if (events.get(position) instanceof NewDaySeparatorNotification) {
            newDaySeparatorNotification = (NewDaySeparatorNotification) events.get(position);
            holder.layoutEvent.addView(printNewDay(newDaySeparatorNotification.getDate()));
        } else if (events.get(position) instanceof UnreadMessagesBadge) {
            unreadMessagesBadge = (UnreadMessagesBadge) events.get(position);
            holder.layoutEvent.addView(showNewMessagesEvent(unreadMessagesBadge.getUnreadCount()));
        } else if (events.get(position) instanceof GroupPhotoChangedNotification) {
            groupPhotoChangedNotification = ((GroupPhotoChangedNotification) events.get(position));
            holder.layoutEvent.addView(showGroupPhotoChangedEvent(groupPhotoChangedNotification.getChat(), groupPhotoChangedNotification.getWho()));
        } else if (events.get(position) instanceof GroupCreatedNotification) {
            groupCreatedNotification = ((GroupCreatedNotification) events.get(position));
            holder.layoutEvent.addView(showGroupCreatedEvent(groupCreatedNotification.getFromId(), groupCreatedNotification.getGroupChatName()));
        }
    }

    private View showGroupPhotoChangedEvent(TdApi.Chat chat, TdApi.User who) {
        TextView whoChangedImage = new TextView(activity);
        whoChangedImage.setTextSize(15);
        whoChangedImage.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        whoChangedImage.setVisibility(View.VISIBLE);
        whoChangedImage.setText(who.firstName + " " + who.lastName + " changed group photo");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;

        whoChangedImage.setGravity(Gravity.CENTER);
        whoChangedImage.setLayoutParams(params);
        whoChangedImage.setPadding(0, 8, 0, 8);

        return whoChangedImage;
    }

    private View showGroupCreatedEvent(int fromId, final String title) {
        final TextView whoCreatedGroup = new TextView(activity);
        whoCreatedGroup.setTextSize(15);
        whoCreatedGroup.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        whoCreatedGroup.setVisibility(View.VISIBLE);
        TgClient.send(new TdApi.GetUser(fromId), new TgClient.TLResponseCallback() {
            @Override
            public void onResult(TdApi.TLObject object) {
                TdApi.User user = (TdApi.User) object;
                whoCreatedGroup.setText(user.firstName + " " + user.lastName + " created group \"" + title + "\"");
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;

        whoCreatedGroup.setGravity(Gravity.CENTER);
        whoCreatedGroup.setLayoutParams(params);
        whoCreatedGroup.setPadding(0, 8, 0, 8);
        return whoCreatedGroup;
    }

    private void forwardedMessages(ChatMessageViewHolder holder, TdApi.Message message) {
        LinearLayout layout_forward = new LinearLayout(activity);
        layout_forward.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.forward_line));
        layout_forward.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < 1 /*TODO: aggregate forwarded messages */; i++) {
            RelativeLayout item_chat = (RelativeLayout) layoutInflater.inflate(R.layout.item_chat, (ViewGroup) holder.rootView.getRootView(), false);

            if (i < 1) {
                item_chat.setPadding(15, 0, 0, 0);
            } else {
                item_chat.setPadding(15, 20, 0, 0);
            }

            TextView short_name = (TextView) item_chat.findViewById(R.id.short_name);

            ImageView avatar = (ImageView) item_chat.findViewById(R.id.avatar);

            RoundImage roundedImage = AvatarHelper.createRoundImage(activity, null, Integer.toString(message.forwardFromId));
            AvatarHelper.createImageShortName(true, Integer.toString(message.forwardFromId), short_name);
            avatar.setImageDrawable(roundedImage);

            short_name.setPadding(0, 20, 0, 20);
            short_name.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

            TextView user_name = (TextView) item_chat.findViewById(R.id.user_name);
            user_name.setText(Integer.toString(message.forwardFromId));

            (item_chat.findViewById(R.id.message_status)).setVisibility(View.GONE);

            TextView message_text = new TextView(activity);
            message_text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            message_text.setText("Forward!!");
            message_text.setTextColor(activity.getResources().getColor(R.color.chat_message));
            setMessageContentLayout(message.message, ((FrameLayout) item_chat.findViewById(R.id.message_content)));

            layout_forward.addView(item_chat);
        }
        holder.message_content.addView(layout_forward);
    }


    private void addTextToSend(FrameLayout container, String text) {
        TextView message_text = new TextView(activity);
        message_text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        message_text.setText(text);
        message_text.setTextColor(activity.getResources().getColor(R.color.chat_message));

        container.addView(message_text);
    }

    private void addImageToSend(FrameLayout container, TdApi.PhotoSize thumbnail, TdApi.PhotoSize largePhoto) {
        RelativeLayout relativeLayout = (RelativeLayout) layoutInflater.inflate(R.layout.msg_image,
                (ViewGroup) container.getRootView(), false);
        final ImageView imageView = (ImageView) relativeLayout.findViewById(R.id.image);
        final TextView image_name = (TextView) relativeLayout.findViewById(R.id.image_name);
        TextView image_size = (TextView) relativeLayout.findViewById(R.id.image_size);
        String photoFilePath = TdFileHelper.getInstance().getFile(largePhoto.photo, false);

        String thumbPath = TdFileHelper.getInstance().getFile(thumbnail.photo, false);

        imageView.setImageBitmap(thumbPath == null ? null : BitmapFactory.decodeFile(thumbPath));

        if (photoFilePath != null) {
            TdApi.FileLocal photo = (TdApi.FileLocal) largePhoto.photo;
            image_name.setText(new File(photo.path).getName());
            relativeLayout.findViewById(R.id.progressBar).setVisibility(View.GONE);
            relativeLayout.findViewById(R.id.control_button).setVisibility(View.GONE);
        } else {
            relativeLayout.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            relativeLayout.findViewById(R.id.control_button).setVisibility(View.VISIBLE);
        }

        int photoSize = 0;
        if (largePhoto.photo instanceof TdApi.FileEmpty) {
            final TdApi.FileEmpty currentPhoto = ((TdApi.FileEmpty) largePhoto.photo);
            photoSize = currentPhoto.size;
        } else {
            final TdApi.FileLocal currentPhoto = ((TdApi.FileLocal) largePhoto.photo);
            photoSize = currentPhoto.size;
        }

        image_size.setText(FileSizeUnitsHelper.readableFileSize(photoSize));

        container.addView(relativeLayout);
    }

    private void addAudioToDownload(FrameLayout container, TdApi.Audio audio) {
        RelativeLayout relativeLayout = (RelativeLayout) layoutInflater.inflate(R.layout.msg_downloading, (ViewGroup) container.getRootView(), false);
        (relativeLayout.findViewById(R.id.downloading_audio)).setVisibility(View.VISIBLE);
//        (relativeLayout.findViewById(R.id.downloading_file)).setVisibility(View.GONE);

        seekBar = (SeekBar) relativeLayout.findViewById(R.id.seek_bar);
        seekBar.setThumb(null);

        ImageButton control_button = (ImageButton) relativeLayout.findViewById(R.id.control_button);
        control_button.setImageResource(R.drawable.ic_download_blue);

        final TextView duration = (TextView) relativeLayout.findViewById(R.id.duration);
        int minutes = audio.duration / 60;
        int seconds = audio.duration - minutes * 60;
        duration.setText(String.format("%d:%02d", minutes, seconds));

        progBar = (ProgressBar) relativeLayout.findViewById(R.id.progressBar);

        container.addView(relativeLayout);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) container.getLayoutParams();
        params.setMargins(0, 5, 0, 0);
        container.setLayoutParams(params);
    }

    private void drawUserData(ChatMessageViewHolder holder, TdApi.User user, int date) {
        RoundImage roundedImage = AvatarHelper.createRoundImage(activity,
                user.photoSmall instanceof TdApi.FileLocal ? ((TdApi.FileLocal) user.photoSmall).path : null,
                user.firstName + " " + user.lastName);
        AvatarHelper.createImageShortName(user.photoSmall instanceof TdApi.FileEmpty,
                user.firstName + " " + user.lastName, holder.short_name);
        holder.avatar.setImageDrawable(roundedImage);

        holder.user_name.setText(user.firstName + " " + user.lastName);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.avatar.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_RIGHT, R.id.avatar);
        params.addRule(RelativeLayout.ALIGN_LEFT, R.id.avatar);
        params.addRule(RelativeLayout.ALIGN_TOP, R.id.avatar);
        params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.avatar);

        holder.short_name.setGravity(Gravity.CENTER);
        holder.short_name.setLayoutParams(params);

        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        df.setTimeZone(TimeZone.getDefault());
        holder.time.setText(df.format(((long) date) * 1000L));
    }

    private TextView printNewDay(Date date) {
        TextView chat_day = new TextView(activity);
        chat_day.setTextSize(15);
        chat_day.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        chat_day.setVisibility(View.VISIBLE);
        chat_day.setText(new SimpleDateFormat("MMMM d").format(date));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;

        chat_day.setGravity(Gravity.CENTER);
        chat_day.setLayoutParams(params);
        chat_day.setPadding(0, 8, 0, 8);

        return chat_day;
    }

    private LinearLayout printInvitation(ChatEventViewHolder holder, TdApi.User whoInvite, TdApi.User invited) {
        TextView userName1 = new TextView(activity);
        //TODO: go to user's profile
        userName1.setText(Html.fromHtml(String.format("<a href=\"http://www.google.com\">%s</a> ", whoInvite.firstName + " " + whoInvite.lastName)));
        userName1.setMovementMethod(LinkMovementMethod.getInstance());
        userName1.setLinkTextColor(activity.getResources().getColor(R.color.chat_user_name));
        userName1.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        URLSpanNoUnderline.removeUnderlines((Spannable) userName1.getText());

        TextView text_invited = new TextView(activity);
        text_invited.setText("invited ");
        text_invited.setTextColor(activity.getResources().getColor(R.color.chat_extre_text));
        text_invited.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView userName2 = new TextView(activity);
        userName2.setText(Html.fromHtml(String.format("<a href=\"http://www.google.com\">%s</a> ", invited.firstName + " " + invited.lastName)));
        userName2.setMovementMethod(LinkMovementMethod.getInstance());
        userName2.setLinkTextColor(activity.getResources().getColor(R.color.chat_user_name));
        userName2.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        URLSpanNoUnderline.removeUnderlines((Spannable) userName2.getText());

        LinearLayout layoutForInvitation = new LinearLayout(activity);
        layoutForInvitation.setGravity(Gravity.CENTER_HORIZONTAL);
        layoutForInvitation.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
        layoutForInvitation.setPadding(0, 5, 0, 5);
        layoutForInvitation.addView(userName1);
        layoutForInvitation.addView(text_invited);
        layoutForInvitation.addView(userName2);

        return layoutForInvitation;
    }

    private LinearLayout showNewMessagesEvent(int unreadCount) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        TextView unread_messages = new TextView(activity);
        unread_messages.setTextSize(15);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(String.format("%d new %s", unreadCount, (unreadCount > 1 ? "messages" : "message"))).append(" ").append(" ");

        CenteredImageSpan span = new CenteredImageSpan(
                activity.getResources().getDrawable(R.drawable.ic_small_arrow));

        builder.setSpan(span, builder.length() - 1, builder.length(), 0);
        unread_messages.setText(builder);

        unread_messages.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        unread_messages.setLayoutParams(params);
        unread_messages.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        unread_messages.setTextColor(activity.getResources().getColor(R.color.chat_new_mssages_panel));

        LinearLayout layoutForNewMessages = new LinearLayout(activity);
        layoutForNewMessages.setLayoutParams(params);
        layoutForNewMessages.setBackgroundColor(activity.getResources().getColor(R.color.unread_msg));
        layoutForNewMessages.addView(unread_messages);

        return layoutForNewMessages;
    }

    Runnable run = new Runnable() {
        @Override
        public void run() {
            updateSeekBar();
        }
    };

    private void updateSeekBar() {
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
        seekHandler.postDelayed(run, 1000);
    }
}
