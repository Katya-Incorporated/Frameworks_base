package com.android.server.pm.ext;

import android.annotation.UserIdInt;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.ext.PackageId;
import android.util.Slog;

public class GmsCoreUtils {
    private static final String TAG = "GmsCoreUtils";

    public static boolean isGmsRemoteCredentialsServiceComponent(ComponentName componentName) {
        // FIDO2 is from "remote devices", so it's handed by the RemoteService
        return componentName != null
                && PackageId.GMS_CORE_NAME.equals(componentName.getPackageName())
                && "com.google.android.gms.auth.api.credentials.credman.service.RemoteService"
                    .equals(componentName.getClassName());
    }

    public static boolean shouldBypassRemoteEntryCredentialProviderRestrictions(
            Context context, ComponentName remoteCredentialProvider, @UserIdInt int userId) {
        if (!isGmsRemoteCredentialsServiceComponent(remoteCredentialProvider)) {
            return false;
        }

        // Ensure GMS is installed for the user that the credential request is for
        final ApplicationInfo gmsAppInfo;
        try {
            gmsAppInfo = context.getPackageManager().getApplicationInfoAsUser(
                    remoteCredentialProvider.getPackageName(), 0, userId);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "failed to resolve " + remoteCredentialProvider, e);
            return false;
        }
        // getApplicationInfoAsUser is @NonNull, but just mimicking upstream code from
        // ProviderSession
        if (gmsAppInfo != null) {
            final int packageId = gmsAppInfo.ext().getPackageId();
            // ensure it's from verified GMS core
            if (packageId == PackageId.GMS_CORE) {
                // Note: Not checking for Manifest.permission.PROVIDE_REMOTE_CREDENTIALS
                // (signature|privileged|role); it seems FIDO2 works fine without granting that
                // permission
                return true;
            } else {
                Slog.w(TAG,"bad gmsAppInfo packageId " + packageId + " for "
                        + remoteCredentialProvider);
            }
        }

        return false;
    }
}
