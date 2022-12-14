/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import database.ManagerDatabasePool;
import server.MapleItemInformationProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pungin
 */
public class FixShopItemsPrice {

    private List<Integer> loadFromDB() {
        List<Integer> shopItemsId = new ArrayList<>();
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM shopitems ORDER BY itemid")) {
                ResultSet rs = ps.executeQuery();
                int itemId = 0;
                while (rs.next()) {
                    if (itemId != rs.getInt("itemid")) {
                        itemId = rs.getInt("itemid");
                        // System.out.println("ååéå·ID:" + itemId);
                        shopItemsId.add(itemId);
                    }
                }
                rs.close();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("Could not load shop");
        }
        return shopItemsId;
    }

    private void changePrice(int itemId) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con
                    .prepareStatement("SELECT * FROM shopitems WHERE itemid = ? ORDER BY price")) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double a = ii.getPrice(itemId);
                        if (ii.getPrice(itemId) > rs.getLong("price") && rs.getInt("price") != 0) {
                            System.out.println("æ´æ­£ååå¹æ ¼, éå·ID: " + itemId + " ååºID: " + rs.getInt("shopid") + " åå¹æ ¼: "
                                    + rs.getLong("price") + " æ¹å¾å¹æ ¼:" + (long) ii.getPrice(itemId));
                            try (PreparedStatement pp = con.prepareStatement(
                                    "UPDATE shopitems SET price = ? WHERE itemid = ? AND shopid = ?")) {
                                pp.setLong(1, (long) ii.getPrice(itemId));
                                pp.setInt(2, itemId);
                                pp.setInt(3, rs.getInt("shopid"));
                                pp.execute();
                            }
                        }
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.out.println("èçååå¤±æ, éå·ID:" + itemId);
        }
    }

    public static void main() {
        FixShopItemsPrice i = new FixShopItemsPrice();
        System.out.println("æ­£å¨å è¼éå·æ¸æ......");
        MapleItemInformationProvider.getInstance().runEtc(false);
        MapleItemInformationProvider.getInstance().runItems(false);
        System.out.println("æ­£å¨è®åååºææåå......");
        List<Integer> list = i.loadFromDB();
        System.out.println("æ­£å¨èçååºååå¹æ ¼......");
        for (int ii : list) {
            // System.out.println("å½åå¤çéå·ID:" + ii);
            i.changePrice(ii);
        }
        System.out.println("èçååºååå¹æ ¼éä½çµæã");
    }
}
