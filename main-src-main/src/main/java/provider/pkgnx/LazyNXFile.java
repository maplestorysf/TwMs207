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
package provider.pkgnx;

import provider.pkgnx.internal.LazyNXTables;
import provider.pkgnx.internal.NXHeader;
import provider.pkgnx.util.NodeParser;
import tools.data.LittleEndianAccessor;
import tools.data.RandomAccessByteStream;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

/**
 * A lazy-loaded memory-mapped file for reading specification-compliant NX
 * files.
 *
 * @author Aaron Weiss
 * @version 1.0.0
 * @since 1/21/14
 */
public class LazyNXFile extends NXFile {

    private final LittleEndianAccessor lea;
    private Map<Integer, NXNode> nodes;

    /**
     * Creates a new {@code EagerNXFile} from the specified {@code path}.
     *
     * @param path the absolute or relative path to the file
     * @throws IOException if something goes wrong in reading the file
     */
    public LazyNXFile(String path) throws IOException {
        super(path);
        lea = new LittleEndianAccessor(new RandomAccessByteStream(new RandomAccessFile(path, "r")));
        header = new NXHeader(this, lea);
        nodes = new HashMap<Integer, NXNode>();
        tables = new LazyNXTables(header, lea);
    }

    @Override
    public NXNode getNode(int index) {
        if (!nodes.containsKey(index)) {
            lea.seek(header.getNodeOffset() + index * NXNode.NODE_SIZE);
            nodes.put(index, NodeParser.parseNode(this, lea));
        }
        return nodes.get(index);
    }
}
