#!/usr/bin/python
# prochunks.py module
# Functions for running processchunks in the background
#
# Author: David Mastronarde
#
#  $Id$

import os, time
from imodpy import *

# Definitions for indexes to the param array
(TopQuitPci, NumDonePci, ElapsedPci, GotLinesAtPci, OpenedPci, ReadOKPci, GotPIDPci, \
 FinishedPci, ErrorPci, CheckTimePci, InChunkErrorPci, GotDoneSoFarPci, \
 WherePci, OutFilePci, OutNamePci, CheckNamePci) = tuple(range(16))
                                                                                    

# Check a program's top-level check file for finishing current operation or quitting.
# Returns empty string if topCheckFile is None, blank, non-existent, empty, or lacks a
# recognized code on the last line; 'F' if it contains F; or 'Q' if it contains Q; 'P' if
# lookForPause is True and there is a P.  It outputs to proChunkCheckFile: Q if there is
# a Q; P if lookForPause is True and there is P; or P if there is an F and sendPauseForF
# is True.  It outputs a message in the latter two cases so that a message appears
# immediately
def checkForProChunksQuit(topCheckFile, proChunkCheckFile, lookForPause = False,
                          sendPauseForF = False):
   global finishSetAndQuit
   pauseMess = 'RECEIVED SIGNAL TO FINISH CURRENT OPERATIONS AND EXIT'
   if not topCheckFile or not os.path.exists(topCheckFile):
      return 0
   checklines = readTextFile(topCheckFile, None, True)
   if isinstance(checklines, str) or len(checklines) < 1:
      return ''
   if checklines[-1].startswith('Q'):
      writeTextFile(proChunkCheckFile, ['Q'], True)
      return 'Q'
   isP = checklines[-1].startswith('P')
   isF = checklines[-1].startswith('F')
   if isF or (lookForPause and isP):
      if isP or sendPauseForF:
         writeTextFile(proChunkCheckFile, ['P'], True)
      if not finishSetAndQuit:
         prnstr(pauseMess, flush = True)
      finishSetAndQuit = True
      return checklines[-1][0]
   return ''


# Starts a processchunks run in the background with options given in comArray and output 
# to outfile.  proChunkCheckFile is the check file that processchunks will be run with
# param is an array of at least 16 elements that will be initialized.  
# Adds 'processchunks', '-P', '-g', '-c', and proChunkCheckFile to the front of the
# comArray.  Returns the return from bkgdProcess (error message or NULL).
def startProcesschunks(comArray, outfile, proChunkCheckFile, param):
   param[ElapsedPci] = 0.
   param[GotLinesAtPci] = -1.
   param[OpenedPci] = False
   param[ReadOKPci] = False
   param[GotPIDPci] = False
   param[CheckTimePci] = 0.
   param[InChunkErrorPci] = False
   param[GotDoneSoFarPci] = False
   param[WherePci] = None
   param[TopQuitPci] = ''
   param[NumDonePci] = 0
   param[OutFilePci] = None
   param[OutNamePci] = outfile
   param[CheckNamePci] = proChunkCheckFile
   
   comArray = ['processchunks', '-P', '-g', '-c', proChunkCheckFile] + comArray
   message = bkgdProcess(comArray, outfile, 'stdout', True)
   return message


