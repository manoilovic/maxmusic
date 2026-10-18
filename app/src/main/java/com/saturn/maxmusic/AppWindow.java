package com.saturn.maxmusic;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

final class AppWindow {
    private AppWindow() {}

    static void configure(Activity activity, View root) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        activity.getWindow().setNavigationBarDividerColor(Color.TRANSPARENT);
        activity.getWindow().setStatusBarContrastEnforced(false);
        activity.getWindow().setNavigationBarContrastEnforced(false);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            // WebView receives the remaining area, never the same insets a second time.
            return WindowInsetsCompat.CONSUMED;
        });
        applyTheme(activity, root, false);
    }

    static void applyTheme(Activity activity, View root, boolean light) {
        root.setBackgroundColor(Color.parseColor(light ? "#F4F5F8" : "#0B0D11"));
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(light);
        controller.setAppearanceLightNavigationBars(light);
    }
}
