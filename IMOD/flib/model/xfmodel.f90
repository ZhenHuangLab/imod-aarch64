!*****   XFMODEL  *********************************************
! Will take a model from wimp or imod, and either
! a) use corresponding points in two sections to obtain a transformation
! between the sections, or
! b) transform the points in the model to match a new alignment of images
!
! For more details, see man page
!
! $Id$
!
program xfmodel
  use fortmodel
  implicit none
  integer LIMNUMXF, LIMPCLIST, idim, MSIZE_XMAT
  parameter (LIMNUMXF = 100000, LIMPCLIST = 100000)
  real*4 fXf(2,3,LIMNUMXF), gXf(2,3,LIMNUMXF), gTemp(2,3)
  integer*4 nxyz(3), mxyz(3), nx, ny, nz, mode
  equivalence (nxyz(1), nx), (nxyz(2), ny), (nxyz(3), nz)
  real*4 delta(3) /0., 0., 0./
  integer*4 numSec(LIMNUMXF), listZ(LIMNUMXF), indZtoFlist(LIMNUMXF)
  integer*4 numInChunks(LIMNUMXF), numChunks
  integer*4 ixPcList(LIMPCLIST), iyPcList(LIMPCLIST), izPcList(LIMPCLIST)
  parameter (MSIZE_XMAT = 19)
  real*4, allocatable :: xmat(:,:)
  character*320 modelfile, newModel, oldXfgFile, oldXfFile, newXfFile, idfFile
  character*320 magGradFile
  logical gotThis, gotLast, exist, readw_or_imod
  integer*4 getImodMaxes, getImodScales
  integer*4 limPoints/4/                      !min # of points for regression
  !
  integer*4 i, numListZ, numFout, numPcList, izRange, ifFillGap, indFXf, indVal
  integer*4 minXpiece, numXpieces, nxOverlap, minYpiece, numYpieces, nyOverlap
  real*4 xHalfSize, yHalfSize, xCen, yCen, reportMeanCrit, reportMaxCrit, dmin, dmax
  integer*4 ifxfmod, ifTrans, ifRotTrans, ifPrealign, numToFind, ifMagRot
  integer*4 ifSingle, izSingle, ifFullReport, ierr, ierr2, indGXf, ifShiftScale
  real*4 zz, zIndex, zThis, zLast, shiftScale, dmean
  integer*4 numUndefined, iobj, ipt, iz, numFGin, indToInd, izMin, izMax, ibase
  integer*4 lastSec, numPoints, iobject, numInObj, ipnt, isol, ipntMax, j
  real*4 const, rsq, theta, sinTheta, cosTheta, gmag, devmax
  real*4 devAvg, devSD, xx, yy, xLast, yLast, xNew, yNew
  integer*4 loop, numOldG, izSec, il, indInObj, maxX, maxY, maxZ, ifBack, iter
  integer*4 lineUse, lineToUse, ifMagGrad, indControl
  logical done
  real*4 atan2d
  !
  integer*4 ifDistort, idfBinning, iBinning, idfNx, idfNy, indIdf, indPreWarp
  integer*4 nxGrid, nyGrid, limGrid, iPreWarpNx, iPreWarpNy, iwarpNx, iwarpNy
  real*4 xGridStrt, yGridStrt, xGridIntrv, yGridIntrv, pixelIdf, binRatio
  real*4 pixelModel, pixelWarp, pixelPrewarp, prewarpScale, warpScale
  real*4, allocatable :: fieldDx(:,:), fieldDy(:,:), warpDx(:,:,:), warpDy(:,:,:)
  real*4, allocatable :: xWarpStart(:), yWarpStart(:), xWarpIntrv(:), yWarpIntrv(:)
  integer*4, allocatable :: numControl(:,:), nxWarp(:), nyWarp(:)
  character*10240 stringList
  real*4 pixelMagGrad, axisRot, xmodMin, xmodMax, yModMin, yModMax, gridExtendFrac
  real*4 tiltAngles(LIMNUMXF), dmagPerUm(LIMNUMXF), rotPerUm(LIMNUMXF), dx, dy, dx1, dy1
  integer*4 numMagGrad, indWarpFile, limWarpX, limWarpY, limWarpZ
  integer*4 getSizeAdjustedGrid, readCheckWarpFile, getGridParameters, clearWarpFile
  integer*4 setCurrentWarpFile, findMaxGridSize, getLinearTransform
  !
  logical pipInput
  integer*4 numOptArg, numNonOptArg
  integer*4 PipGetInteger, PipGetBoolean, PipNumberOfEntries
  integer*4 PipGetString, PipGetTwoIntegers, PipGetFloatArray, PipGetFloat
  integer*4 PipGetIntegerArray, PipGetNonOptionArg, PipGetTwoFloats
  integer*4 PipGetInOutFile
  !
  ! fallbacks from ../../manpages/autodoc2man -3 2  xfmodel
  !
  integer numOptions
  parameter (numOptions = 24)
  character*(40 * numOptions) options(1)
  options(1) = &
      'input:InputFile:FN:@output:OutputFile:FN:@image:ImageFile:FN:@'// &
      'piece:PieceListFile:FN:@allz:AllZhaveTransforms:B:@'// &
      'center:CenterInXandY:FP:@transonly:TranslationOnly:B:@'// &
      'rottrans:RotationTranslation:B:@magrot:MagRotTrans:B:@'// &
      'sections:SectionsToAnalyze:LI:@single:SingleSection:I:@'// &
      'full:FullReportMeanAndMax:FP:@prealign:PrealignTransforms:FN:@'// &
      'edit:EditTransforms:FN:@xforms:XformsToApply:FN:@'// &
      'useline:UseTransformLine:I:@chunks:ChunkSizes:LI:@'// &
      'back:BackTransform:B:@scale:ScaleShifts:F:@'// &
      'distort:DistortionField:FN:@binning:BinningOfImages:I:@'// &
      'gradient:GradientFile:FN:@param:ParameterFile:PF:@help:usage:B:'
  !
  ! set defaults
  !
  modelfile = ' '
  xCen = 0.
  yCen = 0.
  ifBack = 0
  iBinning = 1
  ifDistort = 0
  ifMagGrad = 0
  numChunks = 0
  shiftScale = 1.
  gridExtendFrac = 0.1
  idim = 100000
  !
  ! Pip startup: set error, parse options, check help, set flag if used
  !
  call PipReadOrParseOptions(options, numOptions, 'xfmodel', &
      'ERROR: XFMODEL - ', .true., 2, 1, 1, numOptArg, &
      numNonOptArg)
  pipInput = numOptArg + numNonOptArg > 0

  !
  ! get parameters
  !
  if (pipInput) then
    ierr = PipGetString('ImageFile', modelfile)
  else
    write(*,'(1x,a,$)') &
        'Image file (or Return to enter Xcen, Ycen directly): '
    read(*,'(a)') modelfile
  endif
  !
  ! if no image file, get crucial info directly
  ! DNM 8/28/02: just get xcen, ycen since origin and delta aren't needed
  !
  if (modelfile == ' ') then
    if (pipInput) then
      ierr = PipGetTwoFloats('CenterInXandY', xCen, yCen)
    else
      print *,char(7), &
          'Be SURE to enter CENTER coordinates, NOT NX and NY'
      write(*,'(1x,a,$)') 'Xcen, Ycen: '
      read(*,*) xCen, yCen
    endif
    do i = 1, LIMNUMXF
      listZ(i) = i - 1
    enddo
    numListZ = LIMNUMXF
    numFout = 0
  else
    !
    ! otherwise get header info from image file
    call imopen(1, modelfile, 'ro')
    !
    ! get header info for proper coordinate usage
    call irdhdr(1, nxyz, mxyz, mode, dmin, dmax, dmean)
    call iiuRetDelta(1, delta)
    ! call irtorg(1, xorig, yorig, zorig)
    ! write(*,'(/,a,a,/)') ' This header info MUST be the' &
    ! , ' same as when model was built'
    !
    modelfile = ' '
    if (pipInput) then
      ierr = PipGetString('PieceListFile', modelfile)
    else
      write(*,'(1x,a,$)') 'Piece list file if image is a'// &
          ' montage, otherwise Return: '
      read(*,'(a)') modelfile
    endif
    call read_piece_list(modelfile, ixPcList, iyPcList, izPcList, &
        numPcList)
    if (numPcList > LIMPCLIST) call exitError( &
        'too many piece coordinates for arrays')
    !
    ! if no pieces, set up mocklist
    if (numPcList == 0) then
      do i = 1, nz
        ixPcList(i) = 0
        iyPcList(i) = 0
        izPcList(i) = i - 1
      enddo
      numPcList = nz
    endif
    ! get ordered list of z values
    call fill_listz(izPcList, numPcList, listZ, numListZ)
    if (numListZ > LIMNUMXF) call exitError( &
        'too many Z values for arrays')

    call checklist(ixPcList, numPcList, 1, nx, minXpiece &
        , numXpieces, nxOverlap)
    call checklist(iyPcList, numPcList, 1, ny, minYpiece &
        , numYpieces, nyOverlap)
    xHalfSize = (nx + (numXpieces - 1) * (nx - nxOverlap)) / 2.
    yHalfSize = (ny + (numYpieces - 1) * (ny - nyOverlap)) / 2.

    ! DNM 8/28/02: don't scale any more
    ! but still add minxpiece - the index coordinates are montage
    ! coordinates starting at minxpiece.
    !
    ! [xy]cen is the amount to SUBTRACT from real model coordinates to
    ! get coordinates centered on the center of the image.  Adding [xy]
    ! orig shifts coordinates to the lower left corner of image.
    ! Subtracting 0.5*nx*delt(1) (half of the image width in real
    ! coordinates) then shifts coords to center of image.  Hence this:
    !
    ! xcen=(minxpiece+xhaf)*delt(1) -xorig
    ! ycen=(minypiece+yhaf)*delt(2) -yorig
    xCen = (minXpiece + xHalfSize)
    yCen = (minYpiece + yHalfSize)
    numFout = numListZ

  endif
  !
  ! find out if gaps in z and ask how to store f's if there are gaps
  !
  izRange = listZ(numListZ) + 1 - listZ(1)
  ifFillGap = 0
  if (izRange > numListZ) then
    write(*,'(1x,a,i4,a)') 'There are', izRange - numListZ, &
        ' gaps in section Z values'
    if (pipInput) then
      ierr = PipGetBoolean('AllZhaveTransforms', ifFillGap)
      if (ifFillGap == 0) then
        print *,'Xform lists will be assumed to have xforms ', &
            'only for existing sections'
      else
        print *,'Xform lists will be assumed to have xforms ', &
            'for all sections'
      endif
    else
      write(*,'(1x,a,/,a$)') 'Enter 0 for xform lists'// &
          ' that have xforms only for existing sections,', &
          '    or 1 for xform lists that have xforms for'// &
          ' all Z values in range: '
      read(*,*) ifFillGap
    endif
  endif
  !
  ! make index from z values to transform list: index=0 for non-existent
  !
  if (pipInput .and. PipGetString('ChunkSizes', stringList) == 0) &
      call parseList(stringList, numInChunks, numChunks)
  do i = 1, LIMNUMXF
    indZtoFlist(i) = 0
  enddo
  if (numChunks == 0) then
    do i = 1, numListZ
      indFXf = listZ(i) + 1 - listZ(1)
      indVal = i
      if (ifFillGap .ne. 0) indVal = indFXf
      indZtoFlist(indFXf) = indVal
    enddo
    if (numFout > 0) numFout = indZtoFlist(listZ(numListZ) + 1 - listZ(1))
  else
    !
    ! or fill index list
    !
    ! print *,numChunks, (numInChunks(j), j=1, numChunks)
    indFXf = 1
    do j = 1, numChunks
      do i = 1, numInChunks(j)
        indZtoFlist(indFXf) = j
        indFXf = indFXf + 1
        if (indFXf >= LIMNUMXF) call exitError('Too many sections in chunks for arrays')
      enddo
    enddo
  endif
  ! print *,(indfl(i), i=1, nlistz)
  !
  ifxfmod = 0
  ifPrealign = 0
  if (PipGetInOutFile('InputFile', 1, 'Input model file', modelfile) &
      .ne. 0) call exitError('No input file specified')
  oldXfgFile = ' '
  oldXfFile = ' '
  idfFile = ' '
  magGradFile = ' '
  ifTrans = 0
  ifRotTrans = 0
  ifMagRot = 0
  lineUse = -1
  if (pipInput) then
    ierr = PipGetBoolean('TranslationOnly', ifTrans)
    ierr = PipGetBoolean('RotationTranslation', ifRotTrans)
    ierr = PipGetBoolean('MagRotTrans', ifMagRot)
    ierr = PipGetInteger('UseTransformLine', lineUse)
    ifShiftScale = 1 - PipGetFloat('ScaleShifts', shiftScale)

    if (PipGetString('XformsToApply', oldXfFile) == 0) ifxfmod = 1
    if (PipGetString('DistortionField', idfFile) == 0) ifxfmod = 1
    if (PipGetString('GradientFile', magGradFile) == 0) ifxfmod = 1
    if (PipGetString('PrealignTransforms', oldXfgFile) == 0) &
        ifPrealign = 1
    !
    ! if back-transform, first check for legality
    !
    if (PipGetBoolean('BackTransform', ifBack) == 0) then
      if (oldXfFile .ne. ' ' .and. oldXfgFile .ne. ' ') call exitError( &
          'You cannot enter both -xform and -prealign with -back')
      if (ifxfmod + ifPrealign == 0) &
          call exitError('You must enter -xform, -prealign,'// &
          ' -distort or -gradient with -back')
      !
      ! in any case, set filename for use in back-transform, clear out
      ! transform filename, set flag for prealign back-transform if any
      ! Set xfmodel -1 if xform is givem, or if preali with no
      ! distortions; i.e. preali with distortion will retransform forward
      ! to original (distorted) aligned stack
      !
      if (oldXfFile .ne. ' ' .or. (idfFile == ' ' .and. &
          magGradFile == ' ')) ifxfmod = -1
      if (oldXfFile .ne. ' ') oldXfgFile = oldXfFile
      oldXfFile = ' '
      if (oldXfgFile .ne. ' ') ifPrealign = 1
    endif
    !
    if (ifTrans + ifRotTrans + ifMagRot > 1) call exitError &
        ('Only one of -trans, -rottrans, -magrot may be entered')
    if (ifTrans + ifRotTrans + ifMagRot > 0 .and. ifxfmod .ne. 0) &
        call exitError('You cannot both find transforms '// &
        'and transform a model')
    if (ifxfmod == 0 .and. ifShiftScale .ne. 0) call exitError &
        ('You cannot enter -scale unless you are transforming a model')
    if (ifTrans .ne. 0) ifxfmod = 2
    if (ifRotTrans .ne. 0) ifxfmod = 3
    if (ifMagRot .ne. 0) ifxfmod = 4

    if (.not.(ifxfmod == -1 .or. (ifxfmod == 1 .and. &
        (oldXfFile .ne. ' ' .or. oldXfgFile .ne. ' '))))  then
      if (lineUse >= 0) call exitError('You cannot enter -useline'// &
          ' unless you are transforming a model')
      if (numChunks > 0) call exitError('You cannot enter -chunks'// &
          ' unless you are transforming a model')
    endif
    if (lineUse >= 0 .and. numChunks > 0) call exitError('It is '// &
        'meaningless to enter both -useline and -chunks')
  else
    !
    write(*,'(1x,a,/,a,/,a/,a,$)') 'Enter 0 to find '// &
        'transformations, 2 to find X/Y translations only,', &
        '    3 to find translations and rotations only,', &
        '    4 to find translation, rotation and mag change only,', &
        '    1 to transform model, or -1 to back-transform model: '
    read(*,*) ifxfmod
  endif
  !
  ! set variables for restricted fits
  !
  if (ifxfmod == 2) then
    ifxfmod = 0
    ifTrans = 1
    limPoints = 1
  elseif (ifxfmod == 3) then
    ifRotTrans = 1
    ifxfmod = 0
    limPoints = 2
  elseif (ifxfmod == 4) then
    ifRotTrans = 2
    ifxfmod = 0
    limPoints = 3
  endif
  !
  ! read in the model now since its range is needed
  exist = readw_or_imod(modelfile)
  if (.not.exist) goto 91
  !
  ! shift the data to image coordinates before using
  call scale_model(0)
  !
  ! Get model range
  xmodMin = 1.e38
  xmodMax = -1.e38
  yModMin = 1.e38
  yModMax = -1.e38
  do i = 1, n_point
    xmodMin = min(xmodMin, p_coord(1, i))
    xmodMax = max(xmodMax, p_coord(1, i))
    yModMin = min(yModMin, p_coord(2, i))
    yModMax = max(yModMax, p_coord(2, i))
  enddo
  !
  ! Extend the range so that properly extrapolated grids can be used to find inverse
  ! point for forward transforms
  xLast = xmodMax + 1 - xmodMin
  xmodMin = xmodMin - gridExtendFrac * xLast
  xmodMax = xmodMax + gridExtendFrac + xLast
  yLast = yModMax + 1 - yModMin
  yModMin = yModMin - gridExtendFrac * yLast
  yModMax = yModMax + gridExtendFrac + yLast
  !
  ifSingle = 0
  ifFullReport = 0
  numToFind = 0
  if (pipInput) then
    if (PipGetInOutFile('OutputFile', 2, ' ', newXfFile) .ne. 0) &
        call exitError('NO OUTPUT FILE SPECIFIED')
    if (ifxfmod == 0) then
      oldXfFile = ' '
      ierr = PipGetString('EditTransforms', oldXfFile)

      if (PipGetTwoFloats('FullReportMeanAndMax', reportMeanCrit, reportMaxCrit) &
          == 0) ifFullReport = 1
      if (PipGetInteger('SingleSection', izSingle) == 0) ifSingle = 1
      if (PipGetString('SectionsToAnalyze', stringList) == 0) &
          call parseList(stringList, numSec, numToFind)
    else
      newModel = newXfFile
      if (idfFile .ne. ' ') then
        ifDistort = 1
        indIdf = readCheckWarpFile(idfFile, 1, 1, idfNx, idfNy, i, idfBinning, &
            pixelIdf, iz, stringList)
        if (indIdf < 0) call exitError(stringList)
        if (getGridParameters(1, nxGrid, nyGrid, xGridStrt, yGridStrt, xGridIntrv, &
            yGridIntrv) .ne. 0) call exitError('Getting distortion field parameters')
        limGrid = max(nxGrid, ceiling((xmodMax - xmodMin) / xGridIntrv), &
            nyGrid, ceiling((yModMax - yModMin) / yGridIntrv)) + 2
        allocate(fieldDx(limGrid, limGrid), fieldDy(limGrid, limGrid), stat = ierr)
        call memoryError(ierr, 'arrays for distortion field')
        !
        ! if the center is not yet defined, need to get it now
        !
        if (xCen == 0. .and. yCen == 0.) then
          ierr = getImodMaxes(maxX, maxY, maxZ)
          xCen = maxX / 2.
          yCen = maxY / 2.
          write(*,'(a,2f8.1)') 'Using model header to determine '// &
              'center coordinates:', xCen, yCen
        endif
        !
        ! insist on binning unless situation is unambiguous, and convert
        ! the distortion field by the difference in binning
        !
        if (PipGetInteger('BinningOfImages', iBinning) .ne. 0) then

          if (2. * xCen <= idfNx * idfBinning / 2 .and. &
              2. * yCen <= idfNy * idfBinning / 2) call exitError &
              ('You must specify binning of images because they '// &
              'are not larger than half the camera size')
        endif
        if (iBinning <= 0) call exitError &
            ('IMAGE BINNING MUST BE A POSITIVE NUMBER')
        binRatio = 1.
        if (iBinning .ne. idfBinning) binRatio = idfBinning / float(iBinning)
        if (getSizeAdjustedGrid(1, (xmodMax - xmodMin) / binRatio, &
            (yModMax - yModMin) / binRatio, &
            ((xmodMax + xmodMin) / binRatio - idfNx) / 2., &
            ((yModMax + yModMin) / binRatio - idfNy) / 2., 0, binRatio, 1, &
            nxGrid, nyGrid, xGridStrt, yGridStrt, xGridIntrv, &
            yGridIntrv, fieldDx, fieldDy, limGrid, limGrid, stringList) .ne. 0) &
            call exitError(stringList)
        ierr = clearWarpFile(indIdf)
        !
        ! Need to shift field by difference between image and camera
        ! centers
        !
        xGridStrt = xGridStrt - (idfNx * binRatio / 2. - xCen)
        yGridStrt = yGridStrt - (idfNy * binRatio / 2. - yCen)
        ! print *,ixGridStrt, ixGridStrt, xcen, ycen, idfnx, idfny, binratio
      endif
    endif
    !
    ! mag gradients now
    !
    if (magGradFile .ne. ' ') then
      ifMagGrad = 1
      call readMagGradients(magGradFile, LIMNUMXF, pixelMagGrad, axisRot, &
          tiltAngles, dmagPerUm, rotPerUm, numMagGrad)
    endif

  else
    !
    ! old input: get prealignment or back transforms
    !
    if (ifxfmod < 0) then
      ifPrealign = 1
    else
      write(*,'(1x,a,$)') &
          'Model built on raw sections (0) or prealigned ones(1): '
      read(*,*) ifPrealign
    endif
    !
    if (ifPrealign .ne. 0) then
      write(*,'(1x,a,$)') &
          'File of old g transforms used in prealignment: '
      read(*,'(a)') oldXfgFile
    endif
    !
    if (ifxfmod == 0) then
      write(*,'(1x,a,$)') 'Name of old file of f transforms'// &
          ' to edit (Return if none): '
      read(*,'(a)') oldXfFile
      !
      write(*,'(1x,a,$)') 'Name of new file of f transforms: '
      read(*,'(a)') newXfFile
      !
      write(*,118)
