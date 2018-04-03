package com.github.EdwardAndrew;
import com.github.EdwardAndrew.QLearning.action.Action;
import robocode.*;

import java.io.*;
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

    // Enemy distance can be Close, Medium, Far or Very Far.
    private int enemyDistanceState = 0;
    private int enemyBearingState = 0;

    private final int actionCount = 9;

    private float reward = 0.0f;

    private int[][][][][] QValues = new int[battleFieldXStateCount][battleFieldYStateCount][enemyBearingStateCount][enemyDistanceStateCount][actionCount];

    public void run()
    {
        setAdjustRadarForGunTurn(false);
        setAdjustGunForRobotTurn(true);

        float gamma = 0.8f;
        float epsilon = 0.2f;


        // load in Q values
        load();

        while(true){
            // Reset reward value.
            reward = 0;

            // Find the current states.
            int lastXPositionState = getBattlefieldGrid( this.getX(), this.getBattleFieldWidth(),  battleFieldXStateCount );
            int lastYPositionState = getBattlefieldGrid( this.getY(), this.getBattleFieldHeight(), battleFieldYStateCount );

            int lastEnemyBearingState = enemyBearingState;
            int lastEnemyDistanceState  = enemyDistanceState;

            int action = 0;

            if(getRandomFloat(0,1) < epsilon )
            {
                action = getRandomInteger(0, actionCount-1);
            }
            else
            {
                action = getMaximumActionForState(lastXPositionState, lastYPositionState, lastEnemyBearingState, lastEnemyDistanceState);
            }

            performAction(action);

            execute();

            int outcomeXPositionState = getBattlefieldGrid( this.getX(), this.getBattleFieldWidth(),  battleFieldXStateCount );
            int outcomeYPositionState = getBattlefieldGrid( this.getY(), this.getBattleFieldHeight(), battleFieldYStateCount );

            QValues[lastXPositionState][lastYPositionState][lastEnemyBearingState][lastEnemyDistanceState][action] = (byte)(
            reward + gamma * getMaximumQValueForState(outcomeXPositionState, outcomeYPositionState, enemyBearingState, enemyDistanceState));

        }
    }

    private void performAction(int action)
    {
        if(action == 8)
        {
            stop();
        }
        else
        {
            turnHeading(Action.values()[action].getBearing());
            setAhead(100);
        }

        execute();

        turnGunLeft(360);
    }

    private void turnHeading(double bearing)
    {
        double normalisedBearing = normalizeBearing( this.getHeading() );
        double headingDelta = bearing - normalisedBearing;

        if(headingDelta > 0)
        {
            turnRight( headingDelta );
        }
        else
        {
            turnLeft( Math.abs( headingDelta ) );
        }
    }

