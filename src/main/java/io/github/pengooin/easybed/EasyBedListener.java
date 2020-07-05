package io.github.pengooin.easybed;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

public final class EasyBedListener implements Listener{
	private EasyBed plugin;
	public EasyBedListener(EasyBed plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler(priority = EventPriority.MONITOR) 
	public void bedEnterEvent(PlayerBedEnterEvent event) {
		if(plugin.getVoteStatus()) {
			plugin.voteManager(event.getPlayer());
		}
		else {
			//event.getPlayer().chat("Hello, you clicked a bed, didn't you?");
			//event.getPlayer().chat(event.getBedEnterResult().toString());
			switch(event.getBedEnterResult()) {
			case NOT_POSSIBLE_HERE:
				break;
			case NOT_POSSIBLE_NOW:
				break;
			case NOT_SAFE:
				break;
			case OK:
				if(event.getPlayer().getWorld().getPlayers().size()>1) {
					plugin.startVote(event.getPlayer(),event.getPlayer().getWorld());					
				}
				break;
			case OTHER_PROBLEM:
				break;
			case TOO_FAR_AWAY:
				break;
			default:
				break;	
			}	
		}
	}
}
