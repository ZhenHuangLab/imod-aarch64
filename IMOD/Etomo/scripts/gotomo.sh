#! /bin/bash
#
# for compiling
# grep \\\[Computer.*=.*bigfoot\] ImodCalib/cpu.adoc
#
#Sets up IMOD on Linux or Mac and runs uitests.
#
#Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
#Organization: Dept. of MCD Biology, University of Colorado
#record script file
scriptFile=$_
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
fileVers="1.1"
#repository parameters
devParam="D"
compParam="C"
intParam="I"
regParam="R"
branParam="B"
#IMOD_DIR parameters
worksParam="w"
instParam="i"
repParam="r"
dirParam="d"
#
#Substitute an absolute path for the built-in locations
customLocParam="-c"
versParam="-V"
versParamLong="--version"
cleanParam="-c"
makeParam="-m"
targParam="-t"
skipBuildParam="-sb"
#
repNumTag="repository-number"
etomoVersTag="version-string"
dirTag="IMOD-directory-path"
targTag="uitest.make-target"
cpusTag="number-CPUs"
usageString="usage: [source] gotomo.sh [$customLocParam absolute_path_to_test_location_root] [-($devParam|$compParam|$intParam|$regParam|$branParam $etomoVersTag) [$repNumTag]] [-($worksParam|$repParam|$instParam|$dirParam $dirTag)] [$targParam [$targTag]]"
allValue="all"
#repositories
devName="Development"
compName="Completed"
intName="Integration"
regName="Regression"
branName="Branch"
#variables
ret=
repName=
setImod=
imodRep=
etomoVers=
repNum=
target=
repository=
os=
repositoryPath=
etomoRepositoryPath=
# function setOption
# $1 required
# $2 description
# $3 $0
# $4 current arg - may be empty
# success: ret is assigned $4
# failure: ret is null
setOption ()
{
  ret=
  if [ -n "$4" ] && [[ "$4" != -* ]] ; then
    ret=$4
  elif [ $1 ] ; then
    echo Syntax error: $2 is required.  $helpMsg
    if [ $3 == "bash" ] ; then
      return 1
    else
      exit 1
    fi
  fi
}
# function errCheck
# $1 $?
# $2 $0
errCheck ()
{
  if [ ! $1 -eq 0 ] ; then
    if [ $2 == "bash" ] ; then
      return 0
    else
      exit 0
    fi
  fi
}
while test $# -gt 0; do
  case "$1" in
    $usageParam)
      echo $usageString
      echo
      echo $helpParam for more information
      echo
      if [ $0 == "bash" ] ; then
        return 0
      else
        exit 0
      fi
      ;;
    $helpParam)
      echo Sets up IMOD on Linux or Mac and runs uitests.
      echo
      echo $usageString
      echo
      echo Options:
      echo
      echo -$devParam [$repNumTag]: A $devName repository
      echo -$compParam [$repNumTag]: A $compName repository
      echo -$intParam [$repNumTag]: An $intName repository
      echo -$regParam [$repNumTag]: A $regName repository
      echo -$branParam $etomoVersTag [$repNumTag]: A $branName repository.
      echo
      echo -$worksParam: Sets up IMOD works.
      echo -$repParam: Sets up the IMOD in the repository.
      echo -$instParam: Sets up the installed IMOD.
      echo -$dirParam $dirTag: Sets up the IMOD in the $dirTag.
      echo
      echo "$targParam [$targTag]: Runs uitest(s) from the specified repository against"
      echo "   $targTag.  The default $targTag is '$allValue'.  A repository"
      echo "   is required when this option is used.  The repository will be updated and an"
      echo "   etomo jar file compiled.  Different jars will be created for Mac and Linux,"
      echo "   so uitests can be run against the same repository by a Linux machine and a"
      echo "   Mac machine concurrently."
      echo
      echo $versParam, $versParamLong: Output script version.
      echo
      echo
      echo IMOD environment:
      echo This script can be used to set up an IMOD environment - just run the script with 
      echo source.
      echo
      echo
      echo Examples:
      echo
      echo Set up the IMOD from the $compName repository:
      echo source gotomo.sh -C -r
      echo
      echo "Run all uitests with the current IMOD against the Etomo in the ${devName}1"
      echo repository:
      echo ./gotomo.sh -D 1 -t
      echo
      echo "Run all uitests with the 'works' IMOD:"
      echo ./gotomo.sh -D 1 -w -t
      echo
      echo "Run dual uitests with the IMOD and Etomo in ${branName}1-4.8 (see uitest.make):"
      echo ./gotomo.sh -B 4.8 1 -r -t all-dual
      echo
      echo Not yet implemented:
      echo
      echo Incremental compile of $devName:
      echo ./gotomo.sh -D -m 4
      echo
      echo Clean compile of ${devName}2:
      echo ./gotomo.sh -D 2 -c -m 4
      echo
      echo Run etomo using the installed IMOD:
      echo ./gotomo.sh -i -e
      echo
      echo "Run etomo with debug using the IMOD in the $devName repository:"
      echo ./gotomo.sh -D -r -e -- --debug
      echo
      echo Compile and setup IMOD from the $compName repository:
      echo source gotomo.sh -C -r -c -m 4
      echo
      echo Run uitests with the default IMOD, passing parameters to each etomo:
      echo ./gotomo.sh -I -t temp -- --debug --actions
      echo
      echo Compile and run a uitest with an IMOD from a $branName repository, passing
      echo parameters to etomo, and leaving the IMOD environment intact:
      echo source gotomo.sh -B 4.8 1 -r -c -m 4 -t dual -- --debug --actions
      echo
      echo See also:
      echo uitest
      echo man uitest
      echo uitest.adoc
      echo uitest.make
      echo
      if [ $0 == "bash" ] ; then
        return 0
      else
        exit 0
      fi
      ;;
    $customLocParam)
      shift
      customLocation="$1"
      shift
      ;;
    -$devParam)
      repName=$devName
      shift
      setOption "" $repNumTag $0 $1
      if [ $ret ] ; then
        repNum=$ret
        shift
      fi
      ;;
    -$compParam)
      repName=$compName
      shift
      setOption "" $repNumTag $0 $1
      if [ $ret ] ; then
        repNum=$ret
        shift
      fi
      ;;
    -$intParam)
      repName=$intName
      shift
      setOption "" $repNumTag $0 $1
      if [ $ret ] ; then
        repNum=$ret
        shift
      fi
      ;;
    -$regParam)
      repName=$regName
      shift
      setOption "" $repNumTag $0 $1
      if [ $ret ] ; then
        repNum=$ret
        shift
      fi
      ;;
    -$branParam)
      repName=$branName
      shift
      setOption true $etomoVersTag $0 $1
      etomoVers=$ret
      shift
      setOption "" $repNumTag $0 $1
      if [ $ret ] ; then
        repNum=$ret
        shift
      fi
      ;;
    -$worksParam)
      setImod=true
      export IMOD_DIR="/home/build/Head-Build/works"
      shift
      ;;
    -$repParam)
      setImod=true
      imodRep=true
      shift
      ;;
    -$instParam)
      setImod=true
      export IMOD_DIR="/usr/local/IMOD"
      shift
      ;;
    -$dirParam)
      setImod=true
      shift
      setOption true $dirTag $0 $1
      export IMOD_DIR=$ret
      shift
      ;;
    $targParam)
      shift
      setOption "" $targTag $0 $1
      if [ $ret ] ; then
        target=$ret
        shift
      else
        target=$allValue
      fi
      ;;
    $skipBuildParam)
      shift
      skipBuild=true
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
      echo Syntax error at \"$1\".  $helpMsg
      if [ $0 == "bash" ] ; then
        return 1
      else
        exit 1
      fi
      ;;
  esac
