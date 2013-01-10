[Edit README.md](https://github.com/optimuscoprime/battlecode/edit/master/README.md)

Battlecode
==========

[Drop Bears (#219)](https://www.battlecode.org/contestants/teams/219)

## Official Documentation

* [Rules](https://github.com/battlecode/battlecode-server/blob/2013-1.1.1/specs.md)
* [Javadoc](http://s3.amazonaws.com/battlecode-releases-2013/javadoc/index.html)
* [Software/Testing](http://s3.amazonaws.com/battlecode-releases-2013/docs/software.html)

## Random collection of links

* https://www.battlecode.org/contestants/calendar/
* http://cory.li/battlecode-intro/
* http://video.mit.edu/watch/6370-battlecode-jan-9-8877/
* http://stevearc.blogspot.com.au/2011/12/battlecode-postmortem-2011.html?m=1

## Setup

* [Download and run the installer](https://www.battlecode.org/contestants/releases/)
* (On OSX) Type: `cd /Applications/Battlecode2013/`
* The above directory contains a folder called 'teams'. The repo you are about to clone
  contains the contents of a teams looking directory, so...
* `git clone git@github.com:optimuscoprime/battlecode.git`
  ... the repo over the teams folder.
* then `mv battlecode teams` (or just clone somewhere else and hand pick the teams you 
  want to copy into the 'teams/' directory

## Folder layout

* I've put different robot players into different folders (so they show up as different
  teams when placed in the teams directory of the application installation). In each folder
  is a 'RobotPlayer.java' and a 'desc.txt', which provides notes on the properties of
  that RobotPlayer.

## Testing

* (On OSX): Type `cd /Applications/Battlecode2013`
* Run: `ant build` (I am using genuine Java 1.6, `java -version` "1.6.0_29", I also set `JAVA_HOME=""`)
* Run: `ant run`, set Team A and Team B to teams you have copied into the teams directory, 
  and tick "Compute and view match synchronously" (remember to select a map)

## Git

If you change something, type:
* `git add .`
* `git commit -a`
* `git push`

