/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.wztosql;

import database.ManagerDatabasePool;
import java.sql.Connection;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.swing.Progressbar;
import tools.FileoutputUtil;
import tools.StringUtil;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Itzik
 */
public class DumpNpcNames {

    private static final Map<Integer, String> npcNames = new HashMap<>();

    public static void start(String[] args) {
        try {
            Progressbar.setValue(0);
            Progressbar.setText("轉存NPC名稱...");
            System.out.println("Dumping npc name data.");
            DumpNpcNames dump = new DumpNpcNames();
            dump.dumpNpcNameData();
            System.out.println("Dump complete.");
            Progressbar.setValue(100);
        } catch (SQLException ex) {
            Logger.getLogger(DumpNpcNames.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void dumpNpcNameData() throws SQLException {
        MapleDataProvider npcData = MapleDataProviderFactory.getDataProvider("Npc");
        MapleDataProvider stringDataWZ = MapleDataProviderFactory.getDataProvider("String");
        MapleData npcStringData = stringDataWZ.getData("Npc.img");
        try (PreparedStatement ps = ManagerDatabasePool.getConnection()
                .prepareStatement("DELETE FROM `wz_npcnamedata`")) {
            ps.execute();
        }
        for (MapleData c : npcStringData.getChildren()) {
            if (Progressbar.getValue() < 50) {
                Progressbar.addValue();
            }
            int nid = Integer.parseInt(c.getName());
            String n = StringUtil.getLeftPaddedStr(nid + ".img", '0', 11);
            try {
                if (npcData.getData(n) != null) {// only thing we really have to do is check if it exists. if we wanted
                                                 // to, we could get the script as well :3
                    String name = MapleDataTool.getString("name", c, "MISSINGNO");
                    if (name.contains("Maple TV") || name.contains("Baby Moon Bunny")) {
                        continue;
                    }
                    npcNames.put(nid, name);
                }
            } catch (NullPointerException e) {
            } catch (RuntimeException e) { // swallow, don't add if
            }
        }
        npcNames.keySet().stream().map((key) -> {
            if (Progressbar.getValue() < 90) {
                Progressbar.addValue();
            }
            return key;
        }).forEach((key) -> {
            try (Connection con = ManagerDatabasePool.getConnection();PreparedStatement ps = 
                   con.prepareStatement("INSERT INTO `wz_npcnamedata` (`npc`, `name`) VALUES (?, ?)")) {
                ps.setInt(1, key);
                ps.setString(2, npcNames.get(key));
                ps.execute();
                if(key.toString().equals("1540466")) {
                    int x = 1;
                }
                System.out.println("key: " + key + " name: " + npcNames.get(key));
            } catch (Exception ex) {
                System.out.println("Failed to save key " + key);
                FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
            }
        });
    }
}
