package com.alessiodp.parties.commands.list;

import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.alessiodp.parties.Parties;
import com.alessiodp.parties.commands.CommandData;
import com.alessiodp.parties.commands.ICommand;
import com.alessiodp.parties.configuration.Constants;
import com.alessiodp.parties.configuration.data.ConfigParties;
import com.alessiodp.parties.configuration.data.Messages;
import com.alessiodp.parties.logging.LogLevel;
import com.alessiodp.parties.logging.LoggerManager;
import com.alessiodp.parties.parties.objects.PartyEntity;
import com.alessiodp.parties.players.PartiesPermission;
import com.alessiodp.parties.players.objects.PartyPlayerEntity;
import com.alessiodp.parties.utils.PartiesUtils;
import com.alessiodp.partiesapi.events.PartiesPartyRenameEvent;

public class CommandRename implements ICommand {
	private Parties plugin;
	 
	public CommandRename(Parties parties) {
		plugin = parties;
	}
	
	@Override
	public boolean preRequisites(CommandData commandData) {
		Player player = (Player) commandData.getSender();
		PartyPlayerEntity pp = plugin.getPlayerManager().getPlayer(player.getUniqueId());
		
		/*
		 * Checks for command prerequisites
		 */
		if (!player.hasPermission(PartiesPermission.RENAME.toString())) {
			pp.sendNoPermission(PartiesPermission.RENAME);
			return false;
		}
		
		commandData.setPlayer(player);
		commandData.setPartyPlayer(pp);
		commandData.addPermission(PartiesPermission.RENAME_OTHERS);
		return true;
	}
	
	@Override
	public void onCommand(CommandData commandData) {
		PartyPlayerEntity pp = commandData.getPartyPlayer();
		
		/*
		 * Command handling
		 */
		PartyEntity party = null;
		Type type = Type.WRONGCMD;
		if (commandData.getArgs().length == 2) {
			// Own party
			if (!pp.getPartyName().isEmpty())
				party = plugin.getPartyManager().getParty(pp.getPartyName());
			type = Type.OWN;
		} else if (commandData.getArgs().length == 3) {
			// Another party
			if (commandData.havePermission(PartiesPermission.RENAME_OTHERS)) {
				party = plugin.getPartyManager().getParty(commandData.getArgs()[1]);
				type = Type.ANOTHER;
			}
		}
		
		if (party == null) {
			switch (type) {
			case OWN:
				// No party
				pp.sendMessage(Messages.PARTIES_COMMON_NOTINPARTY);
				break;
			case ANOTHER:
				// Party doesn't exist
				pp.sendMessage(Messages.PARTIES_COMMON_PARTYNOTFOUND
						.replace("%party%", commandData.getArgs()[1]));
				break;
			case WRONGCMD:
				// Wrong command
				if (commandData.havePermission(PartiesPermission.RENAME_OTHERS))
					pp.sendMessage(Messages.MAINCMD_RENAME_WRONGCMD_ADMIN);
				else
					pp.sendMessage(Messages.MAINCMD_RENAME_WRONGCMD);
			}
			return;
		}
		
		if (type.equals(Type.OWN) && !PartiesUtils.checkPlayerRankAlerter(pp, PartiesPermission.PRIVATE_ADMIN_RENAME))
			return;
		
		String partyName = commandData.getArgs()[(type.equals(Type.OWN) ? 1 : 2)]; // type == 1 ? args[1] : args[2]
		
		if (partyName.length() > ConfigParties.GENERAL_NAME_MAXLENGTH) {
			pp.sendMessage(Messages.MAINCMD_CREATE_NAMETOOLONG);
			return;
		}
		if (partyName.length() < ConfigParties.GENERAL_NAME_MINLENGTH) {
			pp.sendMessage(Messages.MAINCMD_CREATE_NAMETOOSHORT);
			return;
		}
		if (PartiesUtils.checkCensor(partyName)) {
			pp.sendMessage(Messages.MAINCMD_CREATE_CENSORED);
			return;
		}
		if (!Pattern.compile(ConfigParties.GENERAL_NAME_ALLOWEDCHARS).matcher(partyName).matches()) {
			pp.sendMessage(Messages.MAINCMD_CREATE_INVALIDNAME);
			return;
		}
		
		if (plugin.getPartyManager().existParty(partyName)) {
			pp.sendMessage(Messages.MAINCMD_CREATE_NAMEEXISTS
					.replace("%party%", partyName));
			return;
		}
		
		/*
		 * Command starts
		 */
		String oldPartyName = party.getName();
		
		// Calling API event
		PartiesPartyRenameEvent partiesRenameEvent = new PartiesPartyRenameEvent(party, partyName, pp, type.equals(Type.ANOTHER) ? true : false);
		Bukkit.getServer().getPluginManager().callEvent(partiesRenameEvent);
		partyName = partiesRenameEvent.getNewPartyName();
		if (!partiesRenameEvent.isCancelled()) {
			party.renamingParty();
			
			plugin.getDatabaseManager().renameParty(oldPartyName, partyName);
			for (Player player : party.getOnlinePlayers()) {
				plugin.getPlayerManager().getPlayer(player.getUniqueId()).setPartyName(partyName);
			}
			
			party.setName(partyName);
			plugin.getPartyManager().getListParties().remove(oldPartyName.toLowerCase());
			plugin.getPartyManager().getListParties().put(partyName.toLowerCase(), party);
			party.callChange();
			
			pp.sendMessage(Messages.MAINCMD_RENAME_RENAMED
					.replace("%old%", oldPartyName), party);
			party.sendBroadcast(pp, Messages.MAINCMD_RENAME_BROADCAST
					.replace("%old%", oldPartyName));
			
			LoggerManager.log(LogLevel.BASIC, Constants.DEBUG_CMD_CREATE_FIXED
					.replace("{player}", pp.getName())
					.replace("{value}", oldPartyName)
					.replace("{party}", party.getName()), true);
		} else {
			LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_API_RENAMEEVENT_DENY
					.replace("{party}", partyName)
					.replace("{player}", pp.getName()), true);
		}
	}
	
	private enum Type {
		OWN, ANOTHER, WRONGCMD;
	}
}