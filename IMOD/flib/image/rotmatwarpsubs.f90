! rotmatwarpsubs.f90  - contains subroutines for Rotatevol, Matchvol, and
! Warpvol
!
! $Id$
!
! SETUP_CUBES_SCRATCH determines the needed size of the scratch files
! opens the scratch files, and sets up tables for which cubes use
! which file
!
subroutine setup_cubes_scratch(aFwd, amInv, numLocTot, nExtra, fileIn, &
    tempDir, tempExt, tim, warping)
  use rotmatwarp
  implicit none
  real*4 aFwd(3,3), amInv(3,3,*)
  character*(*) fileIn
  character*(*) tempDir
  character*(*) tempExt
  character*(*) tim
  logical*4 warping
  integer*4 nxysCubeBase(3), nxyzScr(3), nbigCube(3), nExtra, numLocTot, numChunks(3)
  integer*4 i, ind, j, ix, iy, iz, inMin(3), inMax(3), ival, nzExtra, nChunkSave(3)
  character*320 temp_filename
  character*320 tempName
  real*8 memSize8
  integer*4 iyAxis, izAxis, minYZsize
  real*4 aspect, efficiency, cubeEfficiency, xcen, ycen, zcen
  real*4 aspectMax, relEfficiencyCrit, delAspect
  integer*4 iiuAltChunkSizes
  !
  ! Parameters controlling aspect ratio limit and search
  aspectMax = 4.
  relEfficiencyCrit = 0.9
  minYZsize = 200
  delAspect = 0.2

  memSize8 = memoryLim
  memSize8 = memSize8 * 1024. * 1024. / 4.
  !
  ! Find dominant axis for output from elongation of input along X
  ! Direction turns out to be counterproductive, so it is not used
  idirXaxis = 1
  if (abs(aFwd(2, 1)) >= abs(aFwd(1, 1)) .and. abs(aFwd(2, 1)) >= &
      abs(aFwd(3, 1))) then
    ioutXaxis = 2
    if (aFwd(2, 1) < 0) idirXaxis = -1
    if (abs(aFwd(1, 2)) >= abs(aFwd(2, 2)) .and. abs(aFwd(1, 2)) >= &
        abs(aFwd(3, 2))) then
      ioutYaxis = 1
      ioutZaxis = 3
    else
      ioutYaxis = 3
      ioutZaxis = 1
    endif
  else if (abs(aFwd(3, 1)) >= abs(aFwd(1, 1)) .and. abs(aFwd(3, 1)) >= &
      abs(aFwd(2, 1))) then
    ioutXaxis = 3
    if (aFwd(3, 1) < 0) idirXaxis = -1
    if (abs(aFwd(2, 2)) >= abs(aFwd(1, 2)) .and. abs(aFwd(2, 2)) >= &
        abs(aFwd(3, 2))) then
      ioutYaxis = 2
      ioutZaxis = 1
    else
      ioutYaxis = 1
      ioutZaxis = 2
    endif
  else
    ioutXaxis = 1
    if (aFwd(1, 1) < 0) idirXaxis = -1
    if (abs(aFwd(2, 2)) >= abs(aFwd(1, 2)) .and. abs(aFwd(2, 2)) >= &
        abs(aFwd(3, 2))) then
      ioutYaxis = 2
      ioutZaxis = 3
    else
      ioutYaxis = 3
      ioutZaxis = 2
    endif
  endif
  iyAxis = mod(ioutXaxis, 3) + 1
  izAxis = mod(ioutXaxis + 1, 3) + 1
  !
  ! Do not use multiple slices if 1) Not warping and outer axis = Z or
  ! 2) warping and inner axis is not Z
  if ((.not.warping .and. ioutZaxis == 3) .or. &
      (warping .and. ioutXaxis .ne. 3)) maxZout = 1
  !
  ! Loop on increasing aspect ratios along that axis
  aspect = 1.
  do
    call findSizesForAspect()
    !
    ! first time, record efficiency of cube
    if (aspect == 1) cubeEfficiency = efficiency
    !
    ! If there is only one cube on the output of the X axis, use this ratio
    if (nCubes(ioutXaxis) == 1) exit
    !
    ! If the efficiency has fallen too far, or if the aspect ratio is above
    ! a limit AND one of the other axes has become too small, back up to
    ! last aspect ratio (if any) and use that one
    if (efficiency / cubeEfficiency < relEfficiencyCrit .or. &
        (aspect > aspectMax .and. &
        ((nCubes(iyAxis) > 1 .and. nxysCubeBase(iyAxis) < minYZsize) .or. &
        (nCubes(izAxis) > 1 .and. nxysCubeBase(izAxis) < minYZsize)))) &
        then
      if (iVerbose > 0) print *,'Efficiency ratio', efficiency / cubeEfficiency
      if (aspect > 1.) then
        aspect = aspect - delAspect
        call findSizesForAspect()
      endif
      exit
    endif
    !
    ! Or, loop on next value of aspect ratio
    aspect = aspect + delAspect
  enddo

  ! Find the true output chunk sizes if chunking
  ! Make that be the cube size if there were going to be scratch files, and set up
  ! chunk output regardless if there actually are chunks specified by at least one
  ! non-default entry; otherwise cancel chunking
  if (chunkedHDF) then
    ix = 0
    do i = 1, 3
      nChunkSave(:) = nxyzChunk(:)
      if (nxyzChunk(i) > 0) then 
        nxyzChunk(i) = min(nxyzChunk(i), nxysCubeBase(i))
      else
        nxyzChunk(i) = nxysCubeBase(i)
        ix = ix + 1
      endif
      call iiLimitedTileSize(nxyzOut(i), nxyzChunk(i), numChunks(i), 1, nxysCubeBase(i))
      if (ncubes(1) * ncubes(2) > 1) then
        nxysCubeBase(i) = nxyzChunk(i)
        ncubes(i) = numChunks(i)
      endif
    enddo
    if (fileIn .ne. ' ') then
      if (ncubes(1) * ncubes(2) == 1 .and. ix == 3) then
        chunkedHDF = .false.
      else
        if (iiuAltChunkSizes(6, nxChunk, nyChunk, nzChunk) .ne. 0)  &
            call exitError('Setting up chunks in the HDF output file')
      endif
    else
      nxyzChunk(:) = nChunkSave(:)
    endif
  endif
  !
  ! Compute true needed input dimensions now that cube sizes are known
  ! REPLACE inputDim rather than adjusting it downward, it can be too small
  ! But don't let it get any bigger than actual file size
  inMin = 1000000
  inMax = -1000000
  do j = 1, numLocTot
    do ix = 0, 1
      do iy = 0, 1
        do iz = 0, 1
          xcen = ix * (nxysCubeBase(1) + 1.)
          ycen = iy * (nxysCubeBase(2) + 1.)
          zcen = iz * (nxysCubeBase(3) + 1.)
          do i = 1, 3
            ival = nint(amInv(i, 1, j) * xcen + amInv(i, 2, j) * ycen + amInv(i, 3, j) * &
                zcen)
            ! Adding 2 instead of 1 may be overkill but gives extra safety
            inMin(i) = min(inMin(i), ival - 2)
            inMax(i) = max(inMax(i), ival + 2)
          enddo
        enddo
      enddo
    enddo
  enddo
  do i = 1, 3
    inputDim(i) = min(nxyzIn(i), nExtra + inMax(i) + 1 - inMin(i))
  enddo
  if (iVerbose > 0) &
      write(*,'(a,3i6)') 'Input size adjusted for actual cubes:', inputDim
  arraySize8 = inputDim(1)
  arraySize8 = (arraySize8 * inputDim(2)) * inputDim(3)
  nzExtra = 0
  !
  ! now compute sizes of nearly equal sized near cubes to fill output
  ! volume, store the starting index coordinates
  !
  do i = 1, 3
    if (nCubes(i) > LMCUBE) then
      if (fileIn .ne. ' ' .or. nExtra < 100) call exitError( &
          'Too many cubes in longest direction to fit in arrays')
      call exitError('Too many cubes for arrays, check for wild '// &
          'vectors/big jumps between transforms')
    endif

    nbigCube(i) = mod(nxyzOut(i), nCubes(i))
    ind = 0
    do j = 1, nCubes(i)
      ixyzCube(i, j) = ind
      nxyzCube(i, j) = min(nxyzOut(i) - ind, nxysCubeBase(i))
      if (j <= nbigCube(i) .and. .not. chunkedHDF) nxyzCube(i, j) = nxyzCube(i, j) + 1
      ind = ind + nxyzCube(i, j)
    enddo
    nxyzScr(i) = nxysCubeBase(i) + 1
  enddo
  !
  ! This was originally a test that could fail - and it failed for nz = 1, so
  ! increase the number of planes allocated as needed to hold output row
  do while (nxOut * (nxysCubeBase(2) + 1.) >= arraySize8)
    nzExtra = nzExtra + 1
    arraySize8 = inputDim(1)
    arraySize8 = (arraySize8 * inputDim(2)) * (inputDim(3) + nzExtra)
    if (arraySize8 > 2 * memSize8) then
      print *,inputDim, nxOut, nxysCubeBase(2), nxOut * (nxysCubeBase(2) + 1.), &
          arraySize8, nzExtra, memSize8
      call exitError('Something is wrong trying to make array big enough for output')
    endif
  enddo
  !
  if (fileIn == ' ') return
  !
  ! Allocate memory
  allocate (array(inputDim(1), inputDim(2), inputDim(3) + nzExtra), &
      brray(idimOut(1) * idimOut(2) * maxZout), &
      ifile(nCubes(1), nCubes(2)), &
      izInFile(nCubes(1), nCubes(2), nxyzScr(3)), stat = j)
  if (j .ne. 0) call exitError('Failed to allocate memory for arrays')
  !
  ! Set limits of inner and outer loops depending on axis
  if (ioutXaxis == 1) then
    limInner = nCubes(1)
    limOuter = nCubes(2)
  else
    limInner = nCubes(2)
    limOuter = nCubes(1)
  endif
  !
  ! get an array of file numbers for each cube in X/Y plane
  ! If only one cube in plane, set to final output file
  needScratch = .false.
  do ix = 1, nCubes(1)
    do iy = 1, nCubes(2)
      ifile(ix, iy) = 1
      if (ix > nbigCube(1)) ifile(ix, iy) = ifile(ix, iy) + 1
      if (iy > nbigCube(2)) ifile(ix, iy) = ifile(ix, iy) + 2
      if (nCubes(1) * nCubes(2) == 1 .or. chunkedHDF) then
        ifile(ix, iy) = 6
        if (ix == 1 .and. iy == 1) then
          if (.not. chunkedHDF) print *,'Writing directly to output file'
          if (chunkedHDF) write(*,'(a,i6,a,i5,a,i5)')'Writing directly to output '// &
              'file in chunks of', nxChunk, ' x', nyChunk, ' x', nzChunk

        endif
      else
        needScratch(ifile(ix, iy)) = .true.
      endif
    enddo
  enddo

  write(*,103) nCubes(3), nCubes(1), nCubes(2)
