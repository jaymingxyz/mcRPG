package com.gmail.nossr50.config.party;

import java.io.File;

/**
 * mcRPG has no party system. This class keeps mcMMO's party checks compiling, and every one of
 * them sees parties as disabled. No party.yml is created, because there is nothing to configure.
 */
public class PartyConfig {
    /**
     * @param dataFolder ignored; kept so callers written for mcMMO's file-backed config compile
     */
    public PartyConfig(File dataFolder) {
    }

    public boolean isPartyEnabled() {
        return false;
    }
}
