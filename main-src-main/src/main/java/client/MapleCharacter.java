package client;

import client.MapleTrait.MapleTraitType;
import client.anticheat.CheatTracker;
import client.anticheat.ReportType;
import client.character.stat.*;
import client.familiar.MonsterFamiliar;
import client.forceatoms.ForceAtom;
import client.forceatoms.ForceAtomInfo;
import client.inventory.*;
import client.inventory.MapleImp.ImpFlag;
import client.inventory.VMatrixRecord.VCoreRecordState;
import client.skill.Skill;
import client.skill.SkillEntry;
import client.skill.SkillFactory;
import client.skill.SkillMacro;
import constants.*;
import database.DatabaseException;
import database.ManagerDatabasePool;
import extensions.temporary.UserEffectOpcode;
import handling.channel.ChannelServer;
import handling.login.LoginInformationProvider.JobType;
import handling.login.LoginServer;
import handling.world.*;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import scripting.EventInstanceManager;
import scripting.EventManager;
import scripting.NPCScriptManager;
import server.*;
import server.Timer.MapTimer;
import server.Timer.WorldTimer;
import server.commands.PlayerGMRank;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.PlayerNPC;
import server.maps.*;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.MapleShop;
import server.shops.MapleShopFactory;
import server.shops.MapleShopItem;
import server.stores.IMaplePlayerShop;
import server.subsystem.bingo.MapleBingoRecord;
import tools.*;
import tools.packet.*;
import tools.packet.CField.CScriptMan;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.SummonPacket;
import tools.packet.CWvsContext.*;
import tools.packet.JobPacket.AvengerPacket;
import tools.packet.JobPacket.LuminousPacket;
import tools.packet.JobPacket.PhantomPacket;
import tools.packet.JobPacket.XenonPacket;

import java.awt.*;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MapleCharacter extends AnimatedMapleMapObject implements Serializable, MapleCharacterLook {

    private static final long serialVersionUID = 845748950829L;
    private String name, chalktext, BlessOfFairy_Origin, BlessOfEmpress_Origin, teleportname;
    private long lastCombo, lastfametime, nextConsume, pqStartTime, lastDragonBloodTime, lastBerserkTime,
            lastRecoveryTime, lastSummonTime, mapChangeTime, lastFishingTime, lastFairyTime, lastHPTime, lastMPTime,
            lastFamiliarEffectTime, lastDOTTime, lastRecoveryXenonSurplusTime, exp, meso;
    private byte gmLevel, gender, initialSpawnPoint, skinColor, guildrank = 5, allianceRank = 5, world, fairyExp,
            numClones, subcategory, cardStack, runningBless = 0;
    private short level, job, mulung_energy, combo, showCombo, availableCP, fatigue, totalCP, hpApUsed, mpApUsed,
            scrolledPosition, attackCombo, soulcount = 0;
    private int accountid, id, hair, face, secondHair, secondFace, faceMarking, ears, tail, mapid, fame, pvpExp,
            pvpPoints, guildid = 0, fallcounter, maplepoints, mileage, beanfunGash, chair, itemEffect, rank = 1,
            rankMove = 0, worldRank = 1, worldRankMove = 0, marriageId, marriageItemId, dotHP, pvpTeam, followid,
            battleshipHP, gachexp, challenge, guildContribution = 0, remainingAp, honourExp, honorLevel, runningLight,
            runningLightSlot, runningDark, runningDarkSlot, luminousState, weaponPoint;
    private byte lastWorld = -3;
    private Point old;
    private MonsterFamiliar summonedFamiliar;
    private int[] wishlist = new int[12], rocks, savedLocations, regrocks, hyperrocks, remainingSp = new int[10],
            remainingHSp = new int[2];
    private transient AtomicInteger inst, insd;
    private transient List<LifeMovementFragment> lastres;
    private List<Integer> lastmonthfameids, lastmonthbattleids, extendedSlots;
    private List<MapleDoor> doors;
    private List<MapleOpenGate> mechDoors;
    private List<MaplePet> pets;
    private List<Item> rebuy;
    private MapleShop azwanShopList;
    private MapleImp[] imps;
    private List<Pair<Integer, Boolean>> stolenSkills = new ArrayList<>();
    private transient List<WeakReference<MapleCharacter>> clones;
    private transient Set<MapleMonster> controlled;
    private Map<Integer, String> entered = new LinkedHashMap<>();
    private transient Set<MapleMapObject> visibleMapObjects;
    private transient ReentrantReadWriteLock visibleMapObjectsLock;
    private transient ReentrantReadWriteLock summonsLock;
    private transient ReentrantReadWriteLock controlledLock;
    private transient MapleAndroid android;
    private final Map<MapleQuest, MapleQuestStatus> quests;
    private Map<Integer, String> questinfo;
    private final Map<Skill, SkillEntry> skills;
    private transient Map<MapleStatEffect, MapleBuffStatValueHolder> effects;
    private final Map<String, String> CustomValues = new HashMap<>();
    private final Map<String, String> TempValues = new HashMap<>();
    private transient List<MapleSummon> summons;
    private transient Map<Integer, MapleCoolDownValueHolder> coolDowns;
    private transient Map<MapleDisease, MapleDiseaseValueHolder> diseases;
    private Map<ReportType, Integer> reports;
    private CashShop cs;
    private transient Deque<MapleCarnivalChallenge> pendingCarnivalRequests;
    private transient MapleCarnivalParty carnivalParty;
    private BuddyList buddylist;
    private MonsterBook monsterbook;
    private transient CheatTracker anticheat;
    private MapleClient client;
    private transient MapleParty party;
    private PlayerStats stats;
    private final MapleCharacterCards characterCard;
    private transient MapleMap map;
    private transient MapleShop shop;
    private transient MapleDragon dragon;
    private transient MapleHaku haku;
    private transient MapleExtractor extractor;
    private transient RockPaperScissors rps;
    private Map<Integer, MonsterFamiliar> familiars;
    private MapleStorage storage;
    private transient MapleTrade trade;
    private MapleMount mount;
    private int sp;
    private MapleMessenger messenger;
    private byte[] petStore;
    private transient IMaplePlayerShop playerShop;
    private boolean invincible, canTalk, followinitiator, followon, smega, hasSummon;
    private MapleGuildCharacter mgc;
    private transient EventInstanceManager eventInstance;
    private final List<MapleCharacter> chars = new LinkedList<>(); // this is messy
    private final ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();
    private final Lock rL = mutex.readLock(), wL = mutex.writeLock();
    private transient EventManager eventInstanceAzwan;
    private MapleInventory[] inventory;
    private SkillMacro[] skillMacros = new SkillMacro[5];
    private final Map<MapleTraitType, MapleTrait> traits;
    private MapleKeyLayout keylayout;
    private transient ScheduledFuture<?> mapTimeLimitTask;
    private transient Event_PyramidSubway pyramidSubway = null;
    private transient List<Integer> pendingExpiration = null;
    private transient Map<Skill, SkillEntry> pendingSkills = null;
    private transient Map<Integer, Integer> linkMobs;
    private InnerSkillValueHolder[] innerSkills;
    public boolean innerskill_changed = true;
    private boolean changed_wishlist, changed_trocklocations, changed_regrocklocations, changed_hyperrocklocations,
            changed_skillmacros, changed_savedlocations, changed_questinfo, changed_skills, changed_reports,
            changed_extendedSlots, update_skillswipe;
    private Map<Integer, List<Pair<Integer, Integer>>> salon = new HashMap<>();

    private int str;
    private int luk;
    private int int_;
    private int dex;
    private short chattype = 0;
    private int[] friendshippoints = new int[5];
    private int friendshiptoadd;
    private int wheelItem = 0;
    private MapleCoreAura coreAura;
    private List<MaplePotionPot> potionPots;
    private MapleMarriage marriage;
    private boolean hiddenChatCanSee = false;
    private int comboKill;
    private long lastComboKill;
    private long lastLevelup;
    public List<Pair<Integer, Boolean>> comboKillExps = new ArrayList<>();
    private long lastCheckProcess;
    private final List<MapleProcess> Process = new LinkedList<>();
    private boolean skipOnceChat = true;
    private int killCount = 0;
    private int mod = 0;
    private long stifftime = 0;
    private long lastAttackTime = 0;
    private int forceCounter = 0;
    public int qcount;
    public int 卡圖 = 0;
    public int lastskill = 0;
    private transient CalcDamage calcDamage;
    public int PPoint = 0, dualBrid = 0, acaneAim = 0;
    private List<MapleSummon> linksummon = new ArrayList<>();
    private MapleCharacter cloneOwner;
    private SecondaryStat secondaryStat;
    private ForceAtomInfo forceAtomInfo;
    private List<VMatrixRecord> aVMatrixRecords = new ArrayList<>();
    private int nVMatrixShard = 0;

    private MapleBingoRecord bingoRecord = new MapleBingoRecord();

    private MapleCharacter(final boolean ChannelServer) {
        super.setStance(0);
        super.setPosition(new Point(0, 0));

        inventory = new MapleInventory[MapleInventoryType.values().length];
        for (MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type);
        }
        quests = new LinkedHashMap<>(); // Stupid erev quest.
        questinfo = new LinkedHashMap<>();
        skills = new LinkedHashMap<>(); // Stupid UAs.
        stats = new PlayerStats();
        innerSkills = new InnerSkillValueHolder[3];
        azwanShopList = null;
        characterCard = new MapleCharacterCards();
        for (int i = 0; i < remainingSp.length; i++) {
            remainingSp[i] = 0;
        }
        for (int i = 0; i < remainingHSp.length; i++) {
            remainingHSp[i] = 0;
        }
        traits = new EnumMap<>(MapleTraitType.class);
        for (MapleTraitType t : MapleTraitType.values()) {
            traits.put(t, new MapleTrait(t));
        }
        if (ChannelServer) {
            changed_reports = false;
            changed_skills = false;
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_regrocklocations = false;
            changed_hyperrocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_extendedSlots = false;
            changed_questinfo = false;
            update_skillswipe = false;
            scrolledPosition = 0;
            lastCombo = 0;
            mulung_energy = 0;
            combo = 0;
            showCombo = 0;
            comboKill = 0;
            lastComboKill = 0;
            lastLevelup = 0;
            nextConsume = 0;
            pqStartTime = 0;
            fairyExp = 0;
            cardStack = 0;
            mapChangeTime = 0;
            lastRecoveryTime = 0;
            lastDragonBloodTime = 0;
            lastBerserkTime = 0;
            lastFishingTime = 0;
            lastFairyTime = 0;
            lastHPTime = 0;
            lastMPTime = 0;
            lastFamiliarEffectTime = 0;
            old = new Point(0, 0);
            pvpTeam = 0;
            followid = 0;
            battleshipHP = 0;
            marriageItemId = 0;
            marriage = null;
            fallcounter = 0;
            challenge = 0;
            dotHP = 0;
            lastSummonTime = 0;
            hasSummon = false;
            invincible = false;
            canTalk = true;
            cloneOwner = null;
            followinitiator = false;
            followon = false;
            rebuy = new ArrayList<>();
            linkMobs = new HashMap<>();
            reports = new EnumMap<>(ReportType.class);
            teleportname = "";
            smega = true;
            petStore = new byte[3];
            for (int i = 0; i < petStore.length; i++) {
                petStore[i] = (byte) -1;
            }
            wishlist = new int[30];
            rocks = new int[10];
            regrocks = new int[5];
            hyperrocks = new int[13];
            imps = new MapleImp[3];
            clones = new LinkedList<>();
            for (int i = 0; i < 5; i++) {
                clones.add(new WeakReference<>(null));
            }
//            clones = new WeakReference<MapleCharacter>[5]; // for now
//            for (int i = 0; i < clones.length; i++) {
//                clones[i] = new WeakReference<>(null);
//            }
            familiars = new TreeMap<>();
            extendedSlots = new ArrayList<>();
            effects = new HashMap<>();
            coolDowns = new LinkedHashMap<>();
            secondaryStat = new SecondaryStat();
            forceAtomInfo = new ForceAtomInfo();
            diseases = new ConcurrentEnumMap<>(MapleDisease.class);
            inst = new AtomicInteger(0);// 1 = NPC/ Quest, 2 = Donald, 3 = Hired Merch store, 4 = Storage
            insd = new AtomicInteger(-1);
            keylayout = new MapleKeyLayout();
            doors = new ArrayList<>();
            mechDoors = new ArrayList<>();
            controlled = new LinkedHashSet<>();
            controlledLock = new ReentrantReadWriteLock();
            summons = new LinkedList<>();
            summonsLock = new ReentrantReadWriteLock();
            visibleMapObjects = new LinkedHashSet<>();
            visibleMapObjectsLock = new ReentrantReadWriteLock();
            pendingCarnivalRequests = new LinkedList<>();

            savedLocations = new int[SavedLocationType.values().length];
            for (int i = 0; i < SavedLocationType.values().length; i++) {
                savedLocations[i] = -1;
            }
            pets = new ArrayList<>();
            friendshippoints = new int[5];
            coreAura = new MapleCoreAura(id, 24 * 60);
            potionPots = new ArrayList<>();
        }
    }

    public static MapleCharacter getDefault(final MapleClient client, final JobType type) {
        MapleCharacter ret = new MapleCharacter(false);
        ret.client = client;
        ret.map = null;
        ret.exp = 0;
        ret.gmLevel = (byte) client.getGmLevel();
        if (ServerConstants.IS_BETA_FOR_ADMINS) {
            PlayerGMRank rank = PlayerGMRank.ADMIN;
            if (ret.getGmLevel() != rank.getLevel()) {
                ret.setGmLevel(rank);
            }
            if (client.getGmLevel() != rank.getLevel()) {
                client.setGmLevel(rank);
            }
        }
        ret.job = (short) type.id;
        ret.meso = 0;
        ret.level = 1;
        ret.remainingAp = 0;
        ret.fame = 0;
        ret.accountid = client.getAccID();
        ret.buddylist = new BuddyList((byte) 50);

        ret.stats.力量 = 12;
        ret.stats.敏捷 = 5;
        ret.stats.智力 = 4;
        ret.stats.幸運 = 4;
        ret.stats.maxhp = 50;
        ret.stats.hp = 50;
        ret.stats.maxmp = 5;
        ret.stats.mp = 5;
        ret.gachexp = 0;
        ret.friendshippoints = new int[]{0, 0, 0, 0, 0};
        ret.friendshiptoadd = 0;

        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?")) {
                ps.setInt(1, ret.accountid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ret.client.setAccountName(rs.getString("name"));
                        ret.beanfunGash = rs.getInt("BeanfunGash");
                        ret.maplepoints = rs.getInt("mPoints");
                        ret.mileage = rs.getInt("Mileage");
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("Error getting character default" + e);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
        }

        int[][] guidebooks = new int[][]{{4161001, 0}, {4161047, 1}, {4161048, 2000}, {4161052, 2001},
        {4161054, 3}, {4161079, 2002}};
        int guidebook = 0;
        for (int[] i : guidebooks) {
            if (ret.getJob() == i[1]) {
                guidebook = i[0];
            } else if (ret.getJob() / 1000 == i[1]) {
                guidebook = i[0];
            }
        }
        if (guidebook > 0) {
            ret.getInventory(MapleInventoryType.ETC).addItem(new Item(guidebook, (byte) 0, (short) 1, 0));
        }

        final Map<Skill, SkillEntry> ss = new HashMap<>();

        if (MapleJob.is幻影俠盜(ret.getJob())) {
            ret.getStat().maxhp = 140;
            ret.getStat().hp = 140;
            ret.getStat().maxmp = 38;
            ret.getStat().mp = 38;
        }

        if (MapleJob.is天使破壞者(ret.getJob())) {
            ret.setQuestAdd(MapleQuest.getInstance(25835), (byte) 2, ""); // 任務：愛斯卡達的真面目
            ret.setQuestAdd(MapleQuest.getInstance(25829), (byte) 2, ""); // 任務：這個技能是什麼？
        }

        if (MapleJob.is神之子(ret.getJob())) {
            ret.setLevel((short) 100);
            ret.getStat().力量 = 518;
            ret.getStat().敏捷 = 4;
            ret.getStat().智力 = 4;
            ret.getStat().幸運 = 4;
            ret.getStat().maxhp = 6910;
            ret.getStat().hp = 6910;
            ret.getStat().maxmp = 100;
            ret.getStat().mp = 100;
            ret.setRemainingSp(3, 0); // alpha
            ret.setRemainingSp(3, 1); // beta
        }

        if (MapleJob.is幻獸師(ret.getJob())) {
            ret.job = (short) MapleJob.幻獸師4轉.getId();
        }

        if (MapleJob.is凱內西斯(ret.getJob())) {
            ret.job = (short) MapleJob.凱內西斯1轉.getId();
            ret.level = 10;
            ret.getStat().maxhp = 374;
            ret.getStat().hp = 374;
            ret.getStat().maxmp = 10;
            ret.getStat().mp = 10;
            ret.getStat().力量 = 4;
            ret.getStat().敏捷 = 4;
            ret.getStat().智力 = 52;
            ret.getStat().幸運 = 4;
        }

        if (!ss.isEmpty()) {
            ret.changeSkillLevel_Skip(ss, false);
        }

        return ret;
    }

    public void ReconstructChr(final MapleCharacter player, final MapleClient client) {
        player.setClient(client);
        client.setTempIP(player.getOneTempValue("Transfer", "TempIP"));
        client.setAccountName(player.getOneTempValue("Transfer", "AccountName"));

        final MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
        player.map = mapFactory.getMap(player.map.getId());
        if (player.map == null) { // char is on a map that doesn't exist warp it to spinel forest
            player.map = mapFactory.getMap(950000100);
        } else if (player.map.getForcedReturnId() != 999999999 && player.map.getForcedReturnMap() != null) {
            player.map = player.map.getForcedReturnMap();
            if (player.map.getForcedReturnId() == 4000000) {
                player.initialSpawnPoint = 0;
            }
        }
        MaplePortal portal = player.map.getPortal(player.initialSpawnPoint);
        if (portal == null) {
            portal = player.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first
            // spawnpoint instead
            player.initialSpawnPoint = 0;
        }
        player.setPosition(portal.getPosition());
        // final int messengerid = ct.messengerid;
        // if (messengerid > 0) {
        // player.messenger = World.Messenger.getMessenger(messengerid);
        // }
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) {
        return loadCharFromDB(charid, client, channelserver, null);
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver,
            final Map<Integer, CardData> cads) {
        final MapleCharacter ret = new MapleCharacter(channelserver);
        ret.client = client;
        ret.id = charid;

        ResultSet rs = null;

        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?")) {
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    rs.close();
                    ps.close();
                    throw new RuntimeException("加載角色失敗(未找到角色)");
                }
                ret.name = rs.getString("name");
                ret.level = rs.getShort("level");
                ret.fame = rs.getInt("fame");

                ret.stats.力量 = rs.getShort("str");
                ret.stats.敏捷 = rs.getShort("dex");
                ret.stats.智力 = rs.getShort("int");
                ret.stats.幸運 = rs.getShort("luk");
                ret.stats.maxhp = rs.getInt("maxhp");
                ret.stats.maxmp = rs.getInt("maxmp");
                ret.stats.hp = rs.getInt("hp");
                ret.stats.mp = rs.getInt("mp");
                ret.job = rs.getShort("job");
                ret.gmLevel = rs.getByte("gm");
                ret.exp = ret.level >= ret.maxLevel ? 0 : rs.getLong("exp");
                ret.hpApUsed = rs.getShort("hpApUsed");
                ret.mpApUsed = rs.getShort("mpApUsed");
                String[] sp = rs.getString("sp").split(",");
                for (int i = 0; i < ret.remainingSp.length; i++) {
                    ret.remainingSp[i] = Integer.parseInt(sp[i]);
                }
                String[] hsp = rs.getString("hsp").split(",");
                for (int i = 0; i < ret.remainingHSp.length; i++) {
                    if (ret.remainingHSp.length > 2 && i == 0) {
                        continue;
                    }
                    ret.remainingHSp[i] = Integer.parseInt(hsp[i]);
                }
                ret.remainingAp = rs.getShort("ap");
                ret.meso = rs.getLong("meso");
                ret.skinColor = rs.getByte("skincolor");
                ret.gender = rs.getByte("gender");

                ret.hair = rs.getInt("hair");
                ret.face = rs.getInt("face");
                ret.faceMarking = rs.getInt("faceMarking");
                ret.ears = rs.getInt("ears");
                ret.tail = rs.getInt("tail");
                ret.accountid = rs.getInt("accountid");
                client.setAccID(ret.accountid);
                ret.weaponPoint = rs.getInt("weaponPoint");
                ret.mapid = rs.getInt("map");
                ret.initialSpawnPoint = rs.getByte("spawnpoint");
                ret.world = rs.getByte("world");
                ret.guildid = rs.getInt("guildid");
                ret.guildrank = rs.getByte("guildrank");
                ret.allianceRank = rs.getByte("allianceRank");
                ret.guildContribution = rs.getInt("guildContribution");
                if (ret.guildid > 0) {
                    ret.mgc = new MapleGuildCharacter(ret);
                }
                ret.gachexp = rs.getInt("gachexp");
                ret.buddylist = new BuddyList(rs.getByte("buddyCapacity"));
                ret.honourExp = rs.getInt("honourExp");
                ret.honorLevel = rs.getInt("honourLevel");
                ret.subcategory = rs.getByte("subcategory");
                ret.mount = new MapleMount(ret, 0, PlayerStats.getSkillByJob(1004, ret.job), (byte) 0, (byte) 1, 0);
                ret.rank = rs.getInt("rank");
                ret.rankMove = rs.getInt("rankMove");
                ret.worldRank = rs.getInt("worldRank");
                ret.worldRankMove = rs.getInt("worldRankMove");
                ret.marriageId = rs.getInt("marriageId");
                ret.fatigue = rs.getShort("fatigue");
                ret.pvpExp = rs.getInt("pvpExp");
                ret.pvpPoints = rs.getInt("pvpPoints");
                ret.friendshiptoadd = rs.getInt("friendshiptoadd");

                List<Pair<Integer, Integer>> salon_hair = new LinkedList<>();
                int salon_hairSize = Math.max(rs.getInt("salon_hair"), 3);
                for (int i = 0; i < salon_hairSize; i++) {
                    salon_hair.add(new Pair<>(0, 0));
                }
                ret.salon.put(30000, salon_hair);

                List<Pair<Integer, Integer>> salon_face = new LinkedList<>();
                int salon_faceSize = Math.max(rs.getInt("salon_face"), 3);
                for (int i = 0; i < salon_faceSize; i++) {
                    salon_face.add(new Pair<>(0, 0));
                }
                ret.salon.put(20000, salon_face);

                ret.chattype = rs.getShort("chatcolour");
                for (MapleTrait t : ret.traits.values()) {
                    t.setExp(rs.getInt(t.getType().name()));
                }
                if (channelserver) {
                    ret.calcDamage = new CalcDamage();
                    ret.anticheat = new CheatTracker(ret);
                    MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
                    ret.map = mapFactory.getMap(ret.mapid);
                    if (ret.map == null) { // char is on a map that doesn't exist warp it to spinel forest
                        ret.map = mapFactory.getMap(950000100);
                    }
                    MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                    if (portal == null) {
                        portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first
                        // spawnpoint instead
                        ret.initialSpawnPoint = 0;
                    }
                    ret.setPosition(portal.getPosition());

                    int partyid = rs.getInt("party");
                    if (partyid >= 0) {
                        MapleParty party = World.Party.getParty(partyid);
                        if (party != null && party.getMemberById(ret.id) != null) {
                            ret.party = party;
                        }
                    }
                    String[] pets = rs.getString("pets").split(",");
                    for (int i = 0; i < ret.petStore.length; i++) {
                        ret.petStore[i] = Byte.parseByte(pets[i]);
                    }
                    String[] friendshippoints = rs.getString("friendshippoints").split(",");
                    for (int i = 0; i < 5; i++) {
                        ret.friendshippoints[i] = Integer.parseInt(friendshippoints[i]);
                    }
                    rs.close();
                    ps.close();

                    try (PreparedStatement ps1 = con
                            .prepareStatement("SELECT * FROM salon WHERE characterid = ? ORDER BY position")) {
                        ps1.setInt(1, charid);
                        rs = ps1.executeQuery();
                        while (rs.next()) {
                            int type = rs.getInt("type");
                            if (ret.salon.containsKey(type)) {
                                int pos = rs.getInt("position");
                                ret.salon.get(type).remove(pos);
                                ret.salon.get(type).add(pos, new Pair<>(rs.getInt("itemId"), rs.getInt("gender")));
                            }
                        }
                        rs.close();
                    }

                    try (PreparedStatement ps2 = con.prepareStatement("SELECT * FROM reports WHERE characterid = ?")) {
                        ps2.setInt(1, charid);
                        rs = ps2.executeQuery();
                        while (rs.next()) {
                            if (ReportType.getById(rs.getByte("type")) != null) {
                                ret.reports.put(ReportType.getById(rs.getByte("type")), rs.getInt("count"));
                            }
                        }
                        rs.close();
                    }
                }
                rs.close();
                ps.close();
            }

            if (ret.marriageId > 0) {
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?")) {
                    ps.setInt(1, ret.marriageId);
                    rs = ps.executeQuery();
                    int partnerId = rs.getInt("id");
                    ret.marriage = new MapleMarriage(partnerId, ret.marriageItemId);
                    ret.marriage.setHusbandId(ret.gender == 0 ? ret.id : partnerId);
                    ret.marriage.setWifeId(ret.gender == 1 ? ret.id : partnerId);
                    String partnerName = rs.getString("name");
                    ret.marriage.setHusbandName(ret.gender == 0 ? ret.name : partnerName);
                    ret.marriage.setWifeName(ret.gender == 1 ? ret.name : partnerName);
                    /*
                     * if (rs.next()) { ret.marriage = new MapleMarriage(rs.getInt("id"),
                     * rs.getInt("ring")); ret.marriage.setHusbandId(rs.getInt("husbandId"));
                     * ret.marriage.setWifeId(rs.getInt("husbandId"));
                     * ret.marriage.setHusbandName(rs.getString("husbandName"));
                     * ret.marriage.setWifeName(rs.getString("husbandName")); } else { ret.marriage
                     * = null; }
                     */
                    rs.close();
                }
            }

            if (cads != null) { // so that we load only once.
                ret.characterCard.setCards(cads);
            } else { // load
                ret.characterCard.loadCards(client, channelserver);
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM questinfo WHERE characterid = ?")) {
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                while (rs.next()) {
                    ret.questinfo.put(rs.getInt("quest"), rs.getString("customData"));
                }
                rs.close();
            }

            try (PreparedStatement ps = con
                    .prepareStatement("SELECT * FROM charactercustomdata WHERE characterid = ?")) {
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                while (rs.next()) {
                    ret.CustomValues.put(rs.getString("key"), rs.getString("data"));
                }
                rs.close();
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?")) {
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                try (PreparedStatement pse = con
                        .prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?")) {

                    while (rs.next()) {
                        final int id = rs.getInt("quest");
                        final MapleQuest q = MapleQuest.getInstance(id);
                        final byte stat = rs.getByte("status");
                        if ((stat == 1 || stat == 2) && channelserver && (q == null || q.isBlocked())) { // bigbang
                            continue;
                        }
                        // if (stat == 1 && channelserver && !q.canStart(ret, 0)) { //bigbang //
                        // continue;
                        // }
                        final MapleQuestStatus status = new MapleQuestStatus(q, stat);
                        final long cTime = rs.getLong("time");
                        if (cTime > -1) {
                            status.setCompletionTime(cTime * 1000);
                        }
                        status.setForfeited(rs.getInt("forfeited"));
                        status.setCustomData(rs.getString("customData"));
                        ret.quests.put(q, status);
                        pse.setInt(1, rs.getInt("queststatusid"));
                        try (ResultSet rsMobs = pse.executeQuery()) {
                            while (rsMobs.next()) {
                                status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                            }
                        }
                    }
                }
                rs.close();
            }
            if (channelserver) {
                ret.monsterbook = MonsterBook.loadCards(ret.accountid, ret);
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM inventoryslot where characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        System.out.println("No Inventory slot column found in SQL. [inventoryslot]");
                        return null;
                    } else {
                        ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(rs.getShort("equip"));
                        ret.getInventory(MapleInventoryType.USE).setSlotLimit(rs.getShort("use"));
                        ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(rs.getShort("setup"));
                        ret.getInventory(MapleInventoryType.ETC).setSlotLimit(rs.getShort("etc"));
                        ret.getInventory(MapleInventoryType.CASH).setSlotLimit(rs.getShort("cash"));
                    }
                    rs.close();
                }
                for (Pair<Item, MapleInventoryType> mit : ItemLoader.INVENTORY.loadItems(false, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                    if (mit.getLeft().getPet() != null) {
                        ret.pets.add(mit.getLeft().getPet());
                    }
                }

                /*
                 * ps = con.prepareStatement("SELECT * FROM potionpots WHERE cid = ?");
                 * ps.setInt(1, ret.id); rs = ps.executeQuery(); ret.potionPots = new
                 * ArrayList(); while (rs.next()) { MaplePotionPot pot =
                 * MaplePotionPot.loadFromResult(rs); if (pot != null) {
                 * ret.potionPots.add(pot); } } rs.close(); ps.close();
                 */
                try {
                    PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    ps.setInt(1, ret.accountid);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        ret.getClient().setAccountName(rs.getString("name"));
                        ret.getClient().setGmLevel(PlayerGMRank.getByLevel(rs.getInt("gm")));
                        ret.beanfunGash = rs.getInt("BeanfunGash");
                        ret.maplepoints = rs.getInt("mPoints");
                        ret.mileage = rs.getInt("Mileage");

                        if (rs.getTimestamp("lastlogon") != null) {
                            final Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(rs.getTimestamp("lastlogon").getTime());
                        }
                        if (rs.getInt("banned") > 0) {
                            rs.close();
                            ps.close();
                            if (ret.getClient().getSendCrypto() != null) {
                                ret.getClient().getSession().close();
                                System.err
                                        .println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                            }
                            System.out.println("Loading a banned character");
                            return null;
                        }
                        rs.close();
                        ps.close();

                        ps = con.prepareStatement("UPDATE accounts SET lastlogon = CURRENT_TIMESTAMP() WHERE id = ?");
                        ps.setInt(1, ret.accountid);
                        ps.executeUpdate();
                    } else {
                        rs.close();
                    }
                    ps.close();
                } catch (SQLException ex) {
                    System.out.println("Loading a banned character");
                    return null;
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT skillid, skilllevel, masterlevel, expiration FROM skills WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    Skill skil;
                    while (rs.next()) {
                        final int skid = rs.getInt("skillid");
                        skil = SkillFactory.getSkill(skid);
                        int skl = rs.getInt("skilllevel");
                        byte msl = rs.getByte("masterlevel");
                        if (skil != null && skil.isBeginnerSkill() && skl == 0 && msl > 0) {
                            ret.changed_skills = true;
                        } else if (skil != null && SkillConstants.isApplicableSkill(skid)) {
                            if (skl > skil.getMaxLevel() && (skid < 92000000 || skid > 99999999)) {
                                if (!skil.isBeginnerSkill() && skil.canBeLearnedBy(ret.job) && !skil.isSpecialSkill()
                                        && skil.getFixLevel() == 0) {
                                    ret.remainingSp[GameConstants.getSkillBookBySkill(skid)] += (skl
                                            - skil.getMaxLevel());
                                }
                                skl = (byte) skil.getMaxLevel();
                                ret.changed_skills = true;
                            }
                            if (msl > skil.getMaxLevel()) {
                                msl = (byte) skil.getMaxLevel();
                                ret.changed_skills = true;
                            } else if (msl < skl && skil.getMasterLevel() > 0) {
                                if (skl < 10) {
                                    msl = (byte) skil.getMasterLevel();
                                } else if (skl < 20) {
                                    msl = (byte) Math.min(skil.getMaxLevel(), 20);
                                } else {
                                    msl = (byte) Math.min(skil.getMaxLevel(), 30);
                                }
                                ret.changed_skills = true;
                            }
                            ret.skills.put(skil, new SkillEntry(skl, msl, rs.getLong("expiration")));
                        } else if (skil == null) { // doesnt. exist. e.g. bb
                            if (!MapleJob.isBeginner(skid / 10000) && skid / 10000 != 900 && skid / 10000 != 800
                                    && skid / 10000 != 9000) {
                                ret.remainingSp[GameConstants.getSkillBookBySkill(skid)] += skl;
                                ret.changed_skills = true;
                            }
                        }
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM vcores WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        int skill1 = rs.getInt("connectSkill1");
                        int skill2 = rs.getInt("connectSkill2");
                        int skill3 = rs.getInt("connectSkill3");
                        int coreId = rs.getInt("coreId");
                        byte coreLevel = rs.getByte("coreLevel");
                        int coreExp = rs.getInt("coreExp");
                        int coreState = rs.getByte("coreState");
                        FileTime expireTime = new FileTime(rs.getLong("expireDate"));
                        VMatrixRecord record = new VMatrixRecord();
                        record.setExp(coreExp);
                        record.setSkillID1(skill1);
                        record.setSkillID2(skill2);
                        record.setSkillID3(skill3);
                        record.setIconID(coreId);
                        record.setSkillLv(coreLevel);
                        record.setState(coreState);
                        ret.getVMatrixRecords().add(record);
                    }
                }

                ret.expirationTask(false, true); // do it now

                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM coreauras WHERE cid = ?")) {
                    ps.setInt(1, ret.id);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        ret.coreAura = new MapleCoreAura(ret.id, rs.getInt("expire"));
                        ret.coreAura.setStr(rs.getInt("str"));
                        ret.coreAura.setDex(rs.getInt("dex"));
                        ret.coreAura.setInt(rs.getInt("int"));
                        ret.coreAura.setLuk(rs.getInt("luk"));
                        ret.coreAura.setAtt(rs.getInt("att"));
                        ret.coreAura.setMagic(rs.getInt("magic"));
                        ret.coreAura.setTotal(rs.getInt("total"));
                    } else {
                        ret.coreAura = new MapleCoreAura(ret.id, 24 * 60);
                    }
                    rs.close();
                }

                // Bless of Fairy handling
                try (PreparedStatement ps = con
                        .prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY level DESC")) {
                    ps.setInt(1, ret.accountid);
                    rs = ps.executeQuery();
                    int maxlevel_ = 0, maxlevel_2 = 0;
                    while (rs.next()) {
                        if (rs.getInt("id") != charid) { // Not this character
                            if (MapleJob.is皇家騎士團(rs.getShort("job"))) {
                                int maxlevel = (rs.getShort("level") / 5);

                                if (maxlevel > 24) {
                                    maxlevel = 24;
                                }
                                if (maxlevel > maxlevel_2 || maxlevel_2 == 0) {
                                    maxlevel_2 = maxlevel;
                                    ret.BlessOfEmpress_Origin = rs.getString("name");
                                }
                            }
                            int maxlevel = (rs.getShort("level") / 10);

                            if (maxlevel > 20) {
                                maxlevel = 20;
                            }
                            if (maxlevel > maxlevel_ || maxlevel_ == 0) {
                                maxlevel_ = maxlevel;
                                ret.BlessOfFairy_Origin = rs.getString("name");
                            }

                        }
                    }
                    /*
                     * if (!compensate_previousSP) { for (Entry<Skill, SkillEntry> skill :
                     * ret.skills.entrySet()) { if (!skill.getKey().isBeginnerSkill() &&
                     * !skill.getKey().isSpecialSkill()) {
                     * ret.remainingSp[GameConstants.getSkillBookForSkill(skill.getKey().getId())]
                     * += skill.getValue().skillevel; skill.getValue().skillevel = 0; } }
                     * ret.setQuestAdd(MapleQuest.getInstance(170000), (byte) 0, null); //set it so
                     * never again }
                     */
                    if (ret.BlessOfFairy_Origin == null) {
                        ret.BlessOfFairy_Origin = ret.name;
                    }
                    ret.skills.put(SkillFactory.getSkill(GameConstants.getBOF_ForJob(ret.job)),
                            new SkillEntry(maxlevel_, (byte) 0, -1));
                    if (SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)) != null) {
                        if (ret.BlessOfEmpress_Origin == null) {
                            ret.BlessOfEmpress_Origin = ret.BlessOfFairy_Origin;
                        }
                        ret.skills.put(SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)),
                                new SkillEntry(maxlevel_2, (byte) 0, -1));
                    }
                    rs.close();
                }
                // END

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT skill_id, skill_level, position, rank FROM inner_ability_skills WHERE player_id = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        int skid = rs.getInt("skill_id");
                        Skill mskill = SkillFactory.getSkill(skid);
                        int skl = rs.getInt("skill_level");
                        byte position = rs.getByte("position");
                        byte rank = rs.getByte("rank");
                        if ((mskill != null) && mskill.isInnerSkill() && position >= 1 && position <= 3) {
                            if (skl > mskill.getMaxLevel()) {
                                skl = (byte) mskill.getMaxLevel();
                            }
                            InnerSkillValueHolder InnerSkill = new InnerSkillValueHolder(skid, skl, position, rank);
                            ret.innerSkills[position - 1] = InnerSkill;
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    int position;
                    while (rs.next()) {
                        position = rs.getInt("position");
                        SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"),
                                rs.getString("name"), rs.getInt("shout"), position);
                        ret.skillMacros[position] = macro;
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM familiars WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        ret.familiars.put(rs.getInt("id"),
                                new MonsterFamiliar(rs.getInt("id"), rs.getInt("familiar"), charid,
                                        rs.getString("name"), rs.getByte("grade"), rs.getByte("level"),
                                        rs.getInt("exp"), rs.getShort("skillid"), rs.getInt("option1"),
                                        rs.getInt("option2"), rs.getInt("option3")));
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con
                        .prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();

                    final Map<Integer, Pair<Byte, Integer>> keyb = ret.keylayout.Layout();
                    while (rs.next()) {
                        keyb.put(rs.getInt("key"), new Pair<>(rs.getByte("type"), rs.getInt("action")));
                    }
                    rs.close();
                    ret.keylayout.unchanged();
                }

                try (PreparedStatement ps = con
                        .prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        ret.savedLocations[rs.getInt("locationtype")] = rs.getInt("map");
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    ret.lastfametime = 0;
                    ret.lastmonthfameids = new ArrayList<>(31);
                    while (rs.next()) {
                        ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                        ret.lastmonthfameids.add(rs.getInt("characterid_to"));
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT `accid_to`,`when` FROM battlelog WHERE accid = ? AND DATEDIFF(NOW(),`when`) < 30")) {
                    ps.setInt(1, ret.accountid);
                    rs = ps.executeQuery();
                    ret.lastmonthbattleids = new ArrayList<>();
                    while (rs.next()) {
                        ret.lastmonthbattleids.add(rs.getInt("accid_to"));
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con
                        .prepareStatement("SELECT `itemId` FROM extendedSlots WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        ret.extendedSlots.add(rs.getInt("itemId"));
                    }
                    rs.close();
                }

                ret.buddylist.loadFromDb(charid);
                ret.storage = MapleStorage.loadStorage(ret.accountid);
                ret.cs = new CashShop(ret.accountid, charid, ret.getJob());

                try (PreparedStatement ps = con.prepareStatement("SELECT sn FROM wishlist WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    int i = 0;
                    while (rs.next()) {
                        ret.wishlist[i] = rs.getInt("sn");
                        i++;
                    }
                    while (i < 30) {
                        ret.wishlist[i] = 0;
                        i++;
                    }
                    rs.close();
                }

                // trocklocations
                int r = 0;
                try (PreparedStatement ps = con
                        .prepareStatement("SELECT mapid FROM trocklocations WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();

                    while (rs.next()) {
                        ret.rocks[r] = rs.getInt("mapid");
                        r++;
                    }
                    while (r < 10) {
                        ret.rocks[r] = 999999999;
                        r++;
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con
                        .prepareStatement("SELECT mapid FROM regrocklocations WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    r = 0;
                    while (rs.next()) {
                        ret.regrocks[r] = rs.getInt("mapid");
                        r++;
                    }
                    while (r < 5) {
                        ret.regrocks[r] = 999999999;
                        r++;
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con
                        .prepareStatement("SELECT mapid FROM hyperrocklocations WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    r = 0;
                    while (rs.next()) {
                        ret.hyperrocks[r] = rs.getInt("mapid");
                        r++;
                    }
                    while (r < 13) {
                        ret.hyperrocks[r] = 999999999;
                        r++;
                    }
                    rs.close();
                }
                // end

                try (PreparedStatement ps = con.prepareStatement("SELECT * from stolen WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        ret.stolenSkills.add(new Pair<>(rs.getInt("skillid"), rs.getInt("chosen") > 0));
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM bingorecords WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    r = 0;
                    while (rs.next()) {
                        String answersStr = rs.getString("answers");
                        String rewardTakenStr = rs.getString("rewards");
                        long startTime = rs.getTimestamp("starttime").getTime();
                        long endTime = rs.getTimestamp("endtime").getTime();
                        ret.getBingoRecord().load(answersStr, rewardTakenStr, startTime, endTime);
                        break;
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM imps WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    r = 0;
                    while (rs.next()) {
                        ret.imps[r] = new MapleImp(rs.getInt("itemid"));
                        ret.imps[r].setLevel(rs.getByte("level"));
                        ret.imps[r].setState(rs.getByte("state"));
                        ret.imps[r].setCloseness(rs.getShort("closeness"));
                        ret.imps[r].setFullness(rs.getShort("fullness"));
                        r++;
                    }
                    rs.close();
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM mountdata WHERE characterid = ?")) {
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    if (!rs.next()) {
                        System.out.println("在數據庫沒有找到角色坐騎訊息...");
                        return null;
                    }
                    final Item mount = ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
                    ret.mount = new MapleMount(ret, mount != null ? mount.getItemId() : 0, 80001000,
                            rs.getByte("Fatigue"), rs.getByte("Level"), rs.getInt("Exp"));
                    rs.close();
                }

                ret.stats.recalcLocalStats(true, ret);
            } else { // Not channel server
                for (Pair<Item, MapleInventoryType> mit : ItemLoader.INVENTORY.loadItems(true, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                }
                ret.stats.recalcPVPRank(ret);
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ess) {
            ess.printStackTrace();
            System.out.println("加載角色出錯..");
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ess);
            return null;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignore) {
            }
        }
        return ret;
    }

    public static int getQuestKillCount(MapleCharacter chr, final int mobid) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement pse;
            try (PreparedStatement ps = (PreparedStatement) con
                    .prepareStatement("SELECT queststatusid FROM queststatus WHERE characterid = ?")) {
                ResultSet rse;
                try (ResultSet rs = ps.executeQuery()) {
                    pse = (PreparedStatement) con
                            .prepareStatement("SELECT count FROM queststatusmobs WHERE queststatusid = ?");
                    rse = pse.executeQuery();
                    while (rs.next()) {
                        return rse.getInt("count");
                    }
                }
                rse.close();
            }
            pse.close();
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
        }
        return -1;
    }

    public static void saveNewCharToDB(final MapleCharacter chr, final JobType type, short db) {
        saveNewCharToDB(chr, type, db, 0);
    }

    public static void saveNewCharToDB(final MapleCharacter chr, final JobType type, short db, int keymapType) {
        try {
            Connection con = ManagerDatabasePool.getConnection();

            PreparedStatement ps = null;
            PreparedStatement pse = null;
            ResultSet rs = null;
            try {
                con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                con.setAutoCommit(false);

                ps = con.prepareStatement(
                        "INSERT INTO characters (level, str, dex, luk, `int`, hp, mp, maxhp, maxmp, sp, hsp, ap, skincolor, gender, job, hair, face, faceMarking, ears, tail, weaponPoint, map, meso, party, buddyCapacity, pets, subcategory, friendshippoints, chatcolour, gm, accountid, name, world, position)"
                        + "                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        ManagerDatabasePool.RETURN_GENERATED_KEYS);
                int index = 0;
                ps.setInt(++index, chr.level); // Level
                final PlayerStats stat = chr.stats;
                ps.setInt(++index, stat.getStr()); // Str
                ps.setInt(++index, stat.getDex()); // Dex
                ps.setInt(++index, stat.getInt()); // Int
                ps.setInt(++index, stat.getLuk()); // Luk
                ps.setInt(++index, stat.getHp()); // HP
                ps.setInt(++index, stat.getMp());
                ps.setInt(++index, stat.getMaxHp()); // MP
                ps.setInt(++index, stat.getMaxMp());
                final StringBuilder sps = new StringBuilder();
                for (int i = 0; i < chr.remainingSp.length; i++) {
                    sps.append(chr.remainingSp[i]);
                    sps.append(",");
                }
                final String sp = sps.toString();
                ps.setString(++index, sp.substring(0, sp.length() - 1));
                final StringBuilder hsps = new StringBuilder();
                for (int i = 0; i < chr.remainingHSp.length; i++) {
                    hsps.append(chr.remainingHSp[i]);
                    hsps.append(",");
                }
                final String hsp = hsps.toString();
                ps.setString(++index, hsp.substring(0, hsp.length() - 1));
                if (chr.remainingAp > (999 + 16) - (chr.str + chr.dex + chr.int_ + chr.luk)) {
                    chr.remainingAp = (999 + 16) - (chr.str + chr.dex + chr.int_ + chr.luk);
                }
                ps.setShort(++index, (short) chr.remainingAp); // Remaining AP
                ps.setByte(++index, chr.skinColor);
                ps.setByte(++index, chr.gender);
                ps.setInt(++index, chr.job);
                ps.setInt(++index, chr.hair);
                ps.setInt(++index, chr.face);
                ps.setInt(++index, chr.faceMarking);
                ps.setInt(++index, chr.ears);
                ps.setInt(++index, chr.tail);
                if (db < 0 || db > 10) {
                    db = 0;
                }
                ps.setLong(++index, chr.weaponPoint); // WeaponPoint
                ps.setInt(++index, type.map);
                ps.setLong(++index, chr.meso); // Meso
                ps.setInt(++index, -1); // Party
                ps.setByte(++index, chr.buddylist.getCapacity()); // Buddylist
                ps.setString(++index, "-1,-1,-1");
                ps.setInt(++index, db); // for now
                ps.setString(++index, chr.friendshippoints[0] + "," + chr.friendshippoints[1] + ","
                        + chr.friendshippoints[2] + "," + chr.friendshippoints[3] + "," + chr.friendshippoints[4]);
                ps.setShort(++index, (short) 0);
                ps.setByte(++index, (byte) chr.gmLevel);
                ps.setInt(++index, chr.getAccountID());
                ps.setString(++index, chr.name);
                ps.setByte(++index, chr.world);
                ps.setInt(++index, chr.getClient().getCharacterPos().size() + 1);
                ps.executeUpdate();

                rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    chr.id = rs.getInt(1);
                } else {
                    ps.close();
                    rs.close();
                    throw new DatabaseException("Inserting char failed.");
                }
                ps.close();
                rs.close();
                ps = con.prepareStatement(
                        "INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)",
                        ManagerDatabasePool.RETURN_GENERATED_KEYS);
                pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
                ps.setInt(1, chr.id);
                for (final MapleQuestStatus q : chr.quests.values()) {
                    ps.setInt(2, q.getQuest().getId());
                    ps.setInt(3, q.getStatus());
                    ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                    ps.setInt(5, q.getForfeited());
                    ps.setString(6, q.getCustomData());
                    ps.execute();
                    rs = ps.getGeneratedKeys();
                    if (q.hasMobKills()) {
                        rs.next();
                        for (int mob : q.getMobKills().keySet()) {
                            pse.setInt(1, rs.getInt(1));
                            pse.setInt(2, mob);
                            pse.setInt(3, q.getMobKills(mob));
                            pse.execute();
                        }
                    }
                    rs.close();
                }
                ps.close();
                pse.close();

                ps = con.prepareStatement(
                        "INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration, teachId) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, chr.id);

                for (final Entry<Skill, SkillEntry> skill : chr.skills.entrySet()) {
                    if (SkillConstants.isApplicableSkill(skill.getKey().getId())) { // do not save additional skills
                        ps.setInt(2, skill.getKey().getId());
                        ps.setInt(3, skill.getValue().skillLevel);
                        ps.setByte(4, skill.getValue().masterlevel);
                        ps.setLong(5, skill.getValue().expiration);
                        ps.setInt(6, skill.getValue().teachId);
                        ps.execute();
                    }
                }
                ps.close();

                ps = con.prepareStatement(
                        "INSERT INTO coreauras (cid, str, dex, `int`, luk, att, magic, total, expire) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, chr.id);
                if (MapleJob.is蒼龍俠客(chr.job)) {
                    ps.setInt(2, 3);
                    ps.setInt(3, 3);
                    ps.setInt(4, 3);
                    ps.setInt(5, 3);
                    ps.setInt(6, 3);
                    ps.setInt(7, 3);
                    ps.setInt(8, 24 * 60);
                }

                ps = con.prepareStatement(
                        "INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, chr.id);
                ps.setShort(2, (short) Math.min(FeaturesConfig.defaultInventorySlot, GameConstants.InventorySlotMax)); // 初始裝備欄Eq
                ps.setShort(3, (short) Math.min(FeaturesConfig.defaultInventorySlot, GameConstants.InventorySlotMax)); // 初始消耗欄Use
                ps.setShort(4, (short) Math.min(FeaturesConfig.defaultInventorySlot, GameConstants.InventorySlotMax)); // 初始裝飾欄Setup
                ps.setShort(5, (short) Math.min(FeaturesConfig.defaultInventorySlot, GameConstants.InventorySlotMax)); // 初始其他欄ETC
                ps.setShort(6, (short) GameConstants.InventorySlotMax); // 初始特殊欄Cash
                ps.execute();
                ps.close();

                ps = con.prepareStatement(
                        "INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
                ps.setInt(1, chr.id);
                ps.setByte(2, (byte) 1);
                ps.setInt(3, 0);
                ps.setByte(4, (byte) 0);
                ps.execute();
                ps.close();
                int[] aKey;
                int[] aType;
                int[] aAction;
                if (keymapType == 0) {
                    // 基本模式設定
                    aKey = new int[]{1, 2, 3, 4, 5, 6, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 31, 34, 35,
                        37, 38, 39, 40, 41, 43, 44, 45, 46, 47, 48, 50, 56, 57, 59, 60, 61, 63, 64, 65, 66, 70};
                    aType = new int[]{4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4,
                        4, 5, 5, 4, 4, 4, 4, 5, 5, 6, 6, 6, 6, 6, 6, 6, 4};
                    aAction = new int[]{46, 10, 12, 13, 18, 23, 8, 5, 0, 4, 27, 30, 39, 1, 41, 19, 14, 15, 52, 2, 17,
                        11, 3, 20, 26, 16, 22, 9, 50, 51, 6, 31, 29, 7, 53, 54, 100, 101, 102, 103, 104, 105, 106,
                        47};
                } else {
                    // 進階模式設定
                    aKey = new int[]{1, 20, 21, 22, 23, 25, 26, 27, 29, 34, 35, 36, 37, 38, 39, 40, 41, 43, 44, 45,
                        46, 47, 48, 49, 50, 52, 56, 57, 59, 60, 61, 63, 64, 65, 66, 70, 71, 73, 79, 82, 83};
                    aType = new int[]{4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 4, 4, 5,
                        5, 6, 6, 6, 6, 6, 6, 6, 4, 4, 4, 4, 4, 4};
                    aAction = new int[]{46, 27, 30, 0, 1, 19, 14, 15, 52, 17, 11, 8, 3, 20, 26, 16, 22, 9, 50, 51, 2,
                        31, 29, 5, 7, 4, 53, 54, 100, 101, 102, 103, 104, 105, 106, 47, 12, 13, 23, 10, 18};
                }
                ps = con.prepareStatement(
                        "INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
                ps.setInt(1, chr.id);
                for (int i = 0; i < aKey.length; i++) {
                    ps.setInt(2, aKey[i]);
                    ps.setInt(3, aType[i]);
                    ps.setInt(4, aAction[i]);
                    ps.execute();
                }
                ps.close();

                List<Pair<Item, MapleInventoryType>> listing = new ArrayList<>();
                for (final MapleInventory iv : chr.inventory) {
                    for (final Item item : iv.list()) {
                        listing.add(new Pair<>(item, iv.getType()));
                    }
                }
                ItemLoader.INVENTORY.saveItems(listing, chr.id, con);

                con.commit();
            } catch (SQLException | DatabaseException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                System.err.println("[newcharsave] Error saving character data: " + e);
                e.printStackTrace();
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
                    System.err.println("[newcharsave] Error Rolling Back");
                }
            } finally {
                try {
                    if (pse != null) {
                        pse.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                    con.setAutoCommit(true);
                    con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                    con.close();
                } catch (SQLException e) {
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                    System.err.println("[charsave] Error going back to autocommit mode");
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException se) {
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, se);
        }
        chr.getClient().addCharacterPos(chr.getId());
    }

    public void saveToDB(boolean dc, boolean fromcs) {
        if (isClone()) {
            return;
        }
        getClient().setSaving(true);
        try {
            Connection con = ManagerDatabasePool.getConnection();

            PreparedStatement ps = null;
            PreparedStatement pse = null;
            ResultSet rs = null;

            try {
                con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                con.setAutoCommit(false);

                ps = con.prepareStatement(
                        "UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, hsp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, faceMarking = ?, ears = ?, tail = ?, weaponPoint = ?, map = ?, meso = ?, hpApUsed = ?, mpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, pets = ?, subcategory = ?, gachexp = ?, fatigue = ?, charm = ?, charisma = ?, craft = ?, insight = ?, sense = ?, will = ?, pvpExp = ?, pvpPoints = ?, honourExp = ?, honourLevel = ?, friendshippoints = ?, friendshiptoadd = ?, salon_hair = ?, salon_face = ?, chatcolour = ?, name = ? WHERE id = ?",
                        ManagerDatabasePool.RETURN_GENERATED_KEYS);
                int index = 0;
                ps.setInt(++index, level);
                ps.setInt(++index, fame);
                ps.setInt(++index, stats.getStr());
                ps.setInt(++index, stats.getDex());
                ps.setInt(++index, stats.getLuk());
                ps.setInt(++index, stats.getInt());
                ps.setLong(++index, level >= maxLevel ? 0 : exp);
                ps.setInt(++index, stats.getHp() < 1 ? 50 : stats.getHp());
                ps.setInt(++index, stats.getMp());
                ps.setInt(++index, stats.getMaxHp());
                ps.setInt(++index, stats.getMaxMp());
                final StringBuilder sps = new StringBuilder();
                for (int i = 0; i < remainingSp.length; i++) {
                    sps.append(remainingSp[i]);
                    sps.append(",");
                }
                final String skillpoints = sps.toString();
                ps.setString(++index, skillpoints.substring(0, skillpoints.length() - 1));
                final StringBuilder hsps = new StringBuilder();
                for (int i = 0; i < remainingHSp.length; i++) {
                    hsps.append(remainingHSp[i]);
                    hsps.append(",");
                }
                final String hskillpoints = hsps.toString();
                ps.setString(++index, hskillpoints.substring(0, hskillpoints.length() - 1));
                ps.setInt(++index, remainingAp);
                ps.setByte(++index, gmLevel);
                ps.setByte(++index, skinColor);
                ps.setByte(++index, gender);
                ps.setInt(++index, job);
                ps.setInt(++index, hair);
                ps.setInt(++index, face);
                ps.setInt(++index, faceMarking);
                ps.setInt(++index, ears);
                ps.setInt(++index, tail);
                ps.setInt(++index, weaponPoint);
                if (!fromcs && map != null) {
                    // 下線儲存默認下線地圖
                    boolean noReturn = false;
                    if (map.getId() == 927030090 || map.getId() == 927030091) {
                        noReturn = true;
                    }
                    if (map.getForcedReturnId() != 999999999 && map.getForcedReturnMap() != null && !noReturn) {
                        ps.setInt(++index, map.getForcedReturnId());
                    } else {
                        ps.setInt(++index, stats.getHp() < 1 ? map.getReturnMapId() : map.getId());
                    }
                } else {
                    ps.setInt(++index, mapid);
                }
                ps.setLong(++index, meso);
                ps.setShort(++index, hpApUsed);
                ps.setShort(++index, mpApUsed);
                if (map == null) {
                    ps.setByte(++index, (byte) 0);
                } else {
                    final MaplePortal closest = map.findClosestSpawnpoint(getTruePosition());
                    ps.setByte(++index, (byte) (closest != null ? closest.getId() : 0));
                }
                ps.setInt(++index, party == null ? -1 : party.getId());
                ps.setShort(++index, buddylist.getCapacity());
                final StringBuilder petz = new StringBuilder();
                for (int petLength = 0; petLength < 3; petLength++) {
                    boolean found = false;
                    for (final MaplePet pet : getSummonedPets()) {
                        if (pet.getSummonedValue() == petLength + 1) {
                            pet.saveToDb();
                            petz.append(pet.getInventoryPosition());
                            petz.append(",");
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        petz.append("-1,");
                    }
                }
                final String petstring = petz.toString();
                ps.setString(++index, petstring.substring(0, petstring.length() - 1));
                ps.setByte(++index, subcategory);
                // ps.setInt(++index, marriageId);
                ps.setInt(++index, gachexp);
                ps.setShort(++index, fatigue);
                ps.setInt(++index, traits.get(MapleTraitType.charm).getExp());
                ps.setInt(++index, traits.get(MapleTraitType.charisma).getExp());
                ps.setInt(++index, traits.get(MapleTraitType.craft).getExp());
                ps.setInt(++index, traits.get(MapleTraitType.insight).getExp());
                ps.setInt(++index, traits.get(MapleTraitType.sense).getExp());
                ps.setInt(++index, traits.get(MapleTraitType.will).getExp());
                ps.setInt(++index, pvpExp);
                ps.setInt(++index, pvpPoints);
                ps.setInt(++index, honourExp);
                ps.setInt(++index, honorLevel);
                ps.setString(++index, friendshippoints[0] + "," + friendshippoints[1] + "," + friendshippoints[2] + ","
                        + friendshippoints[3] + "," + friendshippoints[4]);
                ps.setInt(++index, friendshiptoadd);
                ps.setInt(++index, !salon.containsKey(30000) ? 3 : salon.get(30000).size());
                ps.setInt(++index, !salon.containsKey(20000) ? 3 : salon.get(20000).size());
                ps.setShort(++index, chattype);
                ps.setString(++index, name);
                ps.setInt(++index, id);
                if (ps.executeUpdate() < 1) {
                    ps.close();
                    throw new DatabaseException("Character not in database (" + id + ")");
                }
                ps.close();

                deleteWhereCharacterId(con, "DELETE FROM salon WHERE characterid = ?");
                ps = con.prepareStatement(
                        "INSERT INTO salon (characterid, type, position, itemId, gender) VALUES (?, ?, ?, ?, ?)");
                ps.setInt(1, id);
                for (Map.Entry<Integer, List<Pair<Integer, Integer>>> entry : salon.entrySet()) {
                    ps.setInt(2, entry.getKey());
                    int i = 0;
                    for (Pair<Integer, Integer> sl : entry.getValue()) {
                        if (sl.getLeft() != 0) {
                            ps.setInt(3, i);
                            int itemId = sl.getLeft();
                            int gender = sl.getRight();
                            ps.setInt(4, itemId);
                            ps.setInt(5, gender);
                            ps.execute();
                        }
                        i++;
                    }
                }
                ps.close();

                deleteWhereCharacterId(con, "DELETE FROM charactercustomdata WHERE characterid = ?");
                ps = con.prepareStatement(
                        "INSERT INTO charactercustomdata (characterid, `key`, `data`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (Map.Entry<String, String> entry : CustomValues.entrySet()) {
                    ps.setString(2, entry.getKey());
                    ps.setString(3, entry.getValue());
                    ps.execute();
                }
                ps.close();

                deleteWhereCharacterId(con, "DELETE FROM stolen WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO stolen (characterid, skillid, chosen) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (Pair<Integer, Boolean> st : stolenSkills) {
                    ps.setInt(2, st.left);
                    ps.setInt(3, st.right ? 1 : 0);
                    ps.execute();
                }
                ps.close();

                if (changed_skillmacros) {
                    deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
                    for (int i = 0; i < 5; i++) {
                        final SkillMacro macro = skillMacros[i];
                        if (macro != null) {
                            ps = con.prepareStatement(
                                    "INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                            ps.setInt(1, id);
                            ps.setInt(2, macro.getSkill1());
                            ps.setInt(3, macro.getSkill2());
                            ps.setInt(4, macro.getSkill3());
                            ps.setString(5, macro.getName());
                            ps.setInt(6, macro.getShout());
                            ps.setInt(7, i);
                            ps.execute();
                            ps.close();
                        }
                    }
                }
                deleteWhereCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?");
                ps = con.prepareStatement(
                        "INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, id);
                ps.setShort(2, getInventory(MapleInventoryType.EQUIP).getSlotLimit());
                ps.setShort(3, getInventory(MapleInventoryType.USE).getSlotLimit());
                ps.setShort(4, getInventory(MapleInventoryType.SETUP).getSlotLimit());
                ps.setShort(5, getInventory(MapleInventoryType.ETC).getSlotLimit());
                ps.setShort(6, getInventory(MapleInventoryType.CASH).getSlotLimit());
                ps.execute();
                ps.close();

                saveInventory(con);

                if (changed_questinfo) {
                    deleteWhereCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
                    ps = con.prepareStatement(
                            "INSERT INTO questinfo (`characterid`, `quest`, `customData`) VALUES (?, ?, ?)");
                    ps.setInt(1, id);
                    for (final Entry<Integer, String> q : questinfo.entrySet()) {
                        ps.setInt(2, q.getKey());
                        ps.setString(3, q.getValue());
                        ps.execute();
                    }
                    ps.close();
                }

                deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
                ps = con.prepareStatement(
                        "INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)",
                        ManagerDatabasePool.RETURN_GENERATED_KEYS);
                pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
                ps.setInt(1, id);
                for (final MapleQuestStatus q : quests.values()) {
                    ps.setInt(2, q.getQuest().getId());
                    ps.setInt(3, q.getStatus());
                    ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                    ps.setInt(5, q.getForfeited());
                    ps.setString(6, q.getCustomData());
                    ps.execute();
                    rs = ps.getGeneratedKeys();
                    if (q.hasMobKills()) {
                        rs.next();
                        for (int mob : q.getMobKills().keySet()) {
                            pse.setInt(1, rs.getInt(1));
                            pse.setInt(2, mob);
                            pse.setInt(3, q.getMobKills(mob));
                            pse.execute();
                        }
                    }
                    rs.close();
                }
                ps.close();
                pse.close();

                if (changed_skills) {
                    deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
                    ps = con.prepareStatement(
                            "INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration, teachId) VALUES (?, ?, ?, ?, ?, ?)");
                    ps.setInt(1, id);

                    for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                        if (!skill.getKey().isVSkill()) {
                            if (SkillConstants.isApplicableSkill(skill.getKey().getId())
                                    && skill.getKey().getFixLevel() == 0) { // do not save additional skills
                                ps.setInt(2, skill.getKey().getId());
                                ps.setInt(3, skill.getValue().skillLevel);
                                ps.setByte(4, skill.getValue().masterlevel);
                                ps.setLong(5, skill.getValue().expiration);
                                ps.setInt(6, skill.getValue().teachId);
                                ps.execute();
                            }
                        }
                    }
                    ps.close();
                }

                deleteWhereCharacterId(con, "DELETE FROM coreauras WHERE cid = ?");
                ps = con.prepareStatement(
                        "INSERT INTO coreauras (cid, str, dex, `int`, luk, att, magic, total, expire) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, id);
                ps.setInt(2, getCoreAura().getStr());
                ps.setInt(3, getCoreAura().getDex());
                ps.setInt(4, getCoreAura().getInt());
                ps.setInt(5, getCoreAura().getLuk());
                ps.setInt(6, getCoreAura().getAtt());
                ps.setInt(7, getCoreAura().getMagic());
                ps.setInt(8, getCoreAura().getTotal());
                ps.setInt(9, getCoreAura().getExpire());
                ps.execute();
                ps.close();

                if (innerskill_changed) {
                    deleteWhereCharacterId(con, "DELETE FROM inner_ability_skills WHERE player_id = ?");
                    for (int i = 0; i < 3; i++) {
                        InnerSkillValueHolder InnerSkill = innerSkills[i];
                        if (InnerSkill != null) {
                            ps = con.prepareStatement(
                                    "INSERT INTO inner_ability_skills (player_id, skill_id, skill_level, position, rank) VALUES (?, ?, ?, ?, ?)");
                            ps.setInt(1, id);
                            ps.setInt(2, InnerSkill.getSkillId());
                            ps.setInt(3, InnerSkill.getSkillLevel());
                            ps.setInt(4, InnerSkill.getPosition());
                            ps.setInt(5, InnerSkill.getRank());
                            ps.execute();
                            ps.close();
                        }
                    }
                }

                List<MapleCoolDownValueHolder> cd = getCooldowns();
                if (dc && cd.size() > 0) {
                    ps = con.prepareStatement(
                            "INSERT INTO skills_cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
                    ps.setInt(1, getId());
                    for (final MapleCoolDownValueHolder cooling : cd) {
                        ps.setInt(2, cooling.skillId);
                        ps.setLong(3, cooling.startTime);
                        ps.setLong(4, cooling.length);
                        ps.execute();
                    }
                    ps.close();
                }

                if (changed_savedlocations) {
                    deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
                    ps = con.prepareStatement(
                            "INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
                    ps.setInt(1, id);
                    for (final SavedLocationType savedLocationType : SavedLocationType.values()) {
                        if (savedLocations[savedLocationType.getValue()] != -1) {
                            ps.setInt(2, savedLocationType.getValue());
                            ps.setInt(3, savedLocations[savedLocationType.getValue()]);
                            ps.execute();
                        }
                    }
                    ps.close();
                }

                if (changed_reports) {
                    deleteWhereCharacterId(con, "DELETE FROM reports WHERE characterid = ?");
                    ps = con.prepareStatement("INSERT INTO reports VALUES(DEFAULT, ?, ?, ?)");
                    for (Entry<ReportType, Integer> achid : reports.entrySet()) {
                        ps.setInt(1, id);
                        ps.setByte(2, achid.getKey().i);
                        ps.setInt(3, achid.getValue());
                        ps.execute();
                    }
                    ps.close();
                }

                if (buddylist.changed()) {
                    deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ?");
                    ps = con.prepareStatement(
                            "INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, ?)");
                    ps.setInt(1, id);
                    for (BuddylistEntry entry : buddylist.getBuddies()) {
                        ps.setInt(2, entry.getCharacterId());
                        ps.setInt(3, entry.isVisible() ? 0 : 1);
                        ps.execute();
                    }
                    ps.close();
                    buddylist.setChanged(false);
                }

                ps = con.prepareStatement(
                        "UPDATE accounts SET `BeanfunGash` = ?, `Mileage` = ?, `mPoints` = ?, `lastWorld` = ? WHERE id = ?");
                ps.setInt(1, beanfunGash);
                ps.setInt(2, mileage);
                ps.setInt(3, maplepoints);
                ps.setByte(4, lastWorld);
                ps.setInt(5, client.getAccID());
                ps.executeUpdate();
                ps.close();

                if (storage != null) {
                    storage.saveToDB(con);
                }
                if (cs != null) {
                    cs.save(con);
                }
                if (PlayerNPC.Auto_Update) {
                    PlayerNPC.updateByCharId(this);
                }
                keylayout.saveKeys(id, con);
                mount.saveMount(id, con);
                monsterbook.saveCards(accountid, con);

                deleteWhereCharacterId(con, "DELETE FROM familiars WHERE characterid = ?");
                ps = con.prepareStatement(
                        "INSERT INTO familiars (characterid, familiar, name, level, exp, grade, skillid, option1, option2, option3) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, id);
                for (MonsterFamiliar f : familiars.values()) {
                    ps.setInt(2, f.getFamiliar());
                    ps.setString(3, f.getName());
                    ps.setByte(4, f.getLevel());
                    ps.setInt(5, f.getExp());
                    ps.setInt(6, f.getGrade());
                    ps.setInt(7, f.getSkill());
                    ps.setInt(8, f.getOption1());
                    ps.setInt(9, f.getOption2());
                    ps.setInt(10, f.getOption3());
                    ps.executeUpdate();
                }
                ps.close();

                deleteWhereCharacterId(con, "DELETE FROM vcores WHERE characterid = ?");
                ps = con.prepareStatement(
                        "INSERT INTO vcores (characterid, connectSkill1, connectSkill2, connectSkill3, expireDate, coreId, coreLevel, coreExp, coreState) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, id);
                for (VMatrixRecord r : aVMatrixRecords) {
                    ps.setInt(2, r.getSkillID1());
                    ps.setInt(3, r.getSkillID2());
                    ps.setInt(4, r.getSkillID3());
                    ps.setLong(5, r.getExpirationDate());
                    ps.setInt(6, r.getIconID());
                    ps.setByte(7, (byte) r.getSkillLv());
                    ps.setInt(8, r.getExp());
                    ps.setByte(9, (byte) r.getState());
                    ps.executeUpdate();
                }

                deleteWhereCharacterId(con, "DELETE FROM imps WHERE characterid = ?");
                ps = con.prepareStatement(
                        "INSERT INTO imps (characterid, itemid, closeness, fullness, state, level) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, id);
                for (MapleImp imp : imps) {
                    if (imp != null) {
                        ps.setInt(2, imp.getItemId());
                        ps.setShort(3, imp.getCloseness());
                        ps.setShort(4, imp.getFullness());
                        ps.setByte(5, imp.getState());
                        ps.setByte(6, imp.getLevel());
                        ps.executeUpdate();
                    }
                }
                ps.close();
                if (changed_wishlist) {
                    deleteWhereCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?");
                    for (int i = 0; i < getWishlistSize(); i++) {
                        ps = con.prepareStatement("INSERT INTO wishlist(characterid, sn) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, wishlist[i]);
                        ps.execute();
                        ps.close();
                    }
                }

                // if(changed_bingos) {
                deleteWhereCharacterId(con, "DELETE FROM bingorecords WHERE characterid = ?");
                ps = con.prepareStatement(
                        "INSERT INTO bingorecords(characterid, answers, rewards, starttime, endtime) VALUES(?, ?, ?, ?, ?) ");

                ps.setInt(1, getId());
                ps.setString(2, bingoRecord.getEncodeAnswers());
                ps.setString(3, bingoRecord.getEncodeRewardTaken());
                ps.setTimestamp(4, new Timestamp(bingoRecord.getStartTime()));
                ps.setTimestamp(5, new Timestamp(bingoRecord.getEndTime()));
                // }
                if (changed_trocklocations) {
                    deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?");
                    for (int i = 0; i < rocks.length; i++) {
                        if (rocks[i] != 999999999) {
                            ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid) VALUES(?, ?) ");
                            ps.setInt(1, getId());
                            ps.setInt(2, rocks[i]);
                            ps.execute();
                            ps.close();
                        }
                    }
                }

                if (changed_regrocklocations) {
                    deleteWhereCharacterId(con, "DELETE FROM regrocklocations WHERE characterid = ?");
                    for (int i = 0; i < regrocks.length; i++) {
                        if (regrocks[i] != 999999999) {
                            ps = con.prepareStatement("INSERT INTO regrocklocations(characterid, mapid) VALUES(?, ?) ");
                            ps.setInt(1, getId());
                            ps.setInt(2, regrocks[i]);
                            ps.execute();
                            ps.close();
                        }
                    }
                }
                if (changed_hyperrocklocations) {
                    deleteWhereCharacterId(con, "DELETE FROM hyperrocklocations WHERE characterid = ?");
                    for (int i = 0; i < hyperrocks.length; i++) {
                        if (hyperrocks[i] != 999999999) {
                            ps = con.prepareStatement(
                                    "INSERT INTO hyperrocklocations(characterid, mapid) VALUES(?, ?) ");
                            ps.setInt(1, getId());
                            ps.setInt(2, hyperrocks[i]);
                            ps.execute();
                            ps.close();
                        }
                    }
                }
                if (changed_extendedSlots) {
                    deleteWhereCharacterId(con, "DELETE FROM extendedSlots WHERE characterid = ?");
                    for (int i : extendedSlots) {
                        if (getInventory(MapleInventoryType.ETC).findById(i) != null) { // just in case
                            ps = con.prepareStatement("INSERT INTO extendedSlots(characterid, itemId) VALUES(?, ?) ");
                            ps.setInt(1, getId());
                            ps.setInt(2, i);
                            ps.execute();
                            ps.close();
                        }
                    }
                }
                changed_wishlist = false;
                changed_trocklocations = false;
                changed_regrocklocations = false;
                changed_hyperrocklocations = false;
                changed_skillmacros = false;
                changed_savedlocations = false;
                changed_questinfo = false;
                changed_extendedSlots = false;
                changed_skills = false;
                changed_reports = false;
                con.commit();
            } catch (SQLException | DatabaseException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                System.err.println(MapleClient.getLogMessage(this, "[儲存角色] 儲存角色出錯:") + e);
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
                    System.err.println(MapleClient.getLogMessage(this, "[charsave] Error Rolling Back") + e);
                }
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                    if (pse != null) {
                        pse.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                    con.setAutoCommit(true);
                    con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                    con.close();
                } catch (SQLException e) {
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                    System.err.println(
                            MapleClient.getLogMessage(this, "[charsave] Error going back to autocommit mode") + e);
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException se) {
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, se);
        }
        getClient().setSaving(false);
    }

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        deleteWhereCharacterId(con, sql, id);
    }

    public static void deleteWhereCharacterId(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        try {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception ex) {
            System.err.println("移除數據庫訊息錯誤:" + ex);
        }
    }

    public static void deleteWhereCharacterId_NoLock(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.execute();
        }
    }

    public void saveInventory(final Connection con) throws SQLException {
        List<Pair<Item, MapleInventoryType>> listing = new ArrayList<>();
        for (final MapleInventory iv : inventory) {
            for (final Item item : iv.list()) {
                listing.add(new Pair<>(item, iv.getType()));
            }
        }
        ItemLoader.INVENTORY.saveItems(listing, id, con);
    }

    public final PlayerStats getStat() {
        return stats;
    }

    public final void QuestInfoPacket(final tools.data.MaplePacketLittleEndianWriter mplew) {
        if (MapleJob.is幻獸師(getJob())) {
            int beast = MapleJob.is幻獸師(getJob()) ? 1 : 0;
            String ears = Integer.toString(getEars());
            String tail = Integer.toString(getTail());
            questinfo.put(59300, "bTail=" + beast + ";bEar=" + beast + ";TailID=" + tail + ";EarID=" + ears);
        }
        mplew.writeShort(questinfo.size()); // // Party Quest data (quest needs to be added in the quests list)
        for (final Entry<Integer, String> q : questinfo.entrySet()) {
            mplew.writeInt(q.getKey());
            mplew.writeMapleAsciiString(q.getValue() == null ? "" : q.getValue());
        }
    }

    public final void updateInfoQuest(final int questid, String data) {
        if (data == null) {
            data = "";
        }
        questinfo.put(questid, data);
        changed_questinfo = true;
        client.announce(InfoPacket.updateInfoQuest(questid, data));
    }

    public final String getInfoQuest(final int questid) {
        if (questinfo.containsKey(questid)) {
            return questinfo.get(questid);
        }
        return "";
    }

    public final void clearInfoQuest(final int questid) {
        if (questinfo.containsKey(questid)) {
            updateInfoQuest(questid, null);
            questinfo.remove(questid);
        }
    }

    public final int getNumQuest() {
        int i = 0;
        for (final MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !(q.isCustom())) {
                i++;
            }
        }
        return i;
    }

    public final byte getQuestStatus(final int quest) {
        final MapleQuest qq = MapleQuest.getInstance(quest);
        if (getQuestNoAdd(qq) == null) {
            return 0;
        }
        return getQuestNoAdd(qq).getStatus();
    }

    public final MapleQuestStatus getQuest(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest, (byte) 0);
        }
        return quests.get(quest);
    }

    public final void setQuestAdd(final MapleQuest quest, final byte status, final String customData) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus stat = new MapleQuestStatus(quest, status);
            stat.setCustomData(customData);
            quests.put(quest, stat);
        }
    }

    public final MapleQuestStatus getQuestNAdd(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus status = new MapleQuestStatus(quest, (byte) 0);
            quests.put(quest, status);
            return status;
        }
        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestNoAdd(final MapleQuest quest) {
        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestRemove(final MapleQuest quest) {
        return quests.remove(quest);
    }

    public final void updateQuest(final MapleQuestStatus quest) {
        updateQuest(quest, false);
    }

    public final void updateQuest(final MapleQuestStatus quest, final boolean update) {
        quests.put(quest.getQuest(), quest);
        if (!quest.isCustom()) {
            client.announce(InfoPacket.updateQuest(quest));
            if (quest.getStatus() == 1 && !update) {
                client.announce(CField.updateQuestInfo(quest.getQuest().getId(), quest.getNpc()));// was10
            }
        }
    }

    public final Map<Integer, String> getInfoQuest_Map() {
        return questinfo;
    }

    public final Map<MapleQuest, MapleQuestStatus> getQuest_Map() {
        return quests;
    }
    //
    // public List<Pair<MapleBuffStat, MapleBuffStatValueHolder>> getAllEffects() {
    // return new ArrayList<>(effects);
    // }

    // public List<MapleBuffStatValueHolder> getBuffStatValueHolders(MapleBuffStat
    // stat) {
    // List<MapleBuffStatValueHolder> mbsvhs = null;
    // for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
    // if (buffs.getLeft() == stat) {
    // MapleBuffStatValueHolder mbsvh = buffs.getRight();
    // if (mbsvh == null) {
    // continue;
    // }
    // if (mbsvhs == null) {
    // mbsvhs = new ArrayList<>();
    // }
    // mbsvhs.add(mbsvh);
    // }
    // }
    // return mbsvhs;
    // }
    //
    // public MapleBuffStatValueHolder getBuffStatValueHolder(MapleStatEffect
    // effect, MapleBuffStat stat) {
    // if (effect == null) {
    // return null;
    // }
    // return getBuffStatValueHolder(effect.getSourceId(), stat);
    // }
    //
    // public MapleBuffStatValueHolder getBuffStatValueHolder(int skillid,
    // MapleBuffStat stat) {
    // List<MapleBuffStatValueHolder> mbsvhs = getBuffStatValueHolders(stat);
    // if (mbsvhs != null) {
    // for (MapleBuffStatValueHolder mbsvh : mbsvhs) {
    // if (mbsvh == null || mbsvh.effect == null) {
    // continue;
    // }
    // if (mbsvh.effect.getSourceId() == skillid) {
    // return mbsvh;
    // }
    // }
    // }
    // return null;
    // }
    public Integer getBuffedValue(MapleBuffStat stat) {
        if (stat.isIndie() && !constants.GameConstants.isSpecialIndieBuff(stat)
                && !this.getTempstats().aIndieTempStat.isEmpty()) {
            for (Map.Entry<MapleBuffStat, IndieTempStat> entry : this.getTempstats().aIndieTempStat.entrySet()) {
                if (entry.getKey() == stat) {
                    return entry.getValue().getValueSum();
                }
            }
        }
        TemporaryStatBase tsb = this.getTempstats().getStatOption(stat);
        if (tsb == null) {
            return null;
        }
        if (tsb.getReason() <= 0) {
            return null;
        }
        return tsb.getValue();
    }

    public final Integer getBuffedSkill_X(final MapleBuffStat stat) {
        TemporaryStatBase tbs = this.getTempstats().getStatOption(stat);
        if (tbs == null || tbs.getReason() <= 0) {
            return null;
        }
        return tbs.xOption;
    }

    public Integer getBuffedSkill_Y(MapleBuffStat stat) {
        TemporaryStatBase tbs = this.getTempstats().getStatOption(stat);
        if (tbs == null || tbs.getReason() <= 0) {
            return null;
        }
        return tbs.yOption;
    }

    public boolean isBuffFrom(MapleBuffStat stat, Skill skill) {
        TemporaryStatBase tbs = this.getTempstats().getStatOption(stat);
        if (tbs == null) {
            return false;
        } else if (tbs.getReason() != skill.getId()) {
            return false;
        }
        return true;
    }

    public boolean hasBuffSkill(int skillId) {
        List<MapleBuffStatValueHolder> mbsvhs = new ArrayList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : mbsvhs) {
            if (mbsvh == null || mbsvh.effect == null) {
                continue;
            }
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillId) {
                mbsvhs.clear();
                return true;
            }
        }
        mbsvhs.clear();
        return false;
    }

    public int getBuffSource(MapleBuffStat stat) {
        TemporaryStatBase tbs = this.getTempstats().getStatOption(stat);
        if (tbs == null) {
            return -1;
        }
        return tbs.getReason();
    }

    // public int getTrueBuffSource(MapleBuffStat stat) {
    // List<MapleBuffStatValueHolder> mbsvhs = getBuffStatValueHolders(stat);
    // if (mbsvhs == null || mbsvhs.size() <= 0) {
    // return -1;
    // }
    // MapleBuffStatValueHolder mbsvh = mbsvhs.get(0);
    // return mbsvh == null || mbsvh.effect == null ? -1 : mbsvh.effect.isSkill() ?
    // mbsvh.effect.getSourceId() : -mbsvh.effect.getSourceId();
    // }
    public void setBuffedValue(MapleBuffStat stat, int value) {
        this.getTempstats().getStatOption(stat).nOption = value;
    }

    public void setSchedule(MapleBuffStat stat, ScheduledFuture<?> sched) {
        Skill skill = SkillFactory.getSkill(this.getTempstats().getStatOption(stat).getReason());
        MapleStatEffect mse = skill.getEffect(this.getSkillLevel(skill));
        MapleBuffStatValueHolder mbsvh = this.effects.get(mse);
        if (mbsvh == null) {
            return;
        }
        if (mbsvh.schedule != null) {
            mbsvh.schedule.cancel(false);
        }
        mbsvh.schedule = sched;
    }

    public Long getBuffedStartTime(MapleBuffStat stat) {
        TemporaryStatBase a1 = this.getTempstats().getStatOption(stat);
        return (a1.getReason() != 0) ? a1.tLastUpdated : 0L;
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat stat) {
        TemporaryStatBase tbs = this.getTempstats().getStatOption(stat);
        if (tbs == null) {
            return null;
        }
        Skill skill = SkillFactory.getSkill(tbs.getReason());
        if (skill == null) {
            return null;
        }
        for (Entry<MapleStatEffect, MapleBuffStatValueHolder> entry : this.effects.entrySet()) {
            if (entry.getKey().getSourceId() == skill.getId()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[GameConstants.getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public void doDragonBlood() {
        final MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.WeaponCharge);
        if (bloodEffect == null) {
            lastDragonBloodTime = 0;
            return;
        }
        prepareDragonBlood();
        if (stats.getHp() - bloodEffect.getX() <= 1) {
            cancelBuffStats(MapleBuffStat.WeaponCharge);

        } else {
            addHP(-bloodEffect.getX());
            client.announce(EffectPacket.showBuffEffect(true, this, bloodEffect.getSourceId(),
                    UserEffectOpcode.UserEffect_Quest, getLevel(), bloodEffect.getLevel()));
            map.broadcastMessage(MapleCharacter.this, EffectPacket.showBuffEffect(false, this,
                    bloodEffect.getSourceId(), UserEffectOpcode.UserEffect_Quest, getLevel(), bloodEffect.getLevel()),
                    false);
        }
    }

    public final boolean canBlood(long now) {
        return lastDragonBloodTime > 0 && lastDragonBloodTime + 4000 < now;
    }

    private void prepareDragonBlood() {
        lastDragonBloodTime = System.currentTimeMillis();
    }

    public void doRecovery() {
        MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.Regen);
        if (bloodEffect == null) {
            bloodEffect = getStatForBuff(MapleBuffStat.Mechanic);
            if (bloodEffect == null) {
                lastRecoveryTime = 0;
            } else if (bloodEffect.getSourceId() == 35121005) {
                prepareRecovery();
                if (stats.getMp() < bloodEffect.getU()) {
                    cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
                    cancelEffectFromBuffStat(MapleBuffStat.Mechanic);
                } else {
                    addMP(-bloodEffect.getU());
                }
            }
        } else {
            prepareRecovery();
            if (stats.getHp() >= stats.getCurrentMaxHp()) {
                cancelEffectFromBuffStat(MapleBuffStat.Regen);
            } else {
                healHP(bloodEffect.getX());
            }
        }
    }

    public final boolean canRecover(long now) {
        return lastRecoveryTime > 0 && lastRecoveryTime + 5000 < now;
    }

    private void prepareRecovery() {
        lastRecoveryTime = System.currentTimeMillis();
    }

    public void startMapTimeLimitTask(int time, final MapleMap to) {
        if (time <= 0) { // jail
            time = 1;
        }
        client.announce(CField.getClock(time));
        final MapleMap ourMap = getMap();
        time *= 1000;
        mapTimeLimitTask = MapTimer.getInstance().register(() -> {
            switch (ourMap.getId()) {
                case GameConstants.JAIL:
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME))
                            .setCustomData(String.valueOf(System.currentTimeMillis()));
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData("0"); // release them!
                    break;
                case 150000001:
                    changeMap(to, to.getPortal(0));
                    break;
                default:
                    changeMap(to, to.getPortal(0));
                    break;
            }
        }, time, time);
    }

    public boolean canDOT(long now) {
        return lastDOTTime > 0 && lastDOTTime + 8000 < now;
    }

    public boolean hasDOT() {
        return dotHP > 0;
    }

    public void doDOT() {
        addHP(-(dotHP * 4));
        dotHP = 0;
        lastDOTTime = 0;
    }

    public void setDOT(int d, int source, int sourceLevel) {
        this.dotHP = d;
        addHP(-(dotHP * 4));
        map.broadcastMessage(CField.getPVPMist(id, source, sourceLevel, d));
        lastDOTTime = System.currentTimeMillis();
    }

    public void startFishingTask() {
        cancelFishingTask();
        lastFishingTime = System.currentTimeMillis();
    }

    public boolean canFish(long now) {
        return lastFishingTime > 0 && lastFishingTime + GameConstants.getFishingTime(false, isGM()) < now;
    }

    public void doFish(long now) {
        lastFishingTime = now;
        if (client == null || client.getPlayer() == null || !client.isReceiving()
                || (!haveItem(2270008, 1, false, true)) || !GameConstants.isFishingMap(getMapId()) || chair <= 0) {
            cancelFishingTask();
            return;
        }
        MapleInventoryManipulator.removeById(client, MapleInventoryType.USE, 2270008, 1, false, false);
        boolean passed = false;
        while (!passed) {
            int randval = RandomRewards.getFishingReward();
            switch (randval) {
                case 0: // Meso
                    final int money = Randomizer.rand(10, 50000);
                    gainMeso(money, true);
                    passed = true;
                    break;
                case 1: // EXP
                    final long experi = Randomizer.nextInt((int) Math.min((Math.abs(getNeededExp() / 200) + 1), 500000));
                    gainExp(experi, true, false, true);
                    passed = true;
                    break;
                default:
                    if (MapleItemInformationProvider.getInstance().itemExists(randval)) {
                        MapleInventoryManipulator.addById(client, randval, (short) 1,
                                "Fishing" + " on " + FileoutputUtil.CurrentReadable_Date());
                        passed = true;
                    }
                    break;
            }
        }
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
            mapTimeLimitTask = null;
        }
    }

    public long getNeededExp() {
        return GameConstants.getExpNeededForLevel(level);
    }

    public void cancelFishingTask() {
        lastFishingTime = 0;
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, int from) {
        registerEffect(effect, starttime, schedule, effect.getStatups(), false, effect.getDuration(), from);
    }

    public void removeIndieTempStat(MapleBuffStat buffStat, MapleStatEffect effect) {
        visibleMapObjectsLock.writeLock().lock();
        try {
            IndieTempStat its = getTempstats().aIndieTempStat.get(buffStat);
            if (its == null) {
                return;
            }
            its.getMElem().remove(effect.isSkill() ? effect.getSourceId() : -effect.getSourceId());
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public void removeTempstat(List<MapleBuffStat> buffStats, MapleStatEffect effect) {
        if (!buffStats.isEmpty() && effect != null) {
            for (MapleBuffStat buffStat : buffStats) {
                if (buffStat.isIndie() && !GameConstants.isSpecialIndieBuff(buffStat)) {
                    this.removeIndieTempStat(buffStat, effect);
                } else {
                    this.getTempstats().getStatOption(buffStat).reset();
                }
            }
        }
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule,
            Map<MapleBuffStat, Integer> statups, boolean silent, final int localDuration, final int cid) {
        if (effect.isHide()) {
            map.broadcastNONGMMessage(this, CField.removePlayerFromMap(getId()), false);
        } else if (effect.isDragonBlood()) {
            prepareDragonBlood();
        } else if (effect.isRecovery()) {
            prepareRecovery();
        } else if (effect.isBerserk()) {
            checkBerserk();
        } else if (effect.isMonsterRiding_()) {
            getMount().startSchedule();
        }
        int clonez = 0;
        for (Map.Entry<MapleBuffStat, Integer> statup : statups.entrySet()) {
            boolean isIndie = statup.getKey().isIndie();
            MapleBuffStat key = statup.getKey();
            Integer value = statup.getValue();
            if (key == MapleBuffStat.Barrier) {
                clonez = value;
            }
            if (key == MapleBuffStat.RideVehicle) {
                if (effect.getSourceId() == 5221006 && battleshipHP <= 0) {
                    battleshipHP = maxBattleshipHP(effect.getSourceId()); // copy this as well
                }
                removeFamiliar();
            }

            if (isIndie) {
                this.removeIndieTempStat(key, effect);
                this.getTempstats().updateIndieStatOption(key, this, effect, value, effect.getSourceId(),
                        (int) starttime, effect.getDuration());
            } else {
                this.getTempstats().updateStatOption(key, this, effect, effect.getSourceId(), value, starttime,
                        localDuration);
            }
            if (this.effects.containsKey(effect)) {
                this.effects.get(effect).schedule.cancel(true);
            }
            this.effects.put(effect, new MapleBuffStatValueHolder(effect, starttime, schedule));
        }
        if (clonez > 0) {
            int cloneSize = Math.max(getNumClones(), getCloneSize());
            if (clonez > cloneSize) { // how many clones to summon
                for (int i = 0; i < clonez - cloneSize; i++) { // 1-1=0
                    cloneLook();
                }
            }
        }
        if (!silent) {
            stats.recalcLocalStats(this);
        }
        if (isShowInfo()) {
            showMessage(8, new StringBuilder().append("註冊一般BUFF效果 - 當前BUFF總數： ").append(effects.size()).append(" 技能： ")
                    .append(effect.getSourceId()).toString());
        }
    }

    public int getPartyEffects() {
        List<MapleStatEffect> partyEffects = new ArrayList<>();
        for (Map.Entry<MapleStatEffect, MapleBuffStatValueHolder> entry : this.effects.entrySet()) {
            if (entry.getKey().isPartyBuff()) {
                partyEffects.add(entry.getKey());
            }
        }
        return partyEffects.size();
    }

    public List<MapleBuffStat> getBuffStats(final MapleStatEffect effect, final long startTime) {
        final List<MapleBuffStat> bstats = new ArrayList<>();
        for (MapleBuffStat buffStat : effect.statups.keySet()) {
            if (buffStat.isIndie()) {
                IndieTempStat indexStat = this.getTempstats().getIndieStatOption(buffStat);
                if (indexStat.getMElem().containsKey(effect.getSourceId())
                        && (indexStat.getMElem().get(effect.getSourceId()).getStart() == (int) startTime
                        || startTime == -1)) {
                    bstats.add(buffStat);
                }
            } else {
                TemporaryStatBase ts = this.getTempstats().getStatOption(buffStat);
                if (ts != null) {
                    if ((ts.tLastUpdated == startTime || startTime == -1) && ts.getReason() == effect.getSourceId()) {
                        bstats.add(buffStat);
                    }
                } else {
                    bstats.add(buffStat);
                }
            }
        }
        return bstats;
    }

    public boolean isBuffed(int skillId) {
        return isBuffed(SkillFactory.getSkill(skillId).getEffect(1));
    }

    public boolean isBuffed(MapleStatEffect effect) {
        return !getBuffStats(effect, -1).isEmpty();
    }

    public void cancelSummon(int summonId, MapleBuffStat stat) {
        List<MapleSummon> toRemove = new ArrayList<>();
        visibleMapObjectsLock.writeLock().lock();
        summonsLock.writeLock().lock();
        try {
            for (MapleSummon summon : summons) {
                if (((summon.getSkill() == summonId
                        || GameConstants.getLinkedAttackSkill(summon.getSkill()) == summonId)
                        && !summon.isMultiSummon()) || (stat == MapleBuffStat.Dance && summonId == 33101008)
                        || (summonId == 35121009 && summon.getSkill() == 35121011)
                        || ((summonId == 86 || summonId == 88 || summonId == 91 || summonId == 180 || summonId == 96)
                        && summon.getSkill() == summonId + 999)
                        || ((summonId == 1085 || summonId == 1087 || summonId == 1090 || summonId == 1179
                        || summonId == 1154) && summon.getSkill() == summonId - 999)) {
                    map.broadcastMessage(SummonPacket.removeSummon(summon, true));
                    map.removeMapObject(summon);
                    visibleMapObjects.remove(summon);
                    toRemove.add(summon);
                }
            }
            for (MapleSummon s : toRemove) {
                summons.remove(s);
            }
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
            summonsLock.writeLock().unlock();
        }
    }

    private boolean deregisterBuffStats(List<MapleBuffStat> stats, MapleStatEffect effect, boolean overwrite) {
        final int effectSize = this.effects.size();
        boolean clonez = false;
        List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<>(stats.size());
        for (MapleBuffStat stat : stats) {
            final MapleBuffStatValueHolder mbsvh = effects.get(effect);
            if (!overwrite) {
                this.removeTempstat(stats, effect);
                this.cancelPlayerBuffs(stats, overwrite);
                effects.remove(effect);
            }
            if (mbsvh != null) {
                boolean addMbsvh = true;
                for (MapleBuffStatValueHolder contained : effectsToCancel) {
                    if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
                        addMbsvh = false;
                    }
                }
                if (addMbsvh) {
                    effectsToCancel.add(mbsvh);
                }
            }
            if (mbsvh == null) {
                continue;
            }
            if (stat == MapleBuffStat.SurplusSupply) {
                continue;
            }
            final int summonId = mbsvh.effect.getSourceId();
            cancelSummon(summonId, stat);

            if (stat == MapleBuffStat.WeaponCharge) {
                lastDragonBloodTime = 0;
            } else if (stat == MapleBuffStat.Regen || mbsvh.effect.getSourceId() == 35121005) {
                lastRecoveryTime = 0;
            } else if (stat == MapleBuffStat.GuidedBullet || stat == MapleBuffStat.ArcaneAim) {
                if (!overwrite) {
                    linkMobs.clear();
                }
            } else if (stat == MapleBuffStat.Barrier) {
                disposeClones();
                clonez = true;
            }
        }

        int toRemoveSize = effectsToCancel.size();
        for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
            if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).isEmpty()) {
                if (cancelEffectCancelTasks.schedule != null) {
                    cancelEffectCancelTasks.schedule.cancel(false);
                    cancelEffectCancelTasks.schedule = null;
                }
            }
        }

        effectsToCancel.clear();
        boolean ok = effectSize - effects.size() == toRemoveSize;

        if (isShowInfo()) {
            showMessage(8,
                    new StringBuilder().append("取消注冊的BUFF效果 - 取消前BUFF總數: ").append(effectSize).append(" 當前BUFF總數 ")
                            .append(effects.size()).append(" 取消的BUFF數量: ").append(toRemoveSize).append(" 是否相同: ")
                            .append(ok).toString());
        }
        return clonez;
    }

    /**
     * @param effect
     * @param overwrite when overwrite is set no data is sent and all the
     * Buffstats in the StatEffect are deregistered
     * @param startTime
     */
    public void cancelEffect(final MapleStatEffect effect, final boolean overwrite, final long startTime) {
        cancelEffect(effect, overwrite, startTime, effect.getStatups());
    }

    public void cancelEffect(final MapleStatEffect effect, final boolean overwrite, final long startTime,
            Map<MapleBuffStat, Integer> statups) {
        if (effect == null) {
            return;
        }
        MapleBuffStatValueHolder mbvh = this.effects.get(effect);
        if (mbvh == null) {
            return;
        }
        List<MapleBuffStat> buffstats = null;
        if (overwrite) {
            buffstats = new ArrayList<>(statups.keySet());
        } else {
            if (effect.isPartyBuff() && getParty() != null) {
                int from = getParty().cancelPartyBuff(effect.getSourceId(), getId());
                if (from > 0) {
                    getParty().getPartyBuffs(from);
                    MapleCharacter chr;
                    if (from != getId()) {
                        chr = getOnlineCharacterById(from);
                    } else {
                        chr = this;
                    }
                    if (chr != null) {
                        MapleStatEffect.applyPassiveBless(chr);
                    }
                }
            }
            buffstats = getBuffStats(effect, startTime);
        }

        // if (effect.isInfinity() && getBuffedValue(MapleBuffStat.Infinity) != null) {
        // //before
        // int duration = Math.max(effect.getDuration(), effect.alchemistModifyVal(this,
        // effect.getDuration(), false));
        // final long start = getBuffedStartTime(MapleBuffStat.Infinity);
        // duration += (int) ((start - System.currentTimeMillis()));
        // if (duration > 0) {
        // final int neworbcount = getBuffedValue(MapleBuffStat.Infinity) +
        // effect.getDamage();
        // final Map<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class
        // );
        // stat.put(MapleBuffStat.Infinity, neworbcount);
        // setBuffedValue(MapleBuffStat.Infinity, neworbcount);
        // client.announce(BuffPacket.giveBuff(effect.getSourceId(), duration, stat,
        // effect, this));
        // addHP((int) (effect.getHpR() * this.stats.getCurrentMaxHp()));
        // addMP((int) (effect.getMpR() * this.stats.getCurrentMaxMp()));
        // setSchedule(MapleBuffStat.Infinity, BuffTimer.getInstance().schedule(new
        // CancelEffectAction(this, effect, start, stat),
        // effect.alchemistModifyVal(this, 4000, false)));
        // return;
        // }
        // }
        //
        // if (!overwrite && buffstats.contains(MapleBuffStat.AntiMagicShell)) {
        // switch (effect.getSourceId()) {
        // case 2111011: //元素適應(火、毒)
        // Integer val = getBuffedValue(MapleBuffStat.AntiMagicShell);
        // if (val != null && val > 0 && effect.makeChanceResult()) {
        // effect.applyTo(this);
        // return;
        // }
        // if (val != null && effect.statups.get(MapleBuffStat.AntiMagicShell) !=
        // val.intValue() && effect.getCooldown(this) > 0) {
        // addCooldown(effect.getSourceId(), System.currentTimeMillis(),
        // effect.getCooldown(this) * 1000);
        // }
        // break;
        // case 27111004: // 魔力護盾
        // val = getBuffedValue(MapleBuffStat.AntiMagicShell);
        // if (val != null && val > 1) {
        // effect.applyTo(this);
        // return;
        // }
        // if (val != null && effect.statups.get(MapleBuffStat.AntiMagicShell) !=
        // val.intValue() && effect.getCooldown(this) > 0) {
        // addCooldown(effect.getSourceId(), System.currentTimeMillis(),
        // effect.getCooldown(this) * 1000);
        // }
        // break;
        //// case 2211012: //元素適應(雷、冰)
        //// if ( this.getTempstats().getStatOption(MapleBuffStat.BuffLimit)
        // this.effects.get(effect). == -1) {
        //// effect.applyTo(this);
        //// return;
        //// } else if (effect.getCooldown(this) > 0) {
        //// addCooldown(effect.getSourceId(), System.currentTimeMillis(),
        // effect.getCooldown(this) * 1000);
        //// }
        //// break;
        // }
        // }
        //
        // if (buffstats.size() <= 0) {
        //// return;
        // }
        // if (!overwrite && effect.isBerserk() && killCount != 0) {
        // setKillCount(0);
        // getStat().setHp((short) 0, this);
        // getStat().setMp((short) 0, this);
        // updateSingleStat(MapleStat.HP, this.getStat().getHp());
        // updateSingleStat(MapleStat.MP, this.getStat().getMp());
        // }
        //
        final boolean clonez = deregisterBuffStats(buffstats, effect, overwrite);
        //
        if (effect.isMagicDoor()) {
            // remove for all on maps
            if (!getDoors().isEmpty()) {
                removeDoor();
                silentPartyUpdate();
            }
        } else if (effect.isMechDoor()) {
            if (!getMechDoors().isEmpty()) {
                removeMechDoor();
            }
        } else if (effect.isMonsterRiding_()) {
            getMount().cancelSchedule();
        } else if (effect.isMonsterRiding()) {
            cancelEffectFromBuffStat(MapleBuffStat.Mechanic);
        } else if (effect.isAranCombo()) {
            combo = 0;
        }

        if (!overwrite) {
            if (effect.isHide()
                    && client.getChannelServer().getPlayerStorage().getCharacterById(this.getId()) != null) { // Wow
                // this is
                // so
                // fking
                // hacky...
                map.broadcastMessage(this, CField.spawnPlayerMapobject(this), false);
                map.broadcastMessage(this, CField.getEffectSwitch(getId(), getEffectSwitch()), true);
                for (final WeakReference<MapleCharacter> chr : clones) {
                    if (chr.get() != null) {
                        map.broadcastMessage(chr.get(), CField.spawnPlayerMapobject(chr.get()), false);
                        map.broadcastMessage(this, CField.getEffectSwitch(getId(), getEffectSwitch()), true);
                    }
                }
            }
        }
        if (effect.getSourceId() == 35121013 && !overwrite) { // when siege 2 deactivates, missile re-activates
            SkillFactory.getSkill(35121005).getEffect(getTotalSkillLevel(35121005)).applyTo(this);
        }
        if (effect.getSourceId() == 40011288 && !overwrite) { // 拔刀術狀態切換
            short 剣気 = getJianQi();
            剣気 = (short) Math.ceil(剣気 * 0.70);
            setJianQi(剣気);
            MapleStatEffect eff = SkillFactory.getSkill(40011291)
                    .getEffect((int) Math.min(5, Math.max(1, Math.floor(剣気 / 200))));
            dispelBuff(40011292);
            eff.applyTo(this);
        }
        if (effect.getSourceId() == 13110022) {
            dispelBuff(13120003);
            dispelBuff(13110022);
        }
        if (effect.getSourceId() == 42101002 && !overwrite) { // 影朋‧花狐
            if (getHaku() != null) {
                getHaku().setFigureHaku(false);
                getMap().broadcastMessage(this, CField.hakuChange(getHaku()), true);
                getMap().broadcastMessage(this, CField.figureHakuRemove(getHaku().getOwner()), true);
            }
        }
        if (effect.getSourceId() == 80000329 && isInvincible() && !overwrite) {
            setInvincible(false);
            dropMessage(6, "無敵模式關閉");
        }
        if (!overwrite && effect.isPartyBuff() && getParty() == null && getTotalSkillLevel(2300009) > 0) { // 祝福福音
            if (getPartyEffects() > 0) {
                MapleStatEffect.applyPassiveBless(this);
            } else {
                cancelEffectFromBuffStat(MapleBuffStat.BlessEnsenble);
            }
        }
        Skill skill = SkillFactory.getSkill(effect.getSourceId());
        if (skill != null && skill.isChargeSkill() && !overwrite) {
            if (!skillisCooling(effect.getSourceId()) && effect.getCooldown(this) > 0) {
                addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(this) * 1000);
            }
            getMap().broadcastMessage(this, CField.skillCancel(this, effect.getSourceId()), false);
        }
        if (!clonez) {
            for (WeakReference<MapleCharacter> chr : clones) {
                if (chr.get() != null) {
                    chr.get().cancelEffect(effect, overwrite, startTime);
                }
            }
        }
        if (isShowInfo()) {
            dropMessage(5, new StringBuilder().append("取消一般BUFF效果 - 當前BUFF總數：").append(this.effects.size())
                    .append(" 技能：").append(effect.getSourceId()).toString());
        }
    }

    public void cancelBuffStats(MapleBuffStat... stat) {
        List<MapleBuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList, null, false);
        cancelPlayerBuffs(buffStatList, false);
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh == null || mbsvh.effect == null) {
                continue;
            }
            if (this.getTempstats().getStatOption(stat).getReason() == mbsvh.effect.getSourceId()) {
                cancelEffect(mbsvh.effect, false, -1L);
            }
        }
        allBuffs.clear();
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat, int from) {
        List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh == null || mbsvh.effect == null) {
                continue;
            }
            cancelEffect(mbsvh.effect, false, -1L);
        }
        allBuffs.clear();
    }

    private void cancelPlayerBuffs(List<MapleBuffStat> buffstats, boolean overwrite) {
        boolean write = client != null && client.getChannelServer() != null
                && client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
        if (write) {
            if (!overwrite && (buffstats.contains(MapleBuffStat.GuidedBullet)
                    || buffstats.contains(MapleBuffStat.ArcaneAim))) {
                linkMobs.clear();
            }
            if (buffstats.contains(MapleBuffStat.SurplusSupply)) { // 傑諾能量不能被取消
                buffstats.remove(MapleBuffStat.SurplusSupply);
            }
            if (buffstats.size() <= 0) {
                return;
            }
            if (isShowInfo()) {
                dropMessage(5, new StringBuilder().append("取消BUFF效果 - 發送封包 - 是否註冊BUFF時：").append(overwrite).toString());
            }
            if (overwrite) {
                return;
            }
            stats.recalcLocalStats(this);
            client.announce(BuffPacket.cancelBuff(buffstats, this));
            map.broadcastMessage(this, BuffPacket.onResetTemporaryStat(getId(), buffstats), false);

            if (!isClone()) {
                List<WeakReference<MapleCharacter>> clones = getClones();
                for (WeakReference<MapleCharacter> clone : clones) {
                    if (clone.get() != null) {
                        clone.get().getMap().broadcastMessage(clone.get(), BuffPacket.onResetTemporaryStat(clone.get().getId(), buffstats), false);
                    }
                }
            }
        }
    }

    public void dispel() {
        if (!isHidden()) {
            List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
            for (MapleBuffStatValueHolder mbsvh : allBuffs) {
                if (mbsvh == null || mbsvh.effect == null) {
                    continue;
                }
                if (mbsvh.effect.isSkill() && mbsvh.schedule != null && !mbsvh.effect.isMorph()
                        && !mbsvh.effect.isGmBuff() && !mbsvh.effect.isMonsterRiding() && !mbsvh.effect.isMechChange()
                        && !mbsvh.effect.isEnergyCharge() && !mbsvh.effect.isAranCombo()
                        && mbsvh.effect.getSourceId() != 30020232 // 傑諾蓄能系統
                        && !mbsvh.effect.isNotRemoved()) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            }
        }
    }

    public void dispelSkill(int skillid) {
        List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh == null || mbsvh.effect == null) {
                continue;
            }
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public void dispelSummons() {
        List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh == null || mbsvh.effect == null) {
                continue;
            }
            if (mbsvh.effect.getSummonMovementType() != null) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void dispelSummons(int skillid) {
        List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh == null || mbsvh.effect == null) {
                continue;
            }
            if (mbsvh.effect.getSummonMovementType() != null && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public void dispelBuff(int skillid) {
        List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh == null || mbsvh.effect == null) {
                continue;
            }
            if (mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public void cancelAllBuffs_() {
        this.effects.clear();
        this.getTempstats().aIndieTempStat.clear();
    }

    public void cancelAllBuffs() {
        List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
        }
    }

    public void cancelMorphs() {
        List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh == null || mbsvh.effect == null) {
                continue;
            }
            switch (mbsvh.effect.getSourceId()) {
                case 61111008:
                case 61120008:
                case 61121053:
                case 5111005:
                case 5121003:
                case 15111002:
                case 13111005:
                    return; // Since we can't have more than 1, save up on loops
            }
            if (mbsvh.effect.isMorph()) {
                disposeClones();
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public int getMorphState() {
        List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect == null) {
                continue;
            }
            if (mbsvh.effect.isMorph()) {
                return mbsvh.effect.getSourceId();
            }
        }
        return -1;
    }

    public Map<MapleBuffStat, TemporaryStatBase> gertAllBuffs() {
        return this.getTempstats().getStats();
    }

    public void cancelMagicDoor() {
        final List<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(this.effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh == null || mbsvh.effect == null) {
                continue;
            }
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public final void handleEnergyCharge(int skillid, int targets) {
        Skill echskill = SkillFactory.getSkill(skillid);
        int skilllevel = getTotalSkillLevel(echskill);
        if (skilllevel > 0) {
            MapleStatEffect echeff = echskill.getEffect(skilllevel);
            if (targets > 0) {
                if (skillid != 15001022) {
                    if (getBuffedValue(MapleBuffStat.EnergyCharged) == null) {
                        echeff.applyEnergyBuff(this, true, targets);
                    } else {
                        Integer energyLevel = getBuffedValue(MapleBuffStat.EnergyCharged);

                        if (energyLevel < 10000) {
                            energyLevel = energyLevel + echeff.getX() * targets;

                            this.client.announce(CField.EffectPacket.showBuffEffect(true, this, skillid,
                                    UserEffectOpcode.UserEffect_SkillUseBySummoned, getLevel(), skilllevel));
                            this.map.broadcastMessage(this,
                                    CField.EffectPacket.showBuffEffect(false, this, skillid,
                                            UserEffectOpcode.UserEffect_SkillUseBySummoned, getLevel(), skilllevel),
                                    false);

                            if (energyLevel >= 10000) {
                                energyLevel = 10001;
                            }
                            final Map<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                            stat.put(MapleBuffStat.EnergyCharged, energyLevel);
                            setBuffedValue(MapleBuffStat.EnergyCharged, energyLevel);
                            this.client.announce(CWvsContext.BuffPacket.giveBuff(0, 0, stat, echeff, this));
                        }
                        // else if (energyLevel == 10000) {
                        // setBuffedValue(MapleBuffStat.EnergyCharged, 10000);
                        // echeff.applyEnergyBuff(this, false, targets);
                        // }
                    }
                } else if (getBuffedValue(MapleBuffStat.CygnusElementSkill) == null) {
                    return;
                } else {
                    int MaxBuff = 1;
                    if (getTotalSkillLevel(15000023) > 0) {
                        MaxBuff += 1;
                    }
                    if (getTotalSkillLevel(15100025) > 0) {
                        MaxBuff += 1;
                    }
                    if (getTotalSkillLevel(15110026) > 0) {
                        MaxBuff += 1;
                    }
                    if (getTotalSkillLevel(15120008) > 0) {
                        MaxBuff += 1;
                    }
                    if (this.attackCombo < MaxBuff) {
                        this.attackCombo += 1;
                    }
                    final Map<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.IgnoreTargetDEF, attackCombo * 5);
                    long starttime = System.currentTimeMillis();
                    registerEffect(echeff, starttime, null, getId());
                    client.announce(CWvsContext.BuffPacket.giveBuff(15001022, 30000, stat, echeff, this));
                }
            }
        }
    }

    public final void handleBattleshipHP(int damage) {
        if (damage < 0) {
            final MapleStatEffect effect = getStatForBuff(MapleBuffStat.RideVehicle);
            if (effect != null && effect.getSourceId() == 5221006) {
                battleshipHP += damage;
                client.announce(CField.skillCooldown(5221999, battleshipHP / 10));
                if (battleshipHP <= 0) {
                    battleshipHP = 0;
                    giveCoolDowns(5221006, System.currentTimeMillis(), effect.getCooldown(this) * 1000);
                    cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
                }
            }
        }
    }

    public final void handleOrbgain() {
        int orbcount = getBuffedValue(MapleBuffStat.ComboCounter);
        Skill combo = SkillFactory.getSkill(1101013);
        Skill advcombo = SkillFactory.getSkill(1120003);

        int advComboSkillLevel = getTotalSkillLevel(advcombo);
        MapleStatEffect ceffect;
        if (advComboSkillLevel > 0) {
            ceffect = advcombo.getEffect(advComboSkillLevel);
        } else if (getTotalSkillLevel(combo) > 0) {
            ceffect = combo.getEffect(getTotalSkillLevel(combo));
        } else {
            return;
        }

        if (orbcount < ceffect.getX() + 1) {
            int neworbcount = orbcount + 1;
            if ((advComboSkillLevel > 0) && (ceffect.makeChanceResult()) && (neworbcount < ceffect.getX() + 1)) {
                neworbcount++;

            }

            Map<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
            stat.put(MapleBuffStat.ComboCounter, neworbcount);
            setBuffedValue(MapleBuffStat.ComboCounter, neworbcount);
            int duration = ceffect.getDuration();
            duration += (int) (getBuffedStartTime(MapleBuffStat.ComboCounter) - System.currentTimeMillis());

            client.announce(CWvsContext.BuffPacket.giveBuff(combo.getId(), duration, stat, ceffect, this));
            map.broadcastMessage(this, CWvsContext.BuffPacket.giveForeignBuff(this, stat, ceffect), false);
        }
    }

    public void handleOrbconsume(int howmany) {
        Skill normalcombo = SkillFactory.getSkill(1101013);
        if (getTotalSkillLevel(normalcombo) <= 0) {
            return;
        }
        MapleStatEffect ceffect = getStatForBuff(MapleBuffStat.ComboCounter);
        if (ceffect == null) {
            return;

        }
        Map<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
        stat.put(MapleBuffStat.ComboCounter, Math.max(1, getBuffedValue(MapleBuffStat.ComboCounter) - howmany));
        setBuffedValue(MapleBuffStat.ComboCounter, Math.max(1, getBuffedValue(MapleBuffStat.ComboCounter) - howmany));
        int duration = ceffect.getDuration();
        duration += (int) ((getBuffedStartTime(MapleBuffStat.ComboCounter) - System.currentTimeMillis()));

        client.announce(BuffPacket.giveBuff(normalcombo.getId(), duration, stat, ceffect, this));
        map.broadcastMessage(this, BuffPacket.giveForeignBuff(this, stat, ceffect), false);
    }

    public void handleFox(int mobid, int skillid) {
        Skill fox = SkillFactory.getSkill(25100009); // 小狐仙精通 - 25100010
        Skill adv_fox = SkillFactory.getSkill(25120110); // 火狐精通 - 25120115

        int SkillLevel = getTotalSkillLevel(skillid == 25100009 ? fox : adv_fox);
        MapleStatEffect fox_effect = skillid == 25100009 ? fox.getEffect(SkillLevel) : adv_fox.getEffect(SkillLevel);
        if (Randomizer.isSuccess(fox_effect.getProb())) {
            client.announce(SkillPacket.隱月小狐仙(this.getId(), 25100010, 13, false));
        }
    }

    public MapleSummon getSummonBySkill(int skillId) {
        MapleSummon ret = null;
        for (MapleSummon summon : this.getSummonsReadLock()) {
            if (summon.getSkill() == skillId) {
                ret = summon;
            }
        }
        this.unlockSummonsReadLock();
        return ret;
    }

    public short getAttackCombo() {
        return this.attackCombo;
    }

    public void setAttackCombo(short value) {
        this.attackCombo = value;
    }

    public void givePPoint(int skillid) {
        MapleStatEffect eff = null;
        if (skillid != 0) {
            eff = SkillFactory.getSkill(skillid).getEffect(getTotalSkillLevel(skillid));
        }
        int MaxPPoint = 0;
        if (eff != null) {
            if (eff.getPPCon() > 0) {
                PPoint -= eff.getPPCon();
            } else if (eff.getPPRecovery() > 0) {
                PPoint += eff.getPPRecovery();
            } else if (skillid == 142121008) {
                PPoint += 15;
            }
        }

        switch (getJob()) {
            case 14200:
                MaxPPoint = 10;
                break;
            case 14210:
                MaxPPoint = 15;
                break;
            case 14211:
                MaxPPoint = 20;
                break;
            case 14212:
                MaxPPoint = 30;
                break;
        }
        switch (getMapId()) {// 創角地圖時全開
            case 331001110:
            case 331001120:
            case 331001130:
            case 331002300:
            case 331002400:
                MaxPPoint = 30;
                break;
        }
        if (PPoint < 0) {
            PPoint = 0;
        }
        if (MaxPPoint < PPoint) {
            PPoint = MaxPPoint;
        }
        client.announce(JobPacket.KinesisPacket.givePsychicPoint(getJob(), PPoint));
    }

    public void silentEnforceMaxHpMp() {
        stats.setMp(stats.getMp(), this);
        stats.setHp(stats.getHp(), true, this);
    }

    public void enforceMaxHpMp() {
        Map<MapleStat, Long> statups = new EnumMap<>(MapleStat.class);
        if (stats.getMp() > stats.getCurrentMaxMp()) {
            stats.setMp(stats.getMp(), this);
            statups.put(MapleStat.MP, (long) stats.getMp());
        }
        if (stats.getHp() > stats.getCurrentMaxHp()) {
            stats.setHp(stats.getHp(), this);
            statups.put(MapleStat.HP, (long) stats.getHp());
        }
        if (statups.size() > 0) {
            client.announce(CWvsContext.updatePlayerStats(statups, this));
        }
    }

    public MapleMap getMap() {
        return map;
    }

    public MonsterBook getMonsterBook() {
        return monsterbook;
    }

    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public byte getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public final String getBlessOfFairyOrigin() {
        return this.BlessOfFairy_Origin;
    }

    public final String getBlessOfEmpressOrigin() {
        return this.BlessOfEmpress_Origin;
    }
    // the sp isnt fixed oh hold on

    public final short getLevel() {
        return level;
    }

    public final int getFame() {
        return fame;
    }

    public final int getFallCounter() {
        return fallcounter;
    }

    public final MapleClient getClient() {
        return client;
    }

    public final void setClient(final MapleClient client) {
        this.client = client;
    }

    public long getExp() {
        return exp;
    }

    public int getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp[GameConstants.getSkillBookByJob(job)]; // default
    }

    public int getRemainingSp(final int skillbook) {
        return remainingSp[skillbook];
    }

    public int[] getRemainingSps() {
        return remainingSp;
    }

    public int getRemainingSpSize() {
        int ret = 0;
        for (int i = 0; i < remainingSp.length; i++) {
            if (remainingSp[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public int getRemainingHSp(final int mode) {
        return remainingHSp[mode];
    }

    public int getMaxHAp() {
        return GameConstants.getMaxHAp(level);
    }

    public int getRemainingHAp() {
        int maxHap = this.getMaxHAp();
        for (Entry<Skill, SkillEntry> entry : this.getSkills().entrySet()) {
            Skill skill = entry.getKey();
            if (skill.isHyperStat()) {
                int skillLevel = entry.getValue().skillLevel;
                if (skillLevel > skill.getMaxLevel()) {
                    skillLevel = skill.getMaxLevel();
                }
                maxHap -= GameConstants.getHyperStatReqAccumulateAp(skillLevel);
            }
        }
        return maxHap;
    }

    public int[] getRemainingHSps() {
        return remainingHSp;
    }

    public short getHpApUsed() {
        return hpApUsed;
    }

    public short getMpApUsed() {
        return mpApUsed;
    }

    public boolean isHidden() {
        return getBuffSource(MapleBuffStat.DarkSight) / 1000000 == 9;
    }

    public void setHpApUsed(short hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    public void setMpApUsed(short mpApUsed) {
        this.mpApUsed = mpApUsed;
    }

    @Override
    public byte getSkinColor() {
        return skinColor;
    }

    @Override
    public byte getSecondSkinColor() {
        return skinColor;
    }

    public void setSkinColor(byte skinColor) {
        this.skinColor = skinColor;
    }

    @Override
    public short getJob() {
        return job;
    }

    public String getJobName(short id) {
        return MapleJob.getName(MapleJob.getById(id));
    }

    @Override
    public byte getGender() {
        return gender;
    }

    @Override
    public byte getSecondGender() {
        if (MapleJob.is神之子(getJob())) {
            return 1;
        }
        return gender;
    }

    @Override
    public int getHair() {
        return hair;
    }

    @Override
    public int getSecondHair() {
        if (MapleJob.is天使破壞者(getJob())) {
            return 37141; // default id here
        } else if (MapleJob.is神之子(getJob())) {
            return 37623;
        }
        return hair;
    }

    @Override
    public int getFace() {
        return face;
    }

    @Override
    public int getSecondFace() {
        if (MapleJob.is天使破壞者(getJob())) {
            return 21173; // default id here
        } else if (MapleJob.is神之子(getJob())) {
            return 21290;
        }
        return face;
    }

    @Override
    public int getFaceMarking() {
        return faceMarking;
    }

    public void setFaceMarking(int mark) {
        this.faceMarking = mark;
    }

    @Override
    public int getEars() {
        return ears;
    }

    public void setEars(int ears) {
        this.ears = ears;
        equipChanged();
    }

    @Override
    public int getTail() {
        return tail;
    }

    public void setTail(int tail) {
        this.tail = tail;
        equipChanged();
    }

    @Override
    public int getElf() {
        String elf = getOneInfo(GameConstants.精靈耳朵, "sw");
        if (elf == null) {
            return 0;
        } else {
            return Integer.valueOf(elf);
        }
    }

    public void setElf(int elf) {
        updateInfoQuest(GameConstants.精靈耳朵, "sw=" + elf);
        equipChanged();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public void setFace(int face) {
        this.face = face;
        equipChanged();
    }

    public void setSecondHair(int hair) {
        this.secondHair = hair;
    }

    public void setSecondFace(int face) {
        this.secondFace = face;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setFallCounter(int fallcounter) {
        this.fallcounter = fallcounter;
    }

    public Point getOldPosition() {
        return old;
    }

    public void setOldPosition(Point x) {
        this.old = x;
    }

    public void setRemainingAp(int remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp[GameConstants.getSkillBookByJob(job)] = remainingSp; // default
    }

    public void setRemainingSp(int remainingSp, final int skillbook) {
        this.remainingSp[skillbook] = remainingSp;
    }

    public void setRemainingHSp(int mode, int amount) {
        this.remainingHSp[mode] = amount;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public void setInvincible(boolean invinc) {
        invincible = invinc;
        if (invincible) {
            SkillFactory.getSkill(80000329).getEffect(1).applyTo(this);
        } else {
            dispelBuff(80000329);
        }
    }

    public boolean isInvincible() {
        return invincible;
    }

    public CheatTracker getCheatTracker() {
        return anticheat;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public void addFame(int famechange) {
        this.fame += famechange;
        getTrait(MapleTraitType.charm).addLocalExp(famechange);
    }

    public void updateFame() {
        updateSingleStat(MapleStat.FAME, this.fame);
    }

    public void changeMapBanish(final int mapid, final String portal, final String msg) {
        dropMessage(5, msg);
        final MapleMap tomap = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(tomap, tomap.getPortal(portal));
    }

    public void changeMap(final MapleMap to, final Point pos) {
        changeMapInternal(to, pos, CField.getWarpToMap(to, 0x80, this), null);
    }

    public void changeMap(final MapleMap to) {
        changeMapInternal(to, to.getPortal(0).getPosition(), CField.getWarpToMap(to, 0, this), to.getPortal(0));
    }

    public void changeMap(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), CField.getWarpToMap(to, pto.getId(), this), null);
    }

    public void changeMapPortal(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), CField.getWarpToMap(to, pto.getId(), this), pto);
    }

    private void changeMapInternal(final MapleMap to, final Point pos, byte[] warpPacket, final MaplePortal pto) {
        if (to == null) {
            return;
        }
        // if (getAntiMacro().inProgress()) {
        // dropMessage(5, "You cannot use it in the middle of the Lie Detector Test.");
        // return;
        // }
        final int nowmapid = map.getId();
        if (eventInstance != null) {
            eventInstance.changedMap(this, to.getId());
        }
        final boolean pyramid = pyramidSubway != null;
        if (map.getId() == nowmapid) {
            client.announce(warpPacket);
            final boolean shouldChange = !isClone()
                    && client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
            final boolean shouldState = map.getId() == to.getId();
            if (shouldChange && shouldState) {
                to.setCheckStates(false);
            }
            map.removePlayer(this);
            if (ServerConstants.AntiKS) {
                try {
                    MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(732648172));
                    boolean antiks;
                    if (stat == null || stat.getCustomData() == null) {
                        antiks = false;
                        stat.setCustomData(antiks + ";" + 0);
                    } else {
                        String[] statss = stat.getCustomData().split(";");
                        try {
                            antiks = Boolean.parseBoolean(statss[0]);
                        } catch (Exception ex) {
                            antiks = false;
                        }
                        List<MapleMonster> monsters = map.getAllMonster();
                        for (MapleMapObject mmo : monsters) {
                            MapleMonster m = (MapleMonster) mmo;
                            if (m.getBelongsTo() == getId()) {
                                stat.setCustomData(antiks + ";" + (Integer.parseInt(statss[1]) - 1));
                                m.expireAntiKS();
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
            if (shouldChange) {
                map = to;
                setStance(0);
                setPosition(new Point(pos.x, pos.y - 50));
                to.addPlayer(this);
                stats.relocHeal(this);
                if (shouldState) {
                    to.setCheckStates(true);
                }
            }
        }
        if (pyramid && pyramidSubway != null) { // checks if they had pyramid before AND after changing
            pyramidSubway.onChangeMap(this, to.getId());
        }
    }

    public void cancelChallenge() {
        if (challenge != 0 && client.getChannelServer() != null) {
            final MapleCharacter chr = client.getChannelServer().getPlayerStorage().getCharacterById(challenge);
            if (chr != null) {
                chr.dropMessage(6, getName() + " has denied your request.");
                chr.setChallenge(0);
            }
            dropMessage(6, "Denied the challenge.");
            challenge = 0;
        }
    }

    public void leaveMap(MapleMap map) {
        controlledLock.writeLock().lock();
        visibleMapObjectsLock.writeLock().lock();
        try {
            for (MapleMonster mons : controlled) {
                if (mons != null) {
                    mons.setController(null);
                    mons.setControllerHasAggro(false);
                    map.updateMonsterController(mons);
                }
            }
            controlled.clear();
            visibleMapObjects.clear();
        } finally {
            controlledLock.writeLock().unlock();
            visibleMapObjectsLock.writeLock().unlock();
        }
        if (chair != 0) {
            chair = 0;
        }
        clearLinkMid();
        cancelFishingTask();
        cancelChallenge();
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        cancelMapTimeLimitTask();
        if (getTrade() != null) {
            MapleTrade.cancelTrade(getTrade(), client, this);
        }
        // antiMacro.reset(); // reset lie detector
    }

    public void changeJob(short newJob) {
        try {
            cancelEffectFromBuffStat(MapleBuffStat.ShadowPartner);
            if (MapleJob.is影武者(newJob)) {
                subcategory = 1;
            } else if (MapleJob.is重砲指揮官(newJob)) {
                subcategory = 2;
            } else if (MapleJob.盜賊.getId() != newJob) {
                subcategory = 0;
            }

            if (MapleJob.isBeginner(job)) {
                resetStats(4, 4, 4, 4);
            }
            this.job = newJob;
            updateSingleStat(MapleStat.JOB, newJob);

            this.forceAtomInfo.reset();

            if (!MapleJob.isBeginner(newJob) && newJob != MapleJob.隱忍.getId() && !MapleJob.is神之子(newJob)
                    && !MapleJob.is幻獸師(newJob)) {
                if (GameConstants.isSeparatedSp(newJob)) {
                    if (MapleJob.is幻影俠盜(job)) {
                        client.announce(PhantomPacket.updateCardStack(0));
                        this.resetRunningDarks();
                    }
                    int changeSp = 5;
                    if (!MapleJob.is影武者(newJob) && MapleJob.getJobGrade(newJob) >= 4) {
                        changeSp = 3;
                    } else if (newJob == MapleJob.影武者.getId()) {
                        changeSp = 3;
                    } else if (MapleJob.is凱撒(newJob)) {
                        changeSp = 3;
                    } else if (MapleJob.is龍魔導士(newJob) && MapleJob.getJobGrade(newJob) == 2
                            && MapleJob.getJobGrade(newJob) == 3) {
                        changeSp = 2;
                    } else if (MapleJob.getJobGrade(newJob) == 1) {
                        if (MapleJob.is夜光(newJob)) {
                            changeSp = 3;
                        }
                    }

                    // 職業技能補滿
                    if (newJob == MapleJob.中忍.getId()) {
                        changeSp += 15;
                    } else if (newJob == MapleJob.上忍.getId()) {
                        changeSp += 5;
                    } else if (newJob == MapleJob.天使破壞者2轉.getId()) {
                        changeSp += 2;
                    } else if (newJob == MapleJob.天使破壞者3轉.getId()) {
                        changeSp += 5;
                    } else if (newJob == MapleJob.劍豪1轉.getId()) {
                        changeSp += 1;
                    } else if (newJob == MapleJob.陰陽師1轉.getId()) {
                        changeSp += 1;
                    }

                    if (changeSp > 0) {
                        remainingSp[GameConstants.getSkillBookByJob(newJob)] += changeSp;
                        client.announce(InfoPacket.getSPMsg((byte) changeSp, newJob));
                    }
                } else {
                    remainingSp[GameConstants.getSkillBookByJob(newJob)]++;
                    if (MapleJob.getJobGrade(newJob) >= 4) {
                        remainingSp[GameConstants.getSkillBookByJob(newJob)] += 2;
                    }

                }
                if (MapleJob.getJobGrade(newJob) == 1 && level >= 10 && MapleJob.is龍魔導士(newJob)) {
                    MapleQuest.getInstance(22100).forceStart(this, 0, null);
                    MapleQuest.getInstance(22100).forceComplete(this, 0);
                    client.announce(CScriptMan.getEvanTutorial("UI/tutorial/evan/14/0"));
                    dropMessage(5, "感覺小龍有話要說，按下小龍後對話看看。");
                }
                if (MapleJob.getJobGrade(newJob) == 2 && level >= 30 && MapleJob.is龍魔導士(newJob)) {
                    client.announce(CField.onAddPopupSay(1013209, 2000, "#face1#主人！現在可以使用[融合技能]了！ ..", ""));
                    client.announce(CField.onAddPopupSay(1013209, 2000, "#face1#我們兩個同心協力，就能更輕鬆的對付敵人。", ""));
                    client.announce(CField.onAddPopupSay(0, 2000, "融合技能？要怎麼做才能使用？", ""));
                    client.announce(CField.onAddPopupSay(1013209, 2000, "#face1#不難。當我要使用技術時，主人再助我一臂之力就行了，嗯～舉例來說…", ""));
                    client.announce(CField.onAddPopupSay(1013209, 2000,
                            "#face1#當我要使用[龍之捷]時，如果主人能使用[風之環]的話，我就能利用那個力量使用[風之捷]。", ""));
                    client.announce(CField.onAddPopupSay(1013209, 2000, "#face1#就是將我的力量跟主人的力量合併。", ""));
                    client.announce(CField.onAddPopupSay(0, 2000, "啊哈！！？？!", ""));
                    client.announce(CField.onAddPopupSay(1013209, 2000, "#face1#主人…原來你還是沒聽懂啊？ ", ""));
                    client.announce(CField.onAddPopupSay(1013209, 2000, "#face1#別擔心。不是有人說百聞不如一見嗎?先試一次，馬上就會明白的。", ""));
                    client.announce(
                            CField.onAddPopupSay(1013209, 2000, "#face1#然後，以後我跟主人的力量變強後，就能學到更多元的[融合技能]，一起努力練習吧！ ", ""));
                    client.announce(CField.onAddPopupSay(0, 2000, "我知道了。寶貝龍！相信我吧！..", ""));
                    client.announce(CField.onAddPopupSay(9010000, 6000,
                            "#b[說明] 2轉#k\n" + "達成等級30而可以進行 #b[2轉]#k了呢！\n" + "\n" + "#r完成[轉職]#k 任務來達成第二次轉職吧！\n" + " ",
                            ""));
                }
                updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
            }
            // 未知是否需要
            if (MapleJob.getJobGrade(newJob) >= 3 && level >= 60) { // 3rd job or higher. lucky for evans who get 80,
                // 100, 120, 160 ap...
                remainingAp += 5;
                updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
            }

            // 處理轉職增加血魔上限開始
            int maxhp = stats.getMaxHp(), maxmp = stats.getMaxMp();

            switch (job) {
                case 0: // 初心者
                case 1000:// 皇家騎士團
                case 3000:// 末日反抗軍
                    break;
                case 800: // 管理員
                case 900: // GM
                case 910: // 超級GM
                case 10000:// 神之子-新手
                case 10100:// 神之子
                case 10110:// 神之子
                case 10111:// 神之子
                case 10112:// 神之子
                    break;

                case 3101:// 惡魔復仇者
                    maxhp += Randomizer.rand(300, 350);
                    break;
                case 3120:// 惡魔復仇者
                    maxhp += Randomizer.rand(400, 450);
                    break;
                case 3121:// 惡魔復仇者
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 3122:// 惡魔復仇者
                    maxhp += Randomizer.rand(100, 150);
                    break;

                case 3200: // 煉獄巫師
                    maxhp += Randomizer.rand(125, 150);
                    maxmp += Randomizer.rand(250, 300);
                    break;
                case 3210: // 煉獄巫師
                    maxhp += Randomizer.rand(175, 200);
                    maxmp += Randomizer.rand(350, 400);
                    break;
                case 3211: // 煉獄巫師
                    maxhp += Randomizer.rand(125, 150);
                    maxmp += Randomizer.rand(250, 300);
                    break;
                case 3212: // 煉獄巫師
                    maxhp += Randomizer.rand(75, 100);
                    maxmp += Randomizer.rand(150, 200);
                    break;

                case 100: // 劍士
                case 1100: // 聖魂劍士
                case 2100: // 狂狼勇士
                case 4001: // 劍豪
                case 4100: // 劍豪
                case 5000: // 米哈逸
                case 5100: // 米哈逸
                case 6000: // 龍之守護者凱薩
                case 6100: // 龍之守護者凱薩
                case 13000:// 皮卡啾
                case 13100:// 皮卡啾
                    maxmp += Randomizer.rand(100, 175);
                case 3001: // 惡魔殺手
                case 3100: // 惡魔殺手
                    maxhp += Randomizer.rand(200, 250);
                    break;

                case 110: // 狂戰士
                case 120: // 見習騎士
                case 130: // 槍騎兵
                case 1110: // 聖魂劍士
                case 2110: // 狂郎勇士
                case 4110: // 劍豪
                case 5110: // 米哈逸
                case 6110: // 龍之守護者凱薩
                case 13110:// 皮卡啾
                    maxmp += Randomizer.rand(150, 175);
                case 3110: // 惡魔殺手
                    maxhp += Randomizer.rand(300, 350);
                    break;

                case 111: // 狂戰士
                case 121: // 見習騎士
                case 131: // 槍騎兵
                case 1111: // 聖魂劍士
                case 2111: // 狂郎勇士
                case 4111: // 劍豪
                case 5111: // 米哈逸
                case 6111: // 龍之守護者凱薩
                case 13111:// 皮卡啾
                    maxmp += Randomizer.rand(100, 125);
                case 3111: // 惡魔殺手
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 112: // 狂戰士
                case 122: // 見習騎士
                case 132: // 槍騎兵
                case 1112: // 聖魂劍士
                case 2112: // 狂郎勇士
                case 4112: // 劍豪
                case 5112: // 米哈逸
                case 6112: // 龍之守護者凱薩
                case 13112:// 皮卡啾
                    maxmp += Randomizer.rand(50, 75);
                case 3112: // 惡魔殺手
                    maxhp += Randomizer.rand(100, 150);
                    break;
                case 200: // 魔法師
                case 1200: // 烈焰巫師
                case 2001: // 龍魔導士
                case 2200: // 龍魔導士
                case 2210: // 龍魔導士
                case 2211: // 龍魔導士
                case 2004: // 夜光
                case 2700: // 夜光
                case 11000:// 幻獸師
                case 11200:// 幻獸師
                    maxmp += Randomizer.rand(75, 90);
                case 4002: // 陰陽師
                case 4200: // 陰陽師
                    maxhp += Randomizer.rand(150, 180);
                    break;
                case 210: // 火毒
                case 220: // 冰雷
                case 230: // 主教
                case 1210: // 烈焰巫師
                case 2212: // 龍魔島士
                case 2213: // 龍魔島士
                case 2710: // 夜光
                case 11210:// 幻獸師
                    maxmp += Randomizer.rand(400, 450);
                case 4210: // 陰陽師
                    maxmp += Randomizer.rand(200, 225);
                    break;

                case 211: // 火毒
                case 221: // 冰雷
                case 231: // 主教
                case 1211: // 烈焰巫師
                case 2214: // 龍魔島士
                case 2215: // 龍魔島士
                case 2711: // 夜光
                case 11211:// 幻獸師
                    maxmp += Randomizer.rand(300, 350);
                case 4211: // 陰陽師
                    maxmp += Randomizer.rand(150, 175);
                    break;
                case 212: // 火毒
                case 222: // 冰雷
                case 232: // 主教
                case 1212: // 烈焰巫師
                case 2216: // 龍魔島士
                case 2217: // 龍魔島士
                case 2218: // 龍魔島士
                case 2712: // 夜光
                case 11212:// 幻獸師
                    maxmp += Randomizer.rand(200, 250);
                case 4212: // 陰陽師
                    maxhp += Randomizer.rand(100, 125);
                    break;

                case 300: // 弓箭手
                case 400: // 盜賊
                case 500: // 海盜
                case 501: // 重砲指揮官
                case 508: // JETT
                case 1300:// 破風使者
                case 1400:// 暗夜行者
                case 1500:// 閃雷悍將
                case 2002:// 精靈遊俠
                case 2300:// 精靈遊俠
                case 2003:// 幻影俠盜
                case 2400:// 幻影俠盜
                case 2005:// 隱月
                case 2500:// 隱月
                case 3300:// 狂豹獵人
                case 3500:// 機甲戰神
                case 3002:// 傑諾
                case 3600:// 傑諾
                    maxmp += Randomizer.rand(100, 125);
                case 6001:// 天使破壞者
                case 6500:// 天使破壞者
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 310: // 獵人
                case 320: // 奴弓手
                case 410: // 刺客
                case 420: // 俠盜
                case 430: // 下忍
                case 510: // 打手
                case 520: // 槍手
                case 530: // 重砲指揮官
                case 570: // JETT
                case 1310:// 破風使者
                case 1410:// 暗夜行者
                case 1510:// 閃雷悍將
                case 2310:// 精靈遊俠
                case 2410:// 幻影俠盜
                case 2510:// 隱月
                case 3310:// 狂豹獵人
                case 3510:// 機甲戰神
                case 3610:// 傑諾
                    maxmp += Randomizer.rand(150, 175);
                case 6510:// 天使破壞者
                    maxhp += Randomizer.rand(300, 350);
                    break;
                case 311: // 獵人
                case 321: // 奴弓手
                case 411: // 刺客
                case 421: // 俠盜
                case 431: // 下忍
                case 511: // 打手
                case 521: // 槍手
                case 531: // 重砲指揮官
                case 571: // JETT
                case 1311:// 破風使者
                case 1411:// 暗夜行者
                case 1511:// 閃雷悍將
                case 2311:// 精靈遊俠
                case 2411:// 幻影俠盜
                case 2511:// 隱月
                case 3311:// 狂豹獵人
                case 3511:// 機甲戰神
                case 3611:// 傑諾
                    maxmp += Randomizer.rand(100, 125);
                case 6511:// 天使破壞者
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 312: // 獵人
                case 322: // 奴弓手
                case 412: // 刺客
                case 422: // 俠盜
                case 432: // 下忍
                case 433: // 隱忍
                case 434: // 影武者
                case 512: // 打手
                case 522: // 槍手
                case 532: // 重砲指揮官
                case 572: // JETT
                case 1312:// 破風使者
                case 1412:// 暗夜行者
                case 1512:// 閃雷悍將
                case 2312:// 精靈遊俠
                case 2412:// 幻影俠盜
                case 2512:// 隱月
                case 3312:// 狂豹獵人
                case 3512:// 機甲戰神
                case 3612:// 傑諾
                    maxmp += Randomizer.rand(50, 75);
                case 6512:// 天使破壞者
                    maxhp += Randomizer.rand(100, 150);
                    break;
                default:
                    System.err.println("職業 " + MapleJob.getById(job).name() + " 未處理轉職HPMP增加");
            }
            if (maxhp >= 500000) {
                maxhp = 500000;
            }
            if (maxmp >= 500000) {
                maxmp = 500000;
            }
            if (MapleJob.is惡魔殺手(job) || MapleJob.is凱內西斯(job)) {
                maxmp = GameConstants.getMPByJob(this);
            } else if (MapleJob.is神之子(job) || MapleJob.is陰陽師(job)) {
                maxmp = 100;
            } else if (MapleJob.isNotMpJob(job)) {
                maxmp = 0;
            }
            stats.setInfo(maxhp, maxmp, maxhp, maxmp);
            Map<MapleStat, Long> statup = new EnumMap<>(MapleStat.class);
            statup.put(MapleStat.MAXHP, (long) maxhp);
            statup.put(MapleStat.MAXMP, (long) maxmp);
            statup.put(MapleStat.HP, (long) maxhp);
            statup.put(MapleStat.MP, (long) maxmp);
            characterCard.recalcLocalStats(this);
            stats.recalcLocalStats(this);
            client.announce(CWvsContext.updatePlayerStats(statup, this));
            map.broadcastMessage(this, EffectPacket.showJobChangeEffect(this), false);
            map.broadcastMessage(this, CField.updateCharLook(this, false), false);
            silentPartyUpdate();
            guildUpdate();
            if (dragon != null) {
                map.broadcastMessage(CField.removeDragon(this.id));
                dragon = null;
            }
            if (haku != null) {
                haku = null;
            }
            baseSkills();
            if (newJob >= 2200 && newJob <= 2218) { // make new
                if (getBuffedValue(MapleBuffStat.RideVehicle) != null) {
                    cancelBuffStats(MapleBuffStat.RideVehicle);
                }
                makeDragon();
            }
            if (MapleJob.is陰陽師(newJob)) {
                if (getBuffedValue(MapleBuffStat.RideVehicle) != null) {
                    cancelBuffStats(MapleBuffStat.RideVehicle);
                }
                makeHaku();
            }
            checkForceShield();
            MapleAndroid a = getAndroid();
            if (a != null) {
                a.showEmotion(this, "job");
            }
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); // all jobs throw errors :(
        }
    }

    public void addJobSkills() {
        Skill skil;
        int[] addskills = null;
        int[] removeskills = null;
        Map<Skill, SkillEntry> list = new HashMap<>();

        if (MapleJob.is狂狼勇士(job)) {
            addskills = new int[]{
                20001295, // 戰鬥衝刺
            };
        }

        if (MapleJob.isBeginner(job)) {
            if (MapleJob.is惡魔(job)) {
                addskills = new int[]{
                    30011109, // 魔族之翼(坐騎)
                    30010110, // 惡魔跳躍
                    30010185, // 魔族之血
                };
            }
        } else if (MapleJob.is重砲指揮官(job)) {
            addskills = new int[]{
                109, // 游泳達人。
            };
        } else if (MapleJob.is蒼龍俠客(job)) {
            addskills = new int[]{
                228, // 草上飛
            };
        } else if (MapleJob.is皇家騎士團(job)) {
            addskills = new int[]{
                10000252, // 元素位移
                10001244, // 元素狂刃
                10001254, // 元素閃現
            };
        } else if (MapleJob.is狂狼勇士(job)) {
            removeskills = new int[]{
                20000014, // 新手區技能
                20000015, // 新手區技能
                20000016, // 新手區技能
            };
        } else if (MapleJob.is精靈遊俠(job)) {
            addskills = new int[]{
                20020109, // 精靈的回復
                20020111, // 時髦的移動
                20020112, // 王的資格
            };
            removeskills = new int[]{
                20021181, // (劇情二段跳)
                20021166, // (劇情普攻)
            };
        } else if (MapleJob.is幻影俠盜(job)) {
            addskills = new int[]{
                20031203, // 水晶花園傳送
                20031205, // 幻影斗篷
                20030206, // 高明洞察力
                20031207, // 技能竊取
                20031208, // 技能管理
                20031260, // 審判方針 AUTO/MANUAL
                job == 2412 ? 20031210 : 20031209, // 卡牌審判
            };
            removeskills = new int[]{
                20031211, // 鬼鬼祟祟的移動
                20031212, // 擾亂
                job == 2412 ? 20031209 : 20031210, // 卡牌審判
            };
        } else if (MapleJob.is夜光(job)) {
            addskills = new int[]{
                20040216, // 光蝕
                20040217, // 暗蝕
                20040221, // 光明力量
                20041222, // 星光順移
                20040219, // 平衡
            };
            removeskills = new int[]{
                20041226, // 光箭
                20040220, // 平衡
            };
            // 光魔法強化
            Skill skill_1 = SkillFactory.getSkill(27000106);
            // 黑暗魔法強化
            Skill skill_2 = SkillFactory.getSkill(27000207);
            if (skill_1 != null && getSkillLevel(skill_1) != 0) {
                if (getSkillLevel(skill_1) < 1) {
                    list.put(skill_1, new SkillEntry((byte) 1, (byte) 5, SkillFactory.getDefaultSExpiry(skill_1)));
                }
                // 星星閃光
                skil = SkillFactory.getSkill(27001100);
                if (skil != null && getSkillLevel(skil) == 0) {
                    list.put(skil, new SkillEntry((byte) 1, (byte) 20, SkillFactory.getDefaultSExpiry(skil)));
                }
            } else if (skill_2 != null && getSkillLevel(skill_2) != 0) {
                if (getSkillLevel(skill_1) < 1) {
                    list.put(skill_2, new SkillEntry((byte) 1, (byte) 5, SkillFactory.getDefaultSExpiry(skill_2)));
                }
                // 黑暗球體
                skil = SkillFactory.getSkill(27001201);
                if (skil != null && getSkillLevel(skil) == 0) {
                    list.put(skil, new SkillEntry((byte) 1, (byte) 20, SkillFactory.getDefaultSExpiry(skil)));
                }
            } else {
                NPCScriptManager.getInstance().start(client, 0, "LuminousSelect");
            }
        } else if (MapleJob.is隱月(job)) {
            addskills = new int[]{
                20050286, // 死裡逃生
            };
            if (level < 20) {
                skil = SkillFactory.getSkill(25001000); // 巨型衝擊
                if (skil != null && getSkillLevel(skil) == 0) {
                    list.put(skil, new SkillEntry((byte) 0, (byte) 25, SkillFactory.getDefaultSExpiry(skil)));
                }
            } else if (level >= 20) {
                skil = SkillFactory.getSkill(25001000); // 巨型衝擊
                int lv = getSkillLevel(skil);
                if (skil != null && lv > 0) {
                    list.put(skil, new SkillEntry((byte) -1, (byte) 0, SkillFactory.getDefaultSExpiry(skil)));
                }
                skil = SkillFactory.getSkill(25001002); // 重拳
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(skil, new SkillEntry((byte) lv, (byte) -1, SkillFactory.getDefaultSExpiry(skil)));
                }
                addskills = new int[]{
                    20050285, // 精靈凝聚1式
                    20050285, // 縮地
                };
            }
        } else if (MapleJob.is惡魔殺手(job)) {
            addskills = new int[]{
                30010111, // 死亡詛咒
            };
        } else if (MapleJob.is惡魔復仇者(job)) {
            addskills = new int[]{
                30010242, // 血之限界
                30010230, // 超越
                30010231, // 效能提升
                30010232, // 轉換星盾
            };
        } else if (MapleJob.is傑諾(job)) {
            addskills = new int[]{
                30020232, // 蓄能系統
                30020240, // 多樣化裝扮
                30021237, // 自由飛行
                30020234, // 全能增幅 I
                30021235, // 普羅梅莎突襲
                30021236, // 多功能模式
                job >= 3612 ? 36120010 : 0, // 全能增幅 V
            };
            removeskills = new int[]{
                30021238, // 刀舞
            };
        } else if (MapleJob.is末日反抗軍(job)) {
            int skillId = 0;
            if (MapleJob.is煉獄巫師(job)) {
                skillId = 30000074;
            } else if (MapleJob.is狂豹獵人(job)) {
                skillId = 30000075;
            } else if (MapleJob.is機甲戰神(job)) {
                skillId = 30000076;
            }
            if (skillId > 0) {
                addskills = new int[]{
                    skillId, // 自由精神
                };
            }
        } else if (MapleJob.is劍豪(job)) {
            addskills = new int[]{
                40010000, // 天賦的才能
                40010067, // 攻守兼備
                40011288, // 拔刀姿勢
                40011289, // 疾風五月雨刃
            };
            removeskills = new int[]{
                40011023, // 心刀
                40011183, // (劇情普攻)
                40011184, // (劇情普攻2)
                40011185, // (劇情普攻3)
                40011186, // (劇情普攻4)
            };
        } else if (MapleJob.is陰陽師(job)) {
            addskills = new int[]{
                40020000, // 五行的加護
                40020001, // 無限的靈力
                40020109, // 花狐
            };
            removeskills = new int[]{
                40021183, // (劇情普攻)
                40021184, // (劇情普攻2)
                40021185, // (劇情普攻3)
                40021186, // (劇情星星酒火)
            };
        } else if (MapleJob.is凱撒(job)) {
            addskills = new int[]{
                60001225, // 指令
                60001216, // 洗牌交換： 防禦模式
                60001217, // 洗牌交換： 攻擊模式
                60001218, // 縱向連接
                60000219, // 變身
            };
            removeskills = new int[]{
                60001229, // 氣焰隱忍
            };

            // 劍龍連斬
            skil = SkillFactory.getSkill(61001000);
            if (skil != null && getSkillLevel(skil) == 0) {
                list.put(skil, new SkillEntry((byte) 1, (byte) 20, SkillFactory.getDefaultSExpiry(skil)));
            }
            // 火焰衝擊
            skil = SkillFactory.getSkill(61001101);
            if (skil != null && getSkillLevel(skil) == 0) {
                list.put(skil, new SkillEntry((byte) 1, (byte) 20, SkillFactory.getDefaultSExpiry(skil)));
            }
        } else if (MapleJob.is天使破壞者(job)) {
            addskills = new int[]{
                60011216, // 繼承人
                60011218, // 魔法起重機
                60011220, // 白日夢
                60011221, // 配飾
                60011222, // 魔法變身
            };
        } else if (MapleJob.is幻獸師(job)) {
            addskills = new int[]{
                110001510, // 精靈召喚模式
                110001500, // 解除模式
                110001506, // 守護者的敏捷身姿
                110001514, // 回歸樹木村莊
                110001501, // 召喚熊熊
                110001502, // 召喚雪豹
                level >= 30 ? 110001503 : 0, // 召喚雀鷹
                level >= 50 ? 110001504 : 0, // 召喚貓咪
            };
            if (level >= 10 || getQuestStatus(59015) >= 1) {
                skil = SkillFactory.getSkill(112000000); // 波波揮擊
                if (skil != null && getSkillLevel(skil) == 0) {
                    list.put(skil, new SkillEntry((byte) 1, (byte) 20, SkillFactory.getDefaultSExpiry(skil)));
                }
            }
            if (level >= 10 || getQuestStatus(59009) >= 1) {
                skil = SkillFactory.getSkill(112100000); // 萊卡痛擊
                if (skil != null && getSkillLevel(skil) == 0) {
                    list.put(skil, new SkillEntry((byte) 1, (byte) 20, SkillFactory.getDefaultSExpiry(skil)));
                }
            }
            if (level >= 30) {
                skil = SkillFactory.getSkill(112110003); // 隊伍攻擊
                if (skil != null && getSkillLevel(skil) == 0) {
                    list.put(skil, new SkillEntry((byte) 1, (byte) 10, SkillFactory.getDefaultSExpiry(skil)));
                }
                skil = SkillFactory.getSkill(110000515); // 武器鍛鍊
                if (skil != null && getSkillLevel(skil) < Math.min((getLevel() - 30) / 10, 10)) {
                    list.put(skil, new SkillEntry((byte) Math.min((getLevel() - 30) / 10, 10), (byte) 10,
                            SkillFactory.getDefaultSExpiry(skil)));
                }
            }
            if (level >= 50) {
                skil = SkillFactory.getSkill(112120000); // 朋友發射
                if (skil != null && getSkillLevel(skil) == 0) {
                    list.put(skil, new SkillEntry((byte) 1, (byte) 10, SkillFactory.getDefaultSExpiry(skil)));
                }
            }
        }

        if (MapleJob.is惡魔(job) && !MapleJob.isBeginner(job)) {
            removeskills = new int[]{
                30010166, // (劇情普攻)
                30011167, // (劇情普攻2)
                30011168, // (劇情普攻3)
                30011169, // (劇情普攻4)
                30011170, // (劇情普攻5)
            };
        }

        if (addskills != null) {
            for (int skillId : addskills) {
                if (skillId <= 0) {
                    continue;
                }
                skil = SkillFactory.getSkill(skillId);
                if (getSkillLevel(skillId) <= 0 && skil != null && skil.canBeLearnedBy(job)) {
                    list.put(skil, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        }

        if (removeskills != null) {
            for (int skillId : removeskills) {
                if (skillId <= 0) {
                    continue;
                }
                skil = SkillFactory.getSkill(skillId);
                if (skil != null) {
                    list.put(skil, new SkillEntry((byte) -1, (byte) 0, -1));
                }
            }
        }

        if (!list.isEmpty()) {
            changeSkillsLevel(list);
        }
    }

    public void baseSkills() { // TODO 添加基礎技能
        checkInnerSkill();
        Skill skil;
        Map<Skill, SkillEntry> list = new HashMap<>();
        Map<Skill, SkillEntry> skils = getSkills();

        // 處理到目前轉數為止當前職業分支的FixLevel跟有默認技能上限的技能
        for (Skill skill : SkillFactory.getAllSkills()) {
            skil = skill;
            if (skil == null || skil.isBeginnerSkill() || !skil.canBeLearnedBy(job) || skil.isInvisible() || getLevel() < skil.getReqLevel()) {
                continue;
            }
            int masterLevel = (byte) (FeaturesConfig.maxSkillLevel ? skil.getMaxLevel() : skil.getMasterLevel());
            SkillEntry skillEntry = skils.get(skil);
            if (skillEntry == null) {
                skillEntry = new SkillEntry((byte) 0, (byte) 0, SkillFactory.getDefaultSExpiry(skil));
            }
            // FixLevel修正
            if (getSkillLevel(skil) <= 0 && skil.getFixLevel() > 0 && skil.getId() != 130000111/* 皮卡啾收納達人 */) {
                skillEntry.skillLevel = (byte) skil.getFixLevel();
                list.put(skil, skillEntry);
            }
            // MasterLevel修正
            if (masterLevel > 0 && skillEntry.masterlevel < masterLevel && !skil.isBeginnerSkill()) {
                skillEntry.masterlevel = (byte) masterLevel;
                list.put(skil, skillEntry);
            }
        }

        // 處理傳授技能
        int linkId = 0;
        int maxLinkLevel = 3;
        if (MapleJob.is重砲指揮官(job)) {
            linkId = 110; // 百烈祝福。
        } else if (MapleJob.is蒼龍俠客(job)) {
            linkId = 1214; // 寶盒的護佑
        } else if (MapleJob.is聖魂劍士(job)) {
            linkId = 10000255; // 西格諾斯祝福（劍士）
            maxLinkLevel = 2;
        } else if (MapleJob.is烈焰巫師(job)) {
            linkId = 10000256; // 西格諾斯祝福（法師）
            maxLinkLevel = 2;
        } else if (MapleJob.is破風使者(job)) {
            linkId = 10000257; // 西格諾斯祝福（弓箭手）
            maxLinkLevel = 2;
        } else if (MapleJob.is暗夜行者(job)) {
            linkId = 10000258; // 西格諾斯祝福（盜賊）
            maxLinkLevel = 2;
        } else if (MapleJob.is閃雷悍將(job)) {
            linkId = 10000259; // 西格諾斯祝福（海盜）
            maxLinkLevel = 2;
        } else if (MapleJob.is精靈遊俠(job)) {
            linkId = 20021110; // 精靈的祝福
        } else if (MapleJob.is幻影俠盜(job)) {
            linkId = 20030204; // 致命本能
        } else if (MapleJob.is夜光(job)) {
            linkId = 20040218; // 滲透
        } else if (MapleJob.is隱月(job)) {
            linkId = 20050286; // 死裡逃生
            maxLinkLevel = 2;
        } else if (MapleJob.is惡魔殺手(job)) {
            linkId = 30010112; // 惡魔之怒
        } else if (MapleJob.is惡魔復仇者(job)) {
            linkId = 30010241; // 狂暴鬥氣
        } else if (MapleJob.is傑諾(job)) {
            linkId = 30020233; // 合成邏輯
            maxLinkLevel = 2;
        } else if (MapleJob.is劍豪(job)) {
            linkId = 40010001; // 疾風傳授
            maxLinkLevel = 1;
        } else if (MapleJob.is陰陽師(job)) {
            linkId = 40020002; // 紫扇傳授
            maxLinkLevel = 2;
        } else if (MapleJob.is米哈逸(job)) {
            linkId = 50001214; // 光之守護
        } else if (MapleJob.is凱撒(job)) {
            linkId = 60000222; // 鋼鐵意志
        } else if (MapleJob.is天使破壞者(job)) {
            linkId = 60011219; // 靈魂契約
        } else if (MapleJob.is神之子(job)) {
            linkId = 100000271; // 時之祝福
            maxLinkLevel = 5;
        } else if (MapleJob.is幻獸師(job)) {
            linkId = 110000800; // 精靈集中
        }
        if (linkId > 0) {
            int nowLinkLevel = 0;
            if (!MapleJob.is神之子(job)) {
                nowLinkLevel = level < 70 ? 0 : level < 120 ? 1 : level < 200 ? 2 : 3;
            }
            skil = SkillFactory.getSkill(linkId);
            if (skil == null) {
                if (isShowErr()) {
                    showInfo("[傳授技能]", true, "更新傳授技能出錯，技能不存在。");
                }
            } else if (!skils.containsKey(skil) || skils.get(skil).skillLevel < nowLinkLevel) {
                list.put(skil, new SkillEntry((byte) Math.min(maxLinkLevel, nowLinkLevel),
                        (byte) (nowLinkLevel > 0 ? nowLinkLevel : -1), SkillFactory.getDefaultSExpiry(skil)));
            }
        }

        // 收納達人處理
        int addSlot = 0;
        short beginner = MapleJob.getBeginner(job);
        if (MapleJob.is皮卡啾(job)) {
            skil = SkillFactory.getSkill((beginner * 10000) + 111);
            if (skil != null && getSkillLevel(skil) < 1) {
                list.put(skil, new SkillEntry((byte) 1, (byte) 0, SkillFactory.getDefaultSExpiry(skil)));
                addSlot += 48;
            }
        } else if (!MapleJob.is冒險家(job) || MapleJob.is重砲指揮官(job)) {
            skil = SkillFactory.getSkill((beginner * 10000) + 111);
            if (skil != null && getSkillLevel(skil) < 1 && MapleJob.getJobGrade(job) == 1) {
                list.put(skil, new SkillEntry((byte) 1, (byte) 0, SkillFactory.getDefaultSExpiry(skil)));
                addSlot += 12;
            }
            skil = SkillFactory.getSkill((beginner * 10000) + 112);
            if (skil != null && getSkillLevel(skil) < 2 && MapleJob.getJobGrade(job) > 1) {
                list.put(skil, new SkillEntry((byte) 2, (byte) 0, SkillFactory.getDefaultSExpiry(skil)));
                addSlot += 12;

                skil = SkillFactory.getSkill((beginner * 10000) + 111);
                if (skil != null && getSkillLevel(skil) != 0) {
                    changeSkillLevel(skil, (byte) -1, (byte) 0);
                } else {
                    addSlot += 12;
                }
            }
        }
        if (addSlot > 0) {
            for (MapleInventoryType type : MapleInventoryType.values()) {
                if (type.getType() <= MapleInventoryType.UNDEFINED.getType() || type.getType() >= MapleInventoryType.CASH.getType()) {
                    continue;
                }
                expandInventory(type.getType(), addSlot);
            }
        }

        if (!list.isEmpty()) {
            changeSkillsLevel(list);
        }

        addJobSkills();
    }

    public void makeDragon() {
        dragon = new MapleDragon(this);
        map.broadcastMessage(CField.spawnDragon(dragon));
    }

    public MapleDragon getDragon() {
        return dragon;
    }

    public void makeHaku() {
        haku = new MapleHaku(this);
        map.broadcastMessage(CField.spawnHaku(haku));
    }

    public MapleHaku getHaku() {
        return haku;
    }

    public void gainAp(short ap) {
        this.remainingAp += ap;
        updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
    }

    public void gainSP(int sp) {
        this.remainingSp[GameConstants.getSkillBookByJob(job)] += sp; // default
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
        client.announce(InfoPacket.getSPMsg((byte) sp, job));
    }

    public void gainSP(int sp, final int skillbook) {
        this.remainingSp[skillbook] += sp;
        updateSingleStat(MapleStat.AVAILABLESP, 0);
        client.announce(InfoPacket.getSPMsg((byte) sp, (short) 0));
    }

    public void gainHSP(int mode, int hsp) {
        this.remainingHSp[mode] += hsp;
        client.announce(CWvsContext.updateHyperSp(mode, hsp));
    }

    public void resetSP(int sp) {
        for (int i = 0; i < remainingSp.length; i++) {
            this.remainingSp[i] = sp;
        }
        updateSingleStat(MapleStat.AVAILABLESP, 0);
    }

    public void resetSp(int jobid) {
        int skillpoint = 0;
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (SkillConstants.isApplicableSkill(skil.getId()) && skil.canBeLearnedBy(getJob())
                    && skil.getId() >= jobid * 1000000 && skil.getId() < (jobid + 1) * 1000000 && !skil.isInvisible()) {
                skillpoint += getSkillLevel(skil);
            }
        }
        gainSP(skillpoint, GameConstants.getSkillBookByJob(jobid));
        final Map<Skill, SkillEntry> skillmap = new HashMap<>(getSkills());
        final Map<Skill, SkillEntry> newList = new HashMap<>();
        for (Entry<Skill, SkillEntry> skill : skillmap.entrySet()) {
            newList.put(skill.getKey(), new SkillEntry((byte) 0, (byte) 0, -1));
        }
        changeSkillsLevel(newList);
        newList.clear();
        skillmap.clear();
    }

    public List<Integer> getProfessions() {
        List<Integer> prof = new ArrayList<>();
        for (int i = 9200; i <= 9204; i++) {
            if (getProfessionLevel(id * 10000) > 0) {
                prof.add(i);
            }
        }
        return prof;
    }

    public byte getProfessionLevel(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (byte) ((ret >>> 24) & 0xFF); // the last byte
    }

    public short getProfessionExp(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (short) (ret & 0xFFFF); // the first two byte
    }

    public boolean addProfessionExp(int id, int expGain) {
        int ret = getProfessionLevel(id);
        if (ret <= 0 || ret >= 10) {
            return false;
        }
        int newExp = getProfessionExp(id) + expGain;
        if (newExp >= GameConstants.getProfessionEXP(ret)) {
            // gain level
            changeProfessionLevelExp(id, ret + 1, newExp - GameConstants.getProfessionEXP(ret));
            int traitGain = (int) Math.pow(2, ret + 1);
            switch (id) {
                case 92000000:
                    traits.get(MapleTraitType.sense).addExp(traitGain, this);
                    break;
                case 92010000:
                    traits.get(MapleTraitType.will).addExp(traitGain, this);
                    break;
                case 92020000:
                case 92030000:
                case 92040000:
                    traits.get(MapleTraitType.craft).addExp(traitGain, this);
                    break;
            }
            return true;
        } else {
            changeProfessionLevelExp(id, ret, newExp);
            return false;
        }
    }

    public void changeProfessionLevelExp(int id, int level, int exp) {
        changeSingleSkillLevel(SkillFactory.getSkill(id), ((level & 0xFF) << 24) + (exp & 0xFFFF), (byte) 10);
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, byte newMasterlevel) { // 1 month
        if (skill == null) {
            return;
        }
        changeSingleSkillLevel(skill, newLevel, newMasterlevel, SkillFactory.getDefaultSExpiry(skill));
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        final Map<Skill, SkillEntry> list = new HashMap<>();
        boolean hasRecovery = false, recalculate = false;
        if (changeSkillData(skill, newLevel, newMasterlevel, expiration)) { // no loop, only 1
            list.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
            if (GameConstants.isRecoveryIncSkill(skill.getId())) {
                hasRecovery = true;
            }
            if (skill.getId() < 80000000) {
                recalculate = true;
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.announce(CWvsContext.updateSkills(list, false));
        reUpdateStat(hasRecovery, recalculate);
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, byte newMasterlevel, long expiration,
            boolean hyper) {
        final Map<Skill, SkillEntry> list = new HashMap<>();
        boolean hasRecovery = false, recalculate = false;
        if (changeSkillData(skill, newLevel, newMasterlevel, expiration)) { // no loop, only 1
            list.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
            if (GameConstants.isRecoveryIncSkill(skill.getId())) {
                hasRecovery = true;
            }
            if (skill.getId() < 80000000) {
                recalculate = true;
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.announce(CWvsContext.updateSkills(list, hyper));
        reUpdateStat(hasRecovery, recalculate);
    }

    public void changeSkillsLevel(final Map<Skill, SkillEntry> ss) {
        changeSkillsLevel(ss, false);
    }

    public void changeSkillsLevel(final Map<Skill, SkillEntry> ss, boolean hyper) {
        if (ss.isEmpty()) {
            return;
        }
        final Map<Skill, SkillEntry> list = new HashMap<>();
        boolean hasRecovery = false, recalculate = false;
        for (final Entry<Skill, SkillEntry> data : ss.entrySet()) {
            if (changeSkillData(data.getKey(), data.getValue().skillLevel, data.getValue().masterlevel,
                    data.getValue().expiration)) {
                list.put(data.getKey(), data.getValue());
                if (GameConstants.isRecoveryIncSkill(data.getKey().getId())) {
                    hasRecovery = true;
                }
                if (data.getKey().getId() < 80000000) {
                    recalculate = true;
                }
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.announce(CWvsContext.updateSkills(list, hyper));
        reUpdateStat(hasRecovery, recalculate);
    }

    public boolean changeSkillData(final Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        if (skill == null || (!SkillConstants.isApplicableSkill(skill.getId())
                && !SkillConstants.isApplicableSkill_(skill.getId()))) {
            return false;
        }
        if (newLevel < 0 && newMasterlevel == 0) {
            if (skills.containsKey(skill)) {
                skills.remove(skill);
            } else {
                return false; // nothing happen
            }
        } else {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
        }
        return true;
    }

    public void changeSonOfLinkedSkill(int skillId, int toChrId) {
        Skill skil = SkillFactory.getSkill(skillId);
        if (skil == null) {
            return;
        }
        final Map<Skill, SkillEntry> enry = new HashMap<>();
        enry.put(skil, new SkillEntry(getTotalSkillLevel(skillId), (byte) skil.getMasterLevel(), -1L, toChrId));
        changeSkillLevel_Skip(enry, true);
    }

    public void changeSkillLevel(Skill skill, byte newLevel, byte newMasterlevel) {
        changeSkillLevel_Skip(skill, newLevel, newMasterlevel);
    }

    public void changeSkillLevel_Skip(Skill skil, int skilLevel, byte masterLevel) {
        final Map<Skill, SkillEntry> enry = new HashMap<>();
        enry.put(skil, new SkillEntry(skilLevel, masterLevel, -1L));
        changeSkillLevel_Skip(enry, true);
    }

    public void changeSkillLevel_Skip(final Map<Skill, SkillEntry> skill, final boolean write) { // only used for
        // temporary skills
        // (not saved into db)
        if (skill.isEmpty()) {
            return;
        }
        final Map<Skill, SkillEntry> newL = new HashMap<>();
        for (final Entry<Skill, SkillEntry> z : skill.entrySet()) {
            if (z.getKey() == null) {
                continue;
            }
            newL.put(z.getKey(), z.getValue());
            if (z.getValue().skillLevel < 0 && z.getValue().masterlevel == 0) {
                if (skills.containsKey(z.getKey())) {
                    changed_skills = true;
                    skills.remove(z.getKey());
                } else {
                    continue;
                }
            } else {
                changed_skills = true;
                skills.put(z.getKey(), z.getValue());
            }
        }
        if (write && !newL.isEmpty()) {
            client.announce(CWvsContext.updateSkills(newL, false));
        }
    }

    public int setSonOfLinkedSkill(int skillId, int toChrId) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM skills WHERE skillid = ? AND teachId = ?")) {
                ps.setInt(1, skillId);
                ps.setInt(2, this.id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con
                    .prepareStatement("SELECT * FROM skills WHERE skillid = ? AND characterid = ?")) {
                ps.setInt(1, skillId);
                ps.setInt(2, toChrId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    rs.close();
                    ps.close();
                    try (PreparedStatement psskills = con.prepareStatement(
                            "INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration, teachId) VALUES (?, ?, ?, ?, ?, ?)")) {
                        psskills.setInt(1, toChrId);
                        psskills.setInt(2, skillId);
                        psskills.setInt(3, 1);
                        psskills.setByte(4, (byte) 1);
                        psskills.setLong(5, -1L);
                        psskills.setInt(6, this.id);
                        psskills.executeUpdate();
                    }
                    return 1;
                }
                rs.close();
            }
            ManagerDatabasePool.closeConnection(con);
            return -1;
        } catch (Exception Ex) {
            System.err.println("傳授技能失敗." + Ex);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, Ex);
        }
        return -1;
    }

    public void reUpdateStat(boolean hasRecovery, boolean recalculate) {
        changed_skills = true;
        if (hasRecovery) {
            stats.relocHeal(this);
        }
        if (recalculate) {
            stats.recalcLocalStats(this);
        }
    }

    public void playerDead() { // TODO 角色死亡
        MapleBuffStat[] 免死BUFF = {MapleBuffStat.HeavensDoor, MapleBuffStat.FlareTrick, MapleBuffStat.PreReviveOnce,
            MapleBuffStat.ReviveOnce};

        for (MapleBuffStat stat : 免死BUFF) {
            MapleStatEffect statss = getStatForBuff(stat);
            if (statss != null) {
                int recoveryHPR = statss.getX() <= 0 ? 100 : statss.getX();
                int coolTime = 0;
                switch (stat) {
                    case FlareTrick:
                        recoveryHPR = statss.getY();
                        // handle无敌(this, statss, 3000);
                        break;
                    case PreReviveOnce:
                        if (Randomizer.isSuccess(statss.getProb())) {
                            if (isShowInfo()) {
                                dropMessage(10, "觸發死裡逃生BUFF失敗，概率" + statss.getProb() + "%。");
                            }
                            continue;
                        }
                        break;
                }
                coolTime = statss.getCooldown(this);
                dropMessage(5, "以消耗掉" + stat.name() + "的效果替代死亡，並回復最大值" + recoveryHPR + "%的HP。");
                getStat().setHp(getStat().getMaxHp() / 100 * recoveryHPR, this);
                setStance(0);
                dispelDebuffs();
                cancelEffectFromBuffStat(stat);
                if (coolTime > 0 && !skillisCooling(statss.getSourceId())) {
                    addCooldown(statss.getSourceId(), System.currentTimeMillis(), coolTime * 1000);
                }
                return;
            }
        }

        if (getSkillLevel(1320016) > 0 && !skillisCooling(1320019)
                && getBuffedValue(MapleBuffStat.Reincarnation) == null) {
            Skill skill = SkillFactory.getSkill(1320019);
            if (skill != null) {
                MapleStatEffect effect = skill.getEffect(getTotalSkillLevel(skill));
                if (effect != null) {
                    addCooldown(1320019, System.currentTimeMillis(), effect.getCooldown(this) * 1000L);
                    skill.getEffect(getTotalSkillLevel(skill)).applyTo(this);
                }
            }
            return;
        }

        updateSingleStat(MapleStat.HP, 0);
        if (android != null) {
            android.showEmotion(this, "dead");
        }
        int deachTipOp;
        int deachTipType;
        if (android != null && (android.getItemId() == 1662072 || android.getItemId() == 1662073)) { // 戰鬥機器人
            deachTipOp = 3;
            deachTipType = 11;
        } else if (getStatForBuff(MapleBuffStat.SoulStone) != null) { // 靈魂之石
            deachTipOp = 1;
            deachTipType = 5;
            setTempValue("靈魂之石", String.valueOf(true));
        } else {
            deachTipOp = 1;
            deachTipType = 0;
        }
        getClient().announce(CField.getDeathTip(deachTipOp, false, deachTipType));

        cancelEffectFromBuffStat(MapleBuffStat.ShadowPartner);
        cancelEffectFromBuffStat(MapleBuffStat.Morph);
        cancelEffectFromBuffStat(MapleBuffStat.Flying);
        cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
        cancelEffectFromBuffStat(MapleBuffStat.Mechanic);
        cancelEffectFromBuffStat(MapleBuffStat.Regen);
        cancelEffectFromBuffStat(MapleBuffStat.IndieMHPR);
        cancelEffectFromBuffStat(MapleBuffStat.IndieMMPR);
        cancelEffectFromBuffStat(MapleBuffStat.IndieMHP);
        cancelEffectFromBuffStat(MapleBuffStat.IndieMMP);
        cancelEffectFromBuffStat(MapleBuffStat.EMHP);
        cancelEffectFromBuffStat(MapleBuffStat.EMMP);
        cancelEffectFromBuffStat(MapleBuffStat.MaxMP);
        cancelEffectFromBuffStat(MapleBuffStat.MaxHP);
        dispelSummons();

        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        checkFollow();
        dotHP = 0;
        lastDOTTime = 0;
        if (GameConstants.isAzwanMap(getMapId())) {
            client.announce(CWvsContext.showAzwanKilled());
        }
        if (!MapleJob.isBeginner(job) && !inPVP() && !GameConstants.isAzwanMap(getMapId())) {
            int amulet = getItemQuantity(5130000, false);
            if (android != null && (android.getItemId() == 1662072 || android.getItemId() == 1662073)) {
            } else if (amulet > 0) {
                MapleInventoryManipulator.removeById(client, MapleInventoryType.CASH, 5130000, 1, true, false);
                amulet--;
                if (amulet > 0xFF) {
                    amulet = 0xFF;
                }
                client.announce(EffectPacket.useAmulet(1, (byte) amulet, (byte) 0, 5130000));
            } else {
                float diepercentage;
                long expforlevel = getNeededExp();
                if (map.isTown() || FieldLimitType.RegularExpLoss.check(map.getFieldLimit())) {
                    diepercentage = 0.01f;
                } else {
                    diepercentage = (float) (0.1f - ((traits.get(MapleTraitType.charisma).getLevel() / 20) / 100f)
                            - (stats.expLossReduceR / 100f));
                }
                long v10 = (exp - (long) ((double) expforlevel * diepercentage));
                if (v10 < 0) {
                    v10 = 0;
                }
                this.exp = v10;
            }
            this.updateSingleStat(MapleStat.EXP, this.exp);
        }
        if (!stats.checkEquipDurabilitys(this, -100)) { // i guess this is how it works ?
            dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
        } // lol
        int 凍結加持道具 = android != null && (android.getItemId() == 1662072 || android.getItemId() == 1662073)
                ? android.getItemId()
                : 0; // 戰鬥機器人
        if (凍結加持道具 == 0) {
            int[] 凍結道具IDs = new int[]{5133000, 5133001};
            for (int itemId : 凍結道具IDs) {
                if (haveItem(itemId, 1, false, true)) {
                    MapleInventoryManipulator.removeById(client, MapleInventoryType.CASH, itemId, 1, true, false);
                    凍結加持道具 = itemId;
                }
            }
        }
        if (凍結加持道具 > 0) {
            client.announce(CField.getSaveBuff(凍結加持道具));
        } else {
            cancelAllBuffs();
            cancelAllBuffs_();
        }
        if (pyramidSubway != null) {
            stats.setHp((short) 50, this);
            setXenonSurplus(0);
            pyramidSubway.fail(this);
        }
    }

    public void updatePartyMemberHP() {
        if (party != null && client.getChannelServer() != null) {
            final int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    final MapleCharacter other = client.getChannelServer().getPlayerStorage()
                            .getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.getClient()
                                .announce(CField.updatePartyMemberHP(getId(), stats.getHp(), stats.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void receivePartyMemberHP() {
        if (party == null) {
            return;
        }
        int channel = client.getChannel();
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                MapleCharacter other = client.getChannelServer().getPlayerStorage()
                        .getCharacterByName(partychar.getName());
                if (other != null) {
                    client.announce(CField.updatePartyMemberHP(other.getId(), other.getStat().getHp(),
                            other.getStat().getCurrentMaxHp()));
                }
            }
        }
    }

    public void healHP(int delta) {
        addHP(delta);
        client.announce(EffectPacket.showHealed(delta));
        getMap().broadcastMessage(this, EffectPacket.showHealed(this, delta), false);
    }

    public void healMP(int delta) {
        if (delta <= 0) {
            return;
        }
        addMP(delta);
        client.announce(EffectPacket.showHealed(delta));
        getMap().broadcastMessage(this, EffectPacket.showHealed(this, delta), false);
    }

    /**
     * Convenience function which adds the supplied parameter to the current hp
     * then directly does a updateSingleStat.
     *
     * @see MapleCharacter#setHp(int)
     * @param delta
     */
    public void addHP(int delta) {
        if (stats.setHp(stats.getHp() + delta, this)) {
            updateSingleStat(MapleStat.HP, stats.getHp());
        }
    }

    /**
     * Convenience function which adds the supplied parameter to the current mp
     * then directly does a updateSingleStat.
     *
     * @see MapleCharacter#setMp(int)
     * @param delta
     */
    public void addMP(int delta) {
        addMP(delta, false);
    }

    public void addMP(int delta, boolean ignore) {
        if (stats.setMp(stats.getMp() + delta, this)) {
            updateSingleStat(MapleStat.MP, stats.getMp());
        }
    }

    public void addMPHP(int hpDiff, int mpDiff) {
        Map<MapleStat, Long> statups = new EnumMap<>(MapleStat.class);

        if (stats.setHp(stats.getHp() + hpDiff, this)) {
            statups.put(MapleStat.HP, (long) stats.getHp());
        }
        if ((mpDiff < 0 && MapleJob.is惡魔殺手(getJob())) || !MapleJob.is惡魔殺手(getJob())) {
            if (stats.setMp(stats.getMp() + mpDiff, this)) {
                statups.put(MapleStat.MP, (long) stats.getMp());
            }
        }
        if (statups.size() > 0) {
            client.announce(CWvsContext.updatePlayerStats(statups, this));
        }
    }

    public void updateSingleStat(MapleStat stat, long newval) {
        updateSingleStat(stat, newval, false);
    }

    /**
     * Updates a single stat of this MapleCharacter for the client. This method
     * only creates and sends an update packet, it does not update the stat
     * stored in this MapleCharacter instance.
     *
     * @param stat
     * @param newval
     * @param itemReaction
     */
    public void updateSingleStat(MapleStat stat, long newval, boolean itemReaction) {
        Map<MapleStat, Long> statup = new EnumMap<>(MapleStat.class);
        statup.put(stat, newval);
        client.announce(CWvsContext.updatePlayerStats(statup, itemReaction, this));
    }

    public void gainExp(final long total, final boolean show, final boolean inChat, final boolean white) {
        try {
            long prevexp = getExp();
            long needed = getNeededExp();
            if (total > 0) {
                stats.checkEquipLevels(this, total); // gms like
            }
            if (level >= maxLevel) {
                setExp(0);
                // if (exp + total > needed) {
                // setExp(needed);
                // } else {
                // exp += total;
                // }
            } else {
                boolean leveled = false;
                long tot = exp + total;
                if (tot >= needed) {
                    exp += total;
                    while (exp >= needed) {
                        levelUp();
                        needed = getNeededExp();
                    }
                    leveled = true;
                    if (level >= maxLevel) {
                        setExp(0);
                    } else {
                        needed = getNeededExp();
                        if (exp >= needed) {
                            setExp(needed - 1);
                        }
                    }
                } else {
                    exp += total;
                }
            }
            if (total != 0) {
                if (exp < 0) { // After adding, and negative
                    if (total > 0) {
                        setExp(needed);
                    } else if (total < 0) {
                        setExp(0);
                    }
                }
                updateSingleStat(MapleStat.EXP, getExp());
                if (show) { // still show the expgain even if it's not there
                    client.announce(InfoPacket.OnIncEXPMessage(total, inChat, white, null));
                }
            }
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); // all jobs throw errors :(
        }
    }

    public void setGM(PlayerGMRank rank) {
        setGmLevel(rank);
    }

    public void setGmLevel(PlayerGMRank rank) {
        this.gmLevel = (byte) rank.getLevel();
    }

    public boolean isShowInfo() {
        return isAdmin() || ServerConfig.LOG_PACKETS;
    }

    public boolean isShowErr() {
        return isSuperGM() || ServerConfig.LOG_PACKETS;
    }

    public void gainExpMonster(int gain, final boolean show, final boolean white, byte pty, final byte Class_Bonus_EXP,
            final byte Premium_Bonus_EXP_PERCENT, MapleMonster mob) {
        if (!isAlive() || gain < 0) {
            return;
        }
        // 獲得怪物的魅力性向
        getTrait(MapleTraitType.charisma).addExp(mob.getStats().getCharismaEXP(), this);

        // 詛咒減少50%經驗
        if (hasDisease(MapleDisease.詛咒)) {
            gain /= 2;
        }

        // 出生劇情地圖無伺服器經驗值倍數加成
        if (!GameConstants.isTutorialMap(getMapId())) {
            int ch_exp = ChannelServer.getInstance(map.getChannel()).getExpRate(getWorld());
            gain *= GameConstants.getExpRate(getJob(), ch_exp);
        }
        if (gain < 0) {
            gain = Integer.MAX_VALUE;
        }

        // 組隊經驗值 處理
        int Party_Bonus_EXP = 0;
        if (pty > 1) {
            Party_Bonus_EXP = gain;
            pty = (byte) Math.min(pty, 6);
            Party_Bonus_EXP *= (5 * (pty * (3 + (1 + pty) / 2)) - 20) / 100.0; // (15+5*2)+(15+5*3)+...+(15+5*n)
            if (map != null && mob.getStats().isPartyBonus() && map.getPartyBonusRate() > 0
                    && mob.getStats().getPartyBonusRate() > 0) {
                Party_Bonus_EXP *= 1 + (mob.getStats().getPartyBonusRate() * Math.min(4, pty) / 100.0);
            }
            Party_Bonus_EXP *= 1 + (Premium_Bonus_EXP_PERCENT / 100.0);
        }
        if (Party_Bonus_EXP < 0) {
            Party_Bonus_EXP = Integer.MAX_VALUE;
        }

        // 結婚紅利經驗值 處理
        int Wedding_Bonus_EXP = 0;
        if (marriageId > 0) {
            MapleCharacter marrChr = map.getCharacterById(marriageId);
            if (marrChr != null) {
                Wedding_Bonus_EXP = (int) (gain * 10L / 100.0D);
            }
        }
        if (Wedding_Bonus_EXP < 0) {
            Wedding_Bonus_EXP = Integer.MAX_VALUE;
        }

        // 道具裝備紅利經驗值 處理
        int Equipment_Bonus_EXP = 0;// (int) ((gain / 100.0) * getStat().equipmentBonusExp);
        if (getStat().equippedFairy > 0 && getFairyExp() > 0) {
            Equipment_Bonus_EXP += (int) (gain * (long) getFairyExp() / 100.0);
        }
        if (Equipment_Bonus_EXP < 0) {
            Equipment_Bonus_EXP = Integer.MAX_VALUE;
        }

        // 高級服務贈送經驗值 處理
        int Internet_Cafe_Bonus_EXP = 0;
        if (haveItem(5420008)) {
            Internet_Cafe_Bonus_EXP = (int) (gain * 25L / 100.0D);
        }
        if (Internet_Cafe_Bonus_EXP < 0) {
            Internet_Cafe_Bonus_EXP = Integer.MAX_VALUE;
        }

        // 神使 神之子，精靈的祝福，皮卡的啾品格 額外經驗值 處理
        int Skill_Bonus_EXP = 0;
        // 精靈的祝福處理
        Skill skil = SkillFactory.getSkill(20021110);
        int skilLevel = getTotalSkillLevel(skil);
        if (skilLevel > 0 && MapleJob.is精靈遊俠(getJob())) {
            Skill_Bonus_EXP += (int) (gain * (long) skil.getEffect(skilLevel).getEXPRate() / 100.0D);
        } else {
            skil = SkillFactory.getSkill(80001040);
            skilLevel = getTotalSkillLevel(skil);
            if (skilLevel > 0) {
                Skill_Bonus_EXP += (int) (gain * (long) skil.getEffect(skilLevel).getEXPRate() / 100.0D);
            }
        }
        // 神使 神之子處理
        skil = SkillFactory.getSkill(71000711);
        skilLevel = getTotalSkillLevel(skil);
        if (skilLevel > 0) {
            Skill_Bonus_EXP += (int) (gain * (long) skil.getEffect(skilLevel).getEXPRate() / 100.0D);
        }
        // 皮卡的啾品格處理
        skil = SkillFactory.getSkill(131000016);
        skilLevel = getTotalSkillLevel(skil);
        if (skilLevel > 0) {
            Skill_Bonus_EXP += (int) (gain * (long) skil.getEffect(skilLevel).getEXPRate() / 100.0D);
        }
        if (Skill_Bonus_EXP < 0) {
            Skill_Bonus_EXP = Integer.MAX_VALUE;
        }

        // 獲得追加經驗值 處理
        int Additional_Bonus_EXP = gain;
        // 挑釁
        final MonsterStatusEffect ms = mob.getBuff(MonsterStatus.M_Showdown);
        if (ms != null && ms.getSkill() == 4121017) {
            Additional_Bonus_EXP *= 1 + (ms.getX() / 100.0);
        }
        // 祈禱
        final Integer holySymbol = getBuffedValue(MapleBuffStat.HolySymbol);
        if (holySymbol != null) {
            Additional_Bonus_EXP *= 1 + (holySymbol.doubleValue() / 100.0);
        }
        // 經驗值倍率模式
        Additional_Bonus_EXP *= getEXPMod();
        // 經驗倍率加持
        Additional_Bonus_EXP *= getStat().expBuff / 100.0;
        if (Additional_Bonus_EXP < 0) {
            Additional_Bonus_EXP = Integer.MAX_VALUE;
        }
        Additional_Bonus_EXP -= gain;
        Additional_Bonus_EXP = Math.max(0, Additional_Bonus_EXP);

        // 加持獎勵經驗值處理
        int Buff_Bonus_EXP = 0;
        Buff_Bonus_EXP += gain * (getStat().indieExp - 100.0) / 100.0;
        if (Buff_Bonus_EXP < 0) {
            Buff_Bonus_EXP = Integer.MAX_VALUE;
        }

        // killCombo處理
        if (comboKillExps.isEmpty()) {
            if ((comboKill > 0) && (System.currentTimeMillis() - lastComboKill > 7000)) {
                comboKill = 0;
            }
            comboKill += 1;
            setComboKill(comboKill);
            lastComboKill = System.currentTimeMillis();
            int comboKillDrop = 0;// comboKill % 50 == 0
            if (comboKill < 300) {
                comboKillDrop = 2023484; // 連續擊殺模式 - 藍色
            } else if (comboKill < 700) {
                comboKillDrop = 2023494; // 連續擊殺模式 - 紫色
            } else {
                comboKillDrop = 2023495; // 連續擊殺模式 - 紅色
            }
            comboKillDrop = comboKill % 50 == 0 ? comboKillDrop : 0;
            if (comboKillDrop > 0) {
                Item dropItem = new Item(comboKillDrop, (byte) 0, (short) 1);
                final MapleMapItem mdrop = new MapleMapItem(dropItem, mob.getPosition(), mob, this, (byte) 0, true);
                mdrop.registerExpire(5000);

                getMap().spawnAndAddRangedMapObject(mdrop, (MapleClient c) -> {
                    if (c != null && c.getPlayer() != null && mob != null && mob.getPosition() != null) {
                        c.announce(CField.dropItemFromMapObject(mdrop, mob.getTruePosition(), mob.getPosition(),
                                (byte) 1));
                    }
                });
            }
        }

        // 燃燒場地經驗值處理
        int Buring_Field_Bonus_EXP = 0;
        long Buring_Field_Step = 0;
        MapleMap m = mob.getMap();
        if (m != null) {
            Buring_Field_Step = m.getBurningFieldStep() * 10;
            Buring_Field_Bonus_EXP = (int) Math.floor(gain * Buring_Field_Step / 100.0);
        }
        if (Buring_Field_Bonus_EXP < 0) {
            Buring_Field_Bonus_EXP = Integer.MAX_VALUE;
        }

        // 總經驗
        long total = gain + Party_Bonus_EXP + Wedding_Bonus_EXP + Equipment_Bonus_EXP + Internet_Cafe_Bonus_EXP
                + Skill_Bonus_EXP + Additional_Bonus_EXP + Class_Bonus_EXP + Buff_Bonus_EXP + Buring_Field_Bonus_EXP;

        if (total < 0) { // just in case
            total = Long.MAX_VALUE;
        }
        // 處理給角色加經驗并升級
        if (total > 0) {
            stats.checkEquipLevels(this, total); // gms like
        }
        long needed = getNeededExp();
        if (level >= maxLevel) {
            setExp(0);
        } else {
            boolean leveled = false;
            if (exp + total >= needed || exp >= needed) {
                exp += total;
                while (exp > needed) {
                    levelUp();
                    needed = getNeededExp();
                    if (!FeaturesConfig.levelUpLimitBreak) {
                        break;
                    }
                }
                leveled = true;
                if (level >= maxLevel) {
                    setExp(0);
                } else {
                    needed = getNeededExp();
                    if (exp >= needed) {
                        setExp(needed);
                    }
                }
            } else {
                exp += total;
            }
        }

        // 處理經驗值數據包
        if (total != 0) {
            if (exp < 0) { // After adding, and negative
                if (total > 0) {
                    setExp(getNeededExp());
                } else if (total < 0) {
                    setExp(0);
                }
            }
            updateSingleStat(MapleStat.EXP, getExp());
            if (show) { // still show the expgain even if it's not there
                final Map<ExpMasks, Integer[]> flag = new HashMap<>();
                // 活動組隊經驗值
                if (Class_Bonus_EXP != 0) {
                    flag.put(ExpMasks.PartyBonusPercentage, new Integer[]{(int) Class_Bonus_EXP});
                }
                // 組隊經驗值
                if (Party_Bonus_EXP != 0) {
                    flag.put(ExpMasks.PartyBonusExp, new Integer[]{Party_Bonus_EXP});
                }
                // 結婚紅利經驗值
                if (Wedding_Bonus_EXP != 0) {
                    flag.put(ExpMasks.WeddingBonusExp, new Integer[]{Wedding_Bonus_EXP});
                }
                // 道具裝備紅利經驗值
                if (Equipment_Bonus_EXP != 0) {
                    flag.put(ExpMasks.ItemBonusExp, new Integer[]{Equipment_Bonus_EXP});
                }
                // 高級服務贈送經驗值
                if (Internet_Cafe_Bonus_EXP != 0) {
                    flag.put(ExpMasks.PremiumIPBonusExp, new Integer[]{Internet_Cafe_Bonus_EXP});
                }
                // 神使 神之子，精靈的祝福 額外經驗值
                if (Skill_Bonus_EXP != 0) {
                    flag.put(ExpMasks.PsdBonusExpRate, new Integer[]{Skill_Bonus_EXP});
                }
                // 加持獎勵經驗值
                if (Buff_Bonus_EXP != 0) {
                    flag.put(ExpMasks.IndieBonusExp, new Integer[]{Buff_Bonus_EXP});
                }
                // 獲得追加經驗值
                if (Additional_Bonus_EXP != 0) {
                    flag.put(ExpMasks.BaseAddExp, new Integer[]{Additional_Bonus_EXP});
                }
                // 燃燒場地獎勵經驗 x%
                if (Buring_Field_Bonus_EXP != 0 || Buring_Field_Step != 0) {
                    flag.put(ExpMasks.RestFieldBonusExp,
                            new Integer[]{(int) Buring_Field_Bonus_EXP, (int) Buring_Field_Step});
                }

                client.announce(InfoPacket.OnIncEXPMessage(gain, false, white, flag));
                if (comboKillExps.isEmpty() && comboKill > 1) {
                    String sValue = this.getInfoQuest(GameConstants.COMBO_RECORD).replace("ComboK=", "");
                    int current = 0;
                    if (!sValue.equals("") && StringUtil.isNumber(sValue)) {
                        current = Integer.parseInt(sValue);
                    }
                    if (comboKill > current) {
                        this.updateInfoQuest(GameConstants.COMBO_RECORD, "ComboK=" + comboKill);
                    }
                    client.announce(InfoPacket.OnStylishKillMessage(1, comboKill, mob.getObjectId()));
                }
            }
        }
        // 劍豪一般姿勢效果剣気處理
        if (isBuffed(40011291)) { // 一般姿勢效果
            setJianQi((short) (getJianQi() + 5));
        }
        comboKillExps.add(new Pair<>(gain, mob.getStats().isBoss()));
    }

    public void setComboKill(int mount) {
        comboKill = mount;
    }

    public int getComboKill() {
        return comboKill;
    }

    public void forceReAddItem_NoUpdate(Item item) {
        MapleInventoryType type = GameConstants.getInventoryType(item);
        getInventory(type).removeSlot(item.getPosition());
        getInventory(type).addFromDB(item);
    }

    public void forceReAddItem(Item item) { // used for stuff like durability, item exp/level, probably owner?
        MapleInventoryType type = GameConstants.getInventoryType(item);
        forceReAddItem_NoUpdate(item);
        if (type != MapleInventoryType.UNDEFINED) {
            client.announce(InventoryPacket.addInventorySlot(item, false));
            // client.announce(InventoryPacket.updateSpecialItemUse(item));
        }
    }

    public void forceReAddItem_Flag(Item item) { // used for flags
        MapleInventoryType type = GameConstants.getInventoryType(item);
        forceReAddItem_NoUpdate(item);
        if (type != MapleInventoryType.UNDEFINED) {
            client.announce(InventoryPacket.updateSpecialItemUse_(item));
        }
    }

    public void forceReAddItem_Book(Item item) { // used for mbook
        MapleInventoryType type = GameConstants.getInventoryType(item);
        forceReAddItem_NoUpdate(item);
        if (type != MapleInventoryType.UNDEFINED) {
            client.announce(CWvsContext.upgradeBook(item, this));
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            World.Party.updateParty(party.getId(), PartyOperation.PartyRes_LoadParty_Done,
                    new MaplePartyCharacter(this));
        }
    }

    public boolean isIntern() {
        return gmLevel >= PlayerGMRank.INTERN.getLevel();
    }

    public boolean isGM() {
        return gmLevel >= PlayerGMRank.GM.getLevel();
    }

    public boolean isSuperGM() {
        return gmLevel >= PlayerGMRank.SUPERGM.getLevel();
    }

    public boolean isAdmin() {
        return gmLevel >= PlayerGMRank.ADMIN.getLevel();
    }

    public int getGmLevel() {
        return gmLevel;
    }

    public boolean hasGmLevel(int level) {
        return gmLevel >= level;
    }

    public boolean isDonator() {
        if (getInventory(MapleInventoryType.EQUIP).findById(1142229) != null) {
            // updateDonorMedal(getInventory(MapleInventoryType.EQUIP).findById(1142229));
            return true;
        }
        if (getInventory(MapleInventoryType.EQUIPPED).findById(1142229) != null) {
            // updateDonorMedal(getInventory(MapleInventoryType.EQUIPPED).findById(1142229));
            return true;
        }
        return false;
    }

    public final MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public final MapleInventory[] getInventorys() {
        return inventory;
    }

    public final void expirationTask(boolean pending, boolean firstLoad) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (pending) {
            if (pendingExpiration != null) {
                for (Integer z : pendingExpiration) {
                    client.announce(InfoPacket.itemExpired(z));
                    if (!firstLoad) {
                        final Pair<Integer, String> replace = ii.replaceItemInfo(z);
                        if (replace != null && replace.left > 0 && replace.right.length() > 0) {
                            dropMessage(5, replace.right);
                        }
                    }
                }
            }
            pendingExpiration = null;
            if (pendingSkills != null) {
                client.announce(CWvsContext.updateSkills(pendingSkills, false));
                for (Skill z : pendingSkills.keySet()) {
                    client.announce(CWvsContext.broadcastMsg(5, "[" + SkillFactory.getSkillName(z.getId())
                            + "] skill has expired and will not be available for use."));
                }
            } // not real msg
            pendingSkills = null;
            return;
        }
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.墜飾欄));
        long expiration;
        final List<Integer> ret = new ArrayList<>();
        final long currenttime = System.currentTimeMillis();
        final List<Triple<MapleInventoryType, Item, Boolean>> toberemove = new ArrayList<>(); // This is here to prevent
        // deadlock.
        final List<Item> tobeunlock = new ArrayList<>(); // This is here to prevent deadlock.

        for (final MapleInventoryType inv : MapleInventoryType.values()) {
            for (final Item item : getInventory(inv)) {
                expiration = item.getExpiration();

                if ((expiration != -1 && !ItemConstants.類型.寵物(item.getItemId()) && currenttime > expiration)
                        || (firstLoad && ii.isLogoutExpire(item.getItemId()))) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        tobeunlock.add(item);
                    } else if (currenttime > expiration) {
                        toberemove.add(new Triple<>(inv, item, false));
                    }
                } else if (ItemConstants.類型.寵物(item.getItemId()) && ii.getLimitedLife(item.getItemId()) > 0
                        && item.getPet() != null && item.getPet().getLimitedLife() <= 0) {
                    toberemove.add(new Triple<>(inv, item, false));
                } else if (item.getPosition() == -59) {
                    if (stat == null || stat.getCustomData() == null
                            || Long.parseLong(stat.getCustomData()) < currenttime) {
                        toberemove.add(new Triple<>(inv, item, true));
                    }
                }
            }
        }
        Item item;
        for (final Triple<MapleInventoryType, Item, Boolean> itemz : toberemove) {
            item = itemz.getMid();
            getInventory(itemz.getLeft()).removeItem(item.getPosition(), item.getQuantity(), false);
            if (itemz.getRight() && getInventory(GameConstants.getInventoryType(item)).getNextFreeSlot() > -1) {
                item.setPosition(getInventory(GameConstants.getInventoryType(item)).getNextFreeSlot());
                getInventory(GameConstants.getInventoryType(item)).addFromDB(item);
            } else {
                ret.add(item.getItemId());
            }
            if (!firstLoad) {
                final Pair<Integer, String> replace = ii.replaceItemInfo(item.getItemId());
                if (replace != null && replace.left > 0) {
                    Item theNewItem;
                    if (GameConstants.getInventoryType(replace.left) == MapleInventoryType.EQUIP) {
                        theNewItem = ii.getEquipById(replace.left);
                        theNewItem.setPosition(item.getPosition());
                    } else {
                        theNewItem = new Item(replace.left, item.getPosition(), (short) 1, 0);
                    }
                    getInventory(itemz.getLeft()).addFromDB(theNewItem);
                }
            }
        }
        for (final Item itemz : tobeunlock) {
            itemz.setExpiration(-1);
            itemz.setFlag(itemz.getFlag() - ItemFlag.LOCK.getValue());
        }
        this.pendingExpiration = ret;

        final Map<Skill, SkillEntry> skilz = new HashMap<>();
        final List<Skill> toberem = new ArrayList<>();
        for (Entry<Skill, SkillEntry> skil : skills.entrySet()) {
            if (skil.getValue().expiration != -1 && currenttime > skil.getValue().expiration) {
                toberem.add(skil.getKey());
            }
        }
        for (Skill skil : toberem) {
            skilz.put(skil, new SkillEntry(0, (byte) 0, -1));
            this.skills.remove(skil);
            changed_skills = true;
        }
        this.pendingSkills = skilz;
        if (stat != null && stat.getCustomData() != null && !"0".equals(stat.getCustomData())
                && Long.parseLong(stat.getCustomData()) < currenttime) { // expired bro
            quests.remove(MapleQuest.getInstance(7830));
            quests.remove(MapleQuest.getInstance(GameConstants.墜飾欄));
        }
    }

    public void refreshBattleshipHP() {
        if (getJob() == 592) {
            client.announce(CWvsContext.giveKilling(currentBattleshipHP()));
        }
    }

    public MapleShop getShop() {
        return shop;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public long getMeso() {
        return meso;
    }

    public final int[] getSavedLocations() {
        return savedLocations;
    }

    public int getSavedLocation(SavedLocationType type) {
        return savedLocations[type.getValue()];
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = getMapId();
        changed_savedlocations = true;
    }

    public void saveLocation(SavedLocationType type, int mapz) {
        savedLocations[type.getValue()] = mapz;
        changed_savedlocations = true;
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = -1;
        changed_savedlocations = true;
    }

    public void gainMeso(long gain, boolean show) {
        gainMeso(gain, show, false);
    }

    public void gainMeso(long gain, boolean show, boolean inChat) {
        if (meso + gain < 0L) {
            gain = -meso;
            meso = 0;
        } else {
            meso += gain;
        }

        // 楓幣大於上限
        if (meso > FeaturesConfig.MESO_MAX) {
            gain -= meso - FeaturesConfig.MESO_MAX;
            meso = FeaturesConfig.MESO_MAX;
        }

        updateSingleStat(MapleStat.MESO, meso, false);
        client.announce(CWvsContext.enableActions());
        if (show) {
            client.announce(InfoPacket.showMesoGain(gain, inChat));
        }
    }

    public void controlMonster(MapleMonster monster, boolean aggro) {
        if (isClone() || monster == null) {
            return;
        }
        monster.setController(this);
        controlledLock.writeLock().lock();
        try {
            controlled.add(monster);
        } finally {
            controlledLock.writeLock().unlock();
        }
        client.announce(MobPacket.controlMonster(monster, false, aggro, GameConstants.isAzwanMap(getMapId())));

        monster.sendStatus(client);
    }

    public void stopControllingMonster(MapleMonster monster) {
        if (isClone() || monster == null) {
            return;
        }
        controlledLock.writeLock().lock();
        try {
            if (controlled.contains(monster)) {
                controlled.remove(monster);
            }
        } finally {
            controlledLock.writeLock().unlock();
        }
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (isClone() || monster == null) {
            return;
        }
        if (monster.getController() == this) {
            monster.setControllerHasAggro(true);
        } else {
            monster.switchController(this, true);
        }
    }

    public int getControlledSize() {
        return controlled.size();
    }

    public int getAccountID() {
        return accountid;
    }

    public void mobKilled(final int id, final int skillID) {
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() != 1 || !q.hasMobKills()) {
                continue;
            }
            if (q.mobKilled(id, skillID)) {
                client.announce(InfoPacket.updateQuestMobKills(q));
                if (q.getQuest().getId() == 59005) {
                    updateInfoQuest(59005, "mob=" + q.getMobKills().get(id));
                }
                if (q.getQuest().canComplete(this, null)) {
                    client.announce(CWvsContext.showQuestCompletion(q.getQuest().getId()));
                }
            }
        }
    }

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 1 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<Pair<Integer, Long>> getCompletedMedals() {
        List<Pair<Integer, Long>> ret = new ArrayList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked() && q.getQuest().getMedalItem() > 0
                    && GameConstants.getInventoryType(q.getQuest().getMedalItem()) == MapleInventoryType.EQUIP) {
                ret.add(new Pair<>(q.getQuest().getId(), q.getCompletionTime()));
            }
        }
        return ret;
    }

    public void setLastLevelup(long time) {
        lastLevelup = time;
    }

    public Map<Skill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public int getAllSkillLevels() {
        int rett = 0;
        for (Entry<Skill, SkillEntry> ret : skills.entrySet()) {
            if (!ret.getKey().isBeginnerSkill() && !ret.getKey().isSpecialSkill() && ret.getValue().skillLevel > 0) {
                rett += ret.getValue().skillLevel;
            }
        }
        return rett;
    }

    public long getSkillExpiry(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillLevel <= 0) {
            return 0;
        }
        return ret.expiration;
    }

    public int getSkillLevel(int skillid) {
        return getSkillLevel(SkillFactory.getSkill(skillid));
    }

    public int getTotalSkillLevel(int skillid) {
        return getTotalSkillLevel(SkillFactory.getSkill(skillid));
    }

    public int getPassiveLevel(final Skill skill) {
        if (skill == null || !skill.canBeLearnedBy(job)) {
            return 0;
        }
        return getTotalSkillLevel(skill);
    }

    public int getTotalSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }

        if (getLevel() < skill.getReqLevel()) {
            return 0;
        }

        switch (skill.getId()) {
            case 80001770: // 升級攻擊光效
                return System.currentTimeMillis() - lastLevelup > 3000 ? 0 : 1;
            case 80001429: // 崩壞之輪
                return getTouchedRune() == 2 ? 1 : 0;
            case 80001431: // 破滅之輪
                return getTouchedRune() == 3 ? 1 : 0;
        }

        // 武陵道場、金字塔、巨大藥水
        if (GameConstants.isMulungSkill(skill.getId()) || GameConstants.isPyramidSkill(skill.getId())
                || GameConstants.isInflationSkill(skill.getId())) {
            return 1;
        }

        // 連結攻擊
        int linkAttack = GameConstants.getLinkedAttackSkill(skill.getId());
        if (linkAttack != skill.getId()) {
            return getTotalSkillLevel(SkillFactory.getSkill(linkAttack));
        }

        int skillLevel = getSkillLevel(skill);

        // 對特定技能增加了等級
        skillLevel += stats.getSkillIncrement(skill.getId());

        int maxLv = skill.getMaxLevel();
        if (!skill.isBeginnerSkill()) {
            if (skillLevel > 0) {
                // 對全部技能增加了等級
                skillLevel += stats.incAllskill;
                // 戰鬥命令增加等級
                skillLevel += stats.combatOrders;
                // 限定戰鬥命令突破技能的上限
                if (stats.combatOrders > 0 && skillLevel + stats.combatOrders > maxLv) {
                    maxLv = Math.min(skill.getTrueMax(), skillLevel + stats.combatOrders);
                }
            }
        }

        skillLevel = Math.min(maxLv, skillLevel);
        return skillLevel;
    }

    public int getSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        int skillLevel;
        SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillLevel <= 0) {
            if (ret != null && ret.masterlevel == -1) {
                skillLevel = 1;
            } else {
                return 0;
            }
        } else {
            skillLevel = ret.skillLevel;
        }
        return skillLevel;
    }

    public byte getMasterLevel(final int skill) {
        return getMasterLevel(SkillFactory.getSkill(skill));
    }

    public byte getMasterLevel(final Skill skill) {
        final SkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }

    public final int maxLevel = 250;

    public void levelUp() {
        if (getLevel() >= maxLevel) {
            return;
        }
        remainingAp += 5;
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (job == 3001 || job == 10000) {// 惡魔殺手,神之子 的新手
            maxhp += Randomizer.rand(52, 56);
            maxmp = 30;
        } else if (MapleJob.isBeginner(job)) { // 新手 (無惡魔,神之子)
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (MapleJob.is劍士(job) && (MapleJob.is冒險家(job) || MapleJob.is皇家騎士團(job))) { // 劍士、聖魂劍士
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (MapleJob.is狂狼勇士(job)) { // 狂狼勇士
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (MapleJob.is惡魔殺手(job)) { // 惡魔殺手
            maxhp += Randomizer.rand(48, 52);
        } else if (MapleJob.is惡魔復仇者(job)) { // 惡魔復仇者
            maxhp += Randomizer.rand(70, 105);
        } else if (MapleJob.is米哈逸(job)) { // 米哈逸
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (MapleJob.is劍豪(job)) { // 劍豪
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (MapleJob.is凱撒(job)) { // 凱薩
            maxhp += Randomizer.rand(70, 105);
            maxmp += Randomizer.rand(10, 20);
        } else if (MapleJob.is神之子(job)) { // 神之子
            maxhp += Randomizer.rand(70, 105);
        } else if (MapleJob.is皮卡啾(job)) { // 皮卡啾
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (MapleJob.is法師(job) && (MapleJob.is冒險家(job) || MapleJob.is皇家騎士團(job))) { // 法師、烈焰巫師
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (MapleJob.is龍魔導士(job)) { // 龍魔導士
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else if (MapleJob.is夜光(job)) { // 夜光
            maxhp += Randomizer.rand(25, 40);
            maxmp += Randomizer.rand(60, 100);
        } else if (MapleJob.is煉獄巫師(job)) { // 煉獄巫師
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if (MapleJob.is陰陽師(job) || MapleJob.is凱內西斯(job)) { // 陰陽師、凱內西斯
            maxhp += Randomizer.rand(48, 52);
        } else if (MapleJob.is弓箭手(job) || (MapleJob.is盜賊(job) && (MapleJob.is冒險家(job) || MapleJob.is皇家騎士團(job)))) {
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if (MapleJob.is幻影俠盜(job)) { // 幻影
            maxhp += Randomizer.rand(56, 67);
            maxmp += Randomizer.rand(74, 100);
        } else if (MapleJob.is拳霸(job)) { // 拳霸
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if (MapleJob.is海盜(job) && MapleJob.is冒險家(job)) { // 除了拳霸的冒險家海盜
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (MapleJob.is閃雷悍將(job) || MapleJob.is機甲戰神(job)) { // 閃雷悍將 機甲戰神
            maxhp += Randomizer.rand(56, 67);
            maxmp += Randomizer.rand(34, 47);
        } else if (MapleJob.is隱月(job)) { // 隱月
            maxhp += Randomizer.rand(66, 77);
            maxmp += Randomizer.rand(44, 57);
        } else if (MapleJob.is傑諾(job)) { // 傑諾
            maxhp += Randomizer.rand(100, 130);
            maxmp += Randomizer.rand(10, 15);
        } else if (MapleJob.is天使破壞者(job)) { // 天使破壞者
            maxhp += Randomizer.rand(56, 67);
        } else if (MapleJob.is幻獸師(job)) { // 幻獸師
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(20, 24);
        } else {
            System.err.println("職業 " + MapleJob.getById(job).name() + " 未處理升級HPMP增加");
        }
        maxmp += stats.getTotalInt() / 10;

        exp -= getNeededExp();
        if (exp < 0) {
            exp = 0;
        }
        level += 1;
        maxhp = Math.min(500000, Math.abs(maxhp));
        maxmp = Math.min(500000, Math.abs(maxmp));
        if (MapleJob.is惡魔殺手(job) || MapleJob.is凱內西斯(job)) {
            maxmp = GameConstants.getMPByJob(this);
        } else if (MapleJob.is神之子(job) || MapleJob.is陰陽師(job)) {
            maxmp = 100;
        } else if (MapleJob.isNotMpJob(job)) {
            maxmp = 0;
        }
        final Map<MapleStat, Long> statup = new EnumMap<>(MapleStat.class);

        statup.put(MapleStat.MAXHP, (long) maxhp);
        statup.put(MapleStat.MAXMP, (long) maxmp);
        statup.put(MapleStat.HP, (long) maxhp);
        statup.put(MapleStat.MP, (long) maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (long) level);
        updateSingleStat(MapleStat.AVAILABLESP, 0);

        // 清除幻獸師創建時多餘的力量值
        if (level <= 2 && MapleJob.is幻獸師(job)) {
            resetStats(4, 4, 4, 4);
            // 蒼龍10等時還原AP
        } else if (level == 10 && MapleJob.is蒼龍俠客(job)) {
            resetStats(4, 4, 4, 4);
            // 10等之前自動配點到力量
        } else if (level <= 10
                // 不需要自動配點的職業
                && !MapleJob.is幻獸師(job)) {
            stats.力量 += remainingAp;
            remainingAp = 0;
            statup.put(MapleStat.STR, (long) stats.getStr());
        }
        if (MapleJob.is神之子(job)) {
            if (level >= 100 && level <= 200) {
                remainingSp[0] += 3; // alpha gets 3sp
                remainingSp[1] += 3; // beta gets 3sp
            }
        } else if (level > 10 && level <= 200) {
            int point = 3;
            if (level > 100 && level <= 140) {
                point += level <= 110 ? 0 : (level - 1) / 10 % 10;
                point *= level % 10 == 3 || level % 10 == 6 || level % 10 == 9 || level % 10 == 0 ? 2 : 1;
            }
            if (level > 140 || MapleJob.is皮卡啾(job)) {
                point = 0;
            }
            remainingSp[GameConstants.getSkillBook(job, level)] += point;
        }

        statup.put(MapleStat.AVAILABLEAP, (long) remainingAp);
        statup.put(MapleStat.AVAILABLESP, (long) remainingSp[GameConstants.getSkillBook(job, level)]);
        obtainHyperSP();
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.announce(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showLevelupEffect(this), false);
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        autoJob();

        if (MapleJob.is神之子(job)) {
            checkZeroWeapon();
            checkZeroTranscendent();
        }
        MapleAndroid a = getAndroid();
        if (a != null) {
            a.showEmotion(this, "levelup");
        }
        if (level == 10) {
            client.announce(CField.onAddPopupSay(9010000, 6000,
                    "#b[說明] 1轉#k\r\n\r\n" + "達成等級10而可以進行 #b[1轉]#k了呢！\r\n\r\n" + "#r完成[轉職]#k 任務來達成第一次轉職吧！\r\n",
                    "FarmSE.img/boxResult"));
        } else if (level == 250 && !isIntern()) {
            setExp(0);
            StringBuilder sb = new StringBuilder();
            sb.append("[恭喜] ");
            addMedalString(client.getPlayer(), sb);
            sb.append(getName()).append("達到了等級").append(level).append("級！請大家一起恭喜他！");
            // addMedalString(client.getPlayer(), sb);
            // sb.append(getName()).append(" on such an amazing achievement!");
            World.Broadcast.broadcastMessage(CWvsContext.broadcastMsg(6, sb.toString()));
        }
        // if (map.getForceMove() > 0 && map.getForceMove() <= getLevel()) {
        // changeMap(map.getReturnMap(), map.getReturnMap().getPortal(0));
        // dropMessage(-1, "You have been expelled from the map.");
        // }
        checkCustomReward(level);
        baseSkills();
        stats.heal(this);
        lastLevelup = System.currentTimeMillis();
    }

    public void obtainHyperSP() {
        if (MapleJob.is神之子(job)) {
            return; // no hypers for zero
        }
        if (MapleJob.is皮卡啾(job)) {
            return;
        }
        switch (level) {
            case 140:
            case 160:
            case 180:
            case 190:
                gainHSP(0, 1);
                break;
            case 150:
            case 170:
                gainHSP(1, 1);
                break;
            case 200:
                gainHSP(0, 1);
                gainHSP(1, 1);
                break;
        }
    }

    // 自動轉職
    public void autoJob() {
        if (MapleJob.is英雄(job)) {
            if (getLevel() >= 60 && job < MapleJob.十字軍.getId()) {
                changeJob((short) MapleJob.十字軍.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.英雄.getId()) {
                changeJob((short) MapleJob.英雄.getId());
            }
        } else if (MapleJob.is聖騎士(job)) {
            if (getLevel() >= 60 && job < MapleJob.騎士.getId()) {
                changeJob((short) MapleJob.騎士.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.聖騎士.getId()) {
                changeJob((short) MapleJob.聖騎士.getId());
            }
        } else if (MapleJob.is黑騎士(job)) {
            if (getLevel() >= 60 && job < MapleJob.嗜血狂騎.getId()) {
                changeJob((short) MapleJob.嗜血狂騎.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.黑騎士.getId()) {
                changeJob((short) MapleJob.黑騎士.getId());
            }
        } else if (MapleJob.is大魔導士_火毒(job)) {
            if (getLevel() >= 60 && job < MapleJob.魔導士_火毒.getId()) {
                changeJob((short) MapleJob.魔導士_火毒.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.大魔導士_火毒.getId()) {
                changeJob((short) MapleJob.大魔導士_火毒.getId());
            }
        } else if (MapleJob.is大魔導士_冰雷(job)) {
            if (getLevel() >= 60 && job < MapleJob.魔導士_冰雷.getId()) {
                changeJob((short) MapleJob.魔導士_冰雷.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.大魔導士_冰雷.getId()) {
                changeJob((short) MapleJob.大魔導士_冰雷.getId());
            }
        } else if (MapleJob.is主教(job)) {
            if (getLevel() >= 60 && job < MapleJob.祭司.getId()) {
                changeJob((short) MapleJob.祭司.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.主教.getId()) {
                changeJob((short) MapleJob.主教.getId());
            }
        } else if (MapleJob.is箭神(job)) {
            if (getLevel() >= 60 && job < MapleJob.遊俠.getId()) {
                changeJob((short) MapleJob.遊俠.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.箭神.getId()) {
                changeJob((short) MapleJob.箭神.getId());
            }
        } else if (MapleJob.is神射手(job)) {
            if (getLevel() >= 60 && job < MapleJob.狙擊手.getId()) {
                changeJob((short) MapleJob.狙擊手.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.神射手.getId()) {
                changeJob((short) MapleJob.神射手.getId());
            }
        } else if (MapleJob.is夜使者(job)) {
            if (getLevel() >= 60 && job < MapleJob.暗殺者.getId()) {
                changeJob((short) MapleJob.暗殺者.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.夜使者.getId()) {
                changeJob((short) MapleJob.夜使者.getId());
            }
        } else if (MapleJob.is暗影神偷(job)) {
            if (getLevel() >= 60 && job < MapleJob.神偷.getId()) {
                changeJob((short) MapleJob.神偷.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.暗影神偷.getId()) {
                changeJob((short) MapleJob.暗影神偷.getId());
            }
        } else if (MapleJob.is影武者(job) || (subcategory == 1 && job == 400)) {
            if (getLevel() >= 20 && job < MapleJob.下忍.getId()) {
                changeJob((short) MapleJob.下忍.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.中忍.getId()) {
                changeJob((short) MapleJob.中忍.getId());
            }
            if (getLevel() >= 45 && job < MapleJob.上忍.getId()) {
                changeJob((short) MapleJob.上忍.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.隱忍.getId()) {
                changeJob((short) MapleJob.隱忍.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.影武者.getId()) {
                changeJob((short) MapleJob.影武者.getId());
            }
        } else if (MapleJob.is拳霸(job)) {
            if (getLevel() >= 60 && job < MapleJob.格鬥家.getId()) {
                changeJob((short) MapleJob.格鬥家.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.拳霸.getId()) {
                changeJob((short) MapleJob.拳霸.getId());
            }
        } else if (MapleJob.is槍神(job)) {
            if (getLevel() >= 60 && job < MapleJob.神槍手.getId()) {
                changeJob((short) MapleJob.神槍手.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.槍神.getId()) {
                changeJob((short) MapleJob.槍神.getId());
            }
        } else if (MapleJob.is重砲指揮官(job) || (subcategory == 2 && job == 0)) {
            if (getLevel() >= 10 && job < MapleJob.砲手.getId()) {
                changeJob((short) MapleJob.砲手.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.重砲兵.getId()) {
                changeJob((short) MapleJob.重砲兵.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.重砲兵隊長.getId()) {
                changeJob((short) MapleJob.重砲兵隊長.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.重砲指揮官.getId()) {
                changeJob((short) MapleJob.重砲指揮官.getId());
            }
        } else if (MapleJob.is蒼龍俠客(job)) {
            if (getLevel() >= 30 && job < MapleJob.蒼龍俠客2轉.getId()) {
                changeJob((short) MapleJob.蒼龍俠客2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.蒼龍俠客3轉.getId()) {
                changeJob((short) MapleJob.蒼龍俠客3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.蒼龍俠客4轉.getId()) {
                changeJob((short) MapleJob.蒼龍俠客4轉.getId());
            }
        } else if (MapleJob.is聖魂劍士(job)) {
            if (getLevel() >= 30 && job < MapleJob.聖魂劍士2轉.getId()) {
                changeJob((short) MapleJob.聖魂劍士2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.聖魂劍士3轉.getId()) {
                changeJob((short) MapleJob.聖魂劍士3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.聖魂劍士4轉.getId()) {
                changeJob((short) MapleJob.聖魂劍士4轉.getId());
            }
        } else if (MapleJob.is烈焰巫師(job)) {
            if (getLevel() >= 30 && job < MapleJob.烈焰巫師2轉.getId()) {
                changeJob((short) MapleJob.烈焰巫師2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.烈焰巫師3轉.getId()) {
                changeJob((short) MapleJob.烈焰巫師3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.烈焰巫師4轉.getId()) {
                changeJob((short) MapleJob.烈焰巫師4轉.getId());
            }
        } else if (MapleJob.is破風使者(job)) {
            if (getLevel() >= 30 && job < MapleJob.破風使者2轉.getId()) {
                changeJob((short) MapleJob.破風使者2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.破風使者3轉.getId()) {
                changeJob((short) MapleJob.破風使者3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.破風使者4轉.getId()) {
                changeJob((short) MapleJob.破風使者4轉.getId());
            }
        } else if (MapleJob.is暗夜行者(job)) {
            if (getLevel() >= 30 && job < MapleJob.暗夜行者2轉.getId()) {
                changeJob((short) MapleJob.暗夜行者2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.暗夜行者3轉.getId()) {
                changeJob((short) MapleJob.暗夜行者3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.暗夜行者4轉.getId()) {
                changeJob((short) MapleJob.暗夜行者4轉.getId());
            }
        } else if (MapleJob.is閃雷悍將(job)) {
            if (getLevel() >= 30 && job < MapleJob.閃雷悍將2轉.getId()) {
                changeJob((short) MapleJob.閃雷悍將2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.閃雷悍將3轉.getId()) {
                changeJob((short) MapleJob.閃雷悍將3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.閃雷悍將4轉.getId()) {
                changeJob((short) MapleJob.閃雷悍將4轉.getId());
            }
        } else if (MapleJob.is狂狼勇士(job)) {
            if (getLevel() >= 10 && job < MapleJob.狂狼勇士1轉.getId()) {
                changeJob((short) MapleJob.狂狼勇士1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.狂狼勇士2轉.getId()) {
                changeJob((short) MapleJob.狂狼勇士2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.狂狼勇士3轉.getId()) {
                changeJob((short) MapleJob.狂狼勇士3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.狂狼勇士4轉.getId()) {
                changeJob((short) MapleJob.狂狼勇士4轉.getId());
            }
        } else if (MapleJob.is龍魔導士(job)) {
            if (getLevel() >= 10 && job < MapleJob.龍魔導士1轉.getId()) {
                changeJob((short) MapleJob.龍魔導士1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.龍魔導士2轉.getId()) {
                changeJob((short) MapleJob.龍魔導士2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.龍魔導士3轉.getId()) {
                changeJob((short) MapleJob.龍魔導士3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.龍魔導士4轉.getId()) {
                changeJob((short) MapleJob.龍魔導士4轉.getId());
            }
        } else if (MapleJob.is精靈遊俠(job)) {
            if (getLevel() >= 10 && job < MapleJob.精靈遊俠1轉.getId()) {
                changeJob((short) MapleJob.精靈遊俠1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.精靈遊俠2轉.getId()) {
                changeJob((short) MapleJob.精靈遊俠2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.精靈遊俠3轉.getId()) {
                changeJob((short) MapleJob.精靈遊俠3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.精靈遊俠4轉.getId()) {
                changeJob((short) MapleJob.精靈遊俠4轉.getId());
            }
        } else if (MapleJob.is幻影俠盜(job)) {
            if (getLevel() >= 10 && job < MapleJob.幻影俠盜1轉.getId()) {
                changeJob((short) MapleJob.幻影俠盜1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.幻影俠盜2轉.getId()) {
                changeJob((short) MapleJob.幻影俠盜2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.幻影俠盜3轉.getId()) {
                changeJob((short) MapleJob.幻影俠盜3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.幻影俠盜4轉.getId()) {
                changeJob((short) MapleJob.幻影俠盜4轉.getId());
            }
        } else if (MapleJob.is夜光(job)) {
            if (getLevel() >= 10 && job < MapleJob.夜光1轉.getId()) {
                changeJob((short) MapleJob.夜光1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.夜光2轉.getId()) {
                changeJob((short) MapleJob.夜光2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.夜光3轉.getId()) {
                changeJob((short) MapleJob.夜光3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.夜光4轉.getId()) {
                changeJob((short) MapleJob.夜光4轉.getId());
            }
        } else if (MapleJob.is隱月(job)) {
            if (getLevel() >= 10 && job < MapleJob.隱月1轉.getId()) {
                changeJob((short) MapleJob.隱月1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.隱月2轉.getId()) {
                changeJob((short) MapleJob.隱月2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.隱月3轉.getId()) {
                changeJob((short) MapleJob.隱月3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.隱月4轉.getId()) {
                changeJob((short) MapleJob.隱月4轉.getId());
            }
        } else if (MapleJob.is惡魔殺手(job)) {
            if (getLevel() >= 30 && job < MapleJob.惡魔殺手2轉.getId()) {
                changeJob((short) MapleJob.惡魔殺手2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.惡魔殺手3轉.getId()) {
                changeJob((short) MapleJob.惡魔殺手3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.惡魔殺手4轉.getId()) {
                changeJob((short) MapleJob.惡魔殺手4轉.getId());
            }
        } else if (MapleJob.is惡魔復仇者(job)) {
            if (getLevel() >= 30 && job < MapleJob.惡魔復仇者2轉.getId()) {
                changeJob((short) MapleJob.惡魔復仇者2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.惡魔復仇者3轉.getId()) {
                changeJob((short) MapleJob.惡魔復仇者3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.惡魔復仇者4轉.getId()) {
                changeJob((short) MapleJob.惡魔復仇者4轉.getId());
            }
        } else if (MapleJob.is煉獄巫師(job)) {
            if (getLevel() >= 30 && job < MapleJob.煉獄巫師2轉.getId()) {
                changeJob((short) MapleJob.煉獄巫師2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.煉獄巫師3轉.getId()) {
                changeJob((short) MapleJob.煉獄巫師3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.煉獄巫師4轉.getId()) {
                changeJob((short) MapleJob.煉獄巫師4轉.getId());
            }
        } else if (MapleJob.is狂豹獵人(job)) {
            if (getLevel() >= 30 && job < MapleJob.狂豹獵人2轉.getId()) {
                changeJob((short) MapleJob.狂豹獵人2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.狂豹獵人3轉.getId()) {
                changeJob((short) MapleJob.狂豹獵人3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.狂豹獵人4轉.getId()) {
                changeJob((short) MapleJob.狂豹獵人4轉.getId());
            }
        } else if (MapleJob.is機甲戰神(job)) {
            if (getLevel() >= 30 && job < MapleJob.機甲戰神2轉.getId()) {
                changeJob((short) MapleJob.機甲戰神2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.機甲戰神3轉.getId()) {
                changeJob((short) MapleJob.機甲戰神3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.機甲戰神4轉.getId()) {
                changeJob((short) MapleJob.機甲戰神4轉.getId());
            }
        } else if (MapleJob.is傑諾(job)) {
            if (getLevel() >= 10 && job < MapleJob.傑諾1轉.getId()) {
                changeJob((short) MapleJob.傑諾1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.傑諾2轉.getId()) {
                changeJob((short) MapleJob.傑諾2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.傑諾3轉.getId()) {
                changeJob((short) MapleJob.傑諾3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.傑諾4轉.getId()) {
                changeJob((short) MapleJob.傑諾4轉.getId());
            }
        } else if (MapleJob.is爆拳槍神(job)) {
            if (getLevel() >= 30 && job < MapleJob.爆拳槍神2轉.getId()) {
                changeJob((short) MapleJob.爆拳槍神2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.爆拳槍神3轉.getId()) {
                changeJob((short) MapleJob.爆拳槍神3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.爆拳槍神4轉.getId()) {
                changeJob((short) MapleJob.爆拳槍神4轉.getId());
            }
        } else if (MapleJob.is劍豪(job)) {
            if (getLevel() >= 10 && job < MapleJob.劍豪1轉.getId()) {
                changeJob((short) MapleJob.劍豪1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.劍豪2轉.getId()) {
                changeJob((short) MapleJob.劍豪2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.劍豪3轉.getId()) {
                changeJob((short) MapleJob.劍豪3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.劍豪4轉.getId()) {
                changeJob((short) MapleJob.劍豪4轉.getId());
            }
        } else if (MapleJob.is陰陽師(job)) {
            if (getLevel() >= 10 && job < MapleJob.陰陽師1轉.getId()) {
                changeJob((short) MapleJob.陰陽師1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.陰陽師2轉.getId()) {
                changeJob((short) MapleJob.陰陽師2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.陰陽師3轉.getId()) {
                changeJob((short) MapleJob.陰陽師3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.陰陽師4轉.getId()) {
                changeJob((short) MapleJob.陰陽師4轉.getId());
            }
        } else if (MapleJob.is米哈逸(job)) {
            if (getLevel() >= 10 && job < MapleJob.米哈逸1轉.getId()) {
                changeJob((short) MapleJob.米哈逸1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.米哈逸2轉.getId()) {
                changeJob((short) MapleJob.米哈逸2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.米哈逸3轉.getId()) {
                changeJob((short) MapleJob.米哈逸3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.米哈逸4轉.getId()) {
                changeJob((short) MapleJob.米哈逸4轉.getId());
            }
        } else if (MapleJob.is凱撒(job)) {
            if (getLevel() >= 10 && job < MapleJob.凱撒1轉.getId()) {
                changeJob((short) MapleJob.凱撒1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.凱撒2轉.getId()) {
                changeJob((short) MapleJob.凱撒2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.凱撒3轉.getId()) {
                changeJob((short) MapleJob.凱撒3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.凱撒4轉.getId()) {
                changeJob((short) MapleJob.凱撒4轉.getId());
            }
        } else if (MapleJob.is卡蒂娜(job)) {
            if (getLevel() >= 10 && job < MapleJob.卡蒂娜1轉.getId()) {
                changeJob((short) MapleJob.卡蒂娜1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.卡蒂娜2轉.getId()) {
                changeJob((short) MapleJob.卡蒂娜2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.卡蒂娜3轉.getId()) {
                changeJob((short) MapleJob.卡蒂娜3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.卡蒂娜4轉.getId()) {
                changeJob((short) MapleJob.卡蒂娜4轉.getId());
            }
        } else if (MapleJob.is天使破壞者(job)) {
            if (getLevel() >= 10 && job < MapleJob.天使破壞者1轉.getId()) {
                changeJob((short) MapleJob.天使破壞者1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.天使破壞者2轉.getId()) {
                changeJob((short) MapleJob.天使破壞者2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.天使破壞者3轉.getId()) {
                changeJob((short) MapleJob.天使破壞者3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.天使破壞者4轉.getId()) {
                changeJob((short) MapleJob.天使破壞者4轉.getId());
            }
        } else if (MapleJob.is凱內西斯(job)) {
            if (getLevel() >= 10 && job < MapleJob.凱內西斯1轉.getId()) {
                changeJob((short) MapleJob.凱內西斯1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.凱內西斯2轉.getId()) {
                changeJob((short) MapleJob.凱內西斯2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.凱內西斯3轉.getId()) {
                changeJob((short) MapleJob.凱內西斯3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.凱內西斯4轉.getId()) {
                changeJob((short) MapleJob.凱內西斯4轉.getId());
            }
        } else if (MapleJob.is伊利恩(job)) {
            if (getLevel() >= 10 && job < MapleJob.伊利恩1轉.getId()) {
                changeJob((short) MapleJob.伊利恩1轉.getId());
            }
            if (getLevel() >= 30 && job < MapleJob.伊利恩2轉.getId()) {
                changeJob((short) MapleJob.伊利恩2轉.getId());
            }
            if (getLevel() >= 60 && job < MapleJob.伊利恩3轉.getId()) {
                changeJob((short) MapleJob.伊利恩3轉.getId());
            }
            if (getLevel() >= 100 && job < MapleJob.伊利恩4轉.getId()) {
                changeJob((short) MapleJob.伊利恩4轉.getId());
            }
        }
        if (getLevel() >= 200 && !MapleJob.is皮卡啾(job)) {
            // 5轉：愛爾達的呼喚
            if (getQuestStatus(1460) != 2) {
                forceCompleteQuest(1460);
            }
            // 5轉：祝福女神的庇佑與你同在
            if (getQuestStatus(1461) != 2) {
                forceCompleteQuest(1461);
            }
            // 楓之谷世界的神秘石
            if (getQuestStatus(1462) != 2) {
                forceCompleteQuest(1462);
            }
            // 格蘭蒂斯的神秘石
            if (getQuestStatus(1463) != 2) {
                forceCompleteQuest(1463);
            }
            // 克拉奇亞的神秘石
            if (getQuestStatus(1464) != 2) {
                forceCompleteQuest(1464);
            }
            // 5轉：紀錄力量，完成覺醒
            if (getQuestStatus(1465) != 2) {
                forceCompleteQuest(1465);
                getClient().getSession().writeAndFlush(CField.UIPacket.openUI(0x46B));
            }
        }
    }

    public void changeKeybinding(int key, byte type, int action) {
        if (type != 0) {
            keylayout.Layout().put(key, new Pair<>(type, action));
        } else {
            keylayout.Layout().remove(key);
        }
    }

    public void changeKeybinding(String key_name, byte type, int action) {
        int key;
        switch (key_name.toUpperCase()) {
            case "F1":
            case "F2":
            case "F3":
            case "F4":
            case "F5":
            case "F6":
            case "F7":
            case "F8":
            case "F9":
            case "F10":
            case "F11":
            case "F12":
                key = 58 + Integer.parseInt(key_name.replace("F", ""));
                break;
            case "1":
            case "!":
            case "2":
            case "@":
            case "3":
            case "#":
            case "4":
            case "$":
            case "5":
            case "%":
            case "6":
            case "^":
            case "7":
            case "&":
            case "8":
            case "*":
            case "9":
            case "(":
                key = 1 + Integer.parseInt(key_name);
                break;
            case "0":
            case ")":
                key = 11;
                break;
            case "-":
            case "_":
                key = 12;
                break;
            case "=":
            case "+":
                key = 13;
                break;
            default:
                key = -1;
                break;
        }
        if (key != -1) {
            if (type != 0) {
                keylayout.Layout().put(key, new Pair<>(type, action));
            } else {
                keylayout.Layout().remove(key);
            }
        }
    }

    public void sendMacros() {
        client.announce(CWvsContext.getMacros(skillMacros));
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
        changed_skillmacros = true;
    }

    public final SkillMacro[] getMacros() {
        return skillMacros;
    }

    public int getMaxHp() {
        return getStat().getMaxHp();
    }

    public int getMaxMp() {
        return getStat().getMaxMp();
    }

    public void setHp(int amount) {
        getStat().setHp(amount, this);
    }

    public void setMp(int amount) {
        getStat().setMp(amount, this);
    }

    public void logProcess(String path) {
        if (path == null) {
            return;
        }
        StringBuilder ret = new StringBuilder();
        for (MapleProcess mp : Process) {
            ret.append("\r\n---------------------------------------------------------------------\r\n");
            ret.append("進程路徑：").append(mp.getPath()).append("\r\nMD5值：").append(mp.getMD5()).append("\r\n");
        }
        ret.append("\r\n");
        FileoutputUtil.log(path, ret.toString());
    }

    public boolean ban(String reason, boolean autoban, boolean mac) {
        return ban(reason, null, MapleClient.BanReason.外掛, autoban, false, mac, false);
    }

    public boolean ban(String reason, boolean permBan) {
        return ban(reason, null, MapleClient.BanReason.外掛, false, false, false, false);
    }

    public static boolean ban(String id, String reason, boolean isAccount) {
        return MapleClient.ban(id, reason, null, MapleClient.BanReason.外掛, false, isAccount, false, false, false);
    }

    public final boolean ban(String reason, Calendar tempBan, MapleClient.BanReason greason, boolean autoban,
            boolean ipBan, boolean macBan, boolean hellban) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }
        client.announce(CWvsContext.GMPoliceMessage(true));
        boolean result = MapleClient.ban(getName(), reason, tempBan, greason, autoban, false, ipBan, macBan, hellban);
        client.getSession().close();
        System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
        logProcess(FileoutputUtil.BanLog_Process_Dir + getName() + "_" + getId() + ".txt");
        return result;
    }

    /**
     * Oid of players is always = the cid
     *
     * @return
     */
    @Override
    public int getObjectId() {
        return getId();
    }

    /**
     * Throws unsupported operation exception, oid of players is read only
     *
     * @param id
     */
    @Override
    public void setObjectId(int id) {
        throw new UnsupportedOperationException();
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public void addVisibleMapObject(MapleMapObject mo) {
        if (isClone()) {
            return;
        }
        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.add(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public void removeVisibleMapObject(MapleMapObject mo) {
        if (isClone()) {
            return;
        }
        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.remove(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        visibleMapObjectsLock.readLock().lock();
        try {
            return !isClone() && visibleMapObjects.contains(mo);
        } finally {
            visibleMapObjectsLock.readLock().unlock();
        }
    }

    public Collection<MapleMapObject> getAndWriteLockVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().lock();
        return visibleMapObjects;
    }

    public void unlockWriteVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().unlock();
    }

    public boolean isAlive() {
        return stats.getHp() > 0;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(CField.removePlayerFromMap(this.getObjectId()));
        for (final WeakReference<MapleCharacter> chr : clones) {
            if (chr.get() != null) {
                chr.get().sendDestroyData(client);
            }
        }
        // don't need this, client takes care of it
        /*
         * if (dragon != null) { client.announce(CField.removeDragon(this.getId())); }
         * if (android != null) {
         * client.announce(CField.deactivateAndroid(this.getId())); } if
         * (summonedFamiliar != null) {
         * client.announce(CField.removeFamiliar(this.getId())); }
         */
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (client.getPlayer().allowedToTarget(this)) {
            // if (client.getPlayer() != this)
            client.announce(CField.spawnPlayerMapobject(this));
            client.announce(CField.getEffectSwitch(getId(), getEffectSwitch()));
            if (getParty() != null && !isClone()) {
                updatePartyMemberHP();
                receivePartyMemberHP();
            }

            for (final WeakReference<MapleCharacter> chr : clones) {
                if (chr.get() != null) {
                    chr.get().sendSpawnData(client);
                }
            }
            if (dragon != null) {
                client.announce(CField.spawnDragon(dragon));
            }
            if (haku != null) {
                client.announce(CField.spawnHaku(haku));
            }
            if (android != null) {
                client.announce(CField.spawnAndroid(this, android));
            }
            if (summonedFamiliar != null) {
                client.announce(FamiliarPacket.spawnFamiliar(summonedFamiliar, true, true));
            }
            if (summons != null && summons.size() > 0) {
                summonsLock.readLock().lock();
                try {
                    for (final MapleSummon summon : summons) {
                        client.announce(SummonPacket.spawnSummon(summon, false));
                    }
                } finally {
                    summonsLock.readLock().unlock();
                }
            }
            if (followid > 0 && followon) {
                client.announce(
                        CField.followEffect(followinitiator ? followid : id, followinitiator ? id : followid, null));
            }
        }
    }

    public final void equipChanged() {
        if (map == null) {
            return;
        }
        map.broadcastMessage(this, CField.updateCharLook(this, false), false);
        stats.recalcLocalStats(this);
        if (getMessenger() != null) {
            World.Messenger.updateMessenger(getMessenger().getId(), getName(), client.getChannel());
        }
    }

    @Override
    public int getPet(int i) {
        MaplePet pet = getSummonedPet(i);
        return pet == null ? 0 : pet.getPetItemId();
    }

    public final List<MaplePet> getPets() {
        return pets;
    }

    public void addPet(final MaplePet pet) {
        if (pets.contains(pet)) {
            pets.remove(pet);
        }
        pets.add(pet);
    }

    public void removePet(MaplePet pet) {
        pet.setSummoned(0);
        pets.remove(pet);
    }

    public final List<MaplePet> getSummonedPets() {
        List<MaplePet> ret = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ret.add(null);
        }
        if (pets != null) {
            for (final MaplePet pet : pets) {
                if (pet != null && pet.getSummoned()) {
                    int index = pet.getSummonedValue() - 1;
                    ret.remove(index);
                    ret.add(index, pet);
                }
            }
        }
        List<Integer> nullArr = new ArrayList<>();
        nullArr.add(null);
        ret.removeAll(nullArr);
        return ret;
    }

    public final MaplePet getSummonedPet(final int index) {
        for (final MaplePet pet : getSummonedPets()) {
            if (pet.getSummonedValue() - 1 == index) {
                return pet;
            }
        }
        return null;
    }

    public final int getPetSlotNext() {
        List<MaplePet> petsz = getSummonedPets();
        int index = 0;
        if (petsz.size() >= 3) {
            unequipPet(getSummonedPet(0), false);
        } else {
            boolean[] indexBool = new boolean[]{false, false, false};
            for (int i = 0; i < 3; i++) {
                for (MaplePet p : petsz) {
                    if (p.getSummonedValue() == i + 1) {
                        indexBool[i] = true;
                    }
                }
            }
            for (boolean b : indexBool) {
                if (!b) {
                    break;
                }
                index++;
            }
            index = Math.min(index, 2);
            for (MaplePet p : petsz) {
                if (p.getSummonedValue() == index + 1) {
                    unequipPet(p, false);
                }
            }
        }
        return index;
    }

    public final byte getPetIndex(final MaplePet petz) {
        return (byte) Math.max(-1, petz.getSummonedValue() - 1);
    }

    public final byte getPetIndex(final int petId) {
        for (final MaplePet pet : getSummonedPets()) {
            if (pet.getUniqueId() == petId) {
                return (byte) Math.max(-1, pet.getSummonedValue() - 1);
            }
        }
        return -1;
    }

    public final byte getPetIndexById(final int petId) {
        for (final MaplePet pet : getSummonedPets()) {
            if (pet.getPetItemId() == petId) {
                return (byte) Math.max(-1, pet.getSummonedValue() - 1);
            }
        }
        return -1;
    }

    public final void unequipAllPets() {
        for (final MaplePet pet : getSummonedPets()) {
            unequipPet(pet, false);
        }
    }

    public void unequipPet(MaplePet pet, boolean hunger) {
        if (pet.getSummoned()) {
            pet.saveToDb();

            int index = pet.getSummonedValue() - 1;
            pet.setSummoned(0);
            client.announce(PetPacket
                    .updatePet(getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
            if (map != null) {
                // map.broadcastMessage(this, PetPacket.showPet(this, pet, true, hunger),
                // false);
                map.broadcastMessage(this, PetPacket.removePet(getId(), index), true);
            }
            // List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat,
            // Integer>>();
            // stats.put(MapleStat.PET, Integer.valueOf(0)));
            // showpetupdate isn't done here...
            // client.announce(PetPacket.petStatUpdate(this));
            client.announce(CWvsContext.enableActions());
        }
    }

    /*
     * public void shiftPetsRight() { if (pets[2] == null) { pets[2] = pets[1];
     * pets[1] = pets[0]; pets[0] = null; } }
     */
    public final long getLastFameTime() {
        return lastfametime;
    }

    public final List<Integer> getFamedCharacters() {
        return lastmonthfameids;
    }

    public final List<Integer> getBattledCharacters() {
        return lastmonthbattleids;
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        if (lastfametime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
            return FameStatus.NOT_TODAY;
        } else if (from == null || lastmonthfameids == null || lastmonthfameids.contains(from.getId())) {
            return FameStatus.NOT_THIS_MONTH;
        }
        return FameStatus.OK;
    }

    public void hasGivenFame(MapleCharacter to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(to.getId());
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con
                    .prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)")) {
                ps.setInt(1, getId());
                ps.setInt(2, to.getId());
                ps.execute();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("ERROR writing famelog for char " + getName() + " to " + to.getName() + e);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
        }
    }

    public boolean canBattle(MapleCharacter to) {
        return to != null && lastmonthbattleids != null && !lastmonthbattleids.contains(to.getAccountID());
    }

    public void hasBattled(MapleCharacter to) {
        lastmonthbattleids.add(to.getAccountID());
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO battlelog (accid, accid_to) VALUES (?, ?)")) {
                ps.setInt(1, getAccountID());
                ps.setInt(2, to.getAccountID());
                ps.execute();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("ERROR writing battlelog for char " + getName() + " to " + to.getName() + e);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
        }
    }

    public final MapleKeyLayout getKeyLayout() {
        return this.keylayout;
    }

    public MapleParty getParty() {
        if (party == null) {
            return null;
        } else if (party.isDisbanded()) {
            party = null;
        }
        return party;
    }

    public byte getWorld() {
        return world;
    }

    public void setWorld(byte world) {
        this.world = world;
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void setEventInstanceAzwan(EventManager eventInstance) {
        this.eventInstanceAzwan = eventInstance;
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void clearDoors() {
        doors.clear();
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<>(doors);
    }

    public void addMechDoor(MapleOpenGate door) {
        mechDoors.add(door);
    }

    public void clearMechDoors() {
        mechDoors.clear();
    }

    public List<MapleOpenGate> getMechDoors() {
        return new ArrayList<>(mechDoors);
    }

    public void setSmega() {
        if (smega) {
            smega = false;
            dropMessage(5, "You have set megaphone to disabled mode");
        } else {
            smega = true;
            dropMessage(5, "You have set megaphone to enabled mode");
        }
    }

    public boolean getSmega() {
        return smega;
    }

    public List<MapleSummon> getSummonsReadLock() {
        summonsLock.readLock().lock();
        return summons;
    }

    public int getSummonsSize() {
        return summons.size();
    }

    public void unlockSummonsReadLock() {
        summonsLock.readLock().unlock();
    }

    public void addSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.add(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public void removeSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.remove(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public int getChair() {
        return chair;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public int getTitleEffect() {
        String info = getInfoQuest(GameConstants.稱號);
        return info == null || info.isEmpty() ? 0 : Integer.parseInt(info);
    }

    public int getDamageSkin() {
        String info = getInfoQuest(GameConstants.傷害字型);
        return info == null || info.isEmpty() ? 0 : Integer.parseInt(info);
    }

    public void setDamageSkin(int damageSkin) {
        this.updateInfoQuest(GameConstants.傷害字型, String.valueOf(damageSkin));
    }

    public List<Integer> getSaveDamageSkin() {
        List<Integer> posList = new ArrayList<>();
        if (!questinfo.containsKey(GameConstants.傷害字型儲存)) {
            return posList;
        }
        for (int i = 0; i < 29; i++) {
            String damage = getOneInfo(GameConstants.傷害字型儲存, String.valueOf(i));
            if (StringTool.parseInt(damage) > 0) {
                posList.add(StringTool.parseInt(damage));
            }
        }
        return posList;
    }

    public void updateSaveDamageSkin(int damageSkin) {
        List<Integer> posList = getSaveDamageSkin();
        if (posList.size() > 28) {
            return;
        }
        if (posList.contains(damageSkin)) {
            posList.remove(damageSkin);
        } else {
            posList.add(damageSkin);
        }
        String info = "";
        int size = posList.size();
        for (int i = 0; i < size; i++) {
            info += i + "=" + posList.get(i) + ";";
        }
        info = info.isEmpty() ? info : info.substring(0, info.length() - 1);
        updateInfoQuest(GameConstants.傷害字型儲存, info);
        if (info.isEmpty()) {
            clearInfoQuest(GameConstants.傷害字型儲存);
        }
    }

    public void setChair(int chair) {
        this.chair = chair;
        stats.relocHeal(this);
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    public void setTitleEffect(int titleEffect) {
        MapleQuestStatus queststatus;
        if (titleEffect == 0) {
            queststatus = getQuestRemove(MapleQuest.getInstance(GameConstants.稱號));
        } else {
            queststatus = getQuestNAdd(MapleQuest.getInstance(GameConstants.稱號));
            queststatus.setCustomData(String.valueOf(titleEffect));
        }
        if (queststatus != null) {
            updateQuest(queststatus);
        }
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public int getGuildId() {
        return guildid;
    }

    public byte getGuildRank() {
        return guildrank;
    }

    public int getGuildContribution() {
        return guildContribution;
    }

    public void setGuildId(int _id) {
        guildid = _id;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
            guildContribution = 0;
        }
    }

    public void setGuildRank(byte _rank) {
        guildrank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public void setGuildContribution(int _c) {
        this.guildContribution = _c;
        if (mgc != null) {
            mgc.setGuildContribution(_c);
        }
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public void setAllianceRank(byte rank) {
        allianceRank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public byte getAllianceRank() {
        return allianceRank;
    }

    public MapleGuild getGuild() {
        if (getGuildId() <= 0) {
            return null;
        }
        return World.Guild.getGuild(getGuildId());
    }

    public void setJob(int j) {
        this.job = (short) j;
    }

    public void guildUpdate() {
        if (guildid <= 0) {
            return;
        }
        mgc.setLevel(level);
        mgc.setJobCode(job);
        World.Guild.memberLevelJobUpdate(mgc);
    }

    public void saveGuildStatus() {
        MapleGuild.setOfflineGuildStatus(guildid, guildrank, guildContribution, allianceRank, id);
    }

    public void modifyCSPoints(int type, int quantity) {
        modifyCSPoints(type, quantity, false);
    }

    public void modifyCSPoints(int type, int quantity, boolean show) {
        switch (type) {
            case 1:
                if (beanfunGash + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "樂豆點已達到上限.");
                    }
                    return;
                }
                beanfunGash += quantity;
                break;
            case 2:
                if (maplepoints + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "楓點已達到上限.");
                    }
                    return;
                }
                maplepoints += quantity;
                client.announce(CWvsContext.updateMaplePoint(maplepoints));
                break;
            case 3:
                if (mileage + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "里程已達到上限.");
                    }
                    return;
                }
                mileage += quantity;
                break;
            default:
                break;
        }
        if (show && quantity != 0) {
            dropMessage(-1, (quantity > 0 ? "獲得 " : "失去 ") + Math.abs(quantity)
                    + (type == 1 ? " 樂豆點" : type == 2 ? " 楓點" : type == 3 ? " 里程" : "未知點數") + "。");
            client.announce(EffectPacket.showDodgeChanceEffect());
        }
    }

    public int getCSPoints(int type) {
        switch (type) {
            case 1:
                return beanfunGash;
            case 2:
                return maplepoints;
            case 3:
                return mileage;
            default:
                return 0;
        }
    }

    public final boolean hasEquipped(int itemid) {
        return inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid) >= 1;
    }

    public final boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        int possesed = inventory[type.ordinal()].countById(itemid);
        if (checkEquipped && type == MapleInventoryType.EQUIP) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        if (greaterOrEquals) {
            return possesed >= quantity;
        } else {
            return possesed == quantity;
        }
    }

    public final boolean haveItem(int itemid, int quantity) {
        return haveItem(itemid, quantity, true, true);
    }

    public final boolean haveItem(int itemid) {
        return haveItem(itemid, 1, true, true);
    }

    public List<Item> getRebuy() {
        return this.rebuy;
    }

    public void setRebuy(List<Item> rebuy) {
        this.rebuy = rebuy;

    }

    public void reloadAllFamiliars() {
        this.client.announce(FamiliarPacket.showAllFamiliar(this));
    }

    public List<MapleBuffStatValueHolder> getAllBuffs() {
        return new LinkedList<MapleBuffStatValueHolder>(this.effects.values());

    }

    public void updateVSkillRecords() {
        Map<Skill, SkillEntry> toRemove = new HashMap<>();
        Map<Skill, SkillEntry> toUpdate = new HashMap<>();
        for (Entry<Skill, SkillEntry> entry : this.skills.entrySet()) {
            if (entry.getKey().isVSkill()) {
                toRemove.put(entry.getKey(), entry.getValue());
            }
        }
        toRemove.keySet().forEach(skill -> skills.remove(skill));
        if (!toRemove.isEmpty()) {
            client.announce(CWvsContext.updateVSkills(toRemove, true));
        }
        for (VMatrixRecord vmx : getVMatrixRecords()) {
            final Skill skill1 = SkillFactory.getSkill(vmx.getSkillID1());
            final Skill skill2 = SkillFactory.getSkill(vmx.getSkillID2());
            final Skill skill3 = SkillFactory.getSkill(vmx.getSkillID2());
            final long expireTime = vmx.getExpirationDate();
            if (vmx.getState() == VCoreRecordState.ACTIVE) {
                if (skill1 != null) {
                    toUpdate.put(skill1, new SkillEntry(vmx.getSkillLv(), vmx.getMasterLv(), expireTime));
                }
                if (skill2 != null) {
                    toUpdate.put(skill1, new SkillEntry(vmx.getSkillLv(), vmx.getMasterLv(), expireTime));
                }
                if (skill3 != null) {
                    toUpdate.put(skill1, new SkillEntry(vmx.getSkillLv(), vmx.getMasterLv(), expireTime));
                }
                for (Entry<Skill, SkillEntry> newSkill : toUpdate.entrySet()) {
                    this.changeSkillData(newSkill.getKey(), newSkill.getValue().skillLevel,
                            newSkill.getValue().masterlevel, newSkill.getValue().expiration);
                }
            }
        }
        client.announce(CWvsContext.updateVSkills(toUpdate, false));
        reUpdateStat(false, true);
    }

    public static enum FameStatus {

        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public byte getBuddyCapacity() {
        return buddylist.getCapacity();
    }

    public void setBuddyCapacity(byte capacity) {
        buddylist.setCapacity(capacity);
        client.announce(BuddyListPacket.updateBuddyCapacity(capacity));
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public void addCooldown(int skillId, long startTime, long length) {
        if (skillId == 131001104) {
            length /= 1000;
        }
        if (isAdmin() && isInvincible()) {
            showMessage(10, "伺服器管理員無敵狀態消除技能冷卻" + skillId + "，持續時間：" + length / 1000.0 + "秒");
            client.announce(CField.skillCooldown(skillId, 0));
        } else {
            if (isShowInfo()) {
                showMessage(10, "技能" + skillId + "進入冷卻，持續時間：" + length / 1000.0 + "秒");
            }
            giveCoolDowns(skillId, startTime, length);
            if (length / 1000 > 0) {
                client.announce(CField.skillCooldown(skillId, (int) Math.max(length / 1000, 0)));
            }
        }
    }

    public void removeCooldown(int skillId) {
        String info = "移除技能冷卻[" + skillId + "]";
        if (coolDowns.containsKey(skillId)) {
            info += " 持續時間：" + coolDowns.get(skillId).length / 1000.0 + "秒";
            coolDowns.remove(skillId);
        }
        if (isShowInfo()) {
            showMessage(10, info);
        }
        client.announce(CField.skillCooldown(skillId, 0));
    }

    public boolean skillisCooling(int skillId) {
        return coolDowns.containsKey(skillId);
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        if (!isAdmin() || !isInvincible()) {
            coolDowns.put(skillid, new MapleCoolDownValueHolder(skillid, starttime, length));
        }
    }

    public void giveCoolDowns(final List<MapleCoolDownValueHolder> cooldowns) {
        int time;
        if (cooldowns != null) {
            for (MapleCoolDownValueHolder cooldown : cooldowns) {
                coolDowns.put(cooldown.skillId, cooldown);
            }
        } else {
            try {
                Connection con = ManagerDatabasePool.getConnection();
                ResultSet rs;
                try (PreparedStatement ps = con
                        .prepareStatement("SELECT SkillID,StartTime,length FROM skills_cooldowns WHERE charid = ?")) {
                    ps.setInt(1, getId());
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        if (rs.getLong("length") + rs.getLong("StartTime") - System.currentTimeMillis() <= 0) {
                            continue;
                        }
                        giveCoolDowns(rs.getInt("SkillID"), rs.getLong("StartTime"), rs.getLong("length"));
                    }
                }
                rs.close();
                deleteWhereCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");
                ManagerDatabasePool.closeConnection(con);
            } catch (SQLException e) {
                System.err.println("Error while retriving cooldown from SQL storage");
                FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
            }
        }
    }

    public int getCooldownSize() {
        return coolDowns.size();
    }

    public int getDiseaseSize() {
        return diseases.size();
    }

    public List<MapleCoolDownValueHolder> getCooldowns() {
        List<MapleCoolDownValueHolder> ret = new ArrayList<>();
        for (MapleCoolDownValueHolder mc : coolDowns.values()) {
            if (mc != null) {
                ret.add(mc);
            }
        }
        return ret;
    }

    public final List<MapleDiseaseValueHolder> getAllDiseases() {
        return new ArrayList<>(diseases.values());
    }

    public final boolean hasDisease(final MapleDisease dis) {
        return diseases.containsKey(dis);
    }

    public void giveDebuff(MapleDisease disease, MobSkill skill) {
        giveDebuff(disease, skill.getX(), skill.getDuration(), skill.getSkillId(), skill.getSkillLevel());
    }

    public void giveDebuff(MapleDisease disease, int x, long duration, int skillid, int level) {
        if (isInvincible()) {
            if (isShowInfo()) {
                showInfo("異常狀態", false, "無敵狀態消除異常狀態 - " + disease.name());
            }
            return;
        }
        if (this.map != null && !hasDisease(disease)) {
            int mC = getBuffSource(MapleBuffStat.Mechanic);
            if ((mC > 0) && (mC != 35121005)) {
                return;
            }

            MapleStatEffect effect = getStatForBuff(MapleBuffStat.AntiMagicShell);
            if (effect != null) {
                // 元素適應(雷、冰)
                // if (effect.getSourceId() == 2211012 && getBuffStatValueHolder(2211012,
                // MapleBuffStat.AntiMagicShell).localDuration != -1) {
                // return;
                // }
                switch (disease) {
                    case 誘惑:
                    case 昏迷:
                        int cd = effect.getCooldown(this);
                        cancelEffectFromBuffStat(MapleBuffStat.AntiMagicShell);
                        if (getBuffedValue(MapleBuffStat.AntiMagicShell) == null && cd > 0) {
                            addCooldown(effect.getSourceId(), System.currentTimeMillis(), cd * 1000);
                        }
                        return;
                }
            }
            if (Randomizer.isSuccess(this.stats.異常抗性)) {
                return;
            }
            long tCur = System.currentTimeMillis();
            int rOption = skillid + (level << 16);
            diseases.put(disease, new MapleDiseaseValueHolder(disease, tCur, duration - this.stats.decreaseDebuff));
            getTempstats().updateStatOption(disease.getStat(), this, effect, rOption, x, tCur,
                    (int) (duration - this.stats.decreaseDebuff));
            client.announce(CWvsContext.BuffPacket.giveDebuff(disease, x, skillid, level, (int) duration));
            map.broadcastMessage(this, CWvsContext.BuffPacket.giveForeignDebuff(this.id, disease, skillid, level, x),
                    false);

            if ((x > 0) && (disease == MapleDisease.中毒)) {
                addHP((int) (-(x * ((duration - this.stats.decreaseDebuff) / 1000L))));
            }
        }
    }

    public void dispelDebuff(MapleDisease debuff) {
        if (hasDisease(debuff)) {
            client.announce(BuffPacket.cancelDebuff(debuff));
            map.broadcastMessage(this, BuffPacket.cancelForeignDebuff(id, debuff), false);

            diseases.remove(debuff);
        }
    }

    public void dispelDebuffs() {
        List<MapleDisease> diseasess = new ArrayList<>(diseases.keySet());
        for (MapleDisease d : diseasess) {
            dispelDebuff(d);
        }
    }

    public void cancelAllDebuffs() {
        diseases.clear();
    }

    public void setLevel(final short level) {
        this.level = level;
    }

    public void sendNote(String to, String msg) {
        sendNote(to, msg, 0);
    }

    public void sendNote(String to, String msg, int fame) {
        MapleCharacterUtil.sendNote(to, getName(), msg, fame);
    }

    public void sendMapleGMNote(String to, String msg, int fame) {
        MapleCharacterUtil.sendNote(to, "楓之谷GM", msg, fame);
    }

    public void showNote() {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM notes WHERE `to`=?",
                    ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                ps.setString(1, getName());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.last();
                    int count = rs.getRow();
                    rs.first();
                    client.announce(CCashShop.showNotes(rs, count));
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("Unable to show note" + e);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
        }
    }

    public void deleteNote(int id, int fame) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT gift FROM notes WHERE `id`=?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getInt("gift") == fame && fame > 0) { // not exploited! hurray
                        addFame(fame);
                        updateSingleStat(MapleStat.FAME, getFame());
                        client.announce(InfoPacket.getShowFameGain(fame));
                    }
                }
                rs.close();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?")) {
                ps.setInt(1, id);
                ps.execute();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("Unable to delete note" + e);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
        }
    }

    public int getMulungEnergy() {
        return mulung_energy;
    }

    public void modifyMulungEnergy(boolean inc) {
        if (inc) {
            if (mulung_energy + 100 > 10000) {
                mulung_energy = 10000;
            } else {
                mulung_energy += 100;
            }
        } else {
            mulung_energy = 0;
        }
        client.announce(CWvsContext.MulungEnergy(mulung_energy));
    }

    public void writeMulungEnergy() {
        client.announce(CWvsContext.MulungEnergy(mulung_energy));
    }

    public void writeEnergy(String type, String inc) {
        client.announce(CWvsContext.sendPyramidEnergy(type, inc));
    }

    public void writeStatus(String type, String inc) {
        client.announce(CWvsContext.sendGhostStatus(type, inc));
    }

    public void writePoint(String type, String inc) {
        client.announce(CWvsContext.sendGhostPoint(type, inc));
    }

    public final short getShowCombo() {
        return showCombo;
    }

    public void setShowCombo(final short combo) {
        showCombo = combo;
    }

    public final short getCombo() {
        long num = 0;
        if (lastCombo > 0) {
            num = ((System.currentTimeMillis() - lastCombo) / 3000L);
            // Math.ceil
        }
        short x = combo;
        if (combo > 0 && num > 0) {
            for (long i = 0; i < num; i++) {
                x -= Math.ceil(x * 0.01D);
            }
        }
        return (short) Math.max(0, x);
    }

    public void setCombo(final short combo) {
        this.combo = combo;
        lastCombo = System.currentTimeMillis();
        if (combo % 10 == 0 && combo >= 10 && combo <= 100) {
            if (getTotalSkillLevel(21000000) < combo / 10) {
                return;
            }
            SkillFactory.getSkill(21000000).getEffect(combo / 10).applyComboBuff(this, combo);
        } else if (combo < 10) {
            dispelBuff(21000000);
        }
    }

    public final long getLastCombo() {
        return lastCombo;
    }

    public void setLastCombo(final long combo) {
        this.lastCombo = combo;
    }

    public void checkBerserk() { // berserk is special in that it doesn't use worldtimer :)
        if (job != 132 || lastBerserkTime < 0 || lastBerserkTime + 10000 > System.currentTimeMillis()) {
            return;
        }
        int skillId = 1320016;
        final Skill BerserkX = SkillFactory.getSkill(skillId);
        final int skilllevel = getTotalSkillLevel(BerserkX);
        if (skilllevel >= 1 && map != null) {
            lastBerserkTime = System.currentTimeMillis();
            final MapleStatEffect ampStat = BerserkX.getEffect(skilllevel);
            stats.Berserk = stats.getHp() * 100 / stats.getCurrentMaxHp() >= ampStat.getX();
            client.announce(EffectPacket.showBuffEffect(true, this, skillId, UserEffectOpcode.UserEffect_SkillUse,
                    getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)));
            map.broadcastMessage(this, EffectPacket.showBuffEffect(false, this, skillId,
                    UserEffectOpcode.UserEffect_SkillUse, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)),
                    false);
        } else {
            lastBerserkTime = -1; // somebody thre? O_O
        }
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
        if (map != null) {
            map.broadcastMessage(CCashShop.useChalkboard(getId(), text));
        }
    }

    public String getChalkboard() {
        return chalktext;
    }

    public MapleMount getMount() {
        return mount;
    }

    public int[] getWishlist() {
        return wishlist;
    }

    public void clearWishlist() {
        for (int i = 0; i < 12; i++) {
            wishlist[i] = 0;
        }
        changed_wishlist = true;
    }

    public int getWishlistSize() {
        int ret = 0;
        for (int i = 0; i < 12; i++) {
            if (wishlist[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public void setWishlist(int[] wl) {
        this.wishlist = wl;
        changed_wishlist = true;
    }

    public int[] getRocks() {
        return rocks;
    }

    public int getRockSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (rocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRocks(int map) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == map) {
                rocks[i] = 999999999;
                changed_trocklocations = true;
                break;
            }
        }
    }

    public void addRockMap() {
        if (getRockSize() >= 10) {
            return;
        }
        rocks[getRockSize()] = getMapId();
        changed_trocklocations = true;
    }

    public boolean isRockMap(int id) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getRegRocks() {
        return regrocks;
    }

    public int getRegRockSize() {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRegRocks(int map) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == map) {
                regrocks[i] = 999999999;
                changed_regrocklocations = true;
                break;
            }
        }
    }

    public void addRegRockMap() {
        if (getRegRockSize() >= 5) {
            return;
        }
        regrocks[getRegRockSize()] = getMapId();
        changed_regrocklocations = true;
    }

    public boolean isRegRockMap(int id) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getHyperRocks() {
        return hyperrocks;
    }

    public int getHyperRockSize() {
        int ret = 0;
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromHyperRocks(int map) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == map) {
                hyperrocks[i] = 999999999;
                changed_hyperrocklocations = true;
                break;
            }
        }
    }

    public void addHyperRockMap() {
        if (getRegRockSize() >= 13) {
            return;
        }
        hyperrocks[getHyperRockSize()] = getMapId();
        changed_hyperrocklocations = true;
    }

    public boolean isHyperRockMap(int id) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public List<LifeMovementFragment> getLastRes() {
        return lastres;
    }

    public void setLastRes(List<LifeMovementFragment> lastres) {
        this.lastres = lastres;
    }

    public void dropMessage(int type, String message) {
        switch (type) {
            case -1:
                client.announce(CWvsContext.getTopMsg(message));
                break;
            case -2:
                client.announce(PlayerShopPacket.shopChat(message, 0)); // 0 or what
                break;
            case -3:
                client.announce(CField.getChatText(getId(), message, isSuperGM(), 0)); // 1 = hide
                break;
            case -4:
                client.announce(CField.getChatText(getId(), message, isSuperGM(), 1)); // 1 = hide
                break;
            case -5:
                client.announce(CField.getGameMessage(6, message)); // 灰色
                break;
            case -6:
                client.announce(CField.getGameMessage(10, message)); // 白色背景
                break;
            case -7:
                client.announce(CWvsContext.getMidMsg(message, false, 0));
                break;
            case -8:
                client.announce(CWvsContext.getMidMsg(message, true, 0));
                break;
            case -9:
                client.announce(CWvsContext.InfoPacket.showInfo(message));
                break;
            case -10:
                client.announce(CField.getFollowMessage(message));
                break;
            case -11:
                client.announce(CWvsContext.yellowChat(message));
                break;
            default:
                client.announce(CWvsContext.broadcastMsg(type, message));
        }
    }

    // 0 - 白(普聊)
    // 1 - 綠(悄悄話)
    // 2 - 粉紅(隊伍聊天)
    // 3 - 橙(好友聊天)
    // 4 - 淺紫(公會聊天)
    // 5 - 淺綠(聯盟聊天)
    // 6 - 灰(訊息)
    // 7 - 亮黃
    // 8 - 淡黃
    // 9 - 藍
    // 10 - 黑字白底
    // 11 - 紅
    // 12 - 藍字藍底
    // 13 - 紅字粉紅底(喇叭)
    // 14 - 紅字粉紅底(喇叭_有:時無字)
    // 15 - 黑字黃底(喇叭)
    // 16 - 紫
    // 17 - 黑字綠底(有:時:前面的字變成[W:-1])
    // 18 - 灰(喇叭)
    // 19 - 黃
    // 20 - 青
    // 21 - 黑字黃底
    // 22 - 藍("[]"裡面字會變黃色而"[]"不顯示)
    // 23 - 淡黃
    // 24 - 藍("[]"裡面字會變黃色"[]"顯示)
    // 25 - 玫瑰紅(喇叭)
    // 26 - 淺紫("[]"裡面字會變黃色"[]"顯示)
    // 27 - 淺黃
    // 28 - 橘黃
    // 29 - 無
    // 30 - 草綠
    // 31 - 粉紅白底(喇叭)
    // 32 - 黑字紅底(喇叭)
    // 33 - 黑字綠底(喇叭)
    // 34 - 黃字紅底(喇叭)
    // 35 - 黑字粉紅底(喇叭)
    // 36 - 黑字淺黃底(喇叭)
    // 37 - 黃字紅底
    // 38 - 白字半透大紅底(喇叭)
    // 39 - 遊戲崩潰
    // 40 - 遊戲崩潰
    // 41 - 黑字綠底(喇叭背景)
    // 42 - 黑字紅底(喇叭背景)
    // 43 - 黑字淺黃底(喇叭背景)
    // 44 - 深藍
    public void showMessage(int type, String msg) {
        if (type >= 0 && type <= 44) {
            client.announce(CField.getGameMessage(type, msg));
        } else {
            client.announce(CWvsContext.broadcastMsg(5, msg));
        }
    }

    public void dropSpouseMessage(int type, String msg) {
        showMessage(type, msg);
    }

    public void showInfo(String caption, boolean pink, String msg) {
        short type = (short) (pink ? 11 : 6);
        if (caption != null && !caption.isEmpty()) {
            msg = "[" + caption + "] " + msg;
        }
        showMessage(type, msg);
        dropMessage(-1, msg);
    }

    public IMaplePlayerShop getPlayerShop() {
        return playerShop;
    }

    public void setPlayerShop(IMaplePlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public int getConversation() {
        return inst.get();
    }

    public void setConversation(int inst) {
        this.inst.set(inst);
    }

    public int getDirection() {
        return insd.get();
    }

    public void setDirection(int inst) {
        this.insd.set(inst);
    }

    public MapleCarnivalParty getCarnivalParty() {
        return carnivalParty;
    }

    public void setCarnivalParty(MapleCarnivalParty party) {
        carnivalParty = party;
    }

    public void addCP(int ammount) {
        totalCP += ammount;
        availableCP += ammount;
    }

    public void useCP(int ammount) {
        availableCP -= ammount;
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void resetCP() {
        totalCP = 0;
        availableCP = 0;
    }

    public void addCarnivalRequest(MapleCarnivalChallenge request) {
        pendingCarnivalRequests.add(request);
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return pendingCarnivalRequests.pollLast();
    }

    public void clearCarnivalRequests() {
        pendingCarnivalRequests = new LinkedList<>();
    }

    public void startMonsterCarnival(final int enemyavailable, final int enemytotal) {
        client.announce(MonsterCarnivalPacket.startMonsterCarnival(this, enemyavailable, enemytotal));
    }

    public void CPUpdate(final boolean party, final int available, final int total, final int team) {
        client.announce(MonsterCarnivalPacket.CPUpdate(party, available, total, team));
    }

    public void playerDiedCPQ(final String name, final int lostCP, final int team) {
        client.announce(MonsterCarnivalPacket.playerDiedMessage(name, lostCP, team));
    }

    public boolean getCanTalk() {
        return this.canTalk;
    }

    public void canTalk(boolean talk) {
        this.canTalk = talk;
    }

    public double getEXPMod() {
        return stats.expMod;
    }

    public double getDropMod() {
        return stats.dropMod;
    }

    public CashShop getCashInventory() {
        return cs;
    }

    public void removeItem(int id, int quantity) {
        MapleInventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, quantity, true, false);
        client.announce(InfoPacket.getShowItemGain(id, (short) -quantity, true));
    }

    public void removeAll(int itemId) {
        removeAll(itemId, true);
    }

    public void removeAll(int itemId, boolean show) {
        MapleInventoryType type = GameConstants.getInventoryType(itemId);
        int possessed = getInventory(type).countById(itemId);
        if (possessed > 0) {
            MapleInventoryManipulator.removeById(getClient(), type, itemId, possessed, true, false);
            if (show) {
                getClient().announce(InfoPacket.getShowItemGain(itemId, (short) -possessed, true));
            }
        }
    }

    public Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> getRings(boolean equip) {
        MapleInventory iv = getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        List<MapleRing> crings = new ArrayList<>(), frings = new ArrayList<>(), mrings = new ArrayList<>();
        MapleRing ring;
        for (Item ite : equipped) {
            Equip item = (Equip) ite;
            if (item.getRing() != null) {
                ring = item.getRing();
                ring.setEquipped(true);
                if (ItemConstants.類型.特效戒指(item.getItemId())) {
                    if (equip) {
                        if (ItemConstants.類型.戀人戒指(item.getItemId())) {
                            crings.add(ring);
                        } else if (ItemConstants.類型.友誼戒指(item.getItemId())) {
                            frings.add(ring);
                        } else if (ItemConstants.類型.結婚戒指(item.getItemId())) {
                            mrings.add(ring);
                        }
                    } else if (crings.isEmpty() && ItemConstants.類型.戀人戒指(item.getItemId())) {
                        crings.add(ring);
                    } else if (frings.isEmpty() && ItemConstants.類型.友誼戒指(item.getItemId())) {
                        frings.add(ring);
                    } else if (mrings.isEmpty() && ItemConstants.類型.結婚戒指(item.getItemId())) {
                        mrings.add(ring);
                    } // for 3rd person the actual slot doesnt matter, so we'll use this to have both
                    // shirt/ring same?
                    // however there seems to be something else behind this, will have to sniff
                    // someone with shirt and ring, or more conveniently 3-4 of those
                }
            }
        }
        if (equip) {
            iv = getInventory(MapleInventoryType.EQUIP);
            for (Item ite : iv.list()) {
                Equip item = (Equip) ite;
                if (item.getRing() != null && ItemConstants.類型.戀人戒指(item.getItemId())) {
                    ring = item.getRing();
                    ring.setEquipped(false);
                    if (ItemConstants.類型.友誼戒指(item.getItemId())) {
                        frings.add(ring);
                    } else if (ItemConstants.類型.戀人戒指(item.getItemId())) {
                        crings.add(ring);
                    } else if (ItemConstants.類型.結婚戒指(item.getItemId())) {
                        mrings.add(ring);
                    }
                }
            }
        }
        frings.sort(new MapleRing.RingComparator());
        crings.sort(new MapleRing.RingComparator());
        mrings.sort(new MapleRing.RingComparator());
        return new Triple<>(crings, frings, mrings);
    }

    public void startFairySchedule(boolean exp) {
        startFairySchedule(exp, false);
    }

    public void startFairySchedule(boolean exp, boolean equipped) {
        cancelFairySchedule(exp || stats.equippedFairy == 0);
        if (fairyExp <= 0) {
            fairyExp = (byte) stats.equippedFairy;
        }
        if (equipped && fairyExp < 30 && stats.equippedFairy > 0) {
            dropMessage(5, "您裝備了精靈墜飾在1小時後經驗獲取將增加到" + (fairyExp + stats.equippedFairy) + "%.");
        }
        lastFairyTime = System.currentTimeMillis();
    }

    public final boolean canFairy(long now) {
        return lastFairyTime > 0 && lastFairyTime + (60 * 60 * 1000) < now;
    }

    public final boolean canHP(long now) {
        if (lastHPTime + 5000 < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMP(long now) {
        if (lastMPTime + 5000 < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canHPRecover(long now) {
        if (stats.hpRecoverTime > 0 && lastHPTime + stats.hpRecoverTime < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMPRecover(long now) {
        if (stats.mpRecoverTime > 0 && lastMPTime + stats.mpRecoverTime < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public void spawnPet(byte slot) {
        spawnPet(slot, false, true);
    }

    public void spawnPet(byte slot, boolean lead) {
        spawnPet(slot, lead, true);
    }

    public void spawnPet(byte slot, boolean lead, boolean broadcast) {
        final Item item = getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null || item.getItemId() >= 5010000 || item.getItemId() < 5000000) {
            return;
        }
        switch (item.getItemId()) {
            case 5000047:
            case 5000028: {
                final MaplePet pet = MaplePet.createPet(item.getItemId() + 1);
                if (pet != null) {
                    MapleInventoryManipulator.addById(client, item.getItemId() + 1, (short) 1, item.getOwner(), pet, 45,
                            MapleInventoryManipulator.DAY,
                            "從" + item.toString() + "寵物進化, 時間:" + FileoutputUtil.CurrentReadable_Date());
                    MapleInventoryManipulator.removeFromSlot(client, MapleInventoryType.CASH, slot, (short) 1, false);
                }
                break;
            }
            default: {
                final MaplePet pet = item.getPet();
                if (pet != null
                        && (MapleItemInformationProvider.getInstance().getLimitedLife(item.getItemId()) == 0
                        || pet.getLimitedLife() > 0)
                        && (item.getExpiration() <= 0 || item.getExpiration() > System.currentTimeMillis())) {
                    if (pet.getSummoned()) { // Already summoned, let's keep it
                        unequipPet(pet, false);
                    } else {
                        final Point pos = getPosition();
                        pet.setPos(pos);
                        try {
                            pet.setFh(getMap().getFootholds().findBelow(pos, true).getId());
                        } catch (NullPointerException e) {
                            pet.setFh(0); // lol, it can be fixed by movement
                        }
                        pet.setStance(0);
                        pet.setSummoned(getPetSlotNext() + 1);
                        addPet(pet);
                        if (broadcast && getMap() != null) {
                            client.announce(PetPacket.updatePet(getInventory(MapleInventoryType.CASH)
                                    .getItem((short) (byte) pet.getInventoryPosition())));
                            getMap().broadcastMessage(this, PetPacket.showPet(this, pet, false, false), true);
                            client.announce(
                                    PetPacket.showPetUpdate(this, pet.getUniqueId(), (byte) (pet.getSummonedValue() - 1)));
                            client.announce(PetPacket.petStatUpdate(this));
                        }
                    }
                }
                break;
            }
        }
        client.announce(CWvsContext.enableActions());
    }

    public void cancelFairySchedule(boolean exp) {
        lastFairyTime = 0;
        if (exp) {
            this.fairyExp = 0;
        }
    }

    public void doFairy() {
        if (fairyExp < 30 && stats.equippedFairy > 0) {
            fairyExp += stats.equippedFairy;
            dropMessage(5, "精靈吊墜經驗獲取增加到 " + fairyExp + "%.");
        }
        if (getGuildId() > 0) {
            World.Guild.gainGP(getGuildId(), 20, id);
            client.announce(InfoPacket.getGPContribution(20));
        }
        traits.get(MapleTraitType.will).addExp(5, this); // willpower every hour
        startFairySchedule(false, true);
    }

    public byte getFairyExp() {
        return fairyExp;
    }

    public int getTeam() {
        return pvpTeam;
    }

    public void setTeam(int v) {
        this.pvpTeam = v;
    }

    public void clearLinkMid() {
        linkMobs.clear();
        cancelEffectFromBuffStat(MapleBuffStat.GuidedBullet);
        cancelEffectFromBuffStat(MapleBuffStat.ArcaneAim);
    }

    public int getFirstLinkMid() {
        for (Integer lm : linkMobs.keySet()) {
            return lm;
        }
        return 0;
    }

    public Map<Integer, Integer> getAllLinkMid() {
        return linkMobs;
    }

    public void setLinkMid(int lm, int x) {
        linkMobs.put(lm, x);
    }

    public List<MapleSummon> getAllLinksummon() {
        return this.linksummon;
    }

    public void addLinksummon(MapleSummon x) {
        this.linksummon.add(x);
    }

    public void removeLinksummon(MapleSummon x) {
        if (this.linksummon.size() > 0) {
            this.linksummon.remove(x);
        }
    }

    public int getDamageIncrease(int lm) {
        if (linkMobs.containsKey(lm)) {
            return linkMobs.get(lm);
        }
        return 0;
    }

    public boolean isClone() {
        return cloneOwner != null;
    }

    public MapleCharacter getCloneOwner() {
        return cloneOwner;
    }

    public List<WeakReference<MapleCharacter>> getClones() {
        return clones;
    }

    public MapleCharacter cloneLooks() {
        MapleClient cloneclient = new MapleClient(null, null, new MockIOSession());

        final int minus = (getId() + Randomizer.nextInt(Integer.MAX_VALUE - getId())); // really randomize it, dont want
        // it to fail

        MapleCharacter ret = new MapleCharacter(true);
        ret.id = minus;
        ret.client = cloneclient;
        ret.exp = 0;
        ret.meso = 0;
        ret.remainingAp = 0;
        ret.fame = 0;
        ret.accountid = client.getAccID();
        ret.anticheat = anticheat;
        ret.name = name;
        ret.level = level;
        ret.fame = fame;
        ret.job = job;
        ret.hair = hair;
        ret.face = face;
        ret.faceMarking = faceMarking;
        ret.ears = ears;
        ret.tail = tail;
        ret.skinColor = skinColor;
        ret.monsterbook = monsterbook;
        ret.mount = mount;
        ret.calcDamage = new CalcDamage();
        ret.gmLevel = gmLevel;
        ret.gender = gender;
        ret.mapid = map.getId();
        ret.map = map;
        ret.setStance(getStance());
        ret.chair = chair;
        ret.itemEffect = itemEffect;
        ret.guildid = guildid;
        ret.stats = stats;
        ret.effects.putAll(effects);
        ret.dispelSummons();
        ret.guildrank = guildrank;
        ret.guildContribution = guildContribution;
        ret.allianceRank = allianceRank;
        ret.setPosition(getTruePosition());
        for (Item equip : getInventory(MapleInventoryType.EQUIPPED).newList()) {
            ret.getInventory(MapleInventoryType.EQUIPPED).addFromDB(equip.copy());
        }
        ret.skillMacros = skillMacros;
        ret.keylayout = keylayout;
        ret.questinfo = questinfo;
        ret.savedLocations = savedLocations;
        ret.wishlist = wishlist;
        ret.buddylist = buddylist;
        ret.lastmonthfameids = lastmonthfameids;
        ret.lastfametime = lastfametime;
        ret.storage = storage;
        ret.cs = this.cs;
        ret.client.setAccountName(client.getAccountName());
        ret.client.setGmLevel(PlayerGMRank.getByLevel(client.getGmLevel()));
        ret.beanfunGash = beanfunGash;
        ret.mileage = mileage;
        ret.maplepoints = maplepoints;
        ret.cloneOwner = this;
        ret.client.setChannel(this.client.getChannel());
        while (map.getCharacterById(ret.id) != null
                || client.getChannelServer().getPlayerStorage().getCharacterById(ret.id) != null) {
            ret.id++;
        }
        ret.client.setPlayer(ret);
        return ret;
    }

    public final void cloneLook() {
        if (isClone() || inPVP()) {
            return;
        }
        for (int i = 0; i < clones.size(); i++) {
            if (clones.get(i).get() == null) {
                final MapleCharacter newp = cloneLooks();
                newp.setName("[分身" + (i + 1) + "號]" + newp.getName());
                map.addPlayer(newp);
                map.broadcastMessage(CField.updateCharLook(newp, false));
                map.movePlayer(newp, getTruePosition());
                clones.remove(i);
                clones.add(i, new WeakReference<>(newp));
                return;
            }
        }
    }

    public final void disposeClones() {
        numClones = 0;
        for (int i = 0; i < clones.size(); i++) {
            if (clones.get(i).get() != null) {
                map.removePlayer(clones.get(i).get());
                if (clones.get(i).get().getClient() != null) {
                    clones.get(i).get().getClient().setPlayer(null);
                    clones.get(i).get().client = null;
                }
                clones.remove(i);
                clones.add(i, new WeakReference<>(null));
                numClones++;
            }
        }
    }

    public final int getCloneSize() {
        int z = 0;
        for (WeakReference<MapleCharacter> clone : clones) {
            if (clone.get() != null)
                z++;
        }
        return z;
    }

    public void spawnClones() {
        if (!isGM()) { // removed tetris piece likely, expired or whatever
            numClones = (byte) (stats.hasClone ? 1 : 0);
        }
        for (int i = 0; i < numClones; i++) {
            cloneLook();
        }
        numClones = 0;
    }

    public byte getNumClones() {
        return numClones;
    }

    public void setDragon(MapleDragon d) {
        this.dragon = d;
    }

    public void setHaku(MapleHaku h) {
        this.haku = h;
    }

    public MapleExtractor getExtractor() {
        return extractor;
    }

    public void setExtractor(MapleExtractor me) {
        removeExtractor();
        this.extractor = me;
    }

    public void removeExtractor() {
        if (extractor != null) {
            map.broadcastMessage(CField.removeDecomposer(this.id));
            map.removeMapObject(extractor);
            extractor = null;
        }
    }

    public final void spawnSavedPets() {
        for (int i = 0; i < petStore.length; i++) {
            if (petStore[i] > -1) {
                spawnPet(petStore[i], false, false);
            }
        }
        petStore = new byte[]{-1, -1, -1};
    }

    public final byte[] getPetStores() {
        return petStore;
    }

    public int getPlayerStats() {
        return getHpApUsed() + getMpApUsed() + stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt()
                + getRemainingAp();
    }

    public int getMaxStats(boolean hpmpap) {
        int total = 20;

        if (MapleJob.getJobGrade(job) >= 3 && level >= 60) {
            total += (MapleJob.getJobGrade(job) - 2) * 5;
        }

        total += level * 5;

        if (!hpmpap) {
            total -= getHpApUsed();
            total -= getMpApUsed();
        }

        return total;
    }

    public boolean checkMaxStat() {
        if (getLevel() < 10) {
            return false;
        }
        return getPlayerStats() != getMaxStats(true);
    }

    public void resetStats(int str, int dex, int int_, int luk) {
        resetStats(str, dex, int_, luk, false);
    }

    public void resetStats(final int str, final int dex, final int int_, final int luk, boolean resetAll) {
        Map<MapleStat, Long> stat = new EnumMap<>(MapleStat.class);
        int total = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt() + getRemainingAp();
        if (resetAll) {
            total = getMaxStats(false);
        }
        total -= str;
        stats.力量 = (int) str;

        total -= dex;
        stats.敏捷 = (int) dex;

        total -= int_;
        stats.智力 = (int) int_;

        total -= luk;
        stats.幸運 = (int) luk;

        setRemainingAp((int) total);
        stats.recalcLocalStats(this);
        stat.put(MapleStat.STR, (long) str);
        stat.put(MapleStat.DEX, (long) dex);
        stat.put(MapleStat.INT, (long) int_);
        stat.put(MapleStat.LUK, (long) luk);
        stat.put(MapleStat.AVAILABLEAP, (long) total);
        client.announce(CWvsContext.updatePlayerStats(stat, false, this));
        client.announce(EffectPacket.TutInstructionalBalloon("Effect/OnUserEff.img/RecordClear_BT/clear"));
    }

    public Event_PyramidSubway getPyramidSubway() {
        return pyramidSubway;
    }

    public void setPyramidSubway(Event_PyramidSubway ps) {
        this.pyramidSubway = ps;
    }

    public byte getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(int z) {
        this.subcategory = (byte) z;
    }

    public int itemQuantity(final int itemid) {
        return getInventory(GameConstants.getInventoryType(itemid)).countById(itemid);
    }

    public void setRPS(RockPaperScissors rps) {
        this.rps = rps;
    }

    public RockPaperScissors getRPS() {
        return rps;
    }

    public long getNextConsume() {
        return nextConsume;
    }

    public void setNextConsume(long nc) {
        this.nextConsume = nc;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getWorldRank() {
        return worldRank;
    }

    public int getWorldankMove() {
        return worldRankMove;
    }

    public void getAllModes() {
        if (MapleJob.is幻獸師(job)) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            ss.put(SkillFactory.getSkill(110001501), new SkillEntry((byte) 1, (byte) 1, -1));
            ss.put(SkillFactory.getSkill(110001502), new SkillEntry((byte) 1, (byte) 1, -1));
            ss.put(SkillFactory.getSkill(110001503), new SkillEntry((byte) 1, (byte) 1, -1));
            ss.put(SkillFactory.getSkill(110001504), new SkillEntry((byte) 1, (byte) 1, -1));
            changeSkillsLevel(ss);
            NPCScriptManager.getInstance().dispose(client);
            System.out.println("All modes are added");
        } else {
            dropMessage(6, "You are not beast tamer!");
        }
    }

    public void changeChannel(final int channel) {
        final ChannelServer toch = ChannelServer.getInstance(channel);

        if (channel == client.getChannel() || toch == null || toch.isShutdown()) {
            client.announce(CField.serverBlocked(1));
            return;
        }
        this.forceAtomInfo.reset();
        changeRemoval();
        final ChannelServer ch = ChannelServer.getInstance(client.getChannel());
        if (getMessenger() != null) {
            World.Messenger.silentLeaveMessenger(getMessenger().getId(), new MapleMessengerCharacter(this));
        }
        World.ChannelChange_Data(client, this, channel);
        ch.removePlayer(this);
        client.updateLoginState(MapleClient.CHANGE_CHANNEL, client.getSessionIPAddress());
        final String s = client.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        client.announce(CField.getChannelChange(Integer.parseInt(toch.getIP().split(":")[1])));
        saveToDB(false, false);
        getMap().removePlayer(this);

        client.setPlayer(null);
        client.setReceiving(false);
    }

    public void expandInventory(byte type, int amount) {
        final MapleInventory inv = getInventory(MapleInventoryType.getByType(type));
        inv.addSlot((short) amount);
        client.announce(InventoryPacket.getSlotUpdate(type, inv.getSlotLimit()));
    }

    public boolean allowedToTarget(MapleCharacter other) {
        return other != null && (!other.isHidden() || getGmLevel() >= other.getGmLevel());
    }

    public int getFollowId() {
        return followid;
    }

    public void setFollowId(int fi) {
        this.followid = fi;
        if (fi == 0) {
            this.followinitiator = false;
            this.followon = false;
        }
    }

    public void setFollowInitiator(boolean fi) {
        this.followinitiator = fi;
    }

    public void setFollowOn(boolean fi) {
        this.followon = fi;
    }

    public boolean isFollowOn() {
        return followon;
    }

    public boolean isFollowInitiator() {
        return followinitiator;
    }

    public void checkFollow() {
        if (followid <= 0) {
            return;
        }
        if (followon) {
            map.broadcastMessage(CField.followEffect(id, 0, null));
            map.broadcastMessage(CField.followEffect(followid, 0, null));
        }
        MapleCharacter tt = map.getCharacterById(followid);
        client.announce(CField.getFollowMessage("跟隨解除。"));
        if (tt != null) {
            tt.setFollowId(0);
            tt.getClient().announce(CField.getFollowMessage("跟隨解除。"));
        }
        setFollowId(0);
    }

    public int getMarriageId() {
        return marriageId;
    }

    public void setMarriageId(final int mi) {
        this.marriageId = mi;
    }

    public int getMarriageItemId() {
        return marriageItemId;
    }

    public void setMarriageItemId(final int mi) {
        this.marriageItemId = mi;
    }

    public MapleMarriage getMarriage() {
        return marriage;
    }

    public void setMarriage(MapleMarriage marriage) {
        this.marriage = marriage;
    }

    public boolean isStaff() {
        return this.gmLevel >= 1;
    }

    public boolean startPartyQuest(final int questid) {
        boolean ret = false;
        MapleQuest q = MapleQuest.getInstance(questid);
        if (q == null || !q.isPartyQuest()) {
            return false;
        }
        if (!quests.containsKey(q) || !questinfo.containsKey(questid)) {
            final MapleQuestStatus status = getQuestNAdd(q);
            status.setStatus((byte) 1);
            updateQuest(status);
            switch (questid) {
                case 1300:
                case 1301:
                case 1302: // carnival, ariants.
                    updateInfoQuest(questid,
                            "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0;gvup=0;vic=0;lose=0;draw=0");
                    break;
                case 1303: // ghost pq
                    updateInfoQuest(questid,
                            "min=0;sec=0;date=0000-00-00;have=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0;vic=0;lose=0");
                    break;
                case 1204: // herb town pq
                    updateInfoQuest(questid,
                            "min=0;sec=0;date=0000-00-00;have0=0;have1=0;have2=0;have3=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                case 1206: // ellin pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                default:
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
            }
            ret = true;
        } // started the quest.
        return ret;
    }

    public String getOneInfo(final int questid, final String key) {
        if (!questinfo.containsKey(questid)
                || MapleQuest.getInstance(questid) == null/* || !MapleQuest.getInstance(questid).isPartyQuest() */) {
            return null;
        }
        return StringTool.getOneValue(questinfo.get(questid), key);
    }

    public void updateOneInfo(final int questid, final String key, final String value) {
        if (MapleQuest.getInstance(questid) == null/* || !MapleQuest.getInstance(questid).isPartyQuest() */) {
            return;
        }
        String info = StringTool.updateOneValue(questinfo.getOrDefault(questid, null), key, value);
        if (info == null) {
            return;
        }
        if (info.isEmpty()) {
            clearInfoQuest(questid);
        } else {
            updateInfoQuest(questid, info);
        }
    }

    public void recalcPartyQuestRank(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        if (!startPartyQuest(questid)) {
            final String oldRank = getOneInfo(questid, "rank");
            if (oldRank == null || oldRank.equals("S")) {
                return;
            }
            String newRank;
            switch (oldRank) {
                case "A":
                    newRank = "S";
                    break;
                case "B":
                    newRank = "A";
                    break;
                case "C":
                    newRank = "B";
                    break;
                case "D":
                    newRank = "C";
                    break;
                case "F":
                    newRank = "D";
                    break;
                default:
                    return;
            }
            final List<Pair<String, Pair<String, Integer>>> questInfo = MapleQuest.getInstance(questid)
                    .getInfoByRank(newRank);
            if (questInfo == null) {
                return;
            }
            for (Pair<String, Pair<String, Integer>> q : questInfo) {
                boolean found = false;
                final String val = getOneInfo(questid, q.right.left);
                if (val == null) {
                    return;
                }
                int vall;
                try {
                    vall = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return;
                }
                switch (q.left) {
                    case "less":
                        found = vall < q.right.right;
                        break;
                    case "more":
                        found = vall > q.right.right;
                        break;
                    case "equal":
                        found = vall == q.right.right;
                        break;
                }
                if (!found) {
                    return;
                }
            }
            // perfectly safe
            updateOneInfo(questid, "rank", newRank);
        }
    }

    public void tryPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            pqStartTime = System.currentTimeMillis();
            updateOneInfo(questid, "try", String.valueOf(Integer.parseInt(getOneInfo(questid, "try")) + 1));
        } catch (NumberFormatException e) {
            System.err.println("tryPartyQuest error");
        }
    }

    public void endPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            if (pqStartTime > 0) {
                final long changeTime = System.currentTimeMillis() - pqStartTime;
                final int mins = (int) (changeTime / 1000 / 60), secs = (int) (changeTime / 1000 % 60);
                final int mins2 = Integer.parseInt(getOneInfo(questid, "min"));
                if (mins2 <= 0 || mins < mins2) {
                    updateOneInfo(questid, "min", String.valueOf(mins));
                    updateOneInfo(questid, "sec", String.valueOf(secs));
                    updateOneInfo(questid, "date", FileoutputUtil.CurrentReadable_Date());
                }
                final int newCmp = Integer.parseInt(getOneInfo(questid, "cmp")) + 1;
                updateOneInfo(questid, "cmp", String.valueOf(newCmp));
                updateOneInfo(questid, "CR", String
                        .valueOf((int) Math.ceil((newCmp * 100.0) / Integer.parseInt(getOneInfo(questid, "try")))));
                recalcPartyQuestRank(questid);
                pqStartTime = 0;
            }
        } catch (Exception e) {
            System.err.println("endPartyQuest error");
        }

    }

    public void havePartyQuest(final int itemId) {
        int questid, index = -1;
        switch (itemId) {
            case 1002798:
                questid = 1200; // henesys
                break;
            case 1072369:
                questid = 1201; // kerning
                break;
            case 1022073:
                questid = 1202; // ludi
                break;
            case 1082232:
                questid = 1203; // orbis
                break;
            case 1002571:
            case 1002572:
            case 1002573:
            case 1002574:
                questid = 1204; // herbtown
                index = itemId - 1002571;
                break;
            case 1102226:
                questid = 1303; // ghost
                break;
            case 1102227:
                questid = 1303; // ghost
                index = 0;
                break;
            case 1122010:
                questid = 1205; // magatia
                break;
            case 1032061:
            case 1032060:
                questid = 1206; // ellin
                index = itemId - 1032060;
                break;
            case 3010018:
                questid = 1300; // ariant
                break;
            case 1122007:
                questid = 1301; // carnival
                break;
            case 1122058:
                questid = 1302; // carnival2
                break;
            default:
                return;
        }
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        startPartyQuest(questid);
        updateOneInfo(questid, "have" + (index == -1 ? "" : index), "1");
    }

    public boolean hasSummon() {
        return hasSummon;
    }

    public void setHasSummon(boolean summ) {
        this.hasSummon = summ;
    }

    public void removeDoor() {
        final MapleDoor door = getDoors().iterator().next();
        for (final MapleCharacter chr : door.getTarget().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleCharacter chr : door.getTown().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleDoor destroyDoor : getDoors()) {
            door.getTarget().removeMapObject(destroyDoor);
            door.getTown().removeMapObject(destroyDoor);
        }
        clearDoors();
    }

    public void removeMechDoor() {
        for (final MapleOpenGate destroyDoor : getMechDoors()) {
            for (final MapleCharacter chr : getMap().getCharactersThreadsafe()) {
                destroyDoor.sendDestroyData(chr.getClient());
            }
            getMap().removeMapObject(destroyDoor);
        }
        clearMechDoors();
    }

    public void changeRemoval() {
        changeRemoval(false);
    }

    public void changeRemoval(boolean dc) {
        if (getCheatTracker() != null && dc) {
            getCheatTracker().dispose();
        }
        removeFamiliar();
        dispelSummons();
        if (!dc) {
            cancelEffectFromBuffStat(MapleBuffStat.Flying);
            cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
            cancelEffectFromBuffStat(MapleBuffStat.Mechanic);
            cancelEffectFromBuffStat(MapleBuffStat.Regen);
        }
        if (getPyramidSubway() != null) {
            getPyramidSubway().dispose(this);
        }
        if (playerShop != null && !dc) {
            playerShop.removeVisitor(this);
            if (playerShop.isOwner(this)) {
                playerShop.setOpen(true);
            }
        }
        if (!getDoors().isEmpty()) {
            removeDoor();
        }
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        disposeClones();
        NPCScriptManager.getInstance().dispose(client);
        cancelFairySchedule(false);
    }

    public void updateTick(int newTick) {
        anticheat.updateTick(newTick);
    }

    public String getTeleportName() {
        return teleportname;
    }

    public void setTeleportName(final String tname) {
        teleportname = tname;
    }

    public int maxBattleshipHP(int skillid) {
        return (getTotalSkillLevel(skillid) * 5000) + ((getLevel() - 120) * 3000);
    }

    public int currentBattleshipHP() {
        return battleshipHP;
    }

    public void setBattleshipHP(int v) {
        this.battleshipHP = v;
    }

    public void decreaseBattleshipHP() {
        this.battleshipHP--;
    }

    public int getGachExp() {
        return gachexp;
    }

    public void setGachExp(int ge) {
        this.gachexp = ge;
    }

    public boolean isInBlockedMap() {
        if (!isAlive() || getPyramidSubway() != null || getMap().getSquadByMap() != null || getEventInstance() != null
                || getMap().getEMByMap() != null) {
            return true;
        }
        if ((getMapId() >= 680000210 && getMapId() <= 680000502)
                || (getMapId() / 10000 == 92502 && getMapId() >= 925020100) || (getMapId() / 10000 == 92503)
                || getMapId() == GameConstants.JAIL) {
            return true;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return true;
            }
        }
        if (getMapId() >= 689010000 && getMapId() < 689014000) { // Pink Zakum
            return true;
        }
        return false;
    }

    public boolean isInTownMap() {
        if (hasBlockedInventory() || !getMap().isTown() || FieldLimitType.VipRock.check(getMap().getFieldLimit())
                || getEventInstance() != null) {
            return false;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return false;
            }
        }
        return true;
    }

    public boolean hasBlockedInventory() {
        boolean hasBlockedInventory = !isAlive() || getTrade() != null || getConversation() > 0 || getDirection() >= 0
                || getPlayerShop() != null || map == null;
        return hasBlockedInventory;
    }

    public void startPartySearch(final List<Integer> jobs, final int maxLevel, final int minLevel,
            final int membersNeeded) {
        for (MapleCharacter chr : map.getCharacters()) {
            if (chr.getId() != id && chr.getParty() == null && chr.getLevel() >= minLevel && chr.getLevel() <= maxLevel
                    && (jobs.isEmpty() || jobs.contains((int) chr.getJob())) && (isIntern() || !chr.isIntern())) {
                if (party != null && party.getMembers().size() < 6 && party.getMembers().size() < membersNeeded) {
                    chr.setParty(party);
                    World.Party.updateParty(party.getId(), PartyOperation.PartyRes_JoinParty_Done,
                            new MaplePartyCharacter(chr));
                    chr.receivePartyMemberHP();
                    chr.updatePartyMemberHP();
                } else {
                    break;
                }
            }
        }
    }

    public int getChallenge() {
        return challenge;
    }

    public void setChallenge(int c) {
        this.challenge = c;
    }

    public short getFatigue() {
        return fatigue;
    }

    public void setFatigue(int j) {
        this.fatigue = (short) Math.max(0, j);
        updateSingleStat(MapleStat.FATIGUE, this.fatigue);
    }

    public void fakeRelog() {
        client.announce(CField.onSetField(this));
        final MapleMap mapp = getMap();
        mapp.setCheckStates(false);
        mapp.removePlayer(this);
        mapp.addPlayer(this);
        mapp.setCheckStates(true);

        client.announce(CWvsContext.getFamiliarInfo(this));
    }

    public boolean canSummon() {
        return canSummon(5000);
    }

    public boolean canSummon(int g) {
        if (lastSummonTime + g < System.currentTimeMillis()) {
            lastSummonTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public int getIntNoRecord(int questID) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(questID));
        if (stat == null || stat.getCustomData() == null) {
            return 0;
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public long getLongNoRecord(int questID) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(questID));
        if (stat == null || stat.getCustomData() == null) {
            return 0;
        }
        return Long.parseLong(stat.getCustomData());
    }

    public int getIntRecord(int questID) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(questID));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public long getLongRecord(int questID) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(questID));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Long.parseLong(stat.getCustomData());
    }

    public void updatePetAuto() {
        if (getIntNoRecord(GameConstants.HP_ITEM) > 0) {
            client.announce(CField.petAutoHP(getIntRecord(GameConstants.HP_ITEM)));
        }
        if (getIntNoRecord(GameConstants.MP_ITEM) > 0) {
            client.announce(CField.petAutoMP(getIntRecord(GameConstants.MP_ITEM)));
        }
        if (getIntNoRecord(GameConstants.BUFF_ITEM) > 0) {
            client.announce(CField.petAutoBuff(getIntRecord(GameConstants.BUFF_ITEM)));
        }
    }

    public void sendEnglishQuiz(String msg) {
        client.announce(CScriptMan.getEnglishQuiz(9010000, (byte) 0, 9010000, msg, "00 00"));
    }

    public void setChangeTime() {
        mapChangeTime = System.currentTimeMillis();
    }

    public long getChangeTime() {
        return mapChangeTime;
    }

    public Map<ReportType, Integer> getReports() {
        return reports;
    }

    public void addReport(ReportType type) {
        Integer value = reports.get(type);
        reports.put(type, value == null ? 1 : (value + 1));
        changed_reports = true;
    }

    public void clearReports(ReportType type) {
        reports.remove(type);
        changed_reports = true;
    }

    public void clearReports() {
        reports.clear();
        changed_reports = true;
    }

    public final int getReportPoints() {
        int ret = 0;
        for (Integer entry : reports.values()) {
            ret += entry;
        }
        return ret;
    }

    public final String getReportSummary() {
        final StringBuilder ret = new StringBuilder();
        final List<Pair<ReportType, Integer>> offenseList = new ArrayList<>();
        for (final Entry<ReportType, Integer> entry : reports.entrySet()) {
            offenseList.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        offenseList.sort(new ComparatorImpl());
        for (int x = 0; x < offenseList.size(); x++) {
            ret.append(StringUtil.makeEnumHumanReadable(offenseList.get(x).left.name()));
            ret.append(": ");
            ret.append(offenseList.get(x).right);
            ret.append(" ");
        }
        return ret.toString();
    }

    public short getScrolledPosition() {
        return scrolledPosition;
    }

    public void setScrolledPosition(short s) {
        this.scrolledPosition = s;
    }

    public MapleTrait getTrait(MapleTraitType t) {
        return traits.get(t);
    }

    public void forceCompleteQuest(int id) {
        MapleQuest.getInstance(id).forceComplete(this, 9270035); // troll
    }

    public List<Integer> getExtendedSlots() {
        return extendedSlots;
    }

    public int getExtendedSlot(int index) {
        if (extendedSlots.size() <= index || index < 0) {
            return -1;
        }
        return extendedSlots.get(index);
    }

    public void changedExtended() {
        changed_extendedSlots = true;
    }

    public MapleAndroid getAndroid() {
        return android;
    }

    public void removeAndroid() {
        if (map != null) {
            if (android != null) {
                android.showEmotion(this, "bye");
            }
            map.broadcastMessage(CField.deactivateAndroid(this.id));
        }
        android = null;
    }

    public void setAndroid(MapleAndroid a) {
        android = a;
        if ((map != null) && (a != null)) {
            map.broadcastMessage(CField.spawnAndroid(this, a));
            a.showEmotion(this, "hello");
        }
    }

    public void updateAndroid(short position) {
        if (this.map != null) {
            if (position == 0) {
                this.map.broadcastMessage(CField.updateAndroidLook(this, android));
            } else {
                this.map.broadcastMessage(CField.updateAndroidEquip(this, position));
            }
        }
    }

    public Map<Integer, MonsterFamiliar> getFamiliars() {
        return familiars;
    }

    public MonsterFamiliar getSummonedFamiliar() {
        return summonedFamiliar;
    }

    public void removeFamiliar() {
        if (summonedFamiliar != null && map != null) {
            removeVisibleFamiliar();
        }
        summonedFamiliar = null;
    }

    public void removeVisibleFamiliar() {
        getMap().removeMapObject(summonedFamiliar);
        removeVisibleMapObject(summonedFamiliar);
        getMap().broadcastMessage(FamiliarPacket.removeFamiliar(this.getId()));
        anticheat.resetFamiliarAttack();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        cancelEffect(ii.getItemEffect(ii.getFamiliar(summonedFamiliar.getFamiliar()).getEffectAfter()), false,
                System.currentTimeMillis());
    }

    public void spawnFamiliar(MonsterFamiliar mf, boolean respawn) {
        summonedFamiliar = mf;
        mf.setStance(0);
        mf.setPosition(getPosition());
        mf.setFh(getFh());
        addVisibleMapObject(mf);
        getMap().spawnFamiliar(mf, respawn);
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleStatEffect eff = ii.getItemEffect(ii.getFamiliar(summonedFamiliar.getFamiliar()).getEffectAfter());
        if (eff != null && eff.getInterval() <= 0 && eff.makeChanceResult()) { // i think this is actually done through
            // a recv, which is ATTACK_FAMILIAR +1
            eff.applyTo(this);
        }
        lastFamiliarEffectTime = System.currentTimeMillis();
    }

    public final boolean canFamiliarEffect(long now, MapleStatEffect eff) {
        return lastFamiliarEffectTime > 0 && lastFamiliarEffectTime + eff.getInterval() < now;
    }

    public void doFamiliarSchedule(long now) {
        // if (familiars == null) {
        // return;
        // }
        // for (MonsterFamiliar mf : familiars.values()) {
        // if (summonedFamiliar != null && summonedFamiliar.getId() == mf.getId()) {
        // mf.addFatigue(this, 5);
        // final MapleItemInformationProvider ii =
        // MapleItemInformationProvider.getInstance();
        // final MapleStatEffect eff =
        // ii.getItemEffect(ii.getFamiliar(summonedFamiliar.getFamiliar()).getEffectAfter());
        // if (eff != null && eff.getInterval() > 0 && canFamiliarEffect(now, eff) &&
        // eff.makeChanceResult()) {
        // eff.applyTo(this);
        // }
        // } else if (mf.getFatigue() > 0) {
        // mf.setFatigue(Math.max(0, mf.getFatigue() - 5));
        // }
        // }
    }

    public MapleImp[] getImps() {
        return imps;
    }

    public void sendImp() {
        for (int i = 0; i < imps.length; i++) {
            if (imps[i] != null) {
                client.announce(CWvsContext.updateImp(imps[i], ImpFlag.SUMMONED.getValue(), i, true));
            }
        }
    }

    public int getBattlePoints() {
        return pvpPoints;
    }

    public int getTotalBattleExp() {
        return pvpExp;
    }

    public void setBattlePoints(int p) {
        if (p != pvpPoints) {
            client.announce(InfoPacket.getBPMsg(p - pvpPoints));
            updateSingleStat(MapleStat.BATTLE_POINTS, p);
        }
        this.pvpPoints = p;
    }

    public void setTotalBattleExp(int p) {
        final int previous = pvpExp;
        this.pvpExp = p;
        if (p != previous) {
            stats.recalcPVPRank(this);

            updateSingleStat(MapleStat.BATTLE_EXP, stats.pvpExp);
            updateSingleStat(MapleStat.BATTLE_RANK, stats.pvpRank);
        }
    }

    public void changeTeam(int newTeam) {
        this.pvpTeam = newTeam;
        if (inPVP()) {
            client.announce(CField.getPVPTransform(newTeam + 1));
            map.broadcastMessage(CField.changeTeam(id, newTeam + 1));
        } else {
            client.announce(CField.showEquipEffect(newTeam));
        }
    }

    public void disease(int type, int level) {
        if (MapleDisease.getBySkill(type) == null) {
            return;
        }
        chair = 0;
        client.announce(CField.cancelChair(-1, getId()));
        map.broadcastMessage(this, CField.showChair(id, 0, ""), false);
        giveDebuff(MapleDisease.getBySkill(type), MobSkillFactory.getMobSkill(type, level));
    }

    public boolean inPVP() {
        return eventInstance != null && eventInstance.getName().startsWith("PVP");
    }

    public boolean inAzwan() {
        return mapid >= 262020000 && mapid < 262023000;
    }

    public void clearAllCooldowns() {
        for (MapleCoolDownValueHolder m : getCooldowns()) {
            final int skil = m.skillId;
            removeCooldown(skil);
        }
    }

    public Pair<Double, Boolean> modifyDamageTaken(double damage, MapleMapObject attacke) {
        Pair<Double, Boolean> ret = new Pair<>(damage, false);
        if (damage <= 0) {
            return ret;
        }
        if (Randomizer.isSuccess(stats.ignoreDAMr_rate)) {
            damage -= Math.floor((stats.ignoreDAMr * damage) / 100.0f);
        }
        if (Randomizer.isSuccess(stats.ignoreDAM_rate)) {
            damage -= stats.ignoreDAM;
        }
        final Integer div = getBuffedValue(MapleBuffStat.BlessingArmor);
        final Integer div2 = getBuffedValue(MapleBuffStat.HolyMagicShell);
        if (div2 != null) {
            if (div2 <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.HolyMagicShell);
            } else {
                setBuffedValue(MapleBuffStat.HolyMagicShell, div2 - 1);
                damage = 0;
            }
        } else if (div != null) {
            if (div <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.BlessingArmor);
            } else {
                setBuffedValue(MapleBuffStat.BlessingArmor, div - 1);
                damage = 0;
            }
        }
        MapleStatEffect barrier = getStatForBuff(MapleBuffStat.ComboBarrier);
        if (barrier != null) {
            damage = ((barrier.getX() / 1000.0) * damage);
        }
        barrier = getStatForBuff(MapleBuffStat.MagicShield);
        if (barrier != null) {
            damage = ((barrier.getX() / 1000.0) * damage);
        }
        barrier = getStatForBuff(MapleBuffStat.DamAbsorbShield);
        if (barrier != null) {
            damage -= ((barrier.getX() / 100.0) * damage);
        }
        List<Integer> attack = attacke instanceof MapleMonster || attacke == null ? null : (new ArrayList<Integer>());
        if (damage > 0) {
            if (getJob() == 122 && !skillisCooling(1220013)) {
                final Skill divine = SkillFactory.getSkill(1220013);
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        addCooldown(1220013, System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                    }
                }
            } else if (getBuffedValue(MapleBuffStat.Cyclone) != null
                    && getBuffedValue(MapleBuffStat.HowlingCritical) != null
                    && getBuffedValue(MapleBuffStat.PickPocket) != null) {
                double buff = getBuffedValue(MapleBuffStat.Cyclone).doubleValue();
                double buffz = getBuffedValue(MapleBuffStat.Cyclone).doubleValue();
                if ((int) ((buff / 100.0) * getStat().getMaxHp()) <= damage) {
                    damage -= ((buffz / 100.0) * damage);
                    cancelEffectFromBuffStat(MapleBuffStat.PickPocket);
                }
            } else if (getJob() == 433 || getJob() == 434) {
                final Skill divine = SkillFactory.getSkill(4330001);
                if (getTotalSkillLevel(divine) > 0 && getBuffedValue(MapleBuffStat.DarkSight) == null
                        && !skillisCooling(divine.getId())) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (Randomizer.isSuccess(divineShield.getX())) {
                        divineShield.applyTo(this);
                    }
                }
            } else if ((getJob() == 512 || getJob() == 522) && getBuffedValue(MapleBuffStat.DamR) == null) {
                final Skill divine = SkillFactory.getSkill(getJob() == 512 ? 5120011 : 5220012);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId())) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                    }
                }
            } else if (getJob() == 312 && attacke != null) {
                final Skill divine = SkillFactory.getSkill(3120010);
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        if (attacke instanceof MapleMonster) {
                            final Rectangle bounds = divineShield.calculateBoundingBox(getTruePosition(),
                                    isFacingLeft());
                            final List<MapleMapObject> affected = getMap().getMapObjectsInRect(bounds,
                                    Collections.singletonList(attacke.getType()));
                            int i = 0;

                            for (final MapleMapObject mo : affected) {
                                MapleMonster mons = (MapleMonster) mo;
                                if (mons.getStats().isFriendly() || mons.isFake()) {
                                    continue;
                                }
                                mons.applyStatus(
                                        this, new MonsterStatusEffect(MonsterStatus.M_Stun, 1,
                                                divineShield.getSourceId(), null, false),
                                        false, divineShield.getDuration(), true, divineShield);
                                final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage()
                                        / 100.0);
                                mons.damage(this, theDmg, true);
                                getMap().broadcastMessage(MobPacket.damageMonster(mons.getObjectId(), theDmg));
                                i++;
                                if (i >= divineShield.getMobCount()) {
                                    break;
                                }
                            }
                        } else {
                            MapleCharacter chr = (MapleCharacter) attacke;
                            chr.addHP(-divineShield.getDamage());
                            attack.add(divineShield.getDamage());
                        }
                    }
                }
            } else if ((getJob() == 531 || getJob() == 532) && attacke != null) {
                final Skill divine = SkillFactory.getSkill(5310009); // slea.readInt() = 5310009, then slea.readInt() =
                // damage. (175000)
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        if (attacke instanceof MapleMonster) {
                            final MapleMonster attacker = (MapleMonster) attacke;
                            final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage()
                                    / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            final MapleCharacter attacker = (MapleCharacter) attacke;
                            attacker.addHP(-divineShield.getDamage());
                            attack.add(divineShield.getDamage());
                        }
                    }
                }
            } else if (getJob() == 132 && attacke != null) {
                final Skill divine = SkillFactory.getSkill(1320011);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId())
                        && getBuffSource(MapleBuffStat.Beholder) == 1321007) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                        if (attacke instanceof MapleMonster) {
                            final MapleMonster attacker = (MapleMonster) attacke;
                            final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage()
                                    / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            final MapleCharacter attacker = (MapleCharacter) attacke;
                            attacker.addHP(-divineShield.getDamage());
                            attack.add(divineShield.getDamage());
                        }
                    }
                }
            }
            if (attacke != null) {
                final int damr = (Randomizer.isSuccess(getStat().DAMreflect_rate) ? getStat().DAMreflect : 0)
                        + (getBuffedValue(MapleBuffStat.PowerGuard) != null ? getBuffedValue(MapleBuffStat.PowerGuard)
                        : 0);
                final int bouncedam_ = damr
                        + (getBuffedValue(MapleBuffStat.Guard) != null ? getBuffedValue(MapleBuffStat.Guard) : 0);
                if (bouncedam_ > 0) {
                    long bouncedamage = (long) (damage * bouncedam_ / 100);
                    long bouncer = (long) (damage * damr / 100);
                    damage -= bouncer;
                    if (attacke instanceof MapleMonster) {
                        final MapleMonster attacker = (MapleMonster) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 10);
                        attacker.damage(this, (int) bouncedamage, true);
                        getMap().broadcastMessage(this, MobPacket.damageMonster(attacker.getObjectId(), bouncedamage),
                                getTruePosition());
                        if (getBuffSource(MapleBuffStat.Guard) == 31101003) {
                            MapleStatEffect eff = this.getStatForBuff(MapleBuffStat.Guard);
                            if (eff.makeChanceResult()) {
                                attacker.applyStatus(this, new MonsterStatusEffect(MonsterStatus.M_Stun, 1,
                                        eff.getSourceId(), null, false), false, eff.getSubTime(), true, eff);
                            }
                        }
                    } else {
                        final MapleCharacter attacker = (MapleCharacter) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getStat().getCurrentMaxHp() / 10);
                        attacker.addHP(-((int) bouncedamage));
                        attack.add((int) bouncedamage);
                        if (getBuffSource(MapleBuffStat.Guard) == 31101003) {
                            MapleStatEffect eff = this.getStatForBuff(MapleBuffStat.Guard);
                            if (eff.makeChanceResult()) {
                                attacker.disease(MapleDisease.昏迷.getDisease(), 1);
                            }
                        }
                    }
                    ret.right = true;
                }
                if ((getJob() == 411 || getJob() == 412 || getJob() == 421 || getJob() == 422)
                        && getBuffedValue(MapleBuffStat.SUMMON) != null && attacke != null) {
                    final List<MapleSummon> ss = getSummonsReadLock();
                    try {
                        for (MapleSummon sum : ss) {
                            if (sum.getTruePosition().distanceSq(getTruePosition()) < 400000.0
                                    && (sum.getSkill() == 4111007 || sum.getSkill() == 4211007
                                    || sum.getSkill() == 14111010)) {
                                if (attacke instanceof MapleMonster) {
                                    List<Pair<Long, Boolean>> allDamageNumbers = new ArrayList<>();
                                    List<AttackPair> allDamage = new ArrayList<>();
                                    final MapleMonster attacker = (MapleMonster) attacke;
                                    final long theDmg = (long) (SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX() * damage / 100.0);
                                    allDamageNumbers.add(new Pair<>(theDmg, false));
                                    allDamage.add(new AttackPair(attacker.getObjectId(), allDamageNumbers));
                                    getMap().broadcastMessage(SummonPacket.summonAttack(sum.getOwnerId(), sum.getObjectId(), (byte) 0x84, (byte) 0x11, allDamage, getLevel(), true));
                                    attacker.damage(this, theDmg, true);
                                    checkMonsterAggro(attacker);
                                    if (!attacker.isAlive()) {
                                        getClient().announce(MobPacket.killMonster(attacker.getObjectId(), 1, false));
                                    }
                                } else {
                                    final MapleCharacter chr = (MapleCharacter) attacke;
                                    final int dmg = SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX();
                                    chr.addHP(-dmg);
                                    attack.add(dmg);
                                }
                            }
                        }
                    } finally {
                        unlockSummonsReadLock();
                    }
                }
            }
        }
        if (attack != null && attack.size() > 0 && attacke != null) {
            getMap().broadcastMessage(CField.pvpCool(attacke.getObjectId(), attack));
        }
        ret.left = damage;
        return ret;
    }

    public void onAttack(long maxhp, int maxmp, int skillid, MapleMonster target, int totDamage, int critCount,
            int hits) {
        if (target.isBuffed(MonsterStatus.M_Weakness)) {
            addHP(-(7000 + Randomizer.nextInt(8000)));
        }
        if (Randomizer.isSuccess(stats.hpRecoverProp)) {// i think its out of 100, anyway
            if (stats.hpRecover > 0) {
                healHP(stats.hpRecover);
            }
            if (stats.hpRecoverPercent > 0) {
                int heal = ((int) Math.min(maxhp, Math.min(
                        ((int) ((double) totDamage * (double) stats.hpRecoverPercent / 100.0)), stats.getMaxHp() / 2)));
                if (heal < 0) {
                    heal = 0;
                }
                addHP(heal);
            }
        }
        if (!MapleJob.is惡魔殺手(getJob())) {
            if (Randomizer.isSuccess(stats.mpRecoverProp)) {// i think its out of 100, anyway
                healMP(stats.mpRecover);
            }
        }
        if (getBuffedValue(MapleBuffStat.ComboDrain) != null) {
            int heal = ((int) Math.min(maxhp, Math.min(
                    ((int) ((double) totDamage * (double) getStatForBuff(MapleBuffStat.ComboDrain).getX() / 100.0)),
                    stats.getMaxHp() / 2)));
            heal = Math.max(heal, 0);
            addHP(heal);
        }

        // // 精靈遊俠 靈魂灌注
        // if (getBuffSource(MapleBuffStat.ComboDrain) == 23101003) {
        // addMP(((int) Math.min(maxmp, Math.min(((int) ((double) totDamage * (double)
        // getStatForBuff(MapleBuffStat.ComboDrain).getX() / 100.0)), stats.getMaxMp() /
        // 2))));
        // }
        if (getBuffedValue(MapleBuffStat.Revive) != null && getBuffedValue(MapleBuffStat.SUMMON) == null
                && getSummonsSize() < 4 && canSummon()) {
            final MapleStatEffect eff = getStatForBuff(MapleBuffStat.Revive);
            if (eff.makeChanceResult()) {
                eff.applyTo(this, this, false, null, eff.getDuration());
            }
        }

        int[] venomskills = {4110011, 4210010, 4320005, 14110004};
        for (int i : venomskills) {
            if (i == 4110011) {
                if (getTotalSkillLevel(4120011) > 0) {
                    i = 4120011;
                }
            } else if (i == 4210010) {
                if (getTotalSkillLevel(4220011) > 0) {
                    i = 4220011;
                }
            }
            final Skill skill = SkillFactory.getSkill(i);
            if (getTotalSkillLevel(skill) > 0) {
                final MapleStatEffect venomEffect = skill.getEffect(getTotalSkillLevel(skill));
                if (venomEffect.makeChanceResult() && target != null) {
                    target.applyStatus(this, new MonsterStatusEffect(MonsterStatus.M_Poison, 1, i, null, false), true,
                            venomEffect.getDuration(), true, venomEffect);
                }
                break;
            }
        }
        // effects
        if (skillid > 0) {
            final Skill skil = SkillFactory.getSkill(skillid);
            final MapleStatEffect effect = skil.getEffect(getTotalSkillLevel(skil));
            switch (skillid) {
                case 15111001:
                case 3111008:
                case 1078:
                case 31111003:
                case 11078:
                case 14101006:
                case 33111006: // swipe
                case 4101005: // drain
                case 5111004: { // Energy Drain
                    addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) effect.getX() / 100.0)),
                            stats.getMaxHp() / 2))));
                    break;
                }
                case 5211006:
                case 22151002: // killer wing
                case 5220011: {// homing
                    setLinkMid(target.getObjectId(), effect.getX());
                    break;
                }
                case 33101007: { // jaguar
                    clearLinkMid();
                    break;
                }
            }
        }
    }

    // TODO 攻擊後技能處理
    public void afterAttack(int mobCount, int attackCount, int skillid) {
        switch (getJob()) {
            case 510:
            case 511:
            case 512: {
                handleEnergyCharge(5100015, mobCount * attackCount);
                break;
            }
            case 1510:
            case 1511:
            case 1512: {
                if (this.getBuffSource(MapleBuffStat.CygnusElementSkill) == 15001022) {
                    handleEnergyCharge(15001022, 1);
                }
                break;
            }
            case 592:
                if (skillid == 4221001) {
                    setBattleshipHP(0);
                } else {
                    setBattleshipHP(Math.min(5, currentBattleshipHP() + 1)); // max 5
                }
                refreshBattleshipHP();
                break;
            case 110:
            case 111:
            case 112:
            case 1111:
            case 1112:
            case 2411:
            case 2412:
                if (skillid != 1111008 & getBuffedValue(MapleBuffStat.ComboCounter) != null) { // shout should not give orbs
                    handleOrbgain();
                }
                break;
        }
        if (getBuffedValue(MapleBuffStat.OWL_SPIRIT) != null) {
            if (currentBattleshipHP() > 0) {
                decreaseBattleshipHP();
            }
            if (currentBattleshipHP() <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.OWL_SPIRIT);
            }
        }

        cancelEffectFromBuffStat(MapleBuffStat.HideAttack);
        cancelEffectFromBuffStat(MapleBuffStat.Sneak);
        final MapleStatEffect ds = getStatForBuff(MapleBuffStat.DarkSight);
        int s = getBuffSource(MapleBuffStat.DarkSight);
        if (ds != null && getBuffSource(MapleBuffStat.DarkSight) != 9001004) {
            if (ds.getSourceId() != 4330001 || !ds.makeChanceResult()) {
                cancelEffectFromBuffStat(MapleBuffStat.DarkSight);
            }
        }
    }

    public void applyIceGage(int x) {
        updateSingleStat(MapleStat.ICE_GAGE, x);
    }

    public Rectangle getBounds() {
        return new Rectangle(getTruePosition().x - 25, getTruePosition().y - 75, 50, 75);
    }

    @Override
    public Map<Byte, Integer> getEquips(boolean fusionAnvil) {
        final Map<Byte, Integer> eq = new HashMap<>();
        for (final Item item : inventory[MapleInventoryType.EQUIPPED.ordinal()].newList()) {
            int itemId = item.getItemId();
            if (item instanceof Equip && fusionAnvil) {
                if (((Equip) item).getFusionAnvil() != 0) {
                    itemId = ((Equip) item).getFusionAnvil();
                }
            }
            eq.put((byte) item.getPosition(), itemId);
        }
        return eq;
    }

    @Override
    public Map<Byte, Integer> getSecondEquips(boolean fusionAnvil) {
        final Map<Byte, Integer> eq = new HashMap<>();
        for (final Item item : inventory[MapleInventoryType.EQUIPPED.ordinal()].newList()) {
            int itemId = item.getItemId();
            if (item instanceof Equip) {
                if (fusionAnvil) {
                    if (((Equip) item).getFusionAnvil() != 0) {
                        itemId = ((Equip) item).getFusionAnvil();
                    }
                }
                if (MapleJob.is天使破壞者(getJob()) && ItemConstants.類型.套服(itemId)) {
                    itemId = 1051291; // ab def overall
                }
            }
            if (MapleJob.is天使破壞者(getJob())) {
                if (!ItemConstants.類型.套服(itemId) && !ItemConstants.類型.副手(itemId) && !ItemConstants.類型.武器(itemId)
                        && !ItemConstants.類型.勳章(itemId)) {
                    continue;
                }
            }
            eq.put((byte) item.getPosition(), itemId);
        }
        return eq;
    }

    @Override
    public Map<Byte, Integer> getTotems() {
        final Map<Byte, Integer> eq = new HashMap<>();
        for (final Item item : inventory[MapleInventoryType.EQUIPPED.ordinal()].newList()) {
            byte pos = (byte) ((item.getPosition() + 5000) * -1);
            if (pos < 0 || pos > 2) { // 3 totem slots
                continue;
            }
            if (item.getItemId() < 1200000 || item.getItemId() >= 1210000) {
                continue;
            }
            eq.put(pos, item.getItemId());
        }
        return eq;
    }

    public CalcDamage getCalcDamage() {
        return calcDamage;
    }

    public int getCardStackMax() {
        return (MapleJob.幻影俠盜4轉.getId() == getJob() ? 40 : 20);
    }

    public final MapleCharacterCards getCharacterCard() {
        return characterCard;
    }

    public final boolean canHold(final int itemid) {
        return getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public int getStr() {
        return str;
    }

    public void setStr(int str) {
        this.str = str;
        stats.recalcLocalStats(this);
    }

    public int getInt() {
        return int_;
    }

    public void setInt(int int_) {
        this.int_ = int_;
        stats.recalcLocalStats(this);
    }

    public int getLuk() {
        return luk;
    }

    public int getDex() {
        return dex;
    }

    public void setLuk(int luk) {
        this.luk = luk;
        stats.recalcLocalStats(this);
    }

    public void setDex(int dex) {
        this.dex = dex;
        stats.recalcLocalStats(this);
    }

    public static String makeMapleReadable(String in) {
        String i = in.replace('I', 'i');
        i = i.replace('l', 'L');
        i = i.replace("rn", "Rn");
        i = i.replace("vv", "Vv");
        i = i.replace("VV", "Vv");
        return i;
    }

    public void changeMap(int map, int portal) {
        MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, warpMap.getPortal(portal));
    }

    public void unchooseStolenSkill(int skillID) { // base skill
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final int stolenjob = MapleJob.getJobGrade(skillID / 10000);
        boolean changed = false;
        for (Pair<Integer, Boolean> sk : stolenSkills) {
            if (sk.right && MapleJob.getJobGrade(sk.left / 10000) == stolenjob) {
                cancelStolenSkill(sk.left);
                sk.right = false;
                changed = true;
            }
        }
        if (changed) {
            final Skill skil = SkillFactory.getSkill(skillID);
            changeSkillLevel_Skip(skil, getTotalSkillLevel(skil), (byte) 0);
            client.announce(PhantomPacket.replaceStolenSkill(GameConstants.getStealSkill(stolenjob), 0));
        }
    }

    public void cancelStolenSkill(int skillID) {
        final Skill skk = SkillFactory.getSkill(skillID);
        final MapleStatEffect eff = skk.getEffect(getTotalSkillLevel(skk));

        if (eff.isMonsterBuff() || (eff.getStatups().isEmpty() && !eff.getMonsterStati().isEmpty())) {
            for (MapleMonster mons : map.getAllMonstersThreadsafe()) {
                for (MonsterStatus b : eff.getMonsterStati().keySet()) {
                    if (mons.isBuffed(b) && mons.getBuff(b).getFromID() == this.id) {
                        mons.cancelStatus(b);
                    }
                }
            }
        } else if (eff.getDuration() > 0 && !eff.getStatups().isEmpty()) {
            for (MapleCharacter chr : map.getCharactersThreadsafe()) {
                chr.cancelEffect(eff, false, -1, eff.getStatups());
            }
        }
    }

    public void chooseStolenSkill(int skillID) {
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final Pair<Integer, Boolean> dummy = new Pair<>(skillID, false);
        if (stolenSkills.contains(dummy)) {
            unchooseStolenSkill(skillID);
            stolenSkills.get(stolenSkills.indexOf(dummy)).right = true;

            client.announce(PhantomPacket
                    .replaceStolenSkill(GameConstants.getStealSkill(MapleJob.getJobGrade(skillID / 10000)), skillID));
            // if (ServerConstants.CUSTOM_SKILL) {
            // addCooldown(20031208, System.currentTimeMillis(), 5000);
            // }
        }
    }

    public void addStolenSkill(int skillID, int skillLevel) {
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final Pair<Integer, Boolean> dummy = new Pair<>(skillID, true);
        final Skill skil = SkillFactory.getSkill(skillID);
        if (!stolenSkills.contains(dummy) && GameConstants.canSteal(skil)) {
            dummy.right = false;
            skillLevel = Math.min(skil.getMaxLevel(), skillLevel);
            final int jobid = MapleJob.getJobGrade(skillID / 10000);
            if (!stolenSkills.contains(dummy) && getTotalSkillLevel(GameConstants.getStealSkill(jobid)) > 0) {
                int count = 0;
                skillLevel = Math.min(getTotalSkillLevel(GameConstants.getStealSkill(jobid)), skillLevel);
                for (Pair<Integer, Boolean> sk : stolenSkills) {
                    if (MapleJob.getJobGrade(sk.left / 10000) == jobid) {
                        count++;
                    }
                }
                if (count < GameConstants.getNumSteal(jobid)) {
                    stolenSkills.add(dummy);
                    changed_skills = true;
                    changeSkillLevel_Skip(skil, skillLevel, (byte) skillLevel);
                    client.announce(PhantomPacket.addStolenSkill(jobid, count, skillID, skillLevel));
                    // client.announce(MaplePacketCreator.updateStolenSkills(this, jobid));
                }
            }
        }
    }

    public void removeStolenSkill(int skillID) {
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final int jobid = MapleJob.getJobGrade(skillID / 10000);
        final Pair<Integer, Boolean> dummy = new Pair<>(skillID, false);
        int count = -1, cc = 0;
        for (int i = 0; i < stolenSkills.size(); i++) {
            if (stolenSkills.get(i).left == skillID) {
                if (stolenSkills.get(i).right) {
                    unchooseStolenSkill(skillID);
                }
                count = cc;
                break;
            } else if (MapleJob.getJobGrade(stolenSkills.get(i).left / 10000) == jobid) {
                cc++;
            }
        }
        if (count >= 0) {
            cancelStolenSkill(skillID);
            stolenSkills.remove(dummy);
            dummy.right = true;
            stolenSkills.remove(dummy);
            changed_skills = true;
            changeSkillLevel_Skip(SkillFactory.getSkill(skillID), 0, (byte) 0);
            // hacky process begins here
            client.announce(PhantomPacket.replaceStolenSkill(GameConstants.getStealSkill(jobid), 0));
            for (int i = 0; i < GameConstants.getNumSteal(jobid); i++) {
                client.announce(PhantomPacket.removeStolenSkill(jobid, i));
            }
            count = 0;
            for (Pair<Integer, Boolean> sk : stolenSkills) {
                if (MapleJob.getJobGrade(sk.left / 10000) == jobid) {
                    client.announce(PhantomPacket.addStolenSkill(jobid, count, sk.left, getTotalSkillLevel(sk.left)));
                    if (sk.right) {
                        client.announce(PhantomPacket.replaceStolenSkill(GameConstants.getStealSkill(jobid), sk.left));
                    }
                    count++;
                }
            }
            client.announce(PhantomPacket.removeStolenSkill(jobid, count));
            // client.announce(MaplePacketCreator.updateStolenSkills(this, jobid));
        }
    }

    public List<Pair<Integer, Boolean>> getStolenSkills() {
        return stolenSkills;
    }

    public void renewInnerSkills(int itemId) {
        renewInnerSkills(itemId, new ArrayList<>());
    }

    public void renewInnerSkills(int itemId, List<Integer> lockPosition) {
        int lines = getLevel() >= 30 ? 1 : getLevel() >= 50 ? 2 : getLevel() >= 70 ? 3 : 0;
        if (lines < getInnerSkillSize()) {
            lines = getInnerSkillSize() > 3 ? 3 : getInnerSkillSize();
        }

        int maxRank = 3;
        if (itemId == 2702000 || itemId == 2702001) {
            maxRank = 2;
        }

        int innerRank = getInnerRank();
        int upgradeRate;
        int downgradeRate;
        switch (innerRank) {
            case 0:
                upgradeRate = 300;
                downgradeRate = 0;
                break;
            case 1:
                upgradeRate = 100;
                downgradeRate = 50;
                break;
            case 2:
                upgradeRate = 50;
                downgradeRate = 500;
                break;
            case 3:
                upgradeRate = 0;
                downgradeRate = 900;
                break;
            default:
                upgradeRate = 0;
                downgradeRate = 0;
                break;
        }
        if (upgradeRate > Randomizer.nextInt(1000) && innerRank < maxRank && itemId > -2) {
            innerRank++;
        }
        if (downgradeRate > Randomizer.nextInt(1000) && innerRank > 0 && itemId > -1 && 5062800 != itemId) {
            innerRank--;
        }

        InnerSkillValueHolder newskill;
        for (int i = 0; i < lines; i++) {
            newskill = null;
            if (lockPosition.contains(i + 1)) {
                continue;
            }
            while (newskill == null) {
                newskill = InnerAbillity.getInstance().renewSkill(innerRank, i + 1);
                if (newskill != null) {
                    for (InnerSkillValueHolder ski : getInnerSkills()) {
                        if (ski != null && ski.getSkillId() == newskill.getSkillId()) {
                            newskill = null;
                            break;
                        }
                    }
                }
                if (newskill != null) {
                    changeInnerSkill(newskill);
                }
            }
        }
    }

    public int getInnerRank() {
        int innerRank = 0;
        for (InnerSkillValueHolder i : innerSkills) {
            if (i == null) {
                continue;
            }
            if (i.getRank() > innerRank) {
                innerRank = i.getRank();
            }
        }
        return innerRank;
    }

    public InnerSkillValueHolder[] getInnerSkills() {
        return innerSkills;
    }

    public int getInnerSkillSize() {
        int ret = 0;
        for (int i = 0; i < 3; i++) {
            if (innerSkills[i] != null) {
                ret++;
            }
        }
        return ret;
    }

    public int getInnerSkillIdByPos(int rank) {
        if (innerSkills[rank] != null) {
            return innerSkills[rank].getSkillId();
        }
        return 0;
    }

    public int getHonourExp() {
        return honourExp;
    }

    public void setHonourExp(int exp) {
        this.honourExp = exp;
    }

    public int getHonorLevel() {
        if (honorLevel == 0) {
            honorLevel++;
        }
        return honorLevel;
    }

    public void setHonorLevel(int level) {
        this.honorLevel = level;
    }

    public void gainHonour(int amount) {
        honourExp += amount;
        client.announce(InfoPacket.showInfo("名譽" + amount + "已獲得。"));
        client.announce(CWvsContext.updateHonorExp(honourExp));
    }

    public void checkInnerSkill() {
        if (level >= 30 && innerSkills[0] == null) {
            changeInnerSkill(new InnerSkillValueHolder(70000015, 1, (byte) 1, (byte) 0));
        }
        if (level >= 50 && innerSkills[1] == null) {
            changeInnerSkill(new InnerSkillValueHolder(70000015, 3, (byte) 2, (byte) 0));
        }
        if (level >= 70 && innerSkills[2] == null) {
            changeInnerSkill(new InnerSkillValueHolder(70000015, 5, (byte) 3, (byte) 0));
        }
    }

    public void changeInnerSkill(InnerSkillValueHolder data) {
        changeInnerSkill(data.getSkillId(), data.getSkillLevel(), data.getPosition(), data.getRank());
    }

    public void changeInnerSkill(int skillId, int skillevel, byte position, byte rank) {
        Skill skill = SkillFactory.getSkill(skillId);
        if (skill == null || !skill.isInnerSkill() || skillevel <= 0 || position > 3 || position < 1) {
            return;
        }
        if (skillevel > skill.getMaxLevel()) {
            skillevel = skill.getMaxLevel();
        }
        InnerSkillValueHolder InnerSkill = new InnerSkillValueHolder(skillId, skillevel, position, rank);
        innerSkills[(position - 1)] = InnerSkill;
        client.announce(CField.updateInnerPotential(skillId, skillevel, position, rank));
        innerskill_changed = true;
    }

    public void azwanReward(final int map, final int portal) {
        client.announce(CField.UIPacket.openUIOption(0x45, 0));
        client.announce(CWvsContext.enableActions());
        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                changeMap(map, portal);
            }
        }, 5 * 1000);
    }

    public void setTouchedRune(int type) {
        setTempValue("TouchedRune", String.valueOf(type));
    }

    public int getTouchedRune() {
        return StringTool.parseInt(getTempValue("TouchedRune"));
    }

    public long getRuneTimeStamp() {
        return StringTool.parseLong(getValue("LastUseRune"));
    }

    public void setRuneTimeStamp(long time) {
        setValue("LastUseRune", String.valueOf(time));
    }

    public void setValue(String arg, String values) {
        CustomValues.remove(arg);
        if (values == null) {
            return;
        }
        CustomValues.put(arg, values);
    }

    public String getValue(String arg) {
        if (CustomValues.containsKey(arg)) {
            return CustomValues.get(arg);
        }
        return null;
    }

    public String getOneValue(String arg, String key) {
        if (!CustomValues.containsKey(arg)) {
            return null;
        }
        return StringTool.getOneValue(CustomValues.get(arg), key);
    }

    public void updateOneValue(final String arg, final String key, final String value) {
        if (key == null) {
            return;
        }
        String info = StringTool.updateOneValue(getValue(arg), key, value);
        if (info == null) {
            return;
        }
        if (info.isEmpty()) {
            setValue(arg, null);
        } else {
            setValue(arg, info);
        }
    }

    public void setTempValue(String arg, String values) {
        TempValues.remove(arg);
        if (values == null) {
            return;
        }
        TempValues.put(arg, values);
    }

    public String getTempValue(String arg) {
        if (TempValues.containsKey(arg)) {
            return TempValues.get(arg);
        }
        return null;
    }

    public String getOneTempValue(String arg, String key) {
        if (!TempValues.containsKey(arg)) {
            return null;
        }
        return StringTool.getOneValue(TempValues.get(arg), key);
    }

    public void updateOneTempValue(final String arg, final String key, final String value) {
        if (key == null) {
            return;
        }
        String info = StringTool.updateOneValue(getTempValue(arg), key, value);
        if (info == null) {
            return;
        }
        if (info.isEmpty()) {
            setTempValue(arg, null);
        } else {
            setTempValue(arg, info);
        }
    }

    public void setChatType(short chattype) {
        this.chattype = chattype;
    }

    public short getChatType() {
        return chattype;
    }

    public void gainItem(int code, int amount) {
        MapleInventoryManipulator.addById(client, code, (short) amount, null);
    }

    public void gainItem(int code, int amount, String gmLog) {
        MapleInventoryManipulator.addById(client, code, (short) amount, gmLog);
    }

    public final int[] getFriendShipPoints() {
        return friendshippoints;
    }

    public final void setFriendShipPoints(int joejoe, int hermoninny, int littledragon, int ika, int Wooden) {
        this.friendshippoints[0] = joejoe;
        this.friendshippoints[1] = hermoninny;
        this.friendshippoints[2] = littledragon;
        this.friendshippoints[3] = ika;
        this.friendshippoints[4] = Wooden;
    }

    public final int getFriendShipToAdd() {
        return friendshiptoadd;
    }

    public final void setFriendShipToAdd(int points) {
        this.friendshiptoadd = points;
    }

    public final void addFriendShipToAdd(int points) {
        this.friendshiptoadd += points;
    }

    public void makeNewAzwanShop() {
        /*
         * Azwan Etc Scrolls 60% - 30 conqueror coins Azwan Weapon Scrolls 60% - 50
         * conqueror coins 9 Conqueror's Coin - 1 emperor coin Circulator - rank * 1
         * emperor coins Azwan Scrolls 50% - 3 emperor coins Azwan Scrolls 40% - 5
         * emperor coins Azwan Scrolls 30% - 7 emperor coins Azwan Scrolls 20% - 10
         * emperor coins Emperor Armor Recipes - 70 emperor coins Emperor Accessory &
         * Weapon Recipes - 75 emperor coins Emperor Weapon - 300 emperor coins Emperor
         * Armor - 250 emperor coins Duke Accessory - 50 emperor coins Duke Accessory -
         * 50 emperor coins 10 Sunrise Dew - 3 conqueror coins 10 Simset Dew - 4
         * conqueror coins 10 Reindeer Milk - 2 conqueror coins 100% Scrolls - 3
         * conqueror coins
         */
        azwanShopList = new MapleShop(100000000 + getId(), 2182002);
        int itemid = GameConstants
                .getAzwanRecipes()[(int) Math.floor(Math.random() * GameConstants.getAzwanRecipes().length)];
        azwanShopList.addItem(
                new MapleShopItem((short) 1, (short) 1, itemid, 0, (short) 0, 4310038, 75, (byte) 0, 0, 0, 0, false));
        itemid = GameConstants
                .getAzwanScrolls()[(int) Math.floor(Math.random() * GameConstants.getAzwanScrolls().length)];
        azwanShopList.addItem(
                new MapleShopItem((short) 1, (short) 1, itemid, 0, (short) 0, 4310036, 15, (byte) 0, 0, 0, 0, false));
        itemid = (Integer) GameConstants
                .getUseItems()[(int) Math.floor(Math.random() * GameConstants.getUseItems().length)].getLeft();
        int price = (Integer) GameConstants
                .getUseItems()[(int) Math.floor(Math.random() * GameConstants.getUseItems().length)].getRight();
        azwanShopList.addItem(
                new MapleShopItem((short) 1, (short) 1, itemid, price, (short) 0, 0, 0, (byte) 0, 0, 0, 0, false));
        itemid = GameConstants
                .getCirculators()[(int) Math.floor(Math.random() * GameConstants.getCirculators().length)];
        price = InnerAbillity.getInstance().getCirculatorRank(itemid);
        if (price > 10) {
            price = 10;
        }
        azwanShopList.addItem(new MapleShopItem((short) 1, (short) 1, itemid, 0, (short) 0, 4310038, price, (byte) 0, 0,
                0, 0, false));
        client.announce(CField.getWhisper("Jean Pierre", client.getChannel(),
                "Psst! I got some new items in stock! Come take a look! Oh, but if your Honor Level increased, why not wait until you get a Circulator?"));
    }

    public MapleShop getAzwanShop() {
        return azwanShopList;
    }

    public void openAzwanShop() {
        if (azwanShopList == null) {
            MapleShopFactory.getInstance().getShop(2182002).sendShop(client);
        } else {
            getAzwanShop().sendShop(client);
        }
    }

    private static void addMedalString(final MapleCharacter c, final StringBuilder sb) {
        final Item medal = c.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -46);
        if (medal != null) { // Medal
            sb.append("<");
            if (medal.getItemId() == 1142257 && MapleJob.is冒險家(c.getJob())) {
                MapleQuestStatus stat = c.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
                if (stat != null && stat.getCustomData() != null) {
                    sb.append(stat.getCustomData());
                    sb.append("的繼承者");
                } else {
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                }
            } else {
                sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
            }
            sb.append("> ");
        }
    }

    public void updateReward() {
        List<MapleReward> rewards = new LinkedList<>();
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con
                    .prepareStatement("SELECT * FROM rewards WHERE `accid` = ? OR (`accid` IS NULL AND `cid` = ?)")) {
                ps.setInt(1, accountid);
                ps.setInt(2, id);
                try (ResultSet rs = ps.executeQuery()) {
                    // rewards.last();
                    // int size = rewards.getRow();
                    // rewards.first();
                    // client.announce(Reward.updateReward(rewards.getInt("id"), (byte) 9, rewards,
                    // size, 9));
                    while (rs.next()) {
                        rewards.add(new MapleReward(rs.getInt("id"), rs.getLong("start"), rs.getLong("end"),
                                rs.getInt("type"), rs.getInt("itemId"), rs.getInt("mp"), rs.getInt("meso"),
                                rs.getInt("exp"), rs.getString("desc")));
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("Unable to update rewards: " + e);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
        }
        client.announce(Reward.updateReward(0, (byte) 0x09, rewards, 0x09));
    }

    public MapleReward getReward(int id) {
        MapleReward reward = null;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM rewards WHERE `id` = ? AND (`accid` = ? OR (`accid` IS NULL AND `cid` = ?))")) {
                ps.setInt(1, id);
                ps.setInt(2, accountid);
                ps.setInt(3, this.id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        reward = new MapleReward(rs.getInt("id"), rs.getLong("start"), rs.getLong("end"),
                                rs.getInt("type"), rs.getInt("itemId"), rs.getInt("mp"), rs.getInt("meso"),
                                rs.getInt("exp"), rs.getString("desc"));
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("Unable to obtain reward information: " + e);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
        }
        return reward;
    }

    public void addReward(boolean acc, int type, int item, int mp, int meso, int exp, String desc) {
        addReward(acc, 0L, 0L, type, item, mp, meso, exp, desc);
    }

    public void addReward(boolean acc, long start, long end, int type, int item, int mp, int meso, int exp,
            String desc) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO rewards (`accid`, `cid`, `start`, `end`, `type`, `itemId`, `mp`, `meso`, `exp`, `desc`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                if (acc) {
                    ps.setInt(1, accountid);
                } else {
                    ps.setNull(1, Types.INTEGER);
                }
                ps.setInt(2, id);
                ps.setLong(3, start);
                ps.setLong(4, end);
                ps.setInt(5, type);
                ps.setInt(6, item);
                ps.setInt(7, mp);
                ps.setInt(8, meso);
                ps.setInt(9, exp);
                ps.setString(10, desc);
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("Unable to obtain reward: " + e);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
        }
    }

    public void deleteReward(int id) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM rewards WHERE `id` = ?")) {
                ps.setInt(1, id);
                ps.execute();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("Unable to delete reward: " + e);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
        }
        updateReward();
    }

    public void checkCustomReward(int level) {
        if (getClient() == null) {
            return;
        }
        int acLv = StringTool.parseInt(getClient().getValue("lvAchievement"));
        if (acLv < level) {
            getClient().setValue("lvAchievement", String.valueOf(level));
        }
        /*
         * 類型(type)： [1] 道具 [3] 楓葉點數 [4] 楓幣 [5] 經驗值
         */
        List<Integer> rewardItems = new LinkedList<>();
        List<Integer> rewardItemsAcc = new LinkedList<>();
        int rewardMaplePoint = 0;
        int rewardMeso = 0;
        int rewardExp = 0;
        // switch (level) {
        // case 10:
        // rewardItems.add(2450000); // 獵人的幸運
        // rewardItems.add(2022918); // 掉落機率 1.5倍優惠券
        // break;
        // case 20:
        // rewardItems.add(1032099); // 葡萄耳環
        // rewardItems.add(2022918); // 掉落機率 1.5倍優惠券
        // break;
        // case 30:
        // rewardMeso = 1000000;
        // rewardItemsAcc.add(1112659); // 冒險家的克勞特斯戒指
        // rewardItems.add(2450000); // 獵人的幸運
        // break;
        // case 50:
        // rewardItems.add(2022918); // 掉落機率 1.5倍優惠券
        // break;
        // case 70:
        // rewardItems.add(1003016); // 野狼的頭巾
        // rewardItems.add(2450000); // 獵人的幸運
        // rewardItems.add(2022918); // 掉落機率 1.5倍優惠券
        // break;
        // case 100:
        // rewardMeso = 50000000;
        // rewardItems.add(1132043); // 聯合鎖扣
        // rewardItems.add(2450000); // 獵人的幸運
        // rewardItems.add(2022918); // 掉落機率 1.5倍優惠券
        // break;
        // case 120:
        // rewardItems.add(1182007); // 彩虹胸章
        // rewardItems.add(2450000); // 獵人的幸運
        // break;
        // case 150:
        // rewardItems.add(1142349); // 傳說主角的勳章
        // break;
        // case 170:
        // rewardItems.add(1142295); // 簽到終結者的勳章
        // break;
        // case 200:
        // rewardMaplePoint = 30;
        // break;
        // case 250:
        // rewardMaplePoint = 300;
        // break;
        // }
        if (level > acLv) {
            for (int reward : rewardItemsAcc) {
                addReward(true, 1, reward, 0, 0, 0, "恭喜~你的角色" + name + "達到" + level + "級！");
            }
            if (rewardMaplePoint != 0) {
                addReward(true, 3, 0, rewardMaplePoint, 0, 0, "恭喜~你的角色" + name + "達到" + level + "級！");
            }
            if (rewardMeso != 0) {
                addReward(true, 4, 0, 0, rewardMeso, 0, "恭喜~你的角色" + name + "達到" + level + "級！");
            }
            if (rewardExp != 0) {
                addReward(true, 5, 0, 0, 0, rewardExp, "恭喜~你的角色" + name + "達到" + level + "級！");
            }
        }
        for (int reward : rewardItems) {
            addReward(false, 1, reward, 0, 0, 0, "恭喜你達到" + level + "級！");
        }
        updateReward();
    }

    public void newCharRewards() {
        List<Integer> rewards = new LinkedList<>();
        int rewardMeso = 200000000;
        // rewards.add(1142358); // 可愛的新手勳章
        // rewards.add(2022680); // 優質網咖的特權
        // rewards.add(2450031); // 新成員祝福
        for (int reward : rewards) {
            addReward(true, 4, 0, 0, rewardMeso, 0, "這是給新手贈送的特別獎勵\r\n以幫助你開始你的冒險之旅！");
        }
        updateReward();
    }

    public void removeBGLayers() {
        for (byte i = 0; i < 127; i++) {
            client.announce(CField.removeBGLayer(true, 0, i, 0));
            // duration 0 = forever map 0 = current map
        }
    }

    public void showBGLayers() {
        for (byte i = 0; i < 127; i++) {
            client.announce(CField.removeBGLayer(false, 0, i, 0));
            // duration 0 = forever map 0 = current map
        }
    }

    public static final int CUSTOM_BG = 61000000;

    public int getCustomBGState() {
        return getIntNoRecord(CUSTOM_BG);
    }

    public void toggleCustomBGState() {
        getQuestNAdd(MapleQuest.getInstance(CUSTOM_BG)).setCustomData(String.valueOf(getCustomBGState() == 1 ? 0 : 1));
        for (byte i = 0; i < 127; i++) {
            client.announce(CField.removeBGLayer((getCustomBGState() == 1), 0, i, 0));
            // duration 0 = forever map 0 = current map
        }
    }

    public static void addLinkSkill(int cid, int skill) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, cid);
                if (SkillConstants.isApplicableSkill(skill)) { // do not save additional skills
                    ps.setInt(2, skill);
                    ps.setInt(3, 1);
                    ps.setByte(4, (byte) 1);
                    ps.setLong(5, -1);
                    ps.execute();
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            System.err.println("Failed adding link skill: " + ex);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
        }
    }

    public int getWheelItem() {
        return wheelItem;
    }

    public void setWheelItem(int wheelItem) {
        this.wheelItem = wheelItem;
    }

    public static void removePartTime(int cid) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM parttime where cid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            System.err.println("Failed to remove part time job: " + ex);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
        }
    }

    public static void addPartTime(PartTimeJob partTime) {
        if (partTime.getCharacterId() < 1) {
            return;
        }
        addPartTime(partTime.getCharacterId(), partTime.getJob(), partTime.getTime(), partTime.getReward());
    }

    public static void addPartTime(int cid, byte job, long time, int reward) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con
                    .prepareStatement("INSERT INTO parttime (cid, job, time, reward) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, cid);
                ps.setByte(2, job);
                ps.setLong(3, time);
                ps.setInt(4, reward);
                ps.execute();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            System.err.println("Failed to add part time job: " + ex);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
        }
    }

    public static PartTimeJob getPartTime(int cid) {
        PartTimeJob partTime = new PartTimeJob(cid);
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM parttime WHERE cid = ?")) {
                ps.setInt(1, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        partTime.setJob(rs.getByte("job"));
                        partTime.setTime(rs.getLong("time"));
                        partTime.setReward(rs.getInt("reward"));
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (Exception ex) {
            System.err.println("Failed to retrieve part time job: " + ex);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
        }
        return partTime;
    }

    public void handle元素衝擊(int ski) {
        if (getJob() > 122 || getJob() < 120 || ski != 1201011 & ski != 1201012 & ski != 1211008 & ski != 1221004) {
            return;
        }
        if (lastskill == ski || lastskill == 0) {
            lastskill = ski;
            return;
        }
        int skill = getSkillLevel(1220010) > 0 ? 1220010 : 1200014;
        Skill useskill = SkillFactory.getSkill(skill);
        int skilllevel = getSkillLevel(skill);
        MapleStatEffect infoEffect = useskill.getEffect(skilllevel);
        infoEffect.applyBuffEffect(this, -1);
        lastskill = ski;
    }

    public void handleKaiserCombo() {
        if (attackCombo < 1000) {
            attackCombo += 4;
        }
        SkillFactory.getSkill(61111008).getEffect(1).applyKaiser_Combo(this, attackCombo);
    }

    public void resetKaiserCombo() {
        attackCombo = 0;
        SkillFactory.getSkill(61111008).getEffect(1).applyKaiser_Combo(this, attackCombo);
    }

    public void setLuminousMode(final int skillid) {
        if (skillid >= 27100000 && skillid < 27121300 && (luminousState != 20040219 && luminousState != 20040220)) {
            System.out.println("Value:" + ((getLightGauge() - 277) <= 0) + ":" + (runningLight == 1) + ":::"
                    + ((getDarkGauge() + 416) >= 9999) + ":" + (runningDark == 1));
            int chage_skill = GameConstants.getLuminousSkillMode(skillid);
            if ((getLightGauge() <= 0 && runningLight == 1) || (getDarkGauge() >= 9999) && runningDark == 1) {
                System.out.println("執行平衡狀態");
                changeLuminousMode(runningLight == 1 ? 20040217 : 20040216);
                client.announce(CWvsContext.enableActions());
            } else if (skillid >= 27100000 && skillid < 27120400 && getLuminousState() < 20040218) {
                if ((getLightGauge() <= 0 && runningLight == 1)) {
                    chage_skill = 20040217;
                } else if ((getDarkGauge() >= 9999) && runningDark == 1) {
                    chage_skill = 20040216;
                }
                setLuminousState(chage_skill);
                MapleStatEffect effect = SkillFactory.getSkill(chage_skill).getEffect(getTotalSkillLevel(chage_skill));
                Map<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.Larkness, 1);
                client.announce(CWvsContext.BuffPacket.giveBuff(chage_skill, 0, stat, effect, this));
                effect.applyTo(this);
            }
        }
    }

    public void changeLuminousMode(final int skillid) {
        final boolean equilibrium = skillid == 20040220 || skillid == 20040219;
        final boolean eclipse = skillid == 20040217;
        final boolean sunfire = skillid == 20040216;
        final MapleCharacter chr = this;
        int changeSkill = 0;
        if (equilibrium || (!eclipse && !sunfire)) {
            return; // impossible
        }
        dispelBuff(skillid);
        if (runningLight == 1) {
            changeSkill = 20040220;
        } else {
            changeSkill = 20040219;
        }
        luminousState = changeSkill;
        MapleStatEffect effect = SkillFactory.getSkill(changeSkill).getEffect(getTotalSkillLevel(changeSkill));
        Map<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
        stat.put(MapleBuffStat.Larkness, 2);
        client.announce(CWvsContext.BuffPacket.giveBuff(changeSkill, 0, stat, effect, this));
        effect.applyTo(this);
        equipChanged();
        int skill_time = 10000;
        if (getTotalSkillLevel(27120008) > 0) {
            MapleStatEffect effect2 = SkillFactory.getSkill(27120008).getEffect(getTotalSkillLevel(27120008));
            skill_time += effect2.getDuration();
        }
        WorldTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                dispelBuff(luminousState);
                if (runningLight == 1) {
                    luminousState = 20040216;
                } else {
                    luminousState = 20040217;
                }
                MapleStatEffect effect = SkillFactory.getSkill(luminousState)
                        .getEffect(getTotalSkillLevel(luminousState));
                Map<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.Larkness, 1);
                client.announce(CWvsContext.BuffPacket.giveBuff(luminousState, 0, stat, effect, chr));
                effect.applyTo(chr);
            }
        }, skill_time);
    }

    public void handleLuminous(int skillid) {
        if (luminousState == 20040219 || luminousState == 20040220) { // 平衡
            return;
        }
        // System.out.println("執行handleLuminous:" + skillid);
        // gauge = 0 Dark, gauge = 9999 Light
        // 預設值 gauge = 5000, type = 3
        // 黑預設值 gauge = 1, type = 1
        // 每次使用增加 76, 過千就 416
        // 第一次滿值 gauge = 9999, type = 2
        // 每次使用減少 227
        int[] lightSkills = new int[]{27001100, 27101100, 27101101, 27111100, 27111101, 27121100};
        int[] darkSkills = new int[]{27001201, 27101202, 27111202, 27121201, 27121202, 27120211};
        boolean found = false;
        int runningSolt = 0;
        int type = 1;
        // 初始化DarkSolt 跟 LightSolt的值
        if (runningLightSlot == 0 && runningDarkSlot == 0) {
            runningLightSlot = 9999;
            runningDarkSlot = 1;
        }
        // 判斷執行哪個狀態
        for (int light : lightSkills) {
            if (skillid == light && runningDark == 0) {
                runningLight = 1;
            }
        }
        for (int dark : darkSkills) {
            if (skillid == dark && runningLight == 0) {
                runningDark = 1;
            }
        }
        // System.out.println("執行handleLuminous:黑:" + runningDark + " 光:" +
        // runningLight);
        if (runningLight == 1) {
            for (int light : lightSkills) {
                if (skillid == light) {
                    runningLightSlot -= 277;
                    if (runningLightSlot <= 0) {
                        setLuminousMode(skillid);
                        runningLightSlot = 9999;
                        runningLight = 0;
                        runningDark = 1;
                        runningSolt = 1;
                    } else {
                        runningSolt = runningLightSlot;
                        setLuminousMode(skillid);
                    }
                    type = 1;
                    found = true;
                }
            }
        }
        if (runningDark == 1) {
            for (int dark : darkSkills) {
                if (skillid == dark) {
                    if (runningDarkSlot >= 1000) {
                        runningDarkSlot += 416;
                    } else {
                        runningDarkSlot += 76;
                    }
                    if (runningDarkSlot > 10000) {
                        setLuminousMode(skillid);
                        runningDarkSlot = 1;
                        runningLight = 1;
                        runningDark = 0;
                        runningSolt = 9999;
                    } else {
                        runningSolt = runningDarkSlot;
                        setLuminousMode(skillid);
                    }
                    type = 2;
                    found = true;
                }
            }
        }
        if (!found) {
            return;
        }
        System.out.println(runningLightSlot + "::" + runningDarkSlot + "::" + runningSolt);
        client.announce(LuminousPacket.updateLuminousGauge(runningSolt, type));
    }

    public void resetRunningDarks() {
        this.runningDark = 0;
        this.runningDarkSlot = 0;
        this.runningLight = 0;
        this.runningLightSlot = 0;
    }

    public int getLightGauge() {
        return runningLightSlot;
    }

    public int getDarkGauge() {
        return runningDarkSlot;
    }

    public int getLuminousState() {
        return luminousState;
    }

    public void setLuminousState(int luminousState) {
        this.luminousState = luminousState;
    }

    public void setCardStack(byte amount) {
        this.cardStack = amount;
    }

    public byte getCardStack() {
        return this.cardStack;
    }

    public void applyBlackBlessingBuff(int combos) {
        if ((combos == -1) && (this.runningBless == 0)) {
            // combos = 0;
            return;
        }
        Skill skill = SkillFactory.getSkill(27100003);
        int lvl = getTotalSkillLevel(27100003);
        if (lvl > 0) {
            runningBless = ((byte) (runningBless + combos));
            if (runningBless > 3) {
                runningBless = 3;
            }
            if (runningBless == 0) {
                if (getBuffedValue(MapleBuffStat.BlessOfDarkness) != null) {
                    cancelBuffStats(MapleBuffStat.BlessOfDarkness);
                }
            } else {
                skill.getEffect(lvl).applyBlackBlessingBuff(this, runningBless);
            }
        }
    }

    public void doRecoveryXenonSurplus() {
        if (!MapleJob.is傑諾(getJob())) {
            lastRecoveryXenonSurplusTime = 0;
            return;
        }
        prepareRecoveryXenonSurplus();
        gainXenonSurplus(1);
    }

    public boolean canRecoverXenonSurplus(long now) {
        if (!MapleJob.is傑諾(getJob())) {
            return false;
        }
        if (lastRecoveryXenonSurplusTime <= 0) {
            prepareRecoveryXenonSurplus();
        }
        return lastRecoveryXenonSurplusTime > 0 && lastRecoveryXenonSurplusTime + 4000 < now;
    }

    private void prepareRecoveryXenonSurplus() {
        lastRecoveryXenonSurplusTime = System.currentTimeMillis();
    }

    public int getXenonSurplusCountByJob() {
        if (getJob() == MapleJob.傑諾1轉.getId()) {
            return 5;
        } else if (getJob() == MapleJob.傑諾2轉.getId()) {
            return 10;
        } else if (getJob() == MapleJob.傑諾3轉.getId()) {
            return 15;
        } else if (getJob() == MapleJob.傑諾4轉.getId()) {
            return 20;
        } else {
            return 0;
        }
    }

    public int getXenonSurplus() {
        if (getBuffedValue(MapleBuffStat.SurplusSupply) == null) {
            return 0;
        }
        return getBuffedValue(MapleBuffStat.SurplusSupply);
    }

    public void setXenonSurplus(int amount) {
        Integer surplusSupply = getBuffedValue(MapleBuffStat.SurplusSupply);
        if (surplusSupply == null) {
            Skill skill = SkillFactory.getSkill(30020232);
            int skilllevel = getTotalSkillLevel(skill);
            if (skill == null || skilllevel <= 0) {
                return;
            }
            MapleStatEffect effect = skill.getEffect(skilllevel);
            if (effect == null) {
                return;
            }
            effect.applySurplusSupply(this);
            stats.recalcLocalStats(this);
        } else {
            setBuffedValue(MapleBuffStat.SurplusSupply, Math.min(Math.max(amount, 0), getXenonSurplusCountByJob()));
            if (getXenonSurplus() != surplusSupply) {
                stats.recalcLocalStats(this);
                client.announce(XenonPacket.giveXenonSupply(getXenonSurplus()));
            }
        }
    }

    public void gainXenonSurplus(int amount) {
        setXenonSurplus(getXenonSurplus() + amount);
    }

    public void handleAegisSystem(int objectId) { // 處理神盾系統
        Skill skill = SkillFactory.getSkill(36111004);
        long nowtime = System.currentTimeMillis();
        if (getTotalSkillLevel(skill) > 0 && nowtime - getStiffTime() > 1500) {
            this.setStiffTime(nowtime);
            MapleStatEffect effect = skill.getEffect(getTotalSkillLevel(skill));
            if (effect != null && effect.makeChanceResult()) {
                getMap().broadcastMessage(this,
                        SkillPacket.AccurateRocket(this, skill.getId(), null, objectId, 3, 3, true), true);
            }
        }
    }

    public void handleAccurateRocket(List<Integer> Oid, int skillId) { // 處理追縱火箭
        Skill skill = SkillFactory.getSkill(skillId);
        if (getTotalSkillLevel(skill) > 0) {
            MapleStatEffect effect = skill.getEffect(getTotalSkillLevel(skill));
            if (effect != null) {
                getMap().broadcastMessage(this, SkillPacket.AccurateRocket(this, skillId, Oid, 0, 3, 4, false), true);
            }
        }
    }

    public void handleMesosbomb(int skillId, int delay) { // 處理楓幣炸彈
        MapleMapItem mapitem;
        List<MapleMapObject> mesos = new ArrayList<>();
        List<MapleMapObject> items = this.getMap().getItemsInRange(old, 70000.0D);// 地圖上的道具
        List<MapleMapObject> mons = this.getMap().getMonstersInRange(old, 60000.0D);// 地圖上的怪物
        if (mons.size() > 0) {
            for (MapleMapObject obj : items) {
                mapitem = (MapleMapItem) obj;
                if (mapitem.getMeso() > 0 && mapitem.getOwner() == this.getId()) {
                    mesos.add(mapitem);
                    this.getMap().broadcastMessage(CField.removeItemFromMap(mapitem.getObjectId(), 0, this.getId()),
                            mapitem.getPosition());
                    this.getMap().removeMapObject(obj); // 標記
                }
            }
            if (mesos.size() > 0) {
                this.getMap().broadcastMessage(SkillPacket.getMesosBomb(this, skillId, mesos, mons, delay));
            }
        }
        this.client.announce(CWvsContext.enableActions());
    }

    public void updateQuiverKartrige(int mode, boolean isfirst) { // 更新魔幻箭筒
        Skill skill2 = SkillFactory.getSkill(3121016);
        MapleStatEffect effect = skill2.getEffect(getTotalSkillLevel(skill2));
        if (isfirst) {
            MapleStatEffect effec = new MapleStatEffect();
            effec.applyQuiverKartrige(this, true);
        }
        if (this.getTotalSkillLevel(3121016) > 0) {
            if (this.getBuffedValue(MapleBuffStat.QuiverCatridge) == 3) {
                this.qcount = effect.getZ();
            } else {
                this.qcount = effect.getY();
            }
        } else {
            this.qcount = 10;
        }
        this.getClient().announce(SkillPacket.showQuiverKartrigeEffect(this.getId(), mode, qcount, false));
        this.getMap().broadcastMessage(this, SkillPacket.showQuiverKartrigeEffect(this.getId(), mode, qcount, true),
                false);
        this.getMap().broadcastMessage(this, SkillPacket.showQuiverKartrigeEffect2(this.getId()), false);
    }

    public void handleQuiverKartrige(int oid) {
        int mode = getmod();
        Skill skill1 = SkillFactory.getSkill(3101009);
        Skill skill2 = SkillFactory.getSkill(3121016);
        int chance = 0;
        MapleStatEffect effect2 = skill2.getEffect(getTotalSkillLevel(skill2));
        MapleStatEffect effect1 = skill2.getEffect(getTotalSkillLevel(skill1));
        int skillid = effect2 == null ? 3120017 : 3120017;
        switch (mode) {
            case 1:
                chance = effect2 != null ? effect2.getW() : effect1.getW();
                break;
            case 2:
                // <--還沒寫
                break;
            case 3:
                chance = effect2 != null ? effect2.getU() : effect1.getU();
                break;
        }
        if (Randomizer.isSuccess(chance)) { // 效果
            switch (mode) {
                case 1:
                    addHP(this.getStat().getMaxHp() * (effect2 != null ? effect2.getW() : effect1.getW()) / 100);
                    break;
                case 3:
                    getMap().broadcastMessage(SkillPacket.showQuiverKartrigeAction(getId(), skillid, oid));
                    break;
            }
        }

        if (getBuffedValue(MapleBuffStat.AdvancedQuiver) == null) { // 如果沒有無限箭筒的Buff
            if (qcount + 1 > (effect2 != null ? (mode == 3 ? effect2.getZ() : effect2.getY())
                    : (mode == 3 ? effect1.getZ() : effect1.getY()))) {
                qcount = 0;
                effect1.applyQuiverKartrige(this, true);
            } else {
                qcount++;
                effect1.applyQuiverKartrige(this, false);
            }
        }
    }

    public void handleAssassinStack(MapleMonster mob, int visProjectile) {
        Skill skill_2 = SkillFactory.getSkill(4100011);
        Skill skill_4 = SkillFactory.getSkill(4120018);
        MapleStatEffect effect;
        boolean isAssassin;
        if (getTotalSkillLevel(skill_4) > 0) {
            isAssassin = false;
            effect = skill_4.getEffect(getTotalSkillLevel(skill_4));
        } else if (getTotalSkillLevel(skill_2) > 0) {
            isAssassin = true;
            effect = skill_2.getEffect(getTotalSkillLevel(skill_2));
        } else {
            return;
        }
        if ((effect != null) && (effect.makeChanceResult()) && (mob != null)) {
            int mobCount = effect.getMobCount();
            Rectangle bounds = effect.calculateBoundingBox(mob.getTruePosition(), mob.isFacingLeft());
            List<MapleMapObject> affected = this.map.getMapObjectsInRect(bounds,
                    Collections.singletonList(MapleMapObjectType.MONSTER));
            List<Integer> moboids = new ArrayList<>();
            for (MapleMapObject mo : affected) {
                if ((moboids.size() < mobCount) && (mo.getObjectId() != mob.getObjectId())) {
                    moboids.add(mo.getObjectId());
                }
            }
            mob.setmark(false);
            this.forceCounter += 1;
            getMap().broadcastMessage(this, SkillPacket.gainAssassinStack(getId(), mob.getObjectId(), this.forceCounter,
                    isAssassin, moboids, visProjectile, mob.getTruePosition()), true);
            this.forceCounter += moboids.size();
        }
    }

    public short getExceedOverload() {
        if (getBuffedValue(MapleBuffStat.OverloadCount) == null) {
            return 0;
        }
        return getBuffedValue(MapleBuffStat.OverloadCount).shortValue();
    }

    public boolean handleExceedAttack(int skillId) {
        /**
         * 超越技能說明：
         *
         * 前綴有 超越： 的攻擊技能，打一下會增加 OverloadCount 最大為 20 ，為"超越"技能的效果
         *
         * 攻擊技能附帶著 Exceed
         */
        TemporaryStatBase exceed = this.getTempstats().getStatOption(MapleBuffStat.Exceed);
        TemporaryStatBase overload = this.getTempstats().getStatOption(MapleBuffStat.ExceedOverload);

        int nSkillId = skillId % 100;
        if (nSkillId != 0) {
            int nJob = skillId / 10000;
            int nStartNum;
            if (nJob == MapleJob.惡魔復仇者1轉.getId()) {
                nStartNum = 4;
            } else if (nJob == MapleJob.惡魔復仇者2轉.getId()) {
                nStartNum = 7;
            } else if (nJob == MapleJob.惡魔復仇者3轉.getId()) {
                nStartNum = 7;
            } else if (nJob == MapleJob.惡魔復仇者4轉.getId()) {
                nStartNum = 9;
            } else {
                if (isShowErr()) {
                    showInfo("超越攻擊", true, "技能處理異常");
                }
                return false;
            }
            int nLastJob = exceed.getReason() / 10000;
            int nLastStartNum;
            if (nLastJob == MapleJob.惡魔復仇者1轉.getId()) {
                nLastStartNum = 4;
            } else if (nLastJob == MapleJob.惡魔復仇者2轉.getId()) {
                nLastStartNum = 7;
            } else if (nLastJob == MapleJob.惡魔復仇者3轉.getId()) {
                nLastStartNum = 7;
            } else if (nLastJob == MapleJob.惡魔復仇者4轉.getId()) {
                nLastStartNum = 9;
            } else {
                if (isShowErr()) {
                    showInfo("超越攻擊", true, "技能處理異常");
                }
                return false;
            }
            int nLastSkillId = exceed.getReason() % 100;
            int nSkillLastStep = nLastSkillId == 0 ? 1 : (nLastSkillId - nLastStartNum + 2);
            int nSkillNowStep = nSkillId == 0 ? 1 : (nSkillId - nStartNum + 2);
            if (nSkillLastStep < nSkillNowStep - 1) {
                cancelEffect(this.getStatForBuff(MapleBuffStat.Exceed), true, -1);
                if (isShowErr()) {
                    showInfo("超越攻擊", true, "階段錯誤:上階段[" + nSkillLastStep + "]使用階段[" + nSkillNowStep + "]");
                }
                return false;
            }
        }

        if (exceed.getReason() != GameConstants.getLinkedAttackSkill(skillId)) {
            cancelEffect(this.getStatForBuff(MapleBuffStat.Exceed), true, -1);
        }
        // // 超越BUFF處理
        Skill skill = SkillFactory.getSkill(30010230); // 超越
        int skilllevel = getTotalSkillLevel(skill);
        if (skilllevel <= 0 || skill == null) {
            if (isShowErr()) {
                showInfo("超越狀態", true, "技能等級為零或沒有找到這個技能");
            }
            return false;
        }
        MapleStatEffect effect = skill.getEffect(skilllevel);

        if (effect == null) {
            if (isShowErr()) {
                showInfo("超越狀態", true, "技能Effect為NULL");
            }
            return false;
        }
        Integer exceedOverload = this.getTempstats().getStatOption(MapleBuffStat.OverloadCount).nOption;
        int exceedMax = 20;
        skill = SkillFactory.getSkill(31220044); // 超越—超載解放
        skilllevel = getTotalSkillLevel(skill);
        if (skilllevel > 0) {
            exceedMax -= 2;
        }
        if (++exceedOverload <= exceedMax) {
            this.getTempstats().getStatOption(MapleBuffStat.OverloadCount).nOption = exceedOverload;
            client.announce(AvengerPacket.giveExceed(exceedOverload.shortValue()));
        }
        this.getTempstats().getStatOption(MapleBuffStat.Exceed).rOption = GameConstants.getLinkedAttackSkill(skillId);

        int exceedVal = this.getTempstats().getStatOption(MapleBuffStat.Exceed).nOption;
        if (exceedVal < 4) {
            this.getTempstats().getStatOption(MapleBuffStat.Exceed).nOption++;
        }
        long curr = System.currentTimeMillis();
        if (exceed.tLastUpdated + 15000L < curr) {
            exceed.reset();
        }
        return true;
    }

    public MapleCoreAura getCoreAura() {
        return coreAura;
    }

    public void changeWarriorStance(final int skillid) {
        switch (skillid) {
            case 11101022: {
                // 沉月
                // dispelBuff(11111022);
                List<MapleBuffStat> statups = new LinkedList<>();
                statups.add(MapleBuffStat.IndieCr);
                statups.add(MapleBuffStat.BuckShot);
                statups.add(MapleBuffStat.PoseType);
                // client.announce(BuffPacket.cancelBuff(statups));*/
                // client.announce(JobPacket.DawnWarriorPacket.giveMoonfallStance(getSkillLevel(skillid)));
                SkillFactory.getSkill(skillid).getEffect(1).applyTo(this);
                break;
            }
            case 11111022: {
                // 旭日
                // dispelBuff(11101022);
                System.out.println("Start of buff");
                List<MapleBuffStat> statups = new LinkedList<>();
                statups.add(MapleBuffStat.IndieBooster);
                System.out.println("ATT Speed");
                statups.add(MapleBuffStat.IndieDamR);
                System.out.println("DMG Perc");
                statups.add(MapleBuffStat.PoseType);
                System.out.println("WAR Stance");
                // client.announce(BuffPacket.cancelBuff(statups));*/
                // client.announce(JobPacket.DawnWarriorPacket.giveSunriseStance(getSkillLevel(skillid)));
                SkillFactory.getSkill(skillid).getEffect(1).applyTo(this);
                break;
            }
            // 雙重力量
            // equinox
            case 11121005:
                break;
            case 11121011:
                // 雙重力量（沉月）
                dispelBuff(11101022);
                // client.announce(DawnWarriorPacket.giveEquinox_Moon(getSkillLevel(skillid),
                // Integer.MAX_VALUE));
                SkillFactory.getSkill(skillid).getEffect(1).applyTo(this);
                break;
            case 11121012:
                // 雙重力量（旭日）
                dispelBuff(11101022);
                // client.announce(DawnWarriorPacket.giveEquinox_Sun(getSkillLevel(skillid),
                // Integer.MAX_VALUE));
                SkillFactory.getSkill(skillid).getEffect(1).applyTo(this);
                break;
            default:
                break;
        }
    }

    public List<MaplePotionPot> getPotionPots() {
        return potionPots;
    }

    public void giveMiracleBlessing() {
        for (MapleCharacter chr : getMap().getCharactersThreadsafe()) {
            MapleItemInformationProvider.getInstance().getItemEffect(2023055).applyTo(chr);
        }
        getMap().broadcastMessage(CField.startMapEffect(
                name + " has received Double Miracle Time's Mysterious Blessing. Congratulations!", 2023055, true));
        World.Broadcast.broadcastMessage(CWvsContext.broadcastMsg(0x19, 0,
                name + " has received [Double Miracle Time's Miraculous Blessing]. Congratulations!", false));
    }

    public void checkForceShield() {
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        Equip equip;
        boolean potential = false;
        switch (job) {
            case 508:// 蒼龍俠客1轉
                equip = (Equip) li.getEquipById(1352820);
                break;
            case 572:// 蒼龍俠客4轉
                potential = true;
            case 570:// 蒼龍俠客2轉
            case 571:// 蒼龍俠客3轉
                equip = (Equip) li.getEquipById(1352821 + job % 10);
                break;
            case 2500:// 隱月1轉
                equip = (Equip) li.getEquipById(1353100);
                break;
            case 3001:// 惡魔
                equip = (Equip) li.getEquipById(1099001);
                break;
            case 3100:// 惡魔殺手1轉
                equip = (Equip) li.getEquipById(1099000);
                break;
            case 3112:// 惡魔殺手4轉
                potential = true;
            case 3110:// 惡魔殺手2轉
            case 3111:// 惡魔殺手3轉
                equip = (Equip) li.getEquipById(1099001 + job % 10 + ((job % 100) / 10));
                break;
            case 3122:// 惡魔復仇者4轉
                potential = true;
            case 3101:// 惡魔復仇者1轉
            case 3120:// 惡魔復仇者2轉
            case 3121:// 惡魔復仇者3轉
                equip = (Equip) li.getEquipById(1099005 + job % 10 + ((job % 100) / 10));
                break;
            case 5112:// 米哈逸4轉
                potential = true;
            case 5100:// 米哈逸1轉
            case 5110:// 米哈逸2轉
            case 5111:// 米哈逸3轉
                equip = (Equip) li.getEquipById(1098000 + job % 10 + ((job % 100) / 10));
                break;
            case 6001:// 天使破壞者
                equip = (Equip) li.getEquipById(1352600);
                break;
            case 6512:// 天使破壞者4轉
                potential = true;
            case 6500:// 天使破壞者1轉
            case 6510:// 天使破壞者2轉
            case 6511:// 天使破壞者3轉
                equip = (Equip) li.getEquipById(1352601 + job % 10 + ((job % 100) / 10));
                break;
            case 6000:// 凱撒
                equip = (Equip) li.getEquipById(1352500);
                break;
            case 6112:// 凱撒4轉
                potential = true;
            case 6100:// 凱撒1轉
            case 6110:// 凱撒2轉
            case 6111:// 凱撒3轉
                equip = (Equip) li.getEquipById(1352500 + job % 10 + ((job % 100) / 10));
                break;
            case 3002:// 傑諾
                equip = (Equip) li.getEquipById(1353000);
                break;
            case 3612:// 傑諾4轉
                potential = true;
            case 3600:// 傑諾1轉
            case 3610:// 傑諾2轉
            case 3611:// 傑諾3轉
                equip = (Equip) li.getEquipById(1353001 + job % 10 + ((job % 100) / 10));
                break;
            default:
                equip = null;
        }
        if (equip != null) {
            if (potential && equip.getState(false) == 0) {
                equip.resetPotential(false);
            }
            equip.setPosition((short) -10);
            equip.setQuantity((short) 1);
            equip.setGMLog("轉職更變道具獲得, 時間 " + FileoutputUtil.CurrentReadable_Time());
            forceReAddItem_NoUpdate(equip);
            client.announce(InventoryPacket.updateEquippedItem(equip));
            equipChanged();
        }
    }

    public void checkZeroWeapon() {
        if (level < 100) {
            return;
        }
        int lazuli = getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11).getItemId();
        int lapis = getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10).getItemId();
        if (lazuli == getZeroWeapon(false) && lapis == getZeroWeapon(true)) {
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (int i = 0; i < 2; i++) {
            int itemId = i == 0 ? getZeroWeapon(false) : getZeroWeapon(true);
            Equip equip = (Equip) ii.getEquipById(itemId);
            equip.setPosition((short) (i == 0 ? -11 : -10));
            equip.setQuantity((short) 1);
            equip.setGMLog("神之子升級武器, 時間:" + FileoutputUtil.CurrentReadable_Time());
            forceReAddItem_NoUpdate(equip);
            client.announce(InventoryPacket.updateEquippedItem(equip));
        }
        equipChanged();
    }

    public int getZeroWeapon(boolean lapis) {
        if (level < 100) {
            return lapis ? 1562000 : 1572000;
        }
        int weapon = lapis ? 1562001 : 1572001;
        if (level >= 100 && level < 160) {
            weapon += (level % 100) / 10;
        } else if (level >= 160 && level < 170) {
            weapon += 5;
        } else if (level >= 170) {
            weapon += 6;
        }
        return weapon;
    }

    public void zeroChange(boolean beta) {
        getMap().broadcastMessage(this, CField.updateCharLook(this, beta), getPosition());
    }

    public void checkZeroTranscendent() {
        int skill = -1;
        switch (level / 10) {
            case 10:
                skill = 100000267;
                break;
            case 11:
                skill = 100001261;
                break;
            case 12:
                skill = 100001274;
                break;
            case 14:
                skill = 100001272;
                break;
            case 17:
                skill = 100001283;
                break;
            case 20:
                skill = 100001005;
                break;
        }
        if (skill == -1) {
            return;
        }
        Skill skil = SkillFactory.getSkill(skill);
        if (skil != null) {
            changeSkillLevel_Skip(skil, 1, (byte) 1);
        }
    }

    public boolean hasEntered(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEntered(String script, int mapId) {
        if (entered.containsKey(mapId)) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public void enteredScript(String script, int mapid) {
        if (!entered.containsKey(mapid)) {
            entered.put(mapid, script);
        }
    }

    public boolean isHiddenChatCanSee() {
        return hiddenChatCanSee;
    }

    public SecondaryStat getTempstats() {
        return this.secondaryStat;
    }

    public void setHiddenChatCanSee(boolean can) {
        hiddenChatCanSee = can;
    }

    public static String getCharacterNameById(int id) {
        String name = null;
        MapleCharacter chr = getOnlineCharacterById(id);
        if (chr != null) {
            return chr.getName();
        }
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("select name from characters where id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        name = rs.getString("name");
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (Exception ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
        }
        return name;
    }

    public static int getCharacterIdByName(String name) {
        int id = -1;
        MapleCharacter chr = getOnlineCharacterByName(name);
        if (chr != null) {
            return chr.getId();
        }
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("select id from characters where name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (Exception ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
        }
        return id;
    }

    public static MapleCharacter getOnlineCharacterById(int cid) {
        MapleCharacter chr = null;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            chr = cs.getPlayerStorage().getCharacterById(cid);
            if (chr != null) {
                break;
            }
        }
        return chr;
    }

    public static MapleCharacter getOnlineCharacterByName(String name) {
        MapleCharacter chr = null;
        if (World.Find.findChannel(name) >= 1) {
            chr = ChannelServer.getInstance(World.Find.findChannel(name)).getPlayerStorage().getCharacterByName(name);
            if (chr != null) {
                return chr;
            }
        }

        return null;
    }

    public static MapleCharacter getCharacterById(int cid) {
        MapleCharacter chr = getOnlineCharacterById(cid);
        if (chr != null) {
            return chr;
        }
        String name = getCharacterNameById(cid);
        return name == null ? null
                : MapleCharacter.loadCharFromDB(cid, new MapleClient(null, null, new tools.MockIOSession()), true);
    }

    public static MapleCharacter getCharacterByName(String name) {
        MapleCharacter chr = getOnlineCharacterByName(name);
        if (chr != null) {
            return chr;
        }
        int cid = getCharacterIdByName(name);
        return cid == -1 ? null
                : MapleCharacter.loadCharFromDB(cid, new MapleClient(null, null, new tools.MockIOSession()), true);
    }

    public void monsterMultiKill() {
        if (comboKillExps.size() > 2) {
            int size = comboKillExps.size();
            double x;
            switch (size) {
                case 3:
                    x = 3.0D;
                    break;
                case 4:
                    x = 8.0D;
                    break;
                case 5:
                    x = 15.25D;
                    break;
                case 6:
                    x = 19.8D;
                    break;
                case 7:
                    x = 25.2D;
                    break;
                case 8:
                    x = 31.2D;
                    break;
                case 9:
                    x = 37.8D;
                    break;
                case 10:
                    x = 45D;
                    break;
                case 11:
                    x = 49.5D;
                    break;
                case 12:
                    x = 54D;
                    break;
                case 13:
                    x = 58.5D;
                    break;
                case 14:
                    x = 63D;
                    break;
                default:
                    if (size >= 15) {
                        x = 67.5D;
                    } else {
                        x = 0.0D;
                    }
                    break;
            }
            boolean boss = false;
            long multiKillExp = 0;
            for (Pair<Integer, Boolean> exps : comboKillExps) {
                multiKillExp += exps.left;
                if (exps.right) {
                    boss = true;
                }
            }
            if (boss) {
                x /= (double) size;
            }
            multiKillExp /= comboKillExps.size();
            multiKillExp *= x / 100.0D;
            gainExp((int) multiKillExp, false, false, false);

            client.announce(InfoPacket.OnStylishKillMessage(0, multiKillExp, Math.min(comboKillExps.size(), 10)));
        }
        comboKillExps.clear();
    }

    public int getFreeChronosphere() {
        return 7 - getDaysPQLog("免費強化任意門", 7);
    }

    public int getChronosphere() {
        String num = getQuestNAdd(MapleQuest.getInstance(99997)).getCustomData();
        if (num == null) {
            num = "0";
        }
        return Integer.parseInt(num);
    }

    public void setChronosphere(int mount) {
        getQuestNAdd(MapleQuest.getInstance(99997)).setCustomData(String.valueOf(mount));
    }

    public int getPQLog(String pqName) {
        return getPQLog(pqName, 0);
    }

    public int getPQLog(String pqName, int type) {
        return getPQLog(pqName, type, 1, false);
    }

    public int getDaysPQLog(String pqName, int days) {
        return getDaysPQLog(pqName, 0, days);
    }

    public int getDaysPQLog(String pqName, int type, int days) {
        return getPQLog(pqName, type, days, false);
    }

    public int getWeekPQLog(String pqName, int day, boolean week) {
        return getPQLog(pqName, 0, day, week);
    }

    public int getPQLog(String pqName, int type, int day, boolean week) {
        try {
            int count = 0;
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM pqlog WHERE characterid = ? AND pqname = ?");
            ps.setInt(1, this.id);
            ps.setString(2, pqName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getInt("count");
                Timestamp bossTime = rs.getTimestamp("time");
                rs.close();
                ps.close();
                if (type == 0) {
                    int d = getPQdayByWeek(pqName, day, bossTime == null ? 0 : bossTime.getTime(), week);
                    if (d >= 0) {
                        count = 0;
                        ps = con.prepareStatement(
                                "UPDATE pqlog SET count = 0, time = CURRENT_TIMESTAMP() WHERE characterid = ? AND pqname = ?");
                        ps.setInt(1, this.id);
                        ps.setString(2, pqName);
                        ps.executeUpdate();
                        ps.close();
                    }
                }
            } else {
                try (PreparedStatement psu = con
                        .prepareStatement("INSERT INTO pqlog (characterid, pqname, count, type) VALUES (?, ?, ?, ?)")) {
                    psu.setInt(1, this.id);
                    psu.setString(2, pqName);
                    psu.setInt(3, 0);
                    psu.setInt(4, type);
                    psu.executeUpdate();
                    ps.close();
                }
            }
            rs.close();
            ps.close();
            ManagerDatabasePool.closeConnection(con);
            return count;
        } catch (SQLException Ex) {
            System.err.println("Error while get pqlog: " + Ex);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, Ex);
        }
        return -1;
    }

    public int getPQday(String pqName, int days) {
        int day = 0;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM pqlog WHERE characterid = ? AND pqname = ?");
            ps.setInt(1, this.id);
            ps.setString(2, pqName);
            ResultSet rs = ps.executeQuery();
            long time = 0;
            if (rs.next()) {
                time = rs.getTimestamp("time").getTime();
            }
            day = getPQday(pqName, days, time);
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException Ex) {
            System.err.println("Error while get pqday: " + Ex);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, Ex);
        }
        return day;
    }

    public int getPQday(String pqName, int days, long time) {
        return getPQdayByWeek(pqName, days, time, false);
    }

    public int getPQdayByWeek(String pqName, int days, long time, boolean week) {
        Calendar calNow = Calendar.getInstance();
        Calendar sqlcal = Calendar.getInstance();
        if (time > 0) {
            sqlcal.setTimeInMillis(time);
            if (week) {
                int dayOfWeek = sqlcal.get(Calendar.DAY_OF_WEEK);
                if (days < dayOfWeek) {
                    days = dayOfWeek - days;
                } else if (days == dayOfWeek) {
                    days = 7;
                } else if (days > dayOfWeek) {
                    days = 7 - dayOfWeek + days;
                }
            }
            sqlcal.add(Calendar.DAY_OF_YEAR, days);
        }
        int day;
        if (calNow.get(Calendar.YEAR) - sqlcal.get(Calendar.YEAR) > 1) {
            day = 0;
        } else if (calNow.get(Calendar.YEAR) - sqlcal.get(Calendar.YEAR) >= 0) {
            if (calNow.get(Calendar.YEAR) - sqlcal.get(Calendar.YEAR) > 0) {
                sqlcal.add(Calendar.YEAR, 1);
            }
            day = calNow.get(Calendar.DAY_OF_YEAR) - sqlcal.get(Calendar.DAY_OF_YEAR);
        } else {
            day = -1;
        }
        return day;
    }

    public void setPQLog(String pqName) {
        setPQLog(pqName, 0);
    }

    public void setPQLog(String pqName, int type) {
        setPQLog(pqName, type, 1);
    }

    public void setPQLog(String pqName, int type, int count) {
        int pqCount = getPQLog(pqName, type);
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE pqlog SET count = ?, type = ?, time = CURRENT_TIMESTAMP() WHERE characterid = ? AND pqname = ?")) {
                ps.setInt(1, pqCount + count);
                ps.setInt(2, type);
                ps.setInt(3, this.id);
                ps.setString(4, pqName);
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException Ex) {
            System.err.println("Error while set pqlog: " + Ex);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, Ex);
        }
    }

    public void resetPQLog(String pqName) {
        resetPQLog(pqName, 0);
    }

    public void resetPQLog(String pqName, int type) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE pqlog SET count = ?, type = ?, time = CURRENT_TIMESTAMP() WHERE characterid = ? AND pqname = ?")) {
                ps.setInt(1, 0);
                ps.setInt(2, type);
                ps.setInt(3, this.id);
                ps.setString(4, pqName);
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException Ex) {
            System.err.println("Error while reset pqlog: " + Ex);
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, Ex);
        }
    }

    public void iNeedSystemProcess() {
        setLastCheckProcess(System.currentTimeMillis());
        this.getClient().announce(CWvsContext.SystemProcess());
    }

    public long getLastCheckProcess() {
        return lastCheckProcess;
    }

    public void setLastCheckProcess(long lastCheckProcess) {
        this.lastCheckProcess = lastCheckProcess;
    }

    public List<MapleProcess> getProcess() {
        return Process;
    }

    public void setKillCount(int x) {
        killCount = x;
    }

    public int getKillCount() {
        return killCount;
    }

    public void setmod(int x) {
        this.mod = x;
    }

    public int getmod() {
        return this.mod;
    }

    public void setStiffTime(long x) {
        this.stifftime = x;
    }

    public long getStiffTime() {
        return stifftime;
    }

    public boolean useCube(int itemId, Equip eq) {
        return useCube(itemId, eq, 0);
    }

    public boolean useCube(int itemId, Equip eq, int toLock) {
        int stateRate = 4;
        int cubeTpye = ItemConstants.方塊.getCubeType(itemId);
        boolean isBonus = ItemConstants.方塊.CubeType.附加潛能.check(cubeTpye);
        if (!ItemConstants.方塊.canUseCube(eq, itemId)) {
            dropMessage(5, "在這道具無法使用。");
            return false;
        }
        if (eq.getState(isBonus) >= 17 && eq.getState(isBonus) <= 20) {
            int cubeFragment = ItemConstants.方塊.getCubeFragment(itemId);
            if (cubeFragment > 0 && !MapleInventoryManipulator.addById(getClient(), cubeFragment, (short) 1,
                    new StringBuilder().append("使用方塊獲得").append(FileoutputUtil.CurrentReadable_Date()).toString())) {
                return false;
            }
            eq.setOldState(eq.getState(isBonus));
            if (EquipStat.EnhanctBuff.UPGRADE_TIER.check(eq.getEnhanctBuff())) {
                if (isShowInfo()) {
                    dropMessage(-6, "裝備自帶100%潛能等級提升成功率");
                }
                stateRate = 100;
            }
            if (isShowInfo() && isInvincible() && eq.getState(isBonus) < 20 && stateRate < 100) {
                dropMessage(-6, "伺服器管理員無敵狀態潛能等級提升成功率100%");
                stateRate = 100;
            }
            eq.renewPotential(stateRate, cubeTpye, toLock);
            forceReAddItem_NoUpdate(eq);
            return true;
        } else {
            dropMessage(5, "請確認您要重置的道具具有潛能屬性。");
        }
        return false;
    }

    public boolean changeFace(int color) {
        int f = (face / 1000 * 1000) + (color * 100) + (face % 100);
        if (!MapleItemInformationProvider.getInstance().itemExists(f)) {
            return false;
        }
        face = f;
        updateSingleStat(MapleStat.FACE, face);
        equipChanged();
        return true;
    }

    public boolean isEquippedSoulWeapon() {
        Equip weapon = (Equip) getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        if (weapon == null) {
            return false;
        }
        return weapon.getSoulEnchanter() != 0;
    }

    public boolean isSoulWeapon(Equip equip) {
        if (equip == null) {
            return false;
        }
        return equip.getSoulEnchanter() != 0;
    }

    public void equipSoulWeapon(Equip equip) {
        changeSkillLevel(SkillFactory.getSkill(getEquippedSoulSkill()), (byte) -1, (byte) 0);
        changeSkillLevel(SkillFactory.getSkill(equip.getSoulSkill()), (byte) 1, (byte) 1);
        setSoulCount((short) 0);
        getClient().announce(CWvsContext.InventoryPacket.getInventoryFull());
        getClient().announce(CWvsContext.BuffPacket.giveSoulGauge(getSoulCount(), equip.getSoulSkill()));
    }

    public void unequipSoulWeapon(Equip equip) {
        changeSkillLevel(SkillFactory.getSkill(equip.getSoulSkill()), (byte) -1, (byte) 0);
        setSoulCount((short) 0);
        getClient().announce(CWvsContext.BuffPacket.cancelSoulGauge());
        getClient().announce(CWvsContext.BuffPacket.cancelSoulEffect());
        getMap().broadcastMessage(CWvsContext.BuffPacket.cancelForeignSoulEffect(getId()));
        getClient().announce(CWvsContext.InventoryPacket.getInventoryFull());
    }

    public void checkSoulState(boolean useskill) {
        int skillid = getEquippedSoulSkill();
        Skill sk = SkillFactory.getSkill(skillid);
        if (useskill) {
            MapleStatEffect effect = null;
            if (sk != null && getTotalSkillLevel(skillid) > 0) {
                effect = sk.getEffect(getTotalSkillLevel(skillid));
            }
            if (effect != null && getSoulCount() >= 1000 /* effect.getSoulMPCon() 新版魂系統 */
                    && !skillisCooling(skillid)) {
                // setSoulCount((short) (getSoulCount() - effect.getSoulMPCon())); // 新版魂系統會消耗魂,
                // 舊版不會
                getClient().announce(CWvsContext.BuffPacket.cancelSoulEffect());
                getClient().announce(CWvsContext.BuffPacket.giveSoulGauge(999, skillid));
            } else {
                getClient().announce(CWvsContext.BuffPacket.cancelSoulEffect());
                getClient().announce(CWvsContext.BuffPacket.giveSoulGauge(999, skillid));
            }
            getMap().broadcastMessage(CWvsContext.BuffPacket.cancelForeignSoulEffect(getId()));
        } else if (getSoulCount() >= 1000) {
            if (!skillisCooling(skillid)) {
                getClient().announce(CWvsContext.BuffPacket.giveSoulEffect(skillid));
                getMap().broadcastMessage(this, CWvsContext.BuffPacket.giveForeignSoulEffect(getId(), skillid), false);
            } else {
                getClient().announce(CWvsContext.BuffPacket.giveSoulGauge(999, skillid));
            }
        }
        getClient().announce(CWvsContext.InventoryPacket.getInventoryFull());
    }

    public short getSoulCount() {
        return soulcount;
    }

    public void setSoulCount(short soulcount) {
        this.soulcount = (short) Math.min(1000, Math.max(0, soulcount));
    }

    public void addSoulCount(int num) {
        setSoulCount((short) Math.min(1000, Math.max(0, soulcount + num)));
    }

    public short addgetSoulCount(int num) {
        addSoulCount(num);
        return getSoulCount();
    }

    public int getEquippedSoulSkill() {
        Equip weapon = (Equip) getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        return weapon.getSoulSkill();// weapon.getSoulSkill();
    }

    public int getSoulSkillMpCon() {
        int skillid = getEquippedSoulSkill();
        MapleStatEffect skill = SkillFactory.getSkill(skillid).getEffect(getTotalSkillLevel(skillid));
        return skill.getSoulMPCon();
    }

    public int getWeaponPoint() {
        return weaponPoint;
    }

    public void gainWeaponPoint(int wp) {
        this.weaponPoint += wp;
    }

    public void addWeaponPoint(int wp) {
        gainWeaponPoint(wp);
        getClient().announce(CField.ZeroPacket.gainWeaponPoint(wp));
        getClient().announce(CField.ZeroPacket.updateWeaponPoint(getWeaponPoint()));
    }

    public short getJianQi() {
        return StringTool.parseShort(getTempValue("剣気"));
    }

    public void setJianQi(short value) {
        setTempValue("剣気", String.valueOf((short) Math.min(value, 1000)));
        getClient().announce(CField.hayatoJianQi(getJianQi()));
    }

    public long getCooldownLimit(int skillid) {
        for (MapleCoolDownValueHolder mcdvh : getAllCooldowns()) {
            if (mcdvh.skillId == skillid) {
                return System.currentTimeMillis() - mcdvh.startTime;
            }
        }
        return 0;
    }

    public void gainCooldownTime(int skillId, int time) {
        if (coolDowns.containsKey(skillId)) {
            long cooldownTime = coolDowns.get(skillId).length;
            cooldownTime += time * 1000;
            if (cooldownTime <= 0) {
                removeCooldown(skillId);
                client.announce(CField.skillCooldown(skillId, 0));
            } else {
                coolDowns.get(skillId).length = cooldownTime;
                client.announce(CField.skillCooldown(skillId, (int) (cooldownTime / 1000)));
            }
        }
    }

    public List<MapleCoolDownValueHolder> getAllCooldowns() {
        List<MapleCoolDownValueHolder> ret = new ArrayList<>();
        for (MapleCoolDownValueHolder mcdvh : coolDowns.values()) {
            ret.add(new MapleCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
        }
        return ret;
    }

    public boolean cheakSkipOnceChat() {
        skipOnceChat = !skipOnceChat;
        return skipOnceChat;
    }

    public Collection<MapleMonster> getControlledMonsters() {
        return Collections.unmodifiableCollection(controlled);
    }

    public Map<Integer, List<Pair<Integer, Integer>>> getSalon() {
        return salon;
    }

    public List<Integer> getEffectSwitch() {
        if (isClone()) {
            return cloneOwner.getEffectSwitch();
        }
        List<String> effectList = StringTool.splitList(getValue("EffectSwitch"), ";");
        List<Integer> posList = new ArrayList<>();
        if (effectList != null) {
            for (String pos : effectList) {
                posList.add(StringTool.parseInt(pos));
            }
        }
        return posList;
    }

    public void updateEffectSwitch(int pos) {
        List<String> effectList = StringTool.splitList(getValue("EffectSwitch"), ";");
        if (effectList == null) {
            setValue("EffectSwitch", String.valueOf(pos));
            return;
        }
        if (!effectList.contains(String.valueOf(pos))) {
            effectList.add(String.valueOf(pos));
        } else {
            effectList.remove(String.valueOf(pos));
        }
        setValue("EffectSwitch", StringTool.unite(effectList, ";"));
    }

    public int addAntiMacroFailureTimes() {
        int times = getAntiMacroFailureTimes();
        setAntiMacroFailureTimes(++times);
        return times;
    }

    public void setAntiMacroFailureTimes(int times) {
        setValue("測謊機失敗次數", String.valueOf(times));
    }

    public int getAntiMacroFailureTimes() {
        String timesStr = getValue("測謊機失敗次數");
        if (timesStr == null) {
            setAntiMacroFailureTimes(0);
            return 0;
        }
        return StringTool.parseInt(timesStr);
    }

    public void equip(int itemId) {
        equip(itemId, false);
    }

    public void equip(int itemId, boolean replace) {
        equip(itemId, replace, true);
    }

    public void equip(int itemId, boolean replace, boolean add) {
        MapleInventory equipped = getInventory(MapleInventoryType.EQUIPPED);
        MapleInventory equip = getInventory(MapleInventoryType.EQUIP);
        Equip eqp = null;
        for (Item item : equip.newList()) {
            if (item.getItemId() == itemId) {
                eqp = (Equip) item;
            }
        }

        if (eqp == null) {
            if (add) {
                final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
                Item item = li.getEquipById(itemId);
                item.setPosition(equip.getNextFreeSlot());
                item.setGMLog("從穿戴函數中獲得, 時間 " + FileoutputUtil.CurrentReadable_Time());
                MapleInventoryManipulator.addbyItem(client, item);
                eqp = (Equip) item;
            } else {
                return;
            }
        }

        short slot = 0;
        short[] slots = ItemConstants.getEquipedSlot(itemId);
        switch (slots.length) {
            case 0:
                if (isShowErr()) {
                    showInfo("穿戴函數", true, "未找到穿戴位置, 道具ID:" + itemId);
                }
                return;
            case 1:
                slot = slots[0];
                break;
            default:
                for (short i : slots) {
                    if (equipped.getItem(slot) == null) {
                        slot = i;
                        break;
                    }
                }
                if (slot == 0) {
                    slot = slots[0];
                }
                break;
        }
        if (slot == 0) {
            if (isShowErr()) {
                showInfo("穿戴函數", true, "未找到穿戴位置, 道具ID:" + itemId);
            }
            return;
        }

        if (replace && equipped.getItem(slot) != null) {
            equipped.removeSlot(slot);
            getClient().announce(InventoryPacket.dropInventoryItem(MapleInventoryType.EQUIP, slot));
        }
        MapleInventoryManipulator.equip(client, eqp.getPosition(), slot);
    }

    public void unequip(int itemId) {
        unequip(itemId, false);
    }

    public void unequip(int itemId, boolean remove) {
        MapleInventory equipped = getInventory(MapleInventoryType.EQUIPPED);
        Equip eqp = null;
        if (itemId >= 0) {
            for (Item item : equipped.newList()) {
                if (item.getItemId() == itemId) {
                    eqp = (Equip) item;
                }
            }
        } else {
            eqp = (Equip) equipped.getItem((short) itemId);
        }

        if (eqp == null) {
            return;
        }

        if (remove) {
            equipped.removeSlot(eqp.getPosition());
            getClient().announce(InventoryPacket.dropInventoryItem(MapleInventoryType.EQUIP, eqp.getPosition()));
        } else {
            MapleInventoryManipulator.unequip(client, eqp.getPosition(),
                    getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
        }
    }

    public void showPlayerStats() {
        showPlayerStats(this);
    }

    public void showPlayerStats(boolean advanced) {
        showPlayerStats(this, advanced);
    }

    public void showPlayerStats(MapleCharacter chr) {
        showPlayerStats(chr, chr.isGM());
    }

    public void showPlayerStats(MapleCharacter player, boolean advanced) {
        StringBuilder builder = new StringBuilder();
        if (player.getClient().getLastPing() <= 0) {
            player.getClient().sendPing();
        }
        if (advanced) {
            builder.append(MapleClient.getLogMessage(player, ""));
        } else {
            builder.append(player.getName());
        }
        builder.append(" 職業:").append(MapleJob.getById(player.getJob()).name());
        builder.append(" LV.").append(player.getLevel());
        builder.append("地圖:").append(player.getMap().getMapName()).append("(").append(player.getMap().getId())
                .append(") ");
        if (advanced) {
            builder.append(" 坐標(").append(player.getPosition().x).append(", ");
            builder.append(player.getPosition().y).append(")");
        }
        showMessage(0, builder.toString());
        builder = new StringBuilder();
        builder.append("HP:");
        builder.append(player.getStat().getHp());
        builder.append("/");
        builder.append(player.getStat().getCurrentMaxHp());
        builder.append(" MP:");
        builder.append(player.getStat().getMp());
        builder.append("/");
        builder.append(player.getStat().getCurrentMaxMp());
        builder.append(" EXP:");
        builder.append(player.getExp());
        builder.append("/");
        builder.append(player.getNeededExp());
        showMessage(0, builder.toString());
        builder = new StringBuilder();
        builder.append("STR[");
        builder.append(player.getStat().getStr()).append(" + (")
                .append(player.getStat().getTotalStr() - player.getStat().getStr()).append(")]");
        builder.append(" DEX[");
        builder.append(player.getStat().getDex()).append(" + (")
                .append(player.getStat().getTotalDex() - player.getStat().getDex()).append(")]");
        builder.append(" INT[");
        builder.append(player.getStat().getInt()).append(" + (")
                .append(player.getStat().getTotalInt() - player.getStat().getInt()).append(")]");
        builder.append(" LUK[");
        builder.append(player.getStat().getLuk()).append(" + (")
                .append(player.getStat().getTotalLuk() - player.getStat().getLuk()).append(")]");
        showMessage(0, builder.toString());
        builder = new StringBuilder();
        builder.append("物理攻擊力[");
        builder.append(player.getStat().getTotalWatk());
        builder.append("] 魔法攻擊力[");
        builder.append(player.getStat().getTotalMagic());
        builder.append("] 屬性攻擊力[");
        builder.append((int) Math
                .ceil(((player.getStat().trueMastery) / 100.0D) * player.getStat().getCurrentMaxBaseDamage()));
        builder.append(" ~ ");
        builder.append((int) Math.ceil(player.getStat().getCurrentMaxBaseDamage()));
        builder.append("]");
        showMessage(0, builder.toString());
        builder = new StringBuilder();
        builder.append("總傷害[");
        builder.append((int) Math.ceil(player.getStat().dam_r - 100));
        builder.append("%] BOSS攻擊力[");
        builder.append((int) Math.ceil(player.getStat().bossdam_r - 100));
        builder.append("%]");
        showMessage(0, builder.toString());
        builder = new StringBuilder();
        builder.append("防禦力[");
        builder.append(player.getStat().getTotalWDef());
        builder.append("]");
        showMessage(0, builder.toString());
        builder = new StringBuilder();
        builder.append("暴擊發動[");
        builder.append(player.getStat().getCritRate());
        builder.append("%] 最小暴擊[");
        builder.append(player.getStat().getMinCritDamage());
        builder.append("%] 最大暴擊[");
        builder.append(player.getStat().getMaxCritDamage());
        builder.append("%] 無視防禦[");
        builder.append((int) Math.ceil(player.getStat().ignoreTargetDEF));
        builder.append("%]");
        showMessage(0, builder.toString());
        builder = new StringBuilder();
        builder.append("狀態異常耐性[");
        builder.append(player.getStat().異常抗性);
        builder.append("%]格擋[");
        builder.append(player.getStat().格擋率);
        builder.append("%]移動速度[");
        builder.append(player.getStat().移動速度);
        builder.append("%]跳躍力[");
        builder.append(player.getStat().跳躍力);
        builder.append("%] 星光能量[");
        builder.append(player.getStat().getStarForce());
        builder.append("]");
        showMessage(0, builder.toString());
        builder = new StringBuilder();
        builder.append("楓幣:");
        builder.append(player.getMeso());
        builder.append(" 樂豆點數:");
        builder.append(player.getCSPoints(1));
        builder.append(" 楓葉點數:");
        builder.append(player.getCSPoints(2));
        builder.append(" 里程:");
        builder.append(player.getCSPoints(3));
        showMessage(0, builder.toString());
        if (advanced) {
            builder = new StringBuilder();
            builder.append("組隊:");
            builder.append(player.getParty() == null ? -1 : player.getParty().getId());
            builder.append(" 是否交易:");
            builder.append(player.getTrade() != null);
            builder.append(" Latency:");
            builder.append(player.getClient().getLatency());
            builder.append(" PING:");
            builder.append(player.getClient().getLastPing());
            builder.append(" PONG:");
            builder.append(player.getClient().getLastPong());
            showMessage(0, builder.toString());
            builder = new StringBuilder();
            player.getClient().DebugMessage(builder);
            showMessage(0, builder.toString());
        }
    }

    private int ReChargeFalse = 0; // 天使充電失敗
    private int ReChargeFalse_2 = 0; // 天使充電失敗
    private int KillMob_Temp = 0;
    private boolean KillMob_Temp_using = false;

    public void ResetKillMob_Temp() {// 親和力 Ⅱ
        KillMob_Temp = 0;
    }

    public int getKillMob_Temp() {// 親和力 Ⅱ
        return KillMob_Temp;
    }

    public void addKillMob_Temp() {// 親和力 Ⅱ
        KillMob_Temp++;
        if (getTotalSkillLevel(65100005) > 0) {
            MapleStatEffect eff = SkillFactory.getSkill(65100005).getEffect(getTotalSkillLevel(65100005));
            if (getKillMob_Temp() >= eff.getX()) {
                setKillMob_Temp_using(true);
                ResetKillMob_Temp();
            }
        }
    }

    public void setKillMob_Temp_using(boolean use) {// 親和力 Ⅱ
        KillMob_Temp_using = use;
    }

    public boolean getKillMob_Temp_using() {// 親和力 Ⅱ
        return KillMob_Temp_using;
    }

    public int getReChargeFalse() {// 親和力 Ⅲ
        return ReChargeFalse;
    }

    public void addReChargeFalse() {// 親和力 Ⅲ
        ReChargeFalse++;
    }

    public void ResetReChargeFalse() {// 親和力 Ⅲ
        ReChargeFalse = 0;
    }

    public int getReChargeFalse_2() {// 親和力 Ⅳ
        return ReChargeFalse_2;
    }

    public void addReChargeFalse_2() {// 親和力 Ⅳ
        ReChargeFalse_2++;
    }

    public void ResetReChargeFalse_2() {// 親和力 v
        ReChargeFalse_2 = 0;
    }

    public void Handle_ReCharge(MapleStatEffect effect, int Skillid, MapleClient c, boolean special) {
        boolean success = true;
        int Recharge = c.getPlayer().getRechargeAdd(effect, c);
        if (Recharge > -1) {
            if (Randomizer.isSuccess(Recharge)) {
                c.announce(JobPacket.AngelicPacket.unlockSkill());
                c.announce(CField.EffectPacket.showRechargeEffect());
            } else {
                if (!special) {
                    c.announce(JobPacket.AngelicPacket.lockSkill(Skillid));
                }
                success = false;
            }
        } else {
            if (!special) {
                c.announce(JobPacket.AngelicPacket.lockSkill(Skillid));
            }
            success = false;
        }
        if (!success) {
            c.getPlayer().addReChargeFalse();// 親和力 Ⅲ
            c.getPlayer().addReChargeFalse_2();// 親和力 Ⅳ
        } else {
            c.getPlayer().ResetReChargeFalse();
            c.getPlayer().ResetReChargeFalse_2();// 親和力 Ⅳ
        }
    }

    public int getRechargeAdd(MapleStatEffect effect, MapleClient c) {
        int Recharge = effect.getOnActive();
        if (Recharge == 0) {
            return -1;
        }
        Recharge += c.getPlayer().getStat().ReChargeChance;
        MapleStatEffect eff = null;
        if (c.getPlayer().getKillMob_Temp_using()) {// 親和力 Ⅱ
            if (c.getPlayer().getTotalSkillLevel(65100005) > 0) {
                eff = SkillFactory.getSkill(65100005).getEffect(c.getPlayer().getTotalSkillLevel(65100005));
                Recharge += eff.getY();
                c.getPlayer().setKillMob_Temp_using(false);
            }
        }
        if (c.getPlayer().getReChargeFalse() >= 2) {// 親和力 Ⅲ (連續失敗)
            if (c.getPlayer().getTotalSkillLevel(65110006) > 0) {
                eff = SkillFactory.getSkill(65110006).getEffect(c.getPlayer().getTotalSkillLevel(65110006));
                Recharge += eff.getX();
                c.getPlayer().ResetReChargeFalse();
            }
        }
        if (c.getPlayer().getReChargeFalse_2() >= 1) {// 親和力 Ⅳ
            if (c.getPlayer().getTotalSkillLevel(65120006) > 0) {
                eff = SkillFactory.getSkill(65120006).getEffect(c.getPlayer().getTotalSkillLevel(65120006));
                Recharge += eff.getX();
                c.getPlayer().ResetReChargeFalse();
            }
        }
        return Recharge;
    }

    public void updateLastAttackTime() {
        if (lastAttackTime == 0) {
            MapleAntiMacro.updateCooling(name);
        }
        lastAttackTime = System.currentTimeMillis();
    }

    public boolean isAttacking() {
        if (lastAttackTime != 0 && System.currentTimeMillis() - lastAttackTime < 5 * 1000) {
            return true;
        }
        return false;
    }

    public List<VMatrixRecord> getVMatrixRecords() {
        return aVMatrixRecords;
    }

    public int getActiveVMatrixRecordsCount() {
        int count = (int) aVMatrixRecords.stream().filter(VMatrixRecord::isActive).count();
        return count;
    }

    public void addVMatrixRecord(VMatrixRecord record) {
        this.aVMatrixRecords.add(record);
    }

    public void setVMatrixShard(int count) {
        this.nVMatrixShard = count;
        this.updateInfoQuest(1477, new StringBuilder().insert(0, "count=").append(this.nVMatrixShard).toString());
    }

    public void addVMatrixShard(int count) {
        this.nVMatrixShard += count;
        this.updateInfoQuest(1477, new StringBuilder().insert(0, "count=").append(this.nVMatrixShard).toString());
    }

    public int getVMatrixShard() {
        return this.nVMatrixShard;
    }

    public MapleBingoRecord getBingoRecord() {
        return this.bingoRecord;

    }

    private static class ComparatorImpl implements Comparator<Pair<ReportType, Integer>> {

        public ComparatorImpl() {
        }

        @Override
        public final int compare(final Pair<ReportType, Integer> o1, final Pair<ReportType, Integer> o2) {
            final int thisVal = o1.getRight();
            final int anotherVal = o2.getRight();
            return Integer.compare(anotherVal, thisVal);
        }
    }

    public ForceAtom getAtom(int key) {
        return this.forceAtomInfo.getAtom(key);
    }

    public ForceAtom getNewAtom(int atomSkillID, boolean byMob) {
        return this.forceAtomInfo.getNewAtom(this, atomSkillID, byMob);
    }

    public void clearExpireAtom() {
        this.forceAtomInfo.removeExpire();
    }

    public ForceAtomInfo getForceAtomInfo() {
        return this.forceAtomInfo;
    }

    public void removeForceAtom(ForceAtom atom) {
        this.getForceAtomInfo().removeAtom(atom);
    }
}
