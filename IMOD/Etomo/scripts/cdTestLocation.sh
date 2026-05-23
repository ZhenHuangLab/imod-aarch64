#! /bin/bash
#
#Goes to the specified test directory (default is linux)
#
#Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
#Organization: Dept. of MCD Biology, University of Colorado
#
usageParam="-h"
helpParam="--help"
helpMsg="Use $usageParam or $helpParam for more information."
if [ -z "$1" ] ; then
  echo $helpMsg
  if [ $0 == "bash" ] ; then
    return 0
  else
    exit 0
  fi
fi
#constants
fileVers="1.0"
#
#Substitute an absolute path for the built-in locations
customLocParam="-c"
makeParam="-m"
versParam="-V"
locParam="-l"
versParamLong="--version"
#
usageString="usage: source cdTestLocation.sh [-m] [$customLocParam absolute_path_to_test_location_root] directory_for_testing_a_repository"
sharedLocation="/home/NOBACKUP/$USER"
localLocation="NOBACKUP"
testRoot="test datasets"
#variables
repository=
error=
make=0
#
while test $# -gt 0; do
  case "$1" in
    $usageParam)
      echo $usageString
      echo
      if [ $0 == "bash" ] ; then
        return 0
      else
        exit 0
      fi
      ;;
    $helpParam)
      echo Goes to and optionally creates repository test directory
      echo
      echo $usageString
      echo
      echo Options:
      echo
      echo $makeParam: Make test directories.
      echo
      echo Examples:
      echo
      echo cdTestLocation.sh Development1
      echo
      echo On a linux computer with a shared testing area:
      echo   /home/NOBACKUP/$USER/test datasets/Linux/Development1
      echo
      echo On a Mac with a shared testing area:
      echo   /home/NOBACKUP/$USER/test datasets/Darwin/Development1
      echo
      echo On a computer without a shared test area:
      echo   $HOME/NOBACKUP/test datasets/Linux/Development1
      echo
      if [ $0 == "bash" ] ; then
        return 0
      else
        exit 0
      fi
      ;;
    $makeParam)
      make=1
      shift
      ;;
    $customLocParam)
      shift
      customLocation="$1"
      shift
      ;;
    $locParam)
      shift
      sharedLocation="$1$USER"
      shift
      ;;
    $versParam)
      echo $fileVers
      shift
      ;;
    $versParamLong)
      echo $fileVers
      shift
      ;;
    *)
      repository=$1
      shift
      ;;
  esac
done
#go to locatation of repositories
if [ $customLocation ] ; then
  cd $customLocation
elif [ -d $sharedLocation ] ; then
  cd "$sharedLocation"
else
  cd "$HOME"
  if [ -d $localLocation ] ; then
    cd "$localLocation"
  elif [ $make -eq 1 ] ; then
    if [ ! -e $localLocation ] ; then
      mkdir "$localLocation"
      cd "$localLocation"
    fi
  fi
fi
#go to test root
if [ -d "$testRoot" ] ; then
  cd "$testRoot"
elif [ $make -eq 1 ] ; then
  if [ ! -e "$testRoot" ] ; then
    mkdir "$testRoot"
    cd "$testRoot"
  fi
fi
#add the OS name
os=`uname`
if [ -d $os ] ; then
  cd "$os"
elif [ $make -eq 1 ] ; then
  if [ ! -e $os ] ; then
    mkdir "$os"
    cd "$os"
  fi
fi
#go to directory for repository
if [ -d $repository ] ; then
  cd "$repository"
elif [ $make -eq 1 ] ; then
  if [ ! -e $repository ] ; then
    mkdir "$repository"
    cd "$repository"
  fi
fi
pwd

