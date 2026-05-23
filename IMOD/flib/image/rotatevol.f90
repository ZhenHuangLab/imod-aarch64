! * * * * ROTATEVOL * * * * *
!
! ROTATEVOL will rotate all or part of a three-dimension volume
! of data.  The rotations may be by any angles about the three axes.
! Tilt angles and origin information in the header are properly
! maintained so that the new data stack will have a coordinate system
! congruent with the old one.
!
! The program can work on an arbitrarily large volume.  It reconstructs
! a series of sub-regions of the output volume, referred to as cubes
! but actually rectangles. For each cube, it reads into memory a cube
! from the input volume that contains all of the image area that rotates
! into that cube of output volume.  It then uses linear or triquadrati
! interpolation to find each pixel of the output cube, and writes the
! cube to a scratch file.  When all of the cubes in one layer are done,
! it reads back data from the scratch files and assembles each section
! in that layer.
!
! See man page for more details
!
! $Id$
!
program rotatevol
  use rotmatwarp
  implicit none
  real*4 cell(6)
  integer*4 mxyzIn(3), maxDim(3)
  real*4 centerIn(3)
  real*4 aFwd(3,3), aOld(3,3), aNew(3,3), aOldInv(3,3)
  real*4 angles(3), tiltOld(3), tiltNew(3), orig(3), xTemp(3), delta(3)
  !
  character*320 fileIn, fileOut, tempDir, tempExt
  !
  ! DNM 3/8/01: initialize the time in case time(tim) doesn't work
  !
  character dat * 9, tim * 8 / '00:00:00'/
  character*80 titleCh
  integer*4 i, ix, iy, kti, interpOrder, ierr, idirY, idirZ
  real*4 dminIn, dmaxIn, xCen, yCen, zCen
  logical*4 query, backRotate
  !
  logical pipInput
  integer*4 numOptArg, numNonOptArg
  integer*4 PipGetInteger, PipGetFloat
  integer*4 PipGetString, PipGetThreeIntegers, PipGetThreeFloats
  integer*4 PipGetNonOptionArg, PipGetInOutFile, PipGetLogical
  !
  ! fallbacks from ../../manpages/autodoc2man -3 2  rotatevol
  !
  integer numOptions
  parameter (numOptions = 15)
  character*(40 * numOptions) options(1)
  options(1) = &
      'input:InputFile:FN:@output:OutputFile:FN:@tempdir:TemporaryDirectory:CH:@'// &
      'size:OutputSizeXYZ:IT:@center:RotationCenterXYZ:FT:@'// &
      'angles:RotationAnglesZYX:FT:@back:BackRotate:B:@order:InterpolationOrder:I:@'// &
      'query:QuerySizeNeeded:B:@fill:FillValue:F:@chunk:ChunkSizesForHDF:IT:@'// &
      'memory:MemoryLimit:I:@verbose:VerboseOutput:I:@param:ParameterFile:PF:@'// &
      'help:usage:B:'
  !
  ! set defaults here
  !
  interpOrder = 2
  tempDir = ' '
  query = .false.
  backRotate = .false.
  !
  !
  ! Pip startup: set error, parse options, check help, set flag if used
  !
  call PipReadOrParseOptions(options, numOptions, 'rotatevol', &
      'ERROR: ROTATEVOL - ', .true., 3, 1, 1, numOptArg, &
      numNonOptArg)
  pipInput = numOptArg + numNonOptArg > 0

  if (PipGetInOutFile('InputFile', 1, 'Name of input file', fileIn) &
      .ne. 0) call exitError('No input file specified')

  if (pipInput) then
    ierr = PipGetLogical('QuerySizeNeeded', query)
    if (query) call ialprt(.false.)
  endif

  call imopen(5, fileIn, 'RO')
  call irdhdr(5, nxyzIn, mxyzIn, mode, dminIn, dmaxIn, dmeanIn)
  do i = 1, 3
    centerIn(i) = nxyzIn(i) / 2
    nxyzOut(i) = nxyzIn(i)
    angles(i) = 0.
  enddo
  !
  if (.not.query .and. PipGetInOutFile('OutputFile', 2, &
      'Name of output file', fileOut) .ne. 0) &
      call exitError('No output file specified')

  call setMemoryLimitAndHdfChunks(pipinput)
  if (pipInput) then
    ierr = PipGetString('TemporaryDirectory', tempDir)
    ierr = PipGetThreeFloats('RotationCenterXYZ', centerIn(1), &
        centerIn(2), centerIn(3))
    ierr = PipGetInteger('InterpolationOrder', interpOrder)
    ierr = PipGetInteger('VerboseOutput', iVerbose)
    ierr = PipGetThreeIntegers('OutputSizeXYZ', nxOut, nyOut, nzOut)
    ierr = PipGetThreeFloats('RotationAnglesZYX', angles(3), angles(2), &
        angles(1))
    ierr = PipGetFloat('FillValue', dmeanIn)
    ierr = PipGetLogical('BackRotate', backRotate)
  else
    !
    write(*,'(1x,a,/,a,$)') 'Enter path name of directory for '// &
        'temporary files, ', ' or Return to use current directory: '
    read(5, '(a)') tempDir
    !
    write(*,'(1x,a,$)') 'X, Y, and Z dimensions of the output file: '
    read(5,*) nxOut, nyOut, nzOut
    !
    write(*,'(1x,a,/,a,$)') 'Enter X, Y, and Z index coordinates of ' &
        //'the center of rotation', &
        '   in the input file (/ for center of file): '
    read(5,*) (centerIn(i), i = 1, 3)
    print *,'Rotations are applied in the order that you', &
        ' enter them: rotation about the', '   Z axis, then ', &
        'rotation about the Y axis, then rotation about the X axis'
    write(*,'(1x,a,$)') &
        'Rotations about Z, Y, and X axes (gamma, beta, alpha): '
    read(5,*) angles(3), angles(2), angles(1)
  endif
  !
  call PipDone()
  if (nxOut < 1 .or. nyOut < 1 .or. nzOut < 1) call exitError( &
      'A positive output size must be entered on all axes')
  !
  ! get matrices for forward and inverse rotations
  !
  if (backRotate) then
    call icalc_matrix(angles, aInv)
    call inv_matrix(aInv, aFwd)
    call icalc_angles(angles, aFwd)
  else
    call icalc_matrix(angles, aFwd)
    call inv_matrix(aFwd, aInv)
  endif
  !
  ! Compute maximum dimensions required if requested
  !
  if (query) then
    do i = 1, 3
      maxDim(i) = 0
    enddo
    do idirY = -1, 1, 2
      do idirZ = -1, 1, 2
        do i = 1, 3
          maxDim(i) = max(maxDim(i), nint(abs(aFwd(i, 1) * nxIn + &
              idirY * aFwd(i, 2) * nyIn + idirZ * aFwd(i, 3) * nzIn)))
        enddo
      enddo
    enddo
    write(*,'(3i8)') (maxDim(i), i = 1, 3)
    call exit(0)
  endif
  !
  call imopen(6, fileOut, 'NEW')
  !
  ! get true centers of index coordinate systems
  !
  call iiuRetDelta(5, delta)
  do i = 1, 3
    cxyzIn(i) = (nxyzIn(i) - 1) / 2. +centerIn(i) - nxyzIn(i) / 2
    cxyzOut(i) = (nxyzOut(i) - 1) / 2.
    cell(i) = nxyzOut(i) * delta(i)
    cell(i + 3) = 90.
  enddo
  !
  !
  call iiuCreateHeader(6, nxyzOut, nxyzOut, mode, title, 0)
  call iiuAltCell(6, cell)
  call iiuTransLabels(6, 5)
  call time(tim)
  call b3dDate(dat)
  tempExt = 'rot      1'
  !
  write(titleCh, 302) (angles(i), i = 1, 3), dat, tim
  read(titleCh, '(20a4)') (title(kti), kti = 1, 20)
