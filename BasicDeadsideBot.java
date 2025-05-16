import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class BasicDeadsideBot extends ListenerAdapter {

    private static JDA jda;
    private final Map<String, String> factions = new HashMap<>();

    public static void main(String[] args) {
        try {
            // Get the Discord token from environment
            String token = System.getenv("DISCORD_TOKEN");
            if (token == null || token.isEmpty()) {
                System.err.println("DISCORD_TOKEN environment variable not set!");
                System.exit(1);
            }

            System.out.println("Starting Deadside Discord Bot...");

            // Create JDA instance
            JDABuilder builder = JDABuilder.createDefault(token);
            builder.setActivity(Activity.playing("Deadside"));
            builder.enableIntents(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
            );
            builder.addEventListeners(new BasicDeadsideBot());
            
            jda = builder.build();
            jda.awaitReady();
            
            System.out.println("Deadside Bot is now connected to Discord!");
            System.out.println("Connected to " + jda.getGuilds().size() + " servers");
            
        } catch (Exception e) {
            System.err.println("Error starting the bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Bot is ready! Connected to " + jda.getGuilds().size() + " guilds");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        
        Message message = event.getMessage();
        String content = message.getContentRaw();
        MessageChannel channel = event.getChannel();
        
        if (content.equals("!ping")) {
            long time = System.currentTimeMillis();
            channel.sendMessage("Pong!").queue(response -> {
                response.editMessage("Pong: " + (System.currentTimeMillis() - time) + "ms").queue();
            });
        }
        else if (content.equals("!info")) {
            Guild guild = event.getGuild();
            String serverInfo = "**Deadside Bot Info**\n" +
                    "Connected to: " + jda.getGuilds().size() + " servers\n" +
                    "Current Server: " + guild.getName() + "\n" +
                    "Members: " + guild.getMemberCount() + "\n" +
                    "Created: " + guild.getTimeCreated().toString();
            
            channel.sendMessage(serverInfo).queue();
        }
        else if (content.equals("!help")) {
            String helpText = "**Deadside Bot Commands**\n\n" +
                    "- `!ping` - Check bot response time\n" +
                    "- `!info` - Display server information\n" +
                    "- `!help` - Show this help message\n" +
                    "- `!faction create <name> <tag> <description>` - Create a faction\n" +
                    "- `!faction list` - List all factions\n";
            
            channel.sendMessage(helpText).queue();
        }
        else if (content.startsWith("!faction create ")) {
            // Parse command arguments
            String[] args = content.substring("!faction create ".length()).split(" ", 3);
            if (args.length < 2) {
                channel.sendMessage("Usage: !faction create <name> <tag> [description]").queue();
                return;
            }
            
            String name = args[0];
            String tag = args[1];
            String description = args.length > 2 ? args[2] : "No description";
            
            // Add faction
            if (factions.containsKey(name.toLowerCase())) {
                channel.sendMessage("A faction with that name already exists!").queue();
                return;
            }
            
            factions.put(name.toLowerCase(), tag + "|" + description + "|" + event.getAuthor().getId());
            channel.sendMessage("Faction **" + name + "** [" + tag + "] created successfully!").queue();
        }
        else if (content.equals("!faction list")) {
            if (factions.isEmpty()) {
                channel.sendMessage("No factions have been created yet.").queue();
                return;
            }
            
            StringBuilder list = new StringBuilder("**Available Factions:**\n\n");
            for (Map.Entry<String, String> entry : factions.entrySet()) {
                String[] data = entry.getValue().split("\\|");
                String tag = data[0];
                String description = data[1];
                
                list.append("- **").append(entry.getKey()).append("** [").append(tag).append("]: ");
                list.append(description).append("\n");
            }
            
            channel.sendMessage(list.toString()).queue();
        }
    }
}