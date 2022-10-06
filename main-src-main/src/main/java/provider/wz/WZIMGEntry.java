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

import java.awt.Point;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import provider.MapleData;
import provider.MapleDataEntity;
import provider.MapleDataType;
import provider.wz.util.WzLittleEndianAccessor;
import tools.data.RandomAccessByteStream;

public class WZIMGEntry implements MapleData {

    private final WZIMGFile file;
    private String name;
    private MapleDataType type;
    private final List<MapleData> children = new ArrayList<>();
    ;
    private Object data;
    private long entryOffset;
    private final String wzFile;
    private final String parent;

    public WZIMGEntry(WZIMGFile file, String wzFile, String parent, long entryOffset) {
        this.file = file;
        this.wzFile = wzFile;
        this.parent = parent;
        this.entryOffset = entryOffset;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MapleDataType getType() {
        return type;
    }

    @Override
    public List<MapleData> getChildren() {
        if (type == MapleDataType.EXTENDED) {
            try (RandomAccessFile raf = new RandomAccessFile(wzFile, "r")) {
                WzLittleEndianAccessor wlea = new WzLittleEndianAccessor(new RandomAccessByteStream(raf));
                parseEntry(wlea, true);
                finish();
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Collections.unmodifiableList(children);
    }

    @Override
    public MapleData getChildByPath(String path) {
        String segments[] = path.split("/");
        if (segments[0].equals("..")) {
            return ((MapleData) getParent()).getChildByPath(path.substring(path.indexOf("/") + 1));
        }
        MapleData ret = this;
        for (String segment : segments) {
            boolean foundChild = false;
            for (MapleData child : ret.getChildren()) {
                if (child.getName().equals(segment)) {
                    ret = child;
                    foundChild = true;
                    break;
                }
            }
            if (!foundChild) {
                return null;
            }
        }
        return ret;
    }

    @Override
    public Object getData() {
        return data;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(MapleDataType type) {
        this.type = type;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void addChild(WZIMGEntry entry) {
        children.add(entry);
    }

    @Override
    public Iterator<MapleData> iterator() {
        return getChildren().iterator();
    }

    @Override
    public String toString() {
        return getName() + ":" + getData();
    }

    @Override
    public MapleDataEntity getParent() {
        if (parent.equals(file.getRoot().getName())) {
            return file.getRoot();
        } else if (!parent.isEmpty()) {
            return file.getRoot().getChildByPath(parent);
        } else {
            return file.getParent();
        }
    }

    private void parseEntry(WzLittleEndianAccessor wlea, boolean getChild) {
        wlea.seek(entryOffset);
        String type = wlea.readStringBlock(file.getOffset());
        switch (type) {
            case "Property": {
                if (!getChild) {
                    return;
                }
                setType(MapleDataType.PROPERTY);
                // Unknown, seems to be some identifier for PCOM.dll to read as a BMS config
                // file (ascii), whenever not 0?
                wlea.readByte();
                // Unknown, seems to be unused
                wlea.readByte();
                // Amount of variants
                int children = wlea.readCompressedInt();
                for (int i = 0; i < children; i++) {
                    WZIMGEntry cEntry = new WZIMGEntry(file, wzFile, parent + (parent.isEmpty() ? "" : "/") + name, wlea.getPosition());
                    cEntry.parseData(wlea);
                    addChild(cEntry);
                }
                break;
            }
            case "Canvas": {
                if (!getChild) {
                    return;
                }
                setType(MapleDataType.CANVAS);
                wlea.readByte();
                byte marker = wlea.readByte();
                switch (marker) { // do nothing
                    case 0:
                        break;
                    case 1:
                        wlea.readByte();
                        wlea.readByte();
                        int children = wlea.readCompressedInt();
                        for (int i = 0; i < children; i++) {
                            WZIMGEntry child = new WZIMGEntry(file, wzFile, parent + (parent.isEmpty() ? "" : "/") + name, wlea.getPosition());
                            child.parseData(wlea);
                            addChild(child);
                        }
                        break;
                    default:
                        System.out.println("Canvas marker != 1 (" + marker + ")");
                        break;
                }
                int width = wlea.readCompressedInt();
                int height = wlea.readCompressedInt();
                int format = wlea.readCompressedInt();
                int format2 = wlea.readByte();
                wlea.readInt();
                int dataLength = wlea.readInt() - 1;
                wlea.readByte();
                boolean provideImages = false;
                if (provideImages) {
                    byte[] pngdata = wlea.read(dataLength);
                    setData(new PNGMapleCanvas(width, height, dataLength, format + format2, pngdata));
                } else {
                    setData(new PNGMapleCanvas(width, height, dataLength, format + format2, null));
                    wlea.skip(dataLength);
                }
                break;
            }
            case "Shape2D#Vector2D": {
                setType(MapleDataType.VECTOR);
                int x = wlea.readCompressedInt();
                int y = wlea.readCompressedInt();
                setData(new Point(x, y));
                break;
            }
            case "Shape2D#Convex2D": {
                if (!getChild) {
                    return;
                }
                int children = wlea.readCompressedInt();
                for (int i = 0; i < children; i++) {
                    WZIMGEntry cEntry = new WZIMGEntry(file, wzFile, parent + (parent.isEmpty() ? "" : "/") + name, wlea.getPosition());
                    cEntry.parseEntry(wlea, true);
                    addChild(cEntry);
                }
                break;
            }
            case "Sound_DX8": {
                setType(MapleDataType.SOUND);
                wlea.readByte();
                int dataLength = wlea.readCompressedInt();
                wlea.readCompressedInt(); // no clue what this is
                int offset = (int) wlea.getPosition();
                setData(new ImgMapleSound(dataLength, offset - file.getOffset()));
                //wlea.seek(endOfExtendedBlock);
                break;
            }
            case "UOL": {
                setType(MapleDataType.UOL);
                wlea.readByte();
                setData(wlea.readStringBlock(file.getOffset()));
                break;
            }
            default: {
                throw new RuntimeException("Unhandeled extended type: " + type);
            }
        }
    }

    private void parseData(WzLittleEndianAccessor wlea) {
        // Variant name (node name)
        setName(wlea.readStringBlock(file.getOffset()));
        byte type = wlea.readByte();
        switch (type) {
            case 0:
                setType(MapleDataType.IMG_0x00);
                break;
            case 2:
            case 11: // ??? no idea, since 0.49
                setType(MapleDataType.SHORT);
                setData(wlea.readShort());
                break;
            case 3:
            case 19:
                setType(MapleDataType.INT);
                setData(wlea.readCompressedInt());
                break;
            case 20:
                setType(MapleDataType.LONG);
                setData(wlea.readLongValue());
                break;
            case 4:
                setType(MapleDataType.FLOAT);
                setData(wlea.readFloatValue());
                break;
            case 5:
                setType(MapleDataType.DOUBLE);
                setData(wlea.readDouble());
                break;
            case 8:
                setType(MapleDataType.STRING);
                setData(wlea.readStringBlock(file.getOffset()));
                break;
            case 9:
                setType(MapleDataType.EXTENDED);
                long endOfExtendedBlock = wlea.readInt();
                endOfExtendedBlock += wlea.getPosition();
                entryOffset = wlea.getPosition();
                parseEntry(wlea, false);
                wlea.seek(endOfExtendedBlock);
                break;
            default:
                System.out.println("Unknown Image type " + type);
        }
    }

    private void finish() {
        ((ArrayList<MapleData>) children).trimToSize();
    }
}
