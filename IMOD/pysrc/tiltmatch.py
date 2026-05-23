#!/usr/bin/python3
# tiltmatch.py module - search pairs of tilts for best match and alignment
#
# Author: David Mastronarde
#
# $Id$
#

import os, math, glob
from pip import *
from imodpy import *

# Filter for tiltxcorr
sigma1 = 0.03
sigma2 = 0.05
radius2base = 0.25

# Search limit parameters
xyLimitFrac = 5
rotLimit = 15
otherLimits = '.1,.05,2'

# Number of wins more in one rotation direction than the other that will
# make it abandon the other direction
DIRECTION_WIN_THRESH = 4
bigWinFactors = (1.5, 2., 3.)
bigWinThresh = (3, 2, 1)

# Criteria for when to stop doing rotation scan and pick a mean or median angle
numAngForMean = 4
numAngForMedian = 5
angleMeanRangeCrit = 1
angleMedianCrit = 0.8

# Global variables needed by functions
tmpRoot = ''
pid = ''
leaveTmp = 0
tmpDir = ''
modPrefix = 'ERROR: '
modProgname = ''

# Clean up all the temp files before exiting
def cleanup():
   if leaveTmp:
      prnstr('Temporary files left in ' + tmpDir + ' as *' + pid)
   else:
      cleanList = glob.glob(tmpRoot + '*' + pid + '*')
      cleanupFiles(cleanList)
         

# Cleanup files, issue top message if any, and do the Imod error exit
def cleanExitError(message = ''):
   cleanup()
   if message:
      prnstr(modPrefix + message)
   exitFromImodError(modProgname)
   
# Set up names of temp files used in common
def getTempNames(progname):
   global tmpRoot, pid, tmpDir, modPrefix, modProgname
   modPrefix = 'ERROR: ' + progname + ' - '
   modProgname = progname

   tmpDir = imodTempDir()
   pid = '.' + str(os.getpid())
   tmpRoot = progname
   if tmpDir:
      tmpRoot = tmpDir + '/' + progname + '.'

   tmpMinxf = tmpRoot + 'minxf' + pid
   return tmpMinxf


# Return components for temp names; that module global didn't work in cygwin
def getTempComponents():
   return (tmpRoot, tmpDir, pid)


# Make a section list with just the number of views requested
def makeSecList(zeroA, nviewsA):
   asecList = [zeroA]
   ind = 1
   while ind < nviewsA:
      for dir in (1, -1):
         if len(asecList) < nviewsA:
            asecList.append(zeroA + ind * dir)
      ind += 1
   return (asecList, min(asecList))


# Count up wins for each direction and by how much, and stop doing a direction if any
# of the thresholds are met for number of wins by a certain factor
def evaluateWins(pm0, pm1, plusWin, minusWin, pmStart, pmEnd, plusBig, minusBig):
   if pm0 < pm1:
      minusWin += 1
   if pm0 > pm1:
      plusWin += 1
   if plusWin >= minusWin + DIRECTION_WIN_THRESH:
      pmStart = 1
   if minusWin >= plusWin + DIRECTION_WIN_THRESH:
      pmEnd = 0
   for ind in range(len(bigWinFactors)):
      if pm0 * bigWinFactors[ind] < pm1:
         minusBig[ind] += 1
      if pm0 > pm1 * bigWinFactors[ind]:
         plusBig[ind] += 1
      if plusBig[ind] >= bigWinThresh[ind] and not minusWin:
         pmStart = 1
      if minusBig[ind] >= bigWinThresh[ind] and not plusWin:
         pmEnd = 0
   return (plusWin, minusWin, pmStart, pmEnd)


