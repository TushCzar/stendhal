package games.stendhal.server.maps.ados.magician_house;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import games.stendhal.common.MathHelper;
import games.stendhal.common.Rand;
import games.stendhal.server.core.engine.SingletonRepository;
import games.stendhal.server.core.engine.Spot;
import games.stendhal.server.core.engine.StendhalRPZone;
import games.stendhal.server.core.events.MovementListener;
import games.stendhal.server.entity.ActiveEntity;
import games.stendhal.server.entity.item.Item;
import games.stendhal.server.entity.mapstuff.portal.Teleporter;
import games.stendhal.server.entity.player.Player;
import games.stendhal.tools.tiled.LayerDefinition;
import games.stendhal.tools.tiled.StendhalMapStructure;

/**
 * A random maze zone.
 */
public class Maze {
	/** 
	 * Time in minutes how long the player can spend in the maze 
	 * to earn <code>DEFAULT_REWARD_POINTS</code>. Shorter times 
	 * get a higher reward, and longer times lower.
	 */ 
	private static final int DEFAULT_SOLVING_TIME = 5;
	/** 
	 * Amount of points for solving the maze in <code>DEFAULT_SOLVING_TIME</code>.
	 */ 
	private static final int DEFAULT_REWARD_POINTS = 100;
	private static final Logger logger = Logger.getLogger(Maze.class);
	
	private static final int WALL_THICKNESS = 2;
	private static final String[] prizes = {
		"summon scroll",
		"home scroll",
		"ados city scroll",
		"nalwor city scroll",
		"kirdneh city scroll",
		"kalavan city scroll",
		"empty scroll"
	};
	
	private String name;
	private int width, height;
	private Point startPosition;
	private List<Point> corners = null;
	
	private StendhalMapStructure mapStructure;
	private StendhalRPZone	zone = null;
	
	/** The name of the zone where to return a leaving player */
	private String returnZoneName;
	/** The coordinates where to return a leaving player */
	private int returnX, returnY;
	
	/** The time when the player was sent to the maze. */
	private long timeStamp;
	private MazeSign sign;
	
	/**
	 * Create a maze.
	 * 
	 * @param name Name of the maze to be used as the zone name
	 * @param width Width of the generated zone
	 * @param height Height of the generated zone
	 */
	public Maze(String name, int width, int height) {
		this.name = name;
		this.width = width;
		this.height = height;
		
		mapStructure = generateMapStructure(width, height);
	}
	
	/**
	 * Get the location where to teleport a player.
	 * 
	 * @return the intended starting location of the maze
	 */
	public Point getStartPosition() {
		if (startPosition == null) {
			startPosition = Rand.rand(getCorners());
		}
		
		return startPosition;
	}
	
	/**
	 * Set the location where a player logging out or returning 
	 * via a portal should be placed.
	 * 
	 * @param zoneName Name of the return zone
	 * @param x X coordinate
	 * @param y Y coordinate
	 */
	public void setReturnLocation(String zoneName, int x, int y) {
		returnZoneName= zoneName;
		returnX = x;
		returnY = y;
	}
	
	/**
	 * Set the sign for hall of fame.
	 * @param sign sign
	 */
	public void setSign(MazeSign sign) {
		this.sign = sign;
	}
	
	/**
	 * Get the zone generated by this maze instance.
	 * 
	 * @return The generated zone
	 */
	public StendhalRPZone getZone() {
		if (zone == null) {
			zone = generateZone();
		}
		return zone;
	}
	
	/**
	 * Start timing how long the player takest to solve the maze.
	 */
	public void startTiming() {
		timeStamp = System.currentTimeMillis();
	}
	
	private StendhalMapStructure generateMapStructure(int width, int height) {
		LayerDefinition floor = new LayerDefinition(width, height);
		floor.setName("0_floor");
		floor.build();
		
		LayerDefinition terrain = new LayerDefinition(width, height);
		terrain.setName("1_terrain");
		
		LayerDefinition object = new LayerDefinition(width, height);
		object.setName("2_object");
		
		LayerDefinition roof = new LayerDefinition(width, height);
		roof.setName("3_roof");
		
		LayerDefinition collision = new LayerDefinition(width, height);
		collision.setName("collision");

		LayerDefinition protection = new LayerDefinition(width, height);
		protection.setName("protection");
		
		StendhalMapStructure map = new StendhalMapStructure(width, height);
		
		map.addLayer(floor);
		map.addLayer(terrain);
		map.addLayer(object);
		map.addLayer(roof);
		map.addLayer(collision);
		map.addLayer(protection);
		
		generateCollisions(collision);
		
		// solves client caching, but makes other trouble
		//String md5 = Hash.toHexString(Hash.hash(collision.exposeRaw()));
		//name += "_" + md5;
		
		MazePainter painter = new MazePainter();
		painter.paint(map);
		Point pos = getPortalPosition();
		painter.paintPortal(map, pos.x, pos.y);
		
		return map;
	}
	
