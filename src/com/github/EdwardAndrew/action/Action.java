package com.github.EdwardAndrew.action;

public enum Action {

    MOVE_NORTH(0),
    MOVE_NORTH_EAST(45),
    MOVE_EAST(90),
    MOVE_SOUTH_EAST(135),
    MOVE_SOUTH(180),
    MOVE_SOUTH_WEST(-135),
    MOVE_WEST(-90),
    MOVE_NORTH_WEST(-45);

    private double value;

    Action(double value){
        this.value = value;
    }
    public double getBearing(){
        return this.value;
    }
}