118   format(' Enter / to find transforms for all section pairs,',/, &
          '      or -999 to find transforms relative to a single', &
          ' section,',/, &
          '      or a list of the section numbers to find', &
          ' transforms for',/,'         (enter number of second', &
          ' section of each pair, ranges are ok)')
      call rdList(5, numSec, numToFind)
      if (numToFind == 1 .and. numSec(1) == -999) then
        write(*,'(1x,a,$)') 'Number of single section to find'// &
            ' transforms relative to: '
        read(*,*) izSingle
        ifSingle = 1
        print *,'Now enter list of sections to find transforms', &
            ' for (/ for all)'
        numToFind = 0
        call rdList(5, numSec, numToFind)
      endif
      !
      write(*,'(1x,a,$)') '1 for full reports of deviations for'// &
          ' sections with bad fits, 0 for none: '
      read(*,*) ifFullReport
      !
      if (ifFullReport .ne. 0) then
        write(*,'(1x,a,$)') 'Enter criterion mean deviation and'// &
            ' max deviation; full reports will be given', &
            '     for sections with mean OR max greater than'// &
            ' these criteria: '
        read(*,*) reportMeanCrit, reportMaxCrit
      endif
      !
    else
      write(*,'(1x,a,$)') 'New model file name: '
      read(*,'(a)') newModel
      !
      if (ifxfmod > 0) then
        write(*,'(1x,a,$)') 'File for list of g transforms to apply: '
        read(*,'(a)') oldXfFile
      endif
      !
    endif
  endif
  call PipDone()
  !
  ! if the center is not yet defined, use the model header sizes
  !
  if (xCen == 0. .and. yCen == 0.) then
    ierr = getImodMaxes(maxX, maxY, maxZ)
    xCen = maxX / 2.
    yCen = maxY / 2.
    write(*,'(a,2f8.1)') 'Using model header to determine '// &
        'center coordinates:', xCen, yCen
  endif
  !
  ! first fill array with unit transforms in case things get weird
  !
  do i = 1, LIMNUMXF
    call xfUnit(fXf(1, 1, i), 1.)
  enddo
  !
  ! Assess whether either file is a warping file
  indPreWarp = -1
  indWarpFile = -1
  limWarpX = 0
  limWarpY = 0
  limWarpZ = 0
  if (ifPrealign .ne. 0) then
    indPreWarp = readCheckWarpFile(oldXfgFile, 0, 1, iPreWarpNx, iPreWarpNy, numOldG, i, &
        pixelPrewarp, iz, stringList)
    if (indPreWarp < -1) call exitError(stringList)
    if (indPreWarp >= 0) limWarpZ = numOldG
  endif
  if (oldXfFile .ne. ' ') then
    indWarpFile = readCheckWarpFile(oldXfFile, 0, 1, iwarpNx, iwarpNy, numFGin, i, &
        pixelWarp, iz, stringList)
    if (indWarpFile < -1) call exitError(stringList)
    if (indWarpFile >= 0) limWarpZ = max(limWarpZ, numFGin)
  endif
  !
  ! One or other warping exists
  if (limWarpZ > 0) then
    allocate(numControl(limWarpZ, 2), stat = ierr)
    call memoryError(ierr, 'ARRAY FOR NUMBER OF CONTROL POINTS')
    !
    ! Need to know the pixel size of the model.  Take image pixel first.
    pixelModel = delta(1)
    if (delta(1) <= 0) ierr = getImodScales(pixelModel, xLast, yLast)
    !
    ! Find out how big grids need to be
    if (indPreWarp >= 0) then
      write(*,'(a,a)') 'Warping file opened: ', trim(oldXfgFile)
      prewarpScale = pixelPrewarp / pixelModel
      ierr = setCurrentWarpFile(indPreWarp)
      if (findMaxGridSize(xmodMin / prewarpScale, xmodMax / prewarpScale, &
          yModMin / prewarpScale, yModMax / prewarpScale, numControl(1, 1), limWarpX, &
          limWarpY, stringList) .ne. 0) call exitError(stringList)
    endif
    if (indWarpFile >= 0) then
      write(*,'(a,a)') 'Warping file opened: ', trim(oldXfFile)
      warpScale = pixelWarp / pixelModel
      ierr = setCurrentWarpFile(indWarpFile)
      if (findMaxGridSize(xmodMin / warpScale, xmodMax / warpScale, &
          yModMin / warpScale, yModMax / warpScale, numControl(1, 2), i, iz, &
          stringList) .ne. 0) call exitError(stringList)
      limWarpX = max(limWarpX, i)
      limWarpY = max(limWarpY, iz)
    endif
    !
    ! Allocate arrays for grid and parameters for each section
    allocate(warpDx(limWarpX, limWarpY, limWarpZ), warpDy(limWarpX, limWarpY, limWarpZ), &
        nxWarp(limWarpZ), xWarpStart(limWarpZ), xWarpIntrv(limWarpZ), nyWarp(limWarpZ), &
        yWarpStart(limWarpZ), yWarpIntrv(limWarpZ), stat = ierr)
    call memoryError(ierr, 'ARRAYS FOR WARPING GRIDS')
  endif
  !
  ! back-transform if necessary
  if (ifPrealign .ne. 0) then
    if (indPreWarp >= 0) then
      ierr = setCurrentWarpFile(indPreWarp)
      if (numOldG > LIMNUMXF) call exitError( &
          'too many sections in warp file for transform array')
      if (ifShiftScale == 0) shiftScale = prewarpScale
      indControl = 1
      do i = 1, numOldG
        if (getLinearTransform(i, gXf(1, 1, i)) .ne. 0) &
            call exitError('GETTING LINEAR TRANSFORM FROM WARP FILE')
        if (numControl(i, 1) > 2) then
          if (getSizeAdjustedGrid(i, (xmodMax - xmodMin) / prewarpScale, &
              (yModMax - yModMin) / prewarpScale, &
              ((xmodMax + xmodMin) / prewarpScale - iPreWarpNx) / 2., &
              ((yModMax + yModMin) / prewarpScale - iPreWarpNy) / 2., 0, prewarpScale, &
              1,  nxWarp(i), nyWarp(i), xWarpStart(i), yWarpStart(i), xWarpIntrv(i), &
              yWarpIntrv(i), warpDx(1, 1, i), warpDy(1, 1, i), limWarpX, limWarpY, &
              stringList) .ne. 0) call exitError(stringList)
        endif
      enddo
    else
      call dopen(3, oldXfgFile, 'ro', 'f')
      !
      ! get g transforms into g list
      call xfrdall2(3, gXf, numOldG, LIMNUMXF, ierr)
      if (ierr .ne. 0) call exitError('too many transforms for arrays')
      if (numOldG == 0) call exitError('The file of prealign transforms is empty: '// &
          trim(oldXfgFile))
      close(3)
    endif
    !
    ! invert the g's into the g list
    do indGXf = 1, numOldG
      gXf(1, 3, indGXf) = gXf(1, 3, indGXf) * shiftScale
      gXf(2, 3, indGXf) = gXf(2, 3, indGXf) * shiftScale
      call xfInvert(gXf(1, 1, indGXf), gTemp)
      call xfCopy(gTemp, gXf(1, 1, indGXf))
    enddo
    !
    ! apply inverse g's to all points in model
    ! Set up to use a single section if back transforming and user
    ! specified it or there is only one transform
    !
    lineToUse = -1
    if (ifxfmod < 0) then
      lineToUse = lineUse
      if (numOldG == 1 .and. numChunks == 0) then
        lineToUse = 0
        print *,'There is only one transform and it is being', &
            ' applied at all Z values'
      endif
    endif
    write(*,'(a,a)') 'Back-transforming model with inverse of transforms from ', &
        trim(oldXfgFile)
    if (indPreWarp >= 0) call warpModel(numOldG, 1)
    call transformModel(gXf, numOldG, LIMNUMXF, xCen, yCen, indZtoFlist, listZ, &
        lineToUse, numUndefined)

    if (ifxfmod < 0 .and. ifDistort + ifMagGrad == 0) then
      !
      ! write out back-transformed model
      call rescaleWriteModel(newModel, numUndefined)
      call exit(0)
    endif
  endif
  !
  ! read in the old f's or g's for whatever purpose
  !
  if (indWarpFile < 0) numFGin = 0
  lineToUse = -1
  if (oldXfFile .ne. ' ') then
    if (indWarpFile >= 0) then
      ierr = setCurrentWarpFile(indWarpFile)
      if (numFGin > LIMNUMXF) call exitError( &
          'too many sections in warp file for transform array')
      if (ifShiftScale == 0) shiftScale = warpScale
      indControl = 2
      do i = 1, numFGin
        if (getLinearTransform(i, fXf(1, 1, i)) .ne. 0) &
            call exitError('GETTING LINEAR TRANSFORM FROM WARP FILE')
        if (numControl(i, 2) > 2) then
          if (getSizeAdjustedGrid(i, (xmodMax - xmodMin) / warpScale, &
              (yModMax - yModMin) / warpScale, &
              ((xmodMax + xmodMin) / warpScale - iwarpNx) / 2., &
              ((yModMax + yModMin) / warpScale - iwarpNy) / 2., 0, warpScale, &
              1,  nxWarp(i), nyWarp(i), xWarpStart(i), yWarpStart(i), xWarpIntrv(i), &
              yWarpIntrv(i), warpDx(1, 1, i), warpDy(1, 1, i), limWarpX, limWarpY, &
              stringList) .ne. 0) call exitError(stringList)
        endif
      enddo
    else
      call dopen(1, oldXfFile, 'ro', 'f')
      call xfrdall2(1, fXf, numFGin, LIMNUMXF, ierr)
      if (ierr .ne. 0) call exitError('too many transforms for arrays')
      if (numFGin == 0) call exitError('The input file of transforms is empty: '// &
          trim(oldXfFile))
      close(1)
    endif
    do indGXf = 1, numFGin
      fXf(1, 3, indGXf) = fXf(1, 3, indGXf) * shiftScale
      fXf(2, 3, indGXf) = fXf(2, 3, indGXf) * shiftScale
    enddo
    lineToUse = lineUse
    if (numFGin == 1 .and. numChunks == 0) then
      lineToUse = 0
      print *,'There is only one transform and it is being', &
          ' applied at all Z values'
    endif
  endif
  !
  ! TRANSFORMING/UNDISTORTING MODEL
  !
  if (ifxfmod .ne. 0) then
    if (ifDistort + ifMagGrad .ne. 0) then
      if (ifBack .ne. 0) then
        print *,'Redistorting model'
      else
        print *,'Undistorting model'
      endif

      do i = 1, n_point
        if (ifMagGrad .ne. 0) &
            iz = max(1, min(nint(p_coord(3, i) + 1.), numMagGrad))

        if (ifBack == 0) then
          !
          ! undistort the model - find point that distorts to the
          ! given model point
          !
          iter = 1
          xLast = p_coord(1, i)
          yLast = p_coord(2, i)
          done = .false.
          do while (iter < 10 .and. .not.done)
            dx1 = 0.
            dy1 = 0.
            dx = 0.
            dy = 0.
            if (ifMagGrad .ne. 0) &
                call magGradientShift(xLast, yLast, nint(2. * xCen), nint(2. * yCen), &
                xCen, yCen, pixelMagGrad, axisRot, tiltAngles(iz), dmagPerUm(iz), &
                rotPerUm(iz), dx1, dy1)

            if (ifDistort .ne. 0) &
                call interpolateGrid(xLast + dx1, yLast + dy1, fieldDx, fieldDy, &
                limGrid, nxGrid, nyGrid, xGridStrt, yGridStrt, xGridIntrv, &
                yGridIntrv, dx, dy)
            xNew = p_coord(1, i) - (dx + dx1)
            yNew = p_coord(2, i) - (dy + dy1)
            done = abs(xNew - xLast) < 0.01 .and. &
                abs(yNew - yLast) < 0.01
            xLast = xNew
            yLast = yNew
            iter = iter + 1
          enddo
          p_coord(1, i) = xNew
          p_coord(2, i) = yNew
        else
          !
          ! or redistort the model
          !
          dx1 = 0.
          dy1 = 0.
          dx = 0.
          dy = 0.
          if (ifMagGrad .ne. 0) &
              call magGradientShift(p_coord(1, i), p_coord(2, i), &
              nint(2. * xCen), nint(2. * yCen), &
              xCen, yCen, pixelMagGrad, axisRot, tiltAngles(iz), &
              dmagPerUm(iz), rotPerUm(iz), dx1, dy1)

          if (ifDistort .ne. 0) &
              call interpolateGrid(p_coord(1, i) + dx1, p_coord(2, i) + dy1, &
              fieldDx, fieldDy, limGrid, nxGrid, nyGrid, xGridStrt, &
              yGridStrt,  xGridIntrv, yGridIntrv, dx, dy)
          p_coord(1, i) = p_coord(1, i) + dx1 + dx
          p_coord(2, i) = p_coord(2, i) + dy1 + dy
        endif
      enddo
      !
      ! if there was prealignment and no new transforms, get the
      ! prealignment transforms back by inversion and set up to use them
      !
      if (numFGin == 0 .and. ifPrealign .ne. 0 .and. &
          ifxfmod > 0) then
        write(*,'(a,a)') 'Transforming model with reinverted transforms from ', &
            trim(oldXfgFile)
        do indGXf = 1, numOldG
          call xfInvert(gXf(1, 1, indGXf), fXf(1, 1, indGXf))
        enddo
        numFGin = numOldG
        indWarpFile = indPreWarp
      endif
    endif
    !
    numUndefined = 0
    !
    ! transform the model
    !
    if (oldXfFile .ne. ' ') write(*,'(a,a)') &
        'Transforming model with transforms from ', trim(oldXfFile)
    if (numFGin .ne. 0) call transformModel(fXf, numFGin, LIMNUMXF, &
        xCen, yCen, indZtoFlist, listZ, lineToUse, numUndefined)
    if (indWarpFile >= 0) call warpModel(numFGin, 0)

    call rescaleWriteModel(newModel, numUndefined)
  else
    !
    ! SEARCH FOR POINTS TO DERIVE XFORMS FROM
    !
    ! allocate xr, limiting it to much more than could be needed for smaller model
    idim = min(idim, n_point)
    allocate(xmat(MSIZE_XMAT, idim), stat = ierr)
    call memoryError(ierr, 'ARRAY FOR DATA MATRIX')

    ! first find min and max z in model
    izMin = 100000
    izMax = -izMin
    do iobj = 1, max_mod_obj
      ibase = ibase_obj(iobj)
      do ipt = 1, npt_in_obj(iobj)
        i = abs(object(ipt + ibase))
        zz = p_coord(3, i)
        ! zdex=(zz+zorig) /delt(3)
        zIndex = zz
        iz = int(zIndex - nint(zIndex) + 0.5) + nint(zIndex)
        ! if (iz<listz(1) .or.iz>listz(nlistz)) goto 93
        izMin = min(izMin, iz)
        izMax = max(izMax, iz)
      enddo
    enddo
    !
    ! if didn't specify list of section #'s, make such a list from range
    !
    if (numToFind <= 0) then
      numToFind = ifSingle + izMax - izMin
      do i = 1, numToFind
        numSec(i) = izMin + i - ifSingle
      enddo
    endif
    numFout = max(numFout, numFGin)
    write(*,122)
