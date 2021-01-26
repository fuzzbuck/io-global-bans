package example;

import arc.*;
import arc.util.*;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Main extends Plugin{

    public static String endpoint = "https://mindustry.io/api/v1/info";

    public Main(){
        Log.info("<io> Global bans loaded: ");
        Log.info(" - Enabled: " + Core.settings.getBool("io-bans-enabled", true));
        Log.info(" - Ban Mode: " + Core.settings.getBool("io-bans-strict", false));

        //listen for a block selection event
        Events.on(PlayerConnect.class, event -> {
            Log.info(event.player.name + " joining");
            if(Core.settings.getBool("io-bans-enabled", true)){
                CompletableFuture.runAsync(() -> {
                    Log.info("checking");
                    try{
                        HttpRequest req = HttpRequest.post(endpoint);
                        req.contentType("application/x-www-form-urlencoded");
                        req.form("uuid", event.player.uuid());
                        // req.parameter("uuid", event.player.uuid());

                        int code = req.code();
                        String pjson = req.body();

                        // java is such a terrible language
                        JsonObject json = new Gson().fromJson(pjson, JsonObject.class);

                        if (json.has("banneduntil")){
                            if(json.get("banneduntil").getAsLong() > Instant.now().getEpochSecond()){
                                if(Core.settings.getBool("io-bans-strict", false))
                                    Vars.netServer.admins.banPlayer(event.player.uuid());

                                event.player.con.kick(json.get("banreason").getAsString());
                                Log.info("<io> Kicked " + event.player.name + " since he is banned on io.");
                            }
                        }

                    }catch(Exception e){
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("io-bans-enabled", "Toggle the global io bans plugin.", args -> {
            Core.settings.put("io-bans-enabled", !Core.settings.getBool("io-bans-enabled", true));
            Core.settings.forceSave();
            Log.info("<io> " + (Core.settings.getBool("io-bans-enabled", true) ? "enabled" : "disabled") + " io global bans.");
        });
        handler.register("io-bans-strict", "Toggle also banning players if they are banned on io (RECOMMENDED TO KEEP DISABLED IN CASE THEY APPEAL SUCCESSFULLY).", args -> {
            Core.settings.put("io-bans-strict", !Core.settings.getBool("io-bans-strict", false));
            Core.settings.forceSave();
            Log.info("<io> " + (Core.settings.getBool("io-bans-strict", false) ? "enabled" : "disabled") + " io strict mode.");
        });
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){

    }
}
