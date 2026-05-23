/*  readlinevalues.c : Flexible reading of lines from file into arrays
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 2020 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 * $Id$
 */
#include "stdarg.h"
#include "b3dutil.h"
#include "parse_params.h"

/*!
 * Reads values from one or more lines of the file pointed to 
 * by [fp] and places them repeatedly into the successive variables listed in [...].
 * The types of the variables are provided by a corresponding set of letters in [types]:
 * "i" for int, "f" for float, or "d" for double.  [valSize] is the size of the value 
 * arrays.  A value of [numToGetP] greater than zero specifies the number of sets of 
 * values to get; otherwise the function will try to get all lines in the file.  Pass in 
 * 0 to get all lines in the file, with an error return if they do not fit in the 
 * supplied arrays; pass in -1 to get all values up to whatever fits in the arrays, 
 * without an error.  The actual number of values obtained is returned in [numToGetP].  
 * Lines are read into [line], whose size is specified by [maxLine].  If the bit flag
 * RLFV_SEPARATE_LINES is set in [flags], it will read one set of data per line, 
 * ignoring any entries past that and generating an error if a line has fewer values than
 * the number of arguments.  Returns -1 for an error reading the file, -2 for end of file
 * before the specified [numToGetP] values are found, -3 for the whole file not fitting 
 * in the arrays, -4 for an error parsing the data or a failure to find all values on 
 * a line when reading as separate lines, -5 for a value read for an integer argument that
 * is not close enough to an integer, -6 for a memory error, -7 for programming error. 
 * When multiple value arguments are passed, it allocates a temporary array big enough 
 * to fit all the data before sorting it into the different arrays.
 */
int readLinesForValues(FILE *fp, int *numToGetP, int valSize, char *line, int maxLine,
                       int flags, const char *types, ...)
{
  va_list args;
  int readType = PIP_INTEGER;
  int numArgs = strlen(types);
  int anyInts = 0, anyFloats = 0, anyDbls = 0, dataSize = sizeof(int);
  int numToGetIn = *numToGetP;
  int numToGet = numToGetIn;
  int ind, ent, entInd, perr, ierr, numOnLine, numGot = 0;
  int separateLines = (flags & RLFV_SEPARATE_LINES) ? 1 : 0;
  double diff;
  char *valUse;
  float *floatUse;
  int *intUse;
  double *dblUse;
  int **intArgs = NULL;
  float **floatArgs = NULL;
  double **dblArgs = NULL;
  if (!numArgs)
    return -7;

  /* Assess what types are needed and the type that has to be read as */
  anyInts = strchr(types, 'i') ? 1 : 0;
  anyFloats = strchr(types, 'f') ? 1 : 0;
  anyDbls = strchr(types, 'd') ? 1 : 0;
  if (anyDbls) {
    readType = PIP_DOUBLE;
    dataSize = sizeof(double);
  } else if (anyFloats) {
    readType = PIP_FLOAT;
    dataSize = sizeof(float);
  }

  /* Make full array of any kind of pointer needed */
  if (anyInts) {
    intArgs = B3DMALLOC(int *, numArgs);
    if (!intArgs)
      return -6;
  }
  if (anyFloats) {
    floatArgs = B3DMALLOC(float *, numArgs);
    if (!floatArgs)
      return -6;
  }
  if (anyDbls) {
    dblArgs = B3DMALLOC(double *, numArgs);
    if (!dblArgs)
      return -6;
  }

  /* Get the pointers */
  va_start(args, types);
  for (ind = 0; ind < numArgs; ind++) {
    switch (types[ind]) {
    case 'i':
      intArgs[ind] = va_arg(args, int *);
      if (!intArgs[ind])
        return -7;
      break;
    case 'f':
      floatArgs[ind] = va_arg(args, float *);
      if (!floatArgs[ind])
        return -7;
      break;
    case 'd':
      dblArgs[ind] = va_arg(args, double *);
      if (!dblArgs[ind])
        return -7;
      break;
    default:
      va_end(args);
      return -7;
    }
  }

  /* If one arg, assign the argument pointer directly */
  if (numArgs == 1) {
    if (anyInts)
      valUse = (char *)intArgs[0];
    else if (anyFloats)
      valUse =  (char *)floatArgs[0];
    else
      valUse = (char *)dblArgs[0];
  } else {

    // Otherwise allocate the temp array big enough for all data */
    valUse = B3DMALLOC(char, dataSize * numArgs * valSize);
    if (!valUse)
      return -6;
    numToGet *= numArgs;
    valSize *= numArgs;
    intUse = (int *)valUse;
    floatUse = (float *)valUse;
    dblUse = (double *)valUse;
  }
  if (numToGet <= 0)
    numToGet = valSize;

  /* Read lines to get the requested, or all, data */
  perr = 0;
  while (numGot < numToGet) {
    ierr = fgetline(fp, line, maxLine);
    if (ierr == -2 || ierr == -1)
      break;
    if (!ierr)
      continue;
    numOnLine = separateLines ? numArgs : -1;
    //printf("%d  %s\n", numOnLine, line);
    perr = PipGetLineOfValues(line, line, valUse + dataSize * numGot, readType,
                              &numOnLine, valSize - numGot);
    if (perr)
      break;
    numGot = B3DMIN(numGot + numOnLine, numToGet);
    //printf("%d %d\n", numOnLine, numGot);
    if (ierr < 0)
      break;
  }

  /* If no error condition, return the data to multiple args */
  if (numArgs > 1 && !(perr || ierr == -1 || 
                       (ierr < 0 && numToGetIn > 0 && numGot < numToGet) ||
                       (ierr == 0 && numToGetIn == 0 && numGot == valSize))) {
    numGot = numArgs * (numGot / numArgs);
    for (ent = 0; ent < numGot / numArgs && !perr; ent++) {
      for (ind = 0; ind < numArgs; ind++) {
        entInd = ent * numArgs + ind;
        switch (types[ind]) {
        case 'i':
          if (anyDbls) {
            intArgs[ind][ent] = B3DNINT(dblUse[entInd]);
            diff = intArgs[ind][ent] - dblUse[entInd];
            if (diff > 0.001 || diff < -0.001)
              perr = -5;
          } else if (anyFloats) {
            intArgs[ind][ent] = B3DNINT(floatUse[entInd]);
            diff = intArgs[ind][ent] - floatUse[entInd];
            if (diff > 0.001 || diff < -0.001)
              perr = -5;
          } else
            intArgs[ind][ent] = intUse[entInd];
          break;
        case 'f':
          if (anyDbls)
            floatArgs[ind][ent] = dblUse[entInd];
          else
            floatArgs[ind][ent] = floatUse[entInd];
          break;
        case 'd':
          dblArgs[ind][ent] = dblUse[entInd];
          break;
        }
      }
    }
  }
  
  /* Clean up */
  free(intArgs);
  free(floatArgs);
  free(dblArgs);
  if (numArgs > 1)
    free(valUse);
  if (perr == -5)
    return -5;
  if (perr)
    return -4;   /* Parse error OR not enough on line if separate lines */
  
  if (ierr == -1)
    return -1;    /* Read error */

  if (ierr < 0 && numToGetIn > 0 && numGot < numToGet)
    return -2;    /* End of file looking for specific number */

  if (ierr < 0 && numToGetIn == 0 && numGot == valSize)
    return -3;    /* Array was full */

  if (numToGetIn <= 0)
    *numToGetP = numGot / numArgs;
  return 0;
}

