package handling.channel.handler;

import client.MapleCharacter;
import client.MapleJob;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import server.maps.AnimatedMapleMapObject;
import server.movement.*;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.data.LittleEndianAccessor;

public class MovementParse {

    public static List<LifeMovementFragment> parseMovement(final LittleEndianAccessor lea, final int kind,
            MapleCharacter chr) {
        final List<LifeMovementFragment> res = new ArrayList<>();
        final byte numCommands = lea.readByte();

        byte command;
        StaticLifeMovement mov;

        Point pos, wobble, offset;
        short fh, newFh;

        byte newstate, wui;
        short duration;
        for (byte i = 0; i < numCommands; i++) {
            command = lea.readByte();
            switch (command) {
                case 0:
                case 8:
                case 15:
                case 17:
                case 19:
                case 67:
                case 68:
                case 69:
                case 70:
                case 71: {
                    pos = new Point((short) lea.readShort(), lea.readShort());
                    wobble = new Point(lea.readShort(), lea.readShort());
                    newFh = lea.readShort();
                    if (command == 15 || command == 17) {
                        fh = lea.readShort();
                    } else {
                        fh = 0;
                    }
                    offset = new Point(lea.readShort(), lea.readShort());
                    break;
                }
                case 56:
                case 66:
                case 90: {
                    pos = new Point((short) lea.readShort(), lea.readShort());
                    wobble = new Point(lea.readShort(), lea.readShort());
                    newFh = lea.readShort();
                    fh = 0;
                    offset = null;
                    break;
                }
                case 1:
                case 2:
                case 18:
                case 21:
                case 22:
                case 24:
                case 62:
                case 63:
                case 64:
                case 65: {
                    pos = new Point(0, 0);
                    wobble = new Point(lea.readShort(), lea.readShort());
                    newFh = 0;
                    if (command == 21 || command == 22) {
                        fh = lea.readShort();
                    } else {
                        fh = 0;
                    }
                    offset = null;
                    break;
                }
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 50:
                case 51:
                case 55:
                case 57:
                case 59:
                case 60:
                case 61:
                case 72:
                case 73:
                case 74:
                case 76:
                case 81:
                case 83:
                case 85:
                case 86:
                case 87:
                case 88: {
                    pos = new Point(0, 0);
                    wobble = null;
                    newFh = 0;
                    fh = 0;
                    offset = null;
                    break;
                }
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 9:
                case 10:
                case 11:
                case 13:
                case 26:
                case 27:
                case 52:
                case 53:
                case 54:
                case 58:
                case 78:
                case 79:
                case 80:
                case 82:
                case 84:
                case 91: {
                    pos = new Point((short) lea.readShort(), lea.readShort());
                    wobble = null;
                    newFh = lea.readShort();
                    fh = 0;
                    offset = null;
                    break;
                }
                case 14:
                case 16: {
                    pos = new Point(0, 0);
                    wobble = new Point(lea.readShort(), lea.readShort());
                    newFh = 0;
                    fh = lea.readShort();
                    offset = null;
                    break;
                }
                case 49: {
                    pos = new Point(0, 0);
                    wobble = null;
                    newFh = 0;
                    fh = lea.readShort();
                    offset = null;
                    break;
                }
                case 23: {
                    pos = new Point((short) lea.readShort(), lea.readShort());
                    wobble = new Point(lea.readShort(), lea.readShort());
                    newFh = 0;
                    fh = 0;
                    offset = null;
                    break;
                }
                case 12: {
                    pos = new Point(0, 0);

                    mov = new StaticLifeMovement(command, pos, 0, 0, 0);
                    mov.setWui(lea.readByte());
                    res.add(mov);
                    continue;
                }
                default: {
                    if (command == 72 || command == 74) {
                        pos = new Point((short) lea.readShort(), lea.readShort());
                        wobble = new Point(lea.readShort(), lea.readShort());
                        fh = lea.readShort();
                        offset = new Point(lea.readShort(), lea.readShort());

                        mov = new StaticLifeMovement(command, pos, 0, 0, 0);
                        mov.setFh(fh);
                        mov.setPixelsPerSecond(wobble);
                        mov.setOffset(offset);
                        res.add(mov);
                        continue;
                    } else if (command > 0 && command < 88) {
                        pos = new Point(0, 0);
                        wobble = null;
                        newFh = 0;
                        fh = 0;
                        offset = null;
                        break;
                    }
                    System.out.println("未知的移動類型: 0x" + HexTool.toString(command) + " - ( " + command + " )");
                    if (chr.isShowErr()) {
                        chr.showInfo("移動", true, "未知的移動類型: 0x" + HexTool.toString(command) + " - ( " + command + " )");
                    }
                    String moveMsg = "";
                    switch (kind) {
                        case 1:
                            moveMsg = "玩家";
                            break;
                        case 2:
                            moveMsg = "怪物";
                            break;
                        case 3:
                            moveMsg = "寵物";
                            break;
                        case 4:
                            moveMsg = "召喚獸";
                            break;
                        case 5:
                            moveMsg = "龍";
                            break;
                        case 6:
                            moveMsg = "花狐";
                            break;
                        default:
                            break;
                    }
                    FileoutputUtil.log(FileoutputUtil.Movement_Log,
                            moveMsg + "(" + chr.getName() + ") 職業：" + MapleJob.getName(MapleJob.getById(chr.getJob())) + "("
                            + chr.getJob() + ")  未知移動封包 剩餘次數: " + (numCommands - res.size()) + " 移動類型: 0x"
                            + HexTool.toString(command) + ", 封包: " + lea.toString(true));
                    return null;
                }
            }
            newstate = lea.readByte();
            duration = lea.readShort();
            wui = lea.readByte();

            mov = new StaticLifeMovement(command, pos, duration, newstate, newFh);

            mov.setFh(fh);
            if (wobble != null) {
                mov.setPixelsPerSecond(wobble);
            }
            if (offset != null) {
                mov.setOffset(offset);
            }
            mov.setWui(wui);

            res.add(mov);
        }

        double skip = lea.readByte();
        skip = Math.ceil(skip / 2.0D);
        lea.skip((int) skip);
        if (numCommands != res.size()) {
            System.out.println("循環次數[" + numCommands + "]和實際上獲取的循環次數[" + res.size() + "]不符");
            if (chr.isShowErr()) {
                chr.showInfo("移動", true, "循環次數[" + numCommands + "]和實際上獲取的循環次數[" + res.size() + "]不符");
            }
            FileoutputUtil.log(FileoutputUtil.Movement_Log,
                    "循環次數[" + numCommands + "]和實際上獲取的循環次數[" + res.size() + "]不符 " + "(" + chr.getName() + ") 職業："
                    + MapleJob.getName(MapleJob.getById(chr.getJob())) + "(" + chr.getJob() + "移動封包 剩餘次數: "
                    + (numCommands - res.size()) + "  封包: " + lea.toString(true));
            return null; // Probably hack
        }
        return res;
    }

    public static void updatePosition(final List<LifeMovementFragment> movement, final AnimatedMapleMapObject target,
            final int yoffset) {
        if (movement == null) {
            return;
        }
        for (final LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof StaticLifeMovement) {
                    final Point position = move.getPosition();
                    if (position != null) {
                        position.y += yoffset;
                        target.setPosition(position);
                    } else {
                        System.err.println("更新坐標，但是坐標為空");
                    }
                }
                target.setFh(((LifeMovement) move).getNewFh());
                target.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}