103 format(' Rotations done in',i3,' layers, with',i3,' by',i3, &
      ' cubes in each layer')
  !
  ! If needed, open scratch files with 4 different sizes.
  ! compose temporary filenames from the time
  !
  tempExt(4:5) = tim(1:2)
  tempExt(6:7) = tim(4:5)
  tempExt(8:9) = tim(7:8)
  tempName = temp_filename(fileIn, tempDir, tempExt)
  !
  nxyzScr(3) = nxyzScr(3) * nCubes(1) * nCubes(2)
  call ialprt(.false.)
  if (needScratch(1)) then
    call imopen(1, tempName, 'scratch')
    call iiuCreateHeader(1, nxyzScr, nxyzScr, mode, title, 0)
  endif
  !
  tempExt(10:10) = '2'
  tempName = temp_filename(fileIn, tempDir, tempExt)
  nxyzScr(1) = nxyzScr(1) - 1
  if (needScratch(2)) then
    call imopen(2, tempName, 'scratch')
    call iiuCreateHeader(2, nxyzScr, nxyzScr, mode, title, 0)
  endif
  !
  tempExt(10:10) = '4'
  tempName = temp_filename(fileIn, tempDir, tempExt)
  nxyzScr(2) = nxyzScr(2) - 1
  if (needScratch(4)) then
    call imopen(4, tempName, 'scratch')
    call iiuCreateHeader(4, nxyzScr, nxyzScr, mode, title, 0)
  endif
  !
  tempExt(10:10) = '3'
  tempName = temp_filename(fileIn, tempDir, tempExt)
  nxyzScr(1) = nxyzScr(1) + 1
  if (needScratch(3)) then
    call imopen(3, tempName, 'scratch')
    call iiuCreateHeader(3, nxyzScr, nxyzScr, mode, title, 0)
  endif
  return

  !
  ! INTERNAL routine FINDSIZESFORASPECT to find sizes of input/output
  ! given an aspect ratio, adjusting memory as needed for output slices

