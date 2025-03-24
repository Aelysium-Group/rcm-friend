package group.aelysium.rustyconnector.modules.friend.commands;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.util.CommandClient;
import group.aelysium.rustyconnector.modules.friend.FriendRegistry;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.proxy.player.PlayerRegistry;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

public final class CommandUnfriend {
    public static void register(CommandManager<CommandClient> manager, String alias) {
        manager.command(manager.commandBuilder(alias)
            .permission("rustyconnector.command.unfriend")
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
                                    return Suggestion.suggestion(players.fetchByID(s).orElseThrow().username());
                                } catch (Exception ignore) {}
                                return Suggestion.suggestion(s);
                            })
                            .toList()
                    );
                } catch (Exception ignore) {
                    return CompletableFuture.completedFuture(List.of());
                }
            })
            .handler(context -> {
                String targetUsername = context.get("username");
                
                try {
                    FriendRegistry friends = (FriendRegistry) RC.ModuleFlux("Friends").get(3, TimeUnit.SECONDS);
                    
                    Player targetPlayer = RC.P.PlayerFromUsername(targetUsername).orElse(null);
                    if (targetPlayer == null) {
                        context.sender().send(text("There's no player with that username"));
                        return;
                    } else if(!targetPlayer.online()) {
                        context.sender().send(text("You may only send friend requests to online players"));
                        return;
                    }
                    
                    Set<String> currentFriends = friends.fetchFriends(context.sender().id());
                    if(!currentFriends.contains(targetPlayer.id())) {
                        context.sender().send(text("You aren't friends with "+targetUsername));
                        return;
                    }
                    
                    friends.unfriend(context.sender().id(), targetPlayer.id());
                } catch (Exception e) {
                    RC.Error(Error.from(e).whileAttempting("To unfriend two players.").detail("Player1", context.sender().username()).detail("Player2", targetUsername));
                    context.sender().send(RC.Lang("rustyconnector-internalError").generate());
                }
            })
        );
    }
}
