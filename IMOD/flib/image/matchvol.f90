! * * * * MATCHVOL * * * * *
!
! MATCHVOL will transform a volume using a general linear
! transformation.  Its main use is to transform one tomogram from
! a two-axis tilt series so that it matches the other tomogram.
! To do so, it can combine an initial alignment transformation and
! any number of successive refining transformations.  The program
! uses the same algorithm as ROTATEVOL for rotating large volumes.
!
! See man page for more information
!
! $Id$
!
program matchvol
  use rotmatwarp
  implicit none
  real*4 cell(12), dxyzIn(3)
  real*4 mxyzIn(3), centerIn(3)
  real*4 aFwd(3,3), aOld(3,3), aNew(3,3), aOldInv(3,3)
  real*4 angles(3), tiltOld(3), tiltNew(3), origin(3), xTemp(3)
  real*4 aTmp1(3,3), aTmp2(3,3), dxyzTemp1(3), dxyzTemp2(3), delta(3)
  !
  character*320 fileIn, fileOut, tempDir, tempExt, imFileOut
  !
  ! DNM 3/8/01: initialize the time in case time(tim) doesn't work
  !
  character dat * 9, tim * 8 / '00:00:00'/
  character*80 titlech
  integer*4 i, ix, iy, kti, interpOrder, ierr, numToGet, numXFiles
  integer*4 numXLines, j, numTrans, iTrans
  real*4 dminIn, dmaxIn, devmx, xCen, yCen, zCen
  !
  logical pipInput
  integer*4 numOptArg, numNonOptArg
  integer*4 PipGetInteger, PipGetFloatArray, PipNumberOfEntries
  integer*4 PipGetString, PipGetThreeIntegers, PipGetThreeFloats
  integer*4 PipGetNonOptionArg, PipGetInOutFile
  !
  ! fallbacks from ../../manpages/autodoc2man -2 2  matchvol
  !
  integer numOptions
  parameter (numOptions = 14)
  character*(40 * numOptions) options(1)
  options(1) = &
      'input:InputFile:FN:@output:OutputFile:FN:@inverse:InverseFile:FN:@'// &
      'tempdir:TemporaryDirectory:CH:@size:OutputSizeXYZ:IT:@center:CenterXYZ:FT:@'// &
      'xffile:TransformFile:FNM:@3dxform:3DTransform:FAM:@'// &
      'order:InterpolationOrder:I:@chunk:ChunkSizesForHDF:IT:@memory:MemoryLimit:I:@'// &
      'verbose:VerboseOutput:I:@param:ParameterFile:PF:@help:usage:B:'
  !
  ! set defaults here
  !
  interpOrder = 2
  tempDir = ' '
  tempExt = 'mat      1'
  call time(tim)
  call b3dDate(dat)
  !
  ! Pip startup: set error, parse options, check help, set flag if used
  !
  call PipReadOrParseOptions(options, numOptions, 'matchvol', &
      'ERROR: MATCHVOL - ', .true., 3, 1, 1, numOptArg, &
      numNonOptArg)
  pipInput = numOptArg + numNonOptArg > 0

  if (PipGetInOutFile('InputFile', 1, 'Name of input file', fileIn) &
      .ne. 0) call exitError('No input file specified')

  call imopen(5, fileIn, 'RO')
  call irdhdr(5, nxyzIn, mxyzIn, mode, dminIn, dmaxIn, dmeanIn)
  !
  do i = 1, 3
    centerIn(i) = nxyzIn(i) / 2.
    nxyzOut(i) = nxyzIn(i)
  enddo
  !
  if (PipGetInOutFile('OutputFile', 2, 'Name of output file', imFileOut) &
      .ne. 0) call exitError('No output file specified')
  !
  call setMemoryLimitAndHdfChunks(pipinput)
  if (pipInput) then
    ierr = PipGetString('TemporaryDirectory', tempDir)
    ierr = PipGetThreeFloats('CenterXYZ', centerIn(1), &
        centerIn(2), centerIn(3))
    ierr = PipGetInteger('InterpolationOrder', interpOrder)
    ierr = PipGetInteger('VerboseOutput', iVerbose)
    ierr = PipGetThreeIntegers('OutputSizeXYZ', nxOut, nyOut, nzOut)
    !
    ! transforms
    !
    ierr = PipNumberOfEntries('TransformFile', numXFiles)
    ierr = PipNumberOfEntries('3DTransform', numXLines)
    numTrans = numXFiles + numXLines
    if (numTrans == 0) call exitError('No transforms specified')
    call PipAllowCommaDefaults(0)
    do iTrans = 1, numTrans
      if (iTrans <= numXFiles) then
        !
        ! read the files in turn
        !
        ierr = PipGetString('TransformFile', fileOut)
        call dopen(1, fileOut, 'ro', 'f')
        read(1,*) ((aFwd(i, j), j = 1, 3), dxyzIn(i), i = 1, 3)
        close(1)
      else
        !
        ! Then read the in-line transforms in turn
        !
        numToGet = 12
        ierr = PipGetFloatArray('3DTransform', cell, numToGet, 12)
        do i = 1, 3
          do j = 1, 3
            aFwd(i, j) = cell(j + 4 * (i - 1))
          enddo
          dxyzIn(i) = cell(4 * i)
        enddo
      endif
      if (iTrans > 1) then
        call xfCopy3d(aFwd, dxyzIn, aTmp2, dxyzTemp2)
        call xfMult3d(aTmp1, dxyzTemp1, aTmp2, dxyzTemp2, aFwd, dxyzIn)
      endif
      call xfCopy3d(aFwd, dxyzIn, aTmp1, dxyzTemp1)
    enddo
  else
    !
    ! interactive input: transforms can alternate between coming
    ! from file or from input arbitrarily
    !
    write(*,'(1x,a,/,a,$)') 'Enter path name of directory for '// &
        'temporary files, ', ' or Return to use current directory: '
    read(5, '(a)') tempDir
    !
    write(*,'(1x,a,3i5,a,$)') 'X, Y, and Z dimensions of the '// &
        'output file (/ for', nxOut, nyOut, nzOut, '): '
    read(5,*) nxOut, nyOut, nzOut
    !
    write(*,'(1x,a,$)') &
        'Number of successive transformations to apply: '
    read(5,*) numTrans
    do iTrans = 1, numTrans
      print *,'For transformation matrix #', iTrans, &
          ', either enter the name of a file', &
          ' with the transformation, or Return to enter the', &
          ' transformation directly'
      read(5, '(a)') fileOut
      if (fileOut == ' ') then
        print *,'Enter transformation matrix #', iTrans
        read(5,*) ((aFwd(i, j), j = 1, 3), dxyzIn(i), i = 1, 3)
      else
        call dopen(1, fileOut, 'ro', 'f')
        read(1,*) ((aFwd(i, j), j = 1, 3), dxyzIn(i), i = 1, 3)
        close(1)
      endif
      if (iTrans > 1) then
        call xfCopy3d(aFwd, dxyzIn, aTmp2, dxyzTemp2)
        call xfMult3d(aTmp1, dxyzTemp1, aTmp2, dxyzTemp2, aFwd, dxyzIn)
      endif
      call xfCopy3d(aFwd, dxyzIn, aTmp1, dxyzTemp1)
    enddo
  endif
  !
  if (nxOut < 1 .or. nyOut < 1 .or.  nzOut < 1) &
      call exitError('Illegal output size')
  print *,'Forward matrix:'
  write(*,102) ((aFwd(i, j), j = 1, 3), dxyzIn(i), i = 1, 3)
