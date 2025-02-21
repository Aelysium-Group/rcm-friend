package group.aelysium.rustyconnector.modules.static_family;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.proxy.events.NetworkLeaveEvent;

import java.util.Optional;

public class OnDisconnect {
    @EventListener
    public void handle(NetworkLeaveEvent event) {
        try {
            Optional.ofNullable(RC.Kernel().fetchModule("FriendRegistry")).ifPresent(f->f.executeNow(p -> {
                ((FriendRegistry) p).clearCacheFor(event.player().uuid());
            }));
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
    }
}
