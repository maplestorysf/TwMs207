/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

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
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

/**
 *
 * @author Weber
 */
public class SkillBuffDecoder {

    SharkReader sharkReader = null;
    File file = null;

    public void start(String filename) {
        // JFileChooser fd = new JFileChooser();
        // fd.showOpenDialog(null);
        // file = fd.getSelectedFile();
        // if (file != null) {
        // if (!file.exists()) {
        // System.out.println("檔案不存在");
        // return;
        // }
        // }
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
        String info = "";
        boolean prefix = false;
        boolean warp = true;
        String scr = "";
        String consume = "";
        switch (send) {
            case LP_TemporaryStatSet: {

                System.out.println("________________________________________________");
                long mask[] = new long[GameConstants.MAX_BUFFSTAT];
                for (int i = 0; i < mask.length; i++) {
                    mask[i] = slea.readUInt();
                    // System.out.printf("MASK[%d] = %X \n", i, mask[i]);
                }

                List<Integer> status = new ArrayList<>();
                for (int i = 0; i < 32 * mask.length; i++) {
                    int position = (int) Math.floor(i / 32);
                    int nValue = 1 << (31 - (i % 32));
                    if ((mask[position] & nValue) > 0) {
                        System.out.println("偵測到BUFFID :" + i);
                    }
                }
                long pos = slea.getPosition();
                slea.seek(0);
                System.out.println(slea.toString());
                slea.seek(pos);
                int value = slea.readShort();
                // 假設2 byte
                boolean is4bytes = false;
                int skillId = slea.readInt();
                Skill skill = SkillFactory.getSkill(skillId);
                if (skill == null) {
                    slea.seek(slea.getPosition() - 2);
                    value = slea.readInt();
                    skillId = slea.readInt();
                    skill = SkillFactory.getSkill(skillId);
                    is4bytes = true;
                    if (skill == null) {
                        slea.seek(slea.getPosition() - 8);
                        slea.skip(5);
                        while (slea.available() > 13) {
                            int count = slea.readInt();
                            for (int i = 0; i < count; i++) {
                                skillId = slea.readInt();
                                skill = SkillFactory.getSkill(skillId);
                                value = slea.readInt();
                                System.out.println(
                                        "偵測到 indie Val : " + value + " 推測技能 :  " + skill.getId() + "(" + skill.getName());
                                slea.skip(16);
                            }
                        }
                        break;
                    }
                }
                if (skill != null) {
                    System.out.println("推測技能 :  " + skill.getId() + "(" + skill.getName() + ") Value : " + value + " "
                            + (is4bytes ? "4 Bytes" : "2 Bytes"));
                }
                break;
            }

        }
    }

    public static void main(String[] args) {
        SkillFactory.load(false);
        new SkillBuffDecoder().start("C:\\Users\\Weber\\Documents\\MS203\\123.msb");

    }

}
