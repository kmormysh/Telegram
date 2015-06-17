package com.katy.telegram.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.katy.telegram.R;

public class AvatarHelper {

    public static RoundImage createRoundImage(Context context, String image, String user_name) {
        Bitmap bm = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        if (image != null && !image.equals("")) {
            bm = BitmapFactory.decodeFile(image);
        } else {
            String[] avatarColors = context.getResources().getStringArray(R.array.avatar_colors);
            String contactColor = avatarColors[Math.abs(user_name.hashCode() % (avatarColors.length - 1))];
            bm.eraseColor(Color.parseColor(contactColor));
        }
        return new RoundImage(bm);
    }

    public static void createImageShortName(boolean visibility, String user_name, TextView short_name) {
        if (visibility) {
            short_name.setVisibility(View.VISIBLE);
            String[] userName = user_name.toUpperCase().split(" ");
            short_name.setText(userName[0].substring(0, 1));
            if (userName.length > 1) {
                short_name.setText(short_name.getText() + userName[1].substring(0, 1));
            }
        } else {
            short_name.setVisibility(View.GONE);
        }
    }
}