# Checks a processchunks run started with startProcesschunks, opening the output file
# when possible and reading lines from it to assess status.  Also calls 
# checkForProChunksQuit with the passed or default values of lookForPause and
# sendPauseForF.  Prints processchunks output if printOutput is True.
# Returns a 5-tuple:
#    1 for an error or quit, or -1 for a problem starting processchunks, generally with
#        a message
#    -1 for a processchunks ERROR, with message; 1 if finished; 0 if not finished;
#        -2 if processchunks got a pause signal
#    'F' if finish signal received, 'P' if paused signal received and lookForPause is
#        True, 'Q' to quit (which may have a message), '' if nothing
#    number of chunks done
#    a message if any
def checkProChunksLog(topCheckFile, param, lookForPause = False, sendPauseForF = False,
                      printOutput = False):
   finished = 0
   error = 0
   message = ''
   sleepTime = 0.2
   startingTimeOut = 60.
   readErrorTimeout = 30.
   checkInterval = 5.

   try:

      # Seek to the last location before trying again if nothing was gotten.  This is
      # a trick from stackoverflow.com and is needed on Mac
      try:
         if param[WherePci] != None:
            param[OutFilePci].seek(param[WherePci])
      except IOError:
         pass

      # Keep track of whether it opened and whether it read lines without an error
      if not param[OpenedPci]:
         param[OutFilePci] = open(param[OutNamePci], 'r')
         param[OpenedPci] = True
      else:
         param[WherePci] = param[OutFilePci].tell()
         lines = param[OutFilePci].readlines()
         param[ReadOKPci] = True
         if lines:

            # Keep track of last time lines were gotten without error and look for
            # various terminations
            param[GotLinesAtPci] = param[ElapsedPci]
            param[WherePci] = None
            for l in lines:
               if printOutput and param[GotDoneSoFarPci]:
                  prnstr(l.strip())
               if 'DONE SO FAR' in l:
                  param[GotDoneSoFarPci] = True
                  try:
                     param[NumDonePci] = l.split()[0]
                  except ValueError:
                     pass
               if l.startswith('CHUNK ERROR:'):
                  param[InChunkErrorPci] = True
               elif l.startswith('END CHUNK ERROR'):
                  param[InChunkErrorPci] = False
               elif not param[InChunkErrorPci] and l.startswith('ERROR:'):
                  message = 'processchunks ' + l.strip()
                  finished = -1
                  if 'has given processing error 1 times - giving up' not in l:
                     error = 1
                  break
               elif l.startswith('Finished reassembling'):
                  finished = 1
                  break
               elif 'retain' in l and 'existing' in l:
                  finished = -2
                  break
               elif 'PID:' in l:
                  param[GotPIDPci] = True

   except IOError:
      param[ReadOKPci] = False
      pass

   if finished:
      pass

   # Check for various timeouts and quit
   elif not param[OpenedPci] and param[ElapsedPci] > startingTimeOut:
      message = 'Timeout occurred before processchunks output file could be ' +\
          'opened for monitoring'
      error = 1

   elif param[ReadOKPci] and param[GotLinesAtPci] < 0 and \
        param[ElapsedPci] > startingTimeOut:
      message = 'Timeout occurred before processchunks started'
      error = 1

   elif param[ReadOKPci] and not param[GotPIDPci] and \
        param[ElapsedPci] > startingTimeOut:
      message = 'Processchunks apparently failed to run; check ' + \
                  imodAbsPath(param[OutNamePci]) + ' for messages'
      error = 1

   # If can't get the output, tell processchunks to quit
   elif param[OpenedPci] and not param[ReadOKPci] and \
          param[ElapsedPci] - param[GotLinesAtPci] > readErrorTimeout:
      writeTextFile(proChunkCheckFile, ['Q'], True)
      time.sleep(5.)
      message = 'Unable to read processchunks output file without an error'
      error = 1

   # Check for Q ourselves in case processchunks is lost
   elif param[ElapsedPci] - param[CheckTimePci] > checkInterval:
      param[CheckTimePci] = param[ElapsedPci]
      param[TopQuitPci] = checkForProChunksQuit(topCheckFile, param[CheckNamePci],
                                                lookForPause, sendPauseForF)
      if param[TopQuitPci] == 'Q':
         error = 1

   if param[OpenedPci] and (error or finished):
      try:
         param[OutFilePci].close()
      except Exception:
         pass

   return (error, finished, param[TopQuitPci], param[NumDonePci], message)


