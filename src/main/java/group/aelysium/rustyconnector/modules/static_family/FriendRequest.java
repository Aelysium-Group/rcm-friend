package group.aelysium.rustyconnector.modules.static_family;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class FriendRequest {
    private final FriendRegistry registry;
    private final UUID sender;
    private final UUID target;
    private final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);
    private final Instant issuedAt = Instant.now();

    protected FriendRequest(
            @NotNull FriendRegistry registry,
            @NotNull UUID sender,
            @NotNull UUID target
    ) {
        this.registry = registry;
        this.sender = sender;
        this.target = target;
    }

    public UUID sender() {
        return this.sender;
    }
    public UUID target() {
        return this.target;
    }
    public Status status() {
        return this.status.get();
    }
    public boolean expired() {
        return this.issuedAt.plusSeconds(60).isBefore(Instant.now());
    }

    public void accept() {
        if(!this.status.get().equals(Status.PENDING)) return;

        this.registry.friends.computeIfAbsent(this.sender, u -> new HashSet<>()).add(this.target);
        this.registry.friends.computeIfAbsent(this.target, u -> new HashSet<>()).add(this.sender);

        this.status.set(Status.ACCEPTED);

        this.registry.requests.remove(this.target);
    }
    public void ignore() {
        if(!this.status.get().equals(Status.PENDING)) return;

        this.status.set(Status.IGNORED);

        this.registry.requests.remove(this.target);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FriendRequest that = (FriendRequest) o;
        return Objects.equals(sender, that.sender) && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, target);
    }

    public enum Status {
        PENDING,
        ACCEPTED,
        IGNORED
    }
}