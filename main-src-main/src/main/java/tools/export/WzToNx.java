package tools.export;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataEntity;
import provider.MapleDataFileEntry;
import provider.MapleDataTool;
import provider.wz.ImgMapleSound;
import provider.wz.PNGMapleCanvas;
import provider.wz.WZFile;
import provider.wz.WZFileEntry;
import provider.wz.WZIMGFile;
import provider.wz.util.WzLittleEndianAccessor;
import server.ServerProperties;
import tools.data.LittleEndianAccessor;
import tools.data.RandomAccessByteStream;

public class WzToNx {

    public static String IN_PATH = "wz";
    public static String OUT_PATH = "./nx";
    public static boolean CLIENT = false;
    private static final SimpleDateFormat sdfGMT = new SimpleDateFormat("yyyyMMddHHmmss");
    private static long node_offset, string_table_offset, bitmap_table_offset, audio_table_offset;
    private static int nodeSize = 0;
    private static List<String> strings = new LinkedList<String>();
    private static List<ImgMapleSound> audios = new LinkedList<ImgMapleSound>();
    private static List<PNGMapleCanvas> bitmaps = new LinkedList<PNGMapleCanvas>();
    private static List<Long> string_table = new LinkedList<Long>(), bitmap_table = new LinkedList<Long>(), audio_table = new LinkedList<Long>();

    private static List<MapleDataEntity> nodeLevel;

    private static WZFile paseFile(File f) {
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            LittleEndianAccessor lea = new LittleEndianAccessor(new RandomAccessByteStream(raf));
            if (!lea.readAsciiString(4).equalsIgnoreCase("PKG1")) {
                System.err.println("檔案" + f.getName() + "不是WZ檔案");
                return null;
            }
        } catch (Exception e) {
            System.err.println("讀取檔案" + f.getName() + "時出錯:" + e);
            return null;
        }
        WZFile wzFile = null;
        try {
            wzFile = new WZFile(f, false);
        } catch (IOException e) {
            System.err.println("讀取檔案" + f.getName() + "時出錯:" + e);
        }
        if (wzFile == null) return null;

