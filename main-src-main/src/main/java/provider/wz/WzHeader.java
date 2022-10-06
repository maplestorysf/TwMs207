package provider.wz;

public class WzHeader {

    public String Ident;
    public String Copyright;
    public long FSize;
    public int FStart;

    public void RecalculateFileStart() {
        FStart = (int) (Ident.length() + 8 + 4 + Copyright.length() + 1);
    }

    public static WzHeader GetDefault() {
        WzHeader header = new WzHeader();
        header.Ident = "PKG1";
        header.Copyright = "Package file v1.0 Copyright 2002 Wizet, ZMS";
        header.FStart = 60;
        header.FSize = 0;
        return header;
    }
}
