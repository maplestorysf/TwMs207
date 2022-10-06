/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extensions.temporary;

/**
 *
 * @author pungin
 */
public enum FieldEffectOpcode {

    FieldEffect_Summon(0x0),
    FieldEffect_Tremble(0x1),
    FieldEffect_Object(0x2),
    FieldEffect_Object_Disable(0x3),
    FieldEffect_Screen(0x4),
    FieldEffect_Sound(0x5),
    FieldEffect_MobHPTag(0x6),
    FieldEffect_ChangeBGM(0x7),
    FieldEffect_BGMVolumeOnly(0x8),
    FieldEffect_BGMVolume(0x9),
    UNK_A(0xA),
    FieldEffect_RewordRullet(0xB),
    UNK_C(0xC),
    FieldEffect_TopScreen(0xD),
    FieldEffect_Screen_Delayed(0xE),
    FieldEffect_TopScreen_Delayed(0xF),
    FieldEffect_Screen_AutoLetterBox(0x10),
    FieldEffect_FloatingUI(0x11),
    FieldEffect_Blind(0x12),
    FieldEffect_GrayScale(0x13),
    FieldEffect_OnOffLayer(0x14),
    FieldEffect_Overlap(0x15),
    FieldEffect_Overlap_Detail(0x16),
    FieldEffect_Remove_Overlap_Detail(0x17),
    UNK_18(0x18),
    FieldEffect_ColorChange(0x19),
    FieldEffect_StageClear(0x1A),
    FieldEffect_TopScreen_WithOrigin(0x1B),
    FieldEffect_SpineScreen(0x1C),
    FieldEffect_OffSpineScreen(0x1D),
    UNK_1E(0x1E),
    UNK_1F(0x1F),
    ;

    private final int value;

    private FieldEffectOpcode(int value) {
        this.value = value;
    }

    public static FieldEffectOpcode getType(int type) {
        for (FieldEffectOpcode et : values()) {
            if (et.getValue() == type) {
                return et;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }
}