contains
  subroutine findSizesForAspect()
    real*4 devMax(3), devFac(3), expandFac
    integer*4 numOver, indUnder, indOver, numIterReduce, iterReduce
    real*8 memReduced
    !
    ! Find the maximum deviation in each direction of input required to
    ! output an elongated cube
    devMax = 0.
    devFac = 1.
    devFac(ioutXaxis) = aspect
    do j = 1, numLocTot
      do ix = -1, 1, 2
        do iy = -1, 1, 2
          do i = 1, 3
            devMax(i) = max(devMax(i), abs(amInv(i, 1, j) * ix * devFac(1) + &
                amInv(i, 2, j) * iy * devFac(2) + amInv(i, 3, j) * devFac(3)))
          enddo
        enddo
      enddo
    enddo

    ! Get the fraction of input data used in output
    efficiency = aspect / (devMax(1) * devMax(2) * devMax(3))
    if (iVerbose > 0) &
        write(*,'(a,f6.2,a,f7.4,a,3f10.5)') 'Aspect:', aspect, ' efficiency' &
        , efficiency, ' deviations:', devMax
    !
    ! Find factor by which input can be expanded to use up the memory
    ! But first get an initial estimate of the output size to allow a
    ! reduction for memory needed for the multiple slices
    ! of output to be stored; set up to iterate with better output size
    numIterReduce = 1
    memReduced = memSize8
    if (maxZout > 1) then
      expandFac = (memSize8 / (devMax(1) * devMax(2) * devMax(3)))**0.333333
      idimOut(1) = min(nxyzOut(1), int(devFac(1) * expandFac))
      idimOut(2) = min(nxyzOut(2), int(devFac(2) * expandFac))
      numIterReduce = 3
    endif
    !
    ! Iterate to stabilize memory division between input and output
    do iterReduce = 1, numIterReduce
      if (maxZout > 1) then
        !
        ! Reduce by the amount needed for output, but do not decrease the
        ! reduction from previous time, and keep it to half the total
        memReduced = max(memSize8 / 2., min(memReduced, &
            memSize8 - float(maxZout) * idimOut(1) * idimOut(2)))
        if (iVerbose > 0) write(*,'(a,i3,a,i7,a)') 'Iteration', iterReduce, &
            ': for multiple output slices, input memory reduced to', &
            nint(memReduced / (1024. *256.)), ' MB'
      endif
      !
      ! get expansion factor that would use up the memory
      ! and get trial size for equal expansion
      expandFac = (memReduced / (devMax(1) * devMax(2) * devMax(3)))**0.333333
      inputDim = devMax * expandFac + 1.
      !
      ! Count axes where this is big enough for whole volume, and keep track
      ! of at least one axis where it is and is not
      numOver = 0
      indUnder = 1
      indOver = 1
      do i = 1, 3
        if (inputDim(i) >= nxyzIn(i)) then
          numOver = numOver + 1
          indOver = i
        else
          indUnder = i
        endif
      enddo
      if (iVerbose > 0)  write(*,'(a,f9.2,3i8,a,i3)') 'Initial fac', &
          expandFac, inputDim, ' numover', numOver
      !
      ! If this is oversized in one axis, expand the other two axes and
      ! re-evaluate if they are over
      if (numOver == 1 .and. inputDim(indOver) > nxyzIn(indOver)) then
        expandFac = expandFac * sqrt(real(inputDim(indOver)) / nxyzIn(indOver))
        inputDim(indOver) = nxyzIn(indOver)
        do i = 0, 1
          ix = mod(indOver + i, 3) + 1
          inputDim(ix) = devMax(ix) * expandFac + 1.
          if (inputDim(ix) >= nxyzIn(ix)) then
            numOver = numOver + 1
          else
            indUnder = ix
          endif
        enddo
        if (iVerbose > 0) write(*,'(a,f9.2,3i8,a,i3)') 'Expand 2 axes', &
            expandFac, inputDim, ' numover', numOver
      endif
      !
      ! Now if it is oversized in two axes, expand the remaining axis
      if (numOver == 2) then
        ix = mod(indUnder, 3) + 1
        iy = mod(indUnder + 1, 3) + 1
        inputDim(ix) = nxyzIn(ix)
        inputDim(iy) = nxyzIn(iy)
        expandFac = ((memReduced / nxyzIn(ix)) / nxyzIn(iy)) / devMax(indUnder)
        inputDim(indUnder) = devMax(indUnder) * expandFac + 1.
        if (inputDim(indUnder) >= nxyzIn(indUnder)) numOver = 3
        if (iVerbose > 0) write(*,'(a,f9.2,3i8,a,i3)') 'Expand 1 axis', &
            expandFac, inputDim, ' numover', numOver
      endif
      !
      ! If over in all axes, set up input and output sizes
      if (numOver == 3) then
        inputDim = nxyzIn
        idimOut = nxyzOut
      else
        !
        ! Otherwise, set up the output sizes by expanding the elongated cube
        do ix = 1, 3
          idimOut(ix) = min(nxyzOut(ix), int(devFac(ix) * expandFac) - 2 * &
              nExtra - 2)
        enddo
      endif
      !
      ! Get number of cubes and basic cube size on each axis, adjust out size
      nCubes = (nxyzOut - 1) / idimOut + 1
      nxysCubeBase = nxyzOut / nCubes
      if (iVerbose > 0) write(*,'(a,3i6,a,3i4,a,3i6)') 'Initial out', &
          idimOut, '  # cubes', nCubes, '  size', nxysCubeBase
      idimOut = nxysCubeBase + 1
    enddo
  end subroutine findSizesForAspect

