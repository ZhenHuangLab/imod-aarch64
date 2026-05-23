#!/bin/sh
#
# Stub for self-extracting IMOD installer
# Use -help to see the options
#

installer=installIMOD
this="$0"
rootname=${this##*/}
packname=${rootname%.*}.tar.gz
tempdir=IMODtempDir
installdir=""
namedir=""
nameopt=""
custom=0
skipopt=""
scriptopt=""
sysdir=""
yesopt=""
debopt=""
ubuntu=0
ubscripts1=/etc/csh.cshrc
ubscripts2=/etc/bash.bashrc

# Find the end of this script
#
stubline=`awk '/END OF STUB/ {if (gotit > 0) {print NR + 1 ; exit} else { gotit = 1}}' "$this"`
if [ -z $stubline ]; then
    echo "Could not find package in this file!"
    exit 1
fi

extract=0
test=0
while [ $# -gt 0 ]
do
    key="$1"

    case $key in
        -ex*)
            extract=1
            ;;

        -test)
            test=1
            ;;

        -di*)
            installdir=$2
            shift
            ;;

        -na*)
            namedir=$2
            nameopt="-name $namedir"
            spacetst=`echo $namedir | grep '[ /\\]'`
            if [ -n "$spacetst" ]; then
                echo "The entry for -name must be a single directory name"
                echo "with no spaces or directory separators"
                exit 1
            fi
            shift
            ;;

        -sc*)
            sysdir=$2
            shift
            ;;
        
        -sk*)
            skipopt="-skip"
            ;;

        -y*)
            yesopt="-yes"
            ;;
         
        -de*)
            debopt="-debian"
            ubuntu=1
            ;;

        -h*|--help)
            cat <<EOF
This is a self-installing IMOD package which will install IMOD in a default
or specified location and set up startup files.
Options can be added after the name of the package on the command line.  
Options may be abbreviated.  The options are:
  -dir dir     Install IMOD within the given directory.  The directory must
                 exist (unless -name is entered also) and you must have
                 permission to write to it.  
  -name dir    Rename the IMOD directory to the given name.  The script will
                 automatically remove an existing version by this name and not
                 offer to clean up of other versions
  -extract     Just extract the package and the real IMOD install script into
                 a directory named $tempdir, without installing
  -script dir  Place the IMOD startup scripts in the given directory.  On Linux
                 and Windows, scripts will be copied there instead of to 
                 /etc/profile.d.  On Mac, scripts will be copied there
                 and the system startup files will not be modified.
  -skip        Skip copying startup scripts (Linux and Windows) or modifying
                  system startup files (Mac)
  -debian      Modify $ubscripts1 and $ubscripts2 instead of 
                  copying startup scripts to /etc/profile.d
                   - this option is set automatically for Ubuntu versions < 10
                     and is not needed for new Debian which use /etc/profile.d
  -yes         Install this version and remove old versions without asking for
                  confirmation
  -test        Run this self-installing script but not the real install script
  -h, --help   Print this help
EOF
            exit 0
            ;;

        *)
            echo "Illegal option $1"
            exit 1
            ;;
    esac
    shift
done    

sysfiles1=
sysfiles2=    
sysfiles3=
file3text=

system=`uname -s`
defaultdir=/usr/local

case $system in
    *Linux*)
         os=linux
         scripts1=IMOD-linux.csh
         scripts2=IMOD-linux.sh
         if [ $ubuntu -eq 0 ]; then
             relfiles=`\find /etc -maxdepth 1 -type f -name '*release' -print`
             if [ -n "$relfiles" ]; then
                 ubtest=`grep -i ubuntu $relfiles`
                 if [ -n "$ubtest" ]; then 
                     if [ -e /etc/lsb-release ]; then
                         major=`sed -n '/RELEASE/s/.*=\([0-9]*\).*/\1/p' /etc/lsb-release`
                         if [ $major -lt 10 ]; then ubuntu=1 ; fi
                     else
                         ubuntu=1
                     fi
                 fi
             fi
         fi
         if [ $ubuntu -eq 1 ]; then
             sysfiles1=$ubscripts1
             sysfiles2=$ubscripts2
         fi
         ;;

    *CYGWIN*)
        os=windows
        scripts1=IMOD-cygwin.csh
        scripts2=IMOD-cygwin.sh
        ;;

    *Darwin*)
        os=osx
        defaultdir=/Applications
        scripts1=mac.cshrc
        scripts2=mac.profile
        sysfiles1=/etc/csh.login
        sysfiles2=/etc/profile
        if [ -e /etc/zprofile ] ; then 
            sysfiles3=/etc/zprofile 
            file3text=", /etc/zprofile,"
        fi
        if [ `uname -m` = 'arm64' ] ; then
            pkgutil --pkgs | grep Rosetta > /dev/null 2>&1
            if [ $? -eq 1 ] ; then
                echo "You must install Rosetta2 to be able to run IMOD on an M1 Mac"
                exit 1
            fi
        fi
        ;;

    *)
        if [ $extract -eq 0 ]; then
            echo "IMOD will not run on this system ($system)"
            echo "You can add the option -extract to just extract the tar file"
            exit 1
        fi
        ;;    

