package group.aelysium.rustyconnector.modules.static_family;

import group.aelysium.declarative_yaml.DeclarativeYAML;
import group.aelysium.declarative_yaml.annotations.*;
import group.aelysium.declarative_yaml.lib.Printer;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.Gson;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

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

    @Comment({
        "#",
        "# Let your players be notified when their friends log in or log off the network.",
        "# This feature works regardless of what servers your players are on.",
        "#"
    })
    @Node(2)
    public boolean social_notifications = false;

    @Comment({
        "#",
        "# Let your players see the display name of the family their friends are playing in.",
        "# If this is disabled, players will only be able to see that their friends are online.",
        "#"
    })
    @Node(3)
    public boolean social_showFamily = false;

    @Comment({
        "############################################################",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "#                     Friend Messaging                     #",
        "#                                                          #",
        "#               ---------------------------                #",
        "# | Let your players message their friends from anywhere   #",
        "# | on your network!                                       #",
        "#                                                          #",
        "#   NOTE: This command is player only!                     #",
        "#                                                          #",
        "#               ----------------------------               #",
        "#                        Permission:                       #",
        "#                rustyconnector.command.fm                 #",
        "#               ----------------------------               #",
        "#                          Usage:                          #",
        "#                 /fm <username> <message>                 #",
        "#               ----------------------------               #",
        "#                                                          #",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "############################################################",
        "#",
        "# Are friends allowed to use /fm to message each-other on a server.",
        "# If this is disabled, nothing stops them from using another command like /msg to chat.",
        "#"
    })
    @Node(4)
    public boolean social_messagingServer = false;
    @Comment({
        "#",
        "# Are friends allowed to message each-other across servers within a family?",
        "# If enabled, the setting of 'messaging-server' will be ignored.",
        "#"
    })
    @Node(5)
    public boolean social_messagingFamily = false;
    @Comment({
        "#",
        "# Are friends allowed to message each-other across the entire network?",
        "# This setting allows players to message each-other anywhere regardless of what server or family they're on.",
        "# If enabled, the setting of 'messaging-server' and 'messaging-family' will be ignored.",
        "#"
    })
    @Node(6)
    public boolean social_messagingNetwork = false;

    public static FriendConfig New() {
        return DeclarativeYAML.From(FriendConfig.class, new Printer());
    }
}