! * * * * WARPVOL * * * * *
!
! WARPVOL will transform a volume using a series of general linear
! transformations. Its main use is to "warp" one tomogram from
! a two-axis tilt series so that it matches the other tomogram.
! For each position in the volume, it interpolates between adjacent
! adjacent transformations to find the transformation appropriate for
! that position.  Any initial alignment transformation (from
! SOLVEMATCH) must be already contained in the transformations entered
! into this program; this combining of transformations is accomplished
! by FINDWARP.  It can work with either a 2-D matrix of
! transformations (varying in X and Z) or with a general 3-D matrix,
! as output by FINDWARP.  The program uses the same algorithm as
! ROTATEVOL for rotating large volumes.
!
! See man page for more details
!
! $Id$
!
program warpvol
  use rotmatwarp
  implicit none
  real*4 cell(6), dxyzIn(3)
  logical*4, allocatable :: solved(:), solveTemp(:)
  integer*4 mxyzIn(3)
  real*4 aFwd(3,3), aOld(3,3), aNew(3,3), aOldInv(3,3)
  real*4 angles(3), tiltOld(3), tiltNew(3), origin(3), xTemp(3)
  real*4 aTemp1(3,3), aTemp2(3,3), dxyzTemp1(3), dxyzTemp2(3), delta(3)
  real*4, allocatable :: aLoc(:,:,:), dxyzLoc(:,:), alocTemp(:,:,:), dlocTemp(:,:)
  real*4, allocatable :: offsetIn(:,:,:), aPix(:,:,:,:)

  integer*4 izSec(6)
  integer*4 inMin(3), inMax(3), icube(3)
  real*4 freeInput(10), xyzCen(3), xCen, yCen, zCen
  ! equivalence (xcen, xyzcen(1)), (ycen, xyzcen(2)), (zcen, xyzcen(3))
  !
  character*320 filein, fileOut, fileInv, tempDir, tempExt, patchFile, fillFile
  !
  ! DNM 3/8/01: initialize the time in case time(tim) doesn't work
  !
  character dat * 9, tim * 8 / '00:00:00'/
  character*80 titleCh
  integer*4 numLocY, numInput, numLocX, numLocZ, numLocTot, i, j, kti, ja, ix, iy, ierr
  real*4 xLocStart, yLocStart, xLocMax, yLocMax, zLocStart, zLocMax, cenLocX, cenLocY
  real*4 cenLocZ, dxLoc, dyLoc, dzloc, dmin, dmax, tsum
  integer*4 ind, izCube, ixCube, iyCube, indX, indY, indZ, ival, ifEmpty
  integer*4 iz, ixLimit, iyLimit, izLimit, ixp, iyp, ixpP1, ixpM1, iypP1, iypM1, numDone
  real*4 xOffsOut, xp, yp, zp, bval, dx, dy, dz
  real*4 dxSq, dySq, dzSq, fx, fxM1, fxP1, fy, fyM1, fyP1, fz, fzM1, fzP1
  real*4 dmean, dminIn, dmaxIn, d11, d12, d21, d22
  integer*4 iunit, longint, l, izp, izpP1, izpm1, nLinesOut, interpOrder, numExtra
  integer*4 newExtra, lForErr, newLocX, newLocY, newLocZ
  integer*4 nxLowAdd, nyLowAdd, nzLowAdd, nxHighAdd, nyHighAdd, nzHighAdd
  real*4 baseInt, outerCen, xfScale
  integer*4 indInner, indOuter, iyxf
  real*8 cpuTime, wallTime, cpuStart, wallStart, wallCum, threadWall
  integer*4 innerStart, innerEnd, iouterStart, iouterEnd, innerAxis
  integer*4 iouterAxis, innerStride, iouterStride, indBase, iouter, numZtoDo
  integer*4 izStart, izEnd, numZleft, numThreads, numOMPthreads
  !
  logical pipinput
  integer*4 numOptArg, numNonOptArg
  integer*4 PipGetInteger, PipGetFloatArray, PipNumberOfEntries
  integer*4 PipGetString, PipGetThreeIntegers, PipGetThreeFloats, PipGetFloat
  integer*4 PipGetNonOptionArg, PipGetInOutFile, PipGetBoolean
  !
  ! fallbacks from ../../manpages/autodoc2man -2 2  warpvol
  !
  integer numOptions
  parameter (numOptions = 15)
  character*(40 * numOptions) options(1)
  options(1) = &
      'input:InputFile:FN:@output:OutputFile:FN:@xforms:TransformFile:FN:@'// &
      'scale:ScaleTransforms:F:@tempdir:TemporaryDirectory:CH:@'// &
      'size:OutputSizeXYZ:IT:@same:SameSizeAsInput:B:@order:InterpolationOrder:I:@'// &
      'chunk:ChunkSizesForHDF:IT:@memory:MemoryLimit:I:@verbose:VerboseOutput:I:@'// &
      'patch:PatchOutputFile:FN:@filled:FilledInOutputFile:FN:@'// &
      'param:ParameterFile:PF:@help:usage:B:'
  !
  ! set defaults here
  !
  interpOrder = 2
  xfScale = 1.
  baseInt = 0.5
  tempDir = ' '
  tempExt = 'wrp      1'
  call time(tim)
  call b3dDate(dat)
  patchFile = ' '
  fillFile = ' '
  wallCum = 0.
  !
  !
  ! Pip startup: set error, parse options, check help, set flag if used
  !
  call PipReadOrParseOptions(options, numOptions, 'warpvol', &
      'ERROR: WARPVOL - ', .true., 3, 1, 1, numOptArg, &
      numNonOptArg)
  pipinput = numOptArg + numNonOptArg > 0

  if (PipGetInOutFile('InputFile', 1, 'Name of input file', filein) &
      .ne. 0) call exitError('No input file specified')
  call imopen(5, filein, 'RO')
  call irdhdr(5, nxyzIn, mxyzIn, mode, dminIn, dmaxIn, dmeanIn)
  nxOut = nzin
  nyOut = nyIn
  nzOut = nxIn
  !
  if (pipinput) then
    ierr = PipGetString('PatchOutputFile', patchFile)
    ierr = PipGetString('FilledInOutputFile', fillFile)
  endif

  if (PipGetInOutFile('OutputFile', 2, 'Name of output file', fileOut) &
      .ne. 0 .and. patchFile == ' ' .and. fillFile == ' ') &
      call exitError('No output file specified')
  !
  call setMemoryLimitAndHdfChunks(pipinput)
  if (pipinput) then
    ierr = PipGetString('TemporaryDirectory', tempDir)
    ierr = PipGetInteger('InterpolationOrder', interpOrder)
    ierr = PipGetInteger('VerboseOutput', iVerbose)
    ierr = PipGetFloat('ScaleTransforms', xfScale)
    iz = 0
    ierr = PipGetBoolean('SameSizeAsInput', iz)
    if (iz .ne. 0) then
      nxOut = nxIn
      nzOut = nzin
    endif
    ierr = PipGetThreeIntegers('OutputSizeXYZ', nxOut, nyOut, nzOut)
    if (PipGetString('TransformFile', fileInv) .ne. 0) call exitError( &
        'No file with inverse transforms specified')
  else
    write(*,'(1x,a,/,a,$)') 'Enter path name of directory for '// &
        'temporary files, ', ' or Return to use current directory: '
    read(5, '(a)') tempDir
    !
    write(*,'(1x,a,3i5,a,$)') 'X, Y, and Z dimensions of the '// &
        'output file (/ for', nxOut, nyOut, nzOut, '): '
    read(5,*) nxOut, nyOut, nzOut
    !
    ! get matrix for inverse transforms
    !
    print *,'Enter name of file with inverse transformations'
    read(5, '(a)') fileInv
  endif
  call PipDone()
  if (interpOrder < 2) baseInt = 0.
  !
  call dopen(1, fileInv, 'ro', 'f')
  read(1, '(a)') fileInv
  call frefor(fileInv, freeInput, numInput)
  numLocY = nint(freeInput(1))
  if (numInput == 2) then
    numLocX = 1
    numLocZ = nint(freeInput(2))
  else
    numLocX = nint(freeInput(2))
    numLocZ = nint(freeInput(3))
    if (numInput == 9) then
      xLocStart = freeInput(4) * xfScale
      yLocStart = freeInput(5) * xfScale
      zLocStart = freeInput(6) * xfScale
      dxLoc = freeInput(7) * xfScale
      dyLoc = freeInput(8) * xfScale
      dzloc = freeInput(9) * xfScale
    endif
  endif
  numLocTot = numLocY * numLocZ * numLocX
  if (numLocTot == 0) call exitError( &
      'The warp file specifies 0 positions on one AXIS')
  allocate (alocTemp(3, 3, numLocTot), dlocTemp(3, numLocTot), solveTemp(numLocTot), &
      stat = ierr)
  if (ierr .ne. 0) call exitError( &
      'Allocating memory for reading in transformation arrays')
  if (numInput .ne. 9) then
    !
    ! Every position must be present, find min and max to deduce interval
    !
    xLocStart = 1.e10
    xLocMax = -1.e10
    yLocStart = 1.e10
    yLocMax = -1.e10
    zLocStart = 1.e10
    zLocMax = -1.e10

    do l = 1, numLocTot
      lForErr = l
      read(1,*,err = 98, end = 98) cenLocX, cenLocY, cenLocZ
      read(1,*,err = 98, end = 98) ((alocTemp(i, j, l), j = 1, 3), dlocTemp(i, l),  &
          i = 1, 3)
      cenLocX = cenLocX * xfScale
      cenLocY = cenLocY * xfScale
      cenLocZ = cenLocZ * xfScale
      dlocTemp(1:3, l) = dlocTemp(1:3, l) * xfScale
      solveTemp(l) = .true.
      xLocStart = min(cenLocX, xLocStart)
      yLocStart = min(cenLocY, yLocStart)
      zLocStart = min(cenLocZ, zLocStart)
      xLocMax = max(cenLocX, xLocMax)
      yLocMax = max(cenLocY, yLocMax)
      zLocMax = max(cenLocZ, zLocMax)
    enddo
    dxLoc = (xLocMax - xLocStart) / max(1, numLocY - 1)
    dyLoc = (yLocMax - yLocStart) / max(1, numLocX - 1)
    dzloc = (zLocMax - zLocStart) / max(1, numLocZ - 1)
  endif
  !
  ! Now fix the dlocs if only one position, then read other way
  if (numLocY == 1) dxLoc = 1.
  if (numLocX == 1) dyLoc = 1.
  if (numLocZ == 1) dzloc = 1.
  if (numInput == 9) then
    !
    ! Know min and interval, so read each entry, find where it goes in
    ! array and put it there
    do l = 1, numLocTot
      solveTemp(l) = .false.
    enddo
    lForErr = 1
