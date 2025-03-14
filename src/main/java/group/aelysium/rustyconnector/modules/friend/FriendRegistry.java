package group.aelysium.rustyconnector.modules.friend;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.haze.HazeDatabase;
import group.aelysium.rustyconnector.common.modules.ExternalModuleBuilder;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.DataHolder;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filter;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Type;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.CreateRequest;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.ReadRequest;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FriendRegistry implements Module {
    private static final String FRIENDS_TABLE = "friends";

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
    protected final FriendConfig config;
    protected final Flux<HazeDatabase> database;
    protected final Map<UUID, FriendRequest> requests = new ConcurrentHashMap<>();
    protected final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();

    public FriendRegistry(
            @NotNull FriendConfig config
    ) throws Exception {
        this.config = config;

        this.database = RC.P.Haze().fetchDatabase(this.config.database);
        if(this.database == null) throw new NoSuchElementException("No database exists on the haze provider with the name '"+this.config.database+"'.");
        HazeDatabase db = this.database.get(1, TimeUnit.MINUTES);

        this.cleaner.schedule(this::clean, 10, TimeUnit.MINUTES);
        if(db.doesDataHolderExist(FRIENDS_TABLE)) return;

        DataHolder table = new DataHolder(FRIENDS_TABLE);
        Map<String, Type> columns = Map.of(
                "player_uuid", Type.STRING(36).nullable(false),
                "server_id", Type.STRING(64).nullable(false),
                "family_id", Type.STRING(16).nullable(false),
                "last_joined", Type.DATETIME().nullable(false)
        );
        columns.forEach(table::addKey);
        db.createDataHolder(table);
    }

    private void clean() {
        try {
            Set<UUID> expired = new HashSet<>();
            this.requests.forEach((u, i) -> {
                if(i.expired()) expired.add(u);
            });
            expired.forEach(this.requests::remove);
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To clear out expired party invitations."));
        }

        this.cleaner.schedule(this::clean, 20, TimeUnit.SECONDS);
    }

    public void clearCacheFor(@NotNull UUID player) {
        this.friends.remove(player).clear();
    }
    public @NotNull Set<UUID> fetch(@NotNull UUID player) {
        return Optional.ofNullable(this.friends.get(player)).orElse(Set.of());
    }

    public @NotNull FriendRequest sendFriendRequest(@NotNull UUID from, @NotNull UUID to) {
        FriendRequest request = new FriendRequest(this, from, to);
        this.requests.put(to, request);
        return request;
    }

    protected @NotNull Set<UUID> cacheFor(@NotNull UUID player) throws Exception {
        HazeDatabase db = this.database.get(5, TimeUnit.SECONDS);
        ReadRequest sp = db.newReadRequest(FRIENDS_TABLE);
        sp.withFilter(
            Filter
                .by("player1_uuid", new Filter.Value(player, Filter.Qualifier.EQUALS))
                .OR("player2_uuid", new Filter.Value(player, Filter.Qualifier.EQUALS))
        );
        Set<FriendsDTO> response = sp.execute(FriendsDTO.class);

        Set<UUID> friends = new HashSet<>();
        response.forEach(e -> {
            friends.add(e.player1_uuid());
            friends.add(e.player2_uuid());
        });
        friends.remove(player);

        this.friends.put(player, friends);
        return friends;
    }

    protected void createFriendEntry(@NotNull UUID player1, @NotNull UUID player2) throws Exception {
        UUID[] uuids = new UUID[2];
        if (player1.compareTo(player2) <= 0) {
            uuids[0] = player1;
            uuids[1] = player2;
        } else {
            uuids[0] = player2;
            uuids[1] = player1;
        }

        HazeDatabase db = this.database.get(5, TimeUnit.SECONDS);
        CreateRequest sp = db.newCreateRequest(FRIENDS_TABLE);
        sp.parameter("player1_uuid", uuids[0]);
        sp.parameter("player2_uuid", uuids[1]);
        sp.execute();
    }

    @Override
    public @Nullable Component details() {
        return null;
    }

    @Override
    public void close() throws Exception {
        this.friends.forEach((k,v)->v.clear());
        this.friends.clear();
        this.cleaner.close();
    }

    public static class Builder extends ExternalModuleBuilder<FriendRegistry> {
        public void bind(@NotNull ProxyKernel kernel, @NotNull FriendRegistry instance) {
            kernel.fetchModule("EventManager").onStart(e->{
                ((EventManager) e).listen(new OnConnect());
                ((EventManager) e).listen(new OnDisconnect());
            });
        }

        @NotNull
        @Override
        public FriendRegistry onStart(@NotNull Path dataDirectory) throws Exception {
            return new FriendRegistry(FriendConfig.New());
        }
    }

    public record FriendsDTO (
        int id,
        UUID player1_uuid,
        UUID player2_uuid
    ) {}
}