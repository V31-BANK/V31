package org.v31bank.core.money;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Asset}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class AssetTests {

    @Test
    void knowsWhatEachAssetDividesInto() {
        assertEquals(2, Asset.USD.scale());
        assertEquals(0, Asset.JPY.scale());
        assertEquals(8, Asset.BTC.scale());
        assertEquals(18, Asset.ETH.scale());
        assertEquals(6, Asset.USDC.scale());
    }

    @Test
    void separatesOnChainAssetsFromFiat() {
        assertFalse(Asset.USD.isOnChain());
        assertTrue(Asset.BTC.isOnChain());
        assertTrue(Asset.USDC.isOnChain());
        assertEquals(AssetType.STABLECOIN, Asset.USDC.type());
    }

    @Test
    void normalisesTheCode() {
        assertEquals(Asset.BTC, new Asset(" btc ", AssetType.CRYPTO, 8));
        assertEquals(Asset.BTC, Asset.of("btc"));
    }

    @Test
    void rejectsCodesThatAreNotTickers() {
        assertThrows(IllegalArgumentException.class, () -> new Asset("B", AssetType.CRYPTO, 8));
        assertThrows(IllegalArgumentException.class, () -> new Asset("BTC-USD", AssetType.CRYPTO, 8));
        assertThrows(IllegalArgumentException.class, () -> new Asset("", AssetType.CRYPTO, 8));
    }

    @Test
    void rejectsImpossiblePrecision() {
        assertThrows(IllegalArgumentException.class, () -> new Asset("XYZ", AssetType.CRYPTO, -1));
        assertThrows(IllegalArgumentException.class, () -> new Asset("XYZ", AssetType.CRYPTO, Asset.MAX_SCALE + 1));
    }

    @Test
    void allowsATokenItDoesNotList() {
        Asset token = new Asset("PEPE", AssetType.CRYPTO, 18);
        assertEquals("PEPE", token.code());
        assertFalse(Asset.known().contains(token));
    }

    @Test
    void refusesToGuessAtAnUnknownCode() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Asset.of("PEPE"));
        assertTrue(ex.getMessage().contains("No asset is known by the code 'PEPE'"), ex.getMessage());
        assertEquals(Optional.empty(), Asset.find("PEPE"));
        assertEquals(Optional.empty(), Asset.find(null));
    }

    @Test
    void treatsPrecisionAsPartOfIdentity() {
        assertNotEquals(Asset.USDC, new Asset("USDC", AssetType.STABLECOIN, 18));
    }

    @Test
    void reportsItsSmallestUnit() {
        assertEquals(new BigDecimal("0.00000001"), Asset.BTC.minorUnit());
        assertEquals(BigDecimal.ONE, Asset.JPY.minorUnit());
    }

    @Test
    void readsAsItsTicker() {
        assertEquals("BTC", Asset.BTC.toString());
    }

}