102 format(3f10.6,f10.3)
  !
  ! get matrix for inverse transform
  !
  call xfInv3d(aFwd, dxyzIn, aInv, cxyzIn)
  print *,'Inverse matrix:'
  write(*,102) ((aInv(i, j), j = 1, 3), cxyzIn(i), i = 1, 3)

  if (pipInput) then
    fileOut = ' '
    ierr = PipGetString('InverseFile', fileOut)
  else
    print *,'Enter name of file to place inverse transformation in,' &
        , ' or Return for none'
    read(5, '(a)') fileOut
  endif
  if (fileOut .ne. ' ') then
    call dopen(1, fileOut, 'new', 'f')
    write(1, 102) ((aInv(i, j), j = 1, 3), cxyzIn(i), i = 1, 3)
    close(1)
  endif
  call PipDone()
  !
  call imopen(6, imFileOut, 'NEW')
  !
  ! Set up the arrangement of input/output data, allocate arrays
  call setup_cubes_scratch(aFwd, aInv, 1, 0, fileIn, tempDir, tempExt, tim, &
      .false.)
  !
  ! DNM 7/26/02: transfer pixel spacing to same axes; could be weird
  ! if spacings are not isotropic
  !
  call irtdel(5, delta)
  do i = 1, 3
    cell(i) = nxyzOut(i) * delta(i)
    cell(i + 3) = 90.
    ! cxyzin(i) =cxyzin(i) +nxyzin(i) /2.
    cxyzIn(i) = cxyzIn(i) + centerIn(i)
    cxyzOut(i) = nxyzOut(i) / 2.
  enddo
  !
  call icrhdr(6, nxyzOut, nxyzOut, mode, title, 0)
  call ialcel(6, cell)
  call itrlab(6, 5)
  !
  write(titlech, 302) dat, tim
  read(titlech, '(20a4)') (title(kti), kti = 1, 20)
302 format('MATCHVOL: 3-D transformation of tomogram:',t57,a9,2x,a8)
  call irttlt(5, tiltOld)
  call ialtlt(6, tiltOld)
  !
  ! Preserve origin in case of volume scaling
  !
  call irtorg(5, delta(1), delta(2), delta(3))
  call ialorg(6, delta(1), delta(2), delta(3))
  !
  ! Do the work and exit
  call transform_cubes(interpOrder)
end program matchvol
