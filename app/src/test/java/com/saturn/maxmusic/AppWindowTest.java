package com.saturn.maxmusic;

import static org.junit.Assert.*;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.FrameLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={29,35})
public class AppWindowTest {
    @Test public void systemBarsAreAppliedOnceAndKeyboardCanResizeContent() {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            Activity host = activity.get();
            FrameLayout root = new FrameLayout(host);
            AppWindow.configure(host, root);
            host.setContentView(root);
            WindowInsetsCompat bars = new WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0,24,0,48)).build();
            WindowInsetsCompat remaining = ViewCompat.dispatchApplyWindowInsets(root, bars);
            assertEquals(24, root.getPaddingTop());
            assertEquals(48, root.getPaddingBottom());
            assertEquals(Insets.NONE, remaining.getInsets(WindowInsetsCompat.Type.systemBars()));
            ViewCompat.dispatchApplyWindowInsets(root, bars);
            assertEquals(48, root.getPaddingBottom());
            // Before API 30 the platform has no separate IME type. Match its actual
            // system-window/stable inset representation instead of a synthetic type map.
            WindowInsetsCompat keyboard = android.os.Build.VERSION.SDK_INT < 30
                    ? new WindowInsetsCompat.Builder()
                            .setSystemWindowInsets(Insets.of(0,24,0,300))
                            .setStableInsets(Insets.of(0,24,0,48)).build()
                    : new WindowInsetsCompat.Builder(bars)
                            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0,0,0,300)).build();
            ViewCompat.dispatchApplyWindowInsets(root, keyboard);
            assertEquals(300, root.getPaddingBottom());
            ViewCompat.dispatchApplyWindowInsets(root, bars);
            assertEquals(48, root.getPaddingBottom());
        }
    }

    @Test public void lightThemePaintsSafeAreaAndUsesDarkSystemIcons() {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            Activity host = activity.get();
            FrameLayout root = new FrameLayout(host);
            AppWindow.configure(host, root);
            host.setContentView(root);
            AppWindow.applyTheme(host, root, true);
            assertEquals(Color.parseColor("#F4F5F8"), ((ColorDrawable)root.getBackground()).getColor());
            assertTrue(WindowCompat.getInsetsController(host.getWindow(),host.getWindow().getDecorView()).isAppearanceLightStatusBars());
            assertTrue(WindowCompat.getInsetsController(host.getWindow(),host.getWindow().getDecorView()).isAppearanceLightNavigationBars());
            assertFalse(host.getWindow().isNavigationBarContrastEnforced());
            AppWindow.applyTheme(host, root, false);
            assertEquals(Color.parseColor("#0B0D11"), ((ColorDrawable)root.getBackground()).getColor());
            assertFalse(WindowCompat.getInsetsController(host.getWindow(),host.getWindow().getDecorView()).isAppearanceLightStatusBars());
        }
    }
}
