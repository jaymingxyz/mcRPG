package com.gmail.nossr50.mcrpg;

import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/** mcRPG's own web links, sent to players as clickable chat lines. */
public final class McRPGLinks {
    /** The mcRPG GitHub repository. plugin.yml's website must match it. */
    public static final String REPOSITORY = "https://github.com/jaymingxyz/mcRPG";
    /** The mcRPG wiki on GitHub. */
    public static final String WIKI = REPOSITORY + "/wiki";

    private McRPGLinks() {
    }

    /**
     * Sends a locale line whose {0} is the URL. Players can click the line to open the URL; the
     * console just sees the text.
     */
    public static void send(@NotNull CommandSender recipient, @NotNull String localeKey,
            @NotNull String url) {
        final Component line = LocaleLoader.getTextComponent(localeKey, url)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(
                        LocaleLoader.getTextComponent("mcRPG.Link.Hover", url)));
        mcMMO.getAudiences().sender(recipient).sendMessage(line);
    }
}
