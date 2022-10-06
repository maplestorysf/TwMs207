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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import provider.MapleDataEntity;
import provider.MapleDataType;
import provider.wz.util.WzLittleEndianAccessor;

public class WZIMGFile {

    private final WZFileEntry file;
    private final WZIMGEntry root;
    private final boolean provideImages;

    @SuppressWarnings("unused")

    public WZIMGFile(String path, WZFileEntry file, boolean provideImages) {
        this.file = file;
        this.provideImages = provideImages;
        root = new WZIMGEntry(this, path, "", file.getOffset());
        root.setName(file.getName());
        root.setType(MapleDataType.EXTENDED);
    }

    public MapleDataEntity getParent() {
        return file.getParent();
    }

    public long getOffset() {
        return file.getOffset();
    }

    protected void dumpImg(OutputStream out, WzLittleEndianAccessor wlea) throws IOException {
        DataOutputStream os = new DataOutputStream(out);
        long oldPos = wlea.getPosition();
        wlea.seek(file.getOffset());
        for (int x = 0; x < file.getSize(); x++) {
            os.write(wlea.readByte());
        }
        wlea.seek(oldPos);
    }

    public WZIMGEntry getRoot() {
        return root;
    }
}
