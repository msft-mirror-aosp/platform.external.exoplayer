/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.google.android.exoplayer2.extractor.ts;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.exoplayer2.testutil.ExtractorAsserts;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit test for {@link AdtsExtractor}. */
@RunWith(AndroidJUnit4.class)
public final class AdtsExtractorTest {

  @Test
  public void sample() throws Exception {
    ExtractorAsserts.assertBehavior(AdtsExtractor::new, "ts/sample.adts");
  }

  @Test
  public void sample_with_id3() throws Exception {
    ExtractorAsserts.assertBehavior(AdtsExtractor::new, "ts/sample_with_id3.adts");
  }

  @Test
  public void sample_withSeeking() throws Exception {
    ExtractorAsserts.assertBehavior(
        () -> new AdtsExtractor(/* flags= */ AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING),
        "ts/sample_cbs.adts");
  }

  // https://github.com/google/ExoPlayer/issues/6700
  @Test
  public void sample_withSeekingAndTruncatedFile() throws Exception {
    ExtractorAsserts.assertBehavior(
        () -> new AdtsExtractor(/* flags= */ AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING),
        "ts/sample_cbs_truncated.adts");
  }
}
