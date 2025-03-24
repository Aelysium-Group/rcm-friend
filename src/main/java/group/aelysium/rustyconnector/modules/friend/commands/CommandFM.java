package group.aelysium.rustyconnector.modules.friend.commands;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.util.CommandClient;
import group.aelysium.rustyconnector.modules.friend.FriendRegistry;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.proxy.player.PlayerRegistry;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.Component.text;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

public final class CommandFM {
    public static void register(CommandManager<CommandClient> manager, String alias) {
        manager.command(
            manager.commandBuilder(alias)
            .permission("rustyconnector.command.fm")
            .senderType(CommandClient.Player.class)
            .required("username", stringParser(), (context, input) -> {
                try {
                    PlayerRegistry players = RC.Module("PlayerRegistry");
                    return CompletableFuture.completedFuture(
                        ((FriendRegistry) RC.Module("Friends"))
                            .fetchFriends(context.sender().id())
                            .stream()
                            .map(s -> {
                                try {
                                    return players.fetchByID(s).orElse(null);
                                } catch (Exception ignore) {}
                                return null;
                            })
                            .filter(p -> p != null && p.online())
                            .map(p->Suggestion.suggestion(p.username()))
                            .toList()
                    );
                } catch (Exception ignore) {
                    return CompletableFuture.completedFuture(List.of());
                }
            })
            .handler(context -> {
                String targetUsername = context.get("username");
                String message = context.get("message");
                
                Player targetPlayer = RC.P.PlayerFromUsername(targetUsername).orElse(null);
                if (targetPlayer == null) {
                    context.sender().send(text("There's no player with that username"));
                    return;
                } else if(!targetPlayer.online()) {
                    context.sender().send(text(targetUsername+" isn't online"));
                    return;
                }
                
                FriendRegistry friends = RC.Module("Friends");
                if(friends == null) {
                    context.sender().send(RC.Lang("rustyconnector-internalError").generate());
                    return;
                }
                
                if(!friends.fetchFriends(context.sender().id()).contains(targetPlayer.id())) {
                    context.sender().send(text("You and "+targetPlayer.username()+" aren't friends"));
                    return;
                }
                
                context.sender().send(Component.text("[you -> " + targetPlayer.username() + "]: " + message, NamedTextColor.GRAY));
                targetPlayer.message(Component.text("[" + context.sender().username() + " -> you]: " + message, NamedTextColor.GRAY)
                    .hoverEvent(HoverEvent.showText(text("Click to reply")))
                    .clickEvent(ClickEvent.suggestCommand("/fm " + context.sender().username() + " ")));
            })
            .build()
        );
    }
}