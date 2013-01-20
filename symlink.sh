#!/bin/bash

# Hacky script ian made to populate target_dir with symlinks to this teams
# directory.
#If you want to make it work for you make your own hostname section.
# DIR_TO_HERE should be what you give cd to get to this teams dir from there.


TEAMS_DIR="./*"
if [ `hostname` = "10-10-10-15.tpgi.com.au" ]; then
TARGET_DIR="../battlecode2013player/teams/"
THAT_DIR_TO_HERE="../../battlecode/"
fi


for D in $TEAMS_DIR; do
if [ -d "${D}" ]; then
   #echo "found ${D} for ${TARGET_DIR}"   # Teams
   if [ -e ${TARGET_DIR}${D} ]; then
      echo "existed: ${TARGET_DIR}${D}"
   else
      #echo "We should make ${TARGET_DIR}${D} point to ${D}"
      cd ${TARGET_DIR}
      echo -n "Linking from ${TARGET_DIR}"
      ln -v  -s ${THAT_DIR_TO_HERE}${D} ${D}
      cd -
   fi
fi
done

