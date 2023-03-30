package com.unrealdinnerbone.webby;

import com.unrealdinnerbone.config.ConfigCreator;
import com.unrealdinnerbone.config.ConfigManager;
import com.unrealdinnerbone.config.config.IntegerConfig;
import com.unrealdinnerbone.javalinutils.InfluxConfig;
import com.unrealdinnerbone.javalinutils.InfluxPlugin;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.exception.WebResultException;
import com.unrealdinnerbone.unreallib.web.ContentType;
import com.unrealdinnerbone.unreallib.web.HttpHelper;
import io.javalin.Javalin;
import org.slf4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Webby {
    private static final Logger LOGGER = LogHelper.getLogger();
    
    public static void main(String[] args) {
        ConfigManager configManager = ConfigManager.createSimpleEnvPropertyConfigManger();
        Config config = configManager.loadConfig("config", Config::new);
        List<Integer> sentHashes = new ArrayList<>();
        InfluxConfig influxConfig = configManager.loadConfig("influx", InfluxConfig::new);
        Javalin javalin = Javalin.create(javalinConfig -> {
            javalinConfig.plugins.register(new InfluxPlugin(influxConfig));
        }).start(config.port.getValue());
        javalin.get("/", ctx -> ctx.result("Website Online"));
        javalin.post("/v1/discord/{id}/{token}", ctx -> {
            String id = ctx.pathParam("id");
            String token = ctx.pathParam("token");
            String body = ctx.body();
            int o = body.hashCode();
            if(sentHashes.contains(o)) {
                LOGGER.info("Skipping Send Discord Webhook with id: {}", o);
                ctx.status(200).result("{\"message\": \"Skipped\"}");
            }else {
                try {
                    String s = HttpHelper.postOrThrow(URI.create("https://discord.com/api/webhooks/" + id + "/" + token), body, ContentType.JSON);
                    sentHashes.add(o);
                    LOGGER.info("Send Discord Webhook with id: {}", o);
                    ctx.status(200).result(s);
                }catch (WebResultException e) {
                    LOGGER.info("Failed to send Discord Webhook with id: {}", o, e);
                    ctx.status(500).result(e.getBody());
                }
            }
        });
    }


    public static class Config {
        public IntegerConfig port;
        public Config(ConfigCreator configCreator) {
            port = configCreator.createInteger("port", 9595);
        }
    }
}