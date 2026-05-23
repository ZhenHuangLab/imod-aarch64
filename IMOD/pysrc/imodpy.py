#!/usr/bin/python3
# imodpy.py module
#
# Authors: Tor Mohling and David Mastronarde
#
# $Id$
#

# Current system defaults
stdTypeExtensions = ['', 'mrc', 'hdf']
extraTypeExtensions = ['tif']
dfltComExtension = 'com'
dfltTypeExtension = 'mrc'
dfltNameStyle = 1
 
"""A collection of useful functions for use in IMOD scripts

This module provides the following functions:
  runcmd(cmd[, input][,outfile][, inStderr]) - spawn a command with optional input,
                                   return its output or print to stdout or a file
  bkgdProcess(commandArr, [outfile] [, errfile] [, returnOnErr]) - Run a process
                                    in background with optional redirection
  getmrcsize(file)     - run the 'header' command on <file>.
                         returns a triple of x,y,z size integers
  getmrc(file[, doAll][, angleValues])  - run the 'header' command on <file>.
                         returns a 'tuple' of x,y,z,mode,px,py,pz,
                         plus ox,oy,oz,min,max,mean if doAll is True
                         or returns tilt axis angle, binning, spot, camera, and bidir 
                         angle if angleValues is True (overrides doAll)
  getmrcpixel(file)    - run the 'header' command on <file>
                         returns just a single pixel size, using extended header
                         value if any
  getMontageSize(stack, [plName]) - runs 'montagesize' on <stack>; adds piece 
                                     list file plName if supplied and it exists; 
                                     returns nx, ny, nz in a tuple
  runGoodframe(nx, ny) - runs goodframe with nx and ny, returns two sizes or (-1, -1)
                            or (-2, -2) for error
  getImageFormat(file) - runs header to determine format of <file>; returns
                            TIFF, HDF, likeMRC, or MRC
  datasetFilename(suffix [, root] [, typext]) - makes filename from root and the suffix 
                                                modified with a file type extension if any
  setRootAndExtension(root, extension) - sets default rootname and file type extension to 
                                         use in datasetFilename
  findRootAxisAndExtensions([forceSingle]) - Determines data set root name, single/dual
                                             axis, file extension type, and raw stack 
                                             and com file extensions from tilt/eraser coms
  getNamingStyle([default][, require[ [, allowExtra]) - Reads NamingStyle option and 
                            returns a duple of an integer style value and extension string
  defaultNamingStyle()     - Returns default naming style and type extension
  comExtensionFromOption(optDflt)  - Reads MakeComExtensionPcm option with given default 
                                     value and returns a com extension WITH dot
  defaultComExtension()    - Returns the default com file extension WITHOUT dot
  standardTypeExtensions() - Returns an array with allowed standard type extensions
  getTypeExtAllowingExtra(nameStyle) - Returns a type extension for the given name style
  allowedRawStackExtensions()  - Returns array with standard types plus st, tif, tiff
  mapTypeExtensionToStyle(typeExt[, allowExtra])  - Returns a naming style value 
                                                 corresponding to the given type extension
  addOutputFormatVarToLines(lines, nameStyle[, outputFormat][, allowExtra]) 
                                   - Adds a com file line to set IMOD_OUTPUT_FORMAT 
                                     from naming style or format
  setOutputFormatIfNeeded(typeExtension[, allowExtra]) - Sets IMOD_OUTPUT_FORMAT
                                     environment variable from extension or if needed
  makeBackupFile(file)   - renames file to file~, deleting old file~
  exitFromImodError(pn, errout) - prints the error strings in errout and
                                  prepends 'ERROR: pn - ' to the last one
  parselist(line)      -  convert list entry in <line> into list of integers
  readTextFile(filename[ , descrip] [, returnOnErr] [, maxLines) - reads in text file,
                                    strips line endings, returns a list of strings
  writeTextFile(filename, strings [, returnOnErr]) - writes set of strings to a text file
  optionValue(linelist, option, type, ignorecase = False, numVal = 0, otherSep = None) -
                                           finds option value in list of lines
  convertToInteger(valstr, description) - Convert string to int with error 
                                           message if it fails
  completeAndCheckComFile(comfile) - returns complete com file name and root
  cleanChunkFiles(rootname[, logOnly]) - cleans up log and com files from chunks
  cleanupFiles(files) - Removes a list of files with multiple trials if it fails
  extractProgramEntries(comLines, progName, startKey) - extract entries to a program
                                                        from command file lines
  getCygpath(windows, path) - Returns path, or Windows path if windows is not None
  cygwinPath(path)          - Returns path, or a Cygwin path if running in Cygwin

  addIMODbinIgnoreSIGHUP()  - Adds IMOD_DIR/bin to front of PATH and ignores SIGHUP
  printPID(doPrint)   - Prints a PID with system-dependent prefix if doPrint is true
  getIMODversion()    - Returns the IMOD version as a string, or None for error
  imodIsAbsPath(path) - Tests whether the path is an absolute path (works in Cygwin)
  imodAbsPath(path) - Returns absolute path, converted to windows format if on Windows
  newPsutilAPI(version)  - Returns True if psutil is a version with the new API
  imodNice(niceInc) - Sets niceness of process, even on Windows
  imodTempDir() - returns a temporary directory: IMOD_TEMPDIR, /usr/tmp, or /tmp
  setLibPath() - Set path variables for executing Qt programs
  makeCurrentDirWritable([subdir]) - Tries to make sure current directory or given
                               subdirectory is writeable on Windows
  patchSizeFromEntry(patchin) - Gets 3D patch size from letter or three numbers
  autoPatchNumber(size, lower, upper, ifZ, [densityInd]) - Computes number of patches
                               from size and range, depending on axis and density flags
  parallelBoundarySize([default]) - returns size of parallel writing boundary region
                               based on the built-in defaults or an environment variable
  elapsedTimeComponents(startTime) - returns a tuple with minutes, seconds, and tenth of
                                     second since the startTime
  multiCharSplit(line, chars) - Splits line on all characters in chars
  moveOrCopyWithRetry(fromFile, toDir, mess, ifCopy, numTrials) - Moves or copies a file
                                or directory to a directory, even across filesystems
  initializeMoveOrCopy()      - Initializes global variables for moveOrCopyWithRetry
  fmtstr(string, *args) - formats a string with replacement fields
  prnstr(string, file = sys.stdout, end = '\n', flush = False) - replaces print function
"""

# other modules needed by imodpy
import sys, os, re, time, glob, signal, stat, shlex, copy, shutil
from pip import exitError, PipGetInteger

pyVersion = 100 * sys.version_info[0] + 10 * sys.version_info[1]

if pyVersion < 300:
   import exceptions

# Variables used like an enum for selecting optionValue type
STRING_VALUE, INT_VALUE, FLOAT_VALUE, BOOL_VALUE = 0, 1, 2, 3

# The global place to stash error strings and last exit status
errStrings = []
errStatus = 0
runRetryLimit = 10
runMaxTimeForRetry = 0.5
raiseKeyInterrupt = False
finishSetAndQuit = False
fileTypeExtension = ''
currentRootname = ''
numStdTypeExts = len(stdTypeExtensions)
mocRecursiveCopyOK = True
mocRenamesOK = True
mocUseXcopy = False
mocXcopyExists = False

# Use the subprocess module if available except on cygwin where broken
# pipes (early on) occurred occasionally; require it on win32
# popen2 is deprecated in 2.6 onward, and subprocess seems to work now in
# cygwin, so try using it.
useSubprocess = True
if (sys.platform.find('cygwin') < 0 and pyVersion >= 240) or pyVersion >= 260:
   from subprocess import *
else:
   useSubprocess = False
   if sys.platform.find('win32') >= 0:
      prnstr("ERROR: imodpy - Windows Python must be at least version 2.4")
      sys.exit(1)


# define a new exception class so our caller can more easily catch errors
# raised here
class ImodpyError(Exception):
   def __init__(self, args=[]):
      self.args = args
#   def __str__(self):
#      return repr(self.args)


# Use this call to get the error strings from the exception
def getErrStrings():
   return errStrings


