package com.xtiger.chatui;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.*;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.os.Handler;
import android.view.Gravity;
import android.text.Html;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.net.URL;
import java.io.InputStream;
import java.io.File;
import android.os.AsyncTask;
import java.util.ArrayList;
import java.util.List;

public class ChatUI extends AndroidNonvisibleComponent implements Component {

    private ComponentContainer container;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private int sentMessageColor = Color.parseColor("#0084ff");
    private int receivedMessageColor = Color.parseColor("#f0f0f0");
    private int sentTextColor = Color.WHITE;
    private int receivedTextColor = Color.BLACK;
    private float messageCornerRadius = 20f;
    private int avatarSize = 40;
    private int messageMaxWidth = 250;
    private int messageHorizontalPadding = 16;
    private int messageVerticalPadding = 12;
    private boolean showTimestamp = true;
    private boolean showReadStatus = true;
    private int backgroundColor = Color.WHITE;
    private String fontFamily = "sans-serif";
    private int fontSize = 14;
    private boolean showTypingIndicator = false;
    private Handler typingHandler = new Handler();
    private Runnable typingRunnable;
    private List<String> reactions = new ArrayList<>();
    private Typeface typeface;
    private String backgroundImagePath;
    private List<Integer> gradientColors;

    public ChatUI(ComponentContainer container) {
        super(container.$form());
        this.container = container;
        reactions.add("👍");
        reactions.add("❤️");
        reactions.add("😆");
        reactions.add("😮");
        reactions.add("😢");
        reactions.add("😡");
        gradientColors = new ArrayList<>();
    }

