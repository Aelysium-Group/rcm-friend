package group.aelysium.rustyconnector.modules.friend;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.proxy.events.NetworkPostJoinEvent;

import java.util.Optional;

class OnConnect {
    @EventListener
    public void handle(NetworkPostJoinEvent event) {
        try {
            Optional.ofNullable(RC.Kernel().fetchModule("FriendRegistry")).ifPresent(f->f.ifPresent(p -> {
                try {
                    ((FriendRegistry) p).cacheFor(event.player.uuid());
                } catch (Exception e) {
                    RC.Error(Error.from(e).whileAttempting("To get "+event.player.username()+"'s friends.").detail("User UUID", event.player.uuid()));
                }
            }));
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To get "+event.player.username()+"'s friends.").detail("User UUID", event.player.uuid()));
        }
    }
}