# The main call, to read in some more common parameters and do the search
def searchPairs(progname, zeroA, zeroB, nviewsA, nviewsB, imageA, imageB, nxa, nxb, nya,
                nyb, AA, BB, lowestXfFile, lowestAsec, lowestBsec, distort, bilinear,
                allXfOut, expandAfac):
   global leaveTmp
   
   # Get common temp filenames and the additional one needed here
   tmpMinxf = getTempNames(progname)
   tmpImgb = tmpRoot + 'imgb' + pid
   tmpImgAmr = tmpRoot + 'imgamr' + pid
   tmpImgApr = tmpRoot + 'imgapr' + pid
   tmpImgAmx = tmpRoot + 'imgamx' + pid
   tmpImgApx = tmpRoot + 'imgapx' + pid
   tmpXcxf = tmpRoot + 'xcxf' + pid
   tmpXf1 = tmpRoot + 'xf1' + pid
   tmpXf2 = tmpRoot + 'xf2' + pid
   tmpRot90 = tmpRoot + 'rot90' + pid

   # Define search direction, mirroring, whether to use midas
   pmStart = 0
   pmEnd = 1
   angle = PipGetInteger('AngleOfRotation', 0)
   if angle < 0:
      pmEnd = 0
   if angle > 0:
      pmStart = 1
   midas = PipGetBoolean('RunMidas', 0)
   mirror = PipGetInteger('MirrorXaxis', 0)
   leaveTmp = PipGetBoolean('LeaveTempFiles', 0)
   mirStart = 0
   mirEnd = 1
   if mirror > 0:
      mirStart = 1
   if mirror < 0:
      mirEnd = 0

   # Get rotation scan variables and set 3-state flag for find, use, or skip angle
   (scanRotMax, scanRotStep) = PipGetTwoFloats('ScanRotationMaxAndStep', 20., 4.)
   findRotation = 1
   bestAngles = [[], [], [], []]
   if scanRotMax == 0.:
      findRotation = 0
   elif scanRotStep == 0.:
      findRotation = -1
      bestRotation = scanRotMax

   # Set the binning needed to get image size to 512 or less unless the size is
   # bigger than 4K, in which case bin to 1024.  Limit binning to 4 between 2048
   # and 4096.  Set limits on X/Y in search
   size = int(math.floor(math.sqrt(nxa * nya)))
   limit = 512
   if size >= 4096:
      limit = 1024
   simplexBinning = (size + limit - 1) // limit
   if size < 4096 and simplexBinning > 4:
      simplexBinning = 4
   xlimit = nxb // xyLimitFrac
   ylimit = nyb // xyLimitFrac

   # Control the binning in tiltxcorr since speed is more important than high precision
   xcorrBinning = min(12, max(1, int(round(size / 900.))))

   # Relax the filter above binning of 5, where antialiasing will be applied
   radius2 = radius2base
   if xcorrBinning > 5:
      radius2 = min(0.5, radius2base + 0.05 * (xcorrBinning - 5))
   
   # Set up lists to do sections from center out
   (asecList, asecStart) = makeSecList(zeroA, nviewsA)
   (bsecList, bsecStart) = makeSecList(zeroB, nviewsB)

   if allXfOut and (not midas and pmStart == 0 and pmEnd == 1):
      exitError('You need to use midas or specify the rotation direction to get ' +\
                   'all transforms written')
   if allXfOut and not mirror:
      mirEnd = 0

   # set up for midas
   #
   if midas:
      if pmEnd != pmStart:
         pmEnd = 0
   else:
       prnstr("Finding the best matched pair of views in the two series:")
       prnstr("              (Type Ctrl-C to end search)")

   # Loop on section from b, section from a, and -/+90 rotations
   diffMin = 2000000000
   diffLowestTilt = diffMin
   plusWin = 0
   minusWin = 0
   ind = len(bigWinFactors)
   plusBig = [0] * ind
   minusBig = [0] * ind
   mirrorWin = 0
   regularWin = 0
   mirrorBig = [0] * ind
   regularBig = [0] * ind
   regMirText = ('regular', 'mirror ')
   rot90sec = [-1] * 4
   tmpImgAmp = (tmpImgAmr, tmpImgApr, tmpImgAmx, tmpImgApx)

   # Make list of unfound differences.
   # You cannot use [[-1] * nA] * nB because that makes shallow copies of the inner list
   diffList = []
   for ind in range(nviewsB):
      diffList.append([-1.] * nviewsA)
   allXfList = []
   if allXfOut:
      for ind in range(nviewsB):
         allXfList.append(['1 0 0 1 0 0'] * nviewsA)
   
   try:
      for asecInd in range(nviewsA):
         asec = asecList[asecInd]
         for bsecInd in range(nviewsB):
            bsec = bsecList[bsecInd]
            plusMinus = pmStart
            pmDiffs = [1.e37] * 4
            while plusMinus <= pmEnd:
               mirInd = mirStart
               while mirInd <= mirEnd:
                  pmrxInd = plusMinus + 2 * mirInd
                  tmpImga = tmpImgAmp[pmrxInd]
                  if plusMinus:
                     if mirInd:
                        rotstr = fmtstr("0 -{0} -{0} 0 0 0", expandAfac)
                     else:
                        rotstr = fmtstr("0 -{0} {0} 0 0 0", expandAfac)
                     pmAngle = +90
                  else:
                     if mirInd:
                        rotstr = fmtstr("0 {0} {0} 0 0 0", expandAfac)
                     else:
                        rotstr = fmtstr("0 {0} -{0} 0 0 0", expandAfac)
                     pmAngle = -90

                  writeTextFile(tmpRot90, [rotstr])
                  if rot90sec[pmrxInd] != asec:

                     # extract the rotated section from A if it is needed
                     try:
                        runcmd(fmtstr('newstack -sec {} -xform {} -size {},{} -use 0 ' +\
                                         '{} "{}" "{}"', asec, tmpRot90, nxb, nyb,
                                      distort, imageA, tmpImga))
                        rot90sec[pmrxInd] = asec
                     except ImodpyError:
                        cleanExitError(fmtstr('Extracting rotated section from {}', AA))

                  if distort:

                     # If undistorting, need to extract the reference section too,
                     # otherwise the reference is the stack
                     try:
                        runcmd(fmtstr('newstack -sec {} -use 0 {} "{}" "{}"', bsec,
                                      distort, imageB, tmpImgb))
                        refSec = 0
                        refImage = tmpImgb
                     except ImodpyError:
                        cleanExitError(fmtstr('Extracting distortion-corrected section' +\
                                                 ' from {}', BB))

                  else:
                     refSec = bsec
                     refImage = imageB
                        
                  if midas:
                     if not asecInd and not bsecInd and mirInd == mirStart:

                        # first time, run midas
                        if bilinear:
                           prnstr('Starting midas - you should align as well as possible,'
                                  )
                        else:
                           prnstr('Starting midas - you should align translation and ' +\
                                     'rotation,')
                        prnstr(' and save the transform to the already-defined output ' +\
                                  'file')
                        prnstr(' ')
                        try:
                           runcmd(fmtstr('midas -D -r "{}" -rz {} "{}" "{}"', refImage,
                                         refSec, tmpImga, tmpXf1))
                        except ImodpyError:
                           trueError = True
                           if os.path.exists(tmpXf1):

                              # If the xf file exists, then test if there is an ERROR
                              # output AND see if it has a negative status to ignore
                              # a Qt crash on exit
                              errStr = getErrStrings()
                              for line in errStr:
                                 if 'ERROR:' in line:
                                    break
                                 if 'with status' in line:
                                    lsplit = line.strip().split()
                                    try:
                                       status = int(lsplit[-1])
                                       if status < 0:
                                          trueError = False
                                    except Exception:
                                       pass

                           if trueError:
                              cleanExitError()
                           
                        if not os.path.exists(tmpXf1):
                           cleanup()
                           exitError('Transform file not found - cannot proceed')

                        tmpInitXf = tmpXf1
                        prnstr('Finding the best matched pair of views in the two series:'
                               )
                        prnstr('              (Type Ctrl-C to end search)')

                        
                  else:

                     # Run tiltxcorr if no midas
                     angleOpt = ''
                     if findRotation > 0:
                        angleOpt = fmtstr('ScanRotationMaxAndStep {} {}', scanRotMax,
                                          scanRotStep)
                     elif findRotation < 0:
                        angleOpt = fmtstr('ScanRotationMaxAndStep {} 0.', bestRotation)
                        bestAng = bestRotation
                     try:
                        xccom = ['InputFile ' + tmpImga,
                                 'OutputFile ' + tmpXcxf,
                                 'TiltAngles 0',
                                 'ReferenceFile ' + refImage,
                                 'ReferenceView ' + str(refSec + 1),
                                 'BinningToApply ' + str(xcorrBinning),
                                 'FilterRadius2 ' + str(radius2),
                                 'FilterSigma1 ' + str(sigma1),
                                 'FilterSigma2 ' + str(sigma2)]
                        if angleOpt:
                           xccom.append(angleOpt)
                        if xcorrBinning > 5:
                           xccom.append('AntialiasFilter 4')
                           
                        xcLines = runcmd('tiltxcorr -StandardInput', xccom)
                     except ImodpyError:
                        cleanExitError('Running tiltxcorr to get initial correlation ' +\
                                          'alignment')

                     # Extract rotation from the tiltxcorr output
                     if findRotation > 0:
                        for line in xcLines:
                           if 'Best angle in' in line:
                              ind = line.rfind('=')
                              if ind > 0:
                                 try:
                                    bestAng = float(line[ind + 1:])
                                 except Exception:
                                    exitError('Converting best angle output from ' +\
                                                 'Tiltxcorr to float')
                                 bestAngles[pmrxInd].append(bestAng)
                                 #prnstr('Found rotation ' + str(bestAng))
                                 break
                        else:  # ELSE ON FOR
                           exitError('Cannot find rotation angle in output of Tiltxcorr')
                                    

                     # Run xfsimplex looking for rotation only for legacy run (0,0 
                     # entered for -scan) or if angle being used is at end of scan range
                     if findRotation == 0 or (math.fabs(math.fabs(bestAng) - scanRotMax) <
                                              0.1 and scanRotStep != 0.):
                        xfcom = ['AImageFile ' + refImage,
                                 'BImageFile ' + tmpImga,
                                 'OutputFile ' + tmpXf1,
                                 'SectionsToUse ' + str(refSec) + ' 0',
                                 'InitialTransformFile ' + tmpXcxf,
                                 'VariablesToSearch 3',
                                 'BinningToApply ' + str(simplexBinning),
                                 fmtstr('LimitsOnSearch {},{},{}', xlimit, ylimit,
                                        rotLimit)]
                        tmpInitXf = tmpXf1
                        try:
                           #prnstr('Running rotation-only xfsimplex')
                           runcmd('xfsimplex -StandardInput', xfcom)
                        except ImodpyError:
                           cleanExitError('Running first xfsimplex with rotation only')

                     else:
                        tmpInitXf = tmpXcxf


                  # Run xfsimplex again from there, looking for full transform
                  xfcom = ['AImageFile ' + refImage,
                           'BImageFile ' + tmpImga,
                           'OutputFile ' + tmpXf2,
                           'SectionsToUse ' + str(refSec) + ' 0',
                           'InitialTransformFile ' + tmpInitXf,
                           'VariablesToSearch 6',
                           'LinearInterpolation ' + str(bilinear),
                           'BinningToApply ' + str(simplexBinning),
                           fmtstr('LimitsOnSearch {},{},{},{}', xlimit, ylimit, rotLimit,
                                  otherLimits)]
                  try:
                     simpLines = runcmd('xfsimplex -StandardInput', xfcom)
                  except ImodpyError:
                     cleanExitError('Running second xfsimplex with full transform')

                  try:
                     diffspl = simpLines[len(simpLines) - 2].split()
                     diff = float(diffspl[1])
                  except Exception:
                     cleanup()
                     exitError('Extracting difference value from Xfsimplex output')
                  prnstr(fmtstr('{} {} {} {} rotation {:3d} {} difference {:11.6f}', AA,
                                asec + 1, BB, bsec + 1, pmAngle, regMirText[mirInd],
                                diff), end = '')

                  # Accumulate transform list if requested
                  if allXfOut:
                     try:
                        runcmd(fmtstr('xfproduct "{}" "{}" "{}"', tmpRot90, tmpXf2,
                                      tmpXf1))
                     except ImodpyError:
                        cleanExitError('Taking product of 90 degree and found transform')
                     oneXf = readTextFile(tmpXf1)
                     allXfList[bsec - bsecStart][asec - asecStart] = oneXf[0]
                     
                  # Keep track of minimum and transform there
                  if diff < diffMin:
                     prnstr('*', flush=True)
                     diffMin = diff
                     asecBest = asec
                     bsecBest = bsec
                     try:
                        runcmd(fmtstr('xfproduct "{}" "{}" "{}"', tmpRot90, tmpXf2,
                                      tmpMinxf))
                     except ImodpyError:
                        cleanExitError('Taking product of 90 degree and found transform')

                  else:
                     prnstr(' ', flush=True)

                  #  Accumulate differences from plus and minus
                  pmDiffs[pmrxInd] = diff

                  # If XF file at lowest tilt requested, output one for best lowest tilt
                  # pair
                  if lowestXfFile and asec == lowestAsec and bsec == lowestBsec and \
                         diff < diffLowestTilt:
                     diffLowestTilt = diff
                     try:
                        runcmd(fmtstr('xfproduct "{}" "{}" "{}"', tmpRot90, tmpXf2,
                                      lowestXfFile))
                     except ImodpyError:
                        cleanExitError('Taking product of 90 degree and found transform')

                  mirInd += 1      # End of mirror loop

               plusMinus += 1      # end of plusMinus loop

            # If there are both plus and minus or regular and mirrored, count who wins
            # and stop doing a consistent loser, or a big loser sooner
            # First evaluate plus and minus
            if pmStart < pmEnd:
               (plusWin, minusWin, pmStart, pmEnd) = evaluateWins(
                  min(pmDiffs[0], pmDiffs[2]), min(pmDiffs[1], pmDiffs[3]),
                  plusWin, minusWin, pmStart, pmEnd, plusBig, minusBig)

            # Use same logic for regular/mirror
            if mirStart < mirEnd:
               (regularWin, mirrorWin, mirStart, mirEnd) = evaluateWins(
                  min(pmDiffs[0], pmDiffs[1]), min(pmDiffs[2], pmDiffs[3]),
                  regularWin, mirrorWin, mirStart, mirEnd, regularBig, mirrorBig)

            diffList[bsec - bsecStart][asec - asecStart] = min(pmDiffs)

            # Once there is a single direction, see if the angles are consistent enough
            # to stop scanning for them
            pmInd = pmEnd + 2 * mirEnd
            numAng = len(bestAngles[pmInd])
            if pmStart == pmEnd and mirStart == mirEnd and findRotation > 0 and \
                   numAng >= numAngForMean:
               meanAngle = sum(bestAngles[pmInd]) / numAng
               minAngle = min(bestAngles[pmInd])
               maxAngle = max(bestAngles[pmInd])
               if meanAngle - minAngle < angleMeanRangeCrit and \
                      maxAngle - meanAngle < angleMeanRangeCrit:
                  bestRotation = meanAngle
                  findRotation = -1
                  #prnstr(fmtstr('Using mean rotation {:2f}, min = {:2f}, max = {:2f},' +\
                  #                 ' n = {}', meanAngle, minAngle, maxAngle, numAng))
               elif numAng >= numAngForMedian:
                  bestAngles[pmInd].sort()
                  if numAng % 2:
                     median = bestAngles[pmInd][numAng // 2]
                  else:
                     median = (bestAngles[pmInd][numAng // 2] +
                               bestAngles[pmInd][numAng // 2 - 1]) / 2.
                  if (median - bestAngles[pmInd][1] < angleMedianCrit and \
                         maxAngle - median < angleMedianCrit) or \
                         (median - minAngle < angleMedianCrit and \
                             bestAngles[pmInd][-2] - median < angleMedianCrit):
                     bestRotation = median
                     findRotation = -1
                     #prnstr(fmtstr('Using median rotation {:2f}, min = {:2f}, max =' +\
                     #                 ' {:2f}, n = {}', median, minAngle, maxAngle,
                     #              numAng))
                         
   except KeyboardInterrupt:
      pass 

   return (asecBest, bsecBest, diffList, allXfList)
