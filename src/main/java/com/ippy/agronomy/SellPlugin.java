package com.ippy.agronomy;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Material;
import org.bukkit.command.Command;
import net.luckperms.api.LuckPerms;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.milkbowl.vault.economy.Economy;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.Locale;
public class SellPlugin implements CommandExecutor {
    Main main;
    public FileConfiguration Prices;
    public LuckPerms luckPerms = LuckPermsProvider.get();

    public SellPlugin(Main main) {
        this.main = main;
        File file = new File(main.getDataFolder(), "Prices.yml");
        Prices = YamlConfiguration.loadConfiguration(file);
    }

    public int getShop(Player player) {//Gets the metadata called "shop" which will be the name of sell shops (int)
        CachedMetaData metaData = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
        return metaData.getMetaValue("shop", Integer::parseInt).orElse(0);
    }

    public String ItemFormat(Player player) {//Gets an item from your main hand and formats it to all lowercase, no spaces
        if (player.getInventory().getItemInMainHand() != null)
            return player.getInventory().getItemInMainHand().getType().toString().toLowerCase(Locale.ENGLISH).replace("_", "");
        else return "x";
    }

    public float getPrice(Player player) {//Gets the price of held item
        if (ItemFormat(player) == "x") return 0;//if there's no item in the hand returns 0
        else {
            return (float) Prices.getInt(getShop(player) + "." + ItemFormat(player));//returns the price of the item if its in the Prices.yml file, if not returns 0
        }
    }

    public int numOfItems(Player player, boolean x) {//counts how many of held item you have in |true=inventory|false=hand|
        int a = 0;
        Material held = player.getInventory().getItemInMainHand().getType();
        if (x == true)
            for (int i = 0; i < 36; i++) {
                if (player.getInventory().getItem(i) != null) {// null check :)
                    if (held == player.getInventory().getItem(i).getType())
                        a += player.getInventory().getItem(i).getAmount();
                }
            }
        else a += player.getInventory().getItemInMainHand().getAmount();
        return a;
    }

    public void removeItems(Player player) {//removes all the items you sold from your inventory
        @NotNull Material item = player.getInventory().getItemInMainHand().getType();
        player.getInventory().remove(item);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Economy economy = Main.getEconomy();
            if (command.getName().equalsIgnoreCase("sell")) {
                double price = getPrice(player);
                if (price == 0) player.sendMessage(Prices.getString("Can't Sell"));
                else {
                    int itemNum = numOfItems(player, true);
                    double bal = price * itemNum;
                    economy.depositPlayer(player.getName(), bal);
                    String message = Prices.getString("sellmessage");
                    message = message.replace("{VALUE}", bal + "");
                    message = message.replace("{AMOUNT}", itemNum + "");
                    player.sendMessage(message);
                    removeItems(player);

                }
            }
            else if (command.getName().equalsIgnoreCase("sellhand")) {
                double price = getPrice(player);
                if (price == 0) player.sendMessage(Prices.getString("Can't Sell"));
                else {
                    int itemNum = numOfItems(player, false);
                    double bal = price * itemNum;
                    economy.depositPlayer(player.getName(), bal);
                    String message = Prices.getString("sellmessage");
                    message = message.replace("{VALUE}", bal + "");
                    message = message.replace("{AMOUNT}", itemNum + "");
                    player.sendMessage(message);
                    player.getInventory().getItemInMainHand().setType(Material.AIR);


                }

            }

        }

        return false;
    }
}
