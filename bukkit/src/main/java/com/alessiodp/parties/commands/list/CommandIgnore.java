package com.alessiodp.parties.commands.list;

import org.bukkit.entity.Player;

import com.alessiodp.parties.Parties;
import com.alessiodp.parties.commands.CommandData;
import com.alessiodp.parties.commands.ICommand;
import com.alessiodp.parties.configuration.Constants;
import com.alessiodp.parties.configuration.data.Messages;
import com.alessiodp.parties.logging.LogLevel;
import com.alessiodp.parties.logging.LoggerManager;
import com.alessiodp.parties.players.PartiesPermission;
import com.alessiodp.parties.players.objects.PartyPlayerEntity;

public class CommandIgnore implements ICommand {
	private Parties plugin;
	 
	public CommandIgnore(Parties parties) {
		plugin = parties;
	}

	@Override
	public boolean preRequisites(CommandData commandData) {
		Player player = (Player) commandData.getSender();
		PartyPlayerEntity pp = plugin.getPlayerManager().getPlayer(player.getUniqueId());
		
		/*
		 * Checks for command prerequisites
		 */
		if (!player.hasPermission(PartiesPermission.IGNORE.toString())) {
			pp.sendNoPermission(PartiesPermission.IGNORE);
			return false;
		}
		
		commandData.setPlayer(player);
		commandData.setPartyPlayer(pp);
		return true;
	}
	
	@Override
	public void onCommand(CommandData commandData) {
		PartyPlayerEntity pp = commandData.getPartyPlayer();
		
		/*
		 * Command handling
		 */
		boolean isPrompt = false;
		String ignoredParty = "";
		if (commandData.getArgs().length == 1) {
			// Shows ignore list
			isPrompt = true;
		} else {
			// Edit party ignore
			if (commandData.getArgs().length > 2) {
				pp.sendMessage(Messages.MAINCMD_IGNORE_WRONGCMD);
				return;
			}
			
			ignoredParty = commandData.getArgs()[1];
			
			if (!plugin.getPartyManager().existParty(ignoredParty)) {
				pp.sendMessage(Messages.PARTIES_COMMON_PARTYNOTFOUND
						.replace("%party%", ignoredParty));
				return;
			}
		}
		
		/*
		 * Command starts
		 */
		if (isPrompt) {
			StringBuilder builder = new StringBuilder();
			for (String name : pp.getIgnoredParties()) {
				if (builder.length() > 0) {
					builder.append(Messages.MAINCMD_IGNORE_LIST_SEPARATOR);
				}
				builder.append(Messages.MAINCMD_IGNORE_LIST_PARTYPREFIX + name);
			}
			
			String ignores = builder.toString();
			if (pp.getIgnoredParties().size() == 0 || ignores == null)
				ignores = Messages.MAINCMD_IGNORE_LIST_EMPTY;
			
			pp.sendMessage(Messages.MAINCMD_IGNORE_LIST_HEADER
					.replace("%number%", Integer.toString(pp.getIgnoredParties().size())));
			pp.sendMessage(ignores);
		} else {
			if (pp.getIgnoredParties().contains(ignoredParty)) {
				// Remove
				pp.getIgnoredParties().remove(ignoredParty);
				pp.sendMessage(Messages.MAINCMD_IGNORE_STOP
						.replace("%party%", ignoredParty));
				
				LoggerManager.log(LogLevel.MEDIUM, Constants.DEBUG_CMD_IGNORE_START
						.replace("{player}", pp.getName())
						.replace("{party}", ignoredParty), true);
			} else {
				// Add
				pp.getIgnoredParties().add(ignoredParty);
				pp.sendMessage(Messages.MAINCMD_IGNORE_START
						.replace("%party%", ignoredParty));
				
				LoggerManager.log(LogLevel.MEDIUM, Constants.DEBUG_CMD_IGNORE_STOP
						.replace("{player}", pp.getName())
						.replace("{party}", ignoredParty), true);
			}
		}
	}
}
