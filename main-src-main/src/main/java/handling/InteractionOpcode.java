/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling;

/**
 *
 * @author pungin
 */
public enum InteractionOpcode implements WritableIntValueHolder {

    設置物品(0), 設置物品_2(1), 設置物品_3(2), 設置物品_4(3), 設定楓幣(4), 設定楓幣_2(5), 設定楓幣_3(6), 設定楓幣_4(7), 確認交易(8), 確認交易_2(9), 確認交易楓幣(
            10), 確認交易楓幣_2(11), 創建(16), 取消創建(17), 訪問(19), 房間(20), 交易邀請(21), 拒絕邀請(22), 聊天(24), 聊天事件(25), 開啟商店(26), 退出(
            28), 精靈商人_維護(29), 開啟商店_密碼(30), 添加道具(31), 添加道具_2(32), 添加道具_3(33), 添加道具_4(34), 精靈商人_購買道具(
            35), 精靈商人_購買道具2(36), 精靈商人_購買道具3(37), 精靈商人_購買道具4(38), 精靈商人_求購道具(39), 移除道具(47), 精靈商人_離開商店(
            48), 精靈商人_物品整理(49), 精靈商人_關閉商店(50), 精靈商人_關閉完成(51), 管理員更變精靈商人名稱(54), 精靈商人_查看訪問者(
            55), 精靈商人_鎖定清單(56), 精靈商人_更變名稱(59), 精靈商人_添加黑名單(
            57), 精靈商人_移除黑名單(58), 精靈商人_錯誤提示(69), 精靈商人_更新訊息(75), 精靈商人_維護開啟(
            80), START_ROCK_PAPER_SCISSORS1(
            96), START_ROCK_PAPER_SCISSORS2(
            97), START_ROCK_PAPER_SCISSORS3(
            98), INVITE_ROCK_PAPER_SCISSORS(
            112), FINISH_ROCK_PAPER_SCISSORS(
            113), GIVE_UP(
            114), START(
            125),;

    private short code;

    private InteractionOpcode(int newval) {
        this.code = (short) newval;
    }

    @Override
    public short getValue() {
        return code;
    }

    @Override
    public void setValue(short newval) {
        code = newval;
    }

    public static InteractionOpcode getByAction(int i) {
        for (InteractionOpcode s : InteractionOpcode.values()) {
            if (s.getValue() == i) {
                return s;
            }
        }
        return null;
    }
}