122 format(40x,'Deviations between transformed points on',/, &
        41x,'section and points on previous section',/, &
        32x,'Mean     Max  @object & point #   X-Y Position')
    do loop = 1, numToFind
      izSec = numSec(loop)
      !
      ! make sure this is a section in list and find previous z
      !
      lastSec = -100000
      do il = 2, numListZ
        if (izSec == listZ(il)) lastSec = listZ(il - 1)
      enddo
      !
      ! but if doing to single section, allow section to be first in list
      ! as well and set lastsec to z of single section
      !
      if (ifSingle .ne. 0 .and. (lastSec .ne. -100000 .or. &
          izSec == listZ(1))) lastSec = izSingle
      if (lastSec .ne. -100000) then
        !
        ! get points out of objects with points in both this and previous
        ! section
        !
        numPoints = 0
        do iobject = 1, max_mod_obj
          gotLast = .false.
          gotThis = .false.
          numInObj = npt_in_obj(iobject)
          do indInObj = 1, numInObj
            ipnt = object(indInObj + ibase_obj(iobject))
            if (ipnt > 0 .and. ipnt <= n_point) then
              ! zdex=(p_coord(3, ipnt) +zorig) /delt(3)
              zIndex = p_coord(3, ipnt)
              iz = int(zIndex - nint(zIndex) + 0.5) + nint(zIndex)
              if ((iz == izSec) .and. (.not.gotThis .or. &
                  (p_coord(3, ipnt) < zThis))) then
                !
                ! if in second section, and either haven't gotten a point
                ! there before, or this point has a lower z than the
                ! previous point, put x and y into 1st and 2nd
                ! column: independent vars
                !
                xmat(1, numPoints + 1) = p_coord(1, ipnt) - xCen
                xmat(2, numPoints + 1) = p_coord(2, ipnt) - yCen
                gotThis = .true.
                zThis = p_coord(3, ipnt)
                xmat(6, numPoints + 1) = iobject
                xmat(7, numPoints + 1) = indInObj
              elseif ((iz == lastSec) .and. (.not.gotLast .or. &
                  (p_coord(3, ipnt) > zLast))) then
                !
                ! if in first section, and either haven't gotten a point
                ! there before, or this point is higher in z than the
                ! previous point, put x and y into 4th and 5th
                ! column: dependent vars
                !
                xmat(4, numPoints + 1) = p_coord(1, ipnt) - xCen
                xmat(5, numPoints + 1) = p_coord(2, ipnt) - yCen
                gotLast = .true.
              endif
            endif
          enddo
          if (gotThis .and. gotLast) numPoints = numPoints + 1
          if (numPoints >= idim) then
            print *
            print *,'ERROR: XFMODEL - ', &
                'too many points for arrays on section', iz
            call exit(1)
          endif
        enddo                               !done with looking at objects
        if (numPoints >= limPoints) then
          !
          ! now if there are at least limpnts points, do regressions:
          ! first last section x then l.s. y as function of this section
          ! x and y
          ! save results in xform for izsec
          !
          indFXf = indZtoFlist(izSec + 1 - listZ(1))
          if (indFXf <= 0) then
            print *
            print *,'ERROR: XFMODEL - z value out of range for ', &
                'transforms: ', zz
            call exit(1)
          endif
          !
          call findxf(xmat, MSIZE_XMAT, 4, numPoints, xCen, yCen, ifTrans, ifRotTrans, &
              2, fXf(1, 1, indFXf), devAvg, devSD, devmax, ipntMax)
          !
          ! keep track of the highest & lowest transforms obtained
          !
          numFout = max(numFout, indFXf)
          write(*,121) numPoints, izSec, devAvg, devmax, &
              nint(xmat(6, ipntMax)), nint(xmat(7, ipntMax)), &
              xmat(8, ipntMax), xmat(9, ipntMax)