end subroutine setup_cubes_scratch

! setMemoryLimitAndHdfChunks sets the memory limit according to the default and entered
! option, then checks if the chunk option has been entered, and if so sets up for an
! HDF output file and sets the memory limit to account for the chunks size limit.
!
subroutine setMemoryLimitAndHdfChunks(pipInput)
  use rotmatwarp
  implicit none
  logical*4 pipInput
  real*8 physMem/0./, b3dPhysicalMemory
  integer*4 ierr/1/, PipGetInteger, b3dOutputFileType, PipGetThreeIntegers

  if (pipInput) ierr = PipGetInteger('MemoryLimit', memoryLim)
  physMem = b3dPhysicalMemory()
  if (ierr > 0 .and. physMem > 0.) then
    physMem = physMem  / 1024.**2
    if (physMem < 30000) then
      memoryLim = max(768., min(0.6 * physMem, 12000., physMem - 1000.))
    else
      memoryLim = 0.4 * physMem
    endif
  endif
  if (.not. pipInput) return

  ! If chunks, limit size to 4 GPixel as the easiest way to keep chunks legal
  ierr = PipGetThreeIntegers('ChunkSizesForHDF', nxChunk, nyChunk, nzChunk)
  if (nxChunk >= 0 .or. nyChunk >= 0 .or. nzChunk >= 0) then
    if (nxChunk < 0 .or. nyChunk < 0 .or. nzChunk < 0) call exitError( &
        'You cannot enter a negative chunk size; enter 0 for default size on an axis')
    memoryLim = min(memoryLim, 15000)
    call overrideOutputType(5)
    chunkedHDF = .true.
    if (b3dOutputFileType() .ne. 5) call exitError( &
        'Chunks cannot be used; this package was not built with HDF support')
  endif