# output_lines = imodpy.runcmd(cmd, input_lines, output_file)
#  output_lines, input_lines are ARRAYs; output_lines is None if output_file
# not None
#  cmd is a STRING
#
def runcmd(cmd, input=None, outfile=None, inStderr = None, ignoreStatus = 0):
   """runcmd(cmd[, input][, outfile][, inStderr])
       - spawn a command with optional input, either send its output to
       outfile or return its output in an array of strings.
       cmd is a string; input is an array; outfile is a file object or
       the string 'stdout' to send output to standard out.
       inStderr specifies the destination of standard error output, but it is
       ignored if outfile is 'stdout'.  It can be either a file object, 'stdout'
       for it to be combined with standard output, or 'pipe' for it to be neither
       returned nor written into a file (it will either be lost or appear on the
       terminal depending on whether the subprocess module is being used).
       If the command fails with a broken pipe within 0.5 second,
       it will retry up to 10 times.  Call setRetryLimit() to modify the allowed
       number of retries and the maximum run time for a failure to be retried.
       ignoreStatus can be either an int or a list of error status values that 
       can be ignored"""

   global errStrings, errStatus

   cmd = avoidLocalComFile(cmd)

   # Set up flags for whether to collect output or send to stderr
   errStatus = 0
   collect = 1
   toStdout = 0
   output = None
   verbose = os.getenv('RUNCMD_VERBOSE') == '1'
   if verbose:
      prnstr('+++++++++++++++++++++++++')
      prnstr('   runcmd running command:')
      prnstr(cmd)
      if input:
         prnstr('   With input:')
         for l in input:
            prnstr(l)
      sys.stdout.flush()

   if outfile:
      collect = 0
      if isinstance(outfile, str) and outfile == 'stdout':
         toStdout = 1

   retryCount = 0
   while retryCount <= runRetryLimit:
      tryStart = time.time();
      try:
         if useSubprocess:

            # The subprocess interface: input must be all one string
            if input:
               if isinstance(input, list):
                  joined = ''
                  for l in input:
                     joined += l + '\n'
                  input = joined
            else:
               input = None

            if input and pyVersion >= 300 and retryCount == 0:
               input = input.encode()

            if inStderr and isinstance(inStderr, str):
               if inStderr == 'stdout':
                  inStderr = STDOUT
               elif inStderr == 'pipe':
                  inStderr = PIPE

            # Run it three different ways depending on where output goes
            if collect:
               p = Popen(cmd, shell=True, stdout=PIPE, stdin=PIPE, stderr=inStderr)
               kout, kerr = p.communicate(input)
               if pyVersion >= 300:
                  kout = kout.decode(errors='replace')
               if kout:
                  output = kout.splitlines(True)
            elif toStdout:
               p = Popen(cmd, shell=True, stdin=PIPE)
               p.communicate(input)
            else:
               p = Popen(cmd, shell=True, stdout=outfile, stdin=PIPE, stderr=inStderr)
               p.communicate(input)
            ec = p.returncode
            if ec:
               errStatus = ec
            
         else:

            # The old way, use popen, popen2-4 depending on where the output
            # is going
            if toStdout:
               kin = os.popen(cmd, 'w')
               if input:
                  for l in input:
                     prnstr(l, file=kin)
               ec = kin.close()
            else:
               errToFile =inStderr and isinstance(inStderr, file)
               if errToFile:
                  (kin, kout, kerr) = os.popen3(cmd)
               elif inStderr and isinstance(inStderr, str) and inStderr == 'stdout':
                  (kin, kout) = os.popen4(cmd)
               else:
                  (kin, kout) = os.popen2(cmd)
               if input:
                  for l in input:
                     prnstr(l, file=kin)
               kin.close()
               output = kout.readlines()
               kout.close()
               if errToFile:
                  errlines = kerr.readlines()
                  kerr.close()
               kpid, ec = os.wait()
               if not collect and output:
                  outfile.writelines(output)
               if errToFile:
                  inStderr.writelines(errlines)

            # Error status is high byte of os.wait() return value
            if ec:
               errStatus = ec >> 8

         # If it succeeds, break the retry loop
         break

      except KeyboardInterrupt:
         if raiseKeyInterrupt:
            raise KeyboardInterrupt
         sys.exit(1)
      
      except Exception:

         # If it gets a broken pipe after a short enough time,
         # run another trial.  Sleep a bit after trying twice, it helps break the cycle
         # This problem is observed on MAC OSX with commands that take < 0.02 sec,
         # originally with collected output, later with output to file (so what pipe?)
         if str(sys.exc_info()[1]).find('Broken pipe') >= 0 and \
            retryCount < runRetryLimit and time.time() - tryStart < runMaxTimeForRetry:
            retryCount += 1
            if collect:
               prnstr(fmtstr("Retrying " + cmd + " after {:.3f} sec",
                             time.time() - tryStart))
            elif not toStdout and isinstance(outfile, file):
               outfile.truncate()
            if inStderr and useSubprocess and isinstance(inStderr, file):
               inStderr.truncate()
            if retryCount > 1:
               time.sleep(0.1)
         else:
            errStrings = ["command " + cmd + ": " + str(sys.exc_info()[1]) + "\n"]
            raise ImodpyError(errStrings)
      
   if verbose:
      if output:
         prnstr('    Output:')
         for l in output:
            prnstr(l, end='')
      prnstr('-------------------------', flush = True)
      
   if ec and not ((isinstance(ignoreStatus, int) and errStatus == ignoreStatus) or \
              (isinstance(ignoreStatus, list) and errStatus in ignoreStatus)):

      # look thru the output for 'ERROR' line(s) and put them before this
      errstr = cmd + fmtstr(": exited with status {}\n", errStatus)
      errStrings = []
      if collect and output:
         errStrings = [l for l in output
               if l.find('ERROR:') >= 0]
      errStrings.append(errstr)
      raise ImodpyError

   if collect:
      return output
   return None


# Modify the retry limits if necessary
def setRetryLimit(numRetries, maxTime = None):
   """setRetryLimit(numRetries, [maxTime]) - Set max number of retries for runcmd
   with collected output to numRetries (set to 0 to disable retries), and set the
   maximum time after which it will retry to maxTime.
   """
   global runRetryLimit, runMaxTimeForRetry
   runRetryLimit = max(0, numRetries)
   if maxTime:
      runMaxTimeForRetry = maxTime


# Us this call to control whether ctrl C interrupting runcmd will raise an interrupt
def passOnKeyInterrupt(passOn = True):
   global raiseKeyInterrupt
   raiseKeyInterrupt = passOn
   

# Get exit status of last command that was run
def getLastExitStatus():
   return errStatus


# Run a process in background with optional redirection of all output
def bkgdProcess(commandArr, outfile=None, errfile='stdout', returnOnErr = False):
   """bkgdProcess(commandArr, [outfile] [, errfile]) - Run a process in background
   with optional redirection of standard output to a file, and of standard error
   to a separate file or to standard out by default.
   """
   try:

      # If subprocess is allowed, open the files if any
      if useSubprocess:
         outf = None
         errf = None
         errToFile = False
         if errfile == 'stdout':
            errf = STDOUT
         if outfile:
            action = 'Opening ' + outfile + ' for output'
            outf = open(outfile, 'w')
            errToFile = errfile == 'stdout'
         if errfile and errfile != 'stdout':
            action = 'Opening ' + errfile + ' for output'
            errf = open(errfile, 'w')
            errToFile = True

         # Use detached flag on Windows, although it may not be needed
         # In fact, unless stderr is going to a file it keeps it from running there
         action = 'Starting background process ' + commandArr[0]
         if sys.platform.find('win32') >= 0 and errToFile:
            DETACHED_PROCESS = 0x00000008
            Popen(commandArr, shell=False, stdout=outf, stderr=errf,
                  creationflags=DETACHED_PROCESS)
         else:
            Popen(commandArr, shell=False, stdout=outf, stderr=errf)
         return None

      # Otherwise use system call: wrap all args in quotes and add the redirects
      comstr = commandArr[0]
      action = 'Starting background process ' + commandArr[0]
      for i in range(1, len(commandArr)):
         comstr += ' "' + commandArr[i] + '"'
      if outfile:
         comstr += ' > "' + outfile + '"'
      if errfile == 'stdout':
         comstr += ' 2>&1'
      elif errfile:
         comstr += ' 2> "' + errfile + '"'
      comstr += ' &'

      # The return value is not useful when it fails to run
      os.system(comstr)
      return None
         
   except Exception:
      errString = action + "  - " + str(sys.exc_info()[1])
      if returnOnErr:
         return errString
      exitError(errString)