10  read(1,*,err = 98, end = 12) cenLocX, cenLocY, cenLocZ
    cenLocX = cenLocX * xfScale
    cenLocY = cenLocY * xfScale
    cenLocZ = cenLocZ * xfScale
    ix = min(numLocY, max(1, nint((cenLocX - xLocStart) / dxLoc + 1.)))
    iy = min(numLocX, max(1, nint((cenLocY - yLocStart) / dyLoc + 1.)))
    iz = min(numLocZ, max(1, nint((cenLocZ - zLocStart) / dzloc + 1.)))
    l = ix + (iy - 1) * numLocY + (iz - 1) * numLocY * numLocX
    read(1,*,err = 98, end = 98) ((alocTemp(i, j, l), j = 1, 3), dlocTemp(i, l), i = 1, 3)
    dlocTemp(1:3, l) = dlocTemp(1:3, l) * xfScale
    solveTemp(l) = .true.
    lForErr = lForErr + 1
    go to 10
  endif
12 close(1)
  !
  ! determine additional positions needed to cover plane of volume
  nxLowAdd = 0
  nxHighAdd = 0
  nyLowAdd = 0
  nyHighAdd = 0
  nzLowAdd = 0
  nzHighAdd = 0
  if (numLocY > 1) then
    nxLowAdd = max(0, nint((xLocStart + nxOut / 2.) / dxLoc))
    nxHighAdd = max(0, nint((nxOut / 2. - (xLocStart + (numLocY - 1) * dxLoc)) / dxLoc))
  endif
  if (numLocX > numLocZ) then
    nyLowAdd = max(0, nint((yLocStart + nyOut / 2.) / dyLoc))
    nyHighAdd = max(0, nint((nyOut / 2. - (yLocStart + (numLocX - 1) * dyLoc)) / dyLoc))
  elseif (numLocZ > 1) then
    nzLowAdd = max(0, nint((zLocStart + nzOut / 2.) / dzloc))
    nzHighAdd = max(0, nint((nzOut / 2. - (zLocStart + (numLocZ - 1) * dzloc)) / dzloc))
  endif
  !
  ! If any are being added, shift the transforms up in the array
  ! and adjust the parameters
  newLocX = numLocY + nxLowAdd + nxHighAdd
  newLocY = numLocX + nyLowAdd + nyHighAdd
  newLocZ = numLocZ + nzLowAdd + nzHighAdd
  numLocTot = newLocX * newLocY * newLocZ
  allocate (aLoc(3, 3, numLocTot), dxyzLoc(3, numLocTot), solved(numLocTot), stat = ierr)
  if (ierr .ne. 0) call exitError( &
      'Allocating memory for transformation arrays')

  do i = 1, numLocTot
    solved(i) = .false.
  enddo
  call shiftTransforms(alocTemp, dlocTemp, solveTemp, numLocY, numLocX, numLocZ, &
      aLoc, dxyzLoc, solved, newLocX, newLocY, newLocZ, nxLowAdd, nyLowAdd, &
      nzLowAdd)
  xLocStart = xLocStart - nxLowAdd * dxLoc
  yLocStart = yLocStart - nyLowAdd * dyLoc
  zLocStart = zLocStart - nzLowAdd * dzloc
  numLocY = newLocX
  numLocX = newLocY
  numLocZ = newLocZ
  deallocate (alocTemp, dlocTemp, solveTemp, stat = ierr) ;
  !
  ! Fill in missing transforms by extrapolation and weighted averaging
  call fillInTransforms(aLoc, dxyzLoc, solved, numLocY, numLocX, numLocZ, &
      dxLoc, dyLoc, dzloc)
  if (fillFile .ne. ' ') then
    !
    ! Output file of filled in transforms
    call dopen(1, fillFile, 'new', 'f')
    write(1, 104) numLocY, numLocX, numLocZ, xLocStart, yLocStart, zLocStart, dxLoc, dyLoc, dzloc
