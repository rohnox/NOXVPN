package app.openconnect;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.infradead.libopenconnect.LibOpenConnect;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AuthFormPromptTest {

    @Test
    public void passwordPromptOffersAutomaticLoginFlow() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context targetContext = instrumentation.getTargetContext();
        Intent intent = new Intent(targetContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = instrumentation.startActivitySync(intent);

        SharedPreferences preferences = targetContext.getSharedPreferences(
                "auth-form-prompt-test", Context.MODE_PRIVATE);
        preferences.edit()
                .clear()
                .putString("profile_name", "Test VPN")
                .putString("batch_mode", "empty_only")
                .commit();

        LibOpenConnect.AuthForm form = new LibOpenConnect.AuthForm();
        LibOpenConnect.FormOpt password = new LibOpenConnect.FormOpt();
        password.type = LibOpenConnect.OC_FORM_OPT_PASSWORD;
        password.name = "password";
        password.label = "Password";
        form.opts.add(password);

        AuthFormHandler handler = new AuthFormHandler(preferences, form, false, "");
        instrumentation.runOnMainSync(() -> handler.onStart(activity));

        onView(withText(R.string.save_password)).check(matches(isDisplayed()));
        onView(withText(R.string.automatic_login_after_save))
                .check(matches(isDisplayed()))
                .check(matches(isChecked()));
        onView(withText(R.string.automatic_login_hint_title)).check(matches(isDisplayed()));
        onView(withText(R.string.cancel)).perform(click());

        instrumentation.runOnMainSync(activity::finish);
    }
}
