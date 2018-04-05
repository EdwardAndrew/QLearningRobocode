package com.github.EdwardAndrew.QLearning.action;

/***
 * The Actions that the robot can take.
 */
public enum Action {

    TURN_NORTH(0),
    TURN_NORTH_EAST(45),
    TURN_EAST(90),
    TURN_SOUTH_EAST(135),
    TURN_SOUTH(180),
    TURN_SOUTH_WEST(-135),
    TURN_WEST(-90),
    TURN_NORTH_WEST(-45),
    STAY(-1); // Value for STAY will not be used.

    private double degrees;

    /***
     * Constructor, degrees indicate which direction the robot should turn towards.
     * @param degrees
     */
    Action(double degrees)
    {
        this.degrees = degrees;
    }

    /**
     * Get the direction of this action.
     * @return Direction bearing in absolute degrees.
     */
    public double getBearing()
    {
        return this.degrees;
    }
}
