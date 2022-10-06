package server.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import client.MapleCharacter;
import client.MapleClient;
import extensions.temporary.FieldEffectOpcode;
import server.Randomizer;
import server.Timer.EventTimer;
import tools.packet.CField;
import tools.packet.CWvsContext;

/**
 *
 * @author Yuuroido
 */
public class MapleMultiBingo extends MapleEvent {

    private static final int TOTAL_STAGE = 3;// 1~5
    private ScheduledFuture<?> callSchedule;
    private ScheduledFuture<?> responseSchedule;
    private int stage;
    private int remainCount;
    private final List<Byte> numbers;
    private final List<Integer> enabledPlayer;
    private final List<MapleCharacter> ranking;
    private final Map<Integer, List<MapleCharacter>> result;
    private final Map<Integer, List<Byte>> cardData;
    private final Map<Integer, List<Boolean>> markedCardData;
    // mapids:922290000(WaitingArea), 922290100(GameField), 922290200(WaitingArea)
    private long time = 10 * 1000;
    private long timeStarted = 0;

    public MapleMultiBingo(final int channel, final MapleEventType type) {
        super(channel, type);
        this.stage = -1;
        this.numbers = new LinkedList<>();
        this.enabledPlayer = new ArrayList<>();
        this.ranking = new ArrayList<>();
        this.result = new HashMap<>();
        this.cardData = new HashMap<>();
        this.markedCardData = new HashMap<>();
    }

    @Override
    public void finished(MapleCharacter chr) {
    }

    @Override
    public void onMapLoad(MapleCharacter chr) {
        super.onMapLoad(chr);
        if (isTimerStarted() && (getMap(0).getId() == chr.getMapId() || getMap(2).getId() == chr.getMapId())) {
            chr.getClient().getSession().writeAndFlush(CField.getClock((int) (getTimeLeft(10) / 1000)));
        }
    }

    @Override
    public void startEvent() {
        resetSchedule();
        this.stage = 0;
        broadcast(CField.stopClock());
        // getMap(0).setClock(10);
        setClock(0, 10);
        this.timeStarted = System.currentTimeMillis();
        EventTimer.getInstance().schedule(this::warpMap, 10000);
    }

