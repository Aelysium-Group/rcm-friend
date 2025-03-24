package group.aelysium.rustyconnector.modules.friend.events;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.modules.friend.FriendRegistry;
import group.aelysium.rustyconnector.proxy.events.NetworkPostJoinEvent;

import java.util.Optional;

public class OnConnect {
    @EventListener
    public void handle(NetworkPostJoinEvent event) {
        try {
            Optional.ofNullable(RC.Kernel().fetchModule("FriendRegistry")).ifPresent(f->f.ifPresent(p -> {
                try {
                    ((FriendRegistry) p).fetchFriends(event.player.id());
                } catch (Exception e) {
                    RC.Error(Error.from(e).whileAttempting("To get "+event.player.username()+"'s friends.").detail("User ID", event.player.id()));
                }
            }));
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To get "+event.player.username()+"'s friends.").detail("User ID", event.player.id()));
        }
    }
}