end subroutine setMemoryLimitAndHdfChunks

!
! TRANSFORM_CUBES does the complete work of looping on all of the
! cubes, transforming them, writing them to files, and calling the
! routine to reassemble the output file.
! It is used by Rotatevol and Matchvol
!
subroutine transform_cubes(interpOrder)
  use rotmatwarp
  implicit none
  integer*4 interpOrder
  integer*4 izSec(6)
  integer*4 inMin(3), inMax(3), icube(3)
  integer*4 ixCube, iyCube, izCube, i, indx, indy, indz, ival, ifempty
  real*4 xcen, ycen, zcen, xOffsOut, xOffsIn, yOffsIn, zOffsIn, xp, yp, zp
  integer*4 ixLimit, iyLimit, izLimit, ixp, iyp, izp, ixpP1, ixpM1
  real*4 bval, dx, dy, dz
  real*4 dxSq, dySq, dzSq, fx, fxM1, fxP1, fy, fyM1, fyP1, fz, fzM1, fzP1
  integer*4 iypP1, iypM1, izpP1, izpM1, iunit, iy, ix, iz, numDone
  real*4 dmin, dmax, tsum, dmean
  real*4 xpOffset, ypOffset, zpOffset, d11, d12, d21, d22
  integer*4 indInner, indOuter, izStart, numZleft, numZtoDo, izEnd
  integer*4 innerStart, innerEnd, middleStart, middleEnd, iouterStart, iouterEnd
  integer*4 istride(3), innerStride, middleStride, iouterStride, indBase
  integer*4 iouter, middle, inner, numThreads, numOMPthreads
  real*4 cenOuter, cenMid, cenInner
  real*8 wallTime, wallStart, wallCum
  !
  dmin = 1.e20
  dmax = -dmin
  tsum = 0.
  numDone = 0
  wallCum = 0.
  istride(1) = 1
  istride(2) = idimOut(1)
  istride(3) = idimOut(1) * idimOut(2)
  innerStride = istride(ioutXaxis)
  middleStride = istride(ioutYaxis)
  iouterStride = istride(ioutZaxis)
  ! print *,'strides', istride
  !
  ! loop on layers of cubes in Z, do all I/O to complete layer
  !
  izSec(6) = 0
  do izCube = 1, nCubes(3)
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
        call cubeIndexes(ioutXaxis, indInner, indOuter, ixCube, iyCube)
        icube(1) = ixCube
        icube(2) = iyCube
        !
        ! back-transform the corner coordinates of the output cube to
        ! find the limiting index coordinates of the input cube
        !
        do i = 1, 3
          inMin(i) = 1000000
          inMax(i) = -1000000
        enddo
        do indx = 0, 1
          do indy = 0, 1
            do indz = 0, 1
              xcen = ixyzCube(1, ixCube) + indx * nxyzCube(1, ixCube) - cxOut
              ycen = ixyzCube(2, iyCube) + indy * nxyzCube(2, iyCube) - cyOut
              zcen = ixyzCube(3, izCube) + indz * nxyzCube(3, izCube) - czOut
              do i = 1, 3
                ival = nint(aInv(i, 1) * xcen + aInv(i, 2) * ycen + &
                    aInv(i, 3) * zcen + cxyzIn(i))
                inMin(i) = max(0, min(inMin(i), ival - 1))
                inMax(i) = min(nxyzIn(i) - 1, max(inMax(i), ival + 1))
              enddo
            enddo
          enddo
        enddo
        ifempty = 0
        do i = 1, 3
          if (inMin(i) > inMax(i)) ifempty = 1
          if (inMax(i) + 1 - inMin(i) > inputDim(i)) then
            write(*,'(/,a,i2,3i6)') 'ERROR: Transform_cubes - input '// &
                'data larger than calculated size:', i, inputDim(i), &
                inMin(i), inMax(i)
            call exit(1)
          endif
        enddo
        !
        ! load the input cube
        !
        if (ifempty == 0) then
          do iz = inMin(3), inMax(3)
            call iiuSetPosition(5, iz, 0)
            if (iVerbose > 1) &
                write(*,'(10i5)') ixCube, iyCube, izCube, iz, iz + 1 - inMin(3) &
                , inMin(1), inMax(1), inMin(2), inMax(2)
            call irdpas(5, array(1, 1, iz + 1 - inMin(3)), inputDim(1), inputDim(2), &
                inMin(1), inMax(1), inMin(2), inMax(2),*99)
          enddo
        endif
        !
        ! prepare offsets and limits
        !
        xOffsOut = ixyzCube(ioutXaxis, icube(ioutXaxis)) - 1 - cxyzOut(ioutXaxis)
        xOffsIn = cxIn + 1 - inMin(1)
        yOffsIn = cyIn + 1 - inMin(2)
        zOffsIn = czIn + 1 - inMin(3)
        ixLimit = inMax(1) + 1 - inMin(1)
        iyLimit = inMax(2) + 1 - inMin(2)
        izLimit = inMax(3) + 1 - inMin(3)
        !
        ! loop over Z segments of the output cube
        izStart = 1
        numZleft = nxyzCube(3, izCube)
        do while (numZleft > 0)
          numZtoDo = min(numZleft, maxZout)
          izEnd = izStart + numZtoDo - 1
          ! print *,ixcube, iycube, izStart, izEnd, numZleft
          if (ifempty == 0) then

            ! Set up index limits for inner loop
            if (ioutXaxis == 3) then
              innerStart = izStart
              innerEnd = izEnd
            else
              innerStart = 1
              innerEnd = nxyzCube(ioutXaxis, icube(ioutXaxis))
            endif
            !
            ! Set up limits for middle loop
            if (ioutYaxis == 3) then
              middleStart = izStart
              middleEnd = izEnd
            else
              middleStart = 1
              middleEnd = nxyzCube(ioutYaxis, icube(ioutYaxis))
            endif
            !
            ! Set up limits for outer loop
            if (ioutZaxis == 3) then
              iouterStart = izStart
              iouterEnd = izEnd
            else
              iouterStart = 1
              iouterEnd = nxyzCube(ioutZaxis, icube(ioutZaxis))
            endif
            indBase = -idimOut(1) - izStart * idimOut(1) * idimOut(2)
            ! print *,indBase
            ! print *,iouterStart, iouterEnd, middleStart, middleEnd, innerStart, innerEnd
            wallStart = wallTime()
            !
            ! There is no good way to base this on loop size, 8 threads is good for a 
            ! a large volume, less for small one
            numThreads = numOMPthreads(8);
            ! 
            ! Start looping over outer axis of this Z segment; parallelize
            ! loop over middle and inner axes
            do iouter = iouterStart, iouterEnd
              cenOuter = ixyzCube(ioutZaxis, icube(ioutZaxis)) + iouter - 1 - &
                  cxyzOut(ioutZaxis)
              !$OMP PARALLEL DO NUM_THREADS(numThreads) &
              !$OMP& SHARED(nxyzCube, ixyzCube, aInv, xOffsIn, yOffsIn, zOffsIn) &
              !$OMP& SHARED(icube, cxyzOut, cenOuter, iouter, indBase, middleStart) &
              !$OMP& SHARED(interpOrder, dmeanIn, array, brray, ixLimit, iyLimit) &
              !$OMP& SHARED(xOffsOut, innerStart, innerEnd, ioutXaxis, ioutYaxis) &
              !$OMP& SHARED(ioutZaxis, innerStride, middleStride, iouterStride) &
              !$OMP& SHARED(izLimit, middleEnd) &
              !$OMP& DEFAULT(private)
              do middle = middleStart, middleEnd
                cenMid = ixyzCube(ioutYaxis, icube(ioutYaxis)) + middle - 1 &
                    - cxyzOut(ioutYaxis)
                xpOffset = aInv(1, ioutYaxis) * cenMid + aInv(1, ioutZaxis) * cenOuter &
                    + xOffsIn
                ypOffset = aInv(2, ioutYaxis) * cenMid + aInv(2, ioutZaxis) * cenOuter &
                    + yOffsIn
                zpOffset = aInv(3, ioutYaxis) * cenMid + aInv(3, ioutZaxis) * cenOuter &
                    + zOffsIn
                if (interpOrder >= 2) then
                  do inner = innerStart, innerEnd
                    cenInner = inner + xOffsOut
                    !
                    ! get indices in array of input data
                    !
                    xp = aInv(1, ioutXaxis) * cenInner + xpOffset
                    yp = aInv(2, ioutXaxis) * cenInner + ypOffset
                    zp = aInv(3, ioutXaxis) * cenInner + zpOffset
                    bval = dmeanIn
                    !
                    ! do triquadratic interpolation with higher-order
                    ! terms omitted
                    !
                    ixp = nint(xp)
                    iyp = nint(yp)
                    izp = nint(zp)
                    if (ixp >= 1 .and. ixp <= ixLimit .and. &
                        iyp >= 1 .and. iyp <= iyLimit .and. &
                        izp >= 1 .and. izp <= izLimit) then
                      dx = xp - ixp
                      dy = yp - iyp
                      dz = zp - izp
                      ixpP1 = min(ixLimit, ixp + 1)
                      iypP1 = min(iyLimit, iyp + 1)
                      izpP1 = min(izLimit, izp + 1)
                      ixpM1 = max(1, ixp - 1)
                      iypM1 = max(1, iyp - 1)
                      izpM1 = max(1, izp - 1)
                      !
                      ! Set up terms for quadratic interpolation
                      !
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
                    endif
                    brray(indBase + inner * innerStride + middle * &
                        middleStride + iouter * iouterStride) = bval
                  enddo
                else
                  !
                  ! linear interpolation
                  !
                  do inner = innerStart, innerEnd
                    cenInner = inner + xOffsOut
                    !
                    ! get indices in array of input data
                    !
                    xp = aInv(1, ioutXaxis) * cenInner + xpOffset
                    yp = aInv(2, ioutXaxis) * cenInner + ypOffset
                    zp = aInv(3, ioutXaxis) * cenInner + zpOffset
                    bval = dmeanIn
                    ixp = int(xp)
                    iyp = int(yp)
                    izp = int(zp)
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
                    brray(indBase + inner * innerStride + middle * &
                        middleStride + iouter * iouterStride) = bval
                  enddo
                endif
              enddo
              !$OMP END PARALLEL DO
            enddo
          else
            brray(1 : istride(3) * numZtoDo) = dmeanIn
          endif
          wallCum = wallCum + wallTime() - wallStart
          call writeOneCube(ixCube, iyCube, numZtoDo, izStart, izSec, dmin, dmax, tsum)
          !
          ! End of do while: update start and number left to do
          izStart = izEnd + 1
          numZleft = numZleft - numZtoDo
        enddo
        ! print *,ixcube, iycube, izcube
        numDone = numDone + 1
        write(*,'(a,i6,a,i6)') 'Finished', numDone, ' of', &
            nCubes(1) * nCubes(2) * nCubes(3)
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
  if (iVerbose > 0) write(*,'(a,f10.4)') 'Wall time ', wallCum
  !
  dmean = tsum / nzOut
  call iiuWriteHeader(6, title, 1, dmin, dmax, dmean)
  do i = 1, 4
    if (needScratch(i)) call iiuClose(i)
  enddo
  call iiuClose(5)
  call iiuClose(6)
  call exit(0)
