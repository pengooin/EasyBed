package io.github.pengooin.easybed;

import java.io.File;
import java.util.HashMap;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class EasyBed extends JavaPlugin{
	private HashMap<Player,Boolean> votes;
	private boolean voteActive = false;
	private int votesNeeded = 0;
	private int currentVotes = 0;
	private World world;
	private boolean voteInitilizing = false;
	private int currentId = 0;
	
	private double percentage = 0;
	
	private File file = new File(getDataFolder(), "config.yml");
	private YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(file);
	
	@Override
	public void onEnable() {
		getLogger().info("onEnable has been invoked!");
		//This stuff is to get players so a reload won't kill, but just don't reload
		/*for(Player player : Bukkit.getServer().getOnlinePlayers()) {
			playerList.put(player.getName(), playerData(player));
		}*/
		votes = new HashMap<Player, Boolean>();
		
		
		this.saveDefaultConfig();
		this.saveConfig();
			
		new EasyBedListener(this);
	}
	@Override
	public void onDisable() {
		getLogger().info("onDisable has been invoked!");
	}
	
	public void setPercentageFromConfig() {
		this.percentage = this.getConfig().getDouble("percentage");
	}
	
//	public void saveFIle
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("vote")) {
			//Checks if the commander sender was not a player, tells them it must be run by a player
			if(!(sender instanceof Player)) {
				sender.sendMessage("This command can only be run by a player.");
			}
			//If the sender is a player, continues
			else {
				Player player = (Player) sender; //Create instance of player and cast sender into a Player
				voteManager(player); //Attempts to vote for the player
			}
			return true;
		}
		return false;
	}
	boolean getVoteStatus() {
		return voteActive;
	}
	private boolean playerVote(Player voter) {
		if(votes.containsKey(voter)) {
			return votes.replace(voter, false, true); //This outputs true if the player had not voted and changes the players vote to true.
		}
		return false;
	}
	void voteManager(Player voter) {
		if(voteActive) {
			if(playerVote(voter)) {
				voter.spigot().sendMessage(new TextComponent("EasyBed: You voted to change to daytime"));
				currentVotes++;
				if(currentVotes>=votesNeeded) {
					voteSuccess();
					resetVote();
				}
			}
			else {
				voter.spigot().sendMessage(new TextComponent("EasyBed: Your vote failed to execute, if you were not in the world when the vote started, you can not vote. If you already voted, you can't vote again."));
			}
		}
		else {
			voter.spigot().sendMessage(new TextComponent("EasyBed: This vote is no longer active"));
		}
	}
	void startVote(final Player voter, final World worldGiven) {
		if(voteInitilizing||voteActive) {
			voter.spigot().sendMessage(new TextComponent("EasyBed: You can't start a vote for daytime as someone is already starting/started a vote."));
		}
		else {
			voter.spigot().sendMessage(new TextComponent("EasyBed: Stay in bed for 60 ticks (three seconds) to confirm you want to send a vote to change it to daytime and or clear the thunderstorm."));
			voteInitilizing=true;
			BukkitScheduler scheduler = getServer().getScheduler();
	        scheduler.scheduleSyncDelayedTask(this, new Runnable() {
	            //@Override The annotation is not working for some reason.
	            public void run() {
	            	if(voter.isSleeping()) {
	    				world = worldGiven;
	    				voteActive=true;
	    				// Compute based on config file
	    				votesNeeded=((world.getPlayers().size()/2)+1); //Should be automatically truncated due to using integers, this requires a majority
	    				currentVotes=0;
	    				TextComponent message = new TextComponent("EasyBed: " + voter.getDisplayName() + " wants to change to daytime and or clear any thunderstorms. Click this message, type /vote, or click a bed within 30 "
	    						+ "seconds to vote yes for changing to daytime.");
	    				message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vote"));
	    				message.setColor(net.md_5.bungee.api.ChatColor.GREEN);
	    				for(Player player: world.getPlayers()) {
	    					player.spigot().sendMessage(message);
	    					votes.put(player, false);
	    				}
	    				currentId+=1;
	    				voteManager(voter);
	    			}
	            	voteInitilizing=false;
	            }
	        }, 60L); //This is three seconds
	        final int thisId = currentId;
	        scheduler.scheduleSyncDelayedTask(this, new Runnable() {
	        	//@Override The annotation is not working for some reason
	        	public void run() {
	        		if(thisId!=currentId) {
	        			TextComponent failedVoteMessage = new TextComponent("EasyBed: Vote failed.");
	        			failedVoteMessage.setColor(net.md_5.bungee.api.ChatColor.RED);
	        			for(Player player: votes.keySet()) {
	        				player.spigot().sendMessage(failedVoteMessage);
	        			}
	        			resetVote();
	        		}
	        	}
	        }, 700L); //I want this to be 20 seconds, but 400L didn't seem to be that.
		}
	}
	void resetVote() {
		voteActive = false;
		votes.clear();
		currentVotes=0;
		votesNeeded=0;
	}
	void voteSuccess() {
		long currentTimeAdjusted = world.getTime() % 24000;
		if(currentTimeAdjusted > 12541 && currentTimeAdjusted % 24000 < 23458) {
			long timeToSkip = 0;
			if(currentTimeAdjusted < 23000) {
				timeToSkip = 23000-currentTimeAdjusted;
				//Check that time is less than 23000 as sleeping sets the time to this
				//Bukkit.getServer().getPluginManager().callEvent(TimeSkipEvent(world,SkipReason.NIGHT_SKIP,timeToSkip)); These are just call backs they don't do anything
				TextComponent daySuccessMessage = new TextComponent("EasyBed: Setting to daytime!");
				daySuccessMessage.setColor(net.md_5.bungee.api.ChatColor.GOLD);
				for(Player player: world.getPlayers()) {
					player.spigot().sendMessage(daySuccessMessage);
				}
				world.setTime(currentTimeAdjusted+timeToSkip);
			}
			//event.getPlayer().getServer().getPluginManager().callEvent(TimeSkipEvent(world,SkipReason.NIGHT_SKIP,world.getTime() % 24000 < 23000 ? (long) 1: (long) 0));
		}
		else {
			TextComponent dayFailedMessage = new TextComponent("EasyBed: Unable to set to daytime.");
			dayFailedMessage.setColor(net.md_5.bungee.api.ChatColor.GRAY);
			for(Player player: world.getPlayers()) {
				player.spigot().sendMessage(dayFailedMessage);
			}
		}
		if(world.hasStorm()) {
			world.setStorm(false);
			TextComponent stormSuccessMessage = new TextComponent("EasyBed: Clearing the weather!");
			stormSuccessMessage.setColor(net.md_5.bungee.api.ChatColor.GOLD);
			for(Player player: world.getPlayers()) {
				player.spigot().sendMessage(stormSuccessMessage);
			}
		}
		else {
			TextComponent stormFailedMessage = new TextComponent("EasyBed: Unable to clear the weather.");
			stormFailedMessage.setColor(net.md_5.bungee.api.ChatColor.GRAY);
			for(Player player: world.getPlayers()) {
				player.spigot().sendMessage(stormFailedMessage);
			}
		}
	}
}
