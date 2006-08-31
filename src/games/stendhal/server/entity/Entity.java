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
package games.stendhal.server.entity;

import games.stendhal.common.Direction;
import games.stendhal.server.StendhalRPWorld;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import marauroa.common.game.AttributeNotFoundException;
import marauroa.common.game.RPClass;
import marauroa.common.game.RPObject;

public abstract class Entity extends RPObject {
	private int x;

	private int y;

	private Direction direction;

	private double speed;

	private boolean collides;

	public static void generateRPClass() {
		RPClass entity = new RPClass("entity");
		entity.add("description", RPClass.LONG_STRING, RPClass.HIDDEN); // Some things may have a textual description
		entity.add("x", RPClass.SHORT);
		entity.add("y", RPClass.SHORT);
		entity.add("dir", RPClass.BYTE, RPClass.VOLATILE);
		entity.add("speed", RPClass.FLOAT, RPClass.VOLATILE);
	}

	public Entity(RPObject object) throws AttributeNotFoundException {
		super(object);
		direction = Direction.STOP;
		speed = 0;
		update();
	}

	public Entity() throws AttributeNotFoundException {
		super();
	}

	public void update() throws AttributeNotFoundException {
		if (has("x")) {
			x = getInt("x");
		}
		if (has("y")) {
			y = getInt("y");
		}
		if (has("speed")) {
			speed = getDouble("speed");
		}
		if (has("dir")) {
			direction = Direction.build(getInt("dir"));
		}
	}

	public boolean hasDescription() {
		if (has("description")) {
			return (getDescription() != null && getDescription().length() > 0);
		}
		return (false);
	}

	public void setDescription(String text) {
		if (text == null) {
			text = "";
		}
		put("description", text);
	}

	public String getDescription() {
		String description = "";
		if (has("description")) {
			description = get("description");
		}
		return description;
	}

	public void set(int x, int y) {
		setX(x);
		setY(y);
	}

	/**
	 * @deprecated use setX() instead
	 */
	@Deprecated
	public void setx(int x) {
		setX(x);
	}
	
	/**
	 * @deprecated use getX() instead
	 */
	@Deprecated
	public int getx() {
		return getX();
	}

	/**
	 * @deprecated use setY() instead
	 */
	@Deprecated
	public void sety(int y) {
		setY(y);
	}
	
	/**
	 * @deprecated use getY() instead
	 */
	@Deprecated
	public int gety() {
		return getY();
	}

	public void setX(int x) {
		if (x == this.x && x != 0) {
			return;
		}
		this.x = x;
		put("x", x);
	}
	
	public int getX() {
		return x;
	}

	public void setY(int y) {
		if (y == this.y && y != 0) {
			return;
		}
		this.y = y;
		put("y", y);
	}

	public int getY() {
		return y;
	}

	public void setDirection(Direction dir) {
		if (dir == this.direction) {
			return;
		}
		this.direction = dir;
		put("dir", direction.get());
	}

	public Direction getDirection() {
		return direction;
	}

	public void setSpeed(double speed) {
		if (speed == this.speed) {
			return;
		}
		this.speed = speed;
		put("speed", speed);
	}

	public double getSpeed() {
		return speed;
	}

	private int turnsToCompleteMove;

	public boolean isMoveCompleted() {
		++turnsToCompleteMove;
		if (turnsToCompleteMove >= 1.0 / speed) {
			turnsToCompleteMove = 0;
			return true;
		}
		return false;
	}

	public void stop() {
		setSpeed(0);
	}

	public boolean stopped() {
		return speed == 0;
	}

	public void setCollides(boolean collides) {
		this.collides = collides;
	}

	/**
	 * TODO: docu
	 * @return ???
	 */
	public boolean collides() {
		return collides;
	}

	
	/**
	 * Checks whether players, NPCs etc. can walk over this entity.
	 * @return true iff it is impossible to walk over this entity
	 */
	public boolean isObstacle() {
		return true;
	}

	/**
	 * This returns square of the distance between this entity and the
	 * given one.
	 * We're calculating the square because the square root operation
	 * would be expensive. As long as we only need to compare distances,
	 * it doesn't matter if we compare the distances or the squares of
	 * the distances (the square operation is strictly monotonous for positive
	 * numbers).
	 * @param entity the entity to which the distance should be calculated 
	 */
	public double squaredDistance(Entity entity) {
		return squaredDistance(entity.x, entity.y);
	}

