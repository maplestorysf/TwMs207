package constants;

/**
 *
 * @author 寒霜天地
 */
public enum EventListConstants {

    活動1("新增玩家活動", 20160330, 20160413, new int[]{2433956, 2433958, 2435301, 4001871, 2450064}), 活動2("回流玩家活動",
            20160330, 20160413, new int[]{2000019, 2450064, 4001832, 4001872, 2049762}), 活動3("春天大地探索活動", 20160413,
            20160504, new int[]{1142969, 3700371, 2049703, 2048314, 2450064}), 活動4("REBOOT世界活動", 20160330,
            20160622, new int[]{1142970, 1113237, 1113238, 1113239, 1113240}),;

    public static final String EVENT_NAME = "楓之谷活動";

    private final String eventName;
    private final int startTime, endTime;
    private final int[] itemId;

    private EventListConstants(String eventName, int startTime, int endTime, int[] itemId) {
        this.eventName = eventName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.itemId = itemId;
    }

    public String getEventName() {
        return eventName;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public int[] getItemId() {
        return itemId;
    }

    public int getItemQuantity() {
        return itemId.length;
    }
}
