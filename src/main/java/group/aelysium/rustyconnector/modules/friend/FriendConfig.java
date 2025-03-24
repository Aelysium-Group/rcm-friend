package group.aelysium.rustyconnector.modules.friend;

import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.DeclarativeYAML;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.annotations.*;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.lib.Printer;

import java.text.ParseException;

@Namespace("rustyconnector-modules")
@Config("/rcm-friend/config.yml")
@Comment({
        "###########################################################################################################",
        "#|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "###########################################################################################################",
        "#                                                                                                         #",
        "#                      ______   ______    __    ______    __   __    _____    ______                      #",
        "#                     /\\  ___\\ /\\  == \\  /\\ \\  /\\  ___\\  /\\ \"-.\\ \\  /\\  __-. /\\  ___\\                     #",
        "#                     \\ \\  __\\ \\ \\  __<  \\ \\ \\ \\ \\  __\\  \\ \\ \\-.  \\ \\ \\ \\/\\ \\\\ \\___  \\                    #",
        "#                      \\ \\_\\    \\ \\_\\ \\_\\ \\ \\_\\ \\ \\_____\\ \\ \\_\\\\\"\\_\\ \\ \\____- \\/\\_____\\                   #",
        "#                       \\/_/     \\/_/ /_/  \\/_/  \\/_____/  \\/_/ \\/_/  \\/____/  \\/_____/                   #",
        "#                                                                                                         #",
        "#                                                                                                         #",
        "#                                            Welcome to Friends!                                          #",
        "#                                                                                                         #",
        "#                            -------------------------------------------------                            #",
        "#                                                                                                         #",
        "#                      | Allow your users to build groups of friends to hang out with!                    #",
        "#                      | Friends can then create parties and play together across your                    #",
        "#                      | RustyConnector network!                                                          #",
        "#                                                                                                         #",
        "#                            -------------------------------------------------                            #",
        "#                                                                                                         #",
        "###########################################################################################################",
        "#|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "###########################################################################################################"
})
public class FriendConfig {
    private int internalExpirationHash = 0;
    private LiquidTimestamp expirationCache = null;
    
    @Comment({
            "############################################################",
            "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
            "#                      Haze Database                       #",
            "#                                                          #",
            "#               ---------------------------                #",
            "#                                                          #",
            "# | The Friend module uses Haze Databases which            #",
            "# | are provided by RustyConnector itself.                 #",
            "#                                                          #",
            "# | You'll want to make sure you've installed the Haze     #",
            "# | Database Provider of your choice (MySQL for example)   #",
            "# | and then set the name below to be the name of          #",
            "# | the database that you've registered.                   #",
            "#                                                          #",
            "#               ---------------------------                #",
            "#                                                          #",
            "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
            "############################################################"
    })
    @Node(0)
    public String database = "default";

    @Comment({
        "#",
        "# Set the maximum number of friends a user is allowed to have.",
        "# If a user reaches this limit, they will be required to remove a friend",
        "# before they can add new ones.",
        "#"
    })
    @Node(1)
    public int maxFriends = 25;
    
    @Node
    private String friendRequestExpiration = "10 MINUTES";
    
    public LiquidTimestamp requestExpiration() {
        // Just some simple logic so that we don't have to re-parse the string every time we wanna access the liquid timestamp
        if(internalExpirationHash != friendRequestExpiration.hashCode()) {
            internalExpirationHash = friendRequestExpiration.hashCode();
            try {
                expirationCache = LiquidTimestamp.from(friendRequestExpiration);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return expirationCache;
    }
    
    @Comment({
        "############################################################",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "#                      Control Panel                       #",
        "#                                                          #",
        "#               ---------------------------                #",
        "#                                                          #",
        "# | ",
        "#                                                          #",
        "#               ---------------------------                #",
        "#                                                          #",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "############################################################",
        "",
        "#",
        "# Lets you set the name of the friend messaging command.",
        "# Usage:",
        "# /<alias> <username> <message>",
        "#"
    })
    @Node(2)
    public String friendCommandAlias = "friend";
    @Node(3)
    public String unfriendCommandAlias = "unfriend";

    @Comment({
        "#",
        "# Let your players be notified when their friends log in or log off the network.",
        "# This feature works regardless of what servers your players are on.",
        "#"
    })
    @Node(4)
    public boolean social_notifications = false;

    @Comment({
        "#",
        "# Let your players see the display name of the family their friends are playing in.",
        "# If this is disabled, players will only be able to see that their friends are online.",
        "#"
    })
    @Node(5)
    public boolean social_showFamily = false;

    @Comment({
        "############################################################",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "#                     Friend Messaging                     #",
        "#                                                          #",
        "#               ---------------------------                #",
        "#                                                          #",
        "# | Let your players message their friends from anywhere   #",
        "# | on your network!                                       #",
        "#                                                          #",
        "#   NOTE: This command is player only!                     #",
        "#                                                          #",
        "#               ---------------------------                #",
        "#                                                          #",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "############################################################",
        "",
        "#",
        "# Lets you set the name of the friend messaging command.",
        "# Usage:",
        "# /<alias> <username> <message>",
        "#"
    })
    @Node(6)
    public String social_friendMessageAlias = "fm";
    
    @Comment({
        "#",
        "# Are friends allowed to use /fm to message each-other.",
        "#"
    })
    @Node(7)
    public boolean social_messagingEnabled = true;

    public static FriendConfig New() throws ParseException {
        FriendConfig config = DeclarativeYAML.From(FriendConfig.class, new Printer());
        LiquidTimestamp.from(config.friendRequestExpiration);
        return config;
    }
}