# Does a multi-character split on line with the given characters, which will be enclosed
# in [] for a regular expression.  Escape -, [, ] if it is to be taken as a separator
def multiCharSplit(line, chars):
   return filter(None, re.split('[' + chars + ']', line))


# Get essential data from the header of an MRC file
def getmrc(file, doAll = False, angleLineValues = False):
   """getmrc(file)     - run the 'header' command on <file>
    
    Returns a tuple with seven elements (x,y,z,mode,px,py,pz) by default
    (x,y,z are size in int, mode is int, px,py,pz are pixelsize in float).
    If angleLineValues is True, returns a list with 5 elements, each being
    None if not available: [axis angle, binning, spot, camera, bidir angle]
    Otherwise if doAll is True, returns a tuple with 13 elements:
    (x,y,z,mode,px,py,pz,ox,oy,oz,dmin,dmax,dmean)  where the added elements are
    origin in x, y, z and min, max and mean, all in floats"""

   global errStrings

   input = ["InputFile " + file]
   if angleLineValues:
      retval = [None, None, None, None, None]
      keys = ('angle', 'binning', 'spot', 'camera', 'bidir')
      types = (1, 1, 0, 0, 1)
      hdrout = runcmd("header -StandardInput", input)
      for line in hdrout:
         if 'axis' in line.lower() and 'angle' in line:

            # Here is  good way to split on multiple characters; filter removes empty
            # strings from the separators
            tokens = multiCharSplit(line, ' ,=')
            for ind in range(len(tokens) - 1):
               for keyInd in range(len(keys)):
                  if tokens[ind] == keys[keyInd]:
                     try:
                        if (types[keyInd]):
                           retval[keyInd] = float(tokens[ind + 1])
                        else:
                           retval[keyInd] = int(tokens[ind + 1])
                     except ValueError:
                        errStrings = ["header " + file + ": Error converting value to " +\
                                      "integer or float in title " + line + "\n"]
                        raise ImodpyError(errStrings)

      return retval
            
   if doAll:
      hdrout = runcmd("header -si -mo -pi -ori -min -max -mean -StandardInput", input)
      needed = 7
   else:
      hdrout = runcmd("header -si -mo -pi -StandardInput", input)
      needed = 3

   # Eat any lines with PIP fallback output
   while len(hdrout) >= needed:
      if hdrout[0].strip() == '' or 'a' in hdrout[0] or 'e' in hdrout[0] or \
         'i' in hdrout[0] or 'o' in hdrout[0]:
         hdrout.pop(0)
      else:
         break

   if len(hdrout) < needed:
      errStrings = ["header " + file + ": too few lines of output\n"]
      raise ImodpyError(errStrings)

   nxyz = hdrout[0].split()
   pxyz = hdrout[2].split()
   if doAll:
      orixyz = hdrout[3].split()
   if len(nxyz) < 3 or len(pxyz) < 3 or (doAll and len(orixyz) < 3):
      if len(nxyz) < 3:
         badLine = '-si option: ' + hdrout[0].strip()
      elif  len(pxyz) < 3:
         badLine = '-pi option: ' + hdrout[2].strip()
      else:
         badLine = '-ori option: ' + hdrout[3].strip()
      errStrings = ["header " + file + ": too few numbers on line for " + badLine + "\n"]
      raise ImodpyError(errStrings)
   ix = int(nxyz[0])
   iy = int(nxyz[1])
   iz = int(nxyz[2])
   mode = int(hdrout[1])
   px = float(pxyz[0])
   py = float(pxyz[1])
   pz = float(pxyz[2])

   if not doAll:
      return (ix,iy,iz,mode,px,py,pz)

   orix = float(orixyz[0])
   oriy = float(orixyz[1])
   oriz = float(orixyz[2])
   minv = float(hdrout[4])
   maxv = float(hdrout[5])
   meanv = float(hdrout[6])
   return (ix,iy,iz,mode,px,py,pz,orix,oriy,oriz,minv,maxv,meanv)


# Get size from an MRC file
def getmrcsize(file):
   """getmrcsize(file)    - run the 'header' command on <file>
   
   Returns a triple of x,y,z size integers."""
   (ix,iy,iz,mode,px,py,pz) = getmrc(file)
   return(ix,iy,iz)

# Get one pixel size from an MRC file, based on FEI/Agard value if present
def getmrcpixel(file):
   """getmrcpixel(file)    - run the 'header' command on <file>
   Returns just a single pixel size, based on value in extended header if appropriate"""

   global errStrings
   input = ["InputFile " + file]
   hdrout = runcmd("header -StandardInput", input)
   pixel = -1.
   for line in hdrout:
      try:
         if 'Pixel spacing' in line:
            dotInd = line.find('.. ') + 2
            if dotInd < 3:
               errStrings = ["header " + file + ": cannot find pixel sizes"]
               raise ImodpyError(errStrings)
            lsplit = line[dotInd:].strip().split()
            if len(lsplit) < 3:
               errStrings = ["header " + file + ": pixel sizes not interpretable"]
               raise ImodpyError(errStrings)
            pixel = float(lsplit[0])

         if 'size in nanometers =' in line:
            ind = line.find('=') + 1
            lsplit = line[ind:].strip().split()
            if len(lsplit):
               pixel = 10. * float(lsplit[0])

      except ValueError:
         errStrings = ["header " + file + ": error converting pixel size to float"]
         raise ImodpyError(errStrings)

   if pixel < 0:
      errStrings = ["header " + file + ": cannot find pixel size"]
      raise ImodpyError(errStrings)

   return pixel


# Get the size of a montage by running montagesize
def getMontageSize(stack, plName = None):
   """getMontageSize(stack, [plName])  - runs 'montagesize' on <stack>; adds
   piece list file plName if supplied and it exists; returns nx, ny, nz in a tuple"""
   global errStrings
   command = 'montagesize "' + stack + '"'
   if plName and os.path.exists(plName):
      command += ' "' + plName + '"'
   sizeLines = runcmd(command)
   try:
      problem = 'No output returned'
      line = sizeLines[-1]
      ind = line.find('NZ:')
      if ind < 0:
         problem = 'Line with NZ: not found in output'
         ind = -5
      lsplit = line[ind + 3:].split()
      problem = 'Uninterpretable output on line with NZ:'
      rawXsize = int(lsplit[0])
      rawYsize = int(lsplit[1])
      zsize = int(lsplit[2])
      return (rawXsize, rawYsize, zsize)
   except Exception:
      errStrings = command + ': ' + problem
      raise ImodpyError


# Runs goodframe with the two sizes, nx and ny, and returns the two FFT-compatible sizes
# or (-1, -1) for an error running goodframe, or (-2, -2) for an error in the output
def runGoodframe(nx, ny):
   try:
      goodout = runcmd(fmtstr('goodframe {} {}', nx, ny))
      gsplit = goodout[len(goodout) - 1].split()
      gfnx = int(gsplit[0])
      gfny = int(gsplit[1])
      return (gfnx, gfny)

   except ImodpyError:
      return (-1, -1)
   except Exception:
      return (-2, -2)


# Gets the image format as MRC, HDF, TIFF, or likeMRC
def getImageFormat(file):
   """getImageFormat(file)   - runs header and returns TIFF, HDF, likeMRC, or MRC"""
   retVals = ['TIFF', 'HDF', 'likeMRC']
   nonMRC = ['a TIFF', 'an HDF', 'a non-MRC']
   input = ["InputFile " + file]
   hdrout = runcmd("header -StandardInput", input)
   for ind in range(len(nonMRC)):
      for line in hdrout[0:9]:
         if 'This is ' + nonMRC[ind] in line:
            return retVals[ind]
   return 'MRC'


# Returns a full filename with the root name and suffix if the type extension is empty, 
# or with the last period in the suffix converted to underscore and the type extension 
# attached. Suffix should include a period and the descriptive extension.  The root name
# and type extension default to the values set with setRootAndExtension if root and
# typext are not supplied here; typext should not have a dot
def datasetFilename(suffix, root = None, typext = None):
   if root == None:
      root = currentRootname
   if typext == None:
      typext = fileTypeExtension
   if not typext:
      return root + suffix
   index = suffix.rfind('.')
   if index >= 0:
      suffix = suffix[:index] + '_' + suffix[index + 1:]
   return root + suffix + '.' + typext


# Sets the default rootname and standard type extension to use in datasetFilename,
# with no dot on the extension
def setRootAndExtension(root, extension):
   global fileTypeExtension, currentRootname
   fileTypeExtension = extension
   currentRootname = root


# Determines com file extension, single/dual axis, rootname, extension style, and raw
# stack extension by looking for and reading tilt and eraser command files.  Only 
# single-axis
# files will be sought if forceSingle > 0, or only dual-axis if it is < 0
# Returns a tuple with (com file extension; 2 if dual axis, 0 if not, -1 if undetermined;
# rootname excluding a/b for dual axis; file type extension; raw stack extension)
# All extensions exclude the dot.
# If any string is undetermined an empty string is returned, or None for type extension
# The com extension will be com, pcm or empty, even if a tilt file is passed with a 
# different extension.  But it is guaranteed to be com or pcm if there is passed file
# that was run into completeAndCheckComFile
def findRootAxisAndExtensions(forceSingle = 0, useTilt = None):
   # Find tilt com file under either extension and determine axis state
   root = ''
   typeExt = None
   stackExt = ''
   tiltFile = ''
   dualNum = 0
   tiltSum = [0, 0]
   eraserSum = [0, 0]
   tiltExt = ''
   eraserExt = ''
   tryExt = None

   # Test the passed tilt file: if it is not there or extension is bad, clear the markers
   # if it is there, set the com ex
   if useTilt:
      if os.path.exists(useTilt):
         (tiltRoot, tryExt) = os.path.splitext(useTilt)
         if tryExt in ('.com', '.pcm'):
            tiltExt = tryExt[1:]
         else:
            tryExt = None
      else:
         useTilt = None

   # Check tilt and eraser, single and dual unless forced to do only one
   # If already got extension from passed-in tilt, skip tilt, but otherwise do try to 
   # analyze tilt[ab].com
   for (comExt, ind) in list(zip(('com', 'pcm'), (0, 1))):
      if forceSingle >= 0:
         if not tryExt and os.path.exists('tilt.' + comExt):
            tiltExt = comExt
            tiltSum[ind] += 1
         if os.path.exists('eraser.' + comExt):
            eraserExt = comExt
            eraserSum[ind] += 1
      if forceSingle <= 0:
         if not tryExt and os.path.exists('tilta.' + comExt) and \
            os.path.exists('tiltb.' + comExt):
            tiltExt = comExt
            tiltSum[ind] += 2
         if os.path.exists('erasera.' + comExt) and os.path.exists('eraserb.' + comExt):
            eraserExt = comExt
            eraserSum[ind] += 2

   # Require only com or pcm to appear, require no contamination between single and dual
   # names unless it was forced to be one or the other, but do not require both to exist
   if min(tiltSum) > 0 or min(eraserSum) > 0 or max(tiltSum) > 2 or max(eraserSum) > 2 \
      or (min(max(tiltSum), max(eraserSum)) > 0 and (max(tiltSum) != max(eraserSum) or \
                                                  eraserExt != tiltExt)):
      return ('', -1, '', None, '')

   # Finalize com extension now
   comExt = tiltExt
   if not comExt:
      comExt = eraserExt
   useExt = '.' + comExt
   if max(tiltSum) > 1 or max(eraserSum) > 1:
      dualNum = 2
      useExt = 'a.' + comExt
      
   # Then read the file options from the tilt file
   tiltRootFailed = False
   if tiltExt:
      if not useTilt:
         useTilt = 'tilt' + useExt

      # Tilt output is variable, so just for redundancy in test, read track file too
      tiltLines = readTextFile(useTilt, returnOnErr = True)
      trackLines = readTextFile('track' + useExt, returnOnErr = True)
      badTilt = isinstance(tiltLines, str)
      badTrack = isinstance(trackLines, str)
      if not (badTilt and badTrack):
         inputLine = imageLine = ''
         if not badTilt:
            inputLine = optionValue(tiltLines, 'InputProjections', STRING_VALUE)
            descrip = '.ali'
         if not badTrack:
            imageLine = optionValue(trackLines, 'ImageFile', STRING_VALUE)
            descrip = '.preali'

         # If both read OK and option lines were found, proceed with two-name analysis
         if inputLine and imageLine:
            (imgRoot, imgExt) = os.path.splitext(imageLine)
            (root, inpExt) = os.path.splitext(inputLine)

            # If the extensions are descriptive style, file type extension is blank, 
            # if they match then it is a file-type extension
            if inpExt == '.ali' and imgExt == '.preali':
               typeExt = ''
            elif len(inpExt) > 1 and inpExt == imgExt and root.endswith('ali') and \
                 imgRoot.endswith('preali'):
               typeExt = inpExt[1:]
               root = root[:-4]
               imgRoot = imgRoot[:-7]

            # Make sure the rootname is sensible for dual axis
            if dualNum:
               if root.endswith('a') or root.endswith('b'):
                  root = root[:-1]
                  imgRoot = imgRoot[:-1]
               else:
                  root = ''
                  tiltRootFailed = True

            if root != imgRoot:
               root = ''
               tiltRootFailed = True

         # Otherwise just make sure the entry from one file matches the expected one
         # and set the type extension and root that way
         elif inputLine or imageLine:
            if inputLine:
               (root, inpExt) = os.path.splitext(inputLine)
            else:
               (root, inpExt) = os.path.splitext(imageLine)
            if inpExt == descrip:
               typeExt = ''
            elif len(inpExt) > 1 and root.endswith(descrip[1:]):
               typeExt = inpExt[1:]
               root = root[:-len(descrip)]

            # And rootname must still be good for dual axis
            if dualNum:
               if root.endswith('a') or root.endswith('b'):
                  root = root[:-1]
               else:
                  root = ''
                  tiltRootFailed = True
            

   # Next get raw stack extension from eraser.com
   if eraserExt:
      eraserLines = readTextFile('eraser' + useExt, returnOnErr = True)
      if not isinstance(eraserLines, str):
         inputLine = optionValue(eraserLines, 'InputFile', STRING_VALUE)
         if inputLine:
            (root2, inpExt) = os.path.splitext(inputLine)
            if len(inpExt) > 1:
               stackExt = inpExt[1:]

            # This gives us another shot at the rootname if tilt reading failed
            if not root and tiltRootFailed:
               if dualNum:
                  if root2.endswith('a'):
                     root = root2[:-1]
               else:
                  root = root2

   # Return what was successfully gotten
   return (comExt, dualNum, root, typeExt, stackExt)


# Reads the NamingStyle option; if >= 0 it checks that is is not above the limit and 
# returns the style and type extension; if < 0, it returns that value and the value in 
# default if any, unless require is True, in which case it exits with an error
# If allowExtra is True it will allow extension in the extraTypeExtensions list
def getNamingStyle(default = None, require = False, allowExtra = False):
   typeExt = default
   nameStyle = PipGetInteger('NamingStyle', -1)
   maxVal = numStdTypeExts - 1
   if allowExtra:
      maxVal += len(extraTypeExtensions)
   if nameStyle >= 0:
      if nameStyle > maxVal:
         exitError('Naming style must be less than ' + str(maxVal))
      if nameStyle < numStdTypeExts:
         typeExt = stdTypeExtensions[nameStyle]
      else:
         typeExt = extraTypeExtensions[nameStyle - numStdTypeExts]
   elif default == None and require:
      exitError('Cannot determine file naming style from the ' +\
                'command file; you must enter -name')
   return (nameStyle, typeExt)


# Returns default naming style and type extension
def defaultNamingStyle():
   return (dfltNameStyle, dfltTypeExtension)


# Reads the MakeComExtensionPcm option with default value in optDlft and returns
# the com extension implied by the returned value, or the default if 0, WITH a dot
def comExtensionFromOption(optDflt):
   comExt = '.' + dfltComExtension
   makePcm = PipGetInteger('MakeComExtensionPcm', optDflt)
   if makePcm > 0:
      comExt = '.pcm'
   if makePcm < 0:
      comExt = '.com'
   return comExt


# Returns the default com file extension WITHOUT dot
def defaultComExtension():
   return dfltComExtension


# Returns an array with allowed standard type extensions, starting with an empty string
def standardTypeExtensions():
   return copy.deepcopy(stdTypeExtensions)


# Returns a type extension for the given name style value, including extra ones
def getTypeExtAllowingExtra(nameStyle):
   if nameStyle < 0 or nameStyle > numStdTypeExts + len(extraTypeExtensions):
      return None
   if nameStyle < numStdTypeExts:
      return stdTypeExtensions[nameStyle]
   return extraTypeExtensions[nameStyle - numStdTypeExts]


