# Robocode QLearning Robot
This `AdvancedRobot` was created using IntelliJ, [Robocode](http://robocode.sourceforge.net) Version 1.9.3.1 (17-03-2018) and JDK 1.8.

QLearning controls the movement of the robot. The aiming, firing and scanning is programmed.

The robot can be left to train itself as the epsilon value is reduced by `0.00001f` after every round.
Start a new data set and the epsilon value will begin at `1.0f` and will have to perform around
90000 battles before it reaches the lower limit of `0.01f`. However this can be manually edited
as it is the first value in the `QValues.data` file.

## Using Compiled Code
- Copy the `com` folder inside `./out/production/CHA-2555-Artificial-Intelligence/`
- Navigate to your `robocode` installation directory. 
- Paste the folder into `(robocode install directory)/robots`
- Load `robocode`, click `Battle`->`New`. 
- At this point you should see the `com.github.EdwardAndrew` package listed in the left pane.

## Compiling from Source
- Open the project in IntelliJ, or your preferred IDE.
- Ensure `JDK 1.8` and `robocode` libraries are included.
- Build the project.
- Copy the `com` folder inside your output directory.
- Navigate to your `robocode` installation directory. 
- Paste the folder into `(robocode install directory)/robots`
- Load `robocode`, click `Battle`->`New`. 
- At this point you should see the `com.github.EdwardAndrew` package listed in the left pane.

## Using Trained Data
If you do not use the existing trained data sets, the robot will attempt to begin training again.
This may take a long time as the epsilon value will start at `1.0f` and gradually decrease at a rate of `0.00001f`
per battle the robot fights. Using the provided trained data is a good way to avoid waiting for the robot to train itself!

- After copy the robot's file to robocode using either of the methods above, navigate to the
`(robocode install directory)/robots/com/github/EdwardAndrew/QLearningRobot.data` folder
- In this directory are a bunch of folders containing training data for some of the sample robots.
If you've already attempted to run the `QLearningRobot` you will see a small `QValues.data` file.
This file contains the training information.
- Pick which trained data set you want to use from the relevant directory and copy / paste it to the
`QLearningRobot.data` folder.
- The robot will now use the pretrained data for it's decision making!

## Notes: Epsilon Value in Trained Data
The epsilon value is the first entry in the `QValues.data` file. This can be safely opened
with notepad and edited. As the epsilon value controls the chance of how likely the robot
is to perform a completly random random action instead of a learnt action, changing this
value will change the behaviour of the robot.

- Set the value to `1.0f` and the robot will perform entirely random actions.
- Set the value to `0.0f` and the robot will have a 1 in 100 chance of performing
a random action, as the epsilon is defined to never be lower than `0.01f` in the code.
