package com.github.EdwardAndrew;
import com.github.EdwardAndrew.QLearning.action.Action;
import robocode.*;

import java.io.*;
import java.util.InputMismatchException;
import java.util.Scanner;
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

    private final int actionCount = 11;
    private float gamma = 0.8f;
    private float epsilon = 0.2f;

    private float reward = 0.0f;

    int[][][][][] QValues = new int[battleFieldXStateCount][battleFieldYStateCount][enemyBearingStateCount][enemyDistanceStateCount][actionCount];


    public void run()
    {
        // load in Q values
        load();

        while(true){
            // Reset reward value.
            reward = 0;

            // Find the current states.
            int lastXPositionState = getBattledfieldGrid( this.getX(), this.getBattleFieldWidth(),  battleFieldXStateCount );
            int lastYPositionState = getBattledfieldGrid( this.getY(), this.getBattleFieldHeight(), battleFieldYStateCount );

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

            int outcomeXPositionState = getBattledfieldGrid( this.getX(), this.getBattleFieldWidth(),  battleFieldXStateCount );
            int outcomeYPositionState = getBattledfieldGrid( this.getY(), this.getBattleFieldHeight(), battleFieldYStateCount );


            QValues[lastXPositionState][lastYPositionState][lastEnemyBearingState][lastEnemyDistanceState][action] = (byte)(
                    reward + gamma * getMaximumQValueForState(outcomeXPositionState, outcomeYPositionState, enemyBearingState, enemyDistanceState));

        }
    }

    void performAction(int action)
    {
        if(action == 8)
        {
        }
        else if(action == 9)
        {
            setAhead(100);
        }
        else if(action == 10)
        {
            setBack(100);
        }
        else
        {
            turnHeading(Action.values()[action].getBearing());
        }
        setTurnGunLeft(45);
    }

    void turnHeading(double bearing)
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

    int getMaximumActionForState(int XPositionState, int YPositionState, int enemyBearingState, int enemyDistanceState)
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

    int getMaximumQValueForState(int XPositionState, int YPositionState, int enemyBearingState, int enemyDistanceState){
       return QValues[XPositionState][YPositionState][enemyBearingState][enemyDistanceState][getMaximumActionForState(XPositionState, YPositionState, enemyBearingState, enemyDistanceState )];
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

        if(enemyDistanceState == 3)
        {
            setFire(1);
        }
        if(enemyDistanceState == 2)
        {
            setFire(2);
        }
        if(enemyDistanceState == 1)
        {
            setFire(3);
        }
    }

    public void onDeath(DeathEvent e) { reward -= 15; }
    public void onHitByBullet(HitByBulletEvent e){ reward -= 10;}
    public void onHitRobot(HitRobotEvent e){ reward -= 4;}
    public void onHitWall(HitWallEvent e){reward -= 5;}
    public void onWin(WinEvent e){reward += 10;}
    public void onRoundEnded(RoundEndedEvent roundEndedEvent) {
        save();
    }
    //public void onBulletHit(BulletHitEvent e){ reward += 3;};

    void normaliseQMatrix()
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
                                QValues[xState][yState][enemyBearingState][enemyDistanceState][action] =  (int)((QValues[xState][yState][enemyBearingState][enemyDistanceState][action] / highestValue)*255);
                            }
                        }
                    }
                }
            }
        }


    }

    void save(){

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
            printStream.flush();
            printStream.close();
        }
    }

    void load(){

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
            catch(InputMismatchException e)
            {
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
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

    static int getRandomInteger(int min, int max)
    {
        Random rand = new Random();

        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    static float getRandomFloat(float min, float max)
    {
        Random rand = new Random();

        float randomNum = rand.nextFloat() * (max - min) + min;

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
