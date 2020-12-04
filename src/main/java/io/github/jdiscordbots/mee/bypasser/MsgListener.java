package io.github.jdiscordbots.mee.bypasser;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class MsgListener extends ListenerAdapter {

	private static final Logger LOG=LoggerFactory.getLogger(MsgListener.class);
	private static final String FILE_NAME = "roles.dat";
	private static DataStorage data =new DataStorage();

	public MsgListener() {
		if(new File(FILE_NAME).exists()) {
			try {
				load();
			} catch(ClassNotFoundException | IOException e) {
				LOG.error("Cannot load roles", e);
			}
		}
	}

	private static boolean hasModPerm(Member member) {
		return member.hasPermission(Permission.MANAGE_ROLES);
	}
	public static class Holder<T>{
		private T val;
		public Holder(T val){
			this.val=val;
		}
		public T get(){
			return val;
		}
		public void set(T val){
			this.val=val;
		}
	}
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		String msgContent=event.getMessage().getContentRaw();
		Guild g=event.getGuild();
		if(event.getMessage().getMember()==null||event.getMessage().getMember().getUser().isBot()){
			return;
		}
		if(msgContent.startsWith("!rank")) {
			try {
				final Member member;
				if(event.getMessage().getMentionedMembers().isEmpty()) {
					member=event.getMember();
				} else {
					member=event.getMessage().getMentionedMembers().get(0);
				}

				if (member == null){
					return;
				}

				final int level = MeeAPI.getLevel(event.getGuild().getId(), member.getId());
				final Map<String,Boolean> rolesToChange= data.getRoleIdsForGuildAtLevel(event.getGuild().getId(), level);
				rolesToChange.forEach((roleId,add)->{
					final Role role = event.getGuild().getRoleById(roleId);
					if(role!=null) {
						if(hasModPerm(event.getGuild().getSelfMember()) && event.getGuild().getSelfMember().canInteract(role)) {
							if(add) {
								if(!member.getRoles().contains(role)){
									event.getGuild().addRoleToMember(member, role).queue();
								}
							}else {
								if(member.getRoles().contains(role)){
									event.getGuild().removeRoleFromMember(member, role).queue();
								}
							}
						} else if(member.hasPermission(Permission.ADMINISTRATOR)) {
							event.getChannel().sendMessage("Cannot assign role " + role.getName() + " as " + event.getGuild().getSelfMember().getEffectiveName() + " does not have the necessary permissions.").queue();
						}
					}
				});
			} catch (JSONException e) {
				if(LOG.isErrorEnabled()) {
					LOG.error("Cannot load leveling data from the Mee API in guild {}", g.getName(),e);
				}
			} catch(IOException e) {
				if(e.getMessage() != null && e.getMessage().startsWith("Server returned HTTP response code: 401 for URL: ")) {
					final Member member = event.getMember();

					if(member != null && member.hasPermission(Permission.ADMINISTRATOR)) {
						event.getChannel().sendMessage("The server leaderboard needs to be public for automatic role assigning to work. This can be configured on https://mee6.xyz/dashboard/"+event.getGuild().getId()+"/leaderboard").queue();
					}
					if(LOG.isInfoEnabled()) {
						LOG.info("Server leaderboard is not public for guild {}", event.getGuild().getName());
					}
				} else {
					if(LOG.isErrorEnabled()) {
						LOG.error("Cannot load leveling data from the Mee API in guild {}", event.getGuild().getName(),e);
					}
				}
			}
		} else if(event.getMessage().getContentRaw().startsWith("mb!")) {
			final Member member = event.getMember();

			if (member == null || !hasModPerm(member))
				return;

			final String[] split = event.getMessage().getContentRaw().split(" ");
			final String[] args = Arrays.copyOfRange(split, 1, split.length);
			final String command = split[0].substring("mb!".length()).toLowerCase();

			switch(command) {
				case "add": {
					if(args.length < 2) {
						event.getChannel().sendMessage("Error - missing args").queue();
						return;
					}
					int level;
					try {
						level = Integer.parseInt(args[0]);
					} catch(NumberFormatException e) {
						event.getChannel().sendMessage("Error - level required").queue();
						return;
					}
					Role role=null;
					try {
						role=event.getGuild().getRoleById(args[1]);
					} catch(NumberFormatException e) {
						//let it stay null
					}
					if(role == null) {
						event.getChannel().sendMessage("Error - Role not found").queue();
						return;
					}
					
					data.setRole(event.getGuild().getId(), level, role.getId());
					try {
						save();
					} catch(IOException e) {
						LOG.error("Cannot save data",e);
					}
					event.getChannel().sendMessage("added Role "+role.getName()+" for level "+level).queue();
					break;
				} case "remove": {
					int level;
					try {
						level = Integer.parseInt(args[0]);
					} catch(NumberFormatException e) {
						event.getChannel().sendMessage("Error - level required").queue();
						return;
					}
					String id = data.removeRole(event.getGuild().getId(), level);

					final Role role;

					if(id != null && (role = event.getGuild().getRoleById(id)) != null) {
						event.getChannel().sendMessage("removed id " + role.getAsMention() + " from level " + level).allowedMentions(Collections.emptyList()).queue();
					} else {
						event.getChannel().sendMessage("no id for level " + level).allowedMentions(Collections.emptyList()).queue();
					}
					try {
						save();
					} catch(IOException e) {
						LOG.error("Cannot save data",e);
					}
					break;
				} case "show":
				case "list":{
					Map <Integer, String> gRoles = data.getRoles(event.getGuild().getId());
					if(!gRoles.isEmpty()){
						event.getChannel().sendMessage("" + gRoles.entrySet().stream().map(e ->
						{
							final Role role = event.getGuild().getRoleById(e.getValue());
							if (role != null)
								return "Level " + e.getKey() + " is assigned to role " + role.getAsMention();
							return "";
						}).collect(Collectors.joining("\n"))+"\nRoles will "+((data.isAutoRemoveLevels(event.getGuild().getId()))?"":"**not** ")+"be removed if someone reaches a higher role.").allowedMentions(Collections.emptyList()).queue();
					}
					break;
				} case "toggle":{
					boolean rem=!data.isAutoRemoveLevels(event.getGuild().getId());
					data.setAutoRemoveLevels(event.getGuild().getId(),rem);
					try {
						save();
						event.getChannel().sendMessage("Roles will now "+(rem?"":"**not** ")+"be removed if someone reaches a higher role.").queue();
					} catch(IOException e) {
						LOG.error("Cannot save data",e);
					}
					break;
				} case "id": {
					if(args.length < 1) {
						event.getChannel().sendMessage("missing args").queue();
						return;
					}

					final String name = String.join(" ", args);
					final String title = "Roles of name `" + name + "`";
					final String desc = event.getGuild().getRolesByName(name, true).stream().map(role -> role.getAsMention() + " (" + role.getId() + ")").collect(Collectors.joining("\n"));

					if(event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_EMBED_LINKS)) {
						final EmbedBuilder eb = new EmbedBuilder();
						eb.setTitle(title);
						eb.setDescription(desc);

						event.getChannel().sendMessage(eb.build()).queue();
					} else {
						event.getChannel().sendMessage("**"+title+"**\n"+desc).queue();
					}
					break;
				} default: {
					event.getChannel().sendMessage("Command not found: "+command).queue();
				}
			}
		}
	}

	private static void save() throws IOException {
		try(ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(FILE_NAME)))) {
			oos.writeObject(data);
		}
	}

	@SuppressWarnings("unchecked")
	private static void load() throws IOException, ClassNotFoundException {
		try(ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(FILE_NAME)))) {
			Object readObj=ois.readObject();
			if(readObj instanceof Map) {
				data =new DataStorage((Map<String, Map<Integer, String>>) readObj);
				save();//overwrite
			}else if(readObj instanceof DataStorage){
				data =(DataStorage)readObj;
			}else if(readObj==null){
				throw new IOException("read object is null");
			}else{
				throw new IOException("deserialized object has unexcepted data type"+readObj.getClass().getName());
			}
			
		}
	}
}