99 call exitError('Reading file')
end subroutine transform_cubes


! CUBEINDEXES Gets X and Y cube indexes from inner and outer loop indexes
! If X becomes X on output, then make X the inner loop
! If X becomes Y, then make X outer and Y inner, of course
! If X becomes Z, it helps a little to make X outer and Y inner
!
subroutine cubeIndexes(ioutXaxis, indInner, indOuter, ixCube, iyCube)
  implicit none
  integer*4 ioutXaxis, indOuter, indInner, ixCube, iyCube
  if (ioutXaxis == 1) then
    ixCube = indInner
    iyCube = indOuter
  else
    iyCube = indInner
    ixCube = indOuter
  endif
  return
end subroutine cubeIndexes

! writeOneCube writes a set of sections to one cube at the given index, maintaining the
! min/max/mean
!
subroutine writeOneCube(ixCube, iyCube, numZtoDo, izStart, izSec, dmin, dmax, tsum)
  use rotmatwarp
  implicit none
  integer*4 ixCube, iyCube, numZtoDo, izStart, izSec(*)
  real*4 dmin, dmax, tsum, tmin, tmax, tmean
  integer*4 ix, iz, iunit

  iunit = ifile(ixCube, iyCube)
  do iz = 0, numZtoDo - 1
    ix = iz * idimOut(1) * idimOut(2) + 1
    if (iunit == 6) then
      call iclden(brray(ix), idimOut(1), idimOut(2), 1, &
          nxyzCube(1, ixCube), 1, nxyzCube(2, iyCube), tmin, tmax, tmean)
      dmin = min(dmin, tmin)
      dmax = max(dmax, tmax)
      tsum = tsum + tmean
    endif
    call irepak(brray(ix), brray(ix), idimOut(1), idimOut(2), &
        0, nxyzCube(1, ixCube) - 1, 0, nxyzCube(2, iyCube) - 1)
    if (chunkedHDF .and. ncubes(1) * ncubes(2) > 1) then
      call iiuSetPosition(iunit, izSec(iunit) + iz + izStart - 1, ixyzCube(2, iyCube))
      call iiuWriteSecPart(iunit, brray(ix), nxyzCube(1, ixCube), 0, &
          ixyzCube(1, ixCube), ixyzCube(1, ixCube) + nxyzCube(1, ixCube) - 1, &
          0, nxyzCube(2, iyCube) - 1)
    else
      call iiuWriteSection(iunit, brray(ix))
      izInFile(ixCube, iyCube, iz + izStart) = izSec(iunit)
      izSec(iunit) = izSec(iunit) + 1
    endif
  enddo
  return
