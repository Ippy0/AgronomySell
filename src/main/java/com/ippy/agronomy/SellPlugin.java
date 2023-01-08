package com.ippy.agronomy;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;

public class SellPlugin implements CommandExecutor, Listener {
    Main main;
    public FileConfiguration Prices;
    public LuckPerms luckPerms = LuckPermsProvider.get();

    Economy economy = Main.getEconomy();

    boolean autoSell;
    public SellPlugin(Main main) {
        this.main = main;
        File file = new File(main.getDataFolder(), "Prices.yml");
        Prices = YamlConfiguration.loadConfiguration(file);
    }

    public int getShop(Player player) {//Gets the metadata called "shop" which will be the name of sell shops (int)
        CachedMetaData metaData = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
        return metaData.getMetaValue("shop", Integer::parseInt).orElse(0);
    }

    public String ItemFormatHand(Player player) {//Gets an item from your main hand and formats it to all lowercase, no spaces
        if (player.getInventory().getItemInMainHand() != null)
            return player.getInventory().getItemInMainHand().getType().toString().toLowerCase(Locale.ENGLISH).replace("_", "");
        else return "x";
    }
    public String ItemFormatInv(Player player,int slot) {//Gets an item from a specific slot in inventory amd formats it to all lowercase, no spaces
        if(player.getInventory().getItem(slot)!=null){
            return player.getInventory().getItem(slot).getType().toString().toLowerCase(Locale.ENGLISH).replace("_","");
        }
        else return "x";
    }

    public float getPriceHeld(Player player) {//Gets the price of held item
        if (ItemFormatHand(player) == "x") return 0;//if there's no item in the hand returns 0
        else {
            return (float) Prices.getInt(getShop(player) + "." + ItemFormatHand(player));//returns the price of the item if its in the Prices.yml file, if not returns 0
        }
    }
    public float getPriceInv(Player player,int slot) {//Gets the price of held item
        if (ItemFormatInv(player,slot) == "x") return 0;//if there's no item in the hand returns 0
        else {
            return (float) Prices.getInt(getShop(player) + "." + ItemFormatInv(player,slot));//returns the price of the item if its in the Prices.yml file, if not returns 0
        }
    }

    public int numOfItemsHeld(Player player, boolean x) {//counts how many of held item you have in |true=inventory|false=hand|
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
    public int numOfItemsSlot(Player player, int slot){
        return player.getInventory().getItem(slot).getAmount();
    }

    public void removeItems(Player player) {//removes all the items you sold from your inventory
        @NotNull Material item = player.getInventory().getItemInMainHand().getType();
        player.getInventory().remove(item);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("sell")) {
                double price = getPriceHeld(player);
                if (price == 0) player.sendMessage(Prices.getString("Can't Sell"));
                else {
                    int itemNum = numOfItemsHeld(player, true);
                    double bal = price * itemNum;
                    economy.depositPlayer(player.getName(), bal);
                    String message = Prices.getString("sellmessage");
                    message = message.replace("{VALUE}", bal + "");
                    message = message.replace("{AMOUNT}", itemNum + "");
                    player.sendMessage(message);
                    removeItems(player);

                }
            }//sell
            else if (command.getName().equalsIgnoreCase("sellhand")) {
                double price = getPriceHeld(player);
                if (price == 0) player.sendMessage(Prices.getString("Can't Sell"));
                else {
                    int itemNum = numOfItemsHeld(player, false);
                    double bal = price * itemNum;
                    economy.depositPlayer(player.getName(), bal);
                    String message = Prices.getString("sellmessage");
                    message = message.replace("{VALUE}", bal + "");
                    message = message.replace("{AMOUNT}", itemNum + "");
                    player.sendMessage(message);
                    ItemStack item = player.getItemInHand();
                    player.getInventory().removeItem(item);
                }

            }//sell hand
            else if (command.getName().equalsIgnoreCase("autosell")){//turns on auto sell
                autoSell = true;
            }
            else if (command.getName().equalsIgnoreCase("sellinv")){//sells everything from the inventory
                double totalValue=0;
                double totalItems=0;
                for(int i=0;i<36;i++){
                    if(player.getInventory().getItem(i)!=null){
                        double price = getPriceInv(player,i);
                        if(price!=0){
                            totalItems+=numOfItemsSlot(player,i);
                            totalValue+=numOfItemsSlot(player,i)*price;
                            double bal = numOfItemsSlot(player,i)*price;
                            economy.depositPlayer(player.getName(),bal);
                            ItemStack item = player.getInventory().getItem(i);
                            player.getInventory().removeItem(item);
                        }
                    }
                }
                String message = Prices.getString("sellmessage");
                message = message.replace("{VALUE}", totalValue + "");
                message = message.replace("{AMOUNT}", totalItems + "");
                player.sendMessage(message);
            }
        }

        return false;
    }
    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent e){
        e.getPlayer().sendMessage("Nerd");
        if(autoSell){
            Player player = e.getPlayer();
            for(int i=0;i<36;i++){
                if(player.getInventory().getItem(i)!=null){
                    double price = getPriceInv(player ,i);
                    if(price!=0){
                        double bal = numOfItemsSlot(player,i)*price;
                        economy.depositPlayer(player.getName(),bal);
                        ItemStack item = player.getInventory().getItem(i);
                        player.getInventory().removeItem(item);
                    }
                }
            }
        }
    }

}
