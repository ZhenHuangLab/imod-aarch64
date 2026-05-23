#!/bin/bash
# This script will make sure that there is a current python.exe in cygwin
# Newer cygwin has only a cygwin link from python to python2.x.exe
# Due to some arcane memory operations related to security vulnerabilities,
# the file now needs to be a hard link instead of a copy
# This will not work when called from non-cygwin applications (java)
PATH="/bin:$PATH"
if [[ -e /bin/python3 ]] ; then
    cygPyth="/bin/python3"
elif [[ -e /bin/python2 ]] ; then
    cygPyth="/bin/python2"
elif [[ -e /bin/python ]] ; then
    cygPyth="/bin/python"
else
    echo "Cannot find /bin/python, /bin/python2, or /bin/python in Cygwin"
    if [[ -e /bin/python.exe ]] ; then
        echo "There is an existing /bin/python.exe that may or may or not work"
    else
        echo "You need to install a Cygwin python to use Cygwin with IMOD"
    fi
    exit 1
fi
makeLink=0
if [[ -e /bin/python.exe && -h "$cygPyth" ]] ; then

    # The IMOD installStub has been copying or linking the link target to python.exe
    # If it already exists, check whether that copy is the same as the current link target
    diff -q /bin/python.exe "$cygPyth" > /dev/null 2>&1
    if [ $? -ne 0 ] ; then 
        echo "The file /bin/python.exe appears to be out of date"
        echo "Making a new hard link of the current python to /bin/python.exe"
        makeLink=1

        # Then check whether it is still a copy instead of a hard link
    elif [[ "$(stat -c %h python.exe)" -eq 1 ]] ; then
        echo "The file /bin/python.exe is a copy, not a hard link"
        echo "Making a hard link instead, of the current python to /bin/python.exe"
        makeLink=1
    fi
    
elif [[ ! -e /bin/python.exe ]] ; then

    # Or, if there is no python.exe and there is a link, make it a hard link too
    if [ -h "$cygPyth" ] ; then 
        echo "The Cygwin link to python will not work for IMOD"
        echo "Making a hard link of the current python to /bin/python.exe"
        makeLink=1
    else
        echo "There is no Python installed in Cygwin"
        exit 1
    fi
fi
if [ $makeLink -eq 1 ] ; then
    ln -Lf "$cygPyth" /bin/python.exe
    if [ $? -ne 0 ] ; then 
        echo "There was an error making a hard link to /bin/python.exe"
        exit 1
    fi
fi
exit 0
