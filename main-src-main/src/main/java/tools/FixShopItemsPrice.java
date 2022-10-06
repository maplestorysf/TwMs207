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
                        // System.out.println("商品道具ID:" + itemId);
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
                            System.out.println("更正商品價格, 道具ID: " + itemId + " 商店ID: " + rs.getInt("shopid") + " 原價格: "
                                    + rs.getLong("price") + " 改後價格:" + (long) ii.getPrice(itemId));
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
            System.out.println("處理商品失敗, 道具ID:" + itemId);
        }
    }

    public static void main() {
        FixShopItemsPrice i = new FixShopItemsPrice();
        System.out.println("正在加載道具數據......");
        MapleItemInformationProvider.getInstance().runEtc(false);
        MapleItemInformationProvider.getInstance().runItems(false);
        System.out.println("正在讀取商店所有商品......");
        List<Integer> list = i.loadFromDB();
        System.out.println("正在處理商店商品價格......");
        for (int ii : list) {
            // System.out.println("当前处理道具ID:" + ii);
            i.changePrice(ii);
        }
        System.out.println("處理商店商品價格過低結束。");
    }
}
