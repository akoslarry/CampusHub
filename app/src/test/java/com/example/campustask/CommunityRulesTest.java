package com.example.campustask;

import com.example.campustask.model.CommunityRules;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommunityRulesTest {
    @Test
    public void validatesCustomServiceFields() {
        assertTrue(CommunityRules.canAddService("校车查询", "交通", "查询校车线路和发车时间"));
        assertFalse(CommunityRules.canAddService("", "交通", "查询校车线路和发车时间"));
        assertFalse(CommunityRules.canAddService("校车查询", "", "查询校车线路和发车时间"));
    }

    @Test
    public void validatesForumPostFields() {
        assertTrue(CommunityRules.canPublishPost("求推荐自习室", "晚上哪里比较安静？"));
        assertFalse(CommunityRules.canPublishPost("求推荐自习室", ""));
        assertFalse(CommunityRules.canPublishPost("", "晚上哪里比较安静？"));
    }

    @Test
    public void validatesMarketplaceItemFields() {
        assertTrue(CommunityRules.canPublishItem("二手台灯", 25, "九成新，可宿舍楼下自提", "微信 campus123"));
        assertFalse(CommunityRules.canPublishItem("", 25, "九成新，可宿舍楼下自提", "微信 campus123"));
        assertFalse(CommunityRules.canPublishItem("二手台灯", 0, "九成新，可宿舍楼下自提", "微信 campus123"));
        assertFalse(CommunityRules.canPublishItem("二手台灯", 25, "九成新，可宿舍楼下自提", ""));
    }

    @Test
    public void onlyCustomServiceIdsCanBeDeleted() {
        assertTrue(CommunityRules.isCustomServiceId("custom_12"));
        assertFalse(CommunityRules.isCustomServiceId("schedule"));
        assertFalse(CommunityRules.isCustomServiceId("custom_"));
        assertFalse(CommunityRules.isCustomServiceId(null));
    }

    @Test
    public void bidMustBeHigherThanCurrentPrice() {
        assertTrue(CommunityRules.canPlaceBid(30, 25));
        assertFalse(CommunityRules.canPlaceBid(25, 25));
        assertFalse(CommunityRules.canPlaceBid(20, 25));
        assertFalse(CommunityRules.canPlaceBid(0, 25));
    }

    @Test
    public void ownersCanManageTheirOwnContentOnly() {
        assertTrue(CommunityRules.isOwner("alice", "alice"));
        assertTrue(CommunityRules.isOwner(" alice ", "alice"));
        assertFalse(CommunityRules.isOwner("alice", "bob"));
        assertFalse(CommunityRules.isOwner("", "bob"));
    }

    @Test
    public void sellerCannotBidOnOwnItemOrSoldItem() {
        assertTrue(CommunityRules.canBidOnItem("alice", "bob", false, 50, 40));
        assertFalse(CommunityRules.canBidOnItem("alice", "alice", false, 50, 40));
        assertFalse(CommunityRules.canBidOnItem("alice", "bob", true, 50, 40));
        assertFalse(CommunityRules.canBidOnItem("alice", "bob", false, 40, 40));
    }

    @Test
    public void bidderNeedsEnoughWalletBalance() {
        assertTrue(CommunityRules.canAffordBid(60, 60));
        assertTrue(CommunityRules.canAffordBid(80, 60));
        assertFalse(CommunityRules.canAffordBid(50, 60));
        assertFalse(CommunityRules.canAffordBid(60, 0));
    }

    @Test
    public void walletRechargeAndWithdrawAmountsAreValidated() {
        assertTrue(CommunityRules.canRechargeWallet(1));
        assertFalse(CommunityRules.canRechargeWallet(0));
        assertFalse(CommunityRules.canRechargeWallet(-5));

        assertTrue(CommunityRules.canWithdrawWallet(100, 100));
        assertTrue(CommunityRules.canWithdrawWallet(100, 60));
        assertFalse(CommunityRules.canWithdrawWallet(100, 101));
        assertFalse(CommunityRules.canWithdrawWallet(100, 0));
    }

    @Test
    public void buyerMustAffordFinalMarketplacePayment() {
        assertTrue(CommunityRules.canCompleteMarketplacePayment(120, 120));
        assertTrue(CommunityRules.canCompleteMarketplacePayment(150, 120));
        assertFalse(CommunityRules.canCompleteMarketplacePayment(119, 120));
        assertFalse(CommunityRules.canCompleteMarketplacePayment(120, 0));
    }

    @Test
    public void sellerCanSettleOnlyOwnedActiveItemWithBid() {
        assertTrue(CommunityRules.canSettleSale("alice", "alice", false, 3));
        assertFalse(CommunityRules.canSettleSale("alice", "bob", false, 3));
        assertFalse(CommunityRules.canSettleSale("alice", "alice", true, 3));
        assertFalse(CommunityRules.canSettleSale("alice", "alice", false, 0));
    }

    @Test
    public void marketplaceSettlementUsesTwoPercentFee() {
        assertEquals(2, CommunityRules.platformFee(100));
        assertEquals(98, CommunityRules.sellerIncome(100));
        assertEquals(1, CommunityRules.platformFee(25));
        assertEquals(24, CommunityRules.sellerIncome(25));
    }
}