//    private void turnGun(double bearing){
//        double normalisedBearing = normalizeBearing( this.getGunHeading() );
//        double headingDelta = bearing - normalisedBearing;
//
//        if(headingDelta > 0)
//        {
//            turnGunRight( headingDelta );
//        }
//        else
//        {
//            turnGunLeft( Math.abs( headingDelta ) );
//        }
//    }

    private int getMaximumActionForState(int XPositionState, int YPositionState, int enemyBearingState, int enemyDistanceState)
    {
        int highestQValueIndex =0;

        for(int actionIndex = 0; actionIndex < actionCount; actionIndex++)
        {
            if(QValues[XPositionState][YPositionState][enemyBearingState][enemyDistanceState][actionIndex] > highestQValueIndex)
            {
                highestQValueIndex = actionIndex;
            }
        }

        return  highestQValueIndex;
    }

    private int getMaximumQValueForState(int XPositionState, int YPositionState, int enemyBearingState, int enemyDistanceState){
       return QValues[XPositionState][YPositionState][enemyBearingState][enemyDistanceState][getMaximumActionForState(XPositionState, YPositionState, enemyBearingState, enemyDistanceState )];
    }


    private double getAbsoluteBearing(double degrees)
    {
        double absoluteBearing = this.getHeading() + degrees;

        if( absoluteBearing > 360 ) absoluteBearing -= 360;
        if( absoluteBearing < 0 ) absoluteBearing += 360;

        return absoluteBearing;
    }

    public void onScannedRobot(ScannedRobotEvent scannedRobotEvent)
    {
        enemyDistanceState =  getEnemyDistanceState( scannedRobotEvent.getDistance() );
        double enemyBearing = getAbsoluteBearing( scannedRobotEvent.getBearing());
        enemyBearingState =  bearingToState( enemyBearing , enemyBearingStateCount ) ;

        //turnGun(enemyBearing);


        fire(1);

    }

    public void onDeath(DeathEvent e) { reward -= 100; }
    public void onHitByBullet(HitByBulletEvent e){ reward -= 2;}
    public void onHitRobot(HitRobotEvent e){ reward -= 2;}
    public  void onBulletHit(BulletHitEvent e) { reward += 5; }
    public void onHitWall(HitWallEvent e){
        reward -= 20;
        //TODO: Check which wall we've hit and drive away from it.

    }
    public void onWin(WinEvent e){reward += 100;}
    public void onRoundEnded(RoundEndedEvent roundEndedEvent) {
        save();
    }


    private void normaliseQMatrix()
    {
        int highestValue = QValues[0][0][0][0][0];

        for(int xState = 0; xState < battleFieldXStateCount; xState++)
        {
            for(int yState =0; yState < battleFieldYStateCount; yState++)
            {
                for(int enemyBearingState = 0; enemyBearingState < enemyBearingStateCount; enemyBearingState++)
                {
                    for(int enemyDistanceState = 0; enemyDistanceState < enemyDistanceStateCount; enemyDistanceState++)
                    {
                        for(int action = 0; action < actionCount; action++)
                        {
                            if(QValues[xState][yState][enemyBearingState][enemyDistanceState][action] > highestValue)
                            {
                                highestValue = QValues[xState][yState][enemyBearingState][enemyDistanceState][action];
                            }
                        }
                    }
                }
            }
        }
        for(int xState = 0; xState < battleFieldXStateCount; xState++)
        {
            for(int yState =0; yState < battleFieldYStateCount; yState++)
            {
                for(int enemyBearingState = 0; enemyBearingState < enemyBearingStateCount; enemyBearingState++)
                {
                    for(int enemyDistanceState = 0; enemyDistanceState < enemyDistanceStateCount; enemyDistanceState++)
                    {
                        for(int action = 0; action < actionCount; action++)
                        {
                            if(QValues[xState][yState][enemyBearingState][enemyDistanceState][action] > highestValue)
                            {
                                QValues[xState][yState][enemyBearingState][enemyDistanceState][action] = (QValues[xState][yState][enemyBearingState][enemyDistanceState][action] / highestValue)*255;
                            }
                        }
                    }
                }
            }
        }


    }

    private void save(){

        PrintStream printStream = null;

        try{
            printStream = new PrintStream(new RobocodeFileOutputStream(getDataFile("QValues.data")));

            normaliseQMatrix();

            for(int xState = 0; xState < battleFieldXStateCount; xState++)
            {
                for(int yState = 0; yState < battleFieldYStateCount; yState++)
                {
                    for(int enemyBearingState = 0; enemyBearingState < enemyBearingStateCount; enemyBearingState++)
                    {
                        for(int enemyDistanceState = 0; enemyDistanceState < enemyDistanceStateCount; enemyDistanceState++)
                        {
                            for(int action = 0; action < actionCount; action++)
                            {
                                printStream.print(QValues[xState][yState][enemyBearingState][enemyDistanceState][action]);
                                printStream.print("\n");
                            }
                        }
                    }
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        finally {
            if(printStream != null)
            {
                printStream.flush();
                printStream.close();
            }
        }
    }

    private void load(){

        File f = new File(getDataFile("QValues.data").toString());

        if(f.exists() && !f.isDirectory()) {
            try {
//                Scanner readFile = new Scanner(getDataFile("QValues.data").toString());
//                readFile.useDelimiter(":");
                BufferedReader bufferedReader = new BufferedReader(new FileReader(getDataFile("QValues.data")));
                for (int xState = 0; xState < battleFieldXStateCount; xState++) {
                    for (int yState = 0; yState < battleFieldYStateCount; yState++) {
                        for (int enemyBearingState = 0; enemyBearingState < enemyBearingStateCount; enemyBearingState++) {
                            for (int enemyDistanceState = 0; enemyDistanceState < enemyDistanceStateCount; enemyDistanceState++) {
                                for (int action = 0; action < actionCount; action++) {
//                                    if(readFile.hasNextByte())
//                                    {
//                                        QValues[xState][yState][enemyBearingState][enemyDistanceState][action] = readFile.nextByte();
//                                    }
//                                    else
//                                    {
//                                        System.out.println("Failed to read");
//                                        System.out.println("X:" + xState + " Y:"+yState+" B:"+enemyBearingState+" D:"+enemyDistanceState+" A:"+action);
//                                        break;
//                                    }

                                    QValues[xState][yState][enemyBearingState][enemyDistanceState][action] = Integer.parseInt(bufferedReader.readLine());
                                }
                            }
                        }
                    }
                }
                bufferedReader.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private static double normalizeBearing(double angle) {
        while (angle >=  180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    private static int getBattlefieldGrid(double position, double battleFieldSize, int numberOfGrids )
    {
        double gridXSize = battleFieldSize / numberOfGrids;
        return (int)Math.floor( position / gridXSize );
    }

    private static int getEnemyDistanceState(double distance)
    {
        if( distance < 150 ) return 0;
        if( distance < 300 ) return 1;
        if( distance < 500 ) return 2;
        return 3;
    }

    private static int getRandomInteger(int min, int max)
    {
        return new Random().nextInt((max - min) + 1) + min;
    }

    private static float getRandomFloat(float min, float max)
    {
        return new Random().nextFloat() * (max - min) + min;
    }

    private static int bearingToState(double bearing, int totalEnemyBearingStates)
    {
        double stateSize = 360 / totalEnemyBearingStates;
        bearing += stateSize / 2;

        while (bearing >= 360) bearing -= 360;
        while (bearing < 0) bearing += 360;

        return (int)Math.floor( bearing / stateSize );
    }
}