        return wzFile;
    }

    private static void openOut(RandomAccessByteStream raf) throws IOException {
        raf.seek(0);
        //Magic
        raf.writeAsciiString("PKG4");
        //Node count
        raf.writeInt(nodeSize);
        //Node block offset
        raf.writeLong(node_offset);
        //String count
        raf.writeInt(strings.size());
        //String offset table offset
        raf.writeLong(string_table_offset);

        //Bitmap count
        raf.writeInt(!CLIENT ? 0 : bitmaps.size());
        //Bitmap offset table offset
        raf.writeLong(!CLIENT ? 0 : bitmap_table_offset);
        //Audio count
        raf.writeInt(!CLIENT ? 0 : audios.size());
        //Audio offset table offset
        raf.writeLong(!CLIENT ? 0 : audio_table_offset);
    }

    private static void WriteNodeLevel(RandomAccessByteStream raf, WZFile wzFile) throws FileNotFoundException {
        int nextChildId = nodeSize + nodeLevel.size();
        List<MapleDataEntity> childNodes = new ArrayList<MapleDataEntity>();
        for (MapleDataEntity levelNode : nodeLevel) {
            List<MapleDataEntity> childsList = new ArrayList<MapleDataEntity>();
            if (levelNode instanceof MapleData) {
                childsList.addAll(((MapleData) levelNode).getChildren());
            } else if (levelNode instanceof MapleDataDirectoryEntry) {
                childsList.addAll(((MapleDataDirectoryEntry) levelNode).getSubdirectories());
                childsList.addAll(((MapleDataDirectoryEntry) levelNode).getFiles());
            } else if (levelNode instanceof MapleDataFileEntry) {
                if (!levelNode.getName().endsWith(".img")) {
                    System.err.println("略過未知理類型Node:" + levelNode.getName());
                    continue;
                } else {
                    StringBuffer path = new StringBuffer(levelNode.getName());
                    MapleDataEntity parentNode = levelNode.getParent();
                    while (parentNode != null) {
                        if (parentNode.getParent() != null &&
                                parentNode.getName() != null &&
                                !parentNode.getName().isEmpty()) {
                            path.insert(0, parentNode.getName() + "/");
                        }
                        parentNode = parentNode.getParent();
                    }
                    MapleData dat = wzFile.getData(path.toString());
                    if (dat != null) childsList.addAll(dat.getChildren());
                }
            }

            nodeSize++;
            //Node name
            raf.writeInt(strings.size());
            strings.add(levelNode.getName() == null ? "" : levelNode.getName());

            //First Child ID 
            raf.writeInt(nextChildId++);
            //Children count
            raf.writeShort(childsList.size());
            childNodes.addAll(childsList);

            nextChildId += childsList.size();

            //Type
            short type;
            if (levelNode instanceof MapleData) {
                switch (((MapleData) levelNode).getType()) {
                    case SHORT:
                    case INT:
                    case LONG:
                        //Type
                        raf.writeShort(1);
                        //Data
                        raf.writeLong(MapleDataTool.getLongConvert((MapleData) levelNode, 0));
                        break;
                    case FLOAT:
                    case DOUBLE:
                        //Type
                        raf.writeShort(2);
                        //Data
                        raf.writeLong(Double.doubleToLongBits(MapleDataTool.getDoubleConvert((MapleData) levelNode, 0.0)));
                        break;
                    case STRING:
                        //Type
                        raf.writeShort(3);
                        //Data
                        raf.writeInt(strings.size());
                        strings.add(MapleDataTool.getString((MapleData) levelNode, ""));
                        raf.writeInt(0);
                        break;
                    case VECTOR:
                        //Type
                        raf.writeShort(4);
                        //Data
                        Point p = MapleDataTool.getPoint((MapleData) levelNode);
                        raf.writeInt(p.x);
                        raf.writeInt(p.y);
                        break;
                    case CANVAS:
                        //Type
                        raf.writeShort(5);
                        //Data
                        if (!CLIENT) raf.writeLong(0);
                        else {
                            raf.writeInt(bitmaps.size());
                            PNGMapleCanvas bitmap = (PNGMapleCanvas) ((MapleData) levelNode).getData();
                            bitmaps.add(bitmap);
                            raf.writeShort(bitmap.getWidth());
                            raf.writeShort(bitmap.getHeight());
                        }
                        break;
                    case SOUND:
                        //Type
                        raf.writeShort(6);
                        //Data
                        if (!CLIENT) raf.writeLong(0);
                        else {
                            raf.writeInt(audios.size());
                            ImgMapleSound audio = (ImgMapleSound) ((MapleData) levelNode).getData();
                            audios.add(audio);
                            raf.writeInt((int)audio.getDataLength());
                        }
                        break;
                    case NONE:
                    case IMG_0x00:
                    case EXTENDED:
                    case PROPERTY:
                    case CONVEX:
                    case UOL:
                    case UNKNOWN_TYPE:
                    case UNKNOWN_EXTENDED_TYPE:
                    default:
                        //Type
                        raf.writeShort(0);
                        //Data
                        raf.writeLong(0);
                        break;
                }
            } else {
                //Type
                raf.writeShort(0);
                //Data
                raf.writeLong(0);
            }
            levelNode = null;
        }

        nodeLevel.clear();
        nodeLevel = childNodes;
    }

    private static void writeNodes(RandomAccessByteStream raf, WZFile wzFile) throws IOException {
        raf.seek(52);
        node_offset = raf.getPosition();
        nodeLevel = new LinkedList<MapleDataEntity>();
        nodeLevel.add(wzFile.getRoot());
        while (nodeLevel.size() > 0) WriteNodeLevel(raf, wzFile);
    }

    private static void writeStrings(RandomAccessByteStream raf) throws IOException {
        string_table_offset = raf.getPosition();

        raf.seek(strings.size() * 8 + string_table_offset);
        for (String str : strings) {
            string_table.add(raf.getPosition());
            //Length
            raf.writeShort(str.length());
            //String data
            raf.writeAsciiString(str);
        }

        long logOffset = raf.getPosition();
        raf.seek(string_table_offset);
        for (long offset : string_table) {
            raf.writeLong(offset);
        }
        raf.seek(logOffset);
    }

    private static void writeAudio(RandomAccessByteStream raf) throws IOException {
        audio_table_offset = raf.getPosition();

        raf.seek(audios.size() * 8 + audio_table_offset);
        for (ImgMapleSound audio : audios) {
            audio_table.add(raf.getPosition());
            //Audio data
            raf.write(audio.getData());
        }

        long logOffset = raf.getPosition();
        raf.seek(audio_table_offset);
        for (long offset : audio_table) {
            raf.writeLong(offset);
        }
        raf.seek(logOffset);
    }

    private static void writeBitmaps(RandomAccessByteStream raf) throws IOException {
        bitmap_table_offset = raf.getPosition();

        raf.seek(bitmaps.size() * 8 + bitmap_table_offset);
        for (PNGMapleCanvas bitmap : bitmaps) {
            bitmap_table.add(raf.getPosition());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(bitmap.getImage(), "png", out);
            byte[] imageBytes = out.toByteArray();
            //Length
            raf.writeInt(imageBytes.length);
            //Bitmap data
            raf.write(imageBytes);
        }

        long logOffset = raf.getPosition();
        raf.seek(bitmap_table_offset);
        for (long offset : bitmap_table) {
            raf.writeLong(offset);
        }
        raf.seek(logOffset);
    }

    private static String getTempFileName() {
        Random random = new Random();
        String tempFileName = "";
        for (int i = 0; i < 20; i++) {
            int temp = random.nextInt(2) == 0 ? 0x41 : 0x61;
            tempFileName += String.valueOf((char) (random.nextInt(0x1A) + temp));
        }
        return tempFileName;
    }

    public static void main(String[] args) {
        File inFile = new File(IN_PATH);
        if (!inFile.exists()) {
            System.err.println("找不到WZ檔案");
            return;
        }

        File[] inFiles;
        if (inFile.isDirectory()) {
            inFiles = inFile.listFiles();
        } else {
            inFiles = new File[] {inFile};
        }

        File outDir = new File(OUT_PATH);
        if (outDir.exists() && !outDir.isDirectory()) {
            OUT_PATH += "_WzToNx_" + sdfGMT.format(Calendar.getInstance().getTime());
            outDir = new File(OUT_PATH);
        }
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        for (File f : inFiles) {
            WZFile wzFile = paseFile(f);
            if (wzFile == null) continue;

            nodeSize = 0;
            strings.clear();
            audios.clear();
            bitmaps.clear();
            string_table.clear();
            bitmap_table.clear();
            audio_table.clear();

            String tempFileName;
            File tempFile = null;
            while (tempFile == null || tempFile.exists()) {
                tempFileName = getTempFileName();
                tempFile = new File(outDir.getPath() + "\\" + tempFileName);
            }

            try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
                RandomAccessByteStream rabs = new RandomAccessByteStream(raf);
                writeNodes(rabs, wzFile);
                writeStrings(rabs);
                if (CLIENT) {
                    writeAudio(rabs);
                    writeBitmaps(rabs);
                }
                //Header (52 bytes)
                openOut(rabs);
                rabs.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(f.getName() + "寫入臨時檔案時出錯:" + e);
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                continue;
            }

            String outFileName = f.getName().substring(0, f.getName().lastIndexOf(".")) + ".nx";
            File outFile = new File (outDir.getPath() + "\\" + outFileName);
            if (outFile.exists()) {
                outFile.delete();
            }

            tempFile.renameTo(outFile);
            System.out.println(outFileName + "轉換完成");
        }
    }

    public static void loadSetting() {
        IN_PATH = ServerProperties.getProperty("WZ_IN_PATH", IN_PATH);
        OUT_PATH = ServerProperties.getProperty("WZ_OUT_PATH", OUT_PATH);
        CLIENT = ServerProperties.getProperty("Wz2Nx_Client", CLIENT);
    }

    static {
        loadSetting();
    }
}
