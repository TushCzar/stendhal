/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server.pathfinder;

import games.stendhal.common.Direction;
import games.stendhal.server.StendhalRPWorld;
import games.stendhal.server.StendhalRPZone;
import games.stendhal.server.entity.Entity;
import games.stendhal.server.entity.RPEntity;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import marauroa.common.Log4J;

import org.apache.log4j.Logger;

public class Path {
	/** the logger instance. */
	private static final Logger logger = Log4J.getLogger(Path.class);

	private static StepCallback callback;

	/** The maximum time spent on a search for one particular path (in ms) */
	// Decreased turn time to 10ms see: http://sourceforge.net/tracker/index.php?func=detail&aid=1532769&group_id=1111&atid=101111
	private static final int MAX_PATHFINDING_TIME = 10;

	public static int steps;

	public static class Node {
		public int x;

		public int y;

		public Node(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return "(" + x + "," + y + ")";
		}
	}

	/**
	 * Sets the step-callback. This will be called after each step. <b>Note:
	 * </b> This is a debug method and not part of the 'official api'.
	 */
	public static void setCallback(StepCallback callback) {
		Path.callback = callback;
	}

	private static void moveto(RPEntity entity, int x, int y, double speed) {
		int rndx = x - entity.getX();
		int rndy = y - entity.getY();

		if (Math.abs(rndx) > Math.abs(rndy)) {
			if (Math.signum(rndx) < 0) {
				entity.setDirection(Direction.LEFT);
				entity.setSpeed(speed);
			} else {
				entity.setDirection(Direction.RIGHT);
				entity.setSpeed(speed);
			}
		} else {
			if (Math.signum(rndy) < 0) {
				entity.setDirection(Direction.UP);
				entity.setSpeed(speed);
			} else {
				entity.setDirection(Direction.DOWN);
				entity.setSpeed(speed);
			}
		}
		entity.notifyWorldAboutChanges();
	}

	/**
	 * Finds a path for the Entity <code>entity</code>.
	 * 
	 * @param entity
	 *            the Entity
	 * @param x
	 *            start x
	 * @param y
	 *            start y
	 * @return a list with the path nodes or an empty list if no path is found
	 */
	public static List<Node> searchPath(Entity entity, int x, int y,
			Rectangle2D destination) {
		return searchPath(entity, x, y, destination, -1.0);
	}

	public static List<Node> searchPath(Entity entity, int ex, int ey) {
		return searchPath(entity, entity.getX(), entity.getY(), entity.getArea(
				ex, ey), -1.0);
	}

	/**
	 * Finds a path for the Entity <code>entity</code>.
	 * 
	 * @param entity
	 *            the Entity
	 * @param x
	 *            start x
	 * @param y
	 *            start y
	 * @param destination
	 *            the destination area
	 * @param maxDistance
	 *            the maximum distance (air line) a possible path may be
	 * @return a list with the path nodes or an empty list if no path is found
	 */
	public static List<Node> searchPath(Entity entity, int x, int y,
			Rectangle2D destination, double maxDistance) {
		return searchPath(entity, null, x, y, destination, maxDistance);
	}

	/**
	 * Finds a path for the Entity <code>entity</code>.
	 * 
	 * @param entity
	 *            the Entity
	 * @param zone
	 *            the zone, if null the current zone of entity is used. 			 
	 * @param x
	 *            start x
	 * @param y
	 *            start y
	 * @param destination
	 *            the destination area
	 * @param maxDistance
	 *            the maximum distance (air line) a possible path may be
	 * @return a list with the path nodes or an empty list if no path is found
	 */
	public static List<Node> searchPath(Entity entity, StendhalRPZone zone, int x, int y,
			Rectangle2D destination, double maxDistance) {
		
		if (zone == null) {
			zone = (StendhalRPZone) StendhalRPWorld.get().getRPZone(entity.getID());
		}
		
		// Log4J.startMethod(logger, "searchPath");
//		long startTimeNano = System.nanoTime(); 
		long startTime = System.currentTimeMillis();

		Pathfinder path = new Pathfinder();
		StendhalNavigable navMap = new StendhalNavigable(entity, zone,
				(int) destination.getX(), (int) destination.getY());

		path.setNavigable(navMap);
		path.setStart(new Pathfinder.Node(x, y));
		path.setGoal(new Pathfinder.Node((int) destination.getX(),
				(int) destination.getY()));

		steps = 0;
		path.init();
		// HACK: Time limited the A* search.
		while (path.getStatus() == Pathfinder.IN_PROGRESS
				&& ((System.currentTimeMillis() - startTime) < MAX_PATHFINDING_TIME)) {
			path.doStep();
			steps++;
			if (callback != null) {
				callback.stepDone(path.getBestNode());
			}
		}

		if (path.getStatus() == Pathfinder.IN_PROGRESS) {
//			logger.warn("Pathfinding aborted: " + entity.get("name") + " (" + x + ", " + y + ")    Average Pathfinding time: " + ((float)time / counter / 1000000));
			return new LinkedList<Node>();
		}
//		time = time + System.nanoTime() - startTimeNano;
//		counter++;

		long endTime = System.currentTimeMillis();
		logger.debug("Route (" + x + "," + y + ")-(" + destination + ") S:"
				+ steps + " OL:" + path.getOpen().size() + " CL:"
				+ path.getClosed().size() + " in " + (endTime - startTime)
				+ "ms");

		List<Node> list = new LinkedList<Node>();
		Pathfinder.Node node = path.getBestNode();
		while (node != null) {
			list.add(0, new Node(node.getX(), node.getY()));
			node = node.getParent();
		}

		// Log4J.finishMethod(logger, "searchPath");
		return list;
	}