# Runs processchunks in the background with options given in comArray and output to
# outfile.  Periodically checks topCheckFile for quit and finish signals, calling
# checkForProChunksQuit with the passed or default values of lookForPause and
# sendPauseForF.  Prints processchunks output if printOutput is True
# Adds 'processchunks', '-P', '-g', '-c', and proChunkCheckFile to the front of the
# comArray.  Catches a keyboard interrupt and sends a quit to processchunks.
# Returns a 5-tuple:
#    1 for an error or quit, or -1 for a problem starting processchunks, generally with
#        a message
#    -1 for a processchunks ERROR, with message; 1 if finished; 0 if not finished;
#        -2 if processchunks got a pause signal
#    'F' if finish signal received, 'P' if paused signal received and lookForPause is
#        True, 'Q' to quit (which may have a message), '' if nothing
#    number of chunks done
#    a message if any
def runProcesschunks(comArray, outfile, topCheckFile, proChunkCheckFile,
                     lookForPause = False, sendPauseForF = False, printOutput = False):
   sleepTime = 0.2
   param = [0] * 16
   message = startProcesschunks(comArray, outfile, proChunkCheckFile, param)
   if message:
      return (-1, 0, 0, numDone, message)
   try:
      while True:
         (error, finished, topQuit, numDone, 
          message) = checkProChunksLog(topCheckFile, param, lookForPause, sendPauseForF,
                                       printOutput)
         if error or finished:
            break
         param[ElapsedPci] += sleepTime
         time.sleep(sleepTime)

   except KeyboardInterrupt:
      writeTextFile(proChunkCheckFile, ['Q'], True)
      topQuit = 'Q'
      message = 'Detected keyboard interrupt, told processchunks to quit'
      error = 1

   return (error, finished, topQuit, numDone, message)


# Translates a remote directory entry to processchunks in a command file, where it is
# appropriate for the command file's location, to the entry that is needed in the location
# where it will be used.  remoteStartDir is the original entry, startingDir is the 
# directory where that is valid, and datasetDir is the directory where it will get used
# Returns the modified directory
def transferRemoteDirectory(remoteStartDir, startingDir, datasetDir):
   absDataDir = imodAbsPath(datasetDir).rstrip('/\\')
   absStartDir = imodAbsPath(startingDir).rstrip('/\\')
   prefix = os.path.commonprefix([absStartDir, absDataDir])
   remoteDataDir = ''
   errMess = ''
   if absDataDir == absStartDir:
      remoteDataDir = remoteStartDir
   elif prefix and absDataDir.startswith(prefix) and absStartDir.startswith(prefix):
      if prefix == absDataDir:
         startRemnant = absStartDir[len(prefix):]
         ind = remoteStartDir.find(startRemnant)
         if ind > 0:
            remoteDataDir = remoteStartDir[0:ind];
      else:
         dataRemnant = absDataDir[len(prefix):]
         if dataRemnant.startswith('/') or dataRemnant.startswith('\\'):
            dataRemnant = dataRemnant[1:]
         if prefix == absStartDir:
            remoteDataDir = os.path.normpath(os.path.join(remoteStartDir, dataRemnant))
         else:
            startRemnant = absStartDir[len(prefix):]
            ind = remoteStartDir.find(startRemnant)
            if ind > 0:
               remoteDataDir = os.path.normpath(os.path.join(remoteStartDir[0:ind], \
                                                             dataRemnant))
         
   if not remoteDataDir:
      if prefix:
         errMess = fmtstr('Cannot translate remote directory entry {} to work ' +\
                          'with {} - common prefix of {} and {} is {} , ' + \
                          'remnant of {} is not in remote dir',remoteStartDir, datasetDir,
                          absStartDir, absDataDir, prefix, startRemnant)
      else:
         errMess = fmtstr('Cannot translate remote directory entry {} to work ' + \
                          'with {} - there is no common prefix of {} and {}', 
                          remoteStartDir, datasetDir, absStartDir, absDataDir)

   return (remoteDataDir, errMess)