end subroutine writeOneCube

! RECOMPOSE_CUBES takes a layer of cubes in Z out of the scratch files
! and writes it to the output file
!
subroutine recompose_cubes(izCube, dmin, dmax, tsum)
  use rotmatwarp
  implicit none
  real*4 dmin, dmax, tsum, tmin, tmax, tmean, tempSum
  integer*4 iz, iyCube, ixCube, izCube, nLinesOut, izSec, iunit
  integer*4 maxPlanes, numLeft, numToRead, izStart
  integer*4 indInner, indOuter
  !
  ! whole layer of cubes in z is done.  Find our how many planes can be
  ! recomposed at once
  if (nCubes(1) * nCubes(2) == 1 .or. chunkedHDF) return
  maxPlanes = (arraySize8 / nxOut) / nyOut
  if (maxPlanes < 2) then
    !
    ! If only one plane fits, there is no advantage to doing it all at
    ! once, so reread and compose one row of the output section at a time
    ! in array
    !
    do iz = 1, nxyzCube(3, izCube)
      tempSum = 0.
      do iyCube = 1, nCubes(2)
        nLinesOut = nxyzCube(2, iyCube)
        do ixCube = 1, nCubes(1)
          iunit = ifile(ixCube, iyCube)
          izSec = izInFile(ixCube, iyCube, iz)
          call iiuSetPosition(iunit, izSec, 0)
          call irdsec(iunit, brray,*99)
          call pack_piece(array, nxOut, nyOut, ixyzCube(1, ixCube), &
              0, brray, nxyzCube(1, ixCube), nLinesOut)
        enddo
        call iclden(array, nxOut, nLinesOut, 1, nxOut, 1, nLinesOut, tmin, tmax, &
            tmean)
        dmin = min(dmin, tmin)
        dmax = max(dmax, tmax)
        tempSum = tempSum + tmean * nLinesOut
        call iiuWriteLines(6, array, nLinesOut)
      enddo
      tsum = tsum + tempSum / nyOut
    enddo
  else
    !
    ! Otherwise, do entire planes so that multiple successive slices can
    ! be read from the cubes
    numLeft = nxyzCube(3, izCube)
    izStart = 1
    do while (numLeft > 0)
      numToRead = min(maxPlanes, numLeft)
      numLeft = numLeft - numToRead
      !
      ! read this set of planes from each cube in turn
      do indOuter = 1, limOuter
        do indInner = 1, limInner
          call cubeIndexes(ioutXaxis, indInner, indOuter, ixCube, iyCube)
          iunit = ifile(ixCube, iyCube)
          do iz = 1, numToRead
            izSec = izInFile(ixCube, iyCube, iz + izStart - 1)
            call iiuSetPosition(iunit, izSec, 0)
            call irdsec(iunit, brray,*99)
            call pack_piece3D(array, nxOut, nyOut, maxPlanes, &
                ixyzCube(1, ixCube), ixyzCube(2, iyCube), &
                iz, brray, nxyzCube(1, ixCube), nxyzCube(2, iyCube))
          enddo
        enddo
      enddo
      !
      ! Write the planes and maintain MMM
      do iz = 1, numToRead
        call writeFullPlane(array, nxOut, nyOut, maxPlanes, iz, dmin, dmax, &
            tsum)
      enddo
      izStart = izStart + numToRead
    enddo
  endif
  return