esac

# For install and script dir, need to make an absolute path before
# going into temp dir and running install script
#
if [ -n "$installdir" ]; then
    if [ $os = windows ]; then installdir=`cygpath "$installdir"` ; fi
    absolute=`echo $installdir | grep '^/'`
    if [ $? -ne 0 ]; then installdir="`pwd`""/$installdir" ; fi
fi

copyto=/etc/profile.d
if [ -n "$sysdir" ]; then
    if [ $os = windows ]; then sysdir=`cygpath "$sysdir"` ; fi
    absolute=`echo $sysdir | grep '^/'`
    if [ $? -ne 0 ]; then sysdir="`pwd`""/$sysdir" ; fi
    scriptopt="-script $sysdir"
    copyto="$sysdir"
fi


if [ -z "$installdir" ]; then installdir=$defaultdir ; fi
if [ "$installdir" != "$defaultdir" ]; then custom=1 ; fi

if [ $extract -eq 0 ]; then
    echo ""
    if [ -z "$namedir" ]; then
        echo "This script will install IMOD in $installdir and rename"
        echo "any previous version, or remove another copy of this version."
    else
        echo "This script will install IMOD in $installdir and name it $namedir."
        echo "It will first remove $namedir if it exists, and remove anything that"
        echo "matches the imod_n.n.n name that this IMOD will unpack as"
    fi
    echo ""
    if [ -z "$skipopt" ]; then
        if [ -z "$sysfiles1" ]; then
            echo "It will copy $scripts1 and $scripts2 to $copyto"
        else
            echo "It will edit ${sysfiles1}${file3text} and $sysfiles2 to source a"
            echo " startup script in $installdir/IMOD unless they already appear to do so"
        fi
    fi
    
    if [ -z "$yesopt" ]; then
        echo ""
        echo "You can add the option -h to see a full list of options"
        echo ""
        /bin/echo -n "Enter Y if you want to proceed: "
        read yesno
        if [ "$yesno" != "Y" ]; then
            if [ "$yesno" != "y" ]; then exit 0 ; fi
        fi
    fi
fi

mkdir -p $tempdir    

echo "Extracting $packname ..."
tail -n +$stubline "$this" > $tempdir/$packname

version=`echo $rootname | sed '/\(imod_[0-9.]*\).*/s//\1/' | sed '/\.$/s///'`

cd $tempdir

echo "Extracting $installer"

tar -xzf $packname $version/$installer

if [ ! -e $version/$installer ]; then
    echo "Could not extract $installer"
    exit 1
fi

mv -f $version/$installer .
rmdir $version

if [ $extract -eq 1 ]; then
    echo "Left $packname and $installer in $tempdir"
    exit 0
fi

