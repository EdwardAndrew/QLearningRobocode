/**
 * @author Edward Andrew
 */
package com.github.EdwardAndrew;
import com.github.EdwardAndrew.QLearning.action.Action;
import robocode.*;

import java.awt.geom.Point2D;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class QLearningRobot extends AdvancedRobot {

    // The total amount of each state.
    private final int battleFieldXStateCount = 10;
    private final int battleFieldYStateCount = 10;
    private final int enemyBearingStateCount = 8;
    private final int enemyDistanceStateCount = 4;
    // Total number of action choices.
    private final int actionCount = 9;

    // The state the agent is in.
    private int enemyDistanceState = 0;
    private int enemyBearingState = 0;
    private int xState = 0;
    private int yState = 0;

    // Immediate reward for actions taken.
    private float reward = 0.0f;

    // State -> Action -> Reward values. This is used for the learning process.
    private float[][][][][] QValues = new float[battleFieldXStateCount][battleFieldYStateCount][enemyBearingStateCount][enemyDistanceStateCount][actionCount];

    // value of 1 means random action will be taken 100% of the time
    float epsilon = 1.0f;
    // How much to decrement the epsilon by after each battle.
    float epsilonDecrement = 0.00001f;
    // The epsilon won't go lower than this.
    float epsilonLowerLimit = 0.01f;

    /**
     * Called by the robocode system.
     * Performs an action for each turn.
     *
     * This is where the QLearning algorithm actually runs.
     */
    @Override public void run(){
        // We don't want the radar or gun to inherit rotation.
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

        // Discount factor.
        float gamma = 0.9f;
        // Learning parameter.
        float alpha = 0.5f;

        // load in Q values
        load();

        try {
            while (true) {
                // Reset reward value.
                reward = 0;

                // Find the current states.
                int currentEnemyXState = getQuantisedBattlefieldPosition(this.getX(), this.getBattleFieldWidth(), battleFieldXStateCount);
                int currentEnemyYState = getQuantisedBattlefieldPosition(this.getY(), this.getBattleFieldHeight(), battleFieldYStateCount);
                int lastEnemyBearingState = enemyBearingState;
                int lastEnemyDistanceState = enemyDistanceState;
                xState = currentEnemyXState;
                yState = currentEnemyYState;

                int action;

                // If returned value is below epsilon, perform a random action. Otherwise perform learned action.
                if (getRandomFloat(0, 1) < epsilon) {
                    action = getRandomInteger(0, actionCount - 1);
                } else {
                    // Get the action with the highest QValue for this state.
                    action = getMaximumActionForState(currentEnemyXState, currentEnemyYState, lastEnemyBearingState, lastEnemyDistanceState);
                }

                selectAction(action);
                execute();
                // After performing action, drive forwards and turn the radar 360 degrees.
                setTurnRadarLeft(360);
                ahead(100);

                // Calculate the new current states. We can only detect a change in this function.
                int outcomeXPositionState = getQuantisedBattlefieldPosition(this.getX(), this.getBattleFieldWidth(), battleFieldXStateCount);
                int outcomeYPositionState = getQuantisedBattlefieldPosition(this.getY(), this.getBattleFieldHeight(), battleFieldYStateCount);

                // Update the QValue.
                // Q(St,At) = (1-alpha) * Q(St,At) + alpha * Rt + gamma * Max(Q(St+1,a))
                QValues[currentEnemyXState][currentEnemyYState][lastEnemyBearingState][lastEnemyDistanceState][action] =
                        (1 - alpha) * ((QValues[currentEnemyXState][currentEnemyYState][lastEnemyBearingState][lastEnemyDistanceState][action]) +
                                alpha * (reward + (gamma * getMaximumQValueForState(outcomeXPositionState, outcomeYPositionState, enemyBearingState, enemyDistanceState))));

                xState = outcomeXPositionState;
                yState = outcomeYPositionState;
            }
        }
        catch(ThreadDeath e)
        {
            normaliseQValues();
            save();
        }
    }

    /**
     * Instructs robocode to perform the action associated
     * with the passed in index value.
     * execute() needs to be called afterwards. To actually
     * perform the action.
     * @param action The index of the action to perform.
     */
    private void selectAction(int action){
        // Don't do anything if the robot decided to stay.
        if(Action.values()[action] == Action.STAY)
        {
            doNothing();
        }
        else
        {
            selectTurnTowardsBearing(Action.values()[action].getBearing());
        }
    }

    /**
     * Instructs the robot to turn towards the given heading.
     * execute() needs to be called afterwards to perform the
     * maneuver.
     * @param absoluteBearing The bearing we want the robot
     *                        to face towards are the turn
     *                        is completed.
     */
    private void selectTurnTowardsBearing(double absoluteBearing){
        double normalisedBearing = normaliseBearing( this.getHeading() );
        double headingDelta = normaliseBearing(absoluteBearing - normalisedBearing);

        if(headingDelta > 0)
        {
            setTurnRight( headingDelta );
        }
        else
        {
            setTurnLeft( Math.abs( headingDelta ) );
        }
    }

    /**
     * Instructs the robot to aim towards the given heading.
     * execute() needs to be called afterwards to perform the
     * maneuver.
     * @param absoluteBearing The bearing we want the robot's
     *                          gun to face towards are the turn
     *                          is completed.
     */
    private void selectTurnGunTowardsBearing(double absoluteBearing){
        double normalisedBearing = normaliseBearing( this.getGunHeading() );
        double headingDelta = normaliseBearing(absoluteBearing - normalisedBearing);

        if(headingDelta > 0)
        {
            turnGunRight( headingDelta );
        }
        else
        {
            turnGunLeft( Math.abs( headingDelta ) );
        }
    }

    /**
     * @param xPositionState The quantised X position of the robot.
     * @param yPositionState The quantised Y position of the robot.
     * @param enemyBearingState The quantised absolute bearing of the
     *                          enemy robot.
     * @param enemyDistanceState The quantised distance of the enemy
     *                           robot.
     * @return The Action that yields the highest reward for the
     *          given state.
     */
    private int getMaximumActionForState(int xPositionState, int yPositionState, int enemyBearingState, int enemyDistanceState){
        int highestQValueIndex =0;

        for(int action = 0; action < actionCount; action++)
        {
            if(QValues[xPositionState][yPositionState][enemyBearingState][enemyDistanceState][action] > highestQValueIndex)
            {
                highestQValueIndex = action;
            }
        }

        return  highestQValueIndex;
    }

    /**
     * @param xPositionState The quantised X position of the robot.
     * @param yPositionState The quantised Y position of the robot.
     * @param enemyBearingState The quantised absolute bearing of the
     *                          enemy robot.
     * @param enemyDistanceState The quantised distance of the enemy
     *                           robot.
     * @return The value of the highest reward that can be obtained
     *         from this state.
     */
    private float getMaximumQValueForState(int xPositionState, int yPositionState, int enemyBearingState, int enemyDistanceState){
       return QValues[xPositionState][yPositionState][enemyBearingState][enemyDistanceState][getMaximumActionForState(xPositionState, yPositionState, enemyBearingState, enemyDistanceState )];
    }

    /**
     * Called by robocode when the radar detects an enemy robot.
     * @param enemy - ScannedRobotEvent containing information
     *                about the robot that has been detected.
     */
    @Override public void onScannedRobot(ScannedRobotEvent enemy) {
        // Absolute bearing that the enemy was spotted at.
        double enemyBearing = getAbsoluteBearing( enemy.getBearing());

        // Update the current states for the robot.
        enemyDistanceState =  getQuantisedEnemyDistance( enemy.getDistance() );
        enemyBearingState =  getQuantisedBearing( enemyBearing , enemyBearingStateCount ) ;

        // Don't take our own velocity into account (bullets do not seem to inherit velocity).
        double myXVelocity = 0;
        double myYVelocity = 0;

        // The velocity of the enemy robot's movement.
        double enemyXVelocity = Math.sin(Math.toRadians(enemy.getHeading())) * enemy.getVelocity();
        double enemyYVelocity = Math.cos(Math.toRadians(enemy.getHeading())) * enemy.getVelocity();

        // Relative velocity of both our robots movement.
        double relativeXVelocity = enemyXVelocity - myXVelocity;
        double relativeYVelocity = enemyYVelocity  -myYVelocity;

        // The enemies position coordinates, relative to us.
        double currentRelativeEnemyX = Math.sin(Math.toRadians(enemyBearing)) * enemy.getDistance();
        double currentRelativeEnemyY = Math.cos(Math.toRadians(enemyBearing)) * enemy.getDistance();

        // Select appropriate firepower for distance.
        double firePower = Math.min(500/enemy.getDistance(),3);

        // Calculate bullet speed.
        double bulletSpeed = 20 - firePower * 3;

        // Calculate bullet travel time.
        double timeForBulletToHit = enemy.getDistance() / (bulletSpeed + Point2D.distance(0,0,relativeXVelocity,relativeYVelocity));

        // Predict enemy location at this time.
        double predicatedRelativeEnemyX = currentRelativeEnemyX + (relativeXVelocity * timeForBulletToHit);
        double predicatedRelativeEnemyY = currentRelativeEnemyY + (relativeYVelocity * timeForBulletToHit);

        // Convert the relative coordinates of the enemy to a bearing to aim at.
        double predictedBearing = Math.atan2(predicatedRelativeEnemyX, predicatedRelativeEnemyY) * 180 / 3.141592653589;

        // Turn gun towards predicted bearing.
        selectTurnGunTowardsBearing(predictedBearing);
        execute();

        // If the gun is cool and we're aiming at the direction and not too far away. Then fire.
        if(getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 0.5 && enemyDistanceState < 3)
        {
            fire(firePower);
        }
        else
        {
            // Alternatively, perform a random action.
            selectAction(getRandomInteger(0, actionCount-2));
            execute();
            ahead(100);
        }

    }

    /**
     * Called by robocode when the robot crashes into a wall.
     * @param hitWallEvent - Contains information about the robot
     *                       when it hit the wall.
     */
    @Override public void onHitWall(HitWallEvent hitWallEvent){

        // Negative reward for crashing into the wall.
        reward -= 7;

        // Handle each case and command the robot to drive away from the wall.
        if(yState >= battleFieldYStateCount-3 && xState > 2 &&  xState < battleFieldXStateCount-1){
            selectTurnTowardsBearing(180);
        }
        else if(yState >= battleFieldYStateCount-3 &&  xState >= battleFieldXStateCount-3){
            selectTurnTowardsBearing(-135);
        }
        else if(yState >= battleFieldYStateCount-3 &&  xState <= 2) {
            selectTurnTowardsBearing(135);
        }
        else if (xState <= 2 && yState > 2 && yState < battleFieldYStateCount-3){
            selectTurnTowardsBearing(90);
        }
        else if(xState <= 2 && yState <= 2){
            selectTurnTowardsBearing(45);
        }
        else if(yState <= 2 && xState > 2 && xState < battleFieldXStateCount-3){
            selectTurnTowardsBearing(0);
        }
        else if(yState <= 2 && xState >= battleFieldXStateCount-3){
            selectTurnTowardsBearing(-45);
        }
        else if(xState >= battleFieldXStateCount-3 && yState > 2 && yState < battleFieldYStateCount-3){
            selectTurnTowardsBearing(-90);
        }
        execute();
        ahead(100);
    }

    /**
     * Called by robocode when the robot dies.
     */
    @Override public void onDeath(DeathEvent deathEvent) { reward -= 15; }

    /**
     * Called by robocode when the robot gets hit by a bullet.
     * @param hitByBulletEvent Contains information about the robot when it
     *                         was hit by the bullet.
     */
    @Override public void  onHitByBullet(HitByBulletEvent hitByBulletEvent){ reward -= 7;}

    /**
     * Called by robocode when the robot crashes into another.
     * @param hitRobotEvent Contains information about the robot
     *                      collision.
     */
    @Override public void onHitRobot(HitRobotEvent hitRobotEvent){ reward -= 7;}

    /**
     * Called by robocode when the robot's bullet hits an enemy.
     * @param bulletHitEvent Contains information about the bullet
     *                       that hit the enemy.
     */
    @Override public  void onBulletHit(BulletHitEvent bulletHitEvent) { reward += 5; }

    /***
     * Called by Robocode when the robot win's the round.
     * @param winEvent Contains information about the what
     *                 the round being won.
     */
    @Override public void onWin(WinEvent winEvent){reward += 15;}

    /**
     * Called by robocode when the round ends.
     * @param roundEndedEvent Contains information about the round.
     */
    @Override public void onRoundEnded(RoundEndedEvent roundEndedEvent) {
        normaliseQValues();

        // Decrement the epsilon value.
        epsilon = epsilon > epsilonLowerLimit ? epsilon - epsilonDecrement : epsilon;

        save();
    }

    /**
     * Saves the QValue matrix to a file in the data directory.
     */
    private void save(){

        PrintStream printStream = null;

        try{
            printStream = new PrintStream(new RobocodeFileOutputStream(getDataFile("QValues.data")));
            // Load the epsilon value first.
            printStream.println(epsilon);
            for(int x = 0; x < battleFieldXStateCount; x++)
            {
                for(int y = 0; y < battleFieldYStateCount; y++)
                {
                    for(int enemyBearing = 0; enemyBearing < enemyBearingStateCount; enemyBearing++)
                    {
                        for(int enemyDistance = 0; enemyDistance < enemyDistanceStateCount; enemyDistance++)
                        {
                            for(int action = 0; action < actionCount; action++)
                            {
                                // Store the values rounded to 1 dp.
                                String token = Double.toString(round(QValues[x][y][enemyBearing][enemyDistance][action], 2));
                                printStream.println(token);
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

    /**
     * Loads the QMatrix save file from the data directory.
     */
    private void load(){

        File saveFile = new File(getDataFile("QValues.data").toString());
        int line =0;

        if(saveFile.exists() && !saveFile.isDirectory()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(getDataFile("QValues.data")));
                // Load the epsilon value first.
                epsilon = Float.parseFloat(bufferedReader.readLine());
                for (int x = 0; x < battleFieldXStateCount; x++) {
                    for (int y = 0; y < battleFieldYStateCount; y++) {
                        for (int enemyBearing = 0; enemyBearing < enemyBearingStateCount; enemyBearing++) {
                            for (int enemyDistance = 0; enemyDistance < enemyDistanceStateCount; enemyDistance++) {
                                for (int action = 0; action < actionCount; action++) {
                                    line++;
                                    QValues[x][y][enemyBearing][enemyDistance][action] = (float)Double.parseDouble(bufferedReader.readLine());
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
                System.out.println("IOException at line" + line);
            }
            catch(NullPointerException e)
            {
                e.printStackTrace();
                System.out.println("Null pointer at line: "+ line);
            }
        }
    }

    /**
     * Normalises the QValues.
     */
    private void normaliseQValues(){
        float highestValue = 0;

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
                            float absValue = (float)Math.abs(QValues[xState][yState][enemyBearingState][enemyDistanceState][action]);
                            if(absValue > highestValue)
                            {
                                highestValue = absValue;
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
                            QValues[xState][yState][enemyBearingState][enemyDistanceState][action] =  (QValues[xState][yState][enemyBearingState][enemyDistanceState][action] / highestValue)*99.99f;
                        }
                    }
                }
            }
        }



    }

    /**
     * Takes an angle relative to the heading of the robot and
     * returns an absolute value.
     * @param degrees Angle relative to the robot.
     * @return Absolute value, 180 is south.
     */
    private double getAbsoluteBearing(double degrees){
        double absoluteBearing = this.getHeading() + degrees;

        if( absoluteBearing > 360 ) absoluteBearing -= 360;
        if( absoluteBearing < 0 ) absoluteBearing += 360;

        return absoluteBearing;
    }

    /**
     * Normalises a bearing
     * This function is provided by third party.
     * @see <a href="http://mark.random-article.com/weber/java/robocode/lesson4.html">Function Source</a>
     * @param degrees Bearing to be normalised
     * @return Angle normalised between -180 and 180 degrees.
     */
    private static double normaliseBearing(double degrees){
        while (degrees >=  180) degrees -= 360;
        while (degrees < -180) degrees += 360;
        return degrees;
    }

    /**
     * Returns a random integer between the bounds (inclusive).
     * @param min Lowerbound, the smallest value that can be returned.
     * @param max Upperbound, the largest value that can be returned.
     * @return Random bounded integer.
     */
    private static int getRandomInteger(int min, int max){
        return new Random().nextInt((max - min) + 1) + min;
    }

    /**
     * Returns a random float between the bounds (inclusive).
     * @param min Lowerbound, the smallest value that can be returned.
     * @param max Upperbound, the largest value that can be returned.
     * @return Random bounded float.
     */
    private static float getRandomFloat(float min, float max){
        return new Random().nextFloat() * (max - min) + min;
    }

    /**
     * Rounds a double to the specified number of places.
     * @param value Value to be rounded.
     * @param places Number of places to round to.
     * @return Rounded value.
     */
    private static double round(double value, int places){
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Quantises the enemy enemyDistance.
     * @param enemyDistance
     * @return quantised integer.
     */
    private static int getQuantisedEnemyDistance(double enemyDistance){
        if( enemyDistance < 80 ) return 0;
        if( enemyDistance < 300 ) return 1;
        if( enemyDistance < 600 ) return 2;
        return 3;
    }

    /**
     * Quantise a bearing.
     * @param bearing Bearing to be quantised.
     * @param totalQuantisedValues How many quantised values there are.
     * @return Quantised value between 0 and <code>totalQuantisedValues - 1</code>.
     */
    private static int getQuantisedBearing(double bearing, int totalQuantisedValues){
        double stateSize = 360 / totalQuantisedValues;
        bearing += stateSize / 2;

        while (bearing >= 360) bearing -= 360;
        while (bearing < 0) bearing += 360;

        return (int)Math.floor( bearing / stateSize );
    }

    /**
     * Takes a one dimensional position and returns a quantised value.
     * @param position Robot's X or Y position on the battlefield.
     * @param battleFieldSize Battlefield X or Y size.
     * @param totalQuantisedValues The amount of quantised values.
     * @return <code>int</code> between 0 and <code>totalQuantisedValues - 1</code>
     */
    private static int getQuantisedBattlefieldPosition(double position, double battleFieldSize, int totalQuantisedValues ){
        double gridXSize = battleFieldSize / totalQuantisedValues;
        return (int)Math.floor( position / gridXSize );
    }


}