    public void setClock(int mapid, int second) {
        time = second * 1000;
        broadcast(mapid, CField.getClock((int) (time / 1000)));
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                broadcast(mapid, CField.stopClock());
            }
        }, time);
    }

    public void resetSchedule() {
        if (callSchedule != null) {
            callSchedule.cancel(false);
            callSchedule = null;
        }
        if (responseSchedule != null) {
            responseSchedule.cancel(false);
            responseSchedule = null;
        }
        this.stage = -1;
        this.result.clear();
    }

    @Override
    public void unreset() {
        for (int i = 0; i < type.mapids.length; i++) {
            for (MapleCharacter chr : getMap(i).getCharactersThreadsafe()) {
                super.warpBack(chr);
            }
        }
        if (callSchedule != null) {
            callSchedule.cancel(false);
            callSchedule = null;
        }
        if (responseSchedule != null) {
            responseSchedule.cancel(false);
            responseSchedule = null;
        }
        super.unreset();
        this.stage = -1;
        this.result.clear();
    }

    public boolean isTimerStarted() {
        return timeStarted > 0;
    }

    public long getTimeLeft(long fTime) {
        return (fTime * 1000) - (System.currentTimeMillis() - timeStarted);
    }

    public void markBingoCard(MapleClient c, byte index, byte number) {
        if (numbers.contains(number)) {
            return;
        }
        int cid = c.getPlayer().getId();
        if (cardData.get(cid) == null || !cardData.get(cid).contains(number)
                || !cardData.get(cid).get(index).equals(number)) {
            c.getSession().close();
        } else {
            markedCardData.get(cid).set(index, true);
            List<Byte> bingoLines = checkBingoCard(cid, index);
            if (bingoLines.size() > 0) {
                // why does nexon not let you automatically say "Bingo!"?
                enabledPlayer.add(cid);
            }
            c.getSession().writeAndFlush(CField.Bingo.markBingoCard(index, number, bingoLines));
        }
    }

    public void callBingo(MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        if (ranking.size() < 30) {
            if (!enabledPlayer.contains(chr.getId())) {
                c.getSession().close();
            } else {
                byte rank = (byte) (ranking.size() + 1);
                this.ranking.add(chr);
                getMap(1).broadcastMessage(CField.Bingo.sendBingoRanking(chr.getId(), chr.getName(), stage, rank));
                if (rank == 30) {
                    finishStage();
                }
            }
        } else {
            // hmm..?
            getMap(1).broadcastMessage(CField.Bingo.sendBingoFailed(chr.getId(), chr.getName()));
        }
    }

    public int getStage() {
        return stage;
    }

    /* use with NPC script (9000267) */
    public int getReward(MapleCharacter chr) {
        int bonus = 0;
        int rank;
        for (List<MapleCharacter> list : result.values()) {
            rank = list.indexOf(chr) + 1;
            if (rank == 1) {
                bonus += 50000000;
            } else if (rank == 2) {
                bonus += 25000000;
            } else if (rank == 3) {
                bonus += 10000000;
            } else if (rank <= 5) {
                bonus += 5000000;
            } else if (rank <= 10) {
                bonus += 2500000;
            } else if (rank <= 20) {
                bonus += 1000000;
            } else if (rank <= 30) {
                bonus += 500000;
            }
        }
        return bonus;
    }

    private void warpMap() {
        if (stage == 0) {
            if (getMap(0).getCharactersSize() < 5) {
                getMap(0).broadcastMessage(CWvsContext.InfoPacket.showInfo("這個遊戲由於缺乏參與的玩家而結束。"));
                EventTimer.getInstance().schedule(this::unreset, 10000);
                return;
            }
            this.stage = 1;
            broadcast(CField.stopClock());
            // getMap(0).setClock(900);
            // getMap(2).setClock(900);
            setClock(0, 900);
            setClock(2, 900);
            EventTimer.getInstance().schedule(this::unreset, 900000);
            EventTimer.getInstance().schedule(this::prepareNextStage, 15000);
            for (MapleCharacter chr : getMap(0).getCharactersThreadsafe()) {
                chr.changeMap(getMap(1));
                // chr.updateInfoQuest(14284, "R1=0;R2=0;R3=0");
                sendBingoCard(chr);
            }
        } else {
            for (MapleCharacter chr : getMap(1).getCharactersThreadsafe()) {
                chr.changeMap(getMap(2));
            }
        }
    }

    private void prepareNextStage() {
        if (stage >= 2) {
            int rank = 1;
            for (MapleCharacter chr : ranking) {
                chr.getClient().getSession().writeAndFlush(CField.Bingo.sendBingoResult(stage - 1, rank++));
            }
            result.put(stage - 1, ranking);
        }
        this.remainCount = 50;
        this.enabledPlayer.clear();
        this.ranking.clear();
        this.numbers.clear();
        for (byte i = 1; i < 76; i++) {
            numbers.add(i);
        }
        Collections.shuffle(numbers);
        getMap(1).broadcastMessage(CField.Bingo.setBingoUI(2, stage));// start announce (default)
        EventTimer.getInstance().schedule(this::prepareBingoCard, 3000);
    }

    private void prepareBingoCard() {
        if (stage >= 2) {
            for (MapleCharacter chr : getMap(1).getCharactersThreadsafe()) {
                sendBingoCard(chr);
            }
        }
        getMap(1).broadcastMessage(CField.Bingo.setBingoUI(3, stage));// show bingo card
        EventTimer.getInstance().schedule(this::startStage, 5000);
    }

    private void startStage() {
        getMap(1).broadcastMessage(
                CField.OnFieldEffect(new String[]{"kite/start"}, FieldEffectOpcode.FieldEffect_TopScreen));
        getMap(1).broadcastMessage(
                CField.OnFieldEffect(new String[]{"multiBingo/start"}, FieldEffectOpcode.FieldEffect_Sound));
        getMap(1).broadcastMessage(CField.Bingo.setBingoUI(4, stage));// stop announce
        EventTimer.getInstance().schedule(this::startCallSchedule, 1000);
    }

    private void startCallSchedule() {
        getMap(1).broadcastMessage(CField.Bingo.sendBingoBallCall(-1, remainCount));
        responseSchedule = EventTimer.getInstance().register(this::sendBingoResponse, 5000, 3000);
        callSchedule = EventTimer.getInstance().register(this::callNumber, 5000, 5000);
    }

    private void sendBingoResponse() {
        getMap(1).broadcastMessage(CField.Bingo.sendBingoResponse());
    }

    private void callNumber() {
        if (remainCount <= 0 || ranking.size() >= 30) {
            finishStage();
        } else {
            byte number = numbers.remove(0);
            getMap(1).broadcastMessage(CField.Bingo.sendBingoBallCall(number, --remainCount));
        }
    }

    private void finishStage() {
        responseSchedule.cancel(false);
        callSchedule.cancel(false);
        getMap(1).broadcastMessage(
                CField.OnFieldEffect(new String[]{"kite/start"}, FieldEffectOpcode.FieldEffect_TopScreen));
        getMap(1).broadcastMessage(
                CField.OnFieldEffect(new String[]{"multiBingo/start"}, FieldEffectOpcode.FieldEffect_Sound));
        getMap(1).broadcastMessage(CField.Bingo.setBingoUI(5, stage));// disable bingo button & clear number log
        if (stage < TOTAL_STAGE) {
            this.stage++;
            EventTimer.getInstance().schedule(this::prepareNextStage, 5000);
        } else {
            this.stage = -2;
            EventTimer.getInstance().schedule(this::warpMap, 10000);
        }
    }

    private void sendBingoCard(MapleCharacter chr) {
        List<Byte> card = createBingoCard();
        cardData.put(chr.getId(), card);
        List<Boolean> markedCard = new LinkedList<>();
        for (byte number : card) {
            markedCard.add(number == 0);// joker
        }
        markedCardData.put(chr.getId(), markedCard);
        chr.getClient().getSession().writeAndFlush(CField.Bingo.sendBingoCard(stage, card));
    }

    private List<Byte> createBingoCard() {
        List<Byte> card = new LinkedList<>();
        byte[][] tempCard = new byte[5][5];
        Set<Byte> temp = new LinkedHashSet<>();
        byte line = 0;
        byte count = 0;
        do {
            temp.add((byte) (15 * line + Randomizer.rand(1, 15)));
            if (temp.size() % 5 == 0) {
                for (byte number : temp) {
                    tempCard[line][count] = number;
                    count++;
                }
                temp.clear();
                line++;
                count = 0;
            }
        } while (line < 5);
        for (byte i = 0; i < 5; i++) {
            for (byte j = 0; j < 5; j++) {
                card.add(tempCard[j][i]);
            }
        }
        card.set(12, (byte) 0);// joker
        return card;
    }

    /* 0:horizontal, 1:vertical, 2:lower right diagonal, 3:upper right diagonal */
    private List<Byte> checkBingoCard(int cid, byte index) {
        List<Byte> bingoLines = new ArrayList<>();
        Boolean[] list = markedCardData.get(cid).toArray(new Boolean[]{});
        if (list[0]) {
            if (list[1] && list[2] && list[3] && list[4] && index >= 0 && index <= 4) {
                bingoLines.add((byte) 0);
            }
            if (list[5] && list[10] && list[15] && list[20]
                    && (index == 0 || index == 5 || index == 10 || index == 15 || index == 20)) {
                bingoLines.add((byte) 1);
            }
            if (list[6] && list[12] && list[18] && list[24]
                    && (index == 0 || index == 6 || index == 12 || index == 18 || index == 24)) {
                bingoLines.add((byte) 2);
            }
        }
        if (list[1] && list[6] && list[11] && list[16] && list[21]
                && (index == 1 || index == 6 || index == 11 || index == 16 || index == 21)
                || list[2] && list[7] && list[12] && list[17] && list[22]
                && (index == 2 || index == 7 || index == 12 || index == 17 || index == 22)
                || list[3] && list[8] && list[13] && list[18] && list[23]
                && (index == 3 || index == 8 || index == 13 || index == 18 || index == 23)) {
            bingoLines.add((byte) 1);
        }
        if (list[4]) {
            if (list[9] && list[14] && list[19] && list[24]
                    && (index == 4 || index == 9 || index == 14 || index == 19 || index == 24)) {
                bingoLines.add((byte) 1);
            }
            if (list[8] && list[12] && list[16] && list[20]
                    && (index == 4 || index == 8 || index == 12 || index == 16 || index == 20)) {
                bingoLines.add((byte) 3);
            }
        }
        if (list[5] && list[6] && list[7] && list[8] && list[9] && index >= 5 && index <= 9
                || list[10] && list[11] && list[12] && list[13] && list[14] && index >= 10 && index <= 14
                || list[15] && list[16] && list[17] && list[18] && list[19] && index >= 15 && index <= 19
                || list[20] && list[21] && list[22] && list[23] && list[24] && index >= 20 && index <= 24) {
            bingoLines.add((byte) 0);
        }
        return bingoLines;
    }
}