104 format(i5,2i6,3f11.2,3f10.4)
    do iz = 1, numLocZ
      do iy = 1, numLocX
        do ix = 1, numLocY
          l = ix + (iy - 1) * numLocY + (iz - 1) * numLocY * numLocX
          write(1, 103) xLocStart + (ix - 1) * dxLoc, yLocStart + (iy - 1) * dyLoc, &
              zLocStart + (iz - 1) * dzloc
103       format(3f9.1)
          write(1, 102) ((aLoc(i, j, l), j = 1, 3), dxyzLoc(i, l), i = 1, 3)
102       format(3f10.6,f10.3)
        enddo
      enddo
    enddo
    close(1)
  endif
  if (patchFile .ne. ' ') then
    !
    ! Output patch file of inverse vectors for positions in output volume
    call dopen(1, patchFile, 'new', 'f')
    write(1, '(i8,a)') numLocY * numLocX * numLocZ, ' positions'
    do iz = 1, numLocZ
      do iy = 1, numLocX
        do ix = 1, numLocY
          l = ix + (iy - 1) * numLocY + (iz - 1) * numLocY * numLocX
          xp = xLocStart + (ix - 1) * dxLoc
          yp = yLocStart + (iy - 1) * dyLoc
          zp = zLocStart + (iz - 1) * dzloc
          dx = aLoc(1, 1, l) * xp + aLoc(1, 2, l) * yp + aLoc(1, 3, l) * zp + &
              dxyzLoc(1, l) - xp
          dy = aLoc(2, 1, l) * xp + aLoc(2, 2, l) * yp + aLoc(2, 3, l) * zp + &
              dxyzLoc(2, l) - yp
          dz = aLoc(3, 1, l) * xp + aLoc(3, 2, l) * yp + aLoc(3, 3, l) * zp + &
              dxyzLoc(3, l) - zp
          indX = nint(xp + nxOut / 2.)
          indY = nint(yp + nyOut / 2.)
          indZ = nint(zp + nzOut / 2.)
          write(1, 105) indX, indY, indZ, dx, dy, dz
105       format(3i8,3f12.2)
        enddo
      enddo
    enddo
    close(1)
  endif
  !
  ! Exit if patch or warp output files
  if (patchFile .ne. ' ' .or. fillFile .ne. ' ') call exit(0)
  !
  ! Get mean inverse transform
  aFwd = 0.
  do ja = 1, numLocTot
    call inv_matrix(aLoc(1, 1, ja), aInv)
    aFwd = aFwd + aInv / numLocTot
  enddo
  !
  ! DNM 7/26/02: transfer pixel spacing to same axes
  !
  call iiuRetDelta(5, delta)
  do i = 1, 3
    cell(i) = nxyzOut(i) * delta(i)
    cell(i + 3) = 90.
    cxyzOut(i) = nxyzOut(i) / 2.
  enddo
  !
  ! Unless one layer in Y, allow 5% of memory for transforms
  if (numLocX > 1) memoryLim = 0.95 * memoryLim
  !
  ! Get provisional setup of cubes then find actual limits of input
  ! cubes with this setup - loop until no new extra pixels needed
  !
  numExtra = 0
  newExtra = 1
  do while (newExtra > 0)

    call setup_cubes_scratch(aFwd, aLoc, numLocTot, numExtra, ' ', tempDir, &
        tempExt, tim, .true.)
    !
    newExtra = 0
    do izCube = 1, ncubes(3)
      do ixCube = 1, ncubes(1)
        do iyCube = 1, ncubes(2)
          call testCubeFaces(aLoc, dxyzLoc, xLocStart, dxLoc, yLocStart, dyLoc, &
              zLocStart, dzloc, numLocY, numLocX, numLocZ, ixCube, iyCube, izCube, &
              newExtra, inMin, inMax)
        enddo
      enddo
    enddo
    numExtra = numExtra + newExtra
  enddo
  !
  print *,numExtra, ' extra pixels needed in cubes for final setup'
  !
  ! Get setup again for real this time and open output file after
  ! all potential errors are past
  !
  call imopen(6, fileOut, 'NEW')
  call setup_cubes_scratch(aFwd, aLoc, numLocTot, numExtra, filein, tempDir, &
      tempExt, tim, .true.)
  !
  ! Set up axes and strides and allocate the array for transforms
  iy = idimOut(2)
  if (numLocX == 1 .and. (numLocZ > 1 .or. ioutXaxis == 3)) iy = 1
  if (ioutXaxis == 3) then
    innerAxis = 3
    iouterAxis = 1
    innerStride = idimOut(1) * idimOut(2)
    iouterStride = 1
  else
    innerAxis = 1
    iouterAxis = 3
    innerStride = 1
    iouterStride = idimOut(1) * idimOut(2)
  endif
  ix = idimOut(innerAxis)
  allocate (offsetIn(3, ix, iy), aPix(3, 3, ix, iy), stat = ierr)
  ! print *,'allocated', ix, iy, innerAxis, iouterAxis, idimOut
  if (ierr .ne. 0) call exitError( &
      'Failed to allocate arrays for precomputed transforms')
  !
  !
  call iiuCreateHeader(6, nxyzOut, nxyzOut, mode, title, 0)
  call iiuAltCell(6, cell)
  call iiuTransLabels(6, 5)
  write(titleCh, 302) dat, tim
  read(titleCh, '(20a4)') (title(kti), kti = 1, 20)