121       format(i4, &
              ' points, section #',i4,2x,2f9.2,2i6,4x,2f9.2)
          if (ifFullReport .ne. 0 .and. &
              (devmax >= reportMaxCrit .or. devAvg > reportMeanCrit)) &
              write(*,124) ((xmat(i, j), i = 6, 13), j = 1, numPoints)
124       format('    Object  Point       position        ', &
              'deviation vector   angle   magnitude',/, &
              (f10.0,f6.0,4f10.2,f9.0,f10.2))
        else
          print *,'less than', limPoints, ' points for section # ', &
              izSec
        endif
      endif
    enddo                                   !end of loop the loop
    !
    ! write out enough for whole file, and at least as many as were in
    ! an input file if any
    !
    call dopen(2, newXfFile, 'new', 'f')
    do i = 1, numFout
      call xfwrite(2, fXf(1, 1, i),*94)
    enddo
    close(2)
  endif
  call exit(0)
91 call exitError('Reading model file')
92 call exitError('Reading old f/g file')
94 call exitError('Writing out f file')


CONTAINS

  subroutine warpModel(numSecIn, ifBack)
    integer*4 numSecIn
    integer*4 iobj, ipt, i, iz, indToInd, indGXf, ifBack
    real*4 zz, dx, dy
    !
    numUndefined = 0
    do iobj = 1, max_mod_obj
      do ipt = 1, npt_in_obj(iobj)
        i = abs(object(ibase_obj(iobj) + ipt))
        zz = p_coord(3, i)
        iz = int(zz - nint(zz) + 0.5) + nint(zz)
        indToInd = iz + 1 - listZ(1)
        if (.not. ((indToInd < 1 .or. indToInd > LIMNUMXF) .and. lineToUse < 0)) then
          if (lineToUse < 0) then
            indGXf = indZtoFlist(indToInd)
          else
            indGXf = lineToUse + 1
          endif
          if (indGXf >= 1 .and. indGXf <= numSecIn) then
            if (numControl(indGXf, indControl) > 2) then
              if (ifBack .ne. 0) then
                call interpolateGrid(p_coord(1, i),  p_coord(2, i), &
                    warpDx(1, 1, indGXf), warpDy(1, 1, indGXf), limWarpX, &
                    nxWarp(indGXf), nyWarp(indGXf), xWarpStart(indGXf), &
                    yWarpStart(indGXf), xWarpIntrv(indGXf), yWarpIntrv(indGXf), dx, dy)
                p_coord(1, i) = p_coord(1, i) + dx
                p_coord(2, i) = p_coord(2, i) + dy
              else
                call findInversePoint( p_coord(1, i),  p_coord(2, i), &
                    warpDx(1, 1, indGXf), warpDy(1, 1, indGXf), limWarpX, &
                    nxWarp(indGXf), nyWarp(indGXf), xWarpStart(indGXf), &
                    yWarpStart(indGXf), xWarpIntrv(indGXf), yWarpIntrv(indGXf), &
                    p_coord(1, i),  p_coord(2, i), dx, dy)
              endif
            endif
          endif
        endif
      enddo
    enddo
    return
  end subroutine warpModel

