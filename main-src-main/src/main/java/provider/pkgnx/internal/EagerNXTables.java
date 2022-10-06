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
import tools.data.LittleEndianAccessor;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

/**
 * An eager-loaded set of data tables bound to an {@code NXFile}.
 *
 * @author Aaron Weiss
 * @version 2.0.0
 * @since 6/26/13
 */
public class EagerNXTables extends NXTables {

    private final List<AudioBuf> audioBufs;
    private final List<Bitmap> bitmaps;
    private final List<String> strings;

    /**
     * Creates a set of {@code EagerNXTables}.
     *
     * @param header the header of the {@code NXFile}.
     * @param lea the accessor to read from
     */
    public EagerNXTables(NXHeader header, LittleEndianAccessor lea) {
        lea.seek(header.getSoundOffset());
        audioBufs = new LinkedList<AudioBuf>();
        for (int i = 0; i < header.getSoundCount(); i++) {
            audioBufs.add(new AudioBuf(lea));
        }

        lea.seek(header.getBitmapOffset());
        bitmaps = new LinkedList<Bitmap>();
        for (int i = 0; i < header.getBitmapCount(); i++) {
            bitmaps.add(new Bitmap(lea));
        }

        lea.seek(header.getStringOffset());
        strings = new LinkedList<String>();
        for (int i = 0; i < header.getStringCount(); i++) {
            long offset = lea.readLong();
            long mark = lea.getPosition();
            lea.seek(offset);
            strings.add(lea.readAsciiString(lea.readUShort()));
            lea.seek(mark);
        }
    }

    @Override
    public ByteBuf getAudioBuf(long index, long length) {
        checkIndex(index);
        return audioBufs.get((int) index).getAudioBuf(length);
    }

    @Override
    public BufferedImage getImage(long index, int width, int height) {
        checkIndex(index);
        return bitmaps.get((int) index).getImage(width, height);
    }

    @Override
    public String getString(long index) {
        checkIndex(index);
        return strings.get((int) index);
    }
}
