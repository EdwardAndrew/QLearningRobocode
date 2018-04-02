package com.github.EdwardAndrew;

import robocode.*;
import com.github.EdwardAndrew.action.*;

import java.util.Random;

public class QLearningRobot extends AdvancedRobot {

    /* There are 8 compass directions the robot can travel in,
        N, NE, E, SE, S, SW, W, NW.

        The distance to the enemy is separated into 4 groups.
        Close, Medium, Far, Very (too) far.

        The battlefield position is divided into 9 areas.
        Top Left Corner, Top Middle, Top Right Corener.
        Left Middle. Centre of battlefield. Right Middle
        Bottom Left Corner. Bottom Middle. Bottom Right Corner.
     */

    private final int battleFieldXStateCount = 10;
    private final int battleFieldYStateCount = 10;
    private final int enemyBearingStateCount = 8;
    private final int enemyDistanceStateCount = 4;

    private int currentXPositionState = 0;
    private int currentYPositionState = 0;

    // Enemy distance can be Close, Medium, Far or Very Far.
    private int enemyDistanceState = 0;
    private int enemyBearingState = 0;

    private final int actionCounnt = 8;

    float[][][][][] QValues = new float[battleFieldXStateCount][battleFieldYStateCount][enemyBearingStateCount][enemyDistanceStateCount][actionCounnt];

    public void run()
    {
        // load in Q values

        while(true){
            // Find the current states.
            currentXPositionState = getBattledfieldGrid( this.getX(), this.getBattleFieldWidth(),  battleFieldXStateCount );
            currentYPositionState = getBattledfieldGrid( this.getY(), this.getBattleFieldHeight(), battleFieldYStateCount );

            setTurnHeading( Action.MOVE_WEST.getBearing() );
            setAhead( 100 );
            execute();
        }
    }

    void setTurnHeading(double bearing)
    {
        double normalisedBearing = normalizeBearing( this.getHeading() );
        double headingDelta = bearing - normalisedBearing;

        if(headingDelta > 0)
        {
            setTurnRight( headingDelta );
        }
        else
        {
            setTurnLeft( Math.abs( headingDelta ) );
        }
    }

    double getAbsoluteBearing(double degrees)
    {
        double absoluteBearing = this.getHeading() + degrees;

        if( absoluteBearing > 360 ) absoluteBearing -= 360;
        if( absoluteBearing < 0 ) absoluteBearing += 360;

        return absoluteBearing;
    }

    public void onScannedRobot(ScannedRobotEvent scannedRobotEvent)
    {
        enemyDistanceState =  getEnemyDistanceState( scannedRobotEvent.getDistance() );
        enemyBearingState =  bearingToState( getAbsoluteBearing( scannedRobotEvent.getBearing() ), enemyBearingStateCount ) ;
    }

    public void onRoundEnded(RoundEndedEvent roundEndedEvent)
    {
        // Save the Q values
    }

    void save(){
    }

    void load(){
    }

    static double normalizeBearing(double angle) {
        while (angle >=  180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    static int getBattledfieldGrid( double position, double battleFieldSize, int numberOfGrids )
    {
        double gridXSize = battleFieldSize / numberOfGrids;
        return (int)Math.floor( position / gridXSize );
    }

    static int getEnemyDistanceState(double distance)
    {
        if( distance < 150 ) return 0;
        if( distance < 300 ) return 1;
        if( distance < 500 ) return 2;
        return 3;
    }

    static int randomInteger(int min, int max)
    {
        Random rand = new Random();

        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    static int bearingToState(double bearing, int totalEnemyBearingStates)
    {
        double stateSize = 360 / totalEnemyBearingStates;
        bearing += stateSize / 2;

        while (bearing >= 360) bearing -= 360;
        while (bearing < 0) bearing += 360;

        return (int)Math.floor( bearing / stateSize );
    }
}
