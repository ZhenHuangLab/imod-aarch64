#!/bin/bash

# pre.sh N: Create an N node allocation, saving the id number in 
# slurmalloc.jobid.

ProgName=$0
N=$1

if [[ $# != 2 ]]
then
  echo "Usage: $ProgName N QOS"
  echo "       where N is the number of nodes to allocate"
  echo "         and QOS is the Quality of Service name"
  exit 1
fi

# Free any existing allocation
if [[ -e slurmalloc.jobid ]]
then
  jobid=`cat slurmalloc.jobid`
  scancel -Q jobid >/dev/null 2>&1
  rm slurmalloc.jobid
fi

# Now create a new allocation and store its jobid in slurmalloc.jobid
#
# This is an awful kludge. To be able to srun jobs in the background
# using a previous allocation, the allocation must be created with
# salloc --no-shell. When you do so, the resulting allocation number
# appears in a message on your screen, but it's apparently not sent
# to either stdout or stderr, so we have to resort to ridiculous
# and not completely safe machinations using squeue to get the 
# id number. There's probably a a better way to do this...
tmpfile=`mktemp`
squeue --user=$USER -o "%t %A" | grep '^R ' | sed -e's/^R //' >$tmpfile
echo running salloc -N $1 -c 1 --qos=$2 --no-shell >>~/sallocOut.txt
if salloc -N $1 -c 1 --qos=$2 --no-shell
then
  squeue --user=$USER -o "%t %A" | grep '^R ' | sed -e's/^R //' >>$tmpfile
  sort $tmpfile | uniq >slurmalloc.jobid
  rm -f $tmpfile
  exit 0
else
  exit 1
fi

