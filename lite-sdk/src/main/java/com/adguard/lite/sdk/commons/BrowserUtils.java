/**
 * This file is part of AdGuard Content Blocker (https://github.com/AdguardTeam/ContentBlocker).
 * Copyright © 2018 AdGuard Content Blocker. All rights reserved.
 * <p>
 * AdGuard Content Blocker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * <p>
 * AdGuard Content Blocker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * AdGuard Content Blocker.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.adguard.lite.sdk.commons;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Some functions for working with browsers
 */
public class BrowserUtils {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserUtils.class);

    private static final String YANDEX = "yandex";
    private static final String SAMSUNG = "samsung";

    public static final String YANDEX_BROWSER_PACKAGE = "com.yandex.browser";
    public static final String SAMSUNG_BROWSER_PACKAGE = "com.sec.android.app.sbrowser";
    private static final String SAMSUNG_CONTENT_BLOCKER_ACTION = "com.samsung.android.sbrowser.contentBlocker.ACTION_SETTING";
    private static final String YANDEX_CONTENT_BLOCKER_ACTION = "com.yandex.browser.contentBlocker.ACTION_SETTING";

    private static final String SAMSUNG_PACKAGE_PREFIX = "com.sec.";

    private static final String SAMSUNG_OPTIONS_URI = "internet://extension";
    private static final String SAMSUNG_EXTRA_NAME = "sbrowser.extensions.show_fragment";
    private static final String SAMSUNG_EXTRA_VALUE = "com.sec.android.app.sbrowser.blockers.content_block.view.ContentBlockPreferenceFragment";

    public static final String REFERRER = "adguard1";

    private static final List<String> yandexBrowserPackageList = new ArrayList<>();
    private static final List<String> samsungBrowserPackageList = new ArrayList<>();

    static {
        yandexBrowserPackageList.add("com.yandex.browser");
        yandexBrowserPackageList.add("com.yandex.browser.beta");
        yandexBrowserPackageList.add("com.yandex.browser.alpha");

        samsungBrowserPackageList.add("com.sec.android.app.sbrowser");
        samsungBrowserPackageList.add("com.sec.android.app.sbrowser.beta");
    }

    public static Set<String> getBrowsersAvailableByIntent(Context context) {
        Set<String> result = new HashSet<>();
        Intent intent = new Intent();
        intent.setAction(SAMSUNG_CONTENT_BLOCKER_ACTION);
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            for (ResolveInfo info : list) {
                if (info.activityInfo.packageName.contains(YANDEX)) {
                    result.add(YANDEX);
                } else if (info.activityInfo.packageName.contains(SAMSUNG_PACKAGE_PREFIX) || info.activityInfo.packageName.contains(SAMSUNG)) {
                    result.add(SAMSUNG);
                }
            }
        }

        return result;
    }

    public static Set<String> getBrowsersAvailableByPackage(Context context) {
        Set<String> result = new HashSet<>();
        List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.packageName.startsWith(YANDEX_BROWSER_PACKAGE)) {
                result.add(YANDEX);
            } else if (packageInfo.packageName.startsWith(SAMSUNG_BROWSER_PACKAGE)) {
                result.add(SAMSUNG);
            }
        }

        return result;
    }

    public static Set<String> getKnownBrowsers() {
        Set<String> result = new HashSet<>();
        result.addAll(yandexBrowserPackageList);
        result.addAll(samsungBrowserPackageList);
        return result;
    }

    public static void openSamsungBlockingOptions(Context context) {
        try {
            String packageName = getSamsungPackage(context);
            if (packageName == null) {
                LOG.error("Samsung browser not found");
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(SAMSUNG_OPTIONS_URI));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage(packageName);
            intent.putExtra(SAMSUNG_EXTRA_NAME, SAMSUNG_EXTRA_VALUE);

            startActivity(context, intent);
        } catch (Throwable th) {
            LOG.error("Failed to open Samsung browser settings", th);
        }
    }

    public static boolean isSamsungBrowserAvailable(Context context) {
        Intent intent = new Intent();
        intent.setAction(SAMSUNG_CONTENT_BLOCKER_ACTION);
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            for (ResolveInfo info : list) {
                if (info.activityInfo.packageName.contains(SAMSUNG_PACKAGE_PREFIX) || info.activityInfo.packageName.contains(SAMSUNG)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void openYandexBlockingOptions(Context context) {
        Intent intent = new Intent();
        intent.setAction(YANDEX_CONTENT_BLOCKER_ACTION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            startActivity(context, intent);
            return;
        }

        // For samsung-type action in Yandex browser
        intent.setAction(SAMSUNG_CONTENT_BLOCKER_ACTION);
        list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {

            ComponentName componentName = getYandexBrowser(context, SAMSUNG_CONTENT_BLOCKER_ACTION);
            if (componentName != null) {
                intent.setClassName(componentName.getPackageName(), componentName.getClassName());
            }

            startActivity(context, intent);
        }
    }

    public static boolean isYandexBrowserAvailable(Context context) {
        Intent intent = new Intent();
        intent.setAction(YANDEX_CONTENT_BLOCKER_ACTION);
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            for (ResolveInfo info : list) {
                if (info.activityInfo.packageName.contains(BrowserUtils.YANDEX)) {
                    return true;
                }
            }
        }

        // For samsung-type action in Yandex browser
        intent.setAction(SAMSUNG_CONTENT_BLOCKER_ACTION);
        list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            for (ResolveInfo info : list) {
                if (info.activityInfo.packageName.contains(BrowserUtils.YANDEX)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void startYandexBrowser(Context context) {
        ComponentName componentName = getYandexBrowser(context, Intent.ACTION_MAIN);

        if (componentName != null) {
            startBrowser(context, componentName);
        }
    }

    public static void startSamsungBrowser(Context context) {
        ComponentName componentName = getSamsungBrowser(context);

        if (componentName != null) {
            startBrowser(context, componentName);
        }
    }

    public static void sendUpdateFiltersInBrowser(Context context, String ourPackageName, String browserPackageName) {
        Intent intent = new Intent();
        intent.setAction("com.samsung.android.sbrowser.contentBlocker.ACTION_UPDATE");
        intent.setData(Uri.parse("package:" + ourPackageName));
        intent.setPackage(browserPackageName);
        context.sendBroadcast(intent);
    }

    private static void centerDialogButton(AlertDialog dialog) {
        // Center the button
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.weight = 1.0f;
        layoutParams.gravity = Gravity.CENTER; //this is layout_gravity
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setLayoutParams(layoutParams);
    }

    private static void startBrowser(Context context, ComponentName component) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(component);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(context, intent);
    }

    // https://github.com/AdguardTeam/ContentBlocker/issues/56
    private static ComponentName getSamsungBrowser(Context context) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> installedPackages = context.getPackageManager().queryIntentActivities(mainIntent, 0);

        ArrayList<ActivityInfo> samsungActivities = new ArrayList<>();
        for (ResolveInfo installedPackage : installedPackages) {
            if (installedPackage.activityInfo.packageName.startsWith(SAMSUNG_BROWSER_PACKAGE)) {
                samsungActivities.add(installedPackage.activityInfo);
            }
        }

        if (CollectionUtils.isNotEmpty(samsungActivities)) {
            Collections.sort(samsungActivities, new Comparator<ActivityInfo>() {
                @Override
                public int compare(ActivityInfo lhs, ActivityInfo rhs) {
                    return lhs.packageName.compareTo(rhs.packageName);
                }
            });

            ActivityInfo activityInfo = samsungActivities.get(0);
            return new ComponentName(activityInfo.packageName, activityInfo.name);

        }

        return null;
    }

    // https://github.com/AdguardTeam/ContentBlocker/issues/53
    private static ComponentName getYandexBrowser(Context context, String action) {
        Intent mainIntent = new Intent();
        mainIntent.setAction(action);

        for (String packageName : yandexBrowserPackageList) {
            mainIntent.setPackage(packageName);

            List<ResolveInfo> installedPackages = context.getPackageManager().queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY);

            if (!installedPackages.isEmpty()) {
                ResolveInfo resolveInfo = installedPackages.get(0);
                return new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            }
        }

        return null;
    }

    private static String getSamsungPackage(Context context) {
        return getBrowserPackage(context, SAMSUNG_CONTENT_BLOCKER_ACTION, Arrays.asList(SAMSUNG_PACKAGE_PREFIX, SAMSUNG));
    }

    private static String getBrowserPackage(Context context, String action, List<String> phraseList) {
        List<String> packageNameList = new ArrayList<>();

        Intent intent = new Intent(action);
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo info : list) {
            for (String phrase : phraseList) {
                if (info.activityInfo.packageName.contains(phrase)) {
                    packageNameList.add(info.activityInfo.packageName);
                    break;
                }
            }
        }

        Collections.sort(packageNameList);
        return packageNameList.isEmpty() ? null : packageNameList.get(0);
    }

    /**
     * Starts activity and shows notification if activity not found
     *
     * @param context context
     * @param intent intent
     */
    private static void startActivity(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // TODO add exception handling
        }
    }
}
