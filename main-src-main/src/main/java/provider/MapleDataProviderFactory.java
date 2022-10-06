/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package provider;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import constants.ServerConfig;
import provider.nx.NXDataProvider;
import provider.wz.WZFile;
import provider.wz.XMLWZFile;
import tools.data.LittleEndianAccessor;
import tools.data.RandomAccessByteStream;

public class MapleDataProviderFactory {

    private final static Map<String, MapleDataProvider> wzFiles = new HashMap<String, MapleDataProvider>();
    private static String wzPath;

    static {
        loadPath();
    }

    public static void loadPath() {
        wzPath = ServerConfig.WZ_PATH;
        if (wzPath == null) {
            wzPath = ServerConfig.WZ_TYPE.name();
        }
    }

    private static MapleDataProvider getDataProvider(File in) {
        return getDataProvider(in, false);
    }

    private static MapleDataProvider getDataProvider(File in, boolean provideImages) {
        if (!in.exists()) {
            throw new RuntimeException("檔案不存在" + in.getPath());
        }

        if (in.isDirectory()) {
            return new XMLWZFile(in);
        } else {
            try (RandomAccessFile raf = new RandomAccessFile(in, "r")) {
                LittleEndianAccessor lea = new LittleEndianAccessor(new RandomAccessByteStream(raf));
                String magic = lea.readAsciiString(4);
                raf.close();
                switch (magic) {
                    case "PKG1":
                        return new WZFile(in, provideImages);
                    case "PKG4":
                        return new NXDataProvider(in.getPath());
                    default:
                        throw new RuntimeException("不支援這個" + magic + "格式檔案" + in.getPath());
                }
            } catch (Exception e) {
                throw new RuntimeException("讀取檔案時出錯", e);
            }
        }
    }

    public static MapleDataProvider getDataProvider(String filename) {
        filename += ServerConfig.WZ_TYPE == ServerConfig.WZ_Type.NX ? ".nx" : ".wz";
        try {
            if (!wzFiles.containsKey(filename)) {
                wzFiles.put(filename, getDataProvider(fileInWZPath(filename)));
            }
            return wzFiles.get(filename);
        } catch (Exception e) {
            System.err.println("讀取WZ/NX檔案時發生錯誤:" + e);
            return null;
        }
    }

    public static File fileInWZPath(String filename) {
        return new File(wzPath, filename);
    }

}
