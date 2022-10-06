package extensions.temporary;

/**
 *
 * @author 寒霜天地
 */
public enum StorageType {

    TAKE_OUT(0x09), INVENTORY_FULL(0x0A), MESO_NOT_ENOUGH(0x0B),
    // 0x0C : 因有只限擁有一個的道具，\r\n因此未能查獲道具。
    STORE(0x0D),
    // 0x0E
    ARRANGE(0x0F),
    // 0x10 : MESO_NOT_ENOUGH(0x10),
    FULL(0x11),
    // 0x12
    MESO(0x13),
    // 0x14
    AUTH(0x15), OPEN(0x16), MOVE_FAIL(0x17), ACCOUNT_PROTECT(0x18), CHECK_IP(0x19), FORBID_USE(0x1A), EXPIRED(0x1B),
    // 0x1C : 相關功能目前是無法使用的狀態。
    MESO_EXCEED(0x1D);
    ;

    private final int type;

    private StorageType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
