package org.apache.lucene.index.codecs.sep;

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

import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.codecs.Codec;

import java.io.IOException;

/** Writes ints directly to the file (not in blocks) as
 *  vInt */

public class SingleIntIndexOutput extends IntIndexOutput {
  private final IndexOutput out;
  final static String CODEC = "SINGLE_INTS";
  final static int VERSION_START = 0;
  final static int VERSION_CURRENT = VERSION_START;
  private long markPosition;
  private long lastSavePosition;

  public SingleIntIndexOutput(Directory dir, String fileName) throws IOException {
    out = dir.createOutput(fileName);
    Codec.writeHeader(out, CODEC, VERSION_CURRENT);
  }

  /** Write an int to the primary file */
  public void write(int v) throws IOException {
    out.writeVInt(v);
  }

  public Index index() {
    return new Index();
  }

  public void close() throws IOException {
    out.close();
  }

  public String descFilePointer() {
    return Long.toString(out.getFilePointer());
  }

  private class Index extends IntIndexOutput.Index {
    long fp;
    long lastFP;
    public void mark() {
      fp = out.getFilePointer();
      if (Codec.DEBUG) {
        System.out.println("siio.idx.mark id=" + desc + " fp=" + fp);
      }
    }
    public void set(IntIndexOutput.Index other) {
      lastFP = fp = ((Index) other).fp;
    }
    public void write(IndexOutput indexOut, boolean absolute)
      throws IOException {
      if (Codec.DEBUG) {
        System.out.println("siio.idx.write id=" + desc + " fp=" + fp + " abs=" + absolute + " delta=" + (fp-lastFP));
      }
      if (absolute) {
        indexOut.writeVLong(fp);
      } else {
        indexOut.writeVLong(fp - lastFP);
      }
      lastFP = fp;
    }
    public String toString() {
      return Long.toString(fp);
    }
  }
}
