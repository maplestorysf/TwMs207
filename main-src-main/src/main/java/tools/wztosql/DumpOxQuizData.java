/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.wztosql;

import constants.ServerConstants;
import database.ManagerDatabasePool;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.swing.Progressbar;

import java.nio.charset.CharsetEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Itzik
 */
public class DumpOxQuizData {

    private final Connection con;
    static CharsetEncoder asciiEncoder = ServerConstants.MAPLE_TYPE.getCharset().newEncoder();

    public DumpOxQuizData() throws SQLException {
        this.con = ManagerDatabasePool.getConnection();
    }

    public static void start(String args[]) {
        try {
            Progressbar.setValue(0);
            Progressbar.setText("轉存OX問答...");
            // String output = args[0];
            // File outputDir = new File(output);
            // File cashTxt = new File("ox.sql");
            // outputDir.mkdir();
            // cashTxt.createNewFile();
            System.out.println("OXQuiz.img Loading ...");
            // try (PrintWriter writer = new PrintWriter(new FileOutputStream(cashTxt))) {
            // writer.println("INSERT INTO `wz_oxdata` (`questionset`, `questionid`,
            // `question`, `display`, `answer`) VALUES");
            DumpOxQuizData dump = new DumpOxQuizData();
            dump.dumpOxData();
            // writer.flush();
            // }
            System.out.println("Ox quiz data is complete");
            Progressbar.setValue(100);
        } catch (SQLException ex) {
            Logger.getLogger(DumpOxQuizData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void dumpOxData() throws SQLException {
        MapleDataProvider stringProvider;
        stringProvider = MapleDataProviderFactory.getDataProvider("Etc");
        MapleData ox = stringProvider.getData("OXQuiz.img");
        PreparedStatement ps = con.prepareStatement("DELETE FROM `wz_oxdata`");
        ps.execute();
        ps.close();
        for (MapleData child1 : ox.getChildren()) {
            for (MapleData child2 : child1.getChildren()) {
                if (Progressbar.getValue() < 90) {
                    Progressbar.addValue();
                }
                MapleData q = child2.getChildByPath("q");
                MapleData d = child2.getChildByPath("d");
                int a = MapleDataTool.getInt(child2.getChildByPath("a"));
                String qs = "";
                String ds = "";
                String as;
                if (a == 0) {
                    as = "x";
                } else {
                    as = "o";
                }
                if (q != null) {
                    qs = (String) q.getData();
                }
                if (d != null) {
                    ds = (String) d.getData();
                }
                if (!asciiEncoder.canEncode(child1.getName()) || !asciiEncoder.canEncode(child2.getName())
                        || !asciiEncoder.canEncode(qs) || !asciiEncoder.canEncode(ds) || !asciiEncoder.canEncode(as)) {
                    continue;
                }
                ps = con.prepareStatement(
                        "INSERT INTO `wz_oxdata`" + " (`questionset`, `questionid`, `question`, `display`, `answer`)"
                                + " VALUES (?, ?, ?, ?, ?)");
                ps.setString(1, child1.getName());
                ps.setString(2, child2.getName());
                ps.setString(3, qs);
                ps.setString(4, ds);
                ps.setString(5, as);
                ps.execute();
                ps.close();
                // writer.println("(" + child1.getName() + "," + child2.getName() + ", '" + qs +
                // "', '" + ds + "', '" + as + "'), ");
            }
        }
    }
}
