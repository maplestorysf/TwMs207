package handling.login.handler;

import client.LoginCrypto;
import constants.FeaturesConfig;
import database.ManagerDatabasePool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.FileoutputUtil;

public class AutoRegister {

    public static boolean getAccountExists(String login) {
        boolean accountExists = false;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT name FROM accounts WHERE name = ?")) {
                ps.setString(1, login);
                ResultSet rs = ps.executeQuery();
                if (rs.first()) {
                    accountExists = true;
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            System.out.println("Ex:" + ex);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
        }
        return accountExists;
    }

    public static boolean createAccount(String login, String pwd, String eip, String sMac) {
        String sockAddr = eip;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            String sSQL = FeaturesConfig.accounts_checkIPorMAC == 1
                    ? "SELECT SessionIP FROM accounts WHERE SessionIP = ?"
                    : "SELECT macs FROM accounts WHERE macs = ?";
            try (PreparedStatement ipc = con.prepareStatement(sSQL)) {
                ipc.setString(1,
                        FeaturesConfig.accounts_checkIPorMAC == 1 ? (sockAddr.substring(1, sockAddr.lastIndexOf(':')))
                                : sMac);
                try (ResultSet rs = ipc.executeQuery()) {
                    if (rs.first() == false || rs.last() == true && (FeaturesConfig.accounts_checkIPorMAC == 0)
                            || (FeaturesConfig.accounts_checkIPorMAC > 0
                            && rs.getRow() < FeaturesConfig.ACCOUNTS_PER_IPorMAC)) {
                        try {
                            try (PreparedStatement ps = con.prepareStatement(
                                    "INSERT INTO accounts (name, password, email, birthday, macs, SessionIP) VALUES (?, ?, ?, ?, ?, ?)")) {
                                ps.setString(1, login);
                                ps.setString(2, LoginCrypto.hexSha1(pwd));
                                ps.setString(3, "autoregister@mail.com");
                                ps.setString(4, "2008-04-07 00:00:00");
                                ps.setString(5, sMac);
                                ps.setString(6, sockAddr.substring(1, sockAddr.lastIndexOf(':')));
                                ps.executeUpdate();
                            }

                            return true;
                        } catch (SQLException ex) {
                            System.out.println("Ex:" + ex);
                            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
                        }
                    } else {
                        FileoutputUtil.logToFile(FileoutputUtil.Register_Log,
                                "註冊帳號：[" + login + "] 電腦IP(MAC)：[" + eip + "(" + sMac + ")" + "(註冊次數：" + rs.getRow()
                                + "]" + " 回傳結果-first：" + rs.first() + " 回傳結果-last：" + rs.last() + "\r\n");
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            System.out.println("Ex:" + ex);
        }
        return false;
    }
}