    @SimpleFunction(description = "Initialize the chat UI in a VerticalArrangement")
    public void Initialize(VerticalArrangement arrangement) {
        chatContainer = new LinearLayout(container.$context());
        chatContainer.setOrientation(LinearLayout.VERTICAL);

        scrollView = new ScrollView(container.$context());
        scrollView.addView(chatContainer);

        FrameLayout frameLayout = (FrameLayout) arrangement.getView();
        frameLayout.addView(scrollView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        
        updateBackground();
    }

    @SimpleFunction(description = "Send a message")
    public void Send(String message, String avatarUrl, String senderName) {
        addMessageToUI(message, avatarUrl, true, senderName);
    }

    @SimpleFunction(description = "Receive a message")
    public void Receive(String message, String avatarUrl, String receiverName) {
        addMessageToUI(message, avatarUrl, false, receiverName);
    }

    @SimpleFunction(description = "Show typing indicator")
    public void ShowTypingIndicator() {
        showTypingIndicator = true;
        addTypingIndicator();
    }

    @SimpleFunction(description = "Hide typing indicator")
    public void HideTypingIndicator() {
        showTypingIndicator = false;
        removeTypingIndicator();
    }

    @SimpleFunction(description = "Add reaction to a message")
    public void AddReaction(int messageIndex, String reaction) {
        if (messageIndex >= 0 && messageIndex < chatContainer.getChildCount()) {
            View messageView = chatContainer.getChildAt(messageIndex);
            if (messageView instanceof LinearLayout) {
                LinearLayout reactionContainer = new LinearLayout(container.$context());
                reactionContainer.setOrientation(LinearLayout.HORIZONTAL);
                TextView reactionView = new TextView(container.$context());
                reactionView.setText(reaction);
                reactionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                reactionView.setPadding(8, 4, 8, 4);
                GradientDrawable shape = new GradientDrawable();
                shape.setColor(Color.parseColor("#E0E0E0"));
                shape.setCornerRadius(16);
                reactionView.setBackground(shape);
                reactionContainer.addView(reactionView);
                ((LinearLayout) messageView).addView(reactionContainer);
            }
        }
    }

    @SimpleProperty(description = "Set the color for sent messages")
    public void SentMessageColor(int color) {
        sentMessageColor = color;
    }

    @SimpleProperty(description = "Set the color for received messages")
    public void ReceivedMessageColor(int color) {
        receivedMessageColor = color;
    }

    @SimpleProperty(description = "Set the text color for sent messages")
    public void SentTextColor(int color) {
        sentTextColor = color;
    }

    @SimpleProperty(description = "Set the text color for received messages")
    public void ReceivedTextColor(int color) {
        receivedTextColor = color;
    }

    @SimpleProperty(description = "Set the corner radius for message bubbles")
    public void MessageCornerRadius(float radius) {
        messageCornerRadius = radius;
    }

    @SimpleProperty(description = "Set the size of avatar images")
    public void AvatarSize(int size) {
        avatarSize = size;
    }

    @SimpleProperty(description = "Set the maximum width of message bubbles")
    public void MessageMaxWidth(int width) {
        messageMaxWidth = width;
    }

    @SimpleProperty(description = "Set the horizontal padding of message bubbles")
    public void MessageHorizontalPadding(int padding) {
        messageHorizontalPadding = padding;
    }

    @SimpleProperty(description = "Set the vertical padding of message bubbles")
    public void MessageVerticalPadding(int padding) {
        messageVerticalPadding = padding;
    }

    @SimpleProperty(description = "Show or hide message timestamps")
    public void ShowTimestamp(boolean show) {
        showTimestamp = show;
    }

    @SimpleProperty(description = "Show or hide read status for sent messages")
    public void ShowReadStatus(boolean show) {
        showReadStatus = show;
    }

    @SimpleProperty(description = "Set the background color of the chat")
    public void BackgroundColor(int color) {
        backgroundColor = color;
        updateBackground();
    }

    @SimpleProperty(description = "Set the background image of the chat")
    public void BackgroundImage(String imagePath) {
        backgroundImagePath = imagePath;
        updateBackground();
    }

    @SimpleProperty(description = "Set the font family for messages")
    public void FontFamily(String typefacePath) {
        loadTypeface(typefacePath);
    }

    @SimpleProperty(description = "Set the font size for messages")
    public void FontSize(int size) {
        fontSize = size;
    }

    private void loadTypeface(String typefacePath) {
        try {
            if (isCompanion()) {
                final String packageName = form.getPackageName();
                final String platform = packageName.contains("makeroid")
                        ? "Makeroid"
                        : packageName.contains("Niotron")
                        ? "Niotron"
                        : packageName.contains("Appzard")
                        ? "Appzard"
                        : "AppInventor";
                typefacePath = android.os.Build.VERSION.SDK_INT > 28
                        ? "/storage/emulated/0/Android/data/" + packageName + "/files/assets/" + typefacePath
                        : "/storage/emulated/0/" + platform + "/assets/" + typefacePath;
                typeface = Typeface.createFromFile(new File(typefacePath));
            } else {
                typeface = Typeface.createFromAsset(form.$context().getAssets(), typefacePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            typeface = Typeface.DEFAULT;
        }
    }

    private void updateBackground() {
        if (scrollView != null) {
            if (backgroundImagePath != null && !backgroundImagePath.isEmpty()) {
                try {
                    Bitmap bitmap;
                    if (isCompanion()) {
                        bitmap = BitmapFactory.decodeFile(backgroundImagePath);
                    } else {
                        InputStream inputStream = form.$context().getAssets().open(backgroundImagePath);
                        bitmap = BitmapFactory.decodeStream(inputStream);
                    }
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(container.$context().getResources(), bitmap);
                    scrollView.setBackground(bitmapDrawable);
                } catch (Exception e) {
                    e.printStackTrace();
                    scrollView.setBackgroundColor(backgroundColor);
                }
            } else if (!gradientColors.isEmpty()) {
                GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    gradientColors.stream().mapToInt(Integer::intValue).toArray()
                );
                scrollView.setBackground(gradient);
            } else {
                scrollView.setBackgroundColor(backgroundColor);
            }
        }
    }

    private void addMessageToUI(String message, String avatarUrl, boolean isSent, String name) {
        LinearLayout messageLayout = new LinearLayout(container.$context());
        messageLayout.setOrientation(LinearLayout.VERTICAL);
        messageLayout.setPadding(0, 8, 0, 8);

        TextView nameView = createNameView(name, isSent);
        messageLayout.addView(nameView);

        LinearLayout contentLayout = new LinearLayout(container.$context());
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);

        ImageView avatarView = createAvatarView();
        loadAvatarImage(avatarView, avatarUrl);
        TextView messageView = createMessageView(message, isSent);
        TextView timeView = createTimeView();
        TextView statusView = createStatusView(isSent);

        LinearLayout textLayout = new LinearLayout(container.$context());
        textLayout.setOrientation(LinearLayout.VERTICAL);

        if (isSent) {
            textLayout.addView(messageView);
            if (showTimestamp) textLayout.addView(timeView);
            if (showReadStatus) textLayout.addView(statusView);
            contentLayout.addView(textLayout);
            contentLayout.addView(avatarView);
            contentLayout.setGravity(Gravity.RIGHT);
        } else {
            contentLayout.addView(avatarView);
            textLayout.addView(messageView);
            if (showTimestamp) textLayout.addView(timeView);
            contentLayout.addView(textLayout);
            contentLayout.setGravity(Gravity.LEFT);
        }

        messageLayout.addView(contentLayout);
        chatContainer.addView(messageLayout);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private TextView createNameView(String name, boolean isSent) {
        TextView nameView = new TextView(container.$context());
        nameView.setText(name);
        nameView.setTextColor(isSent ? sentTextColor : receivedTextColor);
        nameView.setTypeface(typeface != null ? typeface : Typeface.DEFAULT_BOLD);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize - 2);
        nameView.setPadding(isSent ? 0 : avatarSize + 16, 4, isSent ? avatarSize + 16 : 0, 4);
        nameView.setGravity(isSent ? Gravity.RIGHT : Gravity.LEFT);
        return nameView;
    }

    private ImageView createAvatarView() {
        ImageView avatarView = new ImageView(container.$context());
        int avatarSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, avatarSize, container.$context().getResources().getDisplayMetrics());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(avatarSizePx, avatarSizePx);
        params.setMargins(8, 0, 8, 0);
        avatarView.setLayoutParams(params);
        return avatarView;
    }

