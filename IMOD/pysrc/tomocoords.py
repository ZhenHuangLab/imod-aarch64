#!/usr/bin/python3
# tomocoords.py module - used by subtomosetup, rawtiltcoords, and ctf3dsetup
#
# Author: David Mastronarde
#
# $Id$
#

yzRatioCrit = 2.

import os, sys, math
from pip import *
from imodpy import *

# Do initial option startup and get the options that are common between the programs
def getCommonOptions(options, progname):
   (opts, nonopts) = PipReadOrParseOptions(sys.argv, options, progname, 1, 0, 1)

   # Get the options
   rootName = PipGetString('RootName', '')
   if not rootName:
      exitError('A root name must be entered')
   volName = PipGetString('VolumeModeled', '')
   if not volName:
      exitError('The name of the volume on which points were picked must be entered')

   # Get position file and determine if it is model or point file
   centerFile = PipGetString('CenterPositionFile', '')
   if not centerFile:
      exitError('You must enter a model or point file name with -center')
   pointFile = ''
   modelFile = ''
   try:
      infoLines = runcmd('imodinfo -h "' + centerFile + '"', inStderr = 'stdout')
      for line in infoLines:
         if 'Error (-1) reading' in line or 'Model has no objects' in line:
            pointFile = centerFile
            break
   except:
      pointFile = centerFile

   if not pointFile:
      modelFile = centerFile
      
   objList = PipGetString('ObjectsToUse', '')
   if objList and pointFile:
      exitError('You cannot enter a list of objects with a point file')

   comFile = PipGetString('CommandFile', 'tilt.com')
   reorientIn = PipGetInteger('ReorientionType', 0)
   enteredOrient = 1 - PipGetErrNo()
   return (rootName, volName, centerFile, pointFile, modelFile, objList, comFile, \
       reorientIn, enteredOrient)


