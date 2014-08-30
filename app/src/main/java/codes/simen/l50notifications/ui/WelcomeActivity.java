package codes.simen.l50notifications.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import codes.simen.l50notifications.OverlayServiceCommon;
import codes.simen.l50notifications.R;
import codes.simen.l50notifications.util.Mlog;

public class WelcomeActivity extends Activity {
    public static final String ACCESSIBILITY_SERVICE_NAME = "codes.simen.l50notifications/codes.simen.l50notifications.NotificationListenerAccessibilityService";
    public static final int REQUEST_CODE = 654;
    private final String logTag = "L Notifications";
    private static boolean isRunning = false;
    private SharedPreferences preferences = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.app_name);
        setContentView(R.layout.activity_welcome);
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (Build.DISPLAY.contains("MIUI")) {
            findViewById(R.id.miui_warning).setVisibility(View.VISIBLE);
        }
        if (Mlog.isLogging)
            doSendTest(null);
    }

    @Override
    protected void onResume () {
        super.onResume();
        isRunning = true;
        TextView status = (TextView) findViewById(R.id.status);
        if (
                ( Build.VERSION.SDK_INT >= 18 && isNotificationListenerEnabled() )
                || isAccessibilityEnabled()
        ) {
            status.setVisibility(View.VISIBLE);
            checkEnabled();
            if (( Build.VERSION.SDK_INT >= 18 && isNotificationListenerEnabled() )
                    && isAccessibilityEnabled() ) {
                final View bothEnabled = findViewById(R.id.bothEnabled);
                bothEnabled.setVisibility(View.VISIBLE);
                bothEnabled.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        gotoAccessibility();
                    }
                });
            } else {
                findViewById(R.id.bothEnabled).setVisibility(View.GONE);
            }
        } else {
            status.setVisibility(View.GONE);
        }

        String lastBug = preferences.getString("lastBug", null);
        if (lastBug != null) {
            if (lastBug.length() > 100) {
                lastBug = lastBug.substring(0,100);
            }
            TextView view = (TextView) findViewById(R.id.errorDisplay);
            view.setText( getString(R.string.bug_report_request).concat( lastBug ) );
            view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause () {
        super.onPause();
        isRunning = false;
    }

    private void checkEnabled() {
        if (preferences.getBoolean("running", false)) {
            TextView status = (TextView) findViewById(R.id.status);
            status.setText(getString(R.string.intro_status_on_confirmed));
        } else if (isRunning) {
            //Mlog.d(logTag, "handler");
            handler.postDelayed(runnable, 5000);
        }
    }
    private final Handler handler = new Handler();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            checkEnabled();
        }
    };

    public void onClick (View v) {
        preferences.edit()
                .putBoolean("running", false)
                .apply();
        if (Build.VERSION.SDK_INT >= 18)
            gotoNotifyservice();
        else
            gotoAccessibility();
    }

    public void doOpenSettings (View v) {
        Intent intent = new Intent();
        intent.setClass(this, SettingsActivity.class);
        startActivityForResult(intent, 0);
    }

    public void doSendTest (View v) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), OverlayServiceCommon.class);
        intent.setAction("TEST");
        intent.putExtra("packageName", getPackageName());
        intent.putExtra("title", "Simen.codes");
        intent.putExtra("text", "Thanks for trying my app! If you like it, please leave a review on Play. If you can\'t get it to work, just email me.");
        intent.putExtra("action", PendingIntent.getActivity(this, 0,
                new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=codes.simen.l50notifications"))
                , PendingIntent.FLAG_UPDATE_CURRENT));

        /*if (Build.VERSION.SDK_INT >= 11) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            intent.putExtra("iconLarge", bitmap);
        }/**/
        intent.putExtra("icon", R.drawable.ic_stat_headsup);

        intent.putExtra("actionCount", 2);
        intent.putExtra("action1title", getString(R.string.action_settings));
        intent.putExtra("action1icon", R.drawable.ic_action_settings);
        intent.putExtra("action1intent", PendingIntent.getActivity(this, 0,
                new Intent(getApplicationContext(), SettingsActivity.class),
                PendingIntent.FLAG_CANCEL_CURRENT));

        intent.putExtra("action2title", getString(R.string.leave_review));
        intent.putExtra("action2icon", R.drawable.ic_checkmark);
        intent.putExtra("action2intent", PendingIntent.getActivity(this, 0,
                new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=codes.simen.l50notifications"))
                , PendingIntent.FLAG_UPDATE_CURRENT));

        startService(intent);
    }

    public void doReport (View view) {
        Mlog.d(logTag, view.toString());

        /*if (Build.VERSION.SDK_INT >= 14) {
            try {
                Exception e = (Exception) ObjectSerializer.deserialize(preferences.getString("lastException", null));
                ApplicationErrorReport report = new ApplicationErrorReport();
                report.packageName = report.processName = getPackageName();
                report.time = System.currentTimeMillis();
                report.type = ApplicationErrorReport.TYPE_CRASH;
                report.systemApp = false;

                ApplicationErrorReport.CrashInfo crash = new ApplicationErrorReport.CrashInfo();
                crash.exceptionClassName = e.getClass().getSimpleName();
                crash.exceptionMessage = e.getMessage()
                        + " (Device: " + android.os.Build.MODEL + ")";

                StringWriter writer = new StringWriter();
                PrintWriter printer = new PrintWriter(writer);
                e.printStackTrace(printer);

                crash.stackTrace = writer.toString();

                StackTraceElement stack = e.getStackTrace()[0];
                crash.throwClassName = stack.getClassName();
                crash.throwFileName = stack.getFileName();
                crash.throwLineNumber = stack.getLineNumber();
                crash.throwMethodName = stack.getMethodName();

                report.crashInfo = crash;

                Intent intent = new Intent(Intent.ACTION_APP_ERROR);
                intent.putExtra(Intent.EXTRA_BUG_REPORT, report);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, 0);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }*/

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        try {
            final int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            intent.putExtra(Intent.EXTRA_TEXT,
                    preferences
                            .getString("lastBug", "Bug not saved") +
                            "--" + preferences.getBoolean("running", false) + " - " +
                            Build.VERSION.SDK_INT + " - " + versionCode + " - " + Build.PRODUCT
            );
            intent.putExtra(Intent.EXTRA_SUBJECT, "Bug in Heads-up " + versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            intent.putExtra(Intent.EXTRA_TEXT,
                    preferences
                            .getString("lastBug", "Bug not saved") +
                            "--" + preferences.getBoolean("running", false) + " - " +
                            Build.VERSION.SDK_INT + " - unknown version" + " - " + Build.PRODUCT
            );
            intent.putExtra(Intent.EXTRA_SUBJECT, "Bug in Heads-up");
        }
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"sb@simen.codes"});
        startActivity(Intent.createChooser(intent, "Select your email client:"));

        preferences
                .edit()
                .remove("lastBug")
                .apply();
    }

    public void doOpenSite(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://simen.codes/"));
        startActivity(intent);
    }


    void gotoNotifyservice() {
        try {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            if (isNotificationListenerEnabled())
                startActivity(intent);
            else
                startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException anfe) {
            try {
                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), getString(R.string.notification_listener_not_found_detour), Toast.LENGTH_LONG).show();
            } catch (ActivityNotFoundException anfe2) {
                Toast.makeText(getApplicationContext(), anfe2.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    void gotoAccessibility() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(getApplicationContext(), getString(R.string.accessibility_toast), Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException anfe) {
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), getString(R.string.accessibility_not_found_detour), Toast.LENGTH_LONG).show();
            } catch (ActivityNotFoundException anfe2) {
                Toast.makeText(getApplicationContext(), anfe2.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }


    boolean isAccessibilityEnabled(){
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(this.getContentResolver(),android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Mlog.d(logTag, "ACCESSIBILITY: " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Mlog.d(logTag, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled==1){


            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            Mlog.d(logTag, "Setting: " + settingValue);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessibilityService = splitter.next();
                    Mlog.d(logTag, "Setting: " + accessibilityService);
                    if (accessibilityService.equalsIgnoreCase(ACCESSIBILITY_SERVICE_NAME)){
                        Mlog.d(logTag, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }

        }
        else{
            Mlog.d(logTag, "***ACCESSIBILITY IS DISABLED***");
        }
        return false;
    }

    boolean isNotificationListenerEnabled() {
        Context context = getApplicationContext();
        try {
            //noinspection ConstantConditions
            ContentResolver contentResolver = context.getContentResolver();
            String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
            String packageName = context.getPackageName();

            // check to see if the enabledNotificationListeners String contains our package name
            return !(enabledNotificationListeners == null
                    || !enabledNotificationListeners.contains(packageName));
        } catch (NullPointerException e) {
            return false;
        }
    }
}
