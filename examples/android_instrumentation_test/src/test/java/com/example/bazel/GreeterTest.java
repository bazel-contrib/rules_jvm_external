/*
 * Copyright 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bazel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class GreeterTest {

  public static final String STRING_TO_BE_TYPED = "Hello Bazel! \ud83D\uDC4B\uD83C\uDF31";

  /**
   * Use {@link ActivityScenarioRule} to create and launch the activity under test, and close it
   * after test completes. This is a replacement for {@link androidx.test.rule.ActivityTestRule}.
   */
  @Rule public ActivityScenarioRule<MainActivity> activityScenarioRule
      = new ActivityScenarioRule<>(MainActivity.class);

  @Test
  public void clickButton_shouldShowGreeting() {
    // Type text and then press the button.
    onView(withId(R.id.clickMeButton)).perform(click());

    // Check that the text was changed.
    onView(withId(R.id.helloBazelTextView)).check(matches(withText(STRING_TO_BE_TYPED)));
  }
}