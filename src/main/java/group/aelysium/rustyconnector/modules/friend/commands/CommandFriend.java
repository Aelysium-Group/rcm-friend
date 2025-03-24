package group.aelysium.rustyconnector.modules.friend.commands;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.util.CommandClient;
import group.aelysium.rustyconnector.modules.friend.FriendRegistry;
import group.aelysium.rustyconnector.modules.friend.FriendRequest;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

public final class CommandFriend {
    public static void register(CommandManager<CommandClient> manager, String alias) {
        manager.command(
            manager.commandBuilder(alias)
                .permission("rustyconnector.command.friend")
            .senderType(CommandClient.Player.class)
            .handler(context -> {
                try {
                    context.sender().send(RC.Lang("rustyconnector-waiting").generate());
                    
                    RC.Lang("rcm-friends-controlBoard").generate(context.sender().id());
                } catch (Exception e) {
                    RC.Error(Error.from(e).whileAttempting("To provide a player their list of friends.").detail("Player ID", context.sender().id()));
                }
            })
            
            .literal("add")
            .required("username", stringParser(), (context, input) -> {
                try {
                    return CompletableFuture.completedFuture(RC.P.Players()
                        .dump()
                        .stream()
                        .filter(Player::online)
                        .map(player -> Suggestion.suggestion(player.username()))
                        .toList());
                } catch (Exception e) {
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
                    if(currentFriends.contains(targetPlayer.id())) {
                        context.sender().send(text("You're already friends with "+targetUsername));
                        return;
                    }
                    
                    if(currentFriends.size() >= friends.config().maxFriends) {
                        context.sender().send(text("You've reached the max number of friends you're allowed to have."));
                        return;
                    }
                    
                    friends.sendFriendRequest(context.sender().id(), targetPlayer.id());
                    
                    LiquidTimestamp expiration = friends.config().requestExpiration();
                    context.sender().send(text("Your friend request was sent to "+targetUsername+"! It will expire in "+expiration.value()+" "+expiration.unit().name().toLowerCase()));
                } catch (Exception e) {
                    RC.Error(Error.from(e).whileAttempting("To send a friend request.").detail("Request Sender", context.sender().username()).detail("Request Target", targetUsername));
                    context.sender().send(RC.Lang("rustyconnector-internalError").generate());
                }
            })
            
            .literal("requests")
            .required("username", stringParser(), (context, input) -> {
                try {
                    return CompletableFuture.completedFuture(
                        ((FriendRegistry) RC.Module("Friends"))
                            .fetchFriendRequests(context.sender().id())
                            .stream()
                            .filter(r -> !r.expired() && r.status().equals(FriendRequest.Status.PENDING))
                            .map(r -> Suggestion.suggestion(r.senderID()))
                            .toList()
                    );
                } catch (Exception ignore) {
                    return CompletableFuture.completedFuture(List.of());
                }
            })
            .required("action", stringParser(), (context, input) -> CompletableFuture.completedFuture(List.of(Suggestion.suggestion("accept"), Suggestion.suggestion("ignore"))))
            .handler(context -> {
                String username = context.get("username");
                String action = context.get("action");
                
                Player senderPlayer = RC.P.PlayerFromUsername(username).orElse(null);
                if (senderPlayer == null) {
                    context.sender().send(text("There's no player with that username"));
                    return;
                }
                
                FriendRegistry friends = RC.Module("Friends");
                if(friends == null) {
                    context.sender().send(RC.Lang("rustyconnector-internalError").generate());
                    return;
                }
                
                Set<FriendRequest> requests = friends.fetchFriendRequests(context.sender().id());
                FriendRequest request = requests.stream().filter(r -> r.senderID().equalsIgnoreCase(username)).findAny().orElse(null);
                if(request == null) {
                    context.sender().send(text("You don't have any pending friend requests from "+username));
                    return;
                }
                if(request.expired() || !request.status().equals(FriendRequest.Status.PENDING)) {
                    context.sender().send(text("This friend request is expired."));
                    return;
                }
                
                if(action.equalsIgnoreCase("ignore"))
                    request.ignore();
                else if(action.equalsIgnoreCase("accept"))
                    request.accept();
                else
                    context.sender().send(text("You must either accept or ignore the request."));
            })
            .build()
        );
    }
}
