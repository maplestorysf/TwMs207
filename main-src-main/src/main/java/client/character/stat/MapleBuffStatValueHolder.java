package client.character.stat;

import java.util.concurrent.ScheduledFuture;
import server.MapleStatEffect;

public class MapleBuffStatValueHolder {

    public MapleStatEffect effect;
    public long startTime;
    public ScheduledFuture<?> schedule;

    public MapleBuffStatValueHolder(MapleStatEffect effect, long startTime, ScheduledFuture<?> schedule) {
        super();
        this.effect = effect;
        this.startTime = startTime;
        this.schedule = schedule;
    }
}