# Returns an array of allowed raw tilt stack extensions, namely the type extensions with
# 'st' in the 0 position and 'tif' and 'tiff' added
def allowedRawStackExtensions():
   allowed = standardTypeExtensions()
   allowed[0] = 'st'
   allowed += ['tif', 'tiff']
   return allowed


# Returns a style value >= 0 corresponding to the given type extension, or 0 if typeExt
# is not an allowed extension
def mapTypeExtensionToStyle(typeExt, allowExtra = False):
   if not typeExt:
      return 0
   for ind in range(numStdTypeExts):
      if typeExt == stdTypeExtensions[ind]:
         return ind;
   if allowExtra:
      for ind in range(len(extraTypeExtensions)):
         if typeExt == extraTypeExtensions[ind]:
            return ind + numStdTypeExts;
      
   return 0;


# Sets the default output format for a given naming style if no format is passed in,
# then inserts a line after the comments in the set of lines to set this format
def addOutputFormatVarToLines(lines, nameStyle, outputFormat = None, allowExtra = False):
   if not outputFormat and nameStyle > 0:
      if nameStyle < numStdTypeExts:
         outputFormat = stdTypeExtensions[nameStyle].upper()
      elif allowExtra and nameStyle < numStdTypeExts + len(extraTypeExtensions):
         outputFormat = extraTypeExtensions[nameStyle - numStdTypeExts].upper()
   if outputFormat:
      toAdd = '$setenv IMOD_OUTPUT_FORMAT ' + outputFormat
      for ind in range(len(lines)):
         if not lines[ind].startswith('#'):
            lines.insert(ind, toAdd)
            break
      else:    # ELSE ON FOR
         lines.append(toAdd)


# Sets environment variable IMOD_OUTPUT_FORMAT either to the upper case of the
# given type extension, or if the extension is blank, sets it to override an existing 
# value to prevent an unallowed file type
# allowExtra will allow current values in the extraTypeExtensions list
def setOutputFormatIfNeeded(typeExtension, allowExtra = False):
   outputFormat = ''
   curFormat = os.getenv('IMOD_OUTPUT_FORMAT')
   if typeExtension:
      outputFormat = typeExtension.upper()
   elif curFormat and curFormat.lower() not in stdTypeExtensions and \
        (not allowExtra or curFormat.lower() not in extraTypeExtensions):
      outputFormat = 'MRC'
   if outputFormat:
      os.environ['IMOD_OUTPUT_FORMAT'] = outputFormat


# Make a backup file
def makeBackupFile(filename):
   """makeBackupFile(file)   - rename file to file~, deleting old file~"""
   if os.path.exists(filename):
      backname = filename + '~'
      try:
         if os.path.exists(backname):
            os.remove(backname)
         os.rename(filename, backname)
      except Exception:
         prnstr(fmtstr('WARNING: Failed to rename existing file {} to {}',
                       filename, backname))


# Print error strings in appropriate format after ImodpyError
def exitFromImodError(pn):
   """exitFromImodError(pn) - prints the error strings in the global errStrings
      and prepends 'ERROR: pn - ' to the last one"""
   line = None
   for l in errStrings:
      if line:
         prnstr(line, end='')
      line = l
   prnstr("ERROR: " + pn + " - " + line, end='')
   sys.exit(1)


# Funtion to parse list of comma-separated ranges
def parselist (line):
   """parselist(line)   - convert list entry in <line> into list of integers
 
   Returns the list of integers, or None for an error. An example of a list is
   1-5,7,9,11,15-20.  Numbers separated by dashes are replaced by
   all of the numbers in the range.  Numbers need not be in any order,
   and backward ranges (10-5) are handled.  Only comma and space
   are valid separators.  A / at the beginning of the string will
   return an error.  Negative numbers can be entered provided
   that the minus sign immediately precedes the number.  E.g.: -3 - -1
   or -3--1 will give -3,-2,-1; -3, -1,1 or -3,-1,1 will give -3,-1,1. """

   list = []
   dashlast = False
   negnum = False
   gotcomma = False
   gotnum = False
   nchars = len(line);

   if not nchars:
      return list
   if (line[0] == '/'):
      return None
   nlist = 0
   ind = 0
   lastnum = 0

   #   find next digit and look for '-', but error out on non -,space
   while (ind < nchars):
      next = line[ind]
      if (next.isdigit()):

         #   got a digit: save ind, find next non-digit
         gotnum = True
         numst = ind
         while (1):
            ind += 1
            next = ''
            if (ind >= nchars):
               break
            next = line[ind]
            if (not next.isdigit()):
               break

         # Convert the number
         if (negnum):
            numst -= 1
         number = int(line[numst:ind])

         # set up loop to add to list
         loopst = number
         idir = 1
         if (dashlast):
            if (lastnum > number):
               idir = -1
            loopst = lastnum + idir
            
         
         i = loopst
         while(idir * i <= idir * number):
            list.append(i)
            nlist += 1
            i += idir

         lastnum = number
         negnum = False
         dashlast = False
         gotcomma = False
         continue
   
      if (next != ',' and next != ' ' and next != '-'):
         return None
      if (next == ','):
         gotcomma = True
      if (next == '-'):
         if (dashlast or (not gotnum) or gotcomma):
            negnum = True
         else:
            dashlast = True
   
      ind += 1

   return list


# Function to read in a text file and strip line endings
def readTextFile(filename, descrip = None, returnOnErr = False, maxLines = None):
   """readTextFile(filename[ , descrip])  - read in text file, strip endings

   Reads in the text file in <filename> if this is a string, or reads from it
   as an open file object if not, strips the line endings and blanks
   from the end of each line, and returns a list of strings.  If maxLines is 
   passed, then it reads up to that number of lines.  Exits with
   exitError on an error opening or reading the file, and adds the optional
   description in <descrip> to the error message.  Or, if returnOnErr is True,
   it returns the error string itself.
   """
   if not descrip:
      descrip = " "
   try:
      errString = "Opening"
      if isinstance(filename, str):
         textfile = open(filename, 'r')
      else:
         textfile = filename
      errString = "Reading"
      if maxLines:
         lines = []
         for ind in range(maxLines):
            line = textfile.readline()
            if not line:
               break
            lines.append(line)
      else:
         lines = textfile.readlines()

   except IOError:
      errString = fmtstr("{} {} {}: {}", errString, descrip, filename, \
                       str(sys.exc_info()[1]))
      if returnOnErr:
         try:
            textfile.close()
         except:
            pass
         return errString
      exitError(errString)

   textfile.close()
   for i in range(len(lines)):
      lines[i] = lines[i].rstrip(' \t\r\n')
   return lines

# Function to write a text file with lines that have no endings
def writeTextFile(filename, strings, returnOnErr = False):
   """writeTextFile(filename, strings) - write set of strings to a text file
   Opens the file <filename> and writes the set of strings in the list
   <strings>, adding a line ending to each.  Exits with exitError upon error,
   or returns an error string if returnOnErr is True.
   """
   try:
      action = 'Opening'
      comf = open(filename, 'w')
      action = 'Writing to'
      if strings:
         for line in strings:
            prnstr(line, file=comf)
      comf.close()
   except IOError:
      errString = action + " file: " + filename + "  - " + str(sys.exc_info()[1])
      if returnOnErr:
         try:
            comf.close()
         except:
            pass
         return errString
      exitError(errString)


