#$if (! -e g5tmpdir) mkdir g5tmpdir
#
# MATCHORWARP RUNS REFINEMATCH AND MATCHVOL, OR FINDWARP AND WARPVOL.
#
####CreatedVersion#### 4.8.28
#
$matchorwarp -StandardInput
InputVolume      g5b.rec
OutputVolume     g5b.mat
SizeXYZorVolume  g5a.rec
RefineLimit      0.3
WarpLimits       0.2,0.27,0.35
ResidualFile     patch.resid
ClipPlaneBoxSize 600
#TemporaryDirectory  g5tmpdir