	/**
	 * This returns square of the distance from this entity to a specific
	 * point.
	 * We're calculating the square because the square root operation
	 * would be expensive. As long as we only need to compare distances,
	 * it doesn't matter if we compare the distances or the squares of
	 * the distances (the square operation is strictly monotonous for positive
	 * numbers).
	 * @param x The horizontal coordinate of the point
	 * @param y The vertical coordinate of the point
	 */
	public double squaredDistance(int x, int y) {
		return (x - this.x) * (x - this.x) + (y - this.y) * (y - this.y);
	}

	/**
	 * Checks whether a certain point is near this entity.
	 * @param x The point's x coordinate
	 * @param y The point's y coordinate
	 * @param step The maximum distance
	 * @return true iff the point is at most <i>step</i> steps away
	 */
	public boolean nextTo(int x, int y, double step) {
		Rectangle2D thisArea = getArea(this.x, this.y);
		thisArea.setRect(thisArea.getX() - step, thisArea.getY() - step,
				thisArea.getWidth() + step, thisArea.getHeight() + step);
		return thisArea.contains(x, y);
	}

	/**
	 * Checks whether the given entity is near this entity.
	 * @param entity the entity
	 * @param step The maximum distance
	 * @return true iff the entity is at most <i>step</i> steps away
	 */
	public boolean nextTo(Entity entity, double step) {
		Rectangle2D thisArea = getArea(x, y);
		Rectangle2D otherArea = entity.getArea(entity.x, entity.y);
		thisArea.setRect(thisArea.getX() - step, thisArea.getY() - step,
				thisArea.getWidth() + step, thisArea.getHeight() + step);
		otherArea.setRect(otherArea.getX() - step, otherArea.getY() - step,
				otherArea.getWidth() + step, otherArea.getHeight() + step);
		return thisArea.intersects(otherArea);
	}

	public boolean facingTo(Entity entity) {
		Rectangle2D thisArea = getArea(x, y);
		Rectangle2D otherArea = entity.getArea(entity.x, entity.y);
		if (direction == Direction.UP && thisArea.getX() == otherArea.getX()
				&& thisArea.getY() - 1 == otherArea.getY()) {
			return true;
		}
		if (direction == Direction.DOWN
				&& thisArea.getX() == otherArea.getX()
				&& thisArea.getY() + 1 == otherArea.getY()) {
			return true;
		}
		if (direction == Direction.LEFT
				&& thisArea.getY() == otherArea.getY()
				&& thisArea.getX() - 1 == otherArea.getX()) {
			return true;
		}
		if (direction == Direction.RIGHT
				&& thisArea.getY() == otherArea.getY()
				&& thisArea.getX() + 1 == otherArea.getX()) {
			return true;
		}
		return false;
	}

	public void faceTo(Entity entity) {
		Rectangle2D otherArea = entity.getArea(entity.getX(), entity.getY());
		setDirection(directionTo((int) otherArea.getX(), (int) otherArea.getY()));
	}

	private Direction directionTo(int px, int py) {
		Rectangle2D area = getArea(x, y);
		int rx = (int) area.getX();
		int ry = (int) area.getY();
		if (Math.abs(px - rx) > Math.abs(py - ry)) {
			if (px - rx > 0) {
				return Direction.RIGHT;
			} else {
				return Direction.LEFT;
			}
		} else {
			if (py - ry > 0) {
				return Direction.DOWN;
			} else {
				return Direction.UP;
			}
		}
	}

	public Rectangle2D getArea(double ex, double ey) {
		Rectangle2D rect = new Rectangle.Double();
		getArea(rect, ex, ey);
		return rect;
	}

	abstract public void getArea(Rectangle2D rect, double x, double y);
	
	/**
	 * Notifies the StendhalRPWorld that this entity's attributes have
	 * changed.
	 * 
	 * TODO: Find a way to move this up to RPObject.
	 */
	public void notifyWorldAboutChanges() {
		StendhalRPWorld.get().modify(this);
	}

	public String describe() {
		String ret = "You see ";
		if (hasDescription()) {
			return (getDescription());
		}
		if (has("name") && get("name") != null) {
			ret += get("name").replace("_", " ");
		} else if (has("subclass")) {
			ret += "a " + get("subclass");
		} else if (has("class")) {
			ret += "a " + get("class");
		} else {
			ret += "something rather undescribed";
			if (has("type")) {
				ret += " of type " + get("type");
			}
			if (has("id")) {
				ret += " with id " + get("id");
			}
			if (has("zone")) {
				ret += " in zone " + get("zone");
			}
		}
		return (ret + ".");
	}
}
