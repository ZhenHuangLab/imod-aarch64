/*   b3dfile.h  -  declarations for b3dfile.cpp
 *
 *   Copyright (C) 1995-2002 by Boulder Laboratory for 3-Dimensional Electron
 *   Microscopy of Cells ("BL3DEMC") and the Regents of the University of 
 *   Colorado.  See implementation file for full copyright notice.
 */                                                                           

/*  $Author$

$Date$

$Revision$

*/

#ifndef B3DFILE_H
#define B3DFILE_H

#ifdef __cplusplus
extern "C" {
#endif

/* special file io commands. */
int bdRGBWrite(FILE *fout, int xsize, int ysize, unsigned char *pixels);
  void iputbyte(FILE *fout, unsigned char val);
  void iputshort(FILE *fout, b3dUInt16 val);
  void iputlong(FILE *fout, b3dUInt32 val);


#ifdef __cplusplus
}
#endif

#endif
