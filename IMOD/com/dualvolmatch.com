# THIS COMMAND FILE RUNS DUALVOLMATCH
#
####CreatedVersion#### 4.8.27
#
$dualvolmatch -StandardInput
RootName	g5
MatchAtoB	9
MaximumResidual	10.
CenterShiftLimit	10.
#
$ echo "STATUS: DUALVOLMATCH RAN SUCCESSFULLY"