    private void loadAvatarImage(ImageView avatarView, String avatarUrl) {
        if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
            new DownloadImageTask(avatarView).execute(avatarUrl);
        } else {
            Bitmap avatarBitmap = BitmapFactory.decodeFile(avatarUrl);
            avatarView.setImageBitmap(avatarBitmap);
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                bmImage.setImageBitmap(result);
            }
        }
    }

    private TextView createMessageView(String message, boolean isSent) {
        TextView messageView = new TextView(container.$context());
        messageView.setText(message);
        messageView.setTextColor(isSent ? sentTextColor : receivedTextColor);
        messageView.setPadding(messageHorizontalPadding, messageVerticalPadding, messageHorizontalPadding, messageVerticalPadding);
        messageView.setTypeface(typeface != null ? typeface : Typeface.DEFAULT);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(isSent ? sentMessageColor : receivedMessageColor);
        shape.setCornerRadius(messageCornerRadius);
        messageView.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 2, 4, 2);
        messageView.setLayoutParams(params);

        int maxWidthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, messageMaxWidth, container.$context().getResources().getDisplayMetrics());
        messageView.setMaxWidth(maxWidthPx);

        return messageView;
    }

    private TextView createTimeView() {
        TextView timeView = new TextView(container.$context());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        timeView.setText(currentTime);
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        timeView.setTextColor(Color.GRAY);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 2, 8, 2);
        timeView.setLayoutParams(params);

        return timeView;
    }

    private TextView createStatusView(boolean isSent) {
        TextView statusView = new TextView(container.$context());
        if (isSent) {
            statusView.setText(Html.fromHtml("&#x2713;&#x2713;")); // Double check mark
            statusView.setTextColor(Color.BLUE);
        }
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 2, 8, 2);
        statusView.setLayoutParams(params);

        return statusView;
    }

    private void addTypingIndicator() {
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }

        typingRunnable = new Runnable() {
            private int dotCount = 0;

            @Override
            public void run() {
                if (showTypingIndicator) {
                    StringBuilder dots = new StringBuilder();
                    for (int i = 0; i < dotCount; i++) {
                        dots.append(".");
                    }
                    TextView typingView = new TextView(container.$context());
                    typingView.setText("Typing" + dots.toString());
                    typingView.setTextColor(Color.GRAY);
                    typingView.setPadding(16, 8, 16, 8);

                    chatContainer.addView(typingView);
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));

                    dotCount = (dotCount + 1) % 4;
                    typingHandler.postDelayed(this, 500);
                }
            }
        };

        typingHandler.post(typingRunnable);
    }

    private void removeTypingIndicator() {
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }
        int lastIndex = chatContainer.getChildCount() - 1;
        if (lastIndex >= 0) {
            View lastView = chatContainer.getChildAt(lastIndex);
            if (lastView instanceof TextView) {
                TextView lastTextView = (TextView) lastView;
                if (lastTextView.getText().toString().startsWith("Typing")) {
                    chatContainer.removeViewAt(lastIndex);
                }
            }
        }
    }

    private boolean isCompanion() {
        return container.$context().getPackageName().contains("makeroid") ||
               container.$context().getPackageName().contains("Niotron") ||
               container.$context().getPackageName().contains("Appzard") ||
               container.$context().getPackageName().contains("appinventor");
    }
}