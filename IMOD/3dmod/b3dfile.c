/*  IMOD VERSION 2.41
 *
 *  b3dfile.c -- Save graphic port image to file. Supports Tiff and SGI rgb.
 *
 *  Original author: James Kremer
 *  Revised by: David Mastronarde   email: mast@colorado.edu
 */

/*****************************************************************************
 *   Copyright (C) 1995-2000 by Boulder Laboratory for 3-Dimensional Fine    *
 *   Structure ("BL3DFS") and the Regents of the University of Colorado.     *
 *                                                                           *
 *   BL3DFS reserves the exclusive rights of preparing derivative works,     *
 *   distributing copies for sale, lease or lending and displaying this      *
 *   software and documentation.                                             *
 *   Users may reproduce the software and documentation as long as the       *
 *   copyright notice and other notices are preserved.                       *
 *   Neither the software nor the documentation may be distributed for       *
 *   profit, either in original form or in derivative works.                 *
 *                                                                           *
 *   THIS SOFTWARE AND/OR DOCUMENTATION IS PROVIDED WITH NO WARRANTY,        *
 *   EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTY OF          *
 *   MERCHANTABILITY AND WARRANTY OF FITNESS FOR A PARTICULAR PURPOSE.       *
 *                                                                           *
 *   This work is supported by NIH biotechnology grant #RR00592,             *
 *   for the Boulder Laboratory for 3-Dimensional Fine Structure.            *
 *   University of Colorado, MCDB Box 347, Boulder, CO 80309                 *
 *****************************************************************************/
/*  $Author$

    $Date$

    $Revision$
*/

#include <stdio.h>
#include "imodconfig.h"

#ifdef __vms
#define B3DFILE_LTLENDIAN
#else
#ifdef B3D_LITTLE_ENDIAN
#define B3DFILE_LTLENDIAN
#else
#define B3DFILE_BIGENDIAN
#endif
#endif

#include "b3dfile.h"

void iputbyte(FILE *fout, unsigned char val)
{
     unsigned char buf[1];
     
     buf[0] = val;
     fwrite(buf, 1, 1, fout);
     return;
}

void iputshort(FILE *fout, b3dUInt16 val)
{
     unsigned char buf[2];
     
     buf[0] = (unsigned char)(val >> 8);
     buf[1] = (unsigned char)(val >> 0);
     fwrite(buf, 2, 1, fout);
     return;
}

void iputlong(FILE *fout, b3dUInt32 val)
{
     unsigned char buf[4];
     
     buf[0] = (unsigned char)(val >> 24);
     buf[1] = (unsigned char)(val >> 16);
     buf[2] = (unsigned char)(val >>  8);
     buf[3] = (unsigned char)(val >>  0);
     fwrite(buf, 4, 1, fout);
     return;
}

int bdRGBWrite(FILE *fout, int xsize, int ysize, 
	       unsigned char *pixels)
{
     char iname[80];
     unsigned int xysize = xsize * ysize;
     unsigned int i;

     /* Create an SGI rgb file */
     iputshort(fout, 474);       /* MAGIC                */
     iputbyte (fout,   0);       /* STORAGE is VERBATIM  */
     iputbyte (fout,   1);       /* BPC is 1             */
     iputshort(fout,   3);       /* DIMENSION is 3       */
     iputshort(fout, (b3dInt16)xsize);     /* XSIZE                */
     iputshort(fout, (b3dInt16)ysize);     /* YSIZE                */
     iputshort(fout,   3);       /* ZSIZE                */
     iputlong (fout, 0l);        /* PIXMIN is 0          */
     iputlong (fout, 255l);      /* PIXMAX is 255        */
     iputlong (fout, 0);         /* DUMMY 4 bytes        */
     fwrite(iname, 80, 1, fout); /* IMAGENAME            */
     iputlong (fout, 0);         /* COLORMAP is 0        */
     for(i=0; i<404; i++)        /* DUMMY 404 bytes      */
	  iputbyte(fout,0);

     for (i = 0; i < xysize; i++)
	  iputbyte (fout, pixels[(i*4)+3]);
     for (i = 0; i < xysize; i++)
	  iputbyte (fout, pixels[(i*4)+2] );
     for (i = 0; i < xysize; i++)
	  iputbyte (fout, pixels[(i*4)+1]);

     return(0);
}

