/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs;

import server.MapleStatEffect;
import server.buffs.buffclasses.adventurer.BowmanBuff;
import server.buffs.buffclasses.adventurer.MagicianBuff;
import server.buffs.buffclasses.adventurer.PirateBuff;
import server.buffs.buffclasses.adventurer.ThiefBuff;
import server.buffs.buffclasses.adventurer.WarriorBuff;
import server.buffs.buffclasses.adventurer.ChivalrousBuff;
import server.buffs.buffclasses.cygnus.DawnWarriorBuff;
import server.buffs.buffclasses.cygnus.ThunderBreakerBuff;
import server.buffs.buffclasses.cygnus.MihileBuff;
import server.buffs.buffclasses.cygnus.WindArcherBuff;
import server.buffs.buffclasses.hero.AranBuff;
import server.buffs.buffclasses.hero.EvanBuff;
import server.buffs.buffclasses.hero.MercedesBuff;
import server.buffs.buffclasses.hero.PhantomBuff;
import server.buffs.buffclasses.hero.LuminousBuff;
import server.buffs.buffclasses.nova.KaiserBuff;
import server.buffs.buffclasses.nova.AngelicBusterBuff;
import server.buffs.buffclasses.resistance.DemonBuff;
import server.buffs.buffclasses.resistance.BattleMageBuff;
import server.buffs.buffclasses.resistance.WildHunterBuff;
import server.buffs.buffclasses.resistance.MechanicBuff;
import server.buffs.buffclasses.resistance.XenonBuff;
import server.buffs.buffclasses.sengoku.HayatoBuff;
import server.buffs.buffclasses.sengoku.KannaBuff;
import server.buffs.buffclasses.zero.ZeroBuff;
import server.buffs.buffclasses.beasttamer.BeastTamerBuff;
import server.buffs.buffclasses.cygnus.BlazeWizardBuff;
import server.buffs.buffclasses.cygnus.NightWalkerBuff;
import server.buffs.buffclasses.gamemaster.GameMasterBuff;
import server.buffs.buffclasses.hero.EunWolBuff;
import server.buffs.buffclasses.kinesis.KinesisBufff;
import server.buffs.buffclasses.pinkbean.PinkBeanBuff;
import server.buffs.buffclasses.others.Skill800003;
import server.buffs.buffclasses.adventurer.BeginnerBuff;
import server.buffs.buffclasses.others.Skill800011;
import server.buffs.buffclasses.vskills.VSkillBowmanBuff;
import server.buffs.buffclasses.vskills.VSkillCommonBuff;
import server.buffs.buffclasses.vskills.VSkillMagicianBuff;
import server.buffs.buffclasses.vskills.VSkillPirateBuff;
import server.buffs.buffclasses.vskills.VSkillThiefBuff;
import server.buffs.buffclasses.vskills.VSkillWarriorBuff;

/**
 *
 * @author Saint
 */
public class BuffClassFetcher {

    public static final Class<?>[] buffClasses = {BeginnerBuff.class, WarriorBuff.class, MagicianBuff.class,
        BowmanBuff.class, ThiefBuff.class, PirateBuff.class, ChivalrousBuff.class, GameMasterBuff.class,
        DawnWarriorBuff.class, BlazeWizardBuff.class, NightWalkerBuff.class, WindArcherBuff.class,
        ThunderBreakerBuff.class, AranBuff.class, EvanBuff.class, MercedesBuff.class, PhantomBuff.class,
        LuminousBuff.class, EunWolBuff.class, DemonBuff.class, BattleMageBuff.class, WildHunterBuff.class,
        MechanicBuff.class, XenonBuff.class, HayatoBuff.class, KannaBuff.class, MihileBuff.class, KaiserBuff.class,
        AngelicBusterBuff.class, ZeroBuff.class, BeastTamerBuff.class, PinkBeanBuff.class, KinesisBufff.class,
        Skill800003.class, Skill800011.class, VSkillCommonBuff.class, VSkillWarriorBuff.class,
        VSkillMagicianBuff.class, VSkillBowmanBuff.class, VSkillThiefBuff.class, VSkillPirateBuff.class};

    public static boolean getHandleMethod(MapleStatEffect eff, int skillid) {
        int jobid = skillid / 10000;
        for (Class<?> c : buffClasses) {
            try {
                if (!AbstractBuffClass.class.isAssignableFrom(c)) {
                    continue;
                }
                AbstractBuffClass cls = (AbstractBuffClass) c.getDeclaredConstructor().newInstance();
                if (cls.containsJob(jobid)) {
                    if (!cls.containsSkill(skillid)) {
                        continue;
                    }
                    cls.handleBuff(eff, skillid);
                    return true;
                }
            } catch (InstantiationException | IllegalAccessException ex) {
                System.err.println("Error: handleBuff method was not found in " + c.getSimpleName() + ".class");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public static void load(boolean reload) {
        for (Class<?> c : buffClasses) {

        }
    }
}
