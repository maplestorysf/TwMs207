/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import client.skill.Skill;
import client.skill.SkillFactory;
import constants.GameConstants;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import server.shark.SharkPacket;
import server.shark.SharkReader;
import test.SkillBuffDecoder;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

/**
 *
 * @author Weber
 */
public class DumpCashShopModItems {
    SharkReader sharkReader = null;
    File file = null;

    public void start(String filename) {
        file = new File(filename);
        sharkReader = new SharkReader(file);
        while (sharkReader.available() > 0) {
            readPacket(sharkReader.read());
        }
    }

    private void readPacket(SharkPacket packet) {
        final LittleEndianAccessor slea = new LittleEndianAccessor(new ByteArrayByteStream(packet.info));
        if (packet.outbound) {
            for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
                if (recv.getValue() == packet.opcode) {
                    try {
                        handleRecv(slea, recv);
                    } catch (Exception ex) {
                        System.err.println("解客戶端包錯誤, 包頭" + recv.name() + "\r\n" + slea.toString(true));
                    }
                    break;
                }
            }
        } else {
            for (final SendPacketOpcode send : SendPacketOpcode.values()) {
                if (send.getValue() == packet.opcode) {
                    try {
                        handleSend(slea, send);
                    } catch (Exception ex) {

                        // System.err.println("解伺服器包錯誤, 包頭" + send.name() + "\r\n" +
                        // slea.toString(true));
                        // ex.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private void handleRecv(LittleEndianAccessor slea, RecvPacketOpcode recv) {
        String info = "";
        boolean prefix = false;
        boolean warp = true;
        String scr = "";
        String consume = "";
        switch (recv) {

        }
    }

    private void handleSend(LittleEndianAccessor slea, SendPacketOpcode send) {
        if (send == SendPacketOpcode.LP_SetCashShopInfo) {
            int limitTimeItemsCount = slea.readInt(); // 現實販售
            for (int i = 0; i < limitTimeItemsCount; i++) {
                System.out.println("限時販售: " + slea.readInt());
            }
        }
    }

    public static void main(String[] args) {
        new DumpCashShopModItems().start("C:\\Users\\Weber\\Documents\\MS199\\MS202_CS.msb");
    }
}
