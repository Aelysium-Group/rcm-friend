package group.aelysium.rustyconnector.modules.friend;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.lang.Lang;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class FriendLang {
    @Lang("rcm-friends-controlBoard")
    public static Component controlBoard(String playerID) {
        FriendRegistry friends;
        try {
            friends = (FriendRegistry) RC.ModuleFlux("Friends").get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To fetch the friends for userID: "+playerID));
            return text("There was an internal issue fetching your friends.", RED);
        }
        
        
        boolean isPartyEnabled = false;
        try {
            // api.services().party().orElseThrow();
            isPartyEnabled = true;
        } catch (Exception ignore) {}
        boolean finalIsPartyEnabled = isPartyEnabled;
        
        Set<String> friendIDs = friends.fetchFriends(playerID);
        if(friendIDs.isEmpty())
            return text("You don't have any friends. Click here to send a friend request.", YELLOW).clickEvent(ClickEvent.suggestCommand("/"+friends.config.friendCommandAlias+" add "));
        
        Set<Player> players = new HashSet<>();
        try {
            friendIDs.forEach(i -> {
                try {
                    players.add(RC.P.PlayerFromID(i).orElseThrow());
                } catch (NoSuchElementException ignore) {}
            });
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To fetch the friends for userID: "+playerID));
            return text("There was an internal issue fetching your friends.", RED);
        }
        
        return join(
            newlines(),
            join(
                spaces(),
                text("--------------", GRAY),
                text("Friends ", WHITE).append(text("(", DARK_GRAY)).append(text(friendIDs.size(), GRAY)).append(text("/"+friends.config.maxFriends+")", DARK_GRAY)),
                text("[+]", GREEN).hoverEvent(HoverEvent.showText(text("Click to add a new friend"))).clickEvent(ClickEvent.suggestCommand("/"+friends.config.friendCommandAlias+" add ")),
                text("--------------", GRAY)
            ),
            join(
                newlines(),
                players.stream().map(p -> join(
                    separator(empty()),
                    text("[x]", RED).hoverEvent(HoverEvent.showText(text("Click to unfriend."))).clickEvent(ClickEvent.runCommand("/"+friends.config.unfriendCommandAlias+" " + p.username())),
                    friends.config.social_messagingEnabled ? text("[m]", YELLOW).hoverEvent(HoverEvent.showText(text("Click to message "+p.username()))).clickEvent(ClickEvent.suggestCommand("/"+friends.config.social_friendMessageAlias+" " + p.username() + " ")) : empty(),
                    // text("[p]", BLUE).hoverEvent(HoverEvent.showText(resolver().get("proxy.friends.panel.invite_party", LanguageResolver.tagHandler("username",friend.username())))).clickEvent(ClickEvent.runCommand("/party invite " + friend.username() + " "))
                    space(),
                    p.online() ?
                        friends.config.social_showFamily ?
                            text(p.username(), WHITE).hoverEvent(HoverEvent.showText(text("Currently Playing on: ", WHITE).append(text(Objects.requireNonNull(p.family().orElseThrow().displayName()), LIGHT_PURPLE))))
                            :
                            text(p.username(), WHITE).hoverEvent(HoverEvent.showText(text("Online", WHITE)))
                        :
                        text(p.username(), GRAY).hoverEvent(HoverEvent.showText(text("Offline", GRAY)))
                )).toList()
            )
        );
    };
}
