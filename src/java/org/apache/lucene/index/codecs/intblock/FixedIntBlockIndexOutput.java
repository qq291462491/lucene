package org.apache.lucene.index.codecs.intblock;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Naive int block API that writes vInts.  This is
 *  expected to give poor performance; it's really only for
 *  testing the pluggability.  One should typically use pfor instead. */

import java.io.IOException;

import org.apache.lucene.index.codecs.sep.IntIndexOutput;
import org.apache.lucene.store.IndexOutput;

public abstract class FixedIntBlockIndexOutput extends IntIndexOutput {

  private IndexOutput out;
  private int blockSize;
  private int[] pending;
  private int upto;
  private long lastSavedFilePointer;
  private int lastSavedUpto;

  protected void init(IndexOutput out, int fixedBlockSize) throws IOException {
    blockSize = fixedBlockSize;
    out.writeVInt(blockSize);
    this.out = out;
    pending = new int[blockSize];
  }

  protected abstract void flushBlock(int[] buffer, IndexOutput out) throws IOException;

  public Index index() throws IOException {
    return new Index();
  }

  public String descFilePointer() {
    return out.getFilePointer() + ":" + upto;
  }

  private class Index extends IntIndexOutput.Index {
    long fp;
    int upto;
    long lastFP;
    int lastUpto;

    public void mark() throws IOException {
      fp = out.getFilePointer();
      upto = FixedIntBlockIndexOutput.this.upto;
    }

    public void set(IntIndexOutput.Index other) throws IOException {
      Index idx = (Index) other;
      lastFP = fp = idx.fp;
      lastUpto = upto = idx.upto;
    }

    public void write(IndexOutput indexOut, boolean absolute) throws IOException {
      if (absolute) {
        indexOut.writeVLong(fp);
        indexOut.writeVInt(upto);
      } else if (fp == lastFP) {
        // same block
        indexOut.writeVLong(0);
        assert upto >= lastUpto;
        indexOut.writeVLong(upto - lastUpto);
      } else {      
        // new block
        indexOut.writeVLong(fp - lastFP);
        indexOut.writeVLong(upto);
      }
      lastUpto = upto;
      lastFP = fp;
    }
  }

  public void write(int v) throws IOException {
    pending[upto++] = v;
    if (upto == blockSize) {
      flushBlock(pending, out);
      upto = 0;
    }
  }

  public void close() throws IOException {
    // NOTE: entries in the block after current upto are
    // invalid
    // nocommit -- zero fill?
    try {
      flushBlock(pending, out);
    } finally {
      out.close();
    }
  }
}
