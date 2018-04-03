package com.github.EdwardAndrew.QLearning.action;

public enum Action {

    TURN_NORTH(0),
    TURN_NORTH_EAST(45),
    TURN_EAST(90),
    TURN_SOUTH_EAST(135),
    TURN_SOUTH(180),
    TURN_SOUTH_WEST(-135),
    TURN_WEST(-90),
    TURN_NORTH_WEST(-45),
    STAY(-1),
    MOVE_FORWARD(-1),
    MOVE_BACKWARD(-1);


    private double value;

    Action(double value)
    {
        this.value = value;
    }

    public double getBearing()
    {
        return this.value;
    }
}
