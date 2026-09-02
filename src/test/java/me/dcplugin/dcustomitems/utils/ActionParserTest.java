package me.dcplugin.dcustomitems.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юнит-тесты разбора строк действий ActionParser.
 * Проверяют нормализацию типа (нижний регистр, дефисы = подчёркивания)
 * и отделение значения.
 */
class ActionParserTest {

    @Test
    void nullActionReturnsNull() {
        assertNull(ActionParser.splitAction(null));
    }

    @Test
    void emptyActionReturnsNull() {
        assertNull(ActionParser.splitAction(""));
        assertNull(ActionParser.splitAction("   "));
    }

    @Test
    void simpleActionKeepsValue() {
        String[] parsed = ActionParser.splitAction("effect:SPEED:10:2");
        assertNotNull(parsed);
        assertEquals("effect", parsed[0]);
        assertEquals("SPEED:10:2", parsed[1]);
    }

    @Test
    void typeIsLowercased() {
        assertEquals("message", ActionParser.splitAction("MESSAGE:hello")[0]);
    }

    @Test
    void hyphensAreNormalizedToUnderscores() {
        assertEquals("damage_mobs", ActionParser.splitAction("damage-mobs:5")[0]);
        assertEquals("effect_nearby", ActionParser.splitAction("effect-nearby:SPEED:5:1:3")[0]);
    }

    @Test
    void mixedDashUnderscoreNormalizes() {
        assertEquals("damage_mobs", ActionParser.splitAction("damage-MOBS:5")[0]);
    }

    @Test
    void actionWithoutValueHasEmptyValue() {
        String[] parsed = ActionParser.splitAction("sound");
        assertNotNull(parsed);
        assertEquals("sound", parsed[0]);
        assertEquals("", parsed[1]);
    }

    @Test
    void valueMayContainColons() {
        String[] parsed = ActionParser.splitAction("title:&6Title|&7Sub:10:40:10");
        assertEquals("title", parsed[0]);
        assertEquals("&6Title|&7Sub:10:40:10", parsed[1]);
    }
}