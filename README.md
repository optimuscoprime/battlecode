[Edit README.md](https://github.com/optimuscoprime/battlecode/edit/master/README.md)

Battlecode
==========

[Drop Bears (#219)](https://www.battlecode.org/contestants/teams/219)

## Official Documentation

[Rules](https://github.com/battlecode/battlecode-server/blob/2013-1.1.1/specs.md)

[Javadoc](http://s3.amazonaws.com/battlecode-releases-2013/javadoc/index.html)

[Software/Testing](http://s3.amazonaws.com/battlecode-releases-2013/docs/software.html)

## Setup

* [Download and run the installer](https://www.battlecode.org/contestants/releases/)
* (On OSX) Type: `cd /Applications/Battlecode2013/teams`
* `git clone git@github.com:optimuscoprime/battlecode.git`
* `mv battlecode team219`

## Testing

* (On OSX): Type `cd /Applications/Battlecode2013`
* Run: `ant build` (I am using genuine Java 1.6, `java -version` "1.6.0_29", I also set `JAVA_HOME=""`)
* Run: `ant run`, set Team A and Team B to team219, and tick "Compute and view match synchronously"

## Git

If you change something, type:
* `git add .`
* `git commit -a`
* `git push`
