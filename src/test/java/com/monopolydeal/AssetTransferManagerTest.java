package com.monopolydeal;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.logic.AssetTransferManager;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AssetTransferManager – canAfford, optimal asset selection, payment execution.
 * Covers Sequence Diagrams 6 (Debt Collector) and 7 (Rent).
 */
class AssetTransferManagerTest {

    private AssetTransferManager atm;

    @BeforeEach
    void setUp() {
        Deck.reset();
        atm = new AssetTransferManager();
    }

    // --- canAfford ---

    @Test
    void testCanAffordWhenBankIsEnough() {
        Player payer = new Player("1", "Payer");
        payer.getBankArea().add(new MoneyCard("5M", 5, 5));
        assertTrue(atm.canAfford(payer, 5));
        assertTrue(atm.canAfford(payer, 3));
    }

    @Test
    void testCanAffordFalseWhenBankInsufficient() {
        Player payer = new Player("1", "Payer");
        payer.getBankArea().add(new MoneyCard("2M", 2, 2));
        assertFalse(atm.canAfford(payer, 5));
    }

    @Test
    void testCanAffordWithMixedAssets() {
        Player payer = new Player("1", "Payer");
        payer.getBankArea().add(new MoneyCard("2M", 2, 2));
        payer.getPropertyArea().add(new PropertyCard("Brown1", 1, PropertyType.BROWN, false));
        assertTrue(atm.canAfford(payer, 3), "Mixed bank+property assets must count toward affordability");
    }

    @Test
    void testCanAffordNullPlayerReturnTrue() {
        assertTrue(atm.canAfford(null, 5), "Null player should return true (nothing blocks payment)");
    }

    // --- selectOptimalPaymentAssets ---

    @Test
    void testSelectOptimalChoosesSmallestSufficientCards() {
        MoneyCard m1 = new MoneyCard("1M", 1, 1);
        MoneyCard m2 = new MoneyCard("2M", 2, 2);
        MoneyCard m5 = new MoneyCard("5M", 5, 5);
        List<com.monopolydeal.model.card.Card> assets = List.of(m1, m2, m5);

        List<com.monopolydeal.model.card.Card> selected = atm.selectOptimalPaymentAssets(assets, 3);
        int total = selected.stream().mapToInt(com.monopolydeal.model.card.Card::getValue).sum();
        assertTrue(total >= 3, "Selected assets must cover the required amount");
    }

    @Test
    void testSelectOptimalReturnsAllWhenInsufficientAssets() {
        // New rule: when assets cannot fully cover the amount, return all assets (pay everything you have)
        MoneyCard m1 = new MoneyCard("1M", 1, 1);
        List<com.monopolydeal.model.card.Card> assets = List.of(m1);
        List<com.monopolydeal.model.card.Card> selected = atm.selectOptimalPaymentAssets(assets, 5);
        assertFalse(selected.isEmpty(), "selectOptimalPaymentAssets must return all assets when total is insufficient");
        assertEquals(1, selected.get(0).getValue(), "Must return the only available card");
    }

    @Test
    void testSelectOptimalWithEmptyList() {
        List<com.monopolydeal.model.card.Card> selected = atm.selectOptimalPaymentAssets(List.of(), 5);
        assertTrue(selected.isEmpty());
    }

    // --- processPayment ---

    @Test
    void testProcessPaymentSuccessTransfersAssets() {
        Player payer = new Player("1", "Payer");
        Player receiver = new Player("2", "Receiver");
        payer.getBankArea().add(new MoneyCard("5M", 5, 5));

        AssetTransferManager.PaymentResult result =
                atm.processPayment(payer, receiver, 5, AssetTransferManager.PaymentMode.USE_MONEY_ONLY);

        assertTrue(result.isSuccess(), "Payment must succeed when payer has enough");
        assertTrue(result.getTotalPaid() >= 5);
        assertEquals(0, payer.getBankArea().total(), "Payer's bank must be depleted after payment");
        assertEquals(5, receiver.getBankArea().total(), "Receiver must gain the paid amount");
    }

    @Test
    void testProcessPaymentPartialWhenCannotAfford() {
        // Rule: if payer cannot cover the full amount they pay everything they have (no failure)
        Player payer = new Player("1", "Payer");
        Player receiver = new Player("2", "Receiver");
        payer.getBankArea().add(new MoneyCard("1M", 1, 1));

        AssetTransferManager.PaymentResult result =
                atm.processPayment(payer, receiver, 10, AssetTransferManager.PaymentMode.USE_MONEY_ONLY);

        assertTrue(result.isSuccess(), "Payment must succeed even when payer cannot cover full amount (pays all they have)");
        assertEquals(0, payer.getBankArea().total(), "Payer must hand over all assets even when insufficient");
        assertEquals(1, receiver.getBankArea().total(), "Receiver must get whatever the payer had");
    }

    @Test
    void testProcessPaymentNullPayerFails() {
        Player receiver = new Player("2", "Receiver");
        AssetTransferManager.PaymentResult result =
                atm.processPayment(null, receiver, 5, AssetTransferManager.PaymentMode.USE_MIXED);
        assertFalse(result.isSuccess());
    }

    @Test
    void testProcessPaymentNullReceiverFails() {
        Player payer = new Player("1", "Payer");
        AssetTransferManager.PaymentResult result =
                atm.processPayment(payer, null, 5, AssetTransferManager.PaymentMode.USE_MIXED);
        assertFalse(result.isSuccess());
    }

    @Test
    void testNoChangeRule() {
        Player payer = new Player("1", "Payer");
        Player receiver = new Player("2", "Receiver");
        // Payer has only a 5M card but owes 3M
        payer.getBankArea().add(new MoneyCard("5M", 5, 5));

        AssetTransferManager.PaymentResult result =
                atm.processPayment(payer, receiver, 3, AssetTransferManager.PaymentMode.USE_MONEY_ONLY);

        assertTrue(result.isSuccess());
        // The 5M card is transferred – no change given
        assertEquals(5, result.getTotalPaid(), "No-change rule: full card value must be transferred");
        assertEquals(0, payer.getBankArea().total(), "Payer must hand over the entire card, no change returned");
    }
}