done
#set repository
if [ -n "$repName" ] ; then
  if [ "$repName" == $branName ] ; then
    repository=$repName$repNum-$etomoVers
  else
    repository=$repName$repNum
  fi
  echo $repository
  if [ $imodRep ] ; then
    export IMOD_DIR=$HOME/$IMOD_REPOSITORY_DIRECTORY/$repository
  fi
fi
#set os
if [ `uname` == "Darwin" ] ; then
  os=mac
else
  os=linux
fi
#set up IMOD
if [ $setImod ] ; then
  if [ $0 != "bash" ] ; then
    echo Info: IMOD will be set up in a separate shell
  fi
  export PATH=$IMOD_DIR/bin:$PATH
  if [ $imodRep ] ; then
    source "${IMOD_DIR}/dist/IMOD-$os.sh"
  else
    source "${IMOD_DIR}/IMOD-$os.sh"
  fi
  echo IMOD_DIR: $IMOD_DIR
  echo which etomo: `which etomo`
fi
#uitest
if [ $target ] ; then
  echo target: $target
  if [ -z $repository ] ; then
    echo Error: no repository to test.
    if [ $0 == "bash" ] ; then
      return 0
    else
      exit 0
    fi
  fi
  #setup
  repositoryPath=$HOME/$IMOD_REPOSITORY_DIRECTORY/$repository
  etomoRepositoryPath=$repositoryPath/Etomo
  export LD_LIBRARY_PATH="${QTDIR}/lib:$LD_LIBRARY_PATH"
  export IMOD_TMPDIR="${IMOD_DIR}/tmp"
  export PIP_PRINT_ENTRIES=1
  export LD_LIBRARY_PATH="${IMOD_QTLIBDIR}:$LD_LIBRARY_PATH"
  export IMOD_SELF_TEST_DIR=$HOME/SelfTestDir
  export IMOD_UNIT_TEST_DATA=$HOME/$IMOD_REPOSITORY_DIRECTORY/ImodTests/EtomoTests/vectors
  export IMOD_JAVADIR=/usr/bin
  export IMOD_UITEST_SCRIPT=$etomoRepositoryPath/scripts
  export IMOD_UITEST_SOURCE=$etomoRepositoryPath/tests
  export IMOD_UITEST_DATA=$etomoRepositoryPath/uitestData
  export IMOD_UITEST_IMAGE_DATA=$HOME/$IMOD_REPOSITORY_DIRECTORY/TomoData
  export IMOD_JUNIT_HOME=/usr/local/junit
  export IMOD_JFCUNIT_HOME=$HOME/lib/junit.extensions.jfcunit
  export IMOD_JAKARTA_REGEXP_HOME=$HOME/lib/junit.extensions.jfcunit
  export IMOD_JAKARTA_REGEXP_JAR=jakarta-regexp-1.2.jar
  export IMOD_CLASS_SEPARATOR=:
  export IMOD_PATH_SEPARATOR=/
  export IMOD_JUNIT_JAR=junit.jar
  export IMOD_JFCUNIT_JAR=jfcunit.jar
  export IMOD_ETOMO_HOME=$etomoRepositoryPath/jar_dir
  export IMOD_ETOMO_JAR=etomoUITest$os.jar
  #trim the LD library
  export LD_LIBRARY_PATH=`echo $LD_LIBRARY_PATH | awk -F: '{for(i=1;i<=NF;i++){if(!($i in a)){a[$i];printf  s$i;s=":"}}}'`
  if [ ! $skipBuild ] ; then
    sync
    #make jar
    cd $repositoryPath
    hg update
    errCheck $? $0
    cd $etomoRepositoryPath
    errCheck $? $0
    rm -f jar_dir/$IMOD_ETOMO_JAR
    errCheck $? $0
    make uitestjarfile
    errCheck $? $0
    # Use a different jar for different OS's because linux and mac run from the
    # same repository at the same time
    cd jar_dir
    errCheck $? $0
    mv -f etomoUITest.jar $IMOD_ETOMO_JAR
    errCheck $? $0
  fi
  # go to test area
  if [ "$customLocation" ] ; then
    cdCommand="source $HOME/bin/cdTestLocation.sh $customParam $customLocation"
  else
    cdCommand="source $HOME/bin/cdTestLocation.sh"
  fi
  $cdCommand -m $repository
  errCheck $? $0
  #test
  # create modifiable local makefiles.
  \cp -vfu $IMOD_UITEST_SOURCE/uitest.make uitest.make
  errCheck $? $0
  make -f uitest.make UITEST_TYPE=diff $target
  #send mail at the end of the test, ignoring killed tests
  ret=$?
  echo ret is $ret
  if [ ! $ret -eq 130 ] ; then
    if [ ! $ret -eq 0 ] ; then
      tail -vn 200 `ls -rt etomo_*_*.log|tail -n 2`>nada
      mail -s "uitest $target on $repository failed on `hostname` error: $ret" $USER@colorado.edu < nada
      if [ $0 == "bash" ] ; then
        return $ret
      else
        exit $ret
      fi
    fi
    tail -vn 100 `ls -rt etomo_*_out.log|tail -n 1`>nada
    #ls -lrt etomo_*_out.log>nada
    mail -s "uitest $target on $repository succeeded on `hostname`" $USER@colorado.edu < nada
  fi
fi
