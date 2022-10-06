/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package constants;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import server.Randomizer;
import server.life.MapleMonster;
import server.maps.MapleMap;

/**
 *
 * @author Pungin
 */
public class FeaturesConfig {

    // 設定註冊帳號檢查的方法 [0 = 關閉檢查][1 = IP檢查][2 = MAC檢查]
    public static int accounts_checkIPorMAC = 2;

    // 設定一個IP或MAC最大建立帳號數
    public static int ACCOUNTS_PER_IPorMAC = 3;

    // 默認道具欄位
    public static byte defaultInventorySlot = 24;

    // 默認最大角色欄
    public static boolean defaultFullCharslot = true;

    // 同一個生怪點生怪次數
    public static int monsterSpawn = 1;

    // 星力強化損毀保持星階
    public static boolean saveSFLevel = false;

    // 默認滿技能等級上限
    public static boolean maxSkillLevel = false;

    // 尊貴裝允許出尊貴潛能
    public static boolean superiorPotential = false;

    // 職業隊伍經驗獎勵
    public static boolean classBonusEXP = false;

    // 突破連續升級限制
    public static boolean levelUpLimitBreak = false;

    // 楓幣上限
    public static long MESO_MAX = 9999999999L;

    // 倉庫楓幣上限
    public static long STORAGE_MESO_MAX = 9999999999L;

    // 寵物默認技能若使用WZ數據填-1
    public static short PET_DEFAULT_FLAG = 0x1E7F;

    public static void gainMobBFGash(MapleMonster monster, MapleCharacter chr) {
        ChannelServer ch = ChannelServer.getInstance(chr.getClient().getChannel());
        // 獲得類型 1 - 樂豆, 2 - 楓點, 3 - 里程
        int type = 2;
        // 系統倍率
        final int caServerrate = ch.getCashRate();
        // BOSS怪物獲得量
        final int cashz = (monster.getStats().isBoss() && monster.getStats().getHPDisplayType() == 0 ? 20 : 0)
                * caServerrate;

        // 通用獲得量
        int cashModifier;
        int level = monster.getStats().getLevel();
        if (level <= 50) {
            cashModifier = 1;
        } else if (level <= 150) {
            cashModifier = 2;
        } else {
            cashModifier = 3;
        }
        // 10等以上擊殺怪物後300%概率獲得樂豆點、楓點或里程
        if (Randomizer.nextInt(1000) < 80 && chr.getLevel() >= 10) {
            chr.modifyCSPoints(type, cashz + cashModifier, true);

            MapleParty party = chr.getParty();
            MapleMap map = chr.getMap();
            if (party != null && map != null) {
                byte pty = 0;
                MapleCharacter pchr = null;
                int i;
                int j;
                for (final MaplePartyCharacter partychar : party.getMembers()) {
                    pchr = map.getCharacterById(partychar.getId());
                    if (pchr != null && pchr.isAlive()) {
                        i = chr.getLevel() - partychar.getLevel();
                        j = level - partychar.getLevel();
                        if ((i <= 5 && i >= -40) || (j <= 5 && j >= -40)) {
                            pty++;
                            if (pchr != chr) {
                                pchr.modifyCSPoints(type, 1, false);
                                pchr.dropMessage(6, "組隊獲得1楓點。");
                            }
                        }
                    }
                }
                if (pty > 1) {
                    chr.modifyCSPoints(type, 1, false);
                    chr.dropMessage(6, "組隊獲得1楓點。");
                }
            }
        }
    }
}
