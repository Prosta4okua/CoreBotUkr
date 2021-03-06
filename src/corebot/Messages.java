package corebot;

import arc.*;
import arc.util.*;
import arc.util.serialization.*;
import corebot.Net.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.utils.*;
import net.dv8tion.jda.api.utils.cache.*;

import java.awt.*;
import java.time.*;
import java.util.Timer;
import java.util.*;

import static corebot.CoreBot.*;

public class Messages extends ListenerAdapter {
    JDA jda;
    TextChannel channel;
    User lastUser;
    Message lastMessage;
    Message lastSentMessage;
    Guild guild;
    Color normalColor = Color.decode("#FAB462");
    Color errorColor = Color.decode("#ff3838");

    public Messages() {
        String token = System.getenv("CORE_BOT_TOKEN");
        Log.info("Found token: @", token != null);

        try {
            jda = JDABuilder
                    .createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_EMOJIS,
                            GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                    .setMemberCachePolicy(MemberCachePolicy.ALL).disableCache(CacheFlag.VOICE_STATE).build();
            jda.awaitReady();
            jda.addEventListener(this);
            guild = jda.getGuildById(guildID);

            Log.info("Discord bot up.");
            Core.net = new arc.Net();

            // mod listings are broken until further notice
            // the format is incompatible and should be enabled with the v6 update
            /*
             * //mod list updater
             * Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
             * Core.net.httpGet(
             * "https://raw.githubusercontent.com/Anuken/MindustryMods/master/mods.json",
             * response -> { if(response.getStatus() != HttpStatus.OK){ return; }
             * 
             * Seq<ModListing> listings = json.fromJson(Array.class, ModListing.class,
             * response.getResultAsString()); listings.sort(Structs.comparing(list ->
             * Date.from(Instant.parse(list.lastUpdated)))); listings.reverse();
             * listings.truncate(20); listings.reverse();
             * 
             * EmbedBuilder embed = new EmbedBuilder(); embed.setColor(normalColor);
             * embed.setTitle("Last Updated Mods");
             * embed.setFooter(Strings.format("Last Updated: @",
             * DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss ZZZZ").format(ZonedDateTime
             * .now()))); for(ModListing listing : listings){ embed.addField(listing.repo +
             * "  " + listing.stars + "★ | " + "*Updated " +
             * durFormat(Duration.between(Instant.parse(listing.lastUpdated),
             * Instant.now()))+ " ago*", Strings.format("**[@](@)**\n@\n\n_\n_",
             * Strings.stripColors(listing.name), "https://github.com/" + listing.repo,
             * Strings.stripColors(listing.description)), false); }
             * 
             * guild.getTextChannelById(modChannelID).editMessageById(663246057660219413L,
             * embed.build()).queue(); }, Log::err); }, 0, 20, TimeUnit.MINUTES);
             */
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String durFormat(Duration duration) {
        if (duration.toDays() > 0)
            return duration.toDays() + "d";
        if (duration.toHours() > 0)
            return duration.toHours() + "h";
        return duration.toMinutes() + "m";
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            commands.handle(event.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        StringBuilder builder = new StringBuilder();
        try {
            event.getUser().openPrivateChannel().complete()
                    .sendMessage("**Ласкаво просимо до українськомовного сервера Mindustry.**"
                            + "\n\n*Упевніться, що ви прочитали #правила і теми каналів до надсилання повідомлень.*\n\n"
                            + "Зверніть увагу, що цей сервер має іншу модерацію, ніж офіційний Mindustry Discord. Слава Україні!")
                    .queue();
        } catch (Exception ignored) {
            // may not be able to send messages to this user, ignore
        }
        builder.append("Ласкаво проси");
        builder.append(event.getUser().getId());
        builder.append("> до українськомовного сервера Mindustry!");
        guild.getTextChannelById(CoreBot.generalChannelID).sendMessage(builder.toString()).queue();
    }

    public void sendUpdate(VersionInfo info) {
        /*
         * String text = info.description; int maxLength = 2000; while(true){ String
         * current = text.substring(0, Math.min(maxLength, text.length())); guild
         * .getTextChannelById(announcementsChannelID) .sendMessage(new EmbedBuilder()
         * .setColor(normalColor).setTitle(info.name)
         * .setDescription(current).build()).queue();
         * 
         * if(text.length() < maxLength){ return; }
         * 
         * text = text.substring(maxLength); }
         */
    }

    public void deleteMessages() {
        Message last = lastMessage, lastSent = lastSentMessage;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                last.delete().queue();
                lastSent.delete().queue();
            }
        }, CoreBot.messageDeleteTime);
    }

    public void deleteMessage() {
        Message last = lastSentMessage;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                last.delete().queue();
            }
        }, CoreBot.messageDeleteTime);
    }

    public void sendCrash(JsonValue value) {

        StringBuilder builder = new StringBuilder();
        value = value.child;
        while (value != null) {
            builder.append("**");
            builder.append(value.name);
            builder.append("**");
            builder.append(": ");
            if (value.name.equals("trace")) {
                builder.append("```xl\n"); // xl formatting looks nice
                builder.append(value.asString().replace("\\n", "\n").replace("\t", "  "));
                builder.append("```");
            } else {
                builder.append(value.asString());
            }
            builder.append("\n");
            value = value.next;
        }
        guild.getTextChannelById(CoreBot.crashReportChannelID).sendMessage(builder.toString()).queue();
    }

    public void text(String text, Object... args) {
        lastSentMessage = channel.sendMessage(format(text, args)).complete();
    }

    public void info(String title, String text, Object... args) {
        MessageEmbed object = new EmbedBuilder().addField(title, format(text, args), true).setColor(normalColor)
                .build();

        lastSentMessage = channel.sendMessage(object).complete();
    }

    public void err(String text, Object... args) {
        err("Error", text, args);
    }

    public void err(String title, String text, Object... args) {
        MessageEmbed e = new EmbedBuilder().addField(title, format(text, args), true).setColor(errorColor).build();
        lastSentMessage = channel.sendMessage(e).complete();
    }

    private String format(String text, Object... args) {
        return Strings.format(text, args);
    }
}
