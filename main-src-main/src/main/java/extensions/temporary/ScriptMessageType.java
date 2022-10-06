/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package extensions.temporary;

/**
 *
 * @author Itzik
 */
public enum ScriptMessageType {

    SM_SAY(0x0), UNK_1(0x1), SM_SAYIMAGE(0x2), SM_ASKYESNO(0x3), SM_ASKTEXT(0x4), SM_ASKNUMBER(0x5), SM_ASKMENU(
            0x6), UNK7(0x7), SM_ASKQUIZ(0x8), SM_ASKSPEEDQUIZ(0x9), SM_ASKICQUIZ(0xA), SM_ASKAVATAREX(
            0xB), SM_ASKANDROID(0xC), SM_ASKPET(0xD), SM_ASKPETALL(0xE), SM_ASKACTIONPET_EVOLUTION(
            0xF), SM_SCRIPT(0x10), SM_ASKACCEPT(0x11), UNK_12(0x12), SM_ASKBOXTEXT(
            0x13), SM_ASKSLIDEMENU(0x14), SM_ASKINGAMEDIRECTION(0x15), SM_PLAYMOVIECLIP(
            0x16), SM_PLAYMOVIECLIP_URL(0x17), SM_ASKCENTER(0x18), UNK_19(0x19), UNK_1A(
            0x1A), SM_ASKSELECTMENU(0x1B), SM_ASKANGELICBUSTER(
            0x1C), SM_SAY_ILLUSTRATION(0x1D), SM_SAY_DUAL_ILLUSTRATION(
            0x1E), SM_ASKYESNO_ILLUSTRATION(
            0x1F), SM_ASKACCEPT_ILLUSTRATION(
            0x20), SM_ASKMENU_ILLUSTRATION(
            0x21), SM_ASKYESNO_DUAL_ILLUSTRATION(
            0x22), SM_ASKACCEPT_DUAL_ILLUSTRATION(
            0x23), SM_ASKMENU_DUAL_ILLUSTRATION(
            0x24), SM_ASKSSN2(
            0x25), SM_ASKAVATAREXZERO(
            0x26), SM_MONOLOGUE(
            0x27), SM_ASK_WEAPONBOX(
            0x28), SM_ASKBOXTEXT_BGIMG(
            0x29), SM_ASK_USER_SURVEY(
            0x2A), SM_SUCCESS_CAMERA(
            0x2B), SM_ASKMIXHAIR(
            0x2C), SM_ASKMIXHAIR_EX_ZERO(
            0x2D), SM_ASKCUSTOMMIXHAIR(
            0x2E), SM_ASKCUSTOMMIXHAIR_AND_PROB(
            0x2F), SM_ASKMIXHAIR_NEW(
            0x30), SM_ASKMIXHAIR_NEW_EX_ZERO(
            0x31), SM_NPCACTION(
            0x32), SM_ASK_SCREEN_SHINNING_STAR_MSG(
            0x33), SM_INPUT_UI(
            0x34), UNK_35(
            0x35), SM_ASKNUMBER_KEYPAD(
            0x36), SM_SPINOFF_GUITAR_RHYTHMGAME(
            0x37), SM_ASK_GHOSTPARK_ENTER_UI(
            0x38), UNK_39(
            0x39), // SM_CAMERA_MSG
    UNK_3A(0x3A), // SM_SLIDE_PUZZLE
    UNK_3B(0x3B), // SM_DISGUISE
    UNK_3C(0x3C), // SM_NEED_CLIENT_RESPONSE
    UNK_3D(0x3D), UNK_3E(0x3E), UNK_3F(0x3F), UNK_40(0x40), UNK_41(0x41), UNK_42(0x42), UNK_43(0x43), UNK_44(
            0x44), UNK_45(0x45), UNK_46(0x46),;

    private final byte type;

    private ScriptMessageType(int type) {
        this.type = (byte) type;
    }

    public static ScriptMessageType getNPCTalkType(int type) {
        for (ScriptMessageType dt : values()) {
            if (dt.getType() == type) {
                return dt;
            }
        }
        return null;
    }

    public byte getType() {
        return type;
    }
}
