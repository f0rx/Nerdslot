package org.nerdslot.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.Exclude;
import com.google.firebase.storage.StorageException;

import org.jetbrains.annotations.NotNull;
import org.nerdslot.Foundation.Nerdslot;

public interface RootInterface extends OnFailureListener {
    String NETWORK_AVAILABLE_ACTION = "Network-Available-Action";
    String IS_NETWORK_AVAILABLE = "isNetworkAvailable";
    String SHARED_PREF_FILE = "sharedPrefs";
    String IS_ADMIN_SHARED_PREF = "is-admin";
    String USER_EMAIL_PERMISSION = "email";
    String USER_PROFILE_PERMISSION = "public_profile";
    String USER_BIRTHDAY_PERMISSION = "user_birthday";
    String USER_FRIENDS_PERMISSION = "user_friends";
    String AUTH_BUG_RESPONSE = "An authentication error occurred! The Admin has been notified!";
    String TAG = "log-tag";
    String TOS = "https://nerdslot.org"; // Terms of Service URL
    int JOB_ID = 100;
    int SELECT_FILE_REQUEST_CODE = 110;

    // Log Strings
    String OPERATION_CANCELLED = "Operation cancelled by User.";

    // References
    String ADMIN_NODE_REFERENCE = "administrators";
    String MAGAZINE_COVER_NODE = "cover";
    String USERS_NODE_REFERENCE = "users";

    default void sendToast(Activity context, String msg) {
        Toast.makeText(context, msg != null && !TextUtils.isEmpty(msg) ? msg : "No Message", Toast.LENGTH_LONG).show();
    }

    default void sendSnackbar(View rootView, String msg) {
        sendSnackbar(rootView, msg, null);
    }

    default void sendSnackbar(View rootView, String msg, String actionMsg) {
        Snackbar sn = Snackbar.make(rootView,
                msg != null && !TextUtils.isEmpty(msg) ? msg : "No Message",
                BaseTransientBottomBar.LENGTH_LONG);
        sn.setAction(actionMsg != null && !TextUtils.isEmpty(actionMsg) ? actionMsg : "Okay", v -> sn.dismiss());
        sn.setActionTextColor(Color.WHITE);
        sn.show();
    }

    default void sendResponse(String msg) {
        sendResponse(null, msg, null);
    }

    default void sendResponse(@Nullable Activity context, String msg) {
        sendResponse(context, msg, null);
    }

    default void sendResponse(String msg, Exception ex) {
        sendResponse(null, msg, ex);
    }

    default void sendResponse(@Nullable Activity context, String msg, @Nullable Exception ex) {
        Log.i(TAG, "sendResponse: " + msg, ex);
        if (context != null)
            Toast.makeText(context, msg != null && !TextUtils.isEmpty(msg) ? msg : "No Message", Toast.LENGTH_LONG).show();
    }

    @Exclude
    default boolean getAuthorizationStatus() {
        SharedPreferences sharedPreferences = Nerdslot.getContext().getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(IS_ADMIN_SHARED_PREF, false);
    }

    @Exclude
    default void setAuthorizationStatus(boolean status) {
        SharedPreferences sharedPreferences = Nerdslot.getContext().getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(IS_ADMIN_SHARED_PREF, status);
        editor.apply();
    }

    @Exclude
    default void resetAuthorizationStatus() {
        SharedPreferences sharedPreferences = Nerdslot.getContext().getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(IS_ADMIN_SHARED_PREF);
        editor.apply();
    }

    default void setEnabled(@NotNull View v, boolean enabled) {
        v.setEnabled(enabled);
    }

    default void setEnabled(boolean enabled, @NotNull View... views) {
        for (View v : views) {
            v.setEnabled(enabled);
        }
    }

    default void setVisibility(@NotNull View v, int visibility) {
        v.setVisibility(visibility);
    }

    default void setVisibility(int visibility, @NotNull View... views) {
        for (View v : views) {
            v.setVisibility(visibility);
        }
    }

    default void setError(@NonNull View v) {
        setError(v, null);
    }

    default void setError(@NonNull View v, @Nullable String errorText) {
        if (v instanceof TextInputLayout) {
            ((TextInputLayout) v).setError(
                    errorText != null && !TextUtils.isEmpty(errorText) && !errorText.equalsIgnoreCase("")
                            ? errorText
                            : "Field is required!");
        }
    }

    default void validateTextInput(TextInputLayout layout, String string) {
        if (!TextUtils.isEmpty(string)) {
            resetError(layout);
        } else {
            setError(layout, "Field is required!");
        }
    }

    default void resetError(@NonNull View v) {
        if (v instanceof TextInputLayout) ((TextInputLayout) v).setError(null);
    }

    default void resetError(@NonNull View... view) {
        for (View v : view) {
            if (v instanceof TextInputLayout) ((TextInputLayout) v).setError(null);
        }
    }

    default void resetView(@NonNull View v) {
        resetView(v, null);
    }

    default void resetView(@NonNull View v, @Nullable String value) {
        if (v instanceof Button) {
            return;
        }
        if (v instanceof EditText) {
            ((EditText) v).setText(value != null && !value.equals("") ? value : "");
            ((EditText) v).setError(null);
        }
    }

    default void resetViews(@NonNull View... views) {
        resetViews(null, views);
    }

    default void resetViews(@Nullable String value, @NotNull View... views) {
        for (View v : views) {
            if (v instanceof Button) {
                return;
            }

            if (v instanceof EditText) {
                ((EditText) v).setText(value != null && !value.equals("") ? value : "");
                ((EditText) v).setError(null);
            }
        }
    }

    @Override
    default void onFailure(@NonNull Exception e) {
        int errorCode = ((StorageException) e).getErrorCode();
        String errorMsg = e.getLocalizedMessage();

        switch (errorCode){
            case StorageException.ERROR_NOT_AUTHENTICATED:
                sendResponse((Activity) Nerdslot.getContext(), errorMsg);
            case StorageException.ERROR_BUCKET_NOT_FOUND:
                sendResponse((Activity) Nerdslot.getContext(), errorMsg);
            case StorageException.ERROR_NOT_AUTHORIZED:
                sendResponse((Activity) Nerdslot.getContext(), "Sorry, you're not authorized to view this.");
            case StorageException.ERROR_OBJECT_NOT_FOUND:
                sendResponse((Activity) Nerdslot.getContext(), "File or Object not found!");
            case StorageException.ERROR_RETRY_LIMIT_EXCEEDED:
                sendResponse((Activity) Nerdslot.getContext(), "Retry Limit Exceeded!");
            case StorageException.ERROR_CANCELED:
                sendResponse((Activity) Nerdslot.getContext(), "Request Cancelled!");
            case StorageException.ERROR_QUOTA_EXCEEDED:
                sendResponse(errorMsg);
            case StorageException.ERROR_UNKNOWN:
                sendResponse((Activity) Nerdslot.getContext(), "Oops! Error unknown.");
        }
    }

    enum MIME_TYPE {
        JPG("image/jpg", 0),
        PNG("image/png", 1),
        IMAGE("image/*", 2),
        EPUB("application/epub+zip", 3),
        PDF("application/pdf", 4),
        TXT("text/plain", 5),
        ZIP("application/zip", 6),
        DOC("application/msword", 7);

        private String mime;
        private int ordinal;

        MIME_TYPE(String mime, int ordinal) {
            this.mime = mime;
            this.ordinal = ordinal;
        }

        @NonNull
        @Override
        public String toString() {
            return mime;
        }

        public int getOrdinal() {
            return ordinal;
        }
    }
}