302 format('WARPVOL: 3-D warping of tomogram:',t57,a9,2x,a8)
  dmin = 1.e20
  dmax = -dmin
  tsum = 0.
  numDone = 0
  call iiuRetTilt(5, tiltOld)
  call iiuAltTilt(6, tiltOld)
  !
  ! loop on layers of cubes in Z, do all I/O to complete layer
  !
  izSec(6) = 0
  do izCube = 1, ncubes(3)
    icube(3) = izCube
    !
    ! initialize files and counters
    !
    do i = 1, 4
      if (needScratch(i)) call iiuSetPosition(i, 0, 0)
      izSec(i) = 0
    enddo
    !
    ! loop on the cubes in the layer
    !
    do indOuter = 1, limOuter
      do indInner = 1, limInner
        cpuStart = cpuTime()
        wallStart = wallTime()
        call cubeIndexes(ioutXaxis, indInner, indOuter, ixCube, iyCube)
        icube(1) = ixCube
        icube(2) = iyCube
        newExtra = -1
        call testCubeFaces(aLoc, dxyzLoc, xLocStart, dxLoc, yLocStart, dyLoc, zLocStart, &
            dzloc, numLocY, numLocX, numLocZ, ixCube, iyCube, izCube, newExtra, &
            inMin, inMax)
        ifEmpty = 0
        do i = 1, 3
          if (inMin(i) > inMax(i)) ifEmpty = 1
        enddo
        !
        ! load the input cube
        if (ifEmpty == 0) then
          if (iVerbose > 1) print *,ixCube, iyCube, izCube, inMin(3), &
              inMax(3), inMax(3) + 1 - inMin(3)
          do iz = inMin(3), inMax(3)
            call iiuSetPosition(5, iz, 0)
            if (iVerbose > 1) then
              write(*,'(10i5)') ixCube, iyCube, izCube, iz, iz + 1 - inMin(3) &
                  , inMin(1), inMax(1), inMin(2), inMax(2)
              call flush(6)
            endif
            call irdpas(5, array(1, 1, iz + 1 - inMin(3)), inputDim(1), inputDim(2), &
                inMin(1), inMax(1), inMin(2), inMax(2),*99)
          enddo
        endif
        !
        ! prepare offsets and limits
        xOffsOut = ixyzCube(innerAxis, icube(innerAxis)) - 1 - cxyzOut(innerAxis)
        ixLimit = inMax(1) + 1 - inMin(1)
        iyLimit = inMax(2) + 1 - inMin(2)
        izLimit = inMax(3) + 1 - inMin(3)
        !
        ! If one layer of cubes in Z, precompute all transforms because it
        ! is invariant in Z
        if (ifEmpty == 0 .and. numLocZ == 1 .and. ioutXaxis .ne. 3) then
          if (iVerbose > 0) print *,'Precomputing for layer'
          zCen = ixyzCube(3, izCube) + czOut
          do iy = 1, nxyzCube(2, iyCube)
            yCen = ixyzCube(2, iyCube) + iy - 1 - cyOut
            do ix = 1, nxyzCube(1, ixCube)
              xCen = ix + xOffsOut
              call interpInv(aLoc, dxyzLoc, xLocStart, dxLoc, yLocStart, &
                  dyLoc, zLocStart, dzloc, numLocY, numLocX, numLocZ, &
                  xCen, yCen, zCen, aPix(1, 1, ix, iy), offsetIn(1, ix, iy))
              do i = 1, 3
                offsetIn(i, ix, iy) = offsetIn(i, ix, iy) + 1 + nxyzIn(i) / 2. -inMin(i)
              enddo
            enddo
          enddo
        endif
        !
        ! loop over Z segments of the output cube (multiple slices in case
        ! where the X axis is being output as the Z axis)
        izStart = 1
        numZleft = nxyzCube(3, izCube)
        do while (numZleft > 0)
          numZtoDo = min(numZleft, maxZout)
          izEnd = izStart + numZtoDo - 1
          ! print *,ixcube, iycube, izStart, izEnd, numZleft
          if (ifEmpty == 0) then

            ! Set up index limits for inner and outer loops
            if (ioutXaxis == 3) then
              innerStart = izStart
              innerEnd = izEnd
              iouterStart = 1
              iouterEnd = nxyzCube(1, ixCube)
            else
              innerStart = 1
              innerEnd = nxyzCube(1, ixCube)
              iouterStart = izStart
              iouterEnd = izEnd
            endif
            indBase = -idimOut(1) - izStart * idimOut(1) * idimOut(2)
            ! print *,iouterStart, iouterEnd, innerStart, innerEnd
            !
            ! Loop on the outer axis in the Z segment
            do iouter = iouterStart, iouterEnd
              xyzCen(iouterAxis) = ixyzCube(iouterAxis, icube(iouterAxis)) + &
                  iouter - 1 - cxyzOut(iouterAxis)
              outerCen = xyzCen(iouterAxis)
              !
              ! get matrices for each inner position either for the one
              ! position in Y or for every one
              if (.not.(numLocZ == 1 .and. ioutXaxis .ne. 3)) then
                iyxf = nxyzCube(2, iyCube)
                if (numLocX == 1) iyxf = 1
                do iy = 1, iyxf
                  xyzCen(2) = ixyzCube(2, iyCube) + iy - 1 - cyOut
                  do ix = innerStart, innerEnd
                    xyzCen(innerAxis) = ix + xOffsOut
                    call interpInv(aLoc, dxyzLoc, xLocStart, dxLoc, yLocStart, dyLoc, &
                        zLocStart, dzloc, numLocY, numLocX, numLocZ, xyzCen(1), &
                        xyzCen(2), xyzCen(3), aPix(1, 1, ix, iy), offsetIn(1, ix, iy))
                    do i = 1, 3
                      offsetIn(i, ix, iy) = offsetIn(i, ix, iy) + 1 + nxyzIn(i) / 2. - &
                          inMin(i)
                    enddo
                  enddo
                enddo
              endif
              threadWall = wallTime()
              numThreads = numOMPthreads(8)
              !
              ! parallelize loop on Y and inner axes
              !
              !$OMP PARALLEL DO NUM_THREADS(numThreads) &
              !$OMP& SHARED(nxyzCube, ixyzCube, outerCen, innerStart, innerEnd) &
              !$OMP& SHARED(iyCube, cyOut, baseInt, indBase, innerStride, iouter) &
              !$OMP& SHARED(interpOrder, xOffsOut, dmeanIn, array, brray, ixLimit) &
              !$OMP& SHARED(aPix, offsetIn, numLocY, iouterStride, idimOut, innerAxis) &
              !$OMP& SHARED(iouterAxis, iyLimit, izLimit, numLocX) &
              !$OMP& DEFAULT(PRIVATE)
              do iy = 1, nxyzCube(2, iyCube)
                yCen = ixyzCube(2, iyCube) + iy - 1 - cyOut
                iyxf = iy
                if (numLocX == 1) iyxf = 1
                do ix = innerStart, innerEnd
                  xCen = ix + xOffsOut
                  !
                  ! get indices in array of input data
                  xp = aPix(1, innerAxis, ix, iyxf) * xCen + aPix(1, 2, ix, iyxf) * yCen+&
                      aPix(1, iouterAxis, ix, iyxf) * outerCen + offsetIn(1, ix, iyxf)
                  yp = aPix(2, innerAxis, ix, iyxf) * xCen + aPix(2, 2, ix, iyxf) * yCen+&
                      aPix(2, iouterAxis, ix, iyxf) * outerCen + offsetIn(2, ix, iyxf)
                  zp = aPix(3, innerAxis, ix, iyxf) * xCen + aPix(3, 2, ix, iyxf) * yCen+&
                      aPix(3, iouterAxis, ix, iyxf) * outerCen + offsetIn(3, ix, iyxf)
                  bval = dmeanIn
                  !
                  ! do generalized evaluation of whether pixel is doable
                  !
                  ixp = int(xp + baseInt)
                  iyp = int(yp + baseInt)
                  izp = int(zp + baseInt)
                  if (ixp >= 1 .and. ixp <= ixLimit .and. &
                      iyp >= 1 .and. iyp <= iyLimit .and. &
                      izp >= 1 .and. izp <= izLimit) then
                    dx = xp - ixp
                    dy = yp - iyp
                    dz = zp - izp
                    ixpP1 = min(ixLimit, ixp + 1)
                    iypP1 = min(iyLimit, iyp + 1)
                    izpP1 = min(izLimit, izp + 1)
                    !
                    if (interpOrder >= 2) then
                      ixpM1 = max(1, ixp - 1)
                      iypM1 = max(1, iyp - 1)
                      izpm1 = max(1, izp - 1)
                      !
                      ! Set up terms for quadratic interpolation
                      ! No longer omit higher-order terms, and no longer limit value to
                      ! min/max of values used for interp
                      dxSq = dx**2
                      dySq = dy**2
                      dzSq = dz**2
                      fx = 1. - dxSq
                      fxM1 = 0.5 * (dxSq - dx)
                      fxP1 = fxM1 + dx
                      fy = 1. - dySq
                      fyM1 = 0.5 * (dySq - dy)
                      fyP1 = fyM1 + dy
                      fz = 1. - dzSq
                      fzM1 = 0.5 * (dzSq - dz)
                      fzP1 = fzM1 + dz

                      bval = fzM1 * (fyM1 * (fxM1 * array(ixpM1, iypM1, izpM1) + &
                          fx * array(ixp, iypM1, izpM1) + &
                          fxP1 * array(ixpP1, iypM1, izpM1)) + &
                          fy * (fxM1 * array(ixpM1, iyp, izpM1) + &
                          fx * array(ixp, iyp, izpM1) + &
                          fxP1 * array(ixpP1, iyp, izpM1)) +  &
                          fyP1 * (fxM1 * array(ixpM1, iypP1, izpM1) + &
                          fx * array(ixp, iypP1, izpM1) + &
                          fxP1 * array(ixpP1, iypP1, izpM1))) + &
                          fz * (fyM1 * (fxM1 * array(ixpM1, iypM1, izp) + &
                          fx * array(ixp, iypM1, izp) + &
                          fxP1 * array(ixpP1, iypM1, izp)) + &
                          fy * (fxM1 * array(ixpM1, iyp, izp) + &
                          fx * array(ixp, iyp, izp) + &
                          fxP1 * array(ixpP1, iyp, izp)) +  &
                          fyP1 * (fxM1 * array(ixpM1, iypP1, izp) + &
                          fx * array(ixp, iypP1, izp) + &
                          fxP1 * array(ixpP1, iypP1, izp))) + &
                          fzP1 * (fyM1 * (fxM1 * array(ixpM1, iypM1, izpP1) + &
                          fx * array(ixp, iypM1, izpP1) + &
                          fxP1 * array(ixpP1, iypM1, izpP1)) + &
                          fy * (fxM1 * array(ixpM1, iyp, izpP1) + &
                          fx * array(ixp, iyp, izpP1) + &
                          fxP1 * array(ixpP1, iyp, izpP1)) +  &
                          fyP1 * (fxM1 * array(ixpM1, iypP1, izpP1) + &
                          fx * array(ixp, iypP1, izpP1) + &
                          fxP1 * array(ixpP1, iypP1, izpP1)))
                    else
                      !
                      ! Set up terms for linear interpolation
                      !
                      d11 = (1. - dx) * (1. - dy)
                      d12 = (1. - dx) * dy
                      d21 = dx * (1. - dy)
                      d22 = dx * dy
                      bval = (1. - dz) * (d11 * array(ixp, iyp, izp) &
                          + d12 * array(ixp, iypP1, izp) &
                          + d21 * array(ixpP1, iyp, izp) &
                          + d22 * array(ixpP1, iypP1, izp)) + &
                          dz * (d11 * array(ixp, iyp, izpP1) &
                          + d12 * array(ixp, iypP1, izpP1) &
                          + d21 * array(ixpP1, iyp, izpP1) &
                          + d22 * array(ixpP1, iypP1, izpP1))
                    endif
                  endif
                  brray(indBase + ix * innerStride + iy * idimOut(1) + &
                      iouter * iouterStride) = bval
                enddo
              enddo
              !$OMP END PARALLEL DO
              wallCum = wallCum + wallTime() - threadWall
            enddo
          else
            brray(1 : idimOut(1) * idimOut(2) * numZtoDo) = dmeanIn
          endif

          call writeOneCube(ixCube, iyCube, numZtoDo, izStart, izSec, dmin, dmax, tsum)
          !
          ! End of do while: update start and number left to do
          izStart = izEnd + 1
          numZleft = numZleft - numZtoDo
        enddo
        ! print *,ixcube, iycube, izcube
        numDone = numDone + 1
        if (iVerbose > 0) write(*,'(a,f10.4,a,f10.4)') 'Cube CPU time:', &
            cpuTime() - cpuStart, '   Wall time:', wallTime() - wallStart
        write(*,'(a,i6,a,i6)') 'Finished', numDone, ' of', &
            ncubes(1) * ncubes(2) * ncubes(3)
        call flush(6)
      enddo
    enddo
    !
    ! whole layer of cubes in z is done.  now reread and compose one
    ! row of the output section at a time in array
    !
    call recompose_cubes(izCube, dmin, dmax, tsum)
    if (chunkedHDF) izSec(6) = izSec(6) + izEnd
  enddo
  !
  if (iVerbose > 0) write(*,'(a,f10.4)') 'Thread wall time ', wallCum
  dmean = tsum / nzOut
  call iiuWriteHeader(6, title, 1, dmin, dmax, dmean)
  do i = 1, 4
    if (needScratch(i)) call iiuClose(i)
  enddo
  call iiuClose(5)
  call iiuClose(6)
  call exit(0)