end program xfmodel


subroutine transformModel(fXf, numFGin, LIMNUMXF, xCen, yCen, &
    indZtoFlist, listZ, lineToUse, numUndefined)
  use fortmodel
  implicit none
  real*4 fXf(2,3,*), xCen, yCen
  integer*4 numFGin, LIMNUMXF, numUndefined, indZtoFlist(*), listZ(*), lineToUse
  integer*4 iobj, ipt, i, iz, indToInd, indGXf
  real*4 zz
  !
  numUndefined = 0
  do iobj = 1, max_mod_obj
    do ipt = 1, npt_in_obj(iobj)
      i = abs(object(ibase_obj(iobj) + ipt))
      zz = p_coord(3, i)
      iz = int(zz - nint(zz) + 0.5) + nint(zz)
      indToInd = iz + 1 - listZ(1)
      if ((indToInd < 1 .or. indToInd > LIMNUMXF) .and. lineToUse < 0) then
        numUndefined = numUndefined + 1
      else
        if (lineToUse < 0) then
          indGXf = indZtoFlist(indToInd)
        else
          indGXf = lineToUse + 1
        endif
        if (indGXf < 1 .or. indGXf > numFGin) then
          numUndefined = numUndefined + 1
        else
          call xfApply(fXf(1, 1, indGXf), xCen, yCen, p_coord(1, i), &
              p_coord(2, i), p_coord(1, i), p_coord(2, i))
        endif
      endif
    enddo
  enddo
  return
end subroutine transformModel


subroutine rescaleWriteModel(newModel, numUndefined)
  use fortmodel
  implicit none
  integer*4 numUndefined
  character*(*) newModel

  !
  if (numUndefined > 0) print *,numUndefined, &
      ' points with Z values out of range of transforms'
  !
  ! write model out
  ! shift the data back for saving
  !
  call scale_model(1)
  call write_wmod(newModel)
  return
end subroutine rescaleWriteModel