# Function to find an option value from a list of strings
def optionValue(linelist, option, type, ignorecase = False, numVal = 0, otherSep = None,
                emptyReturn = None):
   """optionValue(linelist, option, type[, nocase] [, numVal] [,otherSep]) - get option
   value in strings
   Given a list of strings in <linelist>, searches for one(s) that contain the
   text in <option> and that are not commented out.  A white space separator between the
   option and the value is assumed unless another separator is supplied in optional
   argument <otherSep>. The search is
   case-insensitive if optional argument <nocase> is supplied and is not False
   or None.  The return value depends on the value of <type>:
   0 = STRING_VALUE: a string with white space stripped and a comment removed from the end
   1 = INT_VALUE: an array of integers, or one integer if numVal = 1
   2 = FLOAT_VALUE: an array of floats, or one float if numVal = 1
   3 = BOOL_VALUE: a boolean value, True or false
   It returns all values found if numVal = 0, otherwise it returns just the indicated
   number of values.  Values may be separated by commas or whitespace (commas will be
   converted to spaces before splitting).
   It returns None if the option is not found, has fewer than
   numVal values, or contains inappropriate characters; in the latter case it issues a
   WARNING:.  If the option value is empty, it returns emptyReturn and issues a warning 
   that is None.
   If the option occurs more than once, the latest value applies unless there is an
   error return from the first occurrence.
   """
   flags = 0
   if ignorecase:
      flags = re.IGNORECASE
   sep = r'\s'
   if otherSep:
      sep = r'\s*' + otherSep
   optre = re.compile(r'^\s*' + option, flags)
   subre = re.compile('.*' + option + r'[^\s]*' + sep + r'([^#]*).*', flags)
   comre = re.compile(r'\s*#\s*' + option, flags)
   retval = None
   for line in linelist:
      if optre.search(line) and not comre.search(line):
         valstr = subre.sub(r'\1', line).strip()
         if type > 2:

            # A boolean can be any of these values, but it can also be a line with no
            # separator, which requires a separate re test
            bval = valstr.lower()
            if bval == '0' or bval == 'f' or bval == 'off' or bval == 'false':
               retval = False
            elif bval == '1' or bval == 't' or bval == 'on' or bval == 'true' or \
                   bval == "" or \
                   (not otherSep and re.sub('.*' + option + r'[^\s]*' + r'([^#]*).*', \
                                               r'\1', line).strip() == ""):
               retval = True
            else:
               prnstr("WARNING: optionValue - Boolean entry found with " + \
                     "improper value (" + bval + ") in: " + line)
               
         elif valstr == "":
            if emptyReturn:
               retval = [emptyReturn]
               if numVal == 1:
                  retval = emptyReturn
            else:
               prnstr("WARNING: optionValue - No value for option in: " + line)
         elif type <= 0:
            retval = valstr
         else:
            retval = []
            splits = valstr.replace(',', ' ').split()
            try:
               numConv = len(splits)
               if numVal:
                  if numConv < numVal:
                     return None
                  numConv = numVal
               for ind in range(numConv):
                  val = splits[ind]
                  if type == 1:
                     retval.append(int(val))
                  else:
                     retval.append(float(val))
            except Exception:
               prnstr("WARNING: optionValue - Bad character in numeric " + \
                     "entry in: " + line)
               return None

            if numVal == 1:
               retval = retval[0]
               
   return retval


# Convert a string to an integer with an error message if it fails
def convertToInteger(valstr, description):
   try:
      return int(valstr)
   except ValueError:
      exitError('Converting ' + description + ' (' + valstr + ') to integer')
      

# Given a com file entry with optional extension, get the rootname and its
# complete name and check for existence and uniqueness
def completeAndCheckComFile(comfile):
   """completeAndCheckComFile(comfile) - return complete com file name and root
   Given a com file file name that can be file, file., file.com, or file.pcm, compose
   the complete name and the root name and exit with error if file does not
   exist under one or other extension, or if there are both and it is unspecified
   """
   if not comfile:
      exitError("A command file must be entered")

   isCom = comfile.endswith('.com')
   isPcm = comfile.endswith('.pcm')
   if isCom or isPcm:
      rootname = comfile[0 : len(comfile) - 4]
   elif comfile.endswith('.'):
      rootname = comfile.rstrip('.')
   else:
      rootname = comfile

   # Look for the one we know it is, or both if it had neither extension
   if not isCom:
      pcmExists = os.path.exists(rootname + '.pcm')
   if not isPcm:
      comExists = os.path.exists(rootname + '.com')

   # Handle lack of specified file with extension known
   if (isCom and not comExists) or (isPcm and not pcmExists):
      exitError("Command file " + comfile + " does not exist")

   # Handle possibilities of looking for either extension: error or assign name
   if not (isCom or isPcm):
      if comExists and pcmExists:
         exitError(fmtstr('The full command file name must be entered because both ' +\
                          '{0}.com and {0}.pcm exist', rootname))
      if comExists:
         comfile = rootname + '.com'
      elif pcmExists:
         comfile = rootname + '.pcm'
      else:
         exitError(fmtstr('The full command file name must be entered because neither ' +\
                          '{0}.com nor {0}.pcm exist', rootname))

   return (comfile, rootname)


# Clean up log files from chunks, and com files too if logOnly False
def cleanChunkFiles(rootname, logOnly = False):
   rmlist = glob.glob(rootname + '-[0-9][0-9][0-9]*.log')
   if os.path.exists(rootname + '-start.log'):
      rmlist.append(rootname + '-start.log')
   if os.path.exists(rootname + '-finish.log'):
      rmlist.append(rootname + '-finish.log')
   if not logOnly:
      for ext in ('.com', '.pcm'):
         rmlist += glob.glob(rootname + '-[0-9][0-9][0-9]*' + ext)
         if os.path.exists(rootname + '-start' + ext):
            rmlist.append(rootname + '-start' + ext)
         if os.path.exists(rootname + '-finish' + ext):
            rmlist.append(rootname + '-finish' + ext)
   try:
      for filename in rmlist:
         os.remove(filename)
   except Exception:
      pass


# Clean up a list of files with multiple trials if it fails
def cleanupFiles(files):

   # Even wih a wait of 0.01 it only took two trials, so try 0.1 for this
   retryWait = 0.1
   maxTrials = 10
   numToDo = len(files)
   stillToDo = numToDo * [1]
   trial = 0
   while trial < maxTrials and numToDo:
      trial += 1
      for ind in range(len(files)):
         if stillToDo[ind]:
            removed = -1
            try:
               os.remove(files[ind])
               removed = 1
            except OSError:
               removed = str(sys.exc_info()[1]).find('No such')
            if removed >= 0:
               stillToDo[ind] = 0
               numToDo -= 1

      if numToDo:
         time.sleep(retryWait)


# Extract the entries to a program from the lines of a command file, looking for a line
# that runs progName and also contains startKey
def extractProgramEntries(comLines, progName, startKey):
   startline = -1
   endline = len(comLines)

   # Find the line that runs the program, then look for a following command
   for i in range(endline):
      line = comLines[i].strip()
      if line.startswith('$'):
         if startline > 0:
            endline = i
            break
         if progName in line and startKey in line:
            startline = i + 1

   if startline < 0:
      return None
   return comLines[startline:endline]


# Returns the Windows path for path using cygpath -m if windows is not None
def getCygpath(windows, path, typeArg = '-m'):
   if windows:
      try:
         cygtemp = runcmd('cygpath ' + typeArg + ' "' + path + '"', inStderr = 'stdout')
         if cygtemp != None and len(cygtemp) > 0:
            return cygtemp[0].strip()
      except Exception:
         pass
   return path


# Returns a Cygwin path for path using cygpath if running in Cygwin
# Falls back to composing the path itself if cygpath fails
def cygwinPath(path):
   if 'cygwin' in sys.platform:
      try:
         cygtemp = runcmd('cygpath "' + path + '"', inStderr = 'stdout')
         if cygtemp != None and len(cygtemp) > 0:
            return cygtemp[0].strip()
      except Exception:
         path = path.replace('\\', '/')
         if  path[1] == ':':
            path = '/cygdrive/' + path[0].lower() + path[2:]
         mess = ''
         if len(errStrings):
            mess = ': ' + errStrings[0].strip()
         prnstr('WARNING: Error running Cygwin program cygpath' + mess + \
                '; returning ' + path)
   return path


# Adds IMOD_DIR/bin to front of PATH and set up to ignore SIGHUP except in Windows
def addIMODbinIgnoreSIGHUP():
   os.environ['PATH'] = os.path.join(cygwinPath(os.environ['IMOD_DIR']), 'bin') + \
       os.pathsep + os.environ['PATH']
   if 'win32' not in sys.platform:
      try:
         signal.signal(signal.SIGHUP, signal.SIG_IGN)
      except Exception:
         pass
   

# Conditionally prints a PID with appropriate OS-dependent prefix
def printPID(doPrint):
   if not doPrint:
      return
   PID = 'Python PID: ' + str(os.getpid()) + '\n'
   if 'win32' in sys.platform:
      PID = 'Windows ' + PID
   elif 'cygwin' in sys.platform:
      PID = 'Cygwin ' + PID
   sys.stderr.write(PID)
   sys.stderr.flush()


# Returns the IMOD version as a string, or None for error
def getIMODversion(): 
   try:
      infoLines = runcmd('imodinfo')
      lsplit = infoLines[0].split()
      return lsplit[2]
   except Exception:
      return None


