package server;

import constants.WorldConstants;
import database.ManagerDatabasePool;
import tools.FileoutputUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class RankingWorker {

    private final static Map<Integer, List<RankingInformation>> rankings = new HashMap<>();
    private final static Map<String, Integer> worldCommands = new HashMap<>();

    public static Integer getWorldCommand(final String world) {
        return worldCommands.get(world);
    }

    public static Map<String, Integer> getWorldCommands() {
        return worldCommands;
    }

    public static List<RankingInformation> getRankingInfo(final int world) {
        return rankings.get(world);
    }

    public static void run() {
        if (!worldCommands.isEmpty() && rankings.isEmpty()) {
            return;
        }
        loadWorldRankings();
        try {
            Connection con = ManagerDatabasePool.getConnection();
            updateRanking(con);
            ManagerDatabasePool.closeConnection(con);
        } catch (Exception ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            System.err.println("Could not update rankings");
        }
    }

    private static void updateRanking(Connection con) throws Exception {
        StringBuilder sb = new StringBuilder(
                "SELECT c.id, c.world, c.exp, c.level, c.name, c.worldRank, c.rank, c.fame");
        sb.append(
                " FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id WHERE a.gm = 0 AND a.banned = 0 AND c.level >= 30");
        sb.append(" ORDER BY c.level DESC , c.exp DESC , c.fame DESC , c.rank ASC");
        PreparedStatement ps;
        try (PreparedStatement charSelect = con.prepareStatement(sb.toString());
                ResultSet rs = charSelect.executeQuery()) {
            ps = con.prepareStatement(
                    "UPDATE characters SET rank = ?, rankMove = ?, worldRank = ?, worldRankMove = ? WHERE id = ?");
            int rank = 0;
            final Map<Integer, Integer> rankMap = new LinkedHashMap<>();
            for (int i : worldCommands.values()) {
                rankMap.put(i, 0); // job to rank
                rankings.put(i, new ArrayList<>());
            }
            while (rs.next()) {
                int world = rs.getInt("world");
                if (!rankMap.containsKey(world)) { // not supported.
                    continue;
                }
                int worldRank = rankMap.get(world) + 1;
                rankMap.put(world, worldRank);
                rank++;
                rankings.get(-1).add(new RankingInformation(rs.getString("name"), world, rs.getInt("level"),
                        rs.getLong("exp"), rank, rs.getInt("fame")));
                rankings.get(world).add(new RankingInformation(rs.getString("name"), world, rs.getInt("level"),
                        rs.getLong("exp"), worldRank, rs.getInt("fame")));
                ps.setInt(1, rank);
                ps.setInt(2, rs.getInt("rank") - rank);
                ps.setInt(3, worldRank);
                ps.setInt(4, rs.getInt("worldRank") - worldRank);
                ps.setInt(5, rs.getInt("id"));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        ps.close();
    }

    public static void loadWorldRankings() {
        worldCommands.clear();
        worldCommands.put("all", -1);
        for (WorldConstants.Option w : WorldConstants.values()) {
            worldCommands.put(w.name(), w.getWorld());
        }
    }

    public static class RankingInformation {

        public String toString;
        public int rank;

        public RankingInformation(String name, int world, int level, long exp, int rank, int fame) {
            this.rank = rank;
            final StringBuilder builder = new StringBuilder("排名 ");
            builder.append(rank);
            builder.append(" : ");
            builder.append(name);
            builder.append(" - 等級 ");
            builder.append(level);
            builder.append(" ,伺服器 ");
            builder.append(WorldConstants.getById(world).name());
            builder.append(" | ");
            builder.append(exp);
            builder.append(" 經驗值, ");
            builder.append(fame);
            builder.append(" 名聲");
            this.toString = builder.toString(); // Rank 1 : KiDALex - Level 200 Blade Master | 0 EXP, 30000 Fame
        }

        @Override
        public String toString() {
            return toString;
        }
    }
}