302 format('ROTATEVOL: 3D rotation by angles:',3f7.1,t57,a9,2x,a8)
  !
  ! calculate new tilt angles and origin information from old
  !
  call iiuRetTilt(5, tiltOld)
  call icalc_matrix(tiltOld, aOld)
  call inv_matrix(aOld, aOldInv)
  call iiuAltTiltOrig(6, tiltOld)
  call iiuAltTilt(6, tiltOld)
  call iiuAltTiltRot(6, angles)
  call iiuRetTilt(6, tiltNew)
  call icalc_matrix(tiltNew, aNew)
  call iiuRetOrigin(5, orig(1), orig(2), orig(3))
  !
  ! Need to add 0.5 back to all these center coordinates to get
  ! it right for actual 90 degree rotation
  !
  xCen = cxIn + 0.5 - orig(1) / delta(1)
  yCen = cyIn + 0.5 - orig(2) / delta(2)
  zCen = czIn + 0.5 - orig(3) / delta(3)
  do i = 1, 3
    xTemp(i) = aOldInv(i, 1) * xCen + aOldInv(i, 2) * yCen + aOldInv(i, 3) * zCen
  enddo
  do i = 1, 3
    orig(i) = delta(i) * (cxyzOut(i) + 0.5 - &
        (aNew(i, 1) * xTemp(1) + aNew(i, 2) * xTemp(2) + aNew(i, 3) * xTemp(3)))
  enddo
  call iiuAltOrigin(6, orig(1), orig(2), orig(3))
  !
  ! Set up the arrangement of input and output data
  call setup_cubes_scratch(aFwd, aInv, 1, 0, fileIn, tempDir, tempExt, tim, &
      .false.)
  !
  ! Do all the work and exit
  call transform_cubes(interpOrder)
end program rotatevol

