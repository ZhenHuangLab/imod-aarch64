#!/bin/sh
#Don't timestamp if there is nothing to push
todo=`hg outgoing|grep -c changeset`
do_timestamp=0
if [ ! $todo -eq 0 ] ; then
    do_timestamp=1
    echo $todo changesets
fi
#push, exit if push fails
hg push
if [ $? -eq 0 ] ; then
#timestamp
#    if [ $do_timestamp -eq 1 ] ; then
	etomofile=Etomo/src/etomo/logic/VersionControl.java
	datetime=`date +"%-m\/%-d\/%-Y %R"`
	echo $datetime
	sed "/TIME_STAMP/s/\".*\"/\"$datetime\"/" $etomofile > $etomofile.tmp
	if [ $? -eq 0 ] ; then
	    mv -f $etomofile.tmp $etomofile
	    hg commit -m "timestamping" $etomofile
	    hg push
	else
	     echo timestamp update failed
	fi
#    fi
fi
