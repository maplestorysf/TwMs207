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
import handling.RecvPacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import java.util.List;
import server.shark.SharkPacket;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.MapleAESOFB;
import tools.MapleCustomEncryption;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

public class MaplePacketDecoder extends ByteToMessageDecoder {

    public static class DecoderState {

        public int packetlength = -1;
    }

    public static final AttributeKey<DecoderState> DECODER_STATE_KEY = AttributeKey.valueOf(MaplePacketDecoder.class.getName() + ".STATE");

    @Override
    protected void decode(ChannelHandlerContext chc, ByteBuf in, List<Object> message) throws Exception {
        final MapleClient client = (MapleClient) chc.channel().attr(MapleClient.CLIENT_KEY).get();
        final DecoderState decoderState = (DecoderState) chc.channel().attr(DECODER_STATE_KEY).get();

        // 根據封包頭部4個Bytes獲取封包長度
        if (decoderState != null && decoderState.packetlength == -1) {
            if (in.readableBytes() >= 4) {
                final int packetHeader = in.readInt();
                if (!client.getReceiveCrypto().checkPacket(packetHeader)) {
                    chc.channel().close();
                    return;
                }
                decoderState.packetlength = MapleAESOFB.getPacketLength(packetHeader);
            } else {
                return;
            }
        }
        // 截取封包内容並進行解密操作
        if (decoderState != null && in.readableBytes() >= decoderState.packetlength) {
            final byte decryptedPacket[] = new byte[decoderState.packetlength];
            in.readBytes(decryptedPacket);
            decoderState.packetlength = -1;

            client.getReceiveCrypto().crypt(decryptedPacket);

            if (ServerConstants.MAPLE_TYPE == ServerConstants.MapleType.GLOBAL) {
                MapleCustomEncryption.decryptData(decryptedPacket);
            }

            message.add(decryptedPacket);

            // 封包記錄輸出
            if (ServerConfig.LOG_SHARK) {
                final SharkPacket sp = new SharkPacket(decryptedPacket, true);
                client.sl.log(sp);
            }
            short pHeader = new LittleEndianAccessor(new ByteArrayByteStream(decryptedPacket)).readShort();
            short rHeader = pHeader;
            if (client.getOpecodeCrypto() != null && pHeader >= RecvPacketOpcode.CP_BEGIN_USER.getValue()) {
                rHeader = (short) client.getOpecodeCrypto().mapOpcode(pHeader);
            }

            String op = RecvPacketOpcode.nameOf(rHeader);
            if (ServerConfig.LOG_PACKETS && !RecvPacketOpcode.isSpamHeader(RecvPacketOpcode.valueOf(op))) {
                String tab = "";
                for (int i = 4; i > op.length() / 8; i--) {
                    tab += "\t";
                }
                int packetLen = decryptedPacket.length;
                String t = packetLen >= 10 ? packetLen >= 100 ? packetLen >= 1000 ? "" : " " : "  " : "   ";
                final StringBuilder sb = new StringBuilder("[CP]\t" + op + tab + "\t包頭:" + HexTool.getOpcodeToString(rHeader) + (rHeader == pHeader ? "" : (" (" + HexTool.getOpcodeToString(pHeader) + ")")) + t + "[" + packetLen + "字元]");
                System.out.println(sb.toString());
                sb.append("\r\n\r\n").append(HexTool.toString(decryptedPacket)).append("\r\n").append(HexTool.toStringFromAscii(decryptedPacket));
                FileoutputUtil.log(FileoutputUtil.Packet_Log, "\r\n\r\n" + sb.toString() + "\r\n\r\n");
            }
        }
    }
}
