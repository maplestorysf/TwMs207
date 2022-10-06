/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.character.stat;

import client.MapleCharacter;
import constants.GameConstants;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.ConcurrentEnumMap;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;
import server.MapleStatEffect;

/**
 *
 * @author Weber
 */
public class SecondaryStat implements Serializable {

    public int nViperCharge;
    public Map<MapleBuffStat, IndieTempStat> aIndieTempStat;
    public byte nDefenseAtt;
    public byte nPVPDamage;
    public final List<TemporaryStatBase> aTemporaryStat = new ArrayList<>(8);
    public transient Map<MapleBuffStat, TemporaryStatBase> stats;
    public byte nDefenseState;
    public final StopForceAtom pStopForceAtom;

    public SecondaryStat() {
        this.aIndieTempStat = new HashMap<>();
        this.pStopForceAtom = new StopForceAtom();
        this.stats = new ConcurrentEnumMap<>(MapleBuffStat.class);
        aTemporaryStat.add(new EnergyCharged());
        aTemporaryStat.add(new DashSpeed());
        aTemporaryStat.add(new DashJump());
        aTemporaryStat.add(new RideVehicle());
        aTemporaryStat.add(new PartyBooster());
        aTemporaryStat.add(new GuidedBullet());
        aTemporaryStat.add(new Undead());
        aTemporaryStat.add(new Undead());
        aTemporaryStat.add(new RideVehicleExpire());
    }

