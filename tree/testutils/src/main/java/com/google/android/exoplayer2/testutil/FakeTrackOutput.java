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
package com.google.android.exoplayer2.testutil;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.testutil.Dumper.Dumpable;
import com.google.android.exoplayer2.upstream.DataReader;
import com.google.android.exoplayer2.util.Function;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.compatqual.NullableType;

/**
 * A fake {@link TrackOutput}.
 */
public final class FakeTrackOutput implements TrackOutput, Dumper.Dumpable {

  private final ArrayList<DumpableSampleInfo> sampleInfos;
  private final ArrayList<Dumpable> dumpables;

  private byte[] sampleData;
  private int formatCount;

  @Nullable public Format lastFormat;

  public FakeTrackOutput() {
    sampleInfos = new ArrayList<>();
    dumpables = new ArrayList<>();
    sampleData = Util.EMPTY_BYTE_ARRAY;
    formatCount = 0;
  }

  public void clear() {
    sampleInfos.clear();
    dumpables.clear();
    sampleData = Util.EMPTY_BYTE_ARRAY;
    formatCount = 0;
  }

  @Override
  public void format(Format format) {
    addFormat(format);
  }

  @Override
  public int sampleData(DataReader input, int length, boolean allowEndOfInput) throws IOException {
    byte[] newData = new byte[length];
    int bytesAppended = input.read(newData, 0, length);
    if (bytesAppended == C.RESULT_END_OF_INPUT) {
      if (allowEndOfInput) {
        return C.RESULT_END_OF_INPUT;
      }
      throw new EOFException();
    }
    newData = Arrays.copyOf(newData, bytesAppended);
    sampleData = TestUtil.joinByteArrays(sampleData, newData);
    return bytesAppended;
  }

  @Override
  public void sampleData(ParsableByteArray data, int length) {
    byte[] newData = new byte[length];
    data.readBytes(newData, 0, length);
    sampleData = TestUtil.joinByteArrays(sampleData, newData);
  }

  @Override
  public void sampleMetadata(
      long timeUs,
      @C.BufferFlags int flags,
      int size,
      int offset,
      @Nullable CryptoData cryptoData) {
    if (lastFormat == null) {
      throw new IllegalStateException("TrackOutput must receive format before sampleMetadata");
    }
    if (lastFormat.maxInputSize != Format.NO_VALUE && size > lastFormat.maxInputSize) {
      throw new IllegalStateException("Sample size exceeds Format.maxInputSize");
    }
    if (dumpables.isEmpty()) {
      addFormat(lastFormat);
    }
    addSampleInfo(
        timeUs, flags, sampleData.length - offset - size, sampleData.length - offset, cryptoData);
  }

  public void assertSampleCount(int count) {
    assertThat(sampleInfos).hasSize(count);
  }

  public void assertSample(
      int index, byte[] data, long timeUs, int flags, @Nullable CryptoData cryptoData) {
    byte[] actualData = getSampleData(index);
    assertThat(actualData).isEqualTo(data);
    assertThat(getSampleTimeUs(index)).isEqualTo(timeUs);
    assertThat(getSampleFlags(index)).isEqualTo(flags);
    assertThat(getSampleCryptoData(index)).isEqualTo(cryptoData);
  }

  public byte[] getSampleData(int index) {
    return Arrays.copyOfRange(sampleData, getSampleStartOffset(index), getSampleEndOffset(index));
  }

  private byte[] getSampleData(int fromIndex, int toIndex) {
    return Arrays.copyOfRange(sampleData, fromIndex, toIndex);
  }

  public long getSampleTimeUs(int index) {
    return sampleInfos.get(index).timeUs;
  }

  public int getSampleFlags(int index) {
    return sampleInfos.get(index).flags;
  }

  @Nullable
  public CryptoData getSampleCryptoData(int index) {
    return sampleInfos.get(index).cryptoData;
  }

  public int getSampleCount() {
    return sampleInfos.size();
  }

  public List<Long> getSampleTimesUs() {
    List<Long> sampleTimesUs = new ArrayList<>();
    for (DumpableSampleInfo sampleInfo : sampleInfos) {
      sampleTimesUs.add(sampleInfo.timeUs);
    }
    return Collections.unmodifiableList(sampleTimesUs);
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.add("total output bytes", sampleData.length);
    dumper.add("sample count", sampleInfos.size());
    if (dumpables.isEmpty() && lastFormat != null) {
      new DumpableFormat(lastFormat, 0).dump(dumper);
    }
    for (int i = 0; i < dumpables.size(); i++) {
      dumpables.get(i).dump(dumper);
    }
  }

  private int getSampleStartOffset(int index) {
    return sampleInfos.get(index).startOffset;
  }

  private int getSampleEndOffset(int index) {
    return sampleInfos.get(index).endOffset;
  }

  private void addFormat(Format format) {
    lastFormat = format;
    dumpables.add(new DumpableFormat(format, formatCount));
    formatCount++;
  }

