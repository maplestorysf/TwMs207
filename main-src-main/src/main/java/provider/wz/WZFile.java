/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package provider.wz;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.wz.util.WzLittleEndianAccessor;
import tools.data.RandomAccessByteStream;

public class WZFile implements MapleDataProvider {

    private final File wzfile;
    public WzHeader Header;
    public String Name = "";
    private short version = 0;
    private int versionHash = 0;
    private short fileVersion = 0;
    public byte[] WzIv;
    private final WZDirectoryEntry root;
    private final boolean provideImages;
    private long cOffset;

    private final Map<String, WZIMGFile> images = new HashMap<String, WZIMGFile>();

    public WZFile(File wzfile, boolean provideImages) throws IOException {
        Header = WzHeader.GetDefault();
        this.wzfile = wzfile;
        root = new WZDirectoryEntry(wzfile.getName().split("\\.")[0], 0, 0, 0, null);
        this.provideImages = provideImages;
        ParseMainWzDirectory();
    }

    private int GetVersionHash(int encver, int realver) {
        int EncryptedVersionNumber = encver;
        int VersionNumber = realver;
        int VersionHash = 0;
        int DecryptedVersionNumber = 0;
        String VersionNumberStr;
        int a = 0, b = 0, c = 0, d = 0, l = 0;

        VersionNumberStr = String.valueOf(VersionNumber);

        l = VersionNumberStr.length();
        for (int i = 0; i < l; i++) {
            VersionHash = (32 * VersionHash) + Integer.parseInt(String.valueOf(VersionNumberStr.charAt(i))) + 1;
        }
        a = (VersionHash >> 24) & 0xFF;
        b = (VersionHash >> 16) & 0xFF;
        c = (VersionHash >> 8) & 0xFF;
        d = VersionHash & 0xFF;
        DecryptedVersionNumber = (0xff ^ a ^ b ^ c ^ d);

        if (EncryptedVersionNumber == DecryptedVersionNumber) {
            return VersionHash & 0x7FFFFFFF;
        } else {
            return 0;
        }
    }

    private void ParseMainWzDirectory() throws IOException {
        if (!wzfile.exists()) {
            throw new RuntimeException("讀取檔案時出錯:檔案不存在");
        }

        RandomAccessFile raf = new RandomAccessFile(wzfile, "r");
        WzLittleEndianAccessor wlea = new WzLittleEndianAccessor(new RandomAccessByteStream(raf));
        this.Header = new WzHeader();
        this.Header.Ident = wlea.readAsciiString(4);
        this.Header.FSize = wlea.readLong();
        this.Header.FStart = wlea.readInt();
        this.Header.Copyright = wlea.readNullTerminatedAsciiString();
        wlea.skip((int) (Header.FStart - wlea.getPosition()));
        wlea.Header = this.Header;
        this.version = wlea.readShort();
        this.versionHash = GetVersionHash(version, fileVersion);
        wlea.Hash = this.versionHash;
        // Root directory
        parseDirectory(root, wlea);

        cOffset = wlea.getPosition();
        getOffsets(root);

        raf.close();
    }

    private void getOffsets(MapleDataDirectoryEntry dir) {
        for (MapleDataFileEntry file : dir.getFiles()) {
            file.setOffset(cOffset);
            cOffset += file.getSize();
        }
        for (MapleDataDirectoryEntry sdir : dir.getSubdirectories()) {
            getOffsets(sdir);
        }
    }

    private void parseDirectory(WZDirectoryEntry dir, WzLittleEndianAccessor wlea) {
        // Amount of entries
        int entryCount = wlea.readCompressedInt();
        for (int i = 0; i < entryCount; i++) {

            // Get Type
            // Type (if & 1 then its a Directory, else its an Object)
            long pos = wlea.getPosition();
            byte type = wlea.readByte();
            wlea.seek(pos);

            String fname = wlea.readStringBlock(Header.FStart + 1);
            int fsize = wlea.readCompressedInt();
            int checksum = wlea.readCompressedInt();
            long offset = wlea.readOffset();

            if (type == 3) {
                dir.addDirectory(new WZDirectoryEntry(fname, fsize, checksum, offset, dir));
            } else {
                dir.addFile(new WZFileEntry(fname, fsize, checksum, offset, dir));
            }
        }

        for (MapleDataDirectoryEntry idir : dir.getSubdirectories()) {
            parseDirectory((WZDirectoryEntry) idir, wlea);
        }
    }

    public WZIMGFile getImgFile(String path) throws IOException {
        if (images.containsKey(path)) {
            return images.get(path);
        }
        String segments[] = path.split("/");
        WZDirectoryEntry dir = root;
        for (int x = 0; x < segments.length - 1; x++) {
            dir = (WZDirectoryEntry) dir.getEntry(segments[x]);
            if (dir == null) {
                return null;
            }
        }
        WZFileEntry entry = (WZFileEntry) dir.getEntry(segments[segments.length - 1]);
        if (entry == null) {
            return null;
        }
        WZIMGFile image = new WZIMGFile(wzfile.getPath(), entry, provideImages);
        images.put(path, image);
        return image;
    }

    @Override
    public synchronized MapleData getData(String path) {
        try {
            WZIMGFile imgFile = getImgFile(path);
            if (imgFile == null) {
                return null;
            }
            MapleData ret = imgFile.getRoot();
            return ret;
        } catch (IOException e) {
        }
        return null;
    }

    @Override
    public MapleDataDirectoryEntry getRoot() {
        return root;
    }

    public File getFile() {
        return wzfile;
    }
}
