package com.katy.telegram.Models.Messages;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;

import com.katy.telegram.Activities.BaseActivity;
import com.katy.telegram.Adapters.ChatAdapter;

import org.drinkless.td.libcore.telegram.TdApi;

import java.util.Locale;

public class GeoPointChatMessage extends ChatMessage {

    public GeoPointChatMessage(TdApi.Chat chat, TdApi.Message message, TdApi.User from) {
        super(chat, message, from);
    }

    public TdApi.MessageGeoPoint getMessageGeoPoint() {
        return (TdApi.MessageGeoPoint) getMessage().message;
    }

    @Override
    public void onViewAttached(ChatAdapter.ChatMessageViewHolder holder) {
        super.onViewAttached(holder);

        FrameLayout relativeLayout = getAssignedViewHolder().getMessageFrameLayout();
        if (relativeLayout != null) {
            relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?q=loc:%f,%f", getMessageGeoPoint().geoPoint.latitude, getMessageGeoPoint().geoPoint.longitude);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                    BaseActivity.getCurrentActivity().startActivity(intent);
                }
            });
        }
    }
}
