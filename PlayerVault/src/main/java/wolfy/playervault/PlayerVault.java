package wolfy.playervault;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerVault extends JavaPlugin implements Listener {

    private String prefix = "§f[§cPlayerVaults§f] §d";
    private int maxVaults;
    private int minVaults;

    @Override
    public void onEnable() {
        // Ensure the 'playerdata' folder exists
        File playerDataFolder = new File(getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        // Load configuration
        saveDefaultConfig();

        // Read max_vaults and min_vaults values from the configuration
        maxVaults = getConfig().getInt("max_vaults", 5);
        minVaults = getConfig().getInt("min_vaults", 1);

        // Register the event listener
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + "This command can only be run by players.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("playervault") || command.getName().equalsIgnoreCase("pv")) {
            if (args.length == 1) {
                int vaultNumber;
                try {
                    vaultNumber = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + "§cInvalid vault number. Please use a number between 1 and 5.");
                    return true;
                }

                if (vaultNumber < minVaults || vaultNumber > maxVaults) {
                    player.sendMessage(prefix + "§4§lHEY! §cVault number must be between " + minVaults + " and " + maxVaults + "!");
                    return true;
                }

                openPlayerVault(player, vaultNumber);
            } else {
                player.sendMessage(prefix + "§cUsage: /playervault <number>");
            }
        }

        return true;
    }

    private void openPlayerVault(Player player, int vaultNumber) {
        Inventory vaultInventory = Bukkit.createInventory(
                new VaultHolder(vaultNumber),
                9 * 6, // 6 rows inventory
                "§cVault §5" + vaultNumber
        );

        // Load player vault data from the playerdata folder
        File playerDataFile = new File(getDataFolder() + "/playerdata", player.getUniqueId() + ".yml");
        if (playerDataFile.exists()) {
            // Load the vault contents from the player's data file
            loadPlayerVaultContents(player, vaultInventory, vaultNumber);
        } else {
            // Player's data file doesn't exist; create one with default contents
            saveDefaultVaultContents(player, vaultInventory, vaultNumber);
        }

        player.openInventory(vaultInventory);
    }

    // Implement the logic to save player vault contents to their data file
    private void loadPlayerVaultContents(Player player, Inventory vaultInventory, int vaultNumber) {
        File playerDataFile = new File(getDataFolder() + "/playerdata", player.getUniqueId() + ".yml");
        if (!playerDataFile.exists()) {
            return; // Player's data file doesn't exist, no need to load contents
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
        if (config.contains("vault" + vaultNumber)) {
            // Load the vault contents from the configuration
            List<?> contents = config.getList("vault" + vaultNumber);
            if (contents != null) {
                for (int i = 0; i < contents.size(); i++) {
                    Object itemObj = contents.get(i);
                    if (itemObj instanceof ItemStack) {
                        vaultInventory.setItem(i, (ItemStack) itemObj);
                    }
                }
            }
        }
    }

    // Implement the logic to save player vault contents to their data file
    private void saveDefaultVaultContents(Player player, Inventory vaultInventory, int vaultNumber) {
        File playerDataFile = new File(getDataFolder() + "/playerdata", player.getUniqueId() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);

        // Save the vault contents to the configuration
        List<ItemStack> contents = new ArrayList<>(Arrays.asList(vaultInventory.getContents()));
        config.set("vault" + vaultNumber, contents);

        try {
            config.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Event listener for InventoryCloseEvent to save the vault contents when the inventory is closed
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Inventory closedInventory = event.getInventory();

        // Check if the closed inventory is a player's vault
        if (closedInventory.getHolder() instanceof VaultHolder) {
            int vaultNumber = ((VaultHolder) closedInventory.getHolder()).getVaultNumber();

            // Save the vault contents to the player's data file
            savePlayerVaultContents(player, closedInventory, vaultNumber);
        }
    }

    private void savePlayerVaultContents(Player player, Inventory closedInventory, int vaultNumber) {
        File playerDataFile = new File(getDataFolder() + "/playerdata", player.getUniqueId() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);

        // Save the vault contents to the configuration
        List<ItemStack> contents = new ArrayList<>(Arrays.asList(closedInventory.getContents()));
        config.set("vault" + vaultNumber, contents);

        try {
            config.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class VaultHolder implements InventoryHolder {

        private final int vaultNumber;

        public VaultHolder(int vaultNumber) {
            this.vaultNumber = vaultNumber;
        }

        public int getVaultNumber() {
            return vaultNumber;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}