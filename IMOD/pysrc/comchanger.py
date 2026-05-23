#!/usr/bin/python
# comchanger.py module
# Functions for changing com files, and a template-related function
#
# Author: David Mastronarde
#
#  $Id$

import re
from imodpy import *
from pysed import *
from pip import *

# Modify a set of command lines for relevant changes in a potentially large changeList
# comlines are the lines, comRoot is the root of the com file name exclusive of axis
# axisLet is a, b, or empty.
def modifyForChangeList(comlines, comRoot, axisLet, changeList, returnOnErr = False):

   # Analyze the lines for command blocks
   procStarts = []
   procMatch = re.compile('^ *\$ *(\w+).*$')
   for ind in range(len(comlines)):
      line = comlines[ind]
      if re.search(procMatch, line):
         process = re.sub(procMatch, r'\1', line)
         procStarts.append((ind, process))
   numProcs = len(procStarts)
   procStarts.append((len(comlines), ''))

   # Start output with any lines up to the first command
   outlines = []
   if procStarts[0][0]:
      outlines += comlines[0:procStarts[0][0]]

   # Loop on the processes, for each one, run through the change lines and compile the
   # list of relevant changes
   for ind in range(numProcs):
      proclines = comlines[procStarts[ind][0]: procStarts[ind + 1][0]]
      process = procStarts[ind][1]
      if process == 'if':
         outlines += proclines
         continue
      
      comAxis = comRoot + axisLet
      procChanges = []
      for change in changeList:
         if (change[0] == comAxis or change[0] == comRoot) and change[1] == process:
            for ichan in range(len(procChanges)):
               if procChanges[ichan][2] == change[2]:

                  # If single axis, ignore a change that is B-specific
                  if axisLet == '' and change[0] == comRoot + 'b':
                     break
                  
                  # Otherwise, replace with a later entry unless previous one was
                  # axis-specific while this one is generic
                  if axisLet == '' or not \
                         (procChanges[ichan][0] == comAxis and change[0] == comRoot):
                     procChanges[ichan] = change
                  break
            else:

               # If the loop does not break by replacement or ignoring, add the change
               procChanges.append(change)


      # build a sed command
      sedcom = []
      if procChanges:
         for change in procChanges:

            # Modify an existing line or append a new one after the process command
            # if there is a value
            if change[3] != '':
               for line in proclines:
                  if line.startswith(change[2]):
                     sedcom.append(fmtstr('|^{}|s|[ 	].*|	{}|', change[2],
                                           change[3]))
                     break
               else:
                  sedcom.append(fmtstr('|^ *$ *{}|a|{}	{}|', process, change[2],
                                        change[3]))

            else:

               # Or delete the line if there is no value
               sedcom.append('|^' + change[2] + '|d')

         tmplines = pysed(sedcom, proclines, delim = '|', retErr = returnOnErr)
         if isinstance(tmplines, str):
            return tmplines
         outlines += tmplines

      else:
          outlines += proclines

   return outlines


# Add one change to the list with the given prefix and number of components, which
# can be negative to just skip ones without that number of components
# Store a change as a list of all components but the first and the value, which
# can be blank unless valNeed is true
def changeToAddToList(changeList, line, changeFile, prefix, numComponents, \
                         valNeeded = False):
   lsplit = line.split('=')
   keysplit = lsplit[0].split('.')
   errtext = 'entry: '
   allowOtherNums = numComponents < 0
   if allowOtherNums:
      numComponents *= -1
   if changeFile:
      errtext = 'line from ' + changeFile + ': '
   if keysplit[0] == prefix:
      if len(lsplit) < 2:
         exitError('Missing = sign in ' + errtext + line)
      if valNeeded and lsplit[1].strip() == '':
         exitError('Empty value in ' + errtext + line)
      if not (allowOtherNums and len(keysplit) != numComponents):
         if len(keysplit) < numComponents:
            exitError('Not enough components in ' + errtext + line)
         change = []
         for comp in keysplit[1:numComponents]:
            change.append(comp.strip())
         change.append(lsplit[1].strip())
         changeList.append(change)


# Need to get the filenames and one change entries once, there is no way to reset PIP
fileEntries = []
oneOptEntries = []

# Process changes from two kinds of options, specified by fileOption for a file,
# and oneChangeOpt for an entry of one change, and look for changes starting with prefix
# numComponents can be negative to skip directives have different numbers of components
def processChangeOptions(fileOption, oneChangeOpt, prefix, numComponents = 4,
                         valNeeded = False):
   global fileEntries, oneOptEntries
   changeList = []
   if fileOption and not fileEntries:
      numChangers = PipNumberOfEntries(fileOption)
      for chan in range(numChangers):
         changeFile = PipGetString(fileOption, '')
         fileEntries.append(changeFile)

   if oneChangeOpt and not oneOptEntries:
      numChangers = PipNumberOfEntries(oneChangeOpt)
      for chan in range(numChangers):
         line = PipGetString(oneChangeOpt, '')
         oneOptEntries.append(line)

   if fileOption:
      numChangers = len(fileEntries)
      for chan in range(numChangers):
         changeFile = fileEntries[chan]
         changeLines = readTextFile(changeFile)
         for line in changeLines:
            changeToAddToList(changeList, line, changeFile, prefix, numComponents,
                              valNeeded)

   if oneChangeOpt:
      numChangers = len(oneOptEntries)
      for chan in range(numChangers):
         line = oneOptEntries[chan]
         changeToAddToList(changeList, line, '', prefix, numComponents, valNeeded)

   return changeList


# Simple function to look up a setupset directive value
def getSetupsetValue(changeList, prefix, option):
   value = None
   for change in changeList:
      if prefix == change[0] and option == change[1]:
         value = change[2]

   return value


# Return an absolute path to a template file of given type (0 for scope, 1 for system,
# 2 for user) plus an error code and updated value of userTemplateDir and an error 
# message.  If the path is already absolute, it returns that and 0; otherwise if it 
# is not just a filename without directories, it returns None and -1; otherwise it looks 
# for it in the proper places and returns the path and 1, or None and -2 if it cannot 
# be found
def absTemplatePath(template, index, userTemplateDir, errorName):
   errMess = errorName + ' file ' + template + ' not found in expected location'
   if imodIsAbsPath(template):
      return (template, 0, userTemplateDir, "")
   if os.path.dirname(template):
      return (None, -1, userTemplateDir, 'Directive for ' + errorName + \
              ' name must be either an absolute path or just a filename, it is ' + \
              template)

   # For scope template, just look up in the ImodCalib
   errval = (None, -2, userTemplateDir, errMess)
   if index == 0:
      if 'IMOD_CALIB_DIR' not in os.environ:
         return errval
      fullPath = os.path.join(os.environ['IMOD_CALIB_DIR'], 'ScopeTemplate', template)
      if os.path.exists(fullPath):
         return (fullPath, 1, userTemplateDir, '')
      return errval

   # For system template, look it up in ImodCalib then in IMOD
   if index == 1:
      for rootdir in ('IMOD_CALIB_DIR', 'IMOD_DIR'):
         if rootdir in os.environ:
            fullPath = os.path.join(os.environ[rootdir], 'SystemTemplate',
                                    template)
            if os.path.exists(fullPath):
               return (fullPath, 1, userTemplateDir, '')
      return errval

   # For user template, we need to know the directory from .etomo if not the default
   if not userTemplateDir:
      if 'HOME' not in os.environ:
         return errval
      dirpath = os.path.join(os.environ['HOME'], '.etomotemplate')
      if os.path.exists(dirpath):
         userTemplateDir = dirpath
      dotEtomo = os.path.join(os.environ['HOME'], '.etomo')
      if os.path.exists(dotEtomo):
         etomoLines = readTextFile(dotEtomo, None, True)
         if not isinstance(etomoLines, str) and len(etomoLines) > 0:
            defsTempl = optionValue(etomoLines, 'Defaults.UserTemplateDir', STRING_VALUE,
                                    otherSep = '=')
            if defsTempl:
               userTemplateDir = defsTempl

   # Now given the directory, look up the user template file
   errval = (None, -2, userTemplateDir, errMess)
   if not userTemplateDir or not os.path.exists(userTemplateDir):
      return errval
   fullPath = os.path.join(userTemplateDir, template)
   if os.path.exists(fullPath):
      return (fullPath, 1, userTemplateDir, '')
   return errval