  private void addSampleInfo(
      long timeUs, int flags, int startOffset, int endOffset, @Nullable CryptoData cryptoData) {
    DumpableSampleInfo sampleInfo =
        new DumpableSampleInfo(timeUs, flags, startOffset, endOffset, cryptoData, getSampleCount());
    sampleInfos.add(sampleInfo);
    dumpables.add(sampleInfo);
  }

  private final class DumpableSampleInfo implements Dumper.Dumpable {
    public final long timeUs;
    public final int flags;
    public final int startOffset;
    public final int endOffset;
    @Nullable public final CryptoData cryptoData;
    public final int index;

    public DumpableSampleInfo(
        long timeUs,
        int flags,
        int startOffset,
        int endOffset,
        @Nullable CryptoData cryptoData,
        int index) {
      this.timeUs = timeUs;
      this.flags = flags;
      this.startOffset = startOffset;
      this.endOffset = endOffset;
      this.cryptoData = cryptoData;
      this.index = index;
    }

    @Override
    public void dump(Dumper dumper) {
      dumper
          .startBlock("sample " + index)
          .add("time", timeUs)
          .add("flags", flags)
          .add("data", getSampleData(startOffset, endOffset));
      if (cryptoData != null) {
        dumper.add("crypto mode", cryptoData.cryptoMode);
        dumper.add("encryption key", cryptoData.encryptionKey);
      }
      dumper.endBlock();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DumpableSampleInfo that = (DumpableSampleInfo) o;
      return timeUs == that.timeUs
          && flags == that.flags
          && startOffset == that.startOffset
          && endOffset == that.endOffset
          && index == that.index
          && Util.areEqual(cryptoData, that.cryptoData);
    }

    @Override
    public int hashCode() {
      int result = (int) timeUs;
      result = 31 * result + flags;
      result = 31 * result + startOffset;
      result = 31 * result + endOffset;
      result = 31 * result + (cryptoData == null ? 0 : cryptoData.hashCode());
      result = 31 * result + index;
      return result;
    }
  }

  private static final class DumpableFormat implements Dumper.Dumpable {
    private final Format format;
    public final int index;

    private static final Format DEFAULT_FORMAT = new Format.Builder().build();

    public DumpableFormat(Format format, int index) {
      this.format = format;
      this.index = index;
    }

    @Override
    public void dump(Dumper dumper) {
      dumper.startBlock("format " + index);
      addIfNonDefault(dumper, "averageBitrate", format -> format.averageBitrate);
      addIfNonDefault(dumper, "peakBitrate", format -> format.peakBitrate);
      addIfNonDefault(dumper, "id", format -> format.id);
      addIfNonDefault(dumper, "containerMimeType", format -> format.containerMimeType);
      addIfNonDefault(dumper, "sampleMimeType", format -> format.sampleMimeType);
      addIfNonDefault(dumper, "codecs", format -> format.codecs);
      addIfNonDefault(dumper, "maxInputSize", format -> format.maxInputSize);
      addIfNonDefault(dumper, "width", format -> format.width);
      addIfNonDefault(dumper, "height", format -> format.height);
      addIfNonDefault(dumper, "frameRate", format -> format.frameRate);
      addIfNonDefault(dumper, "rotationDegrees", format -> format.rotationDegrees);
      addIfNonDefault(dumper, "pixelWidthHeightRatio", format -> format.pixelWidthHeightRatio);
      addIfNonDefault(dumper, "channelCount", format -> format.channelCount);
      addIfNonDefault(dumper, "sampleRate", format -> format.sampleRate);
      addIfNonDefault(dumper, "pcmEncoding", format -> format.pcmEncoding);
      addIfNonDefault(dumper, "encoderDelay", format -> format.encoderDelay);
      addIfNonDefault(dumper, "encoderPadding", format -> format.encoderPadding);
      addIfNonDefault(dumper, "subsampleOffsetUs", format -> format.subsampleOffsetUs);
      addIfNonDefault(dumper, "selectionFlags", format -> format.selectionFlags);
      addIfNonDefault(dumper, "language", format -> format.language);
      if (format.drmInitData != null) {
        dumper.add("drmInitData", format.drmInitData.hashCode());
      }
      addIfNonDefault(dumper, "metadata", format -> format.metadata);
      if (!format.initializationData.isEmpty()) {
        dumper.startBlock("initializationData");
        for (int i = 0; i < format.initializationData.size(); i++) {
          dumper.add("data", format.initializationData.get(i));
        }
        dumper.endBlock();
      }
      dumper.endBlock();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DumpableFormat that = (DumpableFormat) o;
      return index == that.index && format.equals(that.format);
    }

    @Override
    public int hashCode() {
      int result = format.hashCode();
      result = 31 * result + index;
      return result;
    }

    private void addIfNonDefault(
        Dumper dumper, String field, Function<Format, @NullableType Object> getFieldFunction) {
      @Nullable Object thisValue = getFieldFunction.apply(format);
      @Nullable Object defaultValue = getFieldFunction.apply(DEFAULT_FORMAT);
      if (!Util.areEqual(thisValue, defaultValue)) {
        dumper.add(field, thisValue);
      }
    }
  }
}
