#!/bin/bash
allocID=`cat slurmalloc.jobid`
scancel -Q $allocID >/dev/null 2>&1
rm -f slurmalloc.jobid
exit 0
