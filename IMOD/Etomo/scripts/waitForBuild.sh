#! /bin/bash

#
# Waits for the IMOD build to complete before returning.
#

currentTime=`date +%H`
echo the current hour is $currentTime
if [ $currentTime -ge 3 -a $currentTime -lt 5 ] ; then
  echo avoiding the IMOD build by sleeping for two hours
  sleep 2h
fi
