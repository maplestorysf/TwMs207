package server;

public class CashItem {

    private int Priority;
    private int ItemId;
    private int Count;
    private int Price;
    private int SN;
    private int Period;
    private int Gender;
    private int Class;
    private boolean OnSale;
    private int TimeBegin, TimeEnd;

    // TODO 商城商品::添加新的商品屬性
    public CashItem(int sn, int itemId, int count, int price, int period, int gender, int Class, boolean sale) {
        this.SN = sn;
        this.ItemId = itemId;
        this.Count = count;
        this.Price = price;
        this.Period = period;
        this.Gender = gender;
        this.Class = Class;
        this.OnSale = sale;
        this.Priority = 0;
        this.TimeBegin = 0;
        this.TimeEnd = 0;
    }

    public void setItemId(int ItemId) {
        this.ItemId = ItemId;
    }

    public int getItemId() {
        return ItemId;
    }

    public void setCount(int Count) {
        this.Count = Count;
    }

    public int getCount() {
        return Count;
    }

    public void setPrice(int Price) {
        this.Price = Price;
    }

    public int getPrice() {
        return Price;
    }

    public void setSN(int SN) {
        this.SN = SN;
    }

    public int getSN() {
        return SN;
    }

    public void setPeriod(int Period) {
        this.Period = Period;
    }

    public int getPeriod() {
        return Period;
    }

    public void setGender(int Gender) {
        this.Gender = Gender;
    }

    public int getGender() {
        return Gender;
    }

    public void setPriority(int Priority) {
        this.Priority = Priority;
    }

    public int getPriority() {
        return Priority;
    }

    public void setOnSale(boolean OnSale) {
        this.OnSale = OnSale;
    }

    public boolean isOnSale() {
        return OnSale;
    }

    public void setClass_(int Class) {
        this.Class = Class;
    }

    public int getClass_() {
        return Class;
    }

    public void setTimeBegin(int time) {
        this.TimeBegin = time;
    }

    public int getTimeBegin(int time) {
        return TimeBegin;
    }

    public void setTimeEnd(int time) {
        this.TimeEnd = time;
    }

    public int getTimeEnd(int time) {
        return TimeEnd;
    }

    public boolean genderEquals(int g) {
        return g == this.Gender || this.Gender == 2;
    }
}