# Tests whether the path is an absolute path
def imodIsAbsPath(path):
   if 'cygwin' in sys.platform:
      try:
         pathlines = runcmd('cygpath "' + path + '"', inStderr = 'stdout')
         if len(pathlines):
            path = pathlines[0]
      except Exception:
         pass
   return os.path.isabs(path)


# Return an absolute path, converted to windows format if on Windows
def imodAbsPath(path):

   # Cygwin will not work with a windows path, so convert it
   absp = os.path.abspath(cygwinPath(path))
   if 'win32' in sys.platform or 'cygwin' in sys.platform:
      absp = getCygpath(True, absp)
   return absp
      

# Returns whether psutil uses the new API, given the psutil.__version__ as argument
def newPsutilAPI(version):
   try:
      verSplit = version.split('.')
      return int(verSplit[0]) > 1
   except Exception:
      return False


# Function to increment nice value of current process; for Windows Python it uses psutil
# and sets to below normal priority between 4 and 15, idle priority above 15
def imodNice(niceInc):
   if sys.platform.find('win32') < 0:
      os.nice(niceInc)
      return 0
   if niceInc < 4:
      return 0
   try:
      import psutil
      p = psutil.Process(os.getpid())
      if niceInc <= 15:
         priority = psutil.BELOW_NORMAL_PRIORITY_CLASS
      else:
         priority = psutil.IDLE_PRIORITY_CLASS
      if newPsutilAPI(psutil.__version__):
         p.nice(priority)
      else:
         p.nice = priority         
      return 0
   except ImportError:
      return 1

                  
# Function to return the IMOD temporary directory
def imodTempDir():
   """imodTempDir() - returns a temporary directory: IMOD_TMPDIR, /usr/tmp, or /tmp
   Uses IMOD_TMPDIR as a temporary directory if it is defined, exists, and is writable;
   otherwise uses /usr/tmp if it exists and is writable; otherwise uses /tmp if it
   exists and is writable; otherwise returns None.  Paths are converted to Windows paths
   on Windows.
   """
   windows = sys.platform.find('win32') >= 0 or sys.platform.find('cygwin') >= 0
   imodtemp = os.getenv('IMOD_TMPDIR')
   if imodtemp != None:
      imodtemp = getCygpath(windows, imodtemp)
      if os.path.exists(imodtemp) and os.path.isdir(imodtemp) and \
             os.access(imodtemp, os.W_OK):
         return imodtemp
   imodtemp = getCygpath(windows, '/usr/tmp')
   if os.path.exists(imodtemp) and os.access(imodtemp, os.W_OK):
      return imodtemp
   imodtemp = getCygpath(windows, '/tmp')
   if os.path.exists(imodtemp) and os.access(imodtemp, os.W_OK):
      return imodtemp
   return '.'


# Set the appropriate path variables for executing Qt programs if IMOD_QTLIBDIR is defined
def setLibPath():
   if os.getenv('IMOD_QTLIBDIR') == None:
      return
   macosx = sys.platform.find('darwin') >= 0
   mainvar = 'LD_LIBRARY_PATH'
   if macosx:
      mainvar = 'DYLD_LIBRARY_PATH'
   suffix = ''
   if os.getenv(mainvar) != None:
      suffix = os.pathsep + os.environ[mainvar]
   os.environ[mainvar] = os.environ['IMOD_QTLIBDIR'] + os.pathsep + \
       os.path.join(os.environ['IMOD_DIR'], 'lib') + suffix
   if macosx:
      mainvar = 'DYLD_FRAMEWORK_PATH'
      suffix = ''
      if os.getenv(mainvar) != None:
         suffix = os.pathsep + os.environ[mainvar]
      os.environ[mainvar] = os.environ['IMOD_QTLIBDIR'] + suffix


# Function for runcmd to avoid running a .com file in current directory on Windows
def avoidLocalComFile(command):
   if 'win32' not in sys.platform:
      return command

   def isExecutable(fpath):
      return os.path.isfile(fpath) and os.access(fpath, os.X_OK)

   # There is no problem if this command has a path in front of it or if there is no
   # com file in the current directory
   # This split respects quotes around components and strips them
   comSplit = shlex.split(command)
   comstr = comSplit[0]
   (comPath, comName) = os.path.split(comstr)
   if comPath:
      return command
   if not os.path.isfile(comstr + '.com'):
      return command

   # Problem.  Look on the path to find program.  IMOD is already on front...
   for path in os.environ["PATH"].split(os.pathsep):
      path = path.strip('"')
      fullPath = os.path.join(path, comstr)
      if isExecutable(fullPath) or isExecutable(fullPath + '.exe') or \
          isExecutable(fullPath + '.cmd'):

         # Compose a new command with the full path, and all components quoted
         newcom = '"' + os.path.join(path, comstr) + '"'
         for arg in comSplit[1:]:
            newcom += ' "' + arg + '"'
         return newcom

   return command