# Fix python link in recent Cygwin distributions
python=python
retval=0
if [ $test -eq 0 ]; then
    if [ $os = windows ]; then
        if [ -e /bin/python.exe ]; then
            if [ -L /bin/python ]; then
                diff -q /bin/python.exe /bin/python > /dev/null 2>&1
                if [ $? -ne 0 ]; then
                    echo " "
                    echo "The copy of python in /bin/python.exe appears to be out of date"
                    echo "Making a new copy of the current python to /bin/python.exe"
                    cp -Lf /bin/python /bin/python.exe
                    echo " "
                fi
            fi
        else
            echo " "
            if [ -L /bin/python ]; then
                cp -Lf /bin/python /bin/python.exe
            elif [ -L /bin/python3 ]; then
                cp -Lf /bin/python3 /bin/python.exe
            elif [ -L /bin/python2 ]; then
                cp -Lf /bin/python2 /bin/python.exe
            else
                echo "You must have Python installed in Cygwin to run this installer"
                test=1
            fi
            if [ $test -eq 0 ]; then
                echo " "
                echo "The Cygwin link to python will not work for IMOD"
                echo "Making a copy of its target that is an actual /bin/python.exe"
                echo " "
            fi
        fi
    else

        # Now have to take IMOD off the path first in case it already has a python link
        while true
        do
            imodbin=`which imodinfo 2>/dev/null`
            if [ -z "$imodbin" ] ; then break ; fi
            imodbin=`echo $imodbin | sed '/\/imodinfo/s///'`
            export PATH=`echo $PATH | awk -F":" -v OFS="\n" ' $1=$1 ' | grep -v $imodbin | paste -sd ':' -`
        done

        which python > /dev/null 2>&1
        if [ $? -ne 0 ]; then
            python=
            if [ $os = osx ] ; then
                lookFor=0
                python=`which python3`
                if [ $? -ne 0 ] ; then 
                    lookFor=1
                elif [ $python = /usr/bin/python3 ] ; then
                    lookFor=1
                fi
                if [ $lookFor -ne 0 ] ; then
                    if [ -e /Library/Frameworks/Python.framework/Versions/Current/bin/python3 ] ; then
                        python=/Library/Frameworks/Python.framework/Versions/Current/bin/python3
                    elif [ -e /opt/homebrew/bin/python3 ] ; then
                        python=/opt/homebrew/bin/python3
                    elif [ -e /opt/anaconda3/bin/python ] ; then
                        python=/opt/anaconda3/bin/python
                    elif [ -e /opt/anaconda3/bin/python3 ] ; then
                        python=/opt/anaconda3/bin/python3
                    elif [ -e /opt/miniconda3/bin/python ] ; then
                        python=/opt/miniconda3/bin/python
                    elif [ -e /opt/miniconda3/bin/python3 ] ; then
                        python=/opt/miniconda3/bin/python3
                    elif [ -e /usr/local/anaconda3/bin/python ] ; then
                        python=/usr/local/anaconda3/bin/python
                    elif [ -e /usr/local/anaconda3/bin/python3 ] ; then
                        python=/usr/local/anaconda3/bin/python3
                    elif [ -e /Library/Developer/CommandLineTools/usr/bin/python3 ] ; then
                        python=/Library/Developer/CommandLineTools/usr/bin/python3
                    else
                        echo " "
                        echo "Neither python nor python3 is on the path and no usable python3 was found."
                        echo "You must install a Python3 first: either from python.org (available on IMOD"
                        echo "web site), from homebrew, or in the Apple Developer Command Line Tools."
                        echo "If there is some other Python, it must be on the search path to install IMOD."
                        retval=1
                        python=
                    fi
                fi

            elif [ -e /usr/bin/python3 ] ; then
                python=/usr/bin/python3
            elif [ -e /usr/bin/python2 ] ; then
                python=/usr/bin/python2
            else
                echo " "
                echo "There is no Python on the search path that runs with the command \"python\""
                echo "and there is neither python2 nor python3 in /usr/bin"
                echo "IMOD cannot be installed without one of these or a \"python\" on the path."
                retval=1
            fi
        fi
    fi
fi
if [ $os = windows ]; then export PATH="/bin:$PATH" ; fi

if [ $test -eq 0 ]; then
    if [ -n $python ] ; then
        $python -u $installer -dir "$installdir" $nameopt $scriptopt $skipopt $debopt $yesopt $packname
        retval=$?
        if [ $retval -eq 0 ] ; then
            if [ $python != python ] ; then
                echo " "
                echo "There is no /usr/bin/python, but there is $python."
                echo "Making a link in IMOD/pythonLink from \"python\" to $python"
                echo "because IMOD scripts need a Python on the path that runs with \"python\"."
                echo " "
                if [ -z $namedir ] ; then namedir=IMOD ; fi
                mkdir $installdir/$namedir/pythonLink
                ln -s $python $installdir/$namedir/pythonLink/python
            fi
        fi
    fi
fi

echo " "
echo "Cleaning up $packname, $installer, and $tempdir"
cd ..
if [ -n "$yesopt" ]; then sleep 2 ; fi
rm -f $tempdir/$installer $tempdir/$packname
rm -rf $tempdir
if [ $? -ne 0 ]; then echo "Sorry, you will have to do that yourself: rm -rf $tempdir" ; fi

exit $retval
    
#  $Id$
#

END OF STUB