	private void generateCollisions(LayerDefinition layer) {
		layer.build();
		
		// create a grid
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (!((i % (WALL_THICKNESS + 1) == WALL_THICKNESS) && (j % (WALL_THICKNESS + 1) == WALL_THICKNESS))) {
					setCollide(layer, i, j, true);
				}
			}
		}
		
		burrowCave(getStartPosition(), layer);
		widenCorners(layer);
	}
	
	/**
	 * Make the tunnels. The actual maze generation algorithm.
	 * 
	 * @param point Starting point for the tunnels
	 * @param layer Collision layer
	 */
	private void burrowCave(Point point, LayerDefinition layer) {
		LinkedList<Point> branchPoints = new LinkedList<Point>();
		HashSet<Point> visited = new HashSet<Point>();
		branchPoints.add(point);
		List<Point> neighbours = getUnvisitedNeighbours(point, visited);
		
		do {
			visited.add(point);
			if (neighbours.size() > 0) {
				Point next = Rand.rand(neighbours);
				branchPoints.add(next);
				
				// Knock down the wall between
				int diffx = next.x - point.x;
				if (diffx != 0) {
					diffx /= Math.abs(diffx);
				}
				int diffy = next.y - point.y;
				if (diffy != 0) {
					diffy /= Math.abs(diffy);
				}

				for (int i = 1; i <= WALL_THICKNESS; i++) {
					setCollide(layer, point.x + i * diffx, point.y + i * diffy, false);
				}
				
				point = next;
			} else {
				branchPoints.remove(point);
				if (branchPoints.size() > 0) {
					// branch from the beginning to make nice and long tunnels
					point = branchPoints.getFirst();
				} else {
					point = null;
				}
			}
			
			neighbours = getUnvisitedNeighbours(point, visited);
		} while (point != null);
	}
	
	private List<Point> getUnvisitedNeighbours(Point point, HashSet<Point> visited) {
		if (point == null) {
			return null;
		}
		
		LinkedList<Point> neighbours = new LinkedList<Point>();
		
		Point left = new Point(point.x - (WALL_THICKNESS +1), point.y);
		if ((left.x > 0) && !visited.contains(left)) {
			neighbours.add(left);
		}
		
		Point right = new Point(point.x + (WALL_THICKNESS + 1), point.y);
		if ((right.x < width) && !visited.contains(right)) {
			neighbours.add(right);
		}
		
		Point up = new Point(point.x, point.y - (WALL_THICKNESS +1));
		if ((up.y > 0) && !visited.contains(up)) {
			neighbours.add(up);
		}
		
		Point down = new Point(point.x, point.y + (WALL_THICKNESS +1));
		if ((down.y < height) && !visited.contains(down)) {
			neighbours.add(down);
		}
		
		return neighbours;
	}
	
	private void widenCorners(LayerDefinition layer) {
		// top left corner
		setCollide(layer, WALL_THICKNESS - 1, WALL_THICKNESS - 1, false);
		setCollide(layer, WALL_THICKNESS - 1, WALL_THICKNESS, false);
		setCollide(layer, WALL_THICKNESS, WALL_THICKNESS - 1, false);
		// top right corner
		setCollide(layer, width - width % (WALL_THICKNESS + 1) - 1, WALL_THICKNESS - 1, false);
		setCollide(layer, width - width % (WALL_THICKNESS + 1), WALL_THICKNESS - 1, false);
		setCollide(layer, width - width % (WALL_THICKNESS + 1), WALL_THICKNESS, false);
		// bottom left corner
		setCollide(layer, WALL_THICKNESS - 1, height - height % (WALL_THICKNESS + 1) - 1, false);
		setCollide(layer, WALL_THICKNESS - 1, height - height % (WALL_THICKNESS + 1), false);
		setCollide(layer, WALL_THICKNESS, height - height % (WALL_THICKNESS + 1), false);
		// bottom right corner
		setCollide(layer, width - width % (WALL_THICKNESS + 1) - 1, height - height % (WALL_THICKNESS + 1), false);
		setCollide(layer, width - width % (WALL_THICKNESS + 1), height - height % (WALL_THICKNESS + 1), false);
		setCollide(layer, width - width % (WALL_THICKNESS + 1), height - height % (WALL_THICKNESS + 1) - 1, false);
	}
	
	private List<Point> getCorners() {
		if (corners == null) {
			corners = new LinkedList<Point>();
			// Order is important. Opposite corners should not be next to each other
			corners.add(new Point(WALL_THICKNESS, WALL_THICKNESS));
			corners.add(new Point(width - width % (WALL_THICKNESS + 1) - 1, WALL_THICKNESS));
			corners.add(new Point(width - width % (WALL_THICKNESS + 1) - 1, 
					height - height % (WALL_THICKNESS + 1) - 1));
			corners.add(new Point(WALL_THICKNESS, height - height % (WALL_THICKNESS + 1) - 1));
		}
		
		return corners;
	}
	
	private Point getPortalPosition() {
		// opposite corner to start
		Point start = getStartPosition();
		Point pos = (Point) getCorners().get((getCorners().indexOf(start) + 2) % 4).clone();
		// shift a bit to put the portal deeper in the corner
		pos.x += (start.x > pos.x) ? -1 : 1;
		pos.y += (start.y > pos.y) ? -1 : 1;
		
		return pos;
	}
	
	private void setCollide(LayerDefinition layer, int x, int y, boolean collide) {
		layer.set(x, y, collide ? 1 : 0);
	}
	
	private StendhalRPZone generateZone() {
		mapStructure.build();
		
		final StendhalRPZone zone = new StendhalRPZone(name, width, height);
		
		try {
			zone.addTilesets(name + ".tilesets", mapStructure.getTilesets());
			zone.addLayer(name + ".0_floor", mapStructure.getLayer("0_floor"));
			zone.addLayer(name + ".1_terrain", mapStructure.getLayer("1_terrain"));
			zone.addLayer(name + ".2_object", mapStructure.getLayer("2_object"));
			zone.addLayer(name + ".3_roof", mapStructure.getLayer("3_roof"));

			zone.addCollisionLayer(name + ".collision",
					mapStructure.getLayer("collision"));
			zone.addProtectionLayer(name + ".protection",
					mapStructure.getLayer("protection"));
		} catch (IOException e) {
			logger.error(e);
		}
		
		// Create the return portal
		Teleporter portal = new ReturnTeleporter(new Spot(SingletonRepository.getRPWorld().getZone(returnZoneName), returnX, returnY));
		Point pos = getPortalPosition();
		portal.setPosition(pos.x, pos.y);
		zone.add(portal);

		// disable double click move and teleport in
		zone.setMoveToAllowed(false);
		zone.disallowIn();
		
		addPrizes(zone);
		
		zone.addMovementListener(new MazeMovementListener());
		return zone;
	}
	
	/**
	 * Drop random prizes to the side corners.
	 * 
	 * @param zone the maze zone to drop the items
	 */
	private void addPrizes(StendhalRPZone zone) {
		int idx = getCorners().indexOf(getStartPosition());
		
		for (int i = 1; i <= 3; i += 2) {
			Item prize = SingletonRepository.getEntityManager().getItem(Rand.rand(prizes));
			Point location = getCorners().get((idx + i) % 4);
			prize.setPosition(location.x, location.y);
			
			zone.add(prize, true);
		}
	}
	
	/**
	 * A listener to destroy the zone when players have left and to return the 
	 * player to the right place in case she logged out.
	 */
	private final class MazeMovementListener implements MovementListener {
		public Rectangle2D getArea() {
			return new Rectangle2D.Double(0, 0, width, height);
		}

		public void onEntered(final ActiveEntity entity, final StendhalRPZone zone, final int newX,
				final int newY) {
			// ignore
		}

		public void onExited(final ActiveEntity entity, final StendhalRPZone zone, final int oldX,
				final int oldY) {
			if (!(entity instanceof Player)) {
				return;
			}
			if (zone.getPlayers().size() == 1) {
				// since we are about to destroy the arena, change the player zoneid to house so that 
				// if they are relogging, they can enter back to the house (not the default zone of PlayerRPClass). 
				// If they are out or walking out the portal it works as before.
				entity.put("zoneid", returnZoneName);
				entity.put("x", returnX);
				entity.put("y", returnY);
				SingletonRepository.getRPWorld().removeZone(zone);
			}
		}

		public void onMoved(final ActiveEntity entity, final StendhalRPZone zone, final int oldX,
				final int oldY, final int newX, final int newY) {
			// ignore
		}
	}
	
	protected void rewardPlayer(Player player) {
		long timediff = System.currentTimeMillis() - timeStamp;
		double normalized = timediff / (double) (DEFAULT_SOLVING_TIME * MathHelper.MILLISECONDS_IN_ONE_MINUTE);
		// theoretical maximum e * DEFAULT_REWARD_POINTS
		int points = (int) (DEFAULT_REWARD_POINTS * Math.exp(1 - normalized));
		
		SingletonRepository.getRuleProcessor().addHallOfFamePoints(player.getName(), "M", points);
	}
	
	private class ReturnTeleporter extends Teleporter {
		public ReturnTeleporter(Spot spot) {
			super(spot);
		}

		@Override
		protected boolean usePortal(final Player player) {
			boolean success = super.usePortal(player);
			if (success) {
				rewardPlayer(player);
				sign.updatePlayers();
			}
			return success;
		}
	}
}
