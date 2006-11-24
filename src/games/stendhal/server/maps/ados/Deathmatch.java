package games.stendhal.server.maps.ados;

import java.util.*;

import games.stendhal.common.Direction;
import games.stendhal.common.Pair;
import games.stendhal.server.entity.*;
import games.stendhal.server.entity.creature.*;
import games.stendhal.server.entity.item.*;
import games.stendhal.server.scripting.*;
import games.stendhal.server.entity.npc.*;
import games.stendhal.server.pathfinder.Path;
import games.stendhal.server.StendhalRPWorld;
import games.stendhal.server.StendhalRPZone;
import games.stendhal.server.StendhalScriptSystem;

import marauroa.common.game.IRPZone;

import org.apache.log4j.Logger;


/**
 * Creating the Stendhal Deathmatch Game
 */
public class Deathmatch {
	private NPCList npcs = NPCList.get();
	private StendhalScriptSystem scripts = StendhalScriptSystem.get();

	class DeathmatchCondition extends ScriptCondition {
		Player player;
		public DeathmatchCondition (Player player) {
			this.player = player;
		}
		public boolean fire() {
			if("cancel".equals(player.getQuest("deathmatch"))) {
				return false;
			}
			if(player.getQuest("deathmatch").startsWith("done")) {
				return false;
			}
			
			if ("int_semos_deathmatch".equals(player.get("zoneid"))) {
				return true;
			} else {
				player.setQuest("deathmatch", "cancel");
				return true;
			}
		}
	}
/*
	class DeathmatchAction extends ScriptAction {
		Player player;
		List<Creature> sortedCreatures;
		List<Creature> spawnedCreatures = new ArrayList<Creature>();
		public DeathmatchAction (Player player) {
			this.player = player;
			Collection<Creature> creatures = StendhalRPWorld.get().getRuleManager().getEntityManager().getCreatures();
			sortedCreatures.addAll(creatures);
			Collections.sort(sortedCreatures, new Comparator<Creature>() {
				public int compare(Creature o1, Creature o2) {
					return o1.getLevel() - o2.getLevel();
				}
			});
		}
		public void fire() {
			String questInfo = player.getQuest("deathmatch");
			String[] tokens = (questInfo + ";0;0").split(";");
			String questState = tokens[0];
			String questLevel = tokens[1];
			String questLast	= tokens[2];
			long bailDelay = 2000;		// wait 2 seconds before bail takes effect
			long spawnDelay = 15000;	// spawn a new monster each 15 seconds
			// the player wants to leave the game
			// this is delayed so the player can see the taunting
			if("bail".equals(questState)) {
				if(questLast != null && (new Date()).getTime() - new Long( questLast) > bailDelay ) {
					questState = "cancel";
					player.setQuest("deathmatch", questState);
					// We assume that the player only carries one trophy helmet.
					Item helmet	= player.getFirstEquipped("trophy_helmet");
					if(helmet != null) {
						int defense = 1;
						if(helmet.has("def")) {
							defense = new Integer(helmet.get("def"));
						}
						defense--;
						helmet.put("def",""+defense);
						player.updateItemAtkDef();
					}
					else {
						int xp = player.getLevel() * 80;
						if(xp > player.getXP()) {
							xp = player.getXP();
						}
						player.addXP(-xp);
					}	
					// send the player back to the entrance area
					StendhalRPZone zone = (StendhalRPZone) StendhalRPWorld.get().getRPZone("0_semos_plains_n");
					player.teleport(zone, 100, 115, null, player);
				}
			}
			if("cancel".equals(questState)) {
				// remove the critters that the player was supposed to kill
				for (Creature creature : spawnedCreatures) {
					String id = creature.getID().getZoneID();
					StendhalRPZone zone = (StendhalRPZone) StendhalRPWorld.get().getRPZone(id);
					zone.remove(creature);
				}
				// and finally remove this ScriptAction 
				game.remove(this);
				return;
			}
			// save a little processing time and do things only every spawnDelay miliseconds 
			if(questLast != null && (new Date()).getTime() - new Long( questLast) > spawnDelay )
				{
				int currentLevel = new Integer( questLevel );
				if(currentLevel > player.getLevel() + 7) {
					boolean done = true;
					// check if all our enemies are dead
					for (Creature creature : spawnedCreatures) {
						if(creature.getHP()>0) {
							done = false;
						}
					}
					if(done) {
						// be nice to the player and give him his daily quest creature
						// if he hasn't found it yet
						String dailyInfo = player.getQuest("daily");
						if(dailyInfo != null) {
							String[] dTokens = dailyInfo.split(";");
							String daily = dTokens[0];
							if(!player.hasKilled(daily)) {
								for (Creature creature : sortedCreatures) {
									if (creature.getName().equals(daily)) {
										int x = player.getX() + 1; 
										int y = player.getY() + 1;
										game.add(creature, x, y);
										break;
									}
								}
							}
						}
						questState = "victory";
						// remove this ScriptAction since we're done
						game.remove(this);
					}
				} else {
					// spawn the next stronger creature
					int k = new Integer(questLevel);
					List<Creature> possibleCreaturesToSpawn = new ArrayList<Creature>();
					int lastLevel = 0;
					for (Creature creature : sortedCreatures) {
						if (creature.getLevel() > k) {
							break;
						}					
						if (creature.getLevel() > lastLevel) {
							possibleCreaturesToSpawn.clear();
							lastLevel = creature.getLevel();
						}
						possibleCreaturesToSpawn.add(creature);
					}
					
					Creature creatureToSpawn = null;
					if (possibleCreaturesToSpawn.size() == 0) {
						creatureToSpawn = sortedCreatures.get(sortedCreatures.size() - 1);
					} else if (possibleCreaturesToSpawn.size() == 1) {
						creatureToSpawn = possibleCreaturesToSpawn.get(0);
					} else {
						creatureToSpawn = possibleCreaturesToSpawn.get((int) (Math.random() * possibleCreaturesToSpawn.size()));
					}
					int x = player.getX(); 
					int y = player.getY();
					Creature mycreature = game.add(creatureToSpawn, x, y);
					if (mycreature != null) {
						mycreature.clearDropItemList();
						mycreature.attack(player);
						spawnedCreatures.add(mycreature);
						questLevel = Integer.toString(currentLevel + 1);
					}
				}			
				player.setQuest("deathmatch", questState + ";" + questLevel + ";" + (new Date()).getTime());
			}
		}
	}



	class StartAction extends SpeakerNPC.ChatAction {
		public void fire(Player player, String text, SpeakerNPC engine) {
			engine.say("Have fun!");
			int level = player.getLevel() - 2;
			if(level < 1) {
				level = 1;
			}
			player.setQuest("deathmatch", "start;"+ level + ";" + (new Date()).getTime());
			Pair<ScriptCondition, ScriptAction> script = scripts.addScript(
					new DeathmatchCondition(player), new DeathmatchAction(player));
		}
	}

	class DoneAction extends SpeakerNPC.ChatAction {
		public void fire(Player player, String text, SpeakerNPC engine) {		
			engine.say("You think you did it?");
			String questInfo = player.getQuest("deathmatch");
			String[] tokens = (questInfo+";0;0").split(";");
			String questState = tokens[0];
			String questLevel = tokens[1];
			String questLast	= tokens[2];
			if("victory".equals(questState)) {
				boolean isNew = false;
					// We assume that the player only carries one trophy helmet.
				Item helmet	= player.getFirstEquipped("trophy_helmet");
				if(helmet == null) {
					helmet = StendhalRPWorld.get().getRuleManager().getEntityManager().getItem("trophy_helmet");
					engine.say("Congratulations! Here is your special trophy helmet. Enjoy it. Now, tell me if you want to #leave.");
					isNew = true;
				}
				else {
					engine.say("Congratulations! And your helmet has been magically strengthened. Now, tell me if you want to #leave.");
				}
				int defense = 1;
				if(helmet.has("def")) {
					defense = new Integer(helmet.get("def"));
				}
				defense++;
				int maxdefense = 5 + (player.getLevel() / 5);
				if(defense > maxdefense) {
						engine.say("Congratulations! However, I'm sorry to inform you, the maximum defense for your helmet at your current level is " + maxdefense);
						helmet.put("def",""+maxdefense);					
						
				}
				else {
						helmet.put("def",""+defense);				
				}
				helmet.put("infostring",player.getName());
				helmet.put("persistent",1);
				helmet.setDescription("This is " + player.getName() +	"'s grand prize for Deathmatch winners. Wear it with pride.");
				if(isNew) {
					player.equip(helmet, true);
				}
				player.updateItemAtkDef();
				player.setQuest("deathmatch", "done");
			}
			else {
				engine.say("C'm on, don't lie to me! All you can do now is #bail or win.");
			}
			return;
		}
	}

	class LeaveAction extends SpeakerNPC.ChatAction {
		public void fire(Player player, String text, SpeakerNPC engine) {	 
			if("done".equals(player.getQuest("deathmatch"))) {
				StendhalRPZone zone = (StendhalRPZone) StendhalRPWorld.get().getRPZone("0_semos_plains_n");
				player.teleport(zone, 100, 115, null, player);				
			} else {
				engine.say("I don't think you claimed your #victory yet.");
			}
			return;
		}
	}

	class BailAction extends SpeakerNPC.ChatAction {
		public void fire(Player player, String text, SpeakerNPC engine) {
			String questInfo = player.getQuest("deathmatch");
			if (questInfo == null) {
					engine.say("Coward, you haven't even #started!");
					return;
			}
			String[] tokens = (questInfo+";0;0").split(";");
			String questState = tokens[0];
			String questLevel = tokens[1];
			String questLast	= tokens[2];
			if(!"start".equals(questState)) {
				engine.say("Coward, we haven't even #started!");
				return;
			}
			player.setQuest("deathmatch", "bail;"+ questLevel + ";" + (new Date()).getTime());
			// We assume that the player only carries one trophy helmet.
			Item helmet	= player.getFirstEquipped("trophy_helmet");
			if(helmet != null) {
				engine.say("Coward! I'm sorry to inform you, for this your helmet has been magically weakened.");
			}
			else {
				engine.say("Coward! You're not as experienced as you used to be.");
			}
			return;
		}
	}


	public void createDeathmatch() {
		String myZone = "int_semos_deathmatch";
		StendhalRPWorld world = StendhalRPWorld.get();
		StendhalRPZone zone = (StendhalRPZone) world.getRPZone(new IRPZone.ID(myZone));
	
		// show the player the potential trophy
		Item helmet = StendhalRPWorld.get().getRuleManager().getEntityManager().getItem("trophy_helmet");
		zone.assignRPObjectID(helmet);
		helmet.put("def","20");
		helmet.setDescription("This is the grand prize for Deathmatch winners.");
		helmet.setX(17);
		helmet.setY(4);
		helmet.put("persistent",1);
		zone.add(helmet);

		
		// We create an NPC
		SpeakerNPC npc=new SpeakerNPC("Deathmatch Assistant") {

			@Override
			protected void createPath() {
				setPath(new ArrayList<Path.Node>(), false);
			}

			@Override
			protected void createDialog() {
				
				addGreeting("Welcome to Semos deathmatch! Do you need #help?");
				addJob("I'm the deathmatch assistant. Tell me, if you need #help on that.");
				addHelp("Say '#start' when you're ready! Keep killing #everything that #appears. Say 'victory' when you survived.");
				addGoodbye("I hope you enjoy the Deathmatch!");

				add(ConversationStates.ATTENDING, Arrays.asList("everything", "appears"), ConversationStates.ATTENDING, 
						"Each round you will face stronger enemies. Defend well, kill them or tell me if you want to #bail!", null);
				add(ConversationStates.ATTENDING, Arrays.asList("trophy","helm","helmet"), ConversationStates.ATTENDING,
						"If you win the deathmatch, we reward you with a trophy helmet. Each #victory will strengthen it.", null);

				// 'start' command will start spawning creatures
				add(ConversationStates.ATTENDING, Arrays.asList("start", "go", "fight"), null, 
						ConversationStates.ATTENDING, null, new StartAction());
				
				// 'victory' command will scan, if all creatures are killed and reward the player
				add(ConversationStates.ATTENDING, Arrays.asList("victory", "done", "yay"), null,
						ConversationStates.ATTENDING, null, new DoneAction());
				
				// 'leave' command will send the victorious player home
				add(ConversationStates.ATTENDING, Arrays.asList("leave", "home"), null, 
						ConversationStates.ATTENDING, null, new LeaveAction());
				
				// 'bail' command will teleport the player out of it
				add(ConversationStates.ATTENDING, Arrays.asList("bail", "flee", "run", "exit"), null,
						ConversationStates.ATTENDING, null, new BailAction());
			}

		};

		
		npc.put("class", "darkwizardnpc");
		npc.set(17, 11);
		npc.setDirection(Direction.DOWN);
		npc.initHP(100);
		npcs.add(npc);
		zone.assignRPObjectID(npc);
		zone.addNPC(npc);
	}
	*/
}
