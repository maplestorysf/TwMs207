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
package handling.netty;

import client.MapleClient;
import constants.ServerConfig;
import constants.ServerConstants;
import handling.SendPacketOpcode;
import java.util.concurrent.locks.Lock;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import server.shark.SharkPacket;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.MapleAESOFB;
import tools.MapleCustomEncryption;
import tools.StringUtil;

public class MaplePacketEncoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext chc, Object message, ByteBuf buffer) throws Exception {
        final MapleClient client = (MapleClient) chc.channel().attr(MapleClient.CLIENT_KEY).get();

        if (client != null) {
            byte[] input = ((byte[]) message);

            // 封包記錄輸出
            if (ServerConfig.LOG_SHARK) {
                final SharkPacket sp = new SharkPacket(input, false);
                client.sl.log(sp);
            }
            int pHeader = ((input[0]) & 0xFF) + (((input[1]) & 0xFF) << 8);
            String op = SendPacketOpcode.nameOf(pHeader);
            if (ServerConfig.LOG_PACKETS && !SendPacketOpcode.isSpamHeader(SendPacketOpcode.valueOf(op))) {
                String pHeaderStr = Integer.toHexString(pHeader).toUpperCase();
                pHeaderStr = "0x" + StringUtil.getLeftPaddedStr(pHeaderStr, '0', 4);
                String tab = "";
                for (int i = 4; i > op.length() / 8; i--) {
                    tab += "\t";
                }
                int packetLen = input.length;
                String t = packetLen >= 10 ? packetLen >= 100 ? packetLen >= 1000 ? "" : " " : "  " : "   ";
                final StringBuilder sb = new StringBuilder("[LP]\t" + op + tab + "\t包頭:" + pHeaderStr + t + "["
                        + packetLen/* + "\r\nCaller: " + Thread.currentThread().getStackTrace()[2] */ + "字元]");
                System.out.println(sb.toString());
                sb.append("\r\n\r\n").append(HexTool.toString(input)).append("\r\n").append(HexTool.toStringFromAscii(input));
                FileoutputUtil.log(FileoutputUtil.Packet_Log, "\r\n\r\n" + sb.toString() + "\r\n\r\n");
            }

            // 封包數據進行加密處理
            final byte[] unencrypted = new byte[input.length];
            System.arraycopy(input, 0, unencrypted, 0, input.length); // Copy the input > "unencrypted"
            final byte[] ret = new byte[unencrypted.length + 4]; // Create new bytes with length = "unencrypted" + 4

            final Lock mutex = client.getLock();
            mutex.lock();
            try {
                final byte[] header = client.getSendCrypto().getPacketHeader(unencrypted.length);
                if (ServerConstants.MAPLE_TYPE == ServerConstants.MapleType.GLOBAL) {
                    MapleCustomEncryption.encryptData(unencrypted); // Encrypting Data
                }
                if (client.getPlayer() != null) {
                    // v200+ 的新版本cipher
                    client.getSendCrypto().encryptEx(unencrypted);
                } else {
                    client.getSendCrypto().crypt(unencrypted); // Crypt it with IV
                }
                System.arraycopy(header, 0, ret, 0, 4); // Copy the header > "Ret", first 4 bytes
                System.arraycopy(unencrypted, 0, ret, 4, unencrypted.length); // Copy the unencrypted > "ret"
                buffer.writeBytes(ret);
            } finally {
                mutex.unlock();
            }
        } else { // no client object created yet, send unencrypted (hello)
            buffer.writeBytes((byte[]) message);
        }
    }
}
