package gg.minecrush.reactions;

import gg.minecrush.reactions.storage.yaml.Messages;
import gg.minecrush.reactions.storage.yaml.Rewards;
import gg.minecrush.reactions.storage.yaml.Words;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import gg.minecrush.reactions.util.color;

import java.util.*;
import java.util.stream.Collectors;

public class ReactionManager {

    private final JavaPlugin plugin;
    private String currentAnswer;
    private long reactionStartTime;
    private boolean reactionActive;
    private final Words wordsManager;
    private final Messages messagesManager;
    private final Rewards rewards;


    public String center(String message) {
        int chatWidth = 53;

        int messageLength = ChatColor.stripColor(message).length();
        int spacesNeeded = (chatWidth - messageLength) / 2;

        StringBuilder centeredMessage = new StringBuilder();
        for (int i = 0; i < spacesNeeded; i++) {
            centeredMessage.append(" ");
        }
        centeredMessage.append(message);

        return centeredMessage.toString();
    }

    public ReactionManager(JavaPlugin plugin, Words wordsManager, Messages messagesManager, Rewards rewards) {
        this.plugin = plugin;
        this.reactionActive = false;
        this.wordsManager = wordsManager;
        this.messagesManager = messagesManager;
        this.rewards = rewards;
    }

    public void reset_Reaction(){
        this.reactionActive = false;
    }

    public boolean isReactionActive(){
        return reactionActive;
    }

    public boolean isAnswer(String message) {
        return currentAnswer != null && currentAnswer.equalsIgnoreCase(message);
    }