# Get the coordinates from a model or point file, read the headers of all relevant files,
# and work out the orientation with all the needed messages
def getPointsAndHeaders(modelFile, objList, pointFile, progname, stackName, aliName, \
                           volName, fullRec, enteredOrient, reorient, comFile):
   
   # Get the coordinate list
   pid = '.' + str(os.getpid())
   cleanList = []
   descrip = ''
   if modelFile:
      modConvert = modelFile
      descrip = 'temporary'

      # Extract objects
      if objList:
         modConvert = modelFile + '.obj' + pid
         cleanList.append(modConvert)
         try:
            runcmd(fmtstr('imodextract "{}" "{}" "{}"', objList, modelFile, modConvert))
         except ImodpyError:
            cleanupFiles(cleanList)
            exitFromImodError(progname)

      # Convert to point list, with -scale option to compensate for subset loading
      pointFile = modelFile + '.pt' + pid
      cleanList.append(pointFile)
      try:
         runcmd(fmtstr('model2point -scale -float "{}" "{}"', modConvert, pointFile))
      except ImodpyError:
         cleanupFiles(cleanList)
         exitFromImodError(progname)
         
   # Now read in the point file and process the lines
   pointLines = readTextFile(pointFile, descrip + ' point file', True)
   cleanupFiles(cleanList)
   if isinstance(pointLines, str):
      exitError(pointLines)

   pointList = []
   for line in pointLines:
      try:
         lsplit = line.split()
         if len(lsplit) < 3:
            exitError('There are not three values on the line in ' + descrip + \
                         ' point file: ' + line)
         pointList.append([float(lsplit[0]), float(lsplit[1]), float(lsplit[2])])
      except ValueError:
         exitError('Converting a value to a floating point number on the line in ' + \
                      descrip + ' point file: ' + line)

   # Get the file headers
   try:
      (nxRaw, nyRaw, nzRaw, mode, pixXraw, pixYraw, pixZraw) = getmrc(stackName)
      (nxAli, nyAli, nzAli, mode, pixXali, pixYali, pixZali, origXali, origYali, \
          origZali, dmin, dmax, dmean) = getmrc(aliName, True)
      (nxVol, nyVol, nzVol, mode, pixXvol, pixYvol, pixZvol, origXvol, origYvol, \
          origZvol, dmin, dmax, dmean) = getmrc(volName, True)
      if fullRec:
         (nxFull, nyFull, nzFull, mode, pixXfull, pixYfull, pixZfull, origXfull, \
             origYfull, origZfull, dmin, dmax, dmean) = getmrc(fullRec, True)
      else:
         (nxFull, nyFull, nzFull, pixXfull, pixYfull, pixZfull, origXfull, \
             origYfull, origZfull) = (0, 0, 0, 0, 0, 0, 0, 0, 0)

      headLines = runcmd('header "' + volName + '"')
      aliBinning = int(round(pixXali / pixXraw))
   except ImodpyError:
      exitFromImodError(progname)

   # Deduce post-processing of the volume: look for tilt angles and clip flipyz
   tiltXorig = -999.
   orientTitle = -2
   for line in headLines:
      if 'ilt angles' in line and tiltXorig < -990:
         lsplit = line.split();
         if len(lsplit) < 9:
            exitError('Tilt angles line in header output from ' + volName + \
                         ' has too few items')
         try:
            tiltXorig = float(lsplit[-6])
            tiltXcur = float(lsplit[-3])
         except ValueError:
            exitError('Converting tilt angles to floating point values in header ' +\
                         'output from ' + volName)
      if 'clip: flipyz' in line.lower():
         orientTitle = 1
      if 'clip: rotx' in line.lower():
         orientTitle = -1

   # Set orientation values based on size and angles
   orientSize = -2
   if nzVol < nyVol / yzRatioCrit:
      orientSize = 1
   if nyVol < nzVol / yzRatioCrit:
      orientSize = 0
         
   orientAngle = -2
   if tiltXorig == 90. and tiltXcur == 0.:
      orientAngle = -1
   elif tiltXorig == 0. and tiltXcur == 90.:
      orientAngle = orientSize

   # Give information only when orientation entered
   if enteredOrient:
      if reorient < 0 and orientTitle == 1:
         prnstr('Using specified reorientation even though title indicates rotation ' +\
                   'around X')
      if reorient < 0 and orientAngle != -1:
         prnstr('Using specified reorientation even though header angles indicate ' +\
                   ' rotation around X')
      if reorient >= 0 and orientTitle == -1:
         prnstr('Using specified reorientation even though title indicates swapping ' +\
                   'of Y and Z')
      if reorient >= 0 and orientAngle == -1:
         prnstr('Using specified reorientation even though header angles indicate ' +\
                   'swapping of Y and Z')
   else:

      # Otherwise look for consistency, inform of decision in almost all cases, warn of
      # possible inconsistency, exit if not conclusive
      if orientAngle == -1:
         reorient = -1
         if orientTitle == 1:
            prnstr('WARNING: ' + progname + ' - Assuming reorientation by rotation ' +\
                      'around  X because of header angles, but there is a title ' +\
                      ' indicating swapping of Y and Z')
         if orientSize == 0:
            prnstr('WARNING: ' + progname + ' - Assuming reorientation by rotation ' +\
                      'around X because of header angles, even though Z dimension is ' +\
                      'much bigger than Y')
      elif orientAngle == -2:
         exitError('You must enter -reorient to indicate reorientation type, because ' +\
                      'header angles are not consistent with known types')
      else:
         if orientTitle == -1:
            exitError('You must enter -reorient to indicate reorientation type, ' +\
                         'because header angles are not consistent with title ' +\
                         'indicating rotation around X')
         if orientTitle == 1:
            if orientSize == 0:
               exitError('You must enter -reorient to indicate reorientation type, ' +\
                            'because there is a title indicating swapping of Y and Z ' +\
                            'but the Z dimension is much bigger than Y')
            reorient = 1
            if orientSize == 1:
               prnstr('Assuming reorientation by swapping Y and Z as indicated by title')
            else:
               prnstr('WARNING: ' + progname + ' - Assuming reorientation by swapping ' +\
                         'Y and Z as indicated by title, but Y and Z dimensions do ' +\
                         'not clearly support this assumption')
         else:
            if orientSize < 0:
               exitError('You must enter -reorient to indicate reorientation type, ' +\
                            'because Y and Z dimensions do not clearly indicate ' +\
                            'orientation')
            reorient = orientSize
            if reorient:
               prnstr('Assuming reorientation by swapping Y and Z because of Y and Z ' +\
                         'dimensions, even though there is no flipyz title')
            else:
               prnstr('Assuming no reorientation because of Y and Z dimensions')
               
   comLines = readTextFile(comFile)
   return (pid, cleanList, pointList, aliBinning, reorient, comLines, \
              nxRaw, nyRaw, nzRaw, pixXraw, pixYraw, pixZraw, \
              nxAli, nyAli, nzAli, pixXali, pixYali, pixZali, origXali, origYali, \
              origZali, nxVol, nyVol, nzVol, pixXvol, pixYvol, pixZvol, origXvol, \
              origYvol, origZvol, nxFull, nyFull, nzFull, pixXfull, pixYfull, pixZfull, \
              origXfull, origYfull, origZfull)


