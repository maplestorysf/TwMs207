/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2014 Aaron Weiss
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package provider.pkgnx.internal;

import io.netty.buffer.ByteBuf;
import provider.pkgnx.NXNode;
import tools.data.LittleEndianAccessor;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * A lazy-loaded set of data tables bound to an {@code NXFile}.
 *
 * @author Aaron Weiss
 * @version 1.0.0
 * @since 1/21/14
 */
public class LazyNXTables extends NXTables {

    private Map<Long, AudioBuf> audioBufs;
    private Map<Long, Bitmap> bitmaps;
    private Map<Long, String> strings;
    private final NXHeader header;
    private final LittleEndianAccessor lea;

    public LazyNXTables(NXHeader header, LittleEndianAccessor lea) {
        this.header = header;
        this.lea = lea;
        audioBufs = new HashMap<Long, AudioBuf>();
        bitmaps = new HashMap<Long, Bitmap>();
        strings = new HashMap<Long, String>();
    }

    @Override
    public ByteBuf getAudioBuf(long index, long length) {
        checkIndex(index);
        if (!audioBufs.containsKey(index)) {
            long mark = lea.getPosition();
            try {
                lea.seek(header.getSoundOffset() + index * 8);
                audioBufs.put(index, new AudioBuf(lea));
            } finally {
                lea.seek(mark);
            }
        }
        return audioBufs.get(index).getAudioBuf(length);
    }

    @Override
    public BufferedImage getImage(long index, int width, int height) {
        checkIndex(index);
        if (!bitmaps.containsKey(index)) {
            long mark = lea.getPosition();
            try {
                lea.seek(header.getBitmapOffset() + index * 8);
                bitmaps.put(index, new Bitmap(lea));
            } finally {
                lea.seek(mark);
            }
        }
        return bitmaps.get(index).getImage(width, height);
    }

    @Override
    public String getString(long index) {
        checkIndex(index);
        if (!strings.containsKey(index)) {
            long mark = lea.getPosition();
            try {
                lea.seek(header.getStringOffset() + index * 8);
                lea.seek(lea.readLong());
                strings.put(index, lea.readAsciiString(lea.readUShort()));
            } finally {
                lea.seek(mark);
            }
        }
        return strings.get(index);
    }
}