    public void EncodeForRemote(MaplePacketLittleEndianWriter mplew) {
        final ArrayList<Pair<Integer, Integer>> uFlagData = new ArrayList<>();
        final ArrayList<MapleBuffStat> aDefaultFlags = new ArrayList<>();
        final int[] uFlagTemp = new int[GameConstants.MAX_BUFFSTAT];

        aDefaultFlags.add(MapleBuffStat.IDA_BUFF_53);
        aDefaultFlags.add(MapleBuffStat.IDA_BUFF_54);
        aDefaultFlags.add(MapleBuffStat.PyramidEffect);
        aDefaultFlags.add(MapleBuffStat.KillingPoint);
        aDefaultFlags.add(MapleBuffStat.ZeroAuraStr);
        aDefaultFlags.add(MapleBuffStat.ZeroAuraSpd);
        aDefaultFlags.add(MapleBuffStat.BMageAura);
        aDefaultFlags.add(MapleBuffStat.BattlePvP_Helena_Mark);
        aDefaultFlags.add(MapleBuffStat.BattlePvP_LangE_Protection);
        aDefaultFlags.add(MapleBuffStat.PinkbeanRollingGrade);
        aDefaultFlags.add(MapleBuffStat.AdrenalinBoost);
        aDefaultFlags.add(MapleBuffStat.RWBarrierHeal);
        aDefaultFlags.add(MapleBuffStat.IDA_BUFF_519);
        aDefaultFlags.add(MapleBuffStat.IDA_BUFF_567);
        aDefaultFlags.add(MapleBuffStat.EnergyCharged);
        aDefaultFlags.add(MapleBuffStat.DashSpeed);
        aDefaultFlags.add(MapleBuffStat.DashJump);
        aDefaultFlags.add(MapleBuffStat.RideVehicle);
        aDefaultFlags.add(MapleBuffStat.PartyBooster);
        aDefaultFlags.add(MapleBuffStat.GuidedBullet);
        aDefaultFlags.add(MapleBuffStat.Undead);
        aDefaultFlags.add(MapleBuffStat.RideVehicleExpire);
        aDefaultFlags.add(MapleBuffStat.COUNT_PLUS1);

        if (getStatOption(MapleBuffStat.Speed).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Speed)) {
            uFlagTemp[MapleBuffStat.Speed.getPosition()] |= MapleBuffStat.Speed.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Speed).nOption, 1));
        }
        if (getStatOption(MapleBuffStat.ComboCounter).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ComboCounter)) {
            uFlagTemp[MapleBuffStat.ComboCounter.getPosition()] |= MapleBuffStat.ComboCounter.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ComboCounter).nOption, 1));
        }

        if (getStatOption(MapleBuffStat.IDA_BUFF_82).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_82)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_82.getPosition()] |= MapleBuffStat.IDA_BUFF_82.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_82).nOption, 1));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_83).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_83)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_83.getPosition()] |= MapleBuffStat.IDA_BUFF_83.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_83).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_83).rOption, 4));
        }

        if (getStatOption(MapleBuffStat.WeaponCharge).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.WeaponCharge)) {
            uFlagTemp[MapleBuffStat.WeaponCharge.getPosition()] |= MapleBuffStat.WeaponCharge.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.WeaponCharge).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.WeaponCharge).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ElementalCharge).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ElementalCharge)) {
            uFlagTemp[MapleBuffStat.ElementalCharge.getPosition()] |= MapleBuffStat.ElementalCharge.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ElementalCharge).nOption, 2));
        }
        if (getStatOption(MapleBuffStat.Stun).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Stun)) {
            uFlagTemp[MapleBuffStat.Stun.getPosition()] |= MapleBuffStat.Stun.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Stun).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Stun).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Shock).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Shock)) {
            uFlagTemp[MapleBuffStat.Shock.getPosition()] |= MapleBuffStat.Shock.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Shock).nOption, 1));
        }
        if (getStatOption(MapleBuffStat.Darkness).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Darkness)) {
            uFlagTemp[MapleBuffStat.Darkness.getPosition()] |= MapleBuffStat.Darkness.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Darkness).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Darkness).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Seal).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Seal)) {
            uFlagTemp[MapleBuffStat.Seal.getPosition()] |= MapleBuffStat.Seal.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Seal).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Seal).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Weakness).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Weakness)) {
            uFlagTemp[MapleBuffStat.Weakness.getPosition()] |= MapleBuffStat.Weakness.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Weakness).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Weakness).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.WeaknessMdamage).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.WeaknessMdamage)) {
            uFlagTemp[MapleBuffStat.WeaknessMdamage.getPosition()] |= MapleBuffStat.WeaknessMdamage.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.WeaknessMdamage).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.WeaknessMdamage).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Curse).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Curse)) {
            uFlagTemp[MapleBuffStat.Curse.getPosition()] |= MapleBuffStat.Curse.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Curse).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Curse).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Slow).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Slow)) {
            uFlagTemp[MapleBuffStat.Slow.getPosition()] |= MapleBuffStat.Slow.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Slow).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Slow).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.PvPRaceEffect).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.PvPRaceEffect)) {
            uFlagTemp[MapleBuffStat.PvPRaceEffect.getPosition()] |= MapleBuffStat.PvPRaceEffect.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PvPRaceEffect).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PvPRaceEffect).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.TimeBomb).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.TimeBomb)) {
            uFlagTemp[MapleBuffStat.TimeBomb.getPosition()] |= MapleBuffStat.TimeBomb.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.TimeBomb).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.TimeBomb).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Team).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Team)) {
            uFlagTemp[MapleBuffStat.Team.getPosition()] |= MapleBuffStat.Team.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Team).nOption, 1));
        }
        if (getStatOption(MapleBuffStat.DisOrder).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DisOrder)) {
            uFlagTemp[MapleBuffStat.DisOrder.getPosition()] |= MapleBuffStat.DisOrder.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DisOrder).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DisOrder).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Thread).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Thread)) {
            uFlagTemp[MapleBuffStat.Thread.getPosition()] |= MapleBuffStat.Thread.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Thread).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Thread).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Poison).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Poison)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Poison).nOption, 2));
        }
        if (getStatOption(MapleBuffStat.Poison).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Poison)) {
            uFlagTemp[MapleBuffStat.Poison.getPosition()] |= MapleBuffStat.Poison.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Poison).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Poison).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ShadowPartner).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ShadowPartner)) {
            uFlagTemp[MapleBuffStat.ShadowPartner.getPosition()] |= MapleBuffStat.ShadowPartner.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ShadowPartner).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ShadowPartner).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.DarkSight).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DarkSight)) {
            uFlagTemp[MapleBuffStat.DarkSight.getPosition()] |= MapleBuffStat.DarkSight.getValue();
        }
        if (getStatOption(MapleBuffStat.SoulArrow).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.SoulArrow)) {
            uFlagTemp[MapleBuffStat.SoulArrow.getPosition()] |= MapleBuffStat.SoulArrow.getValue();
        }
        if (getStatOption(MapleBuffStat.Morph).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Morph)) {
            uFlagTemp[MapleBuffStat.Morph.getPosition()] |= MapleBuffStat.Morph.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Morph).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Morph).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Ghost).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Ghost)) {
            uFlagTemp[MapleBuffStat.Ghost.getPosition()] |= MapleBuffStat.Ghost.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Ghost).nOption, 2));
        }
        if (getStatOption(MapleBuffStat.Attract).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Attract)) {
            uFlagTemp[MapleBuffStat.Attract.getPosition()] |= MapleBuffStat.Attract.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Attract).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Attract).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Magnet).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Magnet)) {
            uFlagTemp[MapleBuffStat.Magnet.getPosition()] |= MapleBuffStat.Magnet.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Magnet).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Magnet).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.MagnetArea).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.MagnetArea)) {
            uFlagTemp[MapleBuffStat.MagnetArea.getPosition()] |= MapleBuffStat.MagnetArea.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MagnetArea).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MagnetArea).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.NoBulletConsume).rOption > 0
                || aDefaultFlags.contains(MapleBuffStat.NoBulletConsume)) {
            uFlagTemp[MapleBuffStat.NoBulletConsume.getPosition()] |= MapleBuffStat.NoBulletConsume.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.NoBulletConsume).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.BanMap).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BanMap)) {
            uFlagTemp[MapleBuffStat.BanMap.getPosition()] |= MapleBuffStat.BanMap.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BanMap).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BanMap).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Barrier).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Barrier)) {
            uFlagTemp[MapleBuffStat.Barrier.getPosition()] |= MapleBuffStat.Barrier.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Barrier).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Barrier).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.DojangShield).rOption > 0
                || aDefaultFlags.contains(MapleBuffStat.DojangShield)) {
            uFlagTemp[MapleBuffStat.DojangShield.getPosition()] |= MapleBuffStat.DojangShield.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DojangShield).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DojangShield).rOption, 4));
        }

        if (getStatOption(MapleBuffStat.ReverseInput).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ReverseInput)) {
            uFlagTemp[MapleBuffStat.ReverseInput.getPosition()] |= MapleBuffStat.ReverseInput.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ReverseInput).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ReverseInput).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.RespectPImmune).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.RespectPImmune)) {
            uFlagTemp[MapleBuffStat.RespectPImmune.getPosition()] |= MapleBuffStat.RespectPImmune.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.RespectPImmune).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.RespectMImmune).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.RespectMImmune)) {
            uFlagTemp[MapleBuffStat.RespectMImmune.getPosition()] |= MapleBuffStat.RespectMImmune.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.RespectMImmune).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.DefenseAtt).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DefenseAtt)) {
            uFlagTemp[MapleBuffStat.DefenseAtt.getPosition()] |= MapleBuffStat.DefenseAtt.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DefenseAtt).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.DefenseState).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DefenseState)) {
            uFlagTemp[MapleBuffStat.DefenseState.getPosition()] |= MapleBuffStat.DefenseState.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DefenseState).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.DojangBerserk).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DojangBerserk)) {
            uFlagTemp[MapleBuffStat.DojangBerserk.getPosition()] |= MapleBuffStat.DojangBerserk.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DojangBerserk).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DojangBerserk).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.DojangInvincible).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DojangInvincible)) {
            uFlagTemp[MapleBuffStat.DojangInvincible.getPosition()] |= MapleBuffStat.DojangInvincible.getValue();
        }
        if (getStatOption(MapleBuffStat.RepeatEffect).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.RepeatEffect)) {
            uFlagTemp[MapleBuffStat.RepeatEffect.getPosition()] |= MapleBuffStat.RepeatEffect.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.RepeatEffect).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.RepeatEffect).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ExpBuffRate).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ExpBuffRate)) {
            uFlagTemp[MapleBuffStat.ExpBuffRate.getPosition()] |= MapleBuffStat.ExpBuffRate.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ExpBuffRate).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ExpBuffRate).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.PLAYERS_BUFF435).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.PLAYERS_BUFF435)) {
            uFlagTemp[MapleBuffStat.PLAYERS_BUFF435.getPosition()] |= MapleBuffStat.PLAYERS_BUFF435.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PLAYERS_BUFF435).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PLAYERS_BUFF435).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.StopMotion).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.StopMotion)) {
            uFlagTemp[MapleBuffStat.StopMotion.getPosition()] |= MapleBuffStat.StopMotion.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.StopMotion).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.StopMotion).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Fear).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Fear)) {
            uFlagTemp[MapleBuffStat.Fear.getPosition()] |= MapleBuffStat.Fear.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Fear).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Fear).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_137).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_137)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_137.getPosition()] |= MapleBuffStat.IDA_BUFF_137.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_137).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_137).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.MagicShield).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.MagicShield)) {
            uFlagTemp[MapleBuffStat.MagicShield.getPosition()] |= MapleBuffStat.MagicShield.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MagicShield).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.Flying).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Flying)) {
            uFlagTemp[MapleBuffStat.Flying.getPosition()] |= MapleBuffStat.Flying.getValue();
        }
        if (getStatOption(MapleBuffStat.Frozen).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Frozen)) {
            uFlagTemp[MapleBuffStat.Frozen.getPosition()] |= MapleBuffStat.Frozen.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Frozen).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Frozen).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Frozen2).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Frozen2)) {
            uFlagTemp[MapleBuffStat.Frozen2.getPosition()] |= MapleBuffStat.Frozen2.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Frozen2).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Frozen2).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Web).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Web)) {
            uFlagTemp[MapleBuffStat.Web.getPosition()] |= MapleBuffStat.Web.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Web).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Web).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.DrawBack).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DrawBack)) {
            uFlagTemp[MapleBuffStat.DrawBack.getPosition()] |= MapleBuffStat.DrawBack.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DrawBack).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DrawBack).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.FinalCut).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.FinalCut)) {
            uFlagTemp[MapleBuffStat.FinalCut.getPosition()] |= MapleBuffStat.FinalCut.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FinalCut).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FinalCut).rOption, 4));
        }