	/**
	 * Finds a path for the Entity <code>entity</code> to the other Entity
	 * <code>dest</code>.
	 * 
	 * @param entity
	 *            the Entity (also start point)
	 * @param dest
	 *            the destination Entity
	 */
	public static void searchPathAsynchonous(RPEntity entity, Entity dest) {
		StendhalRPWorld.get().checkPathfinder();

		boolean result = StendhalRPWorld.get().getPathfinder().queuePath(
				new QueuedPath(new SimplePathListener(entity), entity, entity
						.getX(), entity.getY(), dest.getArea(dest.getX(), dest
						.getY())));

		if (!result) {
			logger.warn("Pathfinder queue is full...path not added");
		}
	}

	/**
	 * Finds a path for the Entity <code>entity</code> to the other Entity
	 * <code>dest</code>.
	 * 
	 * @param entity
	 *            the Entity (also start point)
	 * @param dest
	 *            the destination Entity
	 * @return a list with the path nodes or an empty list if no path is found
	 */
	public static List<Node> searchPath(Entity entity, Entity dest) {
		return searchPath(entity, entity.getX(), entity.getY(), dest.getArea(
				dest.getX(), dest.getY()));
	}

	/**
	 * Finds a path for the Entity <code>entity</code> to the other Entity
	 * <code>dest</code>.
	 * 
	 * @param entity
	 *            the Entity (also start point)
	 * @param dest
	 *            the destination Entity
	 * @param maxDistance
	 *            the maximum distance (air line) a possible path may be
	 * @return a list with the path nodes or an empty list if no path is found
	 */
	public static List<Node> searchPath(Entity entity, Entity dest,
			double maxDistance) {
		return searchPath(entity, entity.getX(), entity.getY(), new Rectangle(
				dest.getX(), dest.getY(), 1, 1), maxDistance);
	}

	public static boolean followPath(RPEntity entity, double speed) {
		List<Node> path = entity.getPath();

		if (path.size() == 0) {
			return true;
		}

		int pos = entity.getPathPosition();

		Node actual = path.get(pos);

		if (entity.squaredDistance(actual.x, actual.y) == 0) {
			logger.debug("Completed waypoint(" + pos + ")(" + actual.x + ","
					+ actual.y + ") on Path");
			pos++;
			if (pos < path.size()) {
				entity.setPathPosition(pos);
				actual = path.get(pos);
				logger.debug("Moving to waypoint(" + pos + ")(" + actual.x
						+ "," + actual.y + ") on Path from (" + entity.getX()
						+ "," + entity.getY() + ")");
				moveto(entity, actual.x, actual.y, speed);
				return false;
			} else {
				if (entity.isPathLoop()) {
					entity.setPathPosition(0);
				} else {
					entity.stop();
				}

				return true;
			}
		} else {
			logger.debug("Moving to waypoint(" + pos + ")(" + actual.x + ","
					+ actual.y + ") on Path from (" + entity.getX() + ","
					+ entity.getY() + ")");
			moveto(entity, actual.x, actual.y, speed);
			return false;
		}
	}

	/** this callback is called after every A* step. */
	public interface StepCallback {
		public void stepDone(Pathfinder.Node lastNode);
	}

	/** the threaded-pathfinder callback */
	private static class SimplePathListener implements PathListener {
		/** the entity the path belongs to */
		private RPEntity entity;

		/**
		 * creates a new instance of SimplePathLister
		 * 
		 * @param entity
		 *            the entity the path belongs to
		 */
		public SimplePathListener(RPEntity entity) {
			this.entity = entity;
		}

		/** simply appends the calculated path to the entitys path */
		public void onPathFinished(QueuedPath path, PathState state) {
			if (state == PathState.PATH_FOUND) {
				entity.addToPath(path.getPath());
			}
		}
	}
}