98 write(*,'(/,a,i7)') 'ERROR: WARPVOL - READING WARP FILE AT POSITION', &
      lForErr
  call exit(1)
99 call exitError('Reading image file')
end program warpvol

!
! TESTCUBEFACES tests the faces of an output cube at its corners and
! at all positions corresponding to transforms
! This was a nice internal subroutine but gfortran would not compile it
! with openmp enabled
!
subroutine testCubeFaces(aLoc, dxyzLoc, xLocStart, dxLoc, yLocStart, dyLoc, zLocStart, &
    dzloc, numLocY, numLocX, numLocZ, ixCube, iyCube, izCube, newExtraArg, inMin, &
    inMax)
  use rotmatwarp
  implicit none
  integer*4 newExtraArg, numLocY, numLocX, numLocZ, inMin(3), inMax(3)
  real*4 aLoc(3,3,*), dxyzLoc(3,*), xLocStart, dxLoc, yLocStart, dyLoc, zLocStart, dzloc
  real*4 xCubeLow, xCubeHigh, yCubeLow, yCubeHigh, zCubeLow, zCubeHigh
  integer*4 ixCube, iyCube, izCube
  integer*4 ixLow, ixHigh, iyLow, iyHigh, izLow, izHigh
  real*4 dxUse, dyUse, dzUse, xLocUse, ylocUse, zlocUse, dxyz(3), amInv(3,3)
  integer*4 jfx, jfy, jfz, jval, i
  real*4 xCen, yCen, zCen
  !
  ! back-transform the faces of the output cube at the corners and
  ! at positions corresponding to transforms to
  ! find the limiting index coordinates of the input cube
  !
  do i = 1, 3
    inMin(i) = 100000
    inMax(i) = -100000
  enddo
  call cubeTestLimits(ixyzCube(2, iyCube), nxyzCube(2, iyCube), cyOut, &
      numLocX, yLocStart, dyLoc, yCubeLow, yCubeHigh, iyLow, iyHigh, ylocUse, dyUse)
  call cubeTestLimits(ixyzCube(3, izCube), nxyzCube(3, izCube), czOut, &
      numLocZ, zLocStart, dzloc, zCubeLow, zCubeHigh, izLow, izHigh, zlocUse, dzUse)
  call cubeTestLimits(ixyzCube(1, ixCube), nxyzCube(1, ixCube), cxOut, &
      numLocY, xLocStart, dxLoc, xCubeLow, xCubeHigh, ixLow, ixHigh, xLocUse, dxUse)
  do jfx = ixLow, ixHigh
    do jfy = iyLow, iyHigh
      do jfz = izLow, izHigh
        if (jfx == ixLow .or. jfx == ixHigh .or. jfy == iyLow .or. &
            jfy == iyHigh .or. jfz == izLow .or. jfz == izHigh) &
            then
          xCen = max(xCubeLow, min(xCubeHigh, xLocUse + jfx * dxUse))
          yCen = max(yCubeLow, min(yCubeHigh, ylocUse + jfy * dyUse))
          zCen = max(zCubeLow, min(zCubeHigh, zlocUse + jfz * dzUse))
          call interpInv(aLoc, dxyzLoc, xLocStart, dxLoc, yLocStart, dyLoc, &
              zLocStart, dzloc, numLocY, numLocX, numLocZ, xCen, yCen, &
              zCen, amInv, dxyz)
          do i = 1, 3
            jval = nint(amInv(i, 1) * xCen + amInv(i, 2) * yCen + &
                amInv(i, 3) * zCen + dxyz(i) + nxyzIn(i) / 2.)
            inMin(i) = max(0, min(inMin(i), jval - 2))
            inMax(i) = min(nxyzIn(i) - 1, max(inMax(i), jval + 2))
            !
            ! See if any extra pixels are needed in input
            !
            ! if (newExtrarg >= 0 .and. newExtrarg <inmax(i) + &
            ! 1-inmin(i) -inpdim) then
            ! print *,ixcube, iycube, izcube, xcen, ycen, zcen, &
            ! inmax(i) + 1 - inmin(i) - &
            ! inpdim, i, jval, inmin(i), inmax(i)
            ! print *,ainv(i, 1), ainv(i, 2), ainv(i, 3), dxyz(i)
            ! endif
            if (newExtraArg >= 0) newExtraArg = max(newExtraArg, &
                inMax(i) + 1 - inMin(i) - inputDim(i))
          enddo
        endif
      enddo
    enddo
  enddo