    public void handleCorrectAnswer(Player player, String message) {
        long timeTaken = System.currentTimeMillis() - reactionStartTime;
        String timeFormatted = String.format("%.2f", timeTaken / 1000.0);

        String reward = rewards.getRandomReward();
        if (rewards.getBoolean("rewards." + reward + ".command")){
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewards.getValue("rewards." + reward + ".run-command").replace("%player%", player.getName()));
            });
        }
        if (rewards.getBoolean("rewards." +  reward + ".item")) {
            ConfigurationSection itemSection = rewards.getConfigurationSection("rewards." + reward + ".item-stack");
            if (itemSection != null) {
                ItemStack stack = itemSectionToItemStack(itemSection);
                Map<Integer, ItemStack> remainingItems = player.getInventory().addItem(stack);
                if (!remainingItems.isEmpty()) {
                    for (ItemStack remainingItem : remainingItems.values()) {
                        player.getWorld().dropItem(player.getLocation(), remainingItem);
                    }
                }
            }
        }
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(center(getMessage("chat-game-title")));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(center(getMessage("player-answer").replace("%player%", player.getName()).replace("%message%", message).replace("%time%", timeFormatted)));
        Bukkit.broadcastMessage(center(getMessage("reward-message").replace("%reward%", rewards.getValue("rewards." + reward + ".reward"))));
        Bukkit.broadcastMessage("");


            currentAnswer = null;
            reactionActive = false;

    }

    private ItemStack itemSectionToItemStack(ConfigurationSection section) {
        Material material = Material.valueOf(section.getString("type", "AIR"));
        int amount = section.getInt("amount", 1);

        ItemStack stack = new ItemStack(material, amount);

        if (section.contains("meta")) {
            ItemMeta meta = stack.getItemMeta();

            ConfigurationSection metaSection = section.getConfigurationSection("meta");

            if (metaSection != null) {
                if (metaSection.contains("display-name")) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', metaSection.getString("display-name")));
                }

                if (metaSection.contains("lore")) {
                    List<String> lore = metaSection.getStringList("lore");
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    meta.setLore(coloredLore);
                }

                if (metaSection.contains("enchants")) {
                    ConfigurationSection enchantsSection = metaSection.getConfigurationSection("enchants");
                    for (String enchantmentName : enchantsSection.getKeys(false)) {
                        int level = enchantsSection.getInt(enchantmentName);
                        ((ItemMeta) meta).addEnchant(Objects.requireNonNull(Enchantment.getByName(enchantmentName)), level, true);
                    }
                }
            }

            stack.setItemMeta(meta);
        }

        return stack;
    }

    public void startReaction(String type) {
        if (reactionActive) {
            return;
        }

        if ("math".equalsIgnoreCase(type)) {
            startMathReaction();
        } else if ("scramble".equalsIgnoreCase(type)) {
            startScrambleReaction();
        } else if ("fastest".equalsIgnoreCase(type)) {
            startFastestReaction();
        }

        reactionStartTime = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskLater(plugin, this::checkreaction, 600L);     }

    private void checkreaction() {
        if (reactionActive != false) {
            this.endReaction();
        }
    }

    private String getMessage(String key) {
        return color.c(messagesManager.getReplacedMessage(key));
    }

    private void startMathReaction() {
        Random random = new Random();
        int min = plugin.getConfig().getInt("options.mathMin");
        int max = plugin.getConfig().getInt("options.mathMax");
        int num1 = random.nextInt(max - min + 1) + min;
        int num2 = random.nextInt(max - min + 1) + min;
        currentAnswer = String.valueOf(num1 + num2);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(center(getMessage("chat-game-title")));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(center(getMessage("math-question").replace("%num1%", String.valueOf(num1)).replace("%num2%", String.valueOf(num2))));
        Bukkit.broadcastMessage("");
        reactionActive = true;
    }

    private void startScrambleReaction() {
        List<String> words = wordsManager.getWordList("words");
        if (words.isEmpty()) {
            return;
        }
        String word = words.get(new Random().nextInt(words.size()));
        currentAnswer = word;
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(center(getMessage("chat-game-title")));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(center(getMessage("scramble-question").replace("%word%", scrambleWord(word))));
        Bukkit.broadcastMessage("");
        reactionActive = true;
    }

    private void startFastestReaction() {
        List<String> words = wordsManager.getWordList("words");
        if (words.isEmpty()) {
            return;
        }
        String word = words.get(new Random().nextInt(words.size()));
        currentAnswer = word;
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(center(getMessage("chat-game-title")));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(center(getMessage("fastest-question").replace("%word%", word)));
        Bukkit.broadcastMessage("");
        reactionActive = true;
    }

    private void startFillintheblank() {
        List<String> words = wordsManager.getWordList("words");
        if (words.isEmpty()) {
            return;
        }
        String word = "";
        while(true) { // Might want to find a way not to use a while loop
            word = words.get(new Random().nextInt(words.size()));
            if (word.length() > 4) {
                break;
            }
        }

        if (word.length() <= 4) {
            // Broadcast a message indicating no suitable word was found
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(center(getMessage("chat-game-title")));
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(color.c(getMessage("no-word")));
            Bukkit.broadcastMessage("");
            return;
        }
        currentAnswer = word;
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(center(getMessage("chat-game-title")));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(center(getMessage("fill-in-the-blank-question").replace("%word%", fillInBlank(word))));
        Bukkit.broadcastMessage("");
        reactionActive = true;
    }

    private String scrambleWord(String word) {
        List<Character> characters = word.chars().mapToObj(e -> (char) e).collect(Collectors.toList());
        Collections.shuffle(characters);
        StringBuilder scrambled = new StringBuilder();
        for (char ch : characters) {
            scrambled.append(ch);
        }
        return scrambled.toString();
    }

    private String fillInBlank(String word) {
        char[] characters = word.toCharArray();

        if (word.length() < 3) {
            return word;
        }

        char firstChar = characters[0];
        char lastChar = characters[characters.length - 1];

        StringBuilder filledIn = new StringBuilder();
        Random random = new Random();
        for (int i = 1; i < characters.length - 1; i++) {
            if (characters[i] != ' ') {
                filledIn.append(characters[i]);
            } else {
                char randomChar = (char) (random.nextInt(26) + 'a');
                filledIn.append(randomChar);
            }
        }

        StringBuilder result = new StringBuilder();
        result.append(firstChar);
        result.append(filledIn);
        result.append(lastChar);

        return result.toString();
    }


    public void endReaction() {
        if (reactionActive) {
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(center(getMessage("chat-game-title")));
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(center(getMessage("chat-game-no-answer")));
            Bukkit.broadcastMessage("");
            currentAnswer = null;
            reactionActive = false;
        }
    }

    public void saveConfig() {
        plugin.saveConfig();
    }
}