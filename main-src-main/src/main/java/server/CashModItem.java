/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author pungin
 */
public class CashModItem extends CashItem {

    public List<CashItemFlag> flags = new LinkedList<>();
    private boolean main;
    private String note;

    public CashModItem(int sn, String note, int itemId, int count, int price, int period, int gender, int Class,
            boolean sale, boolean main) {
        super(sn, itemId, count, price, period, gender, Class, sale);
        this.note = note;
        this.main = main;
    }

    public void initFlags(CashItem ci) {
        if (ci != null && ci.getSN() != getSN()) {
            return;
        }
        if (ci == null || ci.getItemId() != getItemId()) {
            flags.add(CashItemFlag.ItemId);
        }
        if (ci == null || ci.getCount() != getCount()) {
            flags.add(CashItemFlag.Count);
        }
        if (ci == null || ci.getPrice() != getPrice()) {
            flags.add(CashItemFlag.Price);
        }
        if (ci == null) {
            flags.add(CashItemFlag.Bonus);
        }
        if (ci == null || ci.getPriority() != getPriority()) {
            flags.add(CashItemFlag.Priority);
        }
        if (ci == null || ci.getPeriod() != getPeriod()) {
            flags.add(CashItemFlag.Period);
        }
        if (ci == null) {
            flags.add(CashItemFlag.MaplePoint);
        }
        if (ci == null) {
            flags.add(CashItemFlag.Meso);
        }
        if (ci == null) {
            flags.add(CashItemFlag.ForPremiumUser);
        }
        if (ci == null || ci.getGender() != getGender()) {
            flags.add(CashItemFlag.CommodityGender);
        }
        if (ci == null || ci.isOnSale() != isOnSale()) {
            flags.add(CashItemFlag.OnSale);
        }
        if (ci == null || ci.getClass_() != getClass_()) {
            flags.add(CashItemFlag.Class);
        }
        if (ci == null) {
            flags.add(CashItemFlag.Limit);
        }
        if (ci == null) {
            flags.add(CashItemFlag.PbCash);
        }
        if (ci == null) {
            flags.add(CashItemFlag.PbPoint);
        }
        if (ci == null) {
            flags.add(CashItemFlag.PbGift);
        }
        if (ci == null) {
            flags.add(CashItemFlag.PackageSN);
        }
        if (ci == null) {
            flags.add(CashItemFlag.ReqPOP);
        }
        if (ci == null) {
            flags.add(CashItemFlag.ReqLEV);
        }
        if (ci == null) {
            flags.add(CashItemFlag.TermStart);
        }
        if (ci == null) {
            flags.add(CashItemFlag.TermEnd);
        }
        if (ci == null) {
            flags.add(CashItemFlag.Refundable);
        }
        if (ci == null) {
            flags.add(CashItemFlag.BombSale);
        }
        if (ci == null) {
            flags.add(CashItemFlag.CategoryInfo);
        }
        if (ci == null) {
            flags.add(CashItemFlag.WorldLimit);
        }
        if (ci == null) {
            flags.add(CashItemFlag.Token);
        }
        if (ci == null) {
            flags.add(CashItemFlag.LimitMax);
        }
        if (ci == null) {
            flags.add(CashItemFlag.CheckQuestID);
        }
        if (ci == null) {
            flags.add(CashItemFlag.OriginalPrice);
        }
        if (ci == null) {
            flags.add(CashItemFlag.Discount);
        }
        if (ci == null) {
            flags.add(CashItemFlag.DiscountRate);
        }
        if (ci == null) {
            flags.add(CashItemFlag.MileageInfo);
        }
        if (ci == null) {
            flags.add(CashItemFlag.Zero);
        }
        if (ci == null) {
            flags.add(CashItemFlag.CheckQuestID2);
        }
    }

    public void setMainItem(boolean main) {
        this.main = main;
    }

    public boolean isMainItem() {
        return main;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public boolean sameAsCashItem() {
        return flags.isEmpty();
    }
}