end subroutine testCubeFaces

!
! CUBETESTLIMITS determines limits for cube testing on one axis
!
subroutine cubeTestLimits(icube, ncube, cout, nunmLoc, startLoc, dxyzLoc, cubeLow, &
    cubeHigh, ilo, ihi, startLocUse, dlocUse)
  implicit none
  integer*4 icube, ncube, nunmLoc, ilo, ihi
  real*4 cout, startLoc, dxyzLoc, cubeLow, cubeHigh, startLocUse, dlocUse
  cubeLow = icube - cout
  cubeHigh = icube + ncube - cout
  if (nunmLoc > 1) then
    ilo = floor((cubeLow - startLoc) / dxyzLoc)
    ihi = ceiling((cubeHigh - startLoc) / dxyzLoc)
    startLocUse = startLoc
    dlocUse = dxyzLoc
  else
    startLocUse = cubeLow
    dlocUse = (cubeHigh - cubeLow) / 2.
    ilo = 0
    ihi = 2
  endif
  return
end subroutine cubeTestLimits

!
! INTERPINV takes the array of transforms and a given position
! xcen, ycen, zcen and determines the interpolated transform minv, dxyz
!
subroutine interpInv(aLoc, dxyzLoc, xLocStart, dxLoc, yLocStart, dyLoc, zLocStart, &
    dzloc, numLocY, numLocX, numLocZ, xCen, yCen, zCen, aInv, dxyz)
  implicit none
  integer*4 numLocY, numLocX, numLocZ
  real*4 aLoc(3,3,numLocY,numLocX,numLocZ), dxyzLoc(3,numLocY,numLocX,numLocZ)
  real*4 aInv(3,3), dxyz(3), xLocStart, dxLoc, yLocStart, dyLoc, zLocStart, dzloc
  real*4  xCen, yCen, zCen
  real*4 x, fx, fx1, y, fy, fy1, z, fz, fz1
  integer*4 ix, ix1, iy, iy1, iz, iz1, i, j
  !
  if (numLocY > 1) then
    x = min(float(numLocY), max(1., 1. +(xCen - xLocStart) / dxLoc))
    ix = x
    ix1 = min(ix + 1, numLocY)
    fx1 = x - ix
    fx = 1. -fx1
  else
    ix = 1
    ix1 = 1
    fx1 = 0.
    fx = 1.
  endif
  !
  if (numLocX > 1) then
    y = min(float(numLocX), max(1., 1. +(yCen - yLocStart) / dyLoc))
    iy = y
    iy1 = min(iy + 1, numLocX)
    fy1 = y - iy
    fy = 1. -fy1
  else
    iy = 1
    iy1 = 1
    fy1 = 0.
    fy = 1.
  endif
  !
  if (numLocZ > 1) then
    z = min(float(numLocZ), max(1., 1. +(zCen - zLocStart) / dzloc))
    iz = z
    iz1 = min(iz + 1, numLocZ)
    fz1 = z - iz
    fz = 1. -fz1
  else
    iz = 1
    iz1 = 1
    fz1 = 0.
    fz = 1.
  endif
  !
  do i = 1, 3
    do j = 1, 3
      aInv(i, j) = fy * (fz * (fx * aLoc(i, j, ix, iy, iz) + &
          fx1 * aLoc(i, j, ix1, iy, iz)) + &
          fz1 * (fx * aLoc(i, j, ix, iy, iz1) + &
          fx1 * aLoc(i, j, ix1, iy, iz1))) + &
          fy1 * (fz * (fx * aLoc(i, j, ix, iy1, iz) + &
          fx1 * aLoc(i, j, ix1, iy1, iz)) + &
          fz1 * (fx * aLoc(i, j, ix, iy1, iz1) + &
          fx1 * aLoc(i, j, ix1, iy1, iz1)))
    enddo
    dxyz(i) = fy * (fz * (fx * dxyzLoc(i, ix, iy, iz) + &
        fx1 * dxyzLoc(i, ix1, iy, iz)) + &
        fz1 * (fx * dxyzLoc(i, ix, iy, iz1) + &
        fx1 * dxyzLoc(i, ix1, iy, iz1))) + &
        fy1 * (fz * (fx * dxyzLoc(i, ix, iy1, iz) + &
        fx1 * dxyzLoc(i, ix1, iy1, iz)) + &
        fz1 * (fx * dxyzLoc(i, ix, iy1, iz1) + &
        fx1 * dxyzLoc(i, ix1, iy1, iz1)))
  enddo
  return