# Function to try to make current directory writable on Windows or at least make sure
# that it is
def makeCurrentDirWritable(subdir = '.'):
   # On Windows, make sure the current directory is rxw, first try the cygwin command
   # then use the system command.  Windows python may be useless at this
   # When the directory is set -rwx, its running of chmod gives permission denied, it sees
   # 777 from os.stat and os.chmod has no effect
   # 12/13/13: At least in Cygwin, some Win 7 systems have files with --------- for
   # permissions and this test failed with no consequence; so finish with real test
   if not ('win32' in sys.platform  or 'cygwin' in sys.platform):
      return None
   try:
      runcmd('chmod u+rwx ' + subdir, inStderr = 'stdout')
   except ImodpyError:
      mode = stat.S_IMODE(os.stat('.')[stat.ST_MODE])
      try:
         os.chmod('.', mode | stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
      except Exception:
         errStr = writeTextFile(subdir + '/writetest.tmp', ['Test for writability'], True)
         cleanupFiles([subdir + '/writetest.tmp'])
         return errStr


# Two functions for managing patch size and number needed by Setupcombine and
# Autopatchfit
PATCHXY = (64, 80, 100, 120)
PATCHZ = (32, 40, 50, 60)
AUTODELPATCHXY = (80, 40)
AUTODELPATCHZ = (30, 20)

# Function to return a patch size from an entry that can be a letter or a size
# in X, Y, Z
def patchSizeFromEntry(patchin):
   indpatch = 'SMLE'.find(patchin.upper())
   err = 0
   if indpatch >= 0 and len(patchin) == 1:
      patchnx = patchny = PATCHXY[indpatch]
      patchnz = PATCHZ[indpatch]
   else:
      psplit = patchin.split(',')
      err = 1
      if len(psplit) == 3:
         try:
            patchnx = int(psplit[0])
            patchny = int(psplit[1])
            patchnz = int(psplit[2])
            err = 0
         except ValueError:
            err = 1
   if err:
      return (0, 0, 0, err)
   return (patchnx, patchny, patchnz, err)

# Function to compute number of patches from size and limits
def autoPatchNumber(size, lower, upper, ifZ, densityInd = 0):
   delta = AUTODELPATCHXY[densityInd]
   if ifZ:
      delta = min(AUTODELPATCHZ[densityInd], 3 * size // 4)
   return int(round(float(upper - lower - size) / delta + 1.))


# Returns the number of pixels for parallel writing boundary regions, either the default
# value here, one supplied by the caller as the argument, or the value of environment
# variable PARALLEL_BOUNDARY_SIZE if it is greater
def parallelBoundarySize(default = 2048):
   valStr = os.getenv('PARALLEL_BOUNDARY_SIZE')
   if valStr:
      try:
         newVal = int(valStr)
         if newVal > default:
            return newVal
      except ValueError:
         pass
   return default
   

# Get elapsed time and break into minutes, seconds, and nearest tenth of second
def elapsedTimeComponents(startTime):
   used = time.time() - startTime
   minutes = int(used / 60.)
   used -= 60 * minutes
   seconds = int(used)
   frac = int(round((used - seconds) * 10))
   return (minutes, seconds, frac)


# Set some global variables before using moveOrCopyWithRetry
# Set skipWinCopy to 1 to not use robocopy, 2 o not use xcopy either
def initializeMoveOrCopy(skipWinCopy = 0):
   global mocRecursiveCopyOK, mocRenamesOK, mocUseXcopy, mocXcopyExists
   mocRenamesOK = True
   mocXcopyExists = False

   # If in Windows, see if recursive copy available with native Windows commands
   # xcopy is deprecated so prefer robocopy
   if ('win32' in sys.platform or 'cygwin' in sys.platform):
      mocXcopyExists = os.path.exists('C:/Windows/system32/xcopy.exe') and skipWinCopy < 2
      if skipWinCopy > 0 or not os.path.exists('C:/Windows/system32/robocopy.exe'):
         if mocXcopyExists:
            mocUseXcopy = True
         else:
            mocRecursiveCopyOK = False
   

# Move or copy a file or directory to toDir with possible retries, using rename, shutil,
# or Windows commands as appropriate and depending on what works.  Call 
# initializeMoveOrCopy before using.
def moveOrCopyWithRetry(fromFile, toDir, mess, ifCopy, numTrials):
   global mocRecursiveCopyOK, mocRenamesOK, mocUseXcopy, mocXcopyExists

   # Need to convert paths to windows if running Windows command from cygwin
   def winPath(filename):
      return getCygpath(isCygwin, filename, '-w')
   
   movingDir = os.path.isdir(fromFile)
   fromFile = os.path.normpath(fromFile)
   toDir =  os.path.normpath(toDir)
   isCygwin = 'cygwin' in sys.platform
   isWindows = isCygwin or 'win32' in sys.platform

   # If moving and renames have been OK so far or not tested yet, try one os.rename
   # and if anything goes wrong, mark renames as bad and fall back to copy/deletes
   # But do not try this on Windows unless it is the same file system (fails on Linux)
   if mocRenamesOK and not ifCopy and \
      (not isWindows or os.stat(fromFile).st_dev == os.stat(toDir).st_dev):
      try:
         os.rename(fromFile, os.path.join(toDir, os.path.basename(fromFile)))
         return 0
      except Exception:
         mocRenamesOK = False
         #prnstr('Simply renaming files into ' + toDir + ' fails, copy/delete is needed')
      
   for trial in range(numTrials):
      try:

         # If multiple trials, or if moving file and no directory copy available at all,
         # use the dog-slow shutil.  Use cygwin cp -rf in preference to this
         if numTrials > 1 or (movingDir and not mocRecursiveCopyOK and not isCygwin):
            if ifCopy:
               shutil.copy(fromFile, toDir)
            else:
               shutil.move(fromFile, toDir)

         else:

            # Otherwise, set up to copy file or directory, using the extended copy command
            # in Windows for directories.  If not available in Windows, use Cygwin cp
            ignore = 0
            moveFileWithRobo = False
            moveDirWithRobo = False
            if isWindows and not (isCygwin and
                                  ((movingDir and not mocRecursiveCopyOK) \
                                   or (not movingDir and not mocXcopyExists))):

               # Move a single file with robocopy if that exists; good exit status is 3
               moveFileWithRobo = not ifCopy and not movingDir and \
                                  mocRecursiveCopyOK and not mocUseXcopy
               if moveFileWithRobo:
                  fromDir = os.path.dirname(fromFile)
                  if not fromDir:
                     fromDir = '.'
                  command = fmtstr('ROBOCOPY /S /MOV "{}" "{}" "{}"', winPath(fromDir),
                                   winPath(toDir), os.path.basename(fromFile))
                  ignore = [1, 3]

               # Or copy a single file if copying, or if can't move with robocopy
               # COPY is a built-in, so have to use xcopy if running in cygwin
               elif not movingDir:
                  pref = ''
                  if isCygwin:
                     pref = 'X'
                  command = fmtstr('{}COPY /Y "{}" "{}"', pref, winPath(fromFile),
                                   winPath(toDir))
               else:

                  # Or copy directory with xcopy or robocopy: the good exit status is 1
                  if mocUseXcopy:
                     command = 'XCOPY /Y /S /I'
                  else:
                     command = 'ROBOCOPY /S /MOVE'
                     ignore = 1
                     moveDirWithRobo = True
                  toFile = os.path.join(toDir, os.path.basename(fromFile))
                  command = fmtstr('{} "{}" "{}"', command, winPath(fromFile),
                                   winPath(toFile))
            else:
               command = fmtstr('cp -rf "{}" "{}"', fromFile, toDir)
            runcmd(command, ignoreStatus = ignore)

            # Now for move, remove the tree or file if not done with robocopy
            if not ifCopy and not moveFileWithRobo and not moveDirWithRobo:
               if movingDir:
                  shutil.rmtree(fromFile)
               else:
                  os.remove(fromFile)

         return 0

      # Just catch anything here and retry or give up
      except Exception:
         if trial < numTrials - 1:
            time.sleep(0.5)

   prnstr('An error occurred ' + mess + ' to ' + toDir + ' :')
   prnstr('    ' + str(sys.exc_info()[1]))
   return 1


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


# Function to format a string in new format for earlier versions of python
def fmtstr(stringIn, *args):
   """fmtstr(string, *args) - formats a string with replacement fields
   fmtstr(string, arg list) is equivalent to string.format(arg list) in Python
   2.7/3.1 or higher provided that the replacement fields all contain either
   no numeric index or a simple numeric index.  Fields can also contain a
   format specifier after a : character.  In Python 2.6 or 3.0, missing
   numeric indices are inserted and the format function is run on the string.
   For earlier versions of python, the replacement fields are turned into
   %-style formatting entries, and then the string is formatted with the %
   operator.  Double braces become single ones, and a % is allowed in the
   string without being doubled.
   """
   string = stringIn

   # The numeric indices did not become optional until 3.1/2.7
   if pyVersion >= 310 or (pyVersion < 300 and pyVersion >= 270):
      return string.format(*args)

   # Insert numeric indices if needed and make sure all have them or need them
   numToAdd = 0
   ind = 0
   while True:
      ind = string.find('{', ind) + 1
      if ind == 0 or ind == len(string):
         break
      next = string[ind]
      if next == '{':
         ind += 1
         continue
      if next == '}' or next == ':':
         if numToAdd < 0:
            prnstr('ERROR: imodpy.fmtstr - Bad unnumbered replacement field '+\
                   'in:' + stringIn)
            sys.exit(1)
         string = string[0:ind] + str(numToAdd) + string[ind:]
         numToAdd += 1
      else:
         if numToAdd > 0:
            prnstr('ERROR: imodpy.fmtstr - Bad numbered replacement field '+\
                   'in:' + stringIn)
            sys.exit(1)
         numToAdd = -1

   if pyVersion >= 260:
      return string.format(*args)

   # Now have to convert to old style.  First escape any %
   string = string.replace('%', '%%')
   ind = 0
   arglist = []
   while True:
      ind = string.find('{', ind) + 1
      if ind == 0 or ind == len(string):
         break
      next = string[ind]
      if next == '{':
         ind += 1
         continue

      # find the end of the number
      colon = string.find(':', ind)
      brace = string.find('}', ind)
      if brace < 0:
         prnstr('ERROR: imodpy.fmtstr - Unclosed brace in: ' + stringIn)
         sys.exit(1)
      if colon < 0:
         numend = brace
      else:
         numend = min(colon, brace)

      # convert number and get argument on list
      argnum = int(string[ind:numend])
      if numend == brace and not isinstance(args[argnum], str):
         arglist.append(str(args[argnum]))
      else:
         arglist.append(args[argnum])

      # Collect start and end strings if any
      basestr = ''
      endstr = ''
      if ind > 1:
         basestr = string[0:ind-1]
      if brace < len(string) - 1:
         endstr = string[brace+1:]
      
      # If there is no :, replace brace and number with %s
      # If there is a :, add a % and take the format string
      if numend == brace:
         string = basestr + '%s' + endstr
      else:
         string = basestr + '%' + string[colon+1:brace] + endstr

   return string % tuple(arglist)
      

# Function to replace print, with same format as new print function
def prnstr(string, file = sys.stdout, end = '\n', flush = False):
   """prnstr(string, file = sys.stdout, end = '\n', flush = False) - replaces
   print function
   This function can be called like the new print function to write to a file,
   or to stdout by default, and add a line ending by default.  If end='',
   then it will not write a space after writing the line, so it can be used to
   write strings with line endings as the old print did.  If end=' ' it will
   write a space after the string as the new print function does.  If flush
   is True or the file is open in binary mode, flush() is called after the
   write.
   """
   binary = 'b' in str(file.mode)
   if pyVersion >= 300 and binary:
      string = string.encode()
      end = end.encode()
   if end == '':
      file.write(string)
   else:
      file.write(string + end)
   if binary or flush:
      file.flush()
