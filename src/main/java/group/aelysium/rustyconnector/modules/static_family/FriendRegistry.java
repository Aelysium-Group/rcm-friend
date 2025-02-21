package group.aelysium.rustyconnector.modules.static_family;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.modules.ExternalModuleTinder;
import group.aelysium.rustyconnector.common.modules.ModuleParticle;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.Database;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.DataHolder;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.DataKey;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filterable;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.query.CreateRequest;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.query.ReadRequest;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FriendRegistry implements ModuleParticle {
    private static final String FRIENDS_TABLE = "friends";

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
    protected final FriendConfig config;
    protected final Map<UUID, FriendRequest> requests = new ConcurrentHashMap<>();
    protected final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();
    protected final Flux<? extends Database> database;

    public FriendRegistry(
            @NotNull FriendConfig config
    ) throws Exception {
        this.config = config;

        this.database = RC.P.Haze().fetchDatabase(this.config.database)
                .orElseThrow(()->new NoSuchElementException("No database exists on the haze provider with the name '"+this.config.database+"'."));
        Database db = this.database.observe(1, TimeUnit.MINUTES);

        this.cleaner.schedule(this::clean, 10, TimeUnit.MINUTES);
        if(db.doesDataHolderExist(FRIENDS_TABLE)) return;

        DataHolder table = new DataHolder(FRIENDS_TABLE);
        List<DataKey> columns = List.of(
                new DataKey("player_uuid", DataKey.DataType.STRING).length(36).nullable(false),
                new DataKey("server_id", DataKey.DataType.STRING).length(64).nullable(false),
                new DataKey("family_id", DataKey.DataType.STRING).length(16).nullable(false),
                new DataKey("last_joined", DataKey.DataType.DATETIME).nullable(false)
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
            RC.Error(group.aelysium.rustyconnector.common.errors.Error.from(e).whileAttempting("To clear out expired party invitations."));
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
        Database db = this.database.observe(5, TimeUnit.SECONDS);
        ReadRequest sp = db.newReadRequest(FRIENDS_TABLE);
        sp.filters().filterBy("player1_uuid", new Filterable.FilterValue(player, Filterable.Qualifier.EQUALS));
        sp.filters().filterBy("player2_uuid", new Filterable.FilterValue(player, Filterable.Qualifier.EQUALS));
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

        Database db = this.database.observe(5, TimeUnit.SECONDS);
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

    public static class Tinder extends ExternalModuleTinder<FriendRegistry> {
        public void bind(@NotNull ProxyKernel kernel, @NotNull Particle instance) {
            kernel.fetchModule("EventManager").executeNow(e->{
                ((EventManager) e).listen(new OnConnect());
                ((EventManager) e).listen(new OnDisconnect());
            });
        }

        @NotNull
        @Override
        public FriendRegistry onStart() throws Exception {
            return new FriendRegistry(FriendConfig.New());
        }
    }

    public record FriendsDTO (
        int id,
        UUID player1_uuid,
        UUID player2_uuid
    ) {}
}