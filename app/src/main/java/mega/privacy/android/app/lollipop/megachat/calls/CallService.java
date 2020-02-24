package mega.privacy.android.app.lollipop.megachat.calls;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import java.io.File;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.listeners.CallListener;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRoom;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public class CallService extends Service{

    MegaApplication app;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    long chatId;

    private Notification.Builder mBuilder;
    private NotificationCompat.Builder mBuilderCompat;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilderCompatO;

    private String notificationChannelId = NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID;
    private String notificationChannelName = NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME;
    private CallListener callListener = new CallListener(this);

    public void onCreate() {
        super.onCreate();
        logDebug("onCreate");

        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();
        megaChatApi.addChatCallListener(callListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            mBuilder = new Notification.Builder(this);
        mBuilderCompat = new NotificationCompat.Builder(this);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logDebug("onStartCommand");

        if (intent == null) {
            stopSelf();
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            chatId = extras.getLong(CHAT_ID, -1);
            logDebug("Chat handle to call: " + chatId);
        }

        if (MegaApplication.getOpenCallChatId() != chatId) {
            MegaApplication.setOpenCallChatId(chatId);
        }
        showCallInProgressNotification();
        return START_NOT_STICKY;
    }

    public void updateNotificationContent(MegaChatCall call) {
        logDebug("updateNotification");
        Notification notif;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (call == null) {
                mBuilderCompatO.setContentText(getString(R.string.action_notification_call_in_progress));
            } else {
                if (call.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    mBuilderCompatO.setContentText(getString(R.string.outgoing_call_starting));
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    mBuilderCompatO.setContentText(getString(R.string.title_notification_incoming_call));
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING || call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    mBuilderCompatO.setContentText(getString(R.string.title_notification_call_in_progress));
                }
            }
            notif = mBuilderCompatO.build();
        } else {
            if (call == null) {
                mBuilderCompat.setContentText(getString(R.string.action_notification_call_in_progress));
            } else {
                if (call.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    mBuilderCompat.setContentText(getString(R.string.outgoing_call_starting));
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    mBuilderCompat.setContentText(getString(R.string.title_notification_incoming_call));
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING || call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    mBuilderCompat.setContentText(getString(R.string.title_notification_call_in_progress));
                }
            }
            notif = mBuilderCompat.build();
        }
        startForeground(NOTIFICATION_CALL_IN_PROGRESS, notif);
    }

    private void showCallInProgressNotification() {
        logDebug("showCallInProgressNotification");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(channel);
            Intent intent = new Intent(this, ChatCallActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(CHAT_ID, chatId);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

            mBuilderCompatO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .addAction(R.drawable.ic_phone_white, getString(R.string.button_notification_call_in_progress), pendingIntent)
                    .setOngoing(false)
                    .setColor(ContextCompat.getColor(this, R.color.mega));

            String title;
            String email;
            long userHandle;
            MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
            if (chat != null) {
                title = chat.getTitle();

                if (chat.isGroup()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Bitmap largeIcon = createDefaultAvatar(-1, title);
                        if (largeIcon != null) {
                            mBuilderCompatO.setLargeIcon(largeIcon);
                        }
                    }
                } else {
                    userHandle = chat.getPeerHandle(0);
                    email = chat.getPeerEmail(0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Bitmap largeIcon = setProfileContactAvatar(userHandle, title, email);
                        if (largeIcon != null) {
                            mBuilderCompatO.setLargeIcon(largeIcon);
                        }
                    }
                }

                mBuilderCompatO.setContentTitle(title);
                MegaChatCall call = megaChatApi.getChatCall(chatId);
                updateNotificationContent(call);

            } else {
                mBuilderCompatO.setContentTitle(getString(R.string.title_notification_call_in_progress));
                mBuilderCompatO.setContentText(getString(R.string.action_notification_call_in_progress));
                Notification notif = mBuilderCompatO.build();
                startForeground(NOTIFICATION_CALL_IN_PROGRESS, notif);
            }

        } else {

            mBuilderCompat = new NotificationCompat.Builder(this);
            mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent intent = new Intent(this, ChatCallActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(CHAT_ID, chatId);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .addAction(R.drawable.ic_phone_white, getString(R.string.button_notification_call_in_progress), pendingIntent)
                    .setOngoing(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilderCompat.setColor(ContextCompat.getColor(this, R.color.mega));
            }

            String title;
            String email;
            long userHandle;
            MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
            if (chat != null) {
                title = chat.getTitle();

                if (chat.isGroup()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Bitmap largeIcon = createDefaultAvatar(-1, title);
                        if (largeIcon != null) {
                            mBuilderCompat.setLargeIcon(largeIcon);
                        }
                    }
                } else {
                    userHandle = chat.getPeerHandle(0);
                    email = chat.getPeerEmail(0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Bitmap largeIcon = setProfileContactAvatar(userHandle, title, email);
                        if (largeIcon != null) {
                            mBuilderCompat.setLargeIcon(largeIcon);
                        }
                    }
                }

                mBuilderCompat.setContentTitle(title);
                MegaChatCall call = megaChatApi.getChatCall(chatId);
                updateNotificationContent(call);

            } else {
                mBuilderCompat.setContentTitle(getString(R.string.title_notification_call_in_progress));
                mBuilderCompat.setContentText(getString(R.string.action_notification_call_in_progress));
                Notification notif = mBuilderCompat.build();
                startForeground(NOTIFICATION_CALL_IN_PROGRESS, notif);
            }
        }
    }

    public Bitmap setProfileContactAvatar(long userHandle, String fullName, String email) {
        Bitmap bitmap = null;
        File avatar = buildAvatarFile(getApplicationContext(), email + ".jpg");

        if (isFileAvailable(avatar)) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                bitmap = getCircleBitmap(bitmap);
                if (bitmap != null) {
                    return bitmap;
                } else {
                    return createDefaultAvatar(userHandle, fullName);
                }
            } else {
                return createDefaultAvatar(userHandle, fullName);
            }
        } else {
            return createDefaultAvatar(userHandle, fullName);
        }
    }

    private Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    private Bitmap createDefaultAvatar(long userHandle, String fullName) {
        int color;
        if (userHandle != -1) {
            color = getColorAvatar(this, megaApi, userHandle);
        } else {
            color = ContextCompat.getColor(this, R.color.divider_upgrade_account);
        }
        return getDefaultAvatar(this, color, fullName, AVATAR_SIZE, true);
    }
    public void checkDestroyCall(){
        stopForeground(true);
        mNotificationManager.cancel(NOTIFICATION_CALL_IN_PROGRESS);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        logDebug("onDestroy");

        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(callListener);
        }

        mNotificationManager.cancel(NOTIFICATION_CALL_IN_PROGRESS);

        MegaApplication.setOpenCallChatId(-1);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