end subroutine interpInv

! FILLINTRANSFORMS fills in a regular array of transforms by
! extrapolation from the nearest transforms to each missing one, where
! the extrapolation is a weighted mean with weights proportional to the
! square of distance from each existing transform.
!
subroutine fillInTransforms(aLoc, dxyzLoc, solved, numLocY, numLocX, numLocZ, &
    dxLoc, dyLoc, dzloc)
  implicit none
  integer*4 numLocY, numLocX, numLocZ, ix, iy, iz, jx, jy, jz, minX, minY, minZ
  real*4 aLoc(3,3,numLocY,numLocX,numLocZ), dxyzLoc(3,numLocY,numLocX,numLocZ)
  logical*4 solved(numLocY,numLocX,numLocZ), boundary
  real*4 dxLoc, dyLoc, dzloc, dist, dmin, angle
  real*4 dx, dy, dz, dxyz(3), wsum, distCrit
  equivalence (dx, dxyz(1)), (dy, dxyz(2)), (dz, dxyz(3))
  integer*4 kx, ky, neighX, neighY, neighZ, indyz, iyStrideFac, izStrideFac, is, indDom
  integer*4 jxMin, jxMax, jyMin, jyMax, jzMin, jzMax
  integer*4 ixStep(12) /1, 1, 1, 0, -1, -1, -1, 0, 1, 1, 1, 0/
  integer*4 iyStep(12) /-1, 0, 1, 1, 1, 0, -1, -1, -1, 0, 1, 1/
  real*4 range/2./
  real*4 atan2d
  !
  do iz = 1, numLocZ
    do iy = 1, numLocX
      do ix = 1, numLocY
        dmin = 1.e30
        if (.not.solved(ix, iy, iz)) then
          !
          ! Find closest position with transform
          do jz = 1, numLocZ
            do jy = 1, numLocX
              do jx = 1, numLocY
                if (solved(jx, jy, jz)) then
                  dist = ((jx - ix) * dxLoc)**2 + ((jy - iy) * dyLoc)**2 + &
                      ((jz - iz) * dzloc)**2
                  if (dist < dmin) then
                    dmin = dist
                    minX = jx
                    minY = jy
                    minZ = jz
                  endif
                endif
              enddo
            enddo
          enddo
          !
          ! zero out the sum
          do jx = 1, 3
            dxyzLoc(jx, ix, iy, iz) = 0.
            do jy = 1, 3
              aLoc(jx, jy, ix, iy, iz) = 0.
            enddo
          enddo
          wsum = 0.
          ! write(*,'(a,3i4,a,3i4,f8.2)') 'Filling in', ix, iy, iz, '  nearest', &
          ! minx, miny, minz, sqrt(dmin)
          !
          ! Get actual distance to look, range of indexes to search, and
          ! the criterion which is square of maximum distance
          dist = range * sqrt(dmin)
          jxMin = max(1, nint(ix - dist / max(dxLoc, 1.) - 1))
          jxMax = min(numLocY, nint(ix + dist / max(dxLoc, 1.) + 1))
          jyMin = max(1, nint(iy - dist / max(dyLoc, 1.) - 1))
          jyMax = min(numLocX, nint(iy + dist / max(dyLoc, 1.) + 1))
          jzMin = max(1, nint(iz - dist / max(dzloc, 1.) - 1))
          jzMax = min(numLocZ, nint(iz + dist / max(dzloc, 1.) + 1))
          distCrit = dist**2
          ! write(*,'(3i6)') jxmin, jxmax, jymin, jymax, jzmin, jzmax
          !
          ! Get dominant index for second dimension
          indyz = 2
          iyStrideFac = 1
          izStrideFac = 0
          if (numLocZ > numLocX) then
            indyz = 3
            izStrideFac = 1
            iyStrideFac = 0
          endif
          !
          ! Loop in the neighborhood, find boundary points within range
          do jz = jzMin, jzMax
            do jy = jyMin, jyMax
              do jx = jxMin, jxMax
                if (solved(jx, jy, jz)) then
                  dx = (jx - ix) * dxLoc
                  dy = (jy - iy) * dyLoc
                  dz = (jz - iz) * dzloc
                  dist = dx**2 + dy**2 + dz**2
                  if (dist <= distCrit) then
                    !
                    ! Find dominant direction to the point
                    angle = atan2d(dxyz(indyz), dx) + 157.5
                    if (angle < 0) angle = angle + 360.
                    indDom = angle / 45. + 1.
                    indDom = max(1, min(8, indDom))
                    !
                    ! Check that this point is a boundary, i.e. does not
                    ! have a neighbor in any one of the 5 directions toward
                    ! or at right angles to the dominant direction
                    boundary = .false.
                    do is = indDom, indDom + 4
                      neighX = jx + ixStep(is)
                      neighY = jy + iyStrideFac * iyStep(is)
                      neighZ = jz + izStrideFac * iyStep(is)
                      if (neighX >= 1 .and. neighX <= numLocY .and. &
                          neighY >= 1 .and. neighY <= numLocX .and. &
                          neighZ >= 1 .and. neighZ <= numLocZ) then
                        if (.not. solved(neighX, neighY, neighZ)) boundary = .true.
                      endif
                    enddo
                    !
                    ! For boundary or min point, add to weighted sum
                    if (boundary .or. (jx == minX .and. jy == &
                        minY .and. jz == minZ)) then
                      ! write(*,'(a,3i4,a,f7.1,i4,f12.8)') 'Adding in', jx, jy, &
                      ! jz, ' angle, dir', angle, indDom, 1./dist
                      do kx = 1, 3
                        dxyzLoc(kx, ix, iy, iz) = dxyzLoc(kx, ix, iy, iz) + &
                            dxyzLoc(kx, jx, jy, jz) / dist
                        do ky = 1, 3
                          aLoc(kx, ky, ix, iy, iz) = aLoc(kx, ky, ix, iy, iz) + &
                              aLoc(kx, ky, jx, jy, jz) / dist
                        enddo
                      enddo
                      wsum = wsum + 1. / dist
                    endif
                  endif
                endif
              enddo
            enddo
          enddo
          !
          ! divide by weight sum
          ! write (*,'(a,f12.8)') 'wsum=', wsum
          do jx = 1, 3
            dxyzLoc(jx, ix, iy, iz) = dxyzLoc(jx, ix, iy, iz) / wsum
            do jy = 1, 3
              aLoc(jx, jy, ix, iy, iz) = aLoc(jx, jy, ix, iy, iz) / wsum
            enddo
            ! write(*,'(3f9.5,f9.2)') (aloc(jx, jy, ix, iy, iz), jy = 1, 3), &
            ! dloc(jx, ix, iy, iz)
          enddo
        endif
      enddo
    enddo
  enddo
  return