# Gets shared values when doing CTF correction for 3D CTF,checks whether an aligned 
# stack has CTF correction title, returns size in X, y, z as well as those values
def getCTFoptionsCheckIfCorrected(progname, tiltLines, ctfLines):
   xaxisTilt = optionValue(tiltLines, 'xaxistilt', 2, 1, numVal = 1)
   tiltUseGPU = optionValue(tiltLines, 'UseGPU', 1, 1, numVal = 1)
   ctfXtilt = optionValue(ctfLines, 'XAxisTilt', 2, 1, numVal = 1)
   ctfUseGPU = optionValue(ctfLines, 'UseGPU', 1, 1, numVal = 1)
   ctfInput = optionValue(ctfLines, 'InputStack', 0, 1)
   ctfOutput = optionValue(ctfLines, 'OutputFileName', 0, 1)
   ctfPixSize = optionValue(ctfLines, 'PixelSize', 2, 1, numVal = 1)
   if not os.path.exists(ctfInput):
      exitError('Input file in ctf correction command file, ' + ctfInput +\
                ', does not exist')
   try:
      headLines = runcmd('header ' + ctfInput)
      for line in headLines:
         if 'ctfphaseflip' in line.lower():
            exitError('Ctfphaseflip appears to have been run already on the input ' +\
                      'stack for CTF correction, ' + ctfInput)
      (nx, ny, nz) = getmrcsize(ctfInput)

   except ImodpyError:
      exitFromImodError(progname)

   if xaxisTilt == None:
      xaxisTilt = 0.
   if tiltUseGPU == None:
      tiltUseGPU = -1
   return (nx, ny, nz, xaxisTilt, tiltUseGPU, ctfXtilt, ctfUseGPU, ctfPixSize, ctfInput,
           ctfOutput)


# Test for X tilt consistency between CTF correction and reconstruction and if it matters
def checkXtiltCtfVsRec(xaxisTilt, ctfXtilt, warnXtiltCrit, ysizeNm, slabNm, slabText,
                       progname):
   if not xaxisTilt:
      xaxisTilt = 0.
   if ctfXtilt != None and math.fabs(xaxisTilt - ctfXtilt) >= 0.1:
      prnstr(fmtstr('WARNING: {} - X-axis tilt for CTF correction is non-zero ' +\
                    '({})  but it differs from X-tilt for reconstruction ({})',
                    progname, ctfXtilt, xaxisTilt))
   else:
      warnStart = 'The difference in X-axis tilts between CTF correction and' +\
                  ' reconstruction'
      if ctfXtilt == None:
         warnStart = 'X-axis tilt is not included in the CTF correction but the' +\
                     ' X-tilt in reconstruction'
         noCtfXtilt = True
         ctfXtilt = 0.
      errorNm = 0.5 * ysizeNm * math.fabs(math.tan(math.radians(xaxisTilt)) - 
                                               math.tan(math.radians(ctfXtilt)))
      if errorNm / slabNm > warnXtiltCrit:
         prnstr(fmtstr('WARNING: {} - {} is big enough to give an error in ' +\
                       'Z-height of {:.0f} nm,' + \
                       ' more than {} of the {} thickness ({:.0f} nm)',
                       progname, warnStart, errorNm, warnXtiltCrit, slabText, slabNm))


# Find the number of command files created by splittilt or splitcorrection
def findSplitComNumber(splitout, descrip):
   retval = -1
   reg = re.compile(r'^([ 0-9]*).* files for [ 0-9]*chunks created.*$')
   for l in splitout:
      if l.startswith('WARNING:'):
         prnstr(l)
      if reg.search(l):
         numstr = reg.sub(r'\1', l)
         if numstr and numstr != "":
            retval = int(numstr)
   if retval < 0:
      exitError('Cannot determine com file number from ' + descrip)
   return retval


# Look for a file name specified by the given option, and if not present, try to derive
# it from the name of the tilt com file by substituting the defaultName string for 'tilt'
def getOrDeriveComFile(option, defaultName, tiltComFile, descrip):
   comFile = PipGetString(option, '')
   if comFile:
      comFile, comRootname = completeAndCheckComFile(comFile)
   else:
      if 'tilt' not in tiltComFile:
         exitError('You must enter the name of the ' + descrip + ' erasing command ' +\
                   'file since ' + tiltComFile + ' does not contain "tilt"')
      comFile = tiltComFile.replace('tilt', defaultName)
      if not os.path.exists(comFile):
         exitError('You must enter the name of the ' + descrip + ' command file, the ' +\
                   'file ' + comFile + ' dose not exist')
   
   return comFile