//        if (getStatOption(MapleBuffStat.Cyclone).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Cyclone)) {
//            uFlagTemp[MapleBuffStat.Cyclone.getPosition()] |= MapleBuffStat.Cyclone.getValue();
//            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Cyclone).nOption, 1));
//        }
        if (getStatOption(MapleBuffStat.OnCapsule).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.OnCapsule)) {
            uFlagTemp[MapleBuffStat.OnCapsule.getPosition()] |= MapleBuffStat.OnCapsule.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.OnCapsule).nOption, 1));
        }
        if (getStatOption(MapleBuffStat.Sneak).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Sneak)) {
            uFlagTemp[MapleBuffStat.Sneak.getPosition()] |= MapleBuffStat.Sneak.getValue();
        }
        if (getStatOption(MapleBuffStat.BeastFormDamageUp).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BeastFormDamageUp)) {
            uFlagTemp[MapleBuffStat.BeastFormDamageUp.getPosition()] |= MapleBuffStat.BeastFormDamageUp.getValue();
        }
        if (getStatOption(MapleBuffStat.Mechanic).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Mechanic)) {
            uFlagTemp[MapleBuffStat.Mechanic.getPosition()] |= MapleBuffStat.Mechanic.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Mechanic).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Mechanic).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.BlessingArmor).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BlessingArmor)) {
            uFlagTemp[MapleBuffStat.BlessingArmor.getPosition()] |= MapleBuffStat.BlessingArmor.getValue();
        }
        if (getStatOption(MapleBuffStat.BlessingArmorIncPAD).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BlessingArmorIncPAD)) {
            uFlagTemp[MapleBuffStat.BlessingArmorIncPAD.getPosition()] |= MapleBuffStat.BlessingArmorIncPAD.getValue();
        }
        if (getStatOption(MapleBuffStat.Inflation).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Inflation)) {
            uFlagTemp[MapleBuffStat.Inflation.getPosition()] |= MapleBuffStat.Inflation.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Inflation).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Inflation).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Explosion).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Explosion)) {
            uFlagTemp[MapleBuffStat.Explosion.getPosition()] |= MapleBuffStat.Explosion.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Explosion).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Explosion).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.DarkTornado).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DarkTornado)) {
            uFlagTemp[MapleBuffStat.DarkTornado.getPosition()] |= MapleBuffStat.DarkTornado.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DarkTornado).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DarkTornado).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.AmplifyDamage).rOption > 0
                || aDefaultFlags.contains(MapleBuffStat.AmplifyDamage)) {
            uFlagTemp[MapleBuffStat.AmplifyDamage.getPosition()] |= MapleBuffStat.AmplifyDamage.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AmplifyDamage).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AmplifyDamage).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HideAttack).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HideAttack)) {
            uFlagTemp[MapleBuffStat.HideAttack.getPosition()] |= MapleBuffStat.HideAttack.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HideAttack).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HideAttack).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HolyMagicShell).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HolyMagicShell)) {
            uFlagTemp[MapleBuffStat.HolyMagicShell.getPosition()] |= MapleBuffStat.HolyMagicShell.getValue();
        }
        if (getStatOption(MapleBuffStat.DevilishPower).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DevilishPower)) {
            uFlagTemp[MapleBuffStat.DevilishPower.getPosition()] |= MapleBuffStat.DevilishPower.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DevilishPower).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DevilishPower).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.SpiritLink).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.SpiritLink)) {
            uFlagTemp[MapleBuffStat.SpiritLink.getPosition()] |= MapleBuffStat.SpiritLink.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SpiritLink).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SpiritLink).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Event).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Event)) {
            uFlagTemp[MapleBuffStat.Event.getPosition()] |= MapleBuffStat.Event.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Event).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Event).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Event2).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Event2)) {
            uFlagTemp[MapleBuffStat.Event2.getPosition()] |= MapleBuffStat.Event2.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Event2).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Event2).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.DeathMark).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DeathMark)) {
            uFlagTemp[MapleBuffStat.DeathMark.getPosition()] |= MapleBuffStat.DeathMark.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DeathMark).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DeathMark).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.PainMark).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.PainMark)) {
            uFlagTemp[MapleBuffStat.PainMark.getPosition()] |= MapleBuffStat.PainMark.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PainMark).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PainMark).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Lapidification).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Lapidification)) {
            uFlagTemp[MapleBuffStat.Lapidification.getPosition()] |= MapleBuffStat.Lapidification.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Lapidification).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Lapidification).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.VampDeath).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.VampDeath)) {
            uFlagTemp[MapleBuffStat.VampDeath.getPosition()] |= MapleBuffStat.VampDeath.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.VampDeath).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.VampDeath).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.VampDeathSummon).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.VampDeathSummon)) {
            uFlagTemp[MapleBuffStat.VampDeathSummon.getPosition()] |= MapleBuffStat.VampDeathSummon.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.VampDeathSummon).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.VampDeathSummon).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.VenomSnake).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.VenomSnake)) {
            uFlagTemp[MapleBuffStat.VenomSnake.getPosition()] |= MapleBuffStat.VenomSnake.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.VenomSnake).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.VenomSnake).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.PyramidEffect).rOption > 0 | aDefaultFlags.contains(MapleBuffStat.PyramidEffect)) {
            uFlagTemp[MapleBuffStat.PyramidEffect.getPosition()] |= MapleBuffStat.PyramidEffect.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PyramidEffect).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.KillingPoint).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.KillingPoint)) {
            uFlagTemp[MapleBuffStat.KillingPoint.getPosition()] |= MapleBuffStat.KillingPoint.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KillingPoint).nOption, 1));
        }
        if (getStatOption(MapleBuffStat.PinkbeanRollingGrade).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.PinkbeanRollingGrade)) {
            uFlagTemp[MapleBuffStat.PinkbeanRollingGrade.getPosition()] |= MapleBuffStat.PinkbeanRollingGrade.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PinkbeanRollingGrade).nOption, 1));
        }
        if (getStatOption(MapleBuffStat.IgnoreTargetDEF).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IgnoreTargetDEF)) {
            uFlagTemp[MapleBuffStat.IgnoreTargetDEF.getPosition()] |= MapleBuffStat.IgnoreTargetDEF.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnoreTargetDEF).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnoreTargetDEF).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Invisible).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Invisible)) {
            uFlagTemp[MapleBuffStat.Invisible.getPosition()] |= MapleBuffStat.Invisible.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Invisible).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Invisible).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Judgement).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Judgement)) {
            uFlagTemp[MapleBuffStat.Judgement.getPosition()] |= MapleBuffStat.Judgement.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Judgement).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Judgement).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.KeyDownAreaMoving).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.KeyDownAreaMoving)) {
            uFlagTemp[MapleBuffStat.KeyDownAreaMoving.getPosition()] |= MapleBuffStat.KeyDownAreaMoving.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KeyDownAreaMoving).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KeyDownAreaMoving).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.StackBuff).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.StackBuff)) {
            uFlagTemp[MapleBuffStat.StackBuff.getPosition()] |= MapleBuffStat.StackBuff.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.StackBuff).nOption, 2));
        }
        if (getStatOption(MapleBuffStat.BlessOfDarkness).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BlessOfDarkness)) {
            uFlagTemp[MapleBuffStat.BlessOfDarkness.getPosition()] |= MapleBuffStat.BlessOfDarkness.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BlessOfDarkness).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.Larkness).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Larkness)) {
            uFlagTemp[MapleBuffStat.Larkness.getPosition()] |= MapleBuffStat.Larkness.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Larkness).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Larkness).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ReshuffleSwitch).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ReshuffleSwitch)) {
            uFlagTemp[MapleBuffStat.ReshuffleSwitch.getPosition()] |= MapleBuffStat.ReshuffleSwitch.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ReshuffleSwitch).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ReshuffleSwitch).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.SpecialAction).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.SpecialAction)) {
            uFlagTemp[MapleBuffStat.SpecialAction.getPosition()] |= MapleBuffStat.SpecialAction.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SpecialAction).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SpecialAction).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.StopForceAtomInfo).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.StopForceAtomInfo)) {
            uFlagTemp[MapleBuffStat.StopForceAtomInfo.getPosition()] |= MapleBuffStat.StopForceAtomInfo.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.StopForceAtomInfo).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.StopForceAtomInfo).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.SoulGazeCriDamR).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.SoulGazeCriDamR)) {
            uFlagTemp[MapleBuffStat.SoulGazeCriDamR.getPosition()] |= MapleBuffStat.SoulGazeCriDamR.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SoulGazeCriDamR).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SoulGazeCriDamR).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.PowerTransferGauge).rOption > 0 | aDefaultFlags.contains(MapleBuffStat.PowerTransferGauge)) {
            uFlagTemp[MapleBuffStat.PowerTransferGauge.getPosition()] |= MapleBuffStat.PowerTransferGauge.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PowerTransferGauge).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PowerTransferGauge).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_545).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_545)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_545.getPosition()] |= MapleBuffStat.IDA_BUFF_545.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_545).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_545).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.AffinitySlug).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.AffinitySlug)) {
            uFlagTemp[MapleBuffStat.AffinitySlug.getPosition()] |= MapleBuffStat.AffinitySlug.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AffinitySlug).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AffinitySlug).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.SoulExalt).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.SoulExalt)) {
            uFlagTemp[MapleBuffStat.SoulExalt.getPosition()] |= MapleBuffStat.SoulExalt.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SoulExalt).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SoulExalt).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HiddenPieceOn).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HiddenPieceOn)) {
            uFlagTemp[MapleBuffStat.HiddenPieceOn.getPosition()] |= MapleBuffStat.HiddenPieceOn.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HiddenPieceOn).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HiddenPieceOn).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.SmashStack).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.SmashStack)) {
            uFlagTemp[MapleBuffStat.SmashStack.getPosition()] |= MapleBuffStat.SmashStack.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SmashStack).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SmashStack).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.MobZoneState).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.MobZoneState)) {
            uFlagTemp[MapleBuffStat.MobZoneState.getPosition()] |= MapleBuffStat.MobZoneState.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MobZoneState).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MobZoneState).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.GiveMeHeal).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.GiveMeHeal)) {
            uFlagTemp[MapleBuffStat.GiveMeHeal.getPosition()] |= MapleBuffStat.GiveMeHeal.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.GiveMeHeal).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.GiveMeHeal).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.TouchMe).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.TouchMe)) {
            uFlagTemp[MapleBuffStat.TouchMe.getPosition()] |= MapleBuffStat.TouchMe.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.TouchMe).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.TouchMe).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Contagion).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Contagion)) {
            uFlagTemp[MapleBuffStat.Contagion.getPosition()] |= MapleBuffStat.Contagion.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Contagion).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Contagion).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Contagion).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Contagion)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Contagion).tOption, 4));
        }
        if (getStatOption(MapleBuffStat.ComboUnlimited).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ComboUnlimited)) {
            uFlagTemp[MapleBuffStat.ComboUnlimited.getPosition()] |= MapleBuffStat.ComboUnlimited.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ComboUnlimited).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ComboUnlimited).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IgnorePCounter).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IgnorePCounter)) {
            uFlagTemp[MapleBuffStat.IgnorePCounter.getPosition()] |= MapleBuffStat.IgnorePCounter.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnorePCounter).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnorePCounter).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IgnoreAllCounter).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IgnoreAllCounter)) {
            uFlagTemp[MapleBuffStat.IgnoreAllCounter.getPosition()] |= MapleBuffStat.IgnoreAllCounter.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnoreAllCounter).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnoreAllCounter).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IgnorePImmune).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IgnorePImmune)) {
            uFlagTemp[MapleBuffStat.IgnorePImmune.getPosition()] |= MapleBuffStat.IgnorePImmune.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnorePImmune).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnorePImmune).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IgnoreAllImmune).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IgnoreAllImmune)) {
            uFlagTemp[MapleBuffStat.IgnoreAllImmune.getPosition()] |= MapleBuffStat.IgnoreAllImmune.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnoreAllImmune).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnoreAllImmune).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.FinalJudgement).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.FinalJudgement)) {
            uFlagTemp[MapleBuffStat.FinalJudgement.getPosition()] |= MapleBuffStat.FinalJudgement.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FinalJudgement).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FinalJudgement).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_289).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_289)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_289.getPosition()] |= MapleBuffStat.IDA_BUFF_289.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_289).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_289).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.KnightsAura).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.KnightsAura)) {
            uFlagTemp[MapleBuffStat.KnightsAura.getPosition()] |= MapleBuffStat.KnightsAura.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KnightsAura).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KnightsAura).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IceAura).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IceAura)) {
            uFlagTemp[MapleBuffStat.IceAura.getPosition()] |= MapleBuffStat.IceAura.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IceAura).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IceAura).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.FireAura).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.FireAura)) {
            uFlagTemp[MapleBuffStat.FireAura.getPosition()] |= MapleBuffStat.FireAura.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FireAura).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FireAura).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.VengeanceOfAngel).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.VengeanceOfAngel)) {
            uFlagTemp[MapleBuffStat.VengeanceOfAngel.getPosition()] |= MapleBuffStat.VengeanceOfAngel.getValue();
        }
        if (getStatOption(MapleBuffStat.HeavensDoor).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HeavensDoor)) {
            uFlagTemp[MapleBuffStat.HeavensDoor.getPosition()] |= MapleBuffStat.HeavensDoor.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HeavensDoor).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HeavensDoor).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.DamAbsorbShield).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DamAbsorbShield)) {
            uFlagTemp[MapleBuffStat.DamAbsorbShield.getPosition()] |= MapleBuffStat.DamAbsorbShield.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DamAbsorbShield).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DamAbsorbShield).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.AntiMagicShell).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.AntiMagicShell)) {
            uFlagTemp[MapleBuffStat.AntiMagicShell.getPosition()] |= MapleBuffStat.AntiMagicShell.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AntiMagicShell).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AntiMagicShell).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.NotDamaged).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.NotDamaged)) {
            uFlagTemp[MapleBuffStat.NotDamaged.getPosition()] |= MapleBuffStat.NotDamaged.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.NotDamaged).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.NotDamaged).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.BleedingToxin).rOption > 0 | aDefaultFlags.contains(MapleBuffStat.BleedingToxin)) {
            uFlagTemp[MapleBuffStat.BleedingToxin.getPosition()] |= MapleBuffStat.BleedingToxin.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BleedingToxin).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BleedingToxin).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.WindBreakerFinal).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.WindBreakerFinal)) {
            uFlagTemp[MapleBuffStat.WindBreakerFinal.getPosition()] |= MapleBuffStat.WindBreakerFinal.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.WindBreakerFinal).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.WindBreakerFinal).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IgnoreMobDamR).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IgnoreMobDamR)) {
            uFlagTemp[MapleBuffStat.IgnoreMobDamR.getPosition()] |= MapleBuffStat.IgnoreMobDamR.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnoreMobDamR).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IgnoreMobDamR).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Asura).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Asura)) {
            uFlagTemp[MapleBuffStat.Asura.getPosition()] |= MapleBuffStat.Asura.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Asura).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Asura).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_301).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_301)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_301.getPosition()] |= MapleBuffStat.IDA_BUFF_301.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_301).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_301).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.UnityOfPower).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.UnityOfPower)) {
            uFlagTemp[MapleBuffStat.UnityOfPower.getPosition()] |= MapleBuffStat.UnityOfPower.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.UnityOfPower).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.UnityOfPower).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Stimulate).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Stimulate)) {
            uFlagTemp[MapleBuffStat.Stimulate.getPosition()] |= MapleBuffStat.Stimulate.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Stimulate).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Stimulate).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ReturnTeleport).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ReturnTeleport)) {
            uFlagTemp[MapleBuffStat.ReturnTeleport.getPosition()] |= MapleBuffStat.ReturnTeleport.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ReturnTeleport).nOption, 1));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ReturnTeleport).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.CapDebuff).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.CapDebuff)) {
            uFlagTemp[MapleBuffStat.CapDebuff.getPosition()] |= MapleBuffStat.CapDebuff.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.CapDebuff).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.CapDebuff).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.OverloadCount).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.OverloadCount)) {
            uFlagTemp[MapleBuffStat.OverloadCount.getPosition()] |= MapleBuffStat.OverloadCount.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.OverloadCount).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.OverloadCount).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.FireBomb).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.FireBomb)) {
            uFlagTemp[MapleBuffStat.FireBomb.getPosition()] |= MapleBuffStat.FireBomb.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FireBomb).nOption, 1));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FireBomb).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.SurplusSupply).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.SurplusSupply)) {
            uFlagTemp[MapleBuffStat.SurplusSupply.getPosition()] |= MapleBuffStat.SurplusSupply.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SurplusSupply).nOption, 1));
        }
        if (getStatOption(MapleBuffStat.NewFlying).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.NewFlying)) {
            uFlagTemp[MapleBuffStat.NewFlying.getPosition()] |= MapleBuffStat.NewFlying.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.NewFlying).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.NewFlying).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.NaviFlying).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.NaviFlying)) {
            uFlagTemp[MapleBuffStat.NaviFlying.getPosition()] |= MapleBuffStat.NaviFlying.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.NaviFlying).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.NaviFlying).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.AmaranthGenerator).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.AmaranthGenerator)) {
            uFlagTemp[MapleBuffStat.AmaranthGenerator.getPosition()] |= MapleBuffStat.AmaranthGenerator.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AmaranthGenerator).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AmaranthGenerator).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.CygnusElementSkill).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.CygnusElementSkill)) {
            uFlagTemp[MapleBuffStat.CygnusElementSkill.getPosition()] |= MapleBuffStat.CygnusElementSkill.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.CygnusElementSkill).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.CygnusElementSkill).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.StrikerHyperElectric).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.StrikerHyperElectric)) {
            uFlagTemp[MapleBuffStat.StrikerHyperElectric.getPosition()] |= MapleBuffStat.StrikerHyperElectric.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.StrikerHyperElectric).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.StrikerHyperElectric).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.EventPointAbsorb).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.EventPointAbsorb)) {
            uFlagTemp[MapleBuffStat.EventPointAbsorb.getPosition()] |= MapleBuffStat.EventPointAbsorb.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.EventPointAbsorb).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.EventPointAbsorb).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.EventAssemble).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.EventAssemble)) {
            uFlagTemp[MapleBuffStat.EventAssemble.getPosition()] |= MapleBuffStat.EventAssemble.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.EventAssemble).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.EventAssemble).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Albatross).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Albatross)) {
            uFlagTemp[MapleBuffStat.Albatross.getPosition()] |= MapleBuffStat.Albatross.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Albatross).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Albatross).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Translucence).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Translucence)) {
            uFlagTemp[MapleBuffStat.Translucence.getPosition()] |= MapleBuffStat.Translucence.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Translucence).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Translucence).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.PoseType).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.PoseType)) {
            uFlagTemp[MapleBuffStat.PoseType.getPosition()] |= MapleBuffStat.PoseType.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PoseType).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PoseType).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.LightOfSpirit).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.LightOfSpirit)) {
            uFlagTemp[MapleBuffStat.LightOfSpirit.getPosition()] |= MapleBuffStat.LightOfSpirit.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.LightOfSpirit).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.LightOfSpirit).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ElementSoul).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ElementSoul)) {
            uFlagTemp[MapleBuffStat.ElementSoul.getPosition()] |= MapleBuffStat.ElementSoul.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ElementSoul).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ElementSoul).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.GlimmeringTime).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.GlimmeringTime)) {
            uFlagTemp[MapleBuffStat.GlimmeringTime.getPosition()] |= MapleBuffStat.GlimmeringTime.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.GlimmeringTime).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.GlimmeringTime).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Reincarnation).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Reincarnation)) {
            uFlagTemp[MapleBuffStat.Reincarnation.getPosition()] |= MapleBuffStat.Reincarnation.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Reincarnation).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Reincarnation).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Beholder).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Beholder)) {
            uFlagTemp[MapleBuffStat.Beholder.getPosition()] |= MapleBuffStat.Beholder.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Beholder).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Beholder).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.QuiverCatridge).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.QuiverCatridge)) {
            uFlagTemp[MapleBuffStat.QuiverCatridge.getPosition()] |= MapleBuffStat.QuiverCatridge.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.QuiverCatridge).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.QuiverCatridge).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ArmorPiercing).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ArmorPiercing)) {
            uFlagTemp[MapleBuffStat.ArmorPiercing.getPosition()] |= MapleBuffStat.ArmorPiercing.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ArmorPiercing).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ArmorPiercing).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.UserControlMob).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.UserControlMob)) {
            uFlagTemp[MapleBuffStat.UserControlMob.getPosition()] |= MapleBuffStat.UserControlMob.getValue();
        }
        if (getStatOption(MapleBuffStat.ZeroAuraStr).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ZeroAuraStr)) {
            uFlagTemp[MapleBuffStat.ZeroAuraStr.getPosition()] |= MapleBuffStat.ZeroAuraStr.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ZeroAuraStr).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ZeroAuraStr).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ZeroAuraSpd).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ZeroAuraSpd)) {
            uFlagTemp[MapleBuffStat.ZeroAuraSpd.getPosition()] |= MapleBuffStat.ZeroAuraSpd.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ZeroAuraSpd).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ZeroAuraSpd).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ImmuneBarrier).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ImmuneBarrier)) {
            uFlagTemp[MapleBuffStat.ImmuneBarrier.getPosition()] |= MapleBuffStat.ImmuneBarrier.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ImmuneBarrier).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ImmuneBarrier).xOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_439).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_439)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_439.getPosition()] |= MapleBuffStat.IDA_BUFF_439.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_439).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_439).xOption, 4));
        }
        if (getStatOption(MapleBuffStat.AnimalChange).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.AnimalChange)) {
            uFlagTemp[MapleBuffStat.AnimalChange.getPosition()] |= MapleBuffStat.AnimalChange.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AnimalChange).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AnimalChange).xOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_441).rOption > 0 | aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_441)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_441.getPosition()] |= MapleBuffStat.IDA_BUFF_441.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_441).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_441).xOption, 4));
        }
        if (getStatOption(MapleBuffStat.Fever).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Fever)) {
            uFlagTemp[MapleBuffStat.Fever.getPosition()] |= MapleBuffStat.Fever.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Fever).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Fever).xOption, 4));
        }
        if (getStatOption(MapleBuffStat.AURA_BOOST).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.AURA_BOOST)) {
            uFlagTemp[MapleBuffStat.AURA_BOOST.getPosition()] |= MapleBuffStat.AURA_BOOST.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AURA_BOOST).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AURA_BOOST).xOption, 4));
        }
        if (getStatOption(MapleBuffStat.FullSoulMP).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.FullSoulMP)) {
            uFlagTemp[MapleBuffStat.FullSoulMP.getPosition()] |= MapleBuffStat.FullSoulMP.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FullSoulMP).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FullSoulMP).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FullSoulMP).xOption, 4));
        }
        if (getStatOption(MapleBuffStat.AntiMagicShell).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.AntiMagicShell)) {
            uFlagTemp[MapleBuffStat.AntiMagicShell.getPosition()] |= MapleBuffStat.AntiMagicShell.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AntiMagicShell).nOption, 1));
        }
        if (getStatOption(MapleBuffStat.Dance).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Dance)) {
            uFlagTemp[MapleBuffStat.Dance.getPosition()] |= MapleBuffStat.Dance.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Dance).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Dance).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.SpiritGuard).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.SpiritGuard)) {
            uFlagTemp[MapleBuffStat.SpiritGuard.getPosition()] |= MapleBuffStat.SpiritGuard.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SpiritGuard).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.SpiritGuard).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_446).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_446)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_446.getPosition()] |= MapleBuffStat.IDA_BUFF_446.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_446).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_446).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ComboTempest).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ComboTempest)) {
            uFlagTemp[MapleBuffStat.ComboTempest.getPosition()] |= MapleBuffStat.ComboTempest.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ComboTempest).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ComboTempest).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HalfstatByDebuff).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HalfstatByDebuff)) {
            uFlagTemp[MapleBuffStat.HalfstatByDebuff.getPosition()] |= MapleBuffStat.HalfstatByDebuff.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HalfstatByDebuff).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HalfstatByDebuff).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.ComplusionSlant).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ComplusionSlant)) {
            uFlagTemp[MapleBuffStat.ComplusionSlant.getPosition()] |= MapleBuffStat.ComplusionSlant.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ComplusionSlant).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ComplusionSlant).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.JaguarSummoned).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.JaguarSummoned)) {
            uFlagTemp[MapleBuffStat.JaguarSummoned.getPosition()] |= MapleBuffStat.JaguarSummoned.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.JaguarSummoned).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.JaguarSummoned).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.BMageAura).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BMageAura)) {
            uFlagTemp[MapleBuffStat.BMageAura.getPosition()] |= MapleBuffStat.BMageAura.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BMageAura).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BMageAura).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.元氣覺醒).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.元氣覺醒)) {
            uFlagTemp[MapleBuffStat.元氣覺醒.getPosition()] |= MapleBuffStat.元氣覺醒.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.元氣覺醒).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.元氣覺醒).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.能量爆炸).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.能量爆炸)) {
            uFlagTemp[MapleBuffStat.能量爆炸.getPosition()] |= MapleBuffStat.能量爆炸.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.能量爆炸).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.能量爆炸).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_508).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_508)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_508.getPosition()] |= MapleBuffStat.IDA_BUFF_508.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_508).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_508).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_509).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_509)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_509.getPosition()] |= MapleBuffStat.IDA_BUFF_509.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_509).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_509).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_510).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_510)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_510.getPosition()] |= MapleBuffStat.IDA_BUFF_510.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_510).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_510).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.交換攻擊).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.交換攻擊)) {
            uFlagTemp[MapleBuffStat.交換攻擊.getPosition()] |= MapleBuffStat.交換攻擊.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.交換攻擊).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.交換攻擊).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.聖靈祈禱).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.聖靈祈禱)) {
            uFlagTemp[MapleBuffStat.聖靈祈禱.getPosition()] |= MapleBuffStat.聖靈祈禱.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.聖靈祈禱).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.聖靈祈禱).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.DarkLighting).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.DarkLighting)) {
            uFlagTemp[MapleBuffStat.DarkLighting.getPosition()] |= MapleBuffStat.DarkLighting.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DarkLighting).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.DarkLighting).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.AttackCountX).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.AttackCountX)) {
            uFlagTemp[MapleBuffStat.AttackCountX.getPosition()] |= MapleBuffStat.AttackCountX.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AttackCountX).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AttackCountX).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.FireBarrier).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.FireBarrier)) {
            uFlagTemp[MapleBuffStat.FireBarrier.getPosition()] |= MapleBuffStat.FireBarrier.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FireBarrier).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.FireBarrier).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.KeyDownMoving).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.KeyDownMoving)) {
            uFlagTemp[MapleBuffStat.KeyDownMoving.getPosition()] |= MapleBuffStat.KeyDownMoving.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KeyDownMoving).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KeyDownMoving).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.MichaelSoulLink).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.MichaelSoulLink)) {
            uFlagTemp[MapleBuffStat.MichaelSoulLink.getPosition()] |= MapleBuffStat.MichaelSoulLink.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MichaelSoulLink).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MichaelSoulLink).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.KinesisPsychicEnergeShield).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.KinesisPsychicEnergeShield)) {
            uFlagTemp[MapleBuffStat.KinesisPsychicEnergeShield.getPosition()] |= MapleBuffStat.KinesisPsychicEnergeShield.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KinesisPsychicEnergeShield).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KinesisPsychicEnergeShield).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.BladeStance).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BladeStance)) {
            uFlagTemp[MapleBuffStat.BladeStance.getPosition()] |= MapleBuffStat.BladeStance.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BladeStance).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BladeStance).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BladeStance).xOption, 4));
        }
        if (getStatOption(MapleBuffStat.Fever).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Fever)) {
            uFlagTemp[MapleBuffStat.Fever.getPosition()] |= MapleBuffStat.Fever.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Fever).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Fever).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.AdrenalinBoost).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.AdrenalinBoost)) {
            uFlagTemp[MapleBuffStat.AdrenalinBoost.getPosition()] |= MapleBuffStat.AdrenalinBoost.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AdrenalinBoost).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.RWBarrierHeal).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.RWBarrierHeal)) {
            uFlagTemp[MapleBuffStat.RWBarrierHeal.getPosition()] |= MapleBuffStat.RWBarrierHeal.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.RWBarrierHeal).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.RWMagnumBlow).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.RWMagnumBlow)) {
            uFlagTemp[MapleBuffStat.RWMagnumBlow.getPosition()] |= MapleBuffStat.RWMagnumBlow.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.RWMagnumBlow).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.RWBarrier).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.RWBarrier)) {
            uFlagTemp[MapleBuffStat.RWBarrier.getPosition()] |= MapleBuffStat.RWBarrier.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.RWBarrier).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_251).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_251)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_251.getPosition()] |= MapleBuffStat.IDA_BUFF_251.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_251).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_251).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_252).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_252)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_252.getPosition()] |= MapleBuffStat.IDA_BUFF_252.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_252).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_252).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_253).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_253)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_253.getPosition()] |= MapleBuffStat.IDA_BUFF_253.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_253).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_253).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_254).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_254)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_254.getPosition()] |= MapleBuffStat.IDA_BUFF_254.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_254).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_254).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Stigma).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Stigma)) {
            uFlagTemp[MapleBuffStat.Stigma.getPosition()] |= MapleBuffStat.Stigma.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Stigma).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Stigma).rOption, 4));
        }

        if (getStatOption(MapleBuffStat.IDA_BUFF_417).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_417)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_417.getPosition()] |= MapleBuffStat.IDA_BUFF_417.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_417).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_417).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_518).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_518)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_518.getPosition()] |= MapleBuffStat.IDA_BUFF_518.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_519).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_519)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_519.getPosition()] |= MapleBuffStat.IDA_BUFF_519.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_519).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_501).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_501)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_501.getPosition()] |= MapleBuffStat.IDA_BUFF_501.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_501).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.散式投擲).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.散式投擲)) {
            uFlagTemp[MapleBuffStat.散式投擲.getPosition()] |= MapleBuffStat.散式投擲.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.散式投擲).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.IncMonsterBattleCaptureRate).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IncMonsterBattleCaptureRate)) {
            uFlagTemp[MapleBuffStat.IncMonsterBattleCaptureRate.getPosition()] |= MapleBuffStat.IncMonsterBattleCaptureRate.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IncMonsterBattleCaptureRate).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IncMonsterBattleCaptureRate).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_550).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_550)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_550.getPosition()] |= MapleBuffStat.IDA_BUFF_550.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_550).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_550).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_549).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_549)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_549.getPosition()] |= MapleBuffStat.IDA_BUFF_549.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_549).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_549).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_548).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_548)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_548.getPosition()] |= MapleBuffStat.IDA_BUFF_548.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_548).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_548).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_547).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_547)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_547.getPosition()] |= MapleBuffStat.IDA_BUFF_547.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_547).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_547).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_84).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_84)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_84.getPosition()] |= MapleBuffStat.IDA_BUFF_84.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_84).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_84).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_555).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_555)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_555.getPosition()] |= MapleBuffStat.IDA_BUFF_555.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_555).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_555).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_556).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_556)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_556.getPosition()] |= MapleBuffStat.IDA_BUFF_556.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_556).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_556).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_557).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_557)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_557.getPosition()] |= MapleBuffStat.IDA_BUFF_557.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_557).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_557).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_558).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_558)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_558.getPosition()] |= MapleBuffStat.IDA_BUFF_558.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_558).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_558).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_559).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_559)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_559.getPosition()] |= MapleBuffStat.IDA_BUFF_559.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_559).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_559).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_553).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_553)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_553.getPosition()] |= MapleBuffStat.IDA_BUFF_553.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_553).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_553).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_560).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_560)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_560.getPosition()] |= MapleBuffStat.IDA_BUFF_560.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_560).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_560).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_560).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_560)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_560.getPosition()] |= MapleBuffStat.IDA_BUFF_560.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_560).tOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_561).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_561)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_561.getPosition()] |= MapleBuffStat.IDA_BUFF_561.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_561).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_561).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_562).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_562)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_562.getPosition()] |= MapleBuffStat.IDA_BUFF_562.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_562).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_562).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_562).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_562)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_562.getPosition()] |= MapleBuffStat.IDA_BUFF_562.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_562).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_562).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_561).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_561)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_561.getPosition()] |= MapleBuffStat.IDA_BUFF_561.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_561).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HayatoStance).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HayatoStance)) {
            uFlagTemp[MapleBuffStat.HayatoStance.getPosition()] |= MapleBuffStat.HayatoStance.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoStance).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoStance).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HayatoStance).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HayatoStance)) {
            uFlagTemp[MapleBuffStat.HayatoStance.getPosition()] |= MapleBuffStat.HayatoStance.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoStance).tOption, 4));
        }

        if (getStatOption(MapleBuffStat.BATTOUJUTSU_STANCE).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BATTOUJUTSU_STANCE)) {
            uFlagTemp[MapleBuffStat.BATTOUJUTSU_STANCE.getPosition()] |= MapleBuffStat.BATTOUJUTSU_STANCE.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BATTOUJUTSU_STANCE).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BATTOUJUTSU_STANCE).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HayatoStanceBonus).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HayatoStanceBonus)) {
            uFlagTemp[MapleBuffStat.HayatoStanceBonus.getPosition()] |= MapleBuffStat.HayatoStanceBonus.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoStanceBonus).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoStanceBonus).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HayatoPAD).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HayatoPAD)) {
            uFlagTemp[MapleBuffStat.HayatoPAD.getPosition()] |= MapleBuffStat.HayatoPAD.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoPAD).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoPAD).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HayatoHPR).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HayatoHPR)) {
            uFlagTemp[MapleBuffStat.HayatoHPR.getPosition()] |= MapleBuffStat.HayatoHPR.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoHPR).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoHPR).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HayatoMPR).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HayatoMPR)) {
            uFlagTemp[MapleBuffStat.HayatoMPR.getPosition()] |= MapleBuffStat.HayatoMPR.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoMPR).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoMPR).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.HayatoCr).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.HayatoCr)) {
            uFlagTemp[MapleBuffStat.HayatoCr.getPosition()] |= MapleBuffStat.HayatoCr.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoCr).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.HayatoCr).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.FireBarrier).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.FireBarrier)) {
            uFlagTemp[MapleBuffStat.FireBarrier.getPosition()] |= MapleBuffStat.FireBarrier.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KannaBDR).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KannaBDR).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.KannaBDR).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.KannaBDR)) {
            uFlagTemp[MapleBuffStat.KannaBDR.getPosition()] |= MapleBuffStat.KannaBDR.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KannaBDR).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.KannaBDR).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.Stance).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Stance)) {
            uFlagTemp[MapleBuffStat.Stance.getPosition()] |= MapleBuffStat.Stance.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Stance).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Stance).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_529).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_529)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_529.getPosition()] |= MapleBuffStat.IDA_BUFF_529.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_529).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_529).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_436).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_436)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_436.getPosition()] |= MapleBuffStat.IDA_BUFF_436.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_436).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_436).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.BLACKHEART_CURSE).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BLACKHEART_CURSE)) {
            uFlagTemp[MapleBuffStat.BLACKHEART_CURSE.getPosition()] |= MapleBuffStat.BLACKHEART_CURSE.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BLACKHEART_CURSE).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BLACKHEART_CURSE).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.COUNTERATTACK).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.COUNTERATTACK)) {
            uFlagTemp[MapleBuffStat.COUNTERATTACK.getPosition()] |= MapleBuffStat.COUNTERATTACK.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.COUNTERATTACK).nOption, 2));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.COUNTERATTACK).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_158).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_158)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_158.getPosition()] |= MapleBuffStat.IDA_BUFF_158.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_158).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_158).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_531).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_531)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_531.getPosition()] |= MapleBuffStat.IDA_BUFF_531.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_531).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_531).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_532).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_532)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_532.getPosition()] |= MapleBuffStat.IDA_BUFF_532.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_532).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_533).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_533)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_533.getPosition()] |= MapleBuffStat.IDA_BUFF_533.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_533).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_535).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_535)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_535.getPosition()] |= MapleBuffStat.IDA_BUFF_535.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_535).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_536).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_536)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_536.getPosition()] |= MapleBuffStat.IDA_BUFF_536.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_536).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_537).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_537)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_537.getPosition()] |= MapleBuffStat.IDA_BUFF_537.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_537).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_537).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_539).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_539)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_539.getPosition()] |= MapleBuffStat.IDA_BUFF_539.getValue();
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_539).nOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_418).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_418)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_418.getPosition()] |= MapleBuffStat.IDA_BUFF_418.getValue();
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_419).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_419)) {
            uFlagTemp[MapleBuffStat.IDA_BUFF_419.getPosition()] |= MapleBuffStat.IDA_BUFF_419.getValue();
        }

        uFlagTemp[MapleBuffStat.IndieStatCount.getPosition()] |= MapleBuffStat.IndieStatCount.getValue();
        uFlagTemp[MapleBuffStat.EnergyCharged.getPosition()] |= MapleBuffStat.EnergyCharged.getValue();
        uFlagTemp[MapleBuffStat.DashSpeed.getPosition()] |= MapleBuffStat.DashSpeed.getValue();
        uFlagTemp[MapleBuffStat.DashJump.getPosition()] |= MapleBuffStat.DashJump.getValue();
        uFlagTemp[MapleBuffStat.RideVehicle.getPosition()] |= MapleBuffStat.RideVehicle.getValue();
        uFlagTemp[MapleBuffStat.PartyBooster.getPosition()] |= MapleBuffStat.PartyBooster.getValue();
        uFlagTemp[MapleBuffStat.GuidedBullet.getPosition()] |= MapleBuffStat.GuidedBullet.getValue();
        uFlagTemp[MapleBuffStat.Undead.getPosition()] |= MapleBuffStat.Undead.getValue();
        uFlagTemp[MapleBuffStat.RideVehicleExpire.getPosition()] |= MapleBuffStat.RideVehicleExpire.getValue();
        uFlagTemp[MapleBuffStat.COUNT_PLUS1.getPosition()] |= MapleBuffStat.COUNT_PLUS1.getValue();
        for (int i = 0; i < uFlagTemp.length; i++) {
            mplew.writeInt(uFlagTemp[i]);
        }
        for (Pair<Integer, Integer> nStats : uFlagData) {
            if (null != nStats.getRight()) {
                switch (nStats.getRight()) {
                    case 4:
                        mplew.writeInt(nStats.getLeft());
                        break;
                    case 2:
                        mplew.writeShort(nStats.getLeft());
                        break;
                    case 1:
                        mplew.write(nStats.getLeft());
                        break;
                    default:
                        break;
                }
            }
        }

        uFlagData.clear();

        mplew.write(this.nDefenseAtt);
        mplew.write(this.nDefenseState);
        mplew.write(this.nPVPDamage);
        mplew.writeInt(this.nViperCharge);
        mplew.writeInt(0);

        if (getStatOption(MapleBuffStat.PoseType).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.PoseType)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.PoseType).nOption, 1));
        }
        if (getStatOption(MapleBuffStat.ZeroAuraStr).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ZeroAuraStr)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ZeroAuraStr).bOption, 1));
        }
        if (getStatOption(MapleBuffStat.ZeroAuraSpd).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.ZeroAuraSpd)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.ZeroAuraSpd).bOption, 1));
        }
        if (getStatOption(MapleBuffStat.BMageAura).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BMageAura)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BMageAura).bOption, 1));
        }
        if (getStatOption(MapleBuffStat.BattlePvP_Helena_Mark).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BattlePvP_Helena_Mark)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BattlePvP_Helena_Mark).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BattlePvP_Helena_Mark).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BattlePvP_Helena_Mark).cOption, 4));
        }
        if (getStatOption(MapleBuffStat.BattlePvP_LangE_Protection).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.BattlePvP_LangE_Protection)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BattlePvP_LangE_Protection).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.BattlePvP_LangE_Protection).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.MichaelSoulLink).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.MichaelSoulLink)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MichaelSoulLink).xOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MichaelSoulLink).bOption, 1));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MichaelSoulLink).cOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.MichaelSoulLink).yOption, 4));
        }
        if (getStatOption(MapleBuffStat.AdrenalinBoost).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.AdrenalinBoost)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.AdrenalinBoost).cOption, 1));
        }
        if (getStatOption(MapleBuffStat.Stigma).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.Stigma)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.Stigma).bOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_417).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_417)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_417).nOption, 2));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_418).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_418)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_418).nOption, 2));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_419).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_419)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_419).nOption, 2));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_518).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_518)) { // Unsure of options
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).xOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).yOption, 4));
        }
        if (getStatOption(MapleBuffStat.VampDeath).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.VampDeath)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.VampDeath).xOption, 4));
        }
        // 553
        if (getStatOption(MapleBuffStat.IDA_BUFF_518).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_518)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).rOption, 4));
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_253).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_253)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_253).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_253).rOption, 4));
        }
        // 567
        if (getStatOption(MapleBuffStat.IDA_BUFF_518).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_518)) {
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).nOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).rOption, 4));
            uFlagData.add(new Pair<>(getStatOption(MapleBuffStat.IDA_BUFF_518).xOption, 4));
        }
        for (Pair<Integer, Integer> nStats : uFlagData) {
            if (null != nStats.getRight()) {
                switch (nStats.getRight()) {
                    case 4:
                        mplew.writeInt(nStats.getLeft());
                        break;
                    case 2:
                        mplew.writeShort(nStats.getLeft());
                        break;
                    case 1:
                        mplew.write(nStats.getLeft());
                        break;
                    default:
                        break;
                }
            }
        }
        pStopForceAtom.Encode(mplew);
        for (TSIndex pIndex : TSIndex.values()) {
            this.getStatOption(TSIndex.get_CTS_from_TSIndex(pIndex.getIndex())).encode(mplew);
        }

        if (getStatOption(MapleBuffStat.IndieStatCount).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IndieStatCount)) {
            this.encodeIndieTempStat(mplew, MapleBuffStat.IndieStatCount);
            // CWvsContext.BuffPacket.IndieTempStat_Decode(mplew,
            // chr.getBuffStatValueHolders(MapleBuffStat.IDA_BUFF_54), effect == null ? 0 :
            // effect.getSourceId());
        }
        if (getStatOption(MapleBuffStat.IDA_BUFF_550).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.IDA_BUFF_550)) {
            mplew.writeInt(getStatOption(MapleBuffStat.IDA_BUFF_550).xOption);
        }
        if (getStatOption(MapleBuffStat.KeyDownMoving).rOption > 0 | aDefaultFlags.contains(MapleBuffStat.KeyDownMoving)) {
            mplew.writeInt(getStatOption(MapleBuffStat.KeyDownMoving).xOption);
        }
        if (getStatOption(MapleBuffStat.NewFlying).rOption > 0 || aDefaultFlags.contains(MapleBuffStat.NewFlying)) {
            mplew.writeInt(getStatOption(MapleBuffStat.NewFlying).xOption);
        }

    }

    public void encodeIndieTempStat(MaplePacketLittleEndianWriter mplew, MapleBuffStat buffStat) {
        this.getIndieStatOption(buffStat).encode(mplew);
    }

    public Map<MapleBuffStat, TemporaryStatBase> getStats() {
        return this.stats;
    }

    public void updateStatOption(MapleBuffStat buffstat, MapleCharacter chr, MapleStatEffect effect, int rOption,
            int nOption, long tLastUpdated, int usExpireTerm) {
        TemporaryStatBase tsb = this.getStatOption(buffstat);
        tsb.nOption = nOption;
        tsb.rOption = effect.isSkill() ? rOption : -rOption;
        tsb.yOption = effect.getY();
        tsb.sOption = tsb.nOption > 0 ? 1 : 0;
        tsb.cOption = chr.getId();
        tsb.xOption = effect.getLevel();
        tsb.isSkill = effect.isSkill();
        tsb.tLastUpdated = tLastUpdated;
        tsb.usExpireTerm = usExpireTerm;
    }

    public void updateIndieStatOption(MapleBuffStat buffstat, MapleCharacter chr, MapleStatEffect effect, int nOption,
            int rOption, int tLastUpdate, int usExpireTerm) {
        IndieTempStat its = this.getIndieStatOption(buffstat);
        IndieTempStatElem itsm = its.getMElem().get(rOption);
        if (itsm == null) {
            itsm = new IndieTempStatElem();
        }
        itsm.nKey = rOption;
        itsm.nValue = nOption;
        itsm.tStart = tLastUpdate;
        itsm.tTerm = usExpireTerm;
        itsm.isSkill = effect.isSkill();
        its.getMElem().put(rOption, itsm);
    }

    public IndieTempStat getIndieStatOption(MapleBuffStat buffstat) {
        IndieTempStat ret = null;
        if (buffstat.isIndie()) {
            ret = this.aIndieTempStat.get(buffstat);
            if (ret == null) {
                ret = new IndieTempStat();
                this.aIndieTempStat.put(buffstat, ret);
            }
        }
        return ret;
    }

    public TemporaryStatBase getStatOption(MapleBuffStat buffstat) {
        TemporaryStatBase ret;
        if (TSIndex.is_valid_TSIndex(buffstat)) {
            ret = this.aTemporaryStat.get(TSIndex.get_TSIndex_from_CTS(buffstat));
        } else {
            if (this.stats.get(buffstat) == null) {
                this.stats.put(buffstat, new TwoStateTemporaryStat(true));
            }
            ret = this.stats.get(buffstat);
        }
        if (buffstat == MapleBuffStat.PyramidEffect) {
            this.stats.get(buffstat).nOption = -1;
        }
        return ret;
    }

}
