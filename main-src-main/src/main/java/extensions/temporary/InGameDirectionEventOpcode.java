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
public enum InGameDirectionEventOpcode {

    InGameDirectionEvent_ForcedAction(0), InGameDirectionEvent_Delay(1), InGameDirectionEvent_EffectPlay(
            2), InGameDirectionEvent_ForcedInput(3), InGameDirectionEvent_PatternInputRequest(
            4), InGameDirectionEvent_CameraMove(5), InGameDirectionEvent_CameraOnCharacter(
            6), InGameDirectionEvent_CameraZoom(7), InGameDirectionEvent_CameraReleaseFromUserPoint(
            8), InGameDirectionEvent_VansheeMode(9), InGameDirectionEvent_FaceOff(
            10), InGameDirectionEvent_Monologue(
            11), InGameDirectionEvent_MonologueScroll(
            12), InGameDirectionEvent_AvatarLookSet(
            13), InGameDirectionEvent_RemoveAdditionalEffect(
            14), InGameDirectionEvent_ForcedMove(
            15), InGameDirectionEvent_ForcedFlip(
            16), InGameDirectionEvent_InputUI(
            17),;

    private final int value;

    private InGameDirectionEventOpcode(int value) {
        this.value = value;
    }

    public static InGameDirectionEventOpcode getType(int type) {
        for (InGameDirectionEventOpcode dt : values()) {
            if (dt.getValue() == type) {
                return dt;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }
}