99 call exitError('Reading file')
end subroutine recompose_cubes

!
! PACK_PIECE packs the contents of brray into appropriate region of array
!
subroutine pack_piece (array, ixDimOut, iyDimOut, ixOffset, iyOffset, &
    brray, nxIn, nyIn)
  implicit none
  integer*4 ixDimOut, iyDimOut, ixOffset, iyOffset, nxIn, nyIn
  real*4 array(ixDimOut,iyDimOut), brray(nxIn,nyIn)
  integer*4 ix, iy
  do iy = 1, nyIn
    do ix = 1, nxIn
      array(ix + ixOffset, iy + iyOffset) = brray(ix, iy)
    enddo
  enddo
  return
end subroutine pack_piece

!
! PACK_PIECE3D packs the contents of brray into appropriate region and
! slice of the 3D array
!
subroutine pack_piece3D (array, ixDimOut, iyDimOut, izDimOut, ixOffset, iyOffset, iz, &
    brray, nxIn, nyIn)
  implicit none
  integer*4 ixDimOut, iyDimOut, ixOffset, iyOffset, nxIn, nyIn, izDimOut, iz
  real*4 array(ixDimOut,iyDimOut,izDimOut), brray(nxIn,nyIn)
  integer*4 ix, iy
  do iy = 1, nyIn
    do ix = 1, nxIn
      array(ix + ixOffset, iy + iyOffset, iz) = brray(ix, iy)
    enddo
  enddo
  return
end subroutine pack_piece3D

!
! WRITEFULLPLANE calculates and maintains density MMM and writes a plane
!
subroutine writeFullPlane(array, nxOut, nyOut, maxPlanes, iz, dmin, dmax, &
    tsum)
  implicit none
  integer*4 nxOut, nyOut, maxPlanes, iz
  real*4 array(nxOut,nyOut,maxPlanes), dmin, dmax, tsum, tmin, tmax, tmean
  call iclden(array(1, 1, iz), nxOut, nyOut, 1, nxOut, 1, nyOut, tmin, tmax, &
      tmean)
  dmin = min(dmin, tmin)
  dmax = max(dmax, tmax)
  tsum = tsum + tmean
  call iiuWriteSection(6, array(1, 1, iz))
  return
end subroutine writeFullPlane