end subroutine fillInTransforms

!
! SHIFTTRANSFORMS shifts the transforms to fill the center of a larger
! array
!
subroutine shiftTransforms(alocIn, dlocIn, solveTemp, numLocY, numLocX, numLocZ, &
    aLoc, dxyzLoc, solved, newLocX, newLocY, newLocZ, nxAdd, nyAdd, nzAdd)
  implicit none
  integer*4 numLocY, numLocX, numLocZ, newLocX, newLocY, newLocZ
  integer*4 nxAdd, nyAdd, nzAdd
  logical*4 solveTemp(numLocY, numLocX, numLocZ), solved(newLocX, newLocY, newLocZ)
  real*4 alocIn(3,3,numLocY, numLocX, numLocZ), dlocIn(3,numLocY, numLocX, numLocZ)
  real*4 aLoc(3,3,newLocX,newLocY,newLocZ), dxyzLoc(3,newLocX,newLocY,newLocZ)
  integer*4 ix, iy, iz, ixn, iyn, izn, jx, jy
  do iz = numLocZ, 1, -1
    izn = iz + nzAdd
    do iy = numLocX, 1, -1
      iyn = iy + nyAdd
      do ix = numLocY, 1, -1
        if (solveTemp(ix, iy, iz)) then
          ixn = ix + nxAdd
          solved(ixn, iyn, izn) = .true.
          do jy = 1, 3
            dxyzLoc(jy, ixn, iyn, izn) = dlocIn(jy, ix, iy, iz)
            do jx = 1, 3
              aLoc(jx, jy, ixn, iyn, izn) = alocIn(jx, jy, ix, iy, iz)
            enddo
          enddo
        endif
      enddo
    enddo
  enddo
  return
end subroutine shiftTransforms